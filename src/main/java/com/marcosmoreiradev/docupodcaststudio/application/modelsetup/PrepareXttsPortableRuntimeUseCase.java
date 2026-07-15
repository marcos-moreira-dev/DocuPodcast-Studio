package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the local advanced AI voice preparation from inside the application, on demand.
 *
 * <p>This use case intentionally does not run at startup. It is triggered from Settings by an explicit
 * user action, prepares the portable Python/runtime under {@code tools/}, and then re-inspects readiness.
 * The bundled script remains a technical adapter, not the normal UX.</p>
 */
public final class PrepareXttsPortableRuntimeUseCase {
    private static final long TIMEOUT_MINUTES = 45;

    private final InspectXttsSetupReadinessUseCase inspector;
    private final ExternalProcessRunner runner;

    public PrepareXttsPortableRuntimeUseCase() {
        this(new InspectXttsSetupReadinessUseCase(),
                ExternalProcessRunner.unavailable("PrepareXttsPortableRuntimeUseCase"));
    }

    public PrepareXttsPortableRuntimeUseCase(InspectXttsSetupReadinessUseCase inspector) {
        this(inspector, ExternalProcessRunner.unavailable("PrepareXttsPortableRuntimeUseCase"));
    }

    public PrepareXttsPortableRuntimeUseCase(InspectXttsSetupReadinessUseCase inspector, ExternalProcessRunner runner) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.runner = runner == null ? ExternalProcessRunner.unavailable("PrepareXttsPortableRuntimeUseCase") : runner;
    }

    public XttsRuntimePreparationReport prepare(OperationalSettings settings, Path applicationRoot) {
        return prepare(settings, applicationRoot, "", ModelSetupProgressListener.noop());
    }

    public XttsRuntimePreparationReport prepare(OperationalSettings settings, Path applicationRoot, String localTtsRepository) {
        return prepare(settings, applicationRoot, localTtsRepository, ModelSetupProgressListener.noop());
    }

    public XttsRuntimePreparationReport prepare(OperationalSettings settings, Path applicationRoot,
                                                ModelSetupProgressListener progressListener) {
        return prepare(settings, applicationRoot, "", progressListener);
    }

    public XttsRuntimePreparationReport prepare(OperationalSettings settings, Path applicationRoot,
                                                String localTtsRepository,
                                                ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        progress.onProgress("Verificando runtime local de Voz IA avanzada...");
        XttsSetupReadinessReport before = inspector.inspect(current, root);
        Path script = before.setupScript();
        Path reportPath = XttsRuntimePreparationReport.defaultReportPath(root);
        if (before.runtimeReady()) {
            progress.onProgress("Runtime local ya preparado; no se ejecuta instalación.");
            return XttsRuntimePreparationReport.skippedReady(before);
        }
        if (script == null || !Files.isRegularFile(script)) {
            return XttsRuntimePreparationReport.failed(script, reportPath, before,
                    "No se encontró el instalador interno de Voz IA avanzada. Reinstala o repara la aplicación.", List.of());
        }

        List<String> command = commandFor(script, localTtsRepository);
        try {
            progress.onProgress("Iniciando instalador interno: " + script.getFileName() + ".");
            ArrayList<String> lines = new ArrayList<>();
            ExternalProcessRequest request = ExternalProcessRequest.of(command, "xtts-portable-runtime-setup",
                            Duration.ofMinutes(TIMEOUT_MINUTES))
                    .withWorkingDirectory(root)
                    .redirectingErrorStream();
            ExternalProcessResult result = runner.run(request, outputObserver(lines, progress));
            if (lines.isEmpty()) {
                lines.addAll(linesFrom(result.combinedOutputTail()));
            }
            if (result.timedOut()) {
                return XttsRuntimePreparationReport.failed(script, reportPath, before,
                        "La preparación de Voz IA avanzada tardó demasiado y fue detenida. Revisa tu conexión y vuelve a intentar.",
                        List.copyOf(lines));
            }
            progress.onProgress("Instalador interno finalizado; leyendo salida y verificando resultado...");
            XttsSetupReadinessReport after = inspector.inspect(current, root);
            boolean success = result.exitCode() == 0 && after.runtimeReady();
            String message = success
                    ? "Runtime local de Voz IA avanzada preparado desde Configuración. Ahora verifica o importa el modelo si todavía falta."
                    : "No se pudo completar la preparación automática de Voz IA avanzada. Revisa el reporte y el detalle técnico.";
            progress.onProgress(message);
            return new XttsRuntimePreparationReport(success, result.exitCode(), script, reportPath, after, List.copyOf(lines), message);
        } catch (IOException ex) {
            return XttsRuntimePreparationReport.failed(script, reportPath, before,
                    "No se pudo iniciar la preparación automática: " + ex.getMessage(), List.of());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return XttsRuntimePreparationReport.failed(script, reportPath, before,
                    "La preparación automática fue interrumpida antes de terminar.", List.of(ex.getMessage()));
        }
    }

    private static List<String> commandFor(Path script, String localTtsRepository) {
        ArrayList<String> command = new ArrayList<>();
        command.add(resolvePowerShellCommand());
        command.add("-NoProfile");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(script.toString());
        String repo = localTtsRepository == null ? "" : localTtsRepository.strip();
        if (!repo.isBlank()) {
            command.add("-LocalTtsRepo");
            command.add(repo);
        }
        return List.copyOf(command);
    }

    private static String resolvePowerShellCommand() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) {
            return "powershell.exe";
        }
        return "pwsh";
    }

    private static ExternalProcessObserver outputObserver(ArrayList<String> lines, ModelSetupProgressListener progress) {
        return new ExternalProcessObserver() {
            @Override
            public void onOutputLine(String line) {
                String normalized = line == null ? "" : line.strip();
                if (normalized.isBlank()) {
                    return;
                }
                lines.add(normalized);
                progress.onProgress(normalized);
                trim(lines);
            }
        };
    }

    private static void trim(ArrayList<String> lines) {
        while (lines.size() > 120) {
            lines.remove(0);
        }
    }

    private static List<String> linesFrom(String output) {
        String text = output == null ? "" : output.strip();
        if (text.isBlank()) {
            return List.of();
        }
        return text.lines().map(String::strip).filter(line -> !line.isBlank()).toList();
    }
}
