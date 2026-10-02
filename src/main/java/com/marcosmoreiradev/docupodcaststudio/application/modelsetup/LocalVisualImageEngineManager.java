package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDiscoveryGateway;
import com.marcosmoreiradev.docupodcaststudio.application.compute.EnvironmentComputeDeviceDiscoveryGateway;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiSystemStats;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiWorkflowSpec;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBackendResolver;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBinding;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBindingVerifier;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineResult;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRuntimeIdentity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Manages the category-neutral local visual generation runtime. */
public class LocalVisualImageEngineManager {
    private final InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase;
    private final ExternalProcessRunner processRunner;
    private final ComfyUiVisualEngineClient visualEngineClient;
    private final ComputeDeviceDiscoveryGateway computeDeviceDiscoveryGateway;
    private final VisualComputeBackendResolver computeBackendResolver = new VisualComputeBackendResolver();
    private final VisualComputeBindingVerifier computeBindingVerifier = new VisualComputeBindingVerifier();
    private final ComfyUiRuntimeCapabilityProbe runtimeCapabilityProbe = new ComfyUiRuntimeCapabilityProbe();
    private final ComfyUiLaunchArgumentPlanner launchArgumentPlanner = new ComfyUiLaunchArgumentPlanner();
    private boolean launchRequested;
    private VisualEngineRuntimeIdentity runtimeIdentity;

    public LocalVisualImageEngineManager() {
        this(new InspectLocalTheatreImageSetupReadinessUseCase(),
                ExternalProcessRunner.unavailable("Generacion visual local"),
                new ComfyUiVisualEngineClient(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()),
                new EnvironmentComputeDeviceDiscoveryGateway());
    }

    public LocalVisualImageEngineManager(InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase,
                                         ExternalProcessRunner processRunner,
                                         HttpClient httpClient) {
        this(readinessUseCase, processRunner, new ComfyUiVisualEngineClient(httpClient),
                new EnvironmentComputeDeviceDiscoveryGateway());
    }

    public LocalVisualImageEngineManager(InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase,
                                         ExternalProcessRunner processRunner,
                                         ComfyUiVisualEngineClient visualEngineClient) {
        this(readinessUseCase, processRunner, visualEngineClient,
                new EnvironmentComputeDeviceDiscoveryGateway());
    }

    public LocalVisualImageEngineManager(InspectLocalTheatreImageSetupReadinessUseCase readinessUseCase,
                                         ExternalProcessRunner processRunner,
                                         ComfyUiVisualEngineClient visualEngineClient,
                                         ComputeDeviceDiscoveryGateway computeDeviceDiscoveryGateway) {
        this.readinessUseCase = Objects.requireNonNull(readinessUseCase, "readinessUseCase");
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.visualEngineClient = Objects.requireNonNull(visualEngineClient, "visualEngineClient");
        this.computeDeviceDiscoveryGateway = Objects.requireNonNull(
                computeDeviceDiscoveryGateway, "computeDeviceDiscoveryGateway");
    }

