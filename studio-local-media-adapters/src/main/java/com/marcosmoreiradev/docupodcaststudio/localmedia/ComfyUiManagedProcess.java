package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** One managed ComfyUI process shared by image and video administration. */
final class ComfyUiManagedProcess implements AutoCloseable {
    private static final Duration DEFAULT_START_TIMEOUT = Duration.ofMinutes(4);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private final Duration startTimeout;
    private volatile Process process;
    private volatile ManagedProcessContainment containment =
            ManagedProcessContainment.NoopProcessContainment.INSTANCE;
    private volatile Instant processStartedAt;
    private volatile boolean adoptedExternal;
    private volatile Path pidFile;

    ComfyUiManagedProcess() {
        this(DEFAULT_START_TIMEOUT);
    }

    ComfyUiManagedProcess(Duration startTimeout) {
        this.startTimeout = startTimeout == null || startTimeout.isNegative() || startTimeout.isZero()
                ? DEFAULT_START_TIMEOUT : startTimeout;
    }

    synchronized void start(RuntimeAssetCatalog assets, ComfyUiLaunchProfile launchProfile,
                            ExecutionContext context) throws IOException, InterruptedException {
        if (process != null && process.isAlive()) return;
        ComfyUiTransport transport = new ComfyUiTransport("http://127.0.0.1:8188");
        ComfyUiLaunchProfile profile = launchProfile == null
                ? ComfyUiLaunchProfile.automatic(ComfyUiMemoryProfile.SAFE_LOW_VRAM)
                : launchProfile;
        if (isReady(transport)) {
            verifyBinding(transport, profile);
            adoptedExternal = true;
            context.progress().report("READY", 1.0,
                    "Se usará el servidor ComfyUI compatible que ya escucha en 127.0.0.1:8188. "
                            + deviceSummary(profile));
            return;
        }
        adoptedExternal = false;
        List<String> repairs = ComfyUiRuntimePreparation.prepare(assets);
        Path python = assets.require(ComfyUiImageEngine.ID, "python");
        Path main = assets.require(ComfyUiImageEngine.ID, "main");
        Path work = assets.require(ComfyUiImageEngine.ID, "runtime");
        if (!Files.isRegularFile(python) || !Files.isRegularFile(main) || !Files.isDirectory(work)) {
            throw new IOException("Falta el runtime ComfyUI administrado.");
        }
        Path logs = assets.root().resolve("logs/comfyui").normalize();
        if (!logs.startsWith(assets.root())) throw new IOException("Ruta de logs ComfyUI inválida.");
        Files.createDirectories(logs);
        ArrayList<String> command = new ArrayList<>(List.of(python.toString(), main.toString()));
        Path extraModelPaths = assets.require(ComfyUiImageEngine.ID, "extraModelPaths");
        if (Files.isRegularFile(extraModelPaths)) {
            command.add("--extra-model-paths-config");
            command.add(extraModelPaths.toString());
        }
        command.addAll(profile.launchArguments());
        command.addAll(List.of("--disable-auto-launch", "--port", "8188"));
        context.progress().report("STARTING", 0.3,
                "Iniciando ComfyUI con " + deviceSummary(profile)
                        + " Argumentos: " + String.join(" ", profile.launchArguments()) + ". "
                        + String.join(" ", repairs));
        process = new ProcessBuilder(command)
                .directory(work.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logs.resolve("comfyui.log").toFile()))
                .start();
        containment = ManagedProcessContainment.create();
        containment.attach(process);
        processStartedAt = OwnedProcessDiagnostics.started("COMFYUI", process, command,
                "ComfyUiManagedProcess", "application shutdown or explicit engine stop",
                containment.diagnostics());
        pidFile = logs.resolve("comfyui.pid");
        Files.writeString(pidFile, Long.toString(process.pid()));
        awaitReady(transport, logs.resolve("comfyui.log"), profile, context);
    }

    synchronized boolean recoverStopped(RuntimeAssetCatalog assets, ComfyUiLaunchProfile profile,
                                        ExecutionContext context) throws IOException, InterruptedException {
        // Never restart an external server, a live process, or a request rejected by a healthy engine.
        if (adoptedExternal || process == null || process.isAlive()) return false;
        context.cancellation().throwIfCancellationRequested();
        containment.close();
        containment = ManagedProcessContainment.NoopProcessContainment.INSTANCE;
        process = null;
        deletePidFile();
        context.progress().report("RECOVERING", 0.0,
                "El motor de imagen se cerró. Reiniciando y reintentando esta imagen una vez.");
        start(assets, profile, context);
        return true;
    }

    synchronized void stop(ExecutionContext context) throws InterruptedException {
        if (adoptedExternal) {
            context.progress().report("STOPPED", 1.0,
                    "El servidor ComfyUI era externo y no fue detenido por la aplicación.");
            adoptedExternal = false;
            return;
        }
        Process current = process;
        if (current == null || !current.isAlive()) return;
        context.progress().report("STOPPING", 0.4, "Deteniendo el runtime local compartido.");
        List<ProcessHandle> descendants = current.descendants().toList();
        descendants.reversed().forEach(ProcessHandle::destroy);
        current.destroy();
        if (!current.waitFor(10, TimeUnit.SECONDS)) {
            descendants.reversed().forEach(ProcessHandle::destroyForcibly);
            current.destroyForcibly();
        }
        containment.close();
        containment = ManagedProcessContainment.NoopProcessContainment.INSTANCE;
        process = null;
        OwnedProcessDiagnostics.stopped("COMFYUI", current, "ComfyUiManagedProcess",
                processStartedAt, "RUNTIME_STOP", current.isAlive() ? null : current.exitValue(), false);
        processStartedAt = null;
        deletePidFile();
    }

    synchronized boolean reloadAfterManagedCatalogChange(ExecutionContext context)
            throws InterruptedException {
        if (adoptedExternal || process == null || !process.isAlive()) return false;
        context.progress().report("RELOADING", 0.9,
                "El catálogo cambió; se reiniciará el ComfyUI administrado en la siguiente operación.");
        stop(context);
        return true;
    }

    private void awaitReady(ComfyUiTransport transport, Path log, ComfyUiLaunchProfile profile,
                            ExecutionContext context) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + startTimeout.toNanos();
        while (System.nanoTime() < deadline) {
            context.cancellation().throwIfCancellationRequested();
            Process current = process;
            if (current == null || !current.isAlive()) {
                int exit = current == null ? -1 : current.exitValue();
                process = null;
                deletePidFile();
                throw new IOException("ComfyUI terminó antes de estar listo (código " + exit + "). "
                        + tail(log, 24));
            }
            if (isReady(transport)) {
                try {
                    verifyBinding(transport, profile);
                } catch (IOException mismatch) {
                    stopManagedProcess();
                    throw mismatch;
                }
                context.progress().report("READY", 1.0,
                        "ComfyUI está listo para recibir trabajos. " + deviceSummary(profile));
                return;
            }
            context.progress().report("STARTING", 0.6, "Esperando que ComfyUI cargue nodos y modelos.");
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        stopManagedProcess();
        throw new IOException("ComfyUI no respondió en " + startTimeout.toSeconds()
                + " segundos. " + tail(log, 24));
    }

    private static void verifyBinding(ComfyUiTransport transport, ComfyUiLaunchProfile profile)
            throws IOException, InterruptedException {
        String failure = profile.verificationFailure(transport.systemStats());
        if (!failure.isBlank()) {
            throw new IOException("ComfyUI respondió, pero no está usando el dispositivo configurado. " + failure);
        }
    }

    private static boolean isReady(ComfyUiTransport transport) throws InterruptedException {
        try {
            return transport.isReady();
        } catch (IOException unavailable) {
            return false;
        }
    }

    private static String deviceSummary(ComfyUiLaunchProfile profile) {
        return "dispositivo=" + profile.displayName()
                + ", backend=" + profile.backend()
                + ", GPU=" + (profile.gpu() ? "sí" : "no") + ".";
    }

    private void stopManagedProcess() {
        Process current = process;
        if (current != null) {
            current.descendants().toList().reversed().forEach(ProcessHandle::destroyForcibly);
            current.destroyForcibly();
        }
        containment.close();
        containment = ManagedProcessContainment.NoopProcessContainment.INSTANCE;
        process = null;
        OwnedProcessDiagnostics.stopped("COMFYUI", current, "ComfyUiManagedProcess",
                processStartedAt, "START_FAILURE", current == null || current.isAlive()
                        ? null : current.exitValue(), true);
        processStartedAt = null;
        deletePidFile();
    }

    @Override
    public void close() {
        try {
            stop(ExecutionContext.defaults("application-shutdown-comfyui"));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            stopManagedProcess();
        }
    }

    private static String tail(Path log, int maximumLines) {
        if (!Files.isRegularFile(log)) return "No se creó un registro de arranque.";
        try {
            List<String> lines = Files.readAllLines(log);
            int start = Math.max(0, lines.size() - maximumLines);
            return "Últimas líneas del registro: " + String.join(" | ", lines.subList(start, lines.size()));
        } catch (IOException ignored) {
            return "No se pudo leer el registro de arranque.";
        }
    }

    private void deletePidFile() {
        Path current = pidFile;
        pidFile = null;
        if (current == null) return;
        try {
            Files.deleteIfExists(current);
        } catch (IOException ignored) {
            // Stale PID files are diagnostic only and never justify hiding a successful stop.
        }
    }
}
