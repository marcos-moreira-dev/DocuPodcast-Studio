package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ModelResidencyKey;
import com.marcosmoreiradev.docupodcaststudio.media.api.OperationDeadline;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** One private loopback Ollama process shared by all local multimodal analysis operations. */
final class ManagedOllamaProcess implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger(
            "docupodcast.runtime.ollama");
    private static final Duration READY_TIMEOUT = Duration.ofMinutes(3);
    private static final int PORT_ATTEMPTS = 3;
    private static final String MODEL_PREFERENCE_FILE = "selected-visual-model.txt";
    private static final String PID_FILE = "visual-analysis.pid";

    private final Path executable;
    private final Path modelsRoot;
    private final Path logsRoot;
    private final PortAllocator portAllocator;
    private final ProcessLauncher processLauncher;
    private final EndpointClient endpointClient;
    private final LongSupplier nanoClock;
    private final int runtimeParallel;
    private final RuntimeOwnership ownership;
    private final String kvCacheType;
    private final boolean flashAttention;
    private final OllamaModelLifecycle lifecycle = new OllamaModelLifecycle();
    private volatile Process process;
    private volatile Instant processStartedAt;
    private volatile int port;
    private volatile ManagedProcessContainment containment =
            ManagedProcessContainment.NoopProcessContainment.INSTANCE;
    private volatile ComputePreference activePreference = ComputePreference.automatic();

    ManagedOllamaProcess(Path executable, Path modelsRoot, Path logsRoot) {
        this(executable, modelsRoot, logsRoot, ManagedOllamaProcess::freePort,
                ProcessBuilder::start, new JavaHttpEndpointClient(), System::nanoTime,
                RuntimeOwnership.MANAGED_PRIVATE_RUNTIME, "q8_0", true);
    }

    ManagedOllamaProcess(Path executable, Path modelsRoot, Path logsRoot,
                         String kvCacheType, boolean flashAttention) {
        this(executable, modelsRoot, logsRoot, ManagedOllamaProcess::freePort,
                ProcessBuilder::start, new JavaHttpEndpointClient(), System::nanoTime,
                RuntimeOwnership.MANAGED_PRIVATE_RUNTIME, kvCacheType, flashAttention);
    }

    ManagedOllamaProcess(Path executable, Path modelsRoot, Path logsRoot,
                         PortAllocator portAllocator, ProcessLauncher processLauncher,
                         EndpointClient endpointClient, LongSupplier nanoClock) {
        this(executable, modelsRoot, logsRoot, portAllocator, processLauncher,
                endpointClient, nanoClock, RuntimeOwnership.MANAGED_PRIVATE_RUNTIME,
                "q8_0", true);
    }

    ManagedOllamaProcess(Path executable, Path modelsRoot, Path logsRoot,
                         PortAllocator portAllocator, ProcessLauncher processLauncher,
                         EndpointClient endpointClient, LongSupplier nanoClock,
                         RuntimeOwnership ownership) {
        this(executable, modelsRoot, logsRoot, portAllocator, processLauncher,
                endpointClient, nanoClock, ownership, "q8_0", true);
    }

    private ManagedOllamaProcess(Path executable, Path modelsRoot, Path logsRoot,
                         PortAllocator portAllocator, ProcessLauncher processLauncher,
                         EndpointClient endpointClient, LongSupplier nanoClock,
                         RuntimeOwnership ownership, String kvCacheType,
                         boolean flashAttention) {
        this.executable = executable.toAbsolutePath().normalize();
        this.modelsRoot = modelsRoot.toAbsolutePath().normalize();
        this.logsRoot = logsRoot.toAbsolutePath().normalize();
        this.portAllocator = java.util.Objects.requireNonNull(portAllocator);
        this.processLauncher = java.util.Objects.requireNonNull(processLauncher);
        this.endpointClient = java.util.Objects.requireNonNull(endpointClient);
        this.nanoClock = java.util.Objects.requireNonNull(nanoClock);
        this.ownership = java.util.Objects.requireNonNullElse(
                ownership, RuntimeOwnership.MANAGED_PRIVATE_RUNTIME);
        this.runtimeParallel = 1;
        this.kvCacheType = java.util.Objects.requireNonNullElse(
                kvCacheType, "q8_0").strip().toLowerCase(Locale.ROOT);
        if (this.kvCacheType.isBlank()) {
            throw new IllegalArgumentException("kvCacheType is required");
        }
        this.flashAttention = flashAttention;
    }

    synchronized URI ensureReady(ExecutionContext context) throws IOException, InterruptedException {
        ExecutionContext execution = context == null
                ? ExecutionContext.defaults("ollama-start") : context;
        ComputePreference preference = execution.computePreference();
        if (process != null && process.isAlive()
                && activePreference.equals(preference)
                && lifecycle.snapshot().activePreference() != null) {
            LOG.log(System.Logger.Level.DEBUG,
                    "ollama.readiness strategy=KNOWN_READY pid={0} generation={1} remainingMs={2} ownership={3}",
                    process.pid(), lifecycle.snapshot().runtimeGeneration(),
                    execution.deadline().remainingMillis(), ownership);
            return endpoint();
        }
        if (process != null && process.isAlive()
                && lifecycle.snapshot().activeRequests() > 0) {
            return endpoint();
        }
        if (process != null && process.isAlive()) stopRuntime();
        if (!Files.isRegularFile(executable)) {
            throw failure(EngineDiagnosticCode.MISSING_RESOURCE,
                    "La descripción visual local necesita preparación.",
                    Map.of("executable", executable.toString()));
        }
        Files.createDirectories(modelsRoot);
        Files.createDirectories(logsRoot);
        reconcileStalePid();

        for (int attempt = 1; attempt <= PORT_ATTEMPTS; attempt++) {
            port = portAllocator.allocate();
            ProcessBuilder builder = processBuilder(preference, port);
            process = processLauncher.start(builder);
            containment = ownership.mayTerminate()
                    ? ManagedProcessContainment.create()
                    : ManagedProcessContainment.NoopProcessContainment.INSTANCE;
            if (ownership.mayTerminate()) containment.attach(process);
            processStartedAt = OwnedProcessDiagnostics.started("OLLAMA", process,
                    List.of(executable.toString(), "serve"), "ManagedOllamaProcess",
                    "application shutdown or explicit runtime stop", containment.diagnostics());
            activePreference = preference;
            lifecycle.runtimeStarted(preference);
            LOG.log(System.Logger.Level.INFO,
                    "ollama.readiness strategy=COLD_START pid={0} generation={1} attempt={2} remainingMs={3} ownership={4}",
                    process.pid(), lifecycle.snapshot().runtimeGeneration(), attempt,
                    execution.deadline().remainingMillis(), ownership);
            writePidRecord(process, port);
            long deadline = nanoClock.getAsLong() + READY_TIMEOUT.toNanos();
            boolean retryPort = false;
            while (nanoClock.getAsLong() < deadline) {
                execution.cancellation().throwIfCancellationRequested();
                if (!process.isAlive()) {
                    int exit = process.exitValue();
                    String details = tail();
                    retryPort = looksLikePortConflict(details) && attempt < PORT_ATTEMPTS;
                    cleanupStoppedProcess();
                    if (retryPort) break;
                    throw failure(looksLikePortConflict(details)
                                    ? EngineDiagnosticCode.PORT_CONFLICT : EngineDiagnosticCode.CHILD_EXIT,
                            "El análisis visual local terminó durante el arranque.",
                            Map.of("exitCode", Integer.toString(exit),
                                    "attempt", Integer.toString(attempt),
                                    "details", details));
                }
                Duration readinessTimeout = remaining(execution,
                        Duration.ofSeconds(3), EngineDiagnosticCode.READINESS_TIMEOUT,
                        "Se agotó el deadline durante readiness de Ollama.");
                if (endpointClient.ready(endpoint(), readinessTimeout,
                        execution.cancellation())) {
                    OwnedProcessDiagnostics.resident("OLLAMA", process,
                            "ManagedOllamaProcess", "MODEL_RUNTIME");
                    execution.progress().report("READY", 1.0,
                            "Descripción visual local lista.");
                    return endpoint();
                }
                execution.progress().report("STARTING", 0.45,
                        "Preparando el análisis visual local.");
                Thread.sleep(remaining(execution, Duration.ofMillis(350),
                        EngineDiagnosticCode.READINESS_TIMEOUT,
                        "Se agotó el deadline durante el polling de readiness."));
            }
            if (retryPort) continue;
            if (process != null && process.isAlive()) {
                String details = tail();
                stopRuntime();
                throw failure(EngineDiagnosticCode.START_TIMEOUT,
                        "El análisis visual local no estuvo listo a tiempo.",
                        Map.of("attempt", Integer.toString(attempt), "details", details));
            }
        }
        throw failure(EngineDiagnosticCode.PORT_CONFLICT,
                "No se pudo reservar un puerto privado tras tres intentos.",
                Map.of("attempts", Integer.toString(PORT_ATTEMPTS)));
    }

    ModelRequest openModelRequest(ModelResidencyKey model, ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext execution = context == null
                ? ExecutionContext.defaults("ollama-model-request") : context;
        while (true) {
            requireRemaining(execution, EngineDiagnosticCode.READINESS_TIMEOUT,
                    "Se agotó el deadline antes de abrir el modelo visual.");
            URI service = ensureReady(execution);
            OllamaModelLifecycle.BeginResult result = lifecycle.tryBeginRequest(
                    execution.operationId(), model, execution.computePreference());
            switch (result.status()) {
                case ACQUIRED -> {
                    return new ModelRequest(service, result.request(), execution);
                }
                case RESTART_REQUIRED -> {
                    synchronized (this) {
                        if (lifecycle.snapshot().activeRequests() == 0) stopRuntime();
                    }
                }
                case WAIT -> {
                    execution.cancellation().throwIfCancellationRequested();
                    lifecycle.awaitStateChange(50L);
                }
                case SHUTTING_DOWN -> throw failure(
                        EngineDiagnosticCode.CANCELLED,
                        "El runtime visual se está cerrando.",
                        Map.of("operationId", execution.operationId()));
            }
        }
    }

    void retainModelResidency(String ownerId, ModelResidencyKey model) {
        lifecycle.retainResidency(ownerId, model);
    }

    void releaseModelResidency(String ownerId, ExecutionContext context)
            throws IOException, InterruptedException {
        releaseModelResidency(ownerId, true, context);
    }

    void releaseModelResidency(String ownerId, boolean unloadWhenIdle,
                               ExecutionContext context)
            throws IOException, InterruptedException {
        executeLifecycleAction(lifecycle.releaseResidency(
                ownerId, unloadWhenIdle), context);
    }

    OllamaModelLifecycle.Snapshot lifecycleSnapshot() {
        return lifecycle.snapshot();
    }

    int runtimeParallel() {
        return runtimeParallel;
    }

    synchronized boolean modelAvailable(String model) {
        if (model == null || model.isBlank()) return false;
        return Files.isRegularFile(OllamaModelStoreInspector.manifest(modelsRoot, model));
    }

    synchronized void pull(String model, boolean replace, ExecutionContext context)
            throws IOException, InterruptedException {
        if (modelAvailable(model) && !replace) {
            context.progress().report("REUSING", 1.0,
                    "El modelo visual instalado se reutiliza sin red.");
            return;
        }
        URI service = ensureReady(context);
        String body = "{\"name\":" + OllamaJson.quote(model) + ",\"stream\":false}";
        EndpointResponse response = endpointClient.post(service.resolve("/api/pull"), body,
                Duration.ofHours(4));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw failure(EngineDiagnosticCode.INVALID_OUTPUT,
                    "La preparación del modelo respondió HTTP " + response.statusCode() + ".",
                    Map.of("response", response.body()));
        }
        if (!modelAvailable(model)) {
            throw failure(EngineDiagnosticCode.INVALID_RESOURCE,
                    "La descarga terminó, pero el catálogo local no enumera " + model + ".",
                    Map.of("model", model));
        }
    }

    synchronized Map<String, String> runtimeDiagnostics(String model)
            throws InterruptedException {
        return runtimeDiagnostics(model,
                ExecutionContext.defaults("ollama-runtime-diagnostics"));
    }

    synchronized Map<String, String> runtimeDiagnostics(
            String model, ExecutionContext context) throws InterruptedException {
        LinkedHashMap<String, String> diagnostics = new LinkedHashMap<>();
        diagnostics.put("pid", process == null ? "" : Long.toString(process.pid()));
        diagnostics.put("port", port <= 0 ? "" : Integer.toString(port));
        diagnostics.put("computeMode", activePreference.mode().name());
        diagnostics.put("deviceId", activePreference.deviceId());
        diagnostics.put("ramOffloadAllowed",
                Boolean.toString(activePreference.allowHostMemory()));
        diagnostics.put("runtimeParallelConfigured",
                Integer.toString(runtimeParallel));
        diagnostics.put("containment", containment.diagnostics());
        if (port > 0) {
            try {
                Duration timeout = context.deadline().remainingOr(
                        Duration.ofSeconds(5));
                if (timeout.isZero()) {
                    diagnostics.put("runtimePs", "deadline-expired");
                    return Map.copyOf(diagnostics);
                }
                EndpointResponse response = endpointClient.get(endpoint().resolve("/api/ps"),
                        timeout);
                diagnostics.put("runtimePs", response.body());
                diagnostics.put("modelLoaded",
                        Boolean.toString(response.body().contains(model)));
            } catch (IOException ignored) {
                diagnostics.put("runtimePs", "unavailable");
            }
        }
        return Map.copyOf(diagnostics);
    }

    /**
     * Requests immediate model eviction and waits until the runtime confirms it.
     * The compute lease must remain held while this method runs.
     */
    private synchronized void performUnload(String model, ExecutionContext context)
            throws IOException, InterruptedException {
        if (model == null || model.isBlank() || port <= 0
                || process == null || !process.isAlive()) {
            return;
        }
        ExecutionContext execution = context == null
                ? ExecutionContext.defaults("ollama-unload-model") : context;
        Duration unloadTimeout = execution.deadline().remainingOr(
                Duration.ofMinutes(2));
        if (unloadTimeout.isZero()) {
            if (ownership.mayTerminate()) {
                stopRuntime();
                lifecycle.runtimeDied();
                execution.resourceLease().confirmModelUnloaded();
                return;
            }
            throw failure(EngineDiagnosticCode.REQUEST_TIMEOUT,
                    "El deadline terminó antes de liberar el runtime externo.",
                    Map.of("ownership", ownership.name()));
        }
        String body = "{\"model\":" + OllamaJson.quote(model)
                + ",\"keep_alive\":0}";
        EndpointResponse response = endpointClient.post(
                endpoint().resolve("/api/generate"), body, unloadTimeout);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw failure(EngineDiagnosticCode.INVALID_OUTPUT,
                    "No se pudo descargar el modelo visual de la memoria.",
                    Map.of("model", model,
                            "statusCode", Integer.toString(response.statusCode()),
                            "response", response.body()));
        }
        long deadline = nanoClock.getAsLong() + execution.deadline()
                .remainingOr(Duration.ofSeconds(45)).toNanos();
        while (nanoClock.getAsLong() < deadline) {
            execution.cancellation().throwIfCancellationRequested();
            Duration pollTimeout = remaining(execution, Duration.ofSeconds(5),
                    EngineDiagnosticCode.REQUEST_TIMEOUT,
                    "El deadline terminó durante la liberación del modelo visual.");
            EndpointResponse loaded = endpointClient.get(
                    endpoint().resolve("/api/ps"), pollTimeout);
            if (loaded.statusCode() / 100 == 2
                    && !loaded.body().contains(model)) {
                execution.progress().report("MODEL_RELEASED", 1.0,
                        "Memoria del modelo visual liberada.");
                return;
            }
            Thread.sleep(remaining(execution, Duration.ofMillis(200),
                    EngineDiagnosticCode.REQUEST_TIMEOUT,
                    "El deadline terminó durante la liberación del modelo visual."));
        }
        throw failure(EngineDiagnosticCode.REQUEST_TIMEOUT,
                "Ollama no confirmó la liberación del modelo visual.",
                Map.of("model", model, "runtimePs", "deadline-expired"));
    }

    synchronized void stop() throws InterruptedException {
        stopRuntime();
        lifecycle.runtimeDied();
    }

    synchronized void recoverUnresponsive(ExecutionContext context, String reason)
            throws InterruptedException {
        long oldPid = process == null ? -1L : process.pid();
        long generation = lifecycle.snapshot().runtimeGeneration();
        LOG.log(System.Logger.Level.WARNING,
                "ollama.recovery reason={0} pid={1} generation={2} remainingMs={3} ownership={4}",
                java.util.Objects.toString(reason, ""), oldPid, generation,
                context == null ? -1L : context.deadline().remainingMillis(), ownership);
        if (!ownership.mayTerminate()) {
            lifecycle.runtimeDied();
            return;
        }
        stopRuntime();
        lifecycle.runtimeDied();
    }

    RuntimeOwnership ownership() { return ownership; }

    synchronized Map<String, String> runtimeIdentityDiagnostics() {
        return Map.of(
                "runtimePid", process == null ? "" : Long.toString(process.pid()),
                "runtimeGeneration", Long.toString(
                        lifecycle.snapshot().runtimeGeneration()),
                "runtimeOwnership", ownership.name());
    }

    private synchronized void stopRuntime() throws InterruptedException {
        Process current = process;
        Instant started = processStartedAt;
        process = null;
        processStartedAt = null;
        containment.close();
        containment = ManagedProcessContainment.NoopProcessContainment.INSTANCE;
        if (ownership.mayTerminate()) terminateTree(current);
        OwnedProcessDiagnostics.stopped("OLLAMA", current, "ManagedOllamaProcess",
                started, "RUNTIME_STOP", current == null || current.isAlive()
                        ? null : current.exitValue(), false);
        deletePidFile();
        port = 0;
    }

    URI endpoint() {
        if (port <= 0) throw new IllegalStateException("Ollama has not been started");
        return URI.create("http://127.0.0.1:" + port);
    }

    Path modelsRoot() {
        return modelsRoot;
    }

    synchronized String preferredModel() {
        Path preference = modelsRoot.resolve(MODEL_PREFERENCE_FILE);
        if (!Files.isRegularFile(preference)) return "";
        try {
            return Files.readString(preference, StandardCharsets.UTF_8).strip();
        } catch (IOException ignored) {
            return "";
        }
    }

    synchronized void rememberPreferredModel(String model) throws IOException {
        if (model == null || model.isBlank()) return;
        Files.createDirectories(modelsRoot);
        Path preference = modelsRoot.resolve(MODEL_PREFERENCE_FILE);
        Path temporary = modelsRoot.resolve(MODEL_PREFERENCE_FILE + ".tmp");
        Files.writeString(temporary, model.strip() + System.lineSeparator(),
                StandardCharsets.UTF_8);
        try {
            Files.move(temporary, preference, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, preference, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void close() {
        try {
            lifecycle.beginShutdown();
            stopRuntime();
            lifecycle.runtimeDied();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeLifecycleAction(OllamaModelLifecycle.Action action,
                                        ExecutionContext context)
            throws IOException, InterruptedException {
        if (action == null
                || action.type() == OllamaModelLifecycle.ActionType.NONE) return;
        boolean succeeded = false;
        try {
            performUnload(action.model().modelIdentity(), cleanupContext(context));
            succeeded = true;
            if (context != null) context.resourceLease().confirmModelUnloaded();
        } finally {
            lifecycle.completeUnload(action.model(), succeeded);
        }
    }

    private static ExecutionContext cleanupContext(ExecutionContext context) {
        ExecutionContext current = context == null
                ? ExecutionContext.defaults("ollama-deferred-unload") : context;
        return new ExecutionContext(current.operationId() + ":cleanup",
                CancellationToken.NONE, current.progress(), current.policy(),
                current.resourceLease(), current.staging(),
                current.computePreference());
    }

    final class ModelRequest implements AutoCloseable {
        private final URI endpoint;
        private final OllamaModelLifecycle.RequestSnapshot snapshot;
        private final ExecutionContext context;
        private boolean unloadWhenIdle;
        private boolean closed;

        private ModelRequest(URI endpoint,
                             OllamaModelLifecycle.RequestSnapshot snapshot,
                             ExecutionContext context) {
            this.endpoint = endpoint;
            this.snapshot = snapshot;
            this.context = context;
        }

        URI endpoint() { return endpoint; }

        ComputePreference preference() { return snapshot.preference(); }

        ModelResidencyKey model() { return snapshot.model(); }

        void confirmModelLoaded() {
            lifecycle.confirmModelLoaded(snapshot.model());
            context.resourceLease().confirmModelResident();
        }

        void unloadWhenIdle() { unloadWhenIdle = true; }

        void assertRuntimeAlive() throws EngineExecutionException {
            if (lifecycle.requestRuntimeValid(snapshot)
                    && process != null && process.isAlive()) return;
            lifecycle.runtimeDied();
            context.resourceLease().confirmModelUnloaded();
            throw failure(EngineDiagnosticCode.CHILD_EXIT,
                    "El runtime visual terminó durante la inferencia.",
                    Map.of("requestId", snapshot.requestId(),
                            "model", snapshot.model().modelIdentity(),
                            "failureScope", "RUNTIME_FAILURE"));
        }

        @Override public void close() throws IOException, InterruptedException {
            if (closed) return;
            closed = true;
            executeLifecycleAction(lifecycle.finishRequest(
                    snapshot, unloadWhenIdle), context);
        }
    }

    private ProcessBuilder processBuilder(ComputePreference preference, int selectedPort) {
        ProcessBuilder builder = new ProcessBuilder(executable.toString(), "serve")
                .directory(executable.getParent().toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(
                        logsRoot.resolve("visual-analysis.log").toFile()));
        Map<String, String> environment = builder.environment();
        environment.put("OLLAMA_HOST", "127.0.0.1:" + selectedPort);
        environment.put("OLLAMA_MODELS", modelsRoot.toString());
        environment.put("OLLAMA_ORIGINS", "http://127.0.0.1");
        environment.put("OLLAMA_NO_CLOUD", "1");
        // Conservative, measured profile for the private visual runtime. The
        // request still chooses its own context; these settings do not become
        // a universal policy for other engines or hardware.
        environment.put("OLLAMA_FLASH_ATTENTION", Boolean.toString(flashAttention));
        environment.put("OLLAMA_KV_CACHE_TYPE", kvCacheType);
        environment.put("OLLAMA_NUM_PARALLEL", Integer.toString(runtimeParallel));
        environment.put("OLLAMA_MAX_LOADED_MODELS", "1");
        if (preference.mode() == ComputePreference.Mode.CPU_ONLY) {
            environment.put("CUDA_VISIBLE_DEVICES", "-1");
            environment.put("GGML_CUDA_VISIBLE_DEVICES", "-1");
        } else if (preference.mode() == ComputePreference.Mode.SPECIFIC_DEVICE) {
            String index = trailingNumber(preference.deviceId());
            if (!index.isBlank()) {
                environment.put("CUDA_VISIBLE_DEVICES", index);
                environment.put("GGML_CUDA_VISIBLE_DEVICES", index);
            }
        }
        return builder;
    }

    private void reconcileStalePid() {
        Path pidFile = logsRoot.resolve(PID_FILE);
        if (!Files.isRegularFile(pidFile)) return;
        try {
            Properties properties = new Properties();
            try (var input = Files.newInputStream(pidFile)) {
                properties.load(input);
            }
            long pid = Long.parseLong(properties.getProperty("pid", "-1"));
            ProcessHandle.of(pid).ifPresent(handle -> {
                String command = handle.info().command().orElse("");
                if (!samePath(command, executable.toString())) {
                    // Never terminate a process merely because a stale PID was reused.
                }
            });
        } catch (RuntimeException | IOException ignored) {
            // Legacy/plain or corrupt PID records are diagnostic only.
        }
        deletePidFile();
    }

    private void writePidRecord(Process child, int selectedPort) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("pid", Long.toString(child.pid()));
        properties.setProperty("port", Integer.toString(selectedPort));
        properties.setProperty("command", executable.toString());
        properties.setProperty("startedAt", child.info().startInstant()
                .orElseGet(Instant::now).toString());
        properties.setProperty("ownerPid", Long.toString(ProcessHandle.current().pid()));
        Path target = logsRoot.resolve(PID_FILE);
        Path temporary = logsRoot.resolve(PID_FILE + ".tmp");
        try (var output = Files.newOutputStream(temporary)) {
            properties.store(output, "DocuPodcast managed Ollama");
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void cleanupStoppedProcess() {
        Process stopped = process;
        Instant started = processStartedAt;
        containment.close();
        containment = ManagedProcessContainment.NoopProcessContainment.INSTANCE;
        process = null;
        processStartedAt = null;
        OwnedProcessDiagnostics.stopped("OLLAMA", stopped, "ManagedOllamaProcess",
                started, "CHILD_EXIT", stopped == null || stopped.isAlive()
                        ? null : stopped.exitValue(), false);
        deletePidFile();
        port = 0;
    }

    private static void terminateTree(Process current) throws InterruptedException {
        if (current == null || !current.isAlive()) return;
        List<ProcessHandle> descendants = current.descendants().toList();
        descendants.reversed().forEach(ProcessHandle::destroy);
        current.destroy();
        if (!current.waitFor(10, TimeUnit.SECONDS)) {
            descendants.reversed().forEach(ProcessHandle::destroyForcibly);
            current.destroyForcibly();
        }
    }

    private void deletePidFile() {
        try {
            Files.deleteIfExists(logsRoot.resolve(PID_FILE));
        } catch (IOException ignored) {
            // A stale diagnostic file must not block shutdown.
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(
                0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private String tail() {
        Path log = logsRoot.resolve("visual-analysis.log");
        if (!Files.isRegularFile(log)) return "No se creó un registro de arranque.";
        try {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            return String.join(" | ", lines.subList(Math.max(0, lines.size() - 20), lines.size()));
        } catch (IOException ignored) {
            return "No se pudo leer el registro técnico.";
        }
    }

    private static boolean looksLikePortConflict(String details) {
        String normalized = details == null ? "" : details.toLowerCase(Locale.ROOT);
        return normalized.contains("address already in use")
                || normalized.contains("bind:")
                || normalized.contains("only one usage of each socket");
    }

    private static String trailingNumber(String value) {
        if (value == null) return "";
        var matcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(value.strip());
        return matcher.find() ? matcher.group(1) : "";
    }

    private static boolean samePath(String left, String right) {
        if (left == null || left.isBlank() || right == null || right.isBlank()) return false;
        try {
            return Path.of(left).toAbsolutePath().normalize()
                    .equals(Path.of(right).toAbsolutePath().normalize());
        } catch (RuntimeException ignored) {
            return left.equalsIgnoreCase(right);
        }
    }

    private static EngineExecutionException failure(
            EngineDiagnosticCode code, String message, Map<String, String> diagnostics) {
        return new EngineExecutionException(code, message, diagnostics);
    }

    @FunctionalInterface
    interface PortAllocator {
        int allocate() throws IOException;
    }

    @FunctionalInterface
    interface ProcessLauncher {
        Process start(ProcessBuilder builder) throws IOException;
    }

    interface EndpointClient {
        boolean ready(URI endpoint) throws InterruptedException;
        default boolean ready(URI endpoint, Duration timeout,
                              CancellationToken cancellation)
                throws InterruptedException {
            cancellation.throwIfCancellationRequested();
            return ready(endpoint);
        }
        EndpointResponse post(URI endpoint, String body, Duration timeout)
                throws IOException, InterruptedException;
        EndpointResponse get(URI endpoint, Duration timeout)
                throws IOException, InterruptedException;
    }

    record EndpointResponse(int statusCode, String body) {
        EndpointResponse {
            body = body == null ? "" : body;
        }
    }

    static final class JavaHttpEndpointClient implements EndpointClient {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20)).build();

        @Override
        public boolean ready(URI endpoint) throws InterruptedException {
            return ready(endpoint, Duration.ofSeconds(3), CancellationToken.NONE);
        }

        @Override
        public boolean ready(URI endpoint, Duration timeout,
                             CancellationToken cancellation)
                throws InterruptedException {
            try {
                return sendCancellable(HttpRequest.newBuilder(
                                endpoint.resolve("/api/tags")).timeout(timeout).GET().build(),
                        timeout, cancellation).statusCode() / 100 == 2;
            } catch (IOException ignored) {
                return false;
            }
        }

        private EndpointResponse sendCancellable(HttpRequest request,
                                                 Duration timeout,
                                                 CancellationToken cancellation)
                throws IOException, InterruptedException {
            var future = client.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long deadline = System.nanoTime() + Math.max(1L, timeout.toNanos());
            try {
                while (true) {
                    cancellation.throwIfCancellationRequested();
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0L) {
                        future.cancel(true);
                        throw new java.net.http.HttpTimeoutException(
                                "Ollama readiness timed out");
                    }
                    try {
                        HttpResponse<String> response = future.get(
                                Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(100)),
                                TimeUnit.NANOSECONDS);
                        return new EndpointResponse(response.statusCode(), response.body());
                    } catch (java.util.concurrent.TimeoutException waiting) {
                        // Re-check cancellation and the shared deadline.
                    } catch (java.util.concurrent.ExecutionException failed) {
                        Throwable cause = failed.getCause();
                        if (cause instanceof IOException io) throw io;
                        throw new IOException("Ollama readiness failed", cause);
                    }
                }
            } finally {
                if (!future.isDone()) future.cancel(true);
            }
        }

        @Override
        public EndpointResponse post(URI endpoint, String body, Duration timeout)
                throws IOException, InterruptedException {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(endpoint).timeout(timeout)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    body, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new EndpointResponse(response.statusCode(), response.body());
        }

        @Override
        public EndpointResponse get(URI endpoint, Duration timeout)
                throws IOException, InterruptedException {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(endpoint).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new EndpointResponse(response.statusCode(), response.body());
        }
    }

    private static Duration remaining(ExecutionContext context, Duration nominal,
                                      EngineDiagnosticCode code, String message)
            throws EngineExecutionException {
        Duration value = context.deadline().remainingOr(nominal);
        if (!value.isZero()) return value;
        throw failure(code, message, Map.of(
                "remainingMs", "0", "phase", "READINESS"));
    }

    private static void requireRemaining(ExecutionContext context,
                                         EngineDiagnosticCode code,
                                         String message)
            throws EngineExecutionException {
        if (!context.deadline().expired()) return;
        throw failure(code, message, Map.of("remainingMs", "0"));
    }
}