    public synchronized ImageEngineReadinessReport inspect(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        LocalTheatreImageSetupReadinessReport setup = readinessUseCase.inspect(current, applicationRoot);
        ImageEngineArtifactInspectionReport artifacts = setup.artifactInspection();
        boolean running = false;
        boolean launchableRuntime = false;
        String computeError = "";
        try {
            running = endpointResponds(current, setup.runtimeDirectory());
            launchableRuntime = setup.runtimePrepared()
                    && !launchCommand(setup.runtimeDirectory(), current, false).isEmpty();
        } catch (RuntimeException ex) {
            computeError = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        }
        ImageEngineRuntimeState state;
        String message;
        String nextAction;
        if (!computeError.isBlank()) {
            state = ImageEngineRuntimeState.ERROR;
            message = computeError;
            nextAction = "Selecciona y prepara el dispositivo exacto en Configuracion > Generacion visual.";
        } else if (!setup.runtimePrepared() && setup.modelUsable()) {
            state = ImageEngineRuntimeState.NOT_PREPARED;
            message = "Modelo instalado, falta runtime local compatible.";
            nextAction = "Instala o importa runtime en tools/image.";
        } else if (!setup.runtimePrepared()) {
            state = ImageEngineRuntimeState.NOT_PREPARED;
            message = "Generacion visual local no preparada.";
            nextAction = "Instala o importa un runtime local compatible.";
        } else if (!setup.modelUsable()) {
            state = ImageEngineRuntimeState.RUNTIME_PREPARED;
            message = artifacts == null ? "Runtime preparado; falta descargar o importar paquete de modelos."
                    : artifacts.userMessage();
            nextAction = "Descargar paquete o importar paquete con workflow real.";
        } else if (!launchableRuntime && !running) {
            state = ImageEngineRuntimeState.MODEL_INSTALLED;
            message = "Modelo instalado, falta runtime local compatible.";
            nextAction = "Importa o instala un runtime con start-image-engine.bat, run.bat, ComfyUI.bat o ComfyUI/main.py.";
        } else if (!running) {
            state = ImageEngineRuntimeState.MODEL_INSTALLED;
            message = "Modelo instalado; falta iniciar o detectar el motor local.";
            nextAction = "Iniciar motor o Probar generacion.";
        } else {
            state = ImageEngineRuntimeState.READY;
            message = "Generacion visual local lista para generar.";
            nextAction = "Abre un modulo compatible y genera un resultado.";
        }
        if (running && !setup.modelUsable()) {
            state = ImageEngineRuntimeState.ENGINE_STARTED;
            message = "Motor iniciado; falta paquete de modelos utilizable.";
            nextAction = "Descargar paquete o importar paquete.";
        }
        return new ImageEngineReadinessReport(
                state,
                setup.applicationRoot(),
                setup.runtimeDirectory(),
                setup.modelDirectory(),
                setup.runtimePrepared(),
                setup.modelUsable(),
                running,
                artifacts,
                setup.missingRequirements(),
                message,
                nextAction);
    }

    public synchronized ImageEngineSmokeReport start(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ImageEngineReadinessReport before = inspect(current, applicationRoot);
        if (before.ready()) {
            return new ImageEngineSmokeReport(true, before.state(), ImageEngineSmokeStage.STARTING_ENGINE, null,
                    "El motor de generacion visual local ya esta respondiendo.", "");
        }
        if (!before.runtimePrepared() || !before.modelInstalled()) {
            ImageEngineSmokeStage stage = !before.runtimePrepared()
                    ? ImageEngineSmokeStage.VERIFYING_RUNTIME
                    : ImageEngineSmokeStage.VERIFYING_MODEL;
            return new ImageEngineSmokeReport(false, before.state(), stage, null,
                    before.userMessage() + " " + before.nextAction(), "");
        }
        Path runtime = before.runtimeDirectory();
        try {
            List<String> command = launchCommand(runtime, current, true);
            if (command.isEmpty()) {
                return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.MODEL_INSTALLED,
                        ImageEngineSmokeStage.VERIFYING_RUNTIME, null,
                        "Modelo instalado, falta runtime local compatible. No se encontro un lanzador local en tools/image.",
                        diagnostics(current, "", "Buscado: start-image-engine.bat, start.bat, run.bat, ComfyUI.bat, main.py o ComfyUI/main.py con Python embebido."));
            }
            ExternalProcessRequest launch = ExternalProcessRequest.of(detachedLaunchCommand(command),
                            "image-engine-start",
                            Duration.ofSeconds(12))
                    .withWorkingDirectory(runtime)
                    .redirectingErrorStream();
            ExternalProcessResult launchResult = processRunner.run(launch);
            if (!launchResult.succeeded()) {
                return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                        ImageEngineSmokeStage.STARTING_ENGINE, null,
                        "No se pudo iniciar el motor de generacion visual local desde el runtime preparado.",
                        diagnostics(current, launchDescription(command), launchResult.combinedOutputTail()));
            }
            launchRequested = true;
            ImageEngineSmokeReport wait = waitUntilReady(current, runtime, readinessWait(current));
            if (wait.success()) {
                return wait;
            }
            return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ENGINE_STARTED,
                    ImageEngineSmokeStage.STARTING_ENGINE, null,
                    "Motor iniciado; sigue arrancando o aun no responde en el endpoint configurado. Espera unos segundos y vuelve a probar.",
                    diagnostics(current, launchDescription(command), launchResult.combinedOutputTail() + "\n" + wait.diagnostic()));
        } catch (IOException | IllegalStateException ex) {
            return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                    ImageEngineSmokeStage.STARTING_ENGINE, null,
                    "No se pudo iniciar el motor de generacion visual local: " + ex.getMessage(), ex.toString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                    ImageEngineSmokeStage.STARTING_ENGINE, null,
                    "Inicio del motor local interrumpido.", ex.toString());
        }
    }

    public synchronized ImageEngineSmokeReport stop(OperationalSettings settings) {
        ImageEngineReadinessReport readiness = inspect(settings, null);
        boolean launchedHere = launchRequested;
        if (readiness.engineResponding()) {
            return new ImageEngineSmokeReport(false, readiness.state(), ImageEngineSmokeStage.STARTING_ENGINE, null,
                    launchedHere
                            ? "Motor iniciado como proceso externo. Cierralo desde su ventana/runtime si sigue respondiendo."
                            : "El motor responde, pero no fue iniciado por DocuPodcast en esta sesion. Detenlo desde su ventana/runtime.",
                    "");
        }
        launchRequested = false;
        runtimeIdentity = null;
        return new ImageEngineSmokeReport(true, ImageEngineRuntimeState.MODEL_INSTALLED,
                ImageEngineSmokeStage.COMPLETE, null,
                "Motor de generacion visual local detenido.", "");
    }

    public synchronized ImageEngineSmokeReport smoke(OperationalSettings settings, Path applicationRoot) {
        return smoke(settings, applicationRoot, ImageEngineSmokeRequest.defaults());
    }

    public synchronized ImageEngineSmokeReport smoke(OperationalSettings settings,
                                                     Path applicationRoot,
                                                     String promptText,
                                                     int steps) {
        return smoke(settings, applicationRoot, new ImageEngineSmokeRequest(
                null,
                null,
                null,
                promptText,
                steps,
                null));
    }

    public synchronized ImageEngineSmokeReport smoke(OperationalSettings settings,
                                                     Path applicationRoot,
                                                     ImageEngineSmokeRequest request) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ImageEngineSmokeRequest smokeRequest = request == null ? ImageEngineSmokeRequest.defaults() : request;
        ImageEngineReadinessReport report = inspect(current, applicationRoot);
        ImageEngineArtifactInspectionReport artifacts = report.artifactInspection();
        if (!report.runtimePrepared()) {
            return new ImageEngineSmokeReport(false, report.state(), ImageEngineSmokeStage.VERIFYING_RUNTIME, null,
                    report.userMessage() + " " + report.nextAction(), "");
        }
        if (!report.modelInstalled()) {
            return new ImageEngineSmokeReport(false, report.state(), ImageEngineSmokeStage.VERIFYING_MODEL, null,
                    report.userMessage() + " " + report.nextAction(), "");
        }
        if (!report.engineResponding()) {
            ImageEngineSmokeReport started = start(current, applicationRoot);
            if (!started.success()) {
                return started;
            }
            report = inspect(current, applicationRoot);
            artifacts = report.artifactInspection();
        }
        if (!report.engineResponding()) {
            return new ImageEngineSmokeReport(false, report.state(), ImageEngineSmokeStage.STARTING_ENGINE, null,
                    "El motor visual local no responde; no se puede generar el PNG de prueba.", "");
        }
        return generateSmokePng(current, applicationRoot, artifacts, smokeRequest);
    }

    private ImageEngineSmokeReport waitUntilReady(OperationalSettings settings, Path runtime, Duration maxWait) {
        long deadline = System.nanoTime() + maxWait.toNanos();
        while (System.nanoTime() < deadline) {
            if (endpointResponds(settings, runtime)) {
                return new ImageEngineSmokeReport(true, ImageEngineRuntimeState.READY,
                        ImageEngineSmokeStage.STARTING_ENGINE, null,
                        "Motor de generacion visual local iniciado y listo.",
                        diagnostics(settings, "", "waitSeconds=" + maxWait.toSeconds()));
            }
            try {
                Thread.sleep(750);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                        ImageEngineSmokeStage.STARTING_ENGINE, null,
                        "Prueba del motor local interrumpida.", ex.toString());
            }
        }
        return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ENGINE_STARTED,
                ImageEngineSmokeStage.STARTING_ENGINE, null,
                "El motor local no respondio dentro del tiempo de prueba.",
                diagnostics(settings, "", "waitSeconds=" + maxWait.toSeconds()));
    }

    private ImageEngineSmokeReport generateSmokePng(OperationalSettings settings, Path applicationRoot,
                                                   ImageEngineArtifactInspectionReport artifacts) {
        return generateSmokePng(settings, applicationRoot, artifacts, ImageEngineSmokeRequest.defaults());
    }

    private ImageEngineSmokeReport generateSmokePng(OperationalSettings settings,
                                                   Path applicationRoot,
                                                   ImageEngineArtifactInspectionReport artifacts,
                                                   ImageEngineSmokeRequest request) {
        ImageEngineSmokeRequest smokeRequest = request == null ? ImageEngineSmokeRequest.defaults() : request;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        ImageEnginePresetSupport presetSupport = ImageEnginePresetSupportPolicy.forProfile(smokeRequest.profile());
        if (!presetSupport.builtInWorkflowAvailable()) {
            return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                    ImageEngineSmokeStage.GENERATING_TEST, null,
                    presetSupport.userMessage(),
                    presetSupport.diagnostic());
        }
        String checkpoint = !presetSupport.checkpointName().isBlank()
                ? presetSupport.checkpointName()
                : artifacts == null || artifacts.checkpointName().isBlank()
                ? settings.imageGeneration().modelName()
                : artifacts.checkpointName();
        FluxModelBundle fluxBundle = null;
        ComfyUiWorkflowSpec workflow = ComfyUiWorkflowSpec.sd15();
        if (presetSupport.fluxCompatible()) {
            if (!new FluxLicenseAcceptanceStore().accepted(root, presetSupport.modelPackageProfile())) {
                return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                        ImageEngineSmokeStage.VERIFYING_MODEL, null,
                        "Importa el modelo descargado y confirma sus condiciones en Configuración. Esta confirmación no inicia sesión en el proveedor.",
                        "licenseAccepted=false\nlicenseUrl=" + FluxLicenseAcceptanceStore.LICENSE_URL);
            }
            fluxBundle = presetSupport.kontextWorkflow()
                    ? FluxModelBundle.inspectKontext(root)
                    : FluxModelBundle.inspect(root);
            if (!fluxBundle.ready()) {
                return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                        ImageEngineSmokeStage.VERIFYING_MODEL, null,
                        "Faltan componentes FLUX: " + String.join(", ", fluxBundle.missingComponents()) + ".", "");
            }
            FluxMemoryPreflight.Report memory = new FluxMemoryPreflight().inspect(root);
            if (!memory.ready()) {
                return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                        ImageEngineSmokeStage.VERIFYING_MODEL, null,
                        memory.userMessage(), memory.diagnostic());
            }
            int[] nativeSize = fluxSmokeNativeSize(smokeRequest);
            workflow = presetSupport.kontextWorkflow()
                    ? ComfyUiWorkflowSpec.fluxKontext(
                            fluxBundle.modelName(), fluxBundle.vaeName(),
                            fluxBundle.clipLName(), fluxBundle.t5Name(),
                            nativeSize[0], nativeSize[1])
                    : ComfyUiWorkflowSpec.flux(fluxBundle.modelName(), fluxBundle.vaeName(),
                            fluxBundle.clipLName(), fluxBundle.t5Name(), nativeSize[0], nativeSize[1]);
            checkpoint = fluxBundle.modelName();
        }
        if (checkpoint == null || checkpoint.isBlank()) {
            return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                    ImageEngineSmokeStage.VERIFYING_MODEL, null,
                    "No se pudo generar prueba: no hay checkpoint real configurado.", "");
        }
        try {
            int targetWidth = smokeRequest.targetWidth();
            int targetHeight = smokeRequest.targetHeight();
            int safeSteps = smokeRequest.steps();
            String prompt = smokePrompt(smokeRequest.promptText()) + ", " + smokeRequest.aspectRatio().promptText();
            Path smokeDir = smokeRequest.outputDirectory() == null
                    ? root.resolve("generated/visual/smoke").normalize()
                    : smokeRequest.outputDirectory().toAbsolutePath().normalize();
            String prefix = "smoke-" + smokeRequest.outputProfile().workflowId()
                    + "-" + smokeRequest.aspectRatio().workflowId()
                    + "-" + System.currentTimeMillis();
            VisualEngineRequest visualRequest = new VisualEngineRequest(
                    prompt,
                    "text, watermark, distorted",
                    checkpoint,
                    safeSteps,
                    presetSupport.defaultCfg(),
                    presetSupport.defaultBatchSize(),
                    targetWidth,
                    targetHeight,
                    smokeDir,
                    prefix);
            VisualEngineResult result = visualEngineClient.generate(
                    settings.imageGeneration().baseUrl(),
                    presetSupport.fluxCompatible()
                            ? Duration.ofMinutes(45)
                            : Duration.ofSeconds(settings.imageGeneration().timeoutSeconds()),
                    visualRequest,
                    workflow,
                    GenerationAttemptPolicy.defaults(),
                    GenerationTaskKind.IMAGE_CANDIDATE,
                    null);
            String consistency = artifacts != null && !artifacts.advancedConsistencyReady()
                    ? " Generacion basica lista; consistencia avanzada requiere adaptadores reales o LoRA."
                    : "";
            int nativeWidth = workflow.generationWidth(visualRequest);
            int nativeHeight = workflow.generationHeight(visualRequest);
            String resizeNote = nativeWidth == targetWidth && nativeHeight == targetHeight
                    ? ""
                    : " Base ComfyUI: " + nativeWidth + "x" + nativeHeight
                    + "; salida redimensionada tecnicamente.";
            return new ImageEngineSmokeReport(true, ImageEngineRuntimeState.READY,
                    ImageEngineSmokeStage.COMPLETE, result.outputPath(),
                    targetWidth,
                    targetHeight,
                    smokeRequest.outputProfile().displayName(),
                    smokeRequest.aspectRatio().label(),
                    "Prueba completada. PNG generado: " + result.outputPath() + ". "
                            + targetWidth + "x" + targetHeight + " "
                            + smokeRequest.aspectRatio().label() + "." + resizeNote + consistency,
                    result.diagnostic()
                            + "\nsteps=" + safeSteps
                            + "\npreset=" + presetSupport.presetId()
                            + "\nvisualProfile=" + smokeRequest.profile().name()
                            + "\nprofile=" + smokeRequest.outputProfile().name()
                            + "\naspectRatio=" + smokeRequest.aspectRatio().name());
        } catch (IOException ex) {
            return new ImageEngineSmokeReport(false, ImageEngineRuntimeState.ERROR,
                    ImageEngineSmokeStage.GENERATING_TEST, null,
                    "No se pudo generar o guardar el PNG de prueba: " + ex.getMessage(), ex.toString());
        }
    }

    private static String smokePrompt(String promptText) {
        String cleaned = promptText == null ? "" : promptText.strip();
        if (cleaned.isBlank()) {
            return "cinematic educational visual, coherent composition, local engine smoke test";
        }
        return cleaned.length() > 1_400 ? cleaned.substring(0, 1_400) : cleaned;
    }

    /**
     * Keeps the engine smoke test small enough to prove a FLUX installation on
     * 4 GB GPUs. Production generation uses the sizes from
     * {@link com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiWorkflowSpec}.
     */
    static int[] fluxSmokeNativeSize(ImageEngineSmokeRequest request) {
        int targetWidth = request.targetWidth();
        int targetHeight = request.targetHeight();
        if (targetWidth == targetHeight) {
            return new int[] {512, 512};
        }
        if (targetWidth > targetHeight && Math.abs(targetWidth / (double) targetHeight - 16.0 / 9.0) < 0.05) {
            return new int[] {512, 288};
        }
        if (targetHeight > targetWidth && Math.abs(targetHeight / (double) targetWidth - 16.0 / 9.0) < 0.05) {
            return new int[] {288, 512};
        }
        double ratio = targetWidth / (double) targetHeight;
        int width = ratio >= 1.0 ? 512 : multipleOfSixteen((int) Math.round(512 * ratio));
        int height = ratio >= 1.0 ? multipleOfSixteen((int) Math.round(512 / ratio)) : 512;
        return new int[] {width, height};
    }

    private static int multipleOfSixteen(int value) {
        return Math.max(16, Math.round(value / 16.0f) * 16);
    }

    private boolean endpointResponds(OperationalSettings settings, Path runtime) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Duration timeout = endpointTimeout(current);
        if (!visualEngineClient.test(current.imageGeneration().baseUrl(), timeout).available()) {
            return false;
        }
        ComfyUiRuntimeCapabilities capabilities = runtime == null
                ? ComfyUiRuntimeCapabilities.unknown("Endpoint externo: flags de arranque no inspeccionados.")
                : runtimeCapabilityProbe.probe(runtime, processRunner);
        VisualComputeBinding expected = resolveComputeBinding(current, capabilities);
        try {
            ComfyUiSystemStats stats = visualEngineClient.systemStats(current.imageGeneration().baseUrl(), timeout);
            VisualComputeBindingVerifier.Verification verification = computeBindingVerifier.verify(expected, stats);
            if (!verification.matches()) {
                throw new IllegalStateException(verification.message());
            }
            runtimeIdentity = new VisualEngineRuntimeIdentity(
                    -1L,
                    stripTrailingSlash(current.imageGeneration().baseUrl()),
                    expected,
                    expected.launchArguments(),
                    stats.pythonVersion(),
                    stats.pytorchVersion(),
                    Instant.now());
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "ComfyUI responde, pero no se pudo verificar el dispositivo seleccionado: " + ex.getMessage(), ex);
        }
    }

    public synchronized Optional<VisualEngineRuntimeIdentity> runtimeIdentity() {
        return Optional.ofNullable(runtimeIdentity);
    }

    private static Duration endpointTimeout(OperationalSettings settings) {
        int seconds = settings == null ? 8 : settings.imageGeneration().timeoutSeconds();
        return Duration.ofSeconds(Math.min(8, Math.max(2, seconds)));
    }

    static Duration readinessWait(OperationalSettings settings) {
        int seconds = settings == null ? 60 : settings.imageGeneration().timeoutSeconds();
        return Duration.ofSeconds(Math.min(240, Math.max(60, seconds)));
    }

    private static String diagnostics(OperationalSettings settings, String launcher, String detail) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        StringBuilder out = new StringBuilder();
        out.append("baseUrl=").append(stripTrailingSlash(current.imageGeneration().baseUrl()));
        out.append("\nmemoryProfile=").append(current.imageGeneration().memoryProfile());
        out.append("\nlowVramLegacy=").append(current.imageGeneration().lowVram());
        out.append("\nselectedDevice=").append(current.compute().selectedDeviceId());
        out.append("\ncomputePolicy=").append(current.compute().policy());
        if (launcher != null && !launcher.isBlank()) {
            out.append("\nlauncher=").append(launcher);
        }
        if (detail != null && !detail.isBlank()) {
            out.append("\n").append(detail.strip());
        }
        return out.toString();
    }

    private static String launchDescription(List<String> command) {
        return command == null ? "" : command.stream()
                .map(LocalVisualImageEngineManager::windowsQuote)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    List<String> launchCommand(Path runtime, OperationalSettings settings, boolean ensureManagedLauncher) {
        ComfyUiRuntimeCapabilities capabilities = runtimeCapabilityProbe.probe(runtime, processRunner);
        Path main = Files.isRegularFile(runtime.resolve("main.py"))
                ? runtime.resolve("main.py")
                : runtime.resolve("ComfyUI/main.py");
        if (Files.isRegularFile(main)) {
            Path python = firstExisting(runtime,
                    "python_embeded/python.exe", "python_embedded/python.exe", "venv/Scripts/python.exe", ".venv/Scripts/python.exe");
            if (python != null) {
                ArrayList<String> command = new ArrayList<>();
                command.add(python.toString());
                command.add(main.toString());
                appendCommonArgs(command, settings, capabilities, runtime);
                return command;
            }
        }
        Path managed = null;
        if (ensureManagedLauncher) {
            try {
                managed = ImportLocalTheatreImageRuntimeUseCase.ensureManagedLauncher(runtime, ModelSetupProgressListener.noop());
            } catch (IOException ignored) {
                managed = null;
            }
        }
        if (managed != null && Files.isRegularFile(managed)) {
            ArrayList<String> command = new ArrayList<>();
            command.add(managed.toString());
            appendCommonArgs(command, settings, capabilities, runtime);
            return command;
        }
        Path explicit = firstExisting(runtime,
                "start-image-engine.bat", "start.bat", "run.bat", "ComfyUI.bat");
        ArrayList<String> command = new ArrayList<>();
        if (explicit != null) {
            command.add(explicit.toString());
            appendCommonArgs(command, settings, capabilities, runtime);
            return command;
        }
        return List.of();
    }

    static List<String> detachedLaunchCommand(List<String> command) {
        if (isWindows()) {
            String launch = "start \"\" /min " + command.stream()
                    .map(LocalVisualImageEngineManager::windowsQuote)
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
            return List.of("cmd", "/d", "/s", "/c", launch);
        }
        return List.of("sh", "-c", shellJoin(command) + " >/dev/null 2>&1 &");
    }

    private static String windowsQuote(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String shellJoin(List<String> command) {
        return command.stream()
                .map(LocalVisualImageEngineManager::shellQuote)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static String shellQuote(String value) {
        String text = value == null ? "" : value;
        return "'" + text.replace("'", "'\"'\"'") + "'";
    }

    private void appendCommonArgs(List<String> command, OperationalSettings settings,
                                  ComfyUiRuntimeCapabilities capabilities, Path runtime) {
        Endpoint endpoint = endpoint(settings);
        command.add("--listen");
        command.add(endpoint.host());
        command.add("--port");
        command.add(Integer.toString(endpoint.port()));
        Path extraModelPaths = runtime.toAbsolutePath().normalize().resolve("extra_model_paths.yaml");
        if (Files.isRegularFile(extraModelPaths)) {
            command.add("--extra-model-paths-config");
            command.add(extraModelPaths.toString());
        }
        command.addAll(resolveComputeBinding(settings, capabilities).launchArguments());
        command.addAll(launchArgumentPlanner.memoryArguments(settings.imageGeneration(), capabilities));
    }

    private VisualComputeBinding resolveComputeBinding(OperationalSettings settings,
                                                        ComfyUiRuntimeCapabilities capabilities) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        List<ComputeDeviceDescriptor> detected = computeDeviceDiscoveryGateway.discover(
                System.getProperties(), System.getenv(), Runtime.getRuntime().availableProcessors());
        return computeBackendResolver.resolve(current.compute(), detected, capabilities);
    }

    private static Path firstExisting(Path root, String... names) {
        for (String name : names) {
            Path candidate = root.resolve(name).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Endpoint endpoint(OperationalSettings settings) {
        try {
            URI uri = URI.create(stripTrailingSlash(settings.imageGeneration().baseUrl()));
            String host = uri.getHost() == null || uri.getHost().isBlank() ? "127.0.0.1" : uri.getHost();
            int port = uri.getPort() <= 0 ? 8188 : uri.getPort();
            return new Endpoint(host, port);
        } catch (RuntimeException ex) {
            return new Endpoint("127.0.0.1", 8188);
        }
    }

    private static String stripTrailingSlash(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8188" : baseUrl.strip();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT).startsWith("http") ? value : "http://" + value;
    }

    private record Endpoint(String host, int port) {
    }

}
