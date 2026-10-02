package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** ComfyUI image adapter with honest readiness for every product preset. */
public final class ComfyUiImageEngine implements ImageGenerationEngine {
    private static final long IP_ADAPTER_SD15_BYTES = 98_183_288L;
    private static final long CLIP_VISION_VIT_H_BYTES = 2_528_373_448L;
    private static final long CONTROLNET_SCRIBBLE_BYTES = 722_601_100L;
    public static final EngineId ID = new EngineId("comfyui");
    public static final EnginePresetId DRAFT = new EnginePresetId("draft");
    public static final EnginePresetId SD15_REGIONAL_IDENTITY =
            new EnginePresetId("sd15-regional-identity");
    public static final EnginePresetId DREAMSHAPER = new EnginePresetId("sd15-dreamshaper");
    public static final EnginePresetId SDXL_REFERENCE = new EnginePresetId("sdxl-reference");
    public static final EnginePresetId FLUX_KONTEXT = new EnginePresetId("flux-kontext");
    public static final EnginePresetId FLUX_HIGH_QUALITY = new EnginePresetId("flux-high-quality");
    public static final EnginePresetId CUSTOM = new EnginePresetId("custom-comfy-workflow");
    /** Neutral auxiliary operation; intentionally omitted from the selectable preset catalog. */
    public static final EnginePresetId MIDDLE_FRAME_INTERPOLATION = new EnginePresetId("middle-frame-interpolation");

    private final EngineConfiguration configuration;
    private final ComfyUiTransport transport;
    private final ManagedRuntime runtime;

    interface ManagedRuntime {
        void ensureReady(ExecutionContext context) throws IOException, InterruptedException;
        boolean recover(ExecutionContext context) throws IOException, InterruptedException;
    }

    @Override public void releaseIdleResources(ExecutionContext context) throws IOException, InterruptedException {
        transport.releaseModels();
    }

    @Override public ComputeResourceDemand resourceDemand(ImageGenerationRequest request, ComputePreference preference) {
        // An externally managed ComfyUI workflow can load arbitrary checkpoints.
        // Keep exclusive admission until residency and its physical demand can be verified;
        // a fixed VRAM estimate would incorrectly advertise safe model co-residency.
        return ImageGenerationEngine.super.resourceDemand(request, preference);
    }

    public ComfyUiImageEngine(EngineConfiguration configuration) {
        this(configuration, null);
    }

    ComfyUiImageEngine(EngineConfiguration configuration, ManagedRuntime runtime) {
        this.runtime = runtime;
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.transport = new ComfyUiTransport(this.configuration.value("baseUrl"));
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.IMAGE_GENERATION, "Imagen local", "local", "http",
                Set.of(), false);
    }

    @Override public List<EnginePresetDescriptor> presets() {
        return List.of(
                preset(SD15_REGIONAL_IDENTITY, "Intervención contextual · calidad adaptativa",
                        "Checkpoint estético SD 1.5 con identidad regional, composición activa y offload adaptativo.",
                        true),
                preset(DRAFT, "Prueba 4 GB SD 1.5", "Generacion corta SD 1.5.", true),
                preset(DREAMSHAPER, "DreamShaper SD 1.5", "Generacion estetica SD 1.5.", false),
                preset(SDXL_REFERENCE, "SDXL base (sin referencias)",
                        "Generación SDXL; el workflow condicionado aún no está disponible.", false),
                preset(FLUX_KONTEXT, "FLUX base (sin referencias)",
                        "Generación FLUX; no anuncia condicionamiento visual.", false),
                preset(FLUX_HIGH_QUALITY, "FLUX alta calidad", "Generacion FLUX de alta calidad.", false),
                preset(CUSTOM, "Workflow ComfyUI personalizado", "Requiere un workflow importado.", false));
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("baseUrl", "Direccion local", "Endpoint HTTP del runtime local.",
                        ConfigurationFieldType.URL, true, "http://127.0.0.1:8188")));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        return inspectPresetReadiness(DRAFT);
    }

    public EngineReadiness inspectPresetReadiness(EnginePresetId presetId) {
        EnginePresetId selected = presetId == null || EnginePresetId.AUTO.equals(presetId)
                ? DRAFT : presetId;
        PresetResources resources = resourcesFor(selected);
        if (!resources.executable()) {
            return EngineReadiness.unavailable(ID,
                    "El preset " + selected.value() + " no está disponible.",
                    humanReason(resources.reason()));
        }
        try {
            if (!transport.isReady()) {
                return EngineReadiness.unavailable(ID, "El motor de imagen rechazó la prueba HTTP.",
                        "Inicia el runtime local.");
            }
            String node = resources.flux() ? "UNETLoader" : "CheckpointLoaderSimple";
            String objectInfo = transport.objectInfo(node);
            if (!objectInfo.contains(resources.modelName())) {
                return new EngineReadiness(ID, ReadinessState.UNAVAILABLE,
                        "ComfyUI no enumera el modelo requerido por " + selected.value() + ".",
                        List.of("Modelo no enumerado: " + resources.modelName()),
                        List.of("Repara las rutas de modelos o importa el modelo desde Motores y dependencias."),
                        "node=" + node + "; model=" + resources.modelName());
            }
            if (resources.flux()) {
                String dualClip = transport.objectInfo("DualCLIPLoader");
                String vae = transport.objectInfo("VAELoader");
                for (String file : List.of(fileName(configuredPath("fluxClipL")),
                        fileName(configuredPath("fluxT5")))) {
                    if (!dualClip.contains(file)) {
                        return EngineReadiness.unavailable(ID,
                                "Falta un codificador de texto FLUX: " + file + ".",
                                "Repara las rutas o importa el componente FLUX.");
                    }
                }
                String vaeName = fileName(configuredPath("fluxVae"));
                if (!vae.contains(vaeName)) {
                    return EngineReadiness.unavailable(ID, "Falta el VAE FLUX: " + vaeName + ".",
                            "Repara las rutas o importa el componente FLUX.");
                }
            }
            if (SD15_REGIONAL_IDENTITY.equals(selected)) {
                for (String requiredNode : List.of(
                        "IPAdapterUnifiedLoader", "IPAdapterAdvanced", "IPAdapterModelLoader",
                        "CLIPVisionLoader", "SolidMask",
                        "MaskComposite", "ConditioningSetMask")) {
                    transport.objectInfo(requiredNode);
                }
                String ipAdapterInfo = transport.objectInfo("IPAdapterModelLoader");
                String ipAdapterName = fileName(configuredPath("ipAdapterModel"));
                if (!ipAdapterInfo.contains(ipAdapterName)) {
                    return EngineReadiness.unavailable(ID,
                            "ComfyUI no enumera IP-Adapter Plus SD 1.5.",
                            "Prepara Consistencia de personajes desde Motores y dependencias.");
                }
                String clipVisionInfo = transport.objectInfo("CLIPVisionLoader");
                String clipVisionName = fileName(configuredPath("clipVisionModel"));
                if (!clipVisionInfo.contains(clipVisionName)) {
                    return EngineReadiness.unavailable(ID,
                            "ComfyUI no enumera CLIP Vision ViT-H.",
                            "Prepara Consistencia de personajes desde Motores y dependencias.");
                }
                String controlInfo = transport.objectInfo("ControlNetLoader");
                String scribbleName = fileName(configuredPath("scribbleControlNet"));
                if (!controlInfo.contains(scribbleName)) {
                    return EngineReadiness.unavailable(ID,
                            "ComfyUI no enumera ControlNet Scribble.",
                            "Prepara Consistencia de personajes desde Motores y dependencias.");
                }
            }
            return EngineReadiness.ready(ID,
                    "Motor local de imagen disponible para " + selected.value() + ".");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return new EngineReadiness(ID, ReadinessState.UNAVAILABLE,
                    "El motor local de imagen no responde o no puede enumerar sus recursos.",
                    List.of(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()),
                    List.of("Inicia o repara ComfyUI desde Motores y dependencias."),
                    ex.toString());
        }
    }

    @Override public ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (context == null) context = ExecutionContext.defaults("comfyui-image");
        context.cancellation().throwIfCancellationRequested();
        if (runtime != null) runtime.ensureReady(context);
        try {
            return generateOnce(request, context);
        } catch (IOException failure) {
            context.cancellation().throwIfCancellationRequested();
            if (runtime == null) throw failure;
            try {
                if (!runtime.recover(context)) throw failure;
                return generateOnce(request, context);
            } catch (IOException retryFailure) {
                if (retryFailure != failure) retryFailure.addSuppressed(failure);
                throw retryFailure;
            }
        }
    }

    private ImageGenerationResult generateOnce(ImageGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        EnginePresetId preset = EnginePresetId.AUTO.equals(request.presetId()) ? DRAFT : request.presetId();
        if (MIDDLE_FRAME_INTERPOLATION.equals(preset)) {
            MediaReference first = reference(request, "start-frame");
            MediaReference second = reference(request, "end-frame");
            Path target = new RifeFrameInterpolator(configuration.value("baseUrl"), configuredPath("rife"))
                    .interpolate(first.file(), second.file(), request.outputDirectory(), request.filenamePrefix(), context);
            return new ImageGenerationResult(List.of(target), Map.of(
                    "engineId", ID.value(), "presetId", preset.value(), "model", RifeFrameInterpolator.MODEL,
                    "frameRole", "middle"));
        }
        PresetResources resources = resourcesFor(preset);
        if (!resources.executable()) {
            throw new IOException("RESOURCE_MISSING: preset=" + preset.value() + " reason=" + resources.reason());
        }
        if (SD15_REGIONAL_IDENTITY.equals(preset)) {
            return generateConditioned(request, context, resources);
        }
        if (!request.mediaReferences().isEmpty() || !request.references().isEmpty()) {
            throw new IOException("RESOURCE_MISSING: preset=" + preset.value()
                    + " reason=conditioned-workflow-not-installed. Las referencias no se ignoraran.");
        }
        String workflow = resources.flux()
                ? ComfyWorkflowTemplate.flux(request.prompt(), request.width(), request.height(), request.seed(),
                request.batchSize(), request.filenamePrefix(), resources.modelName(),
                fileName(configuredPath("fluxVae")), fileName(configuredPath("fluxClipL")),
                fileName(configuredPath("fluxT5")))
                : ComfyWorkflowTemplate.image(resources.workflow(), request.prompt(), request.negativePrompt(),
                request.width(), request.height(), request.seed(), request.batchSize(), request.filenamePrefix(),
                resources.modelName());
        ComfyUiTransport.DownloadedArtifact artifact = transport.execute(workflow, context, "imagen");
        Files.createDirectories(request.outputDirectory());
        String extension = extension(artifact.filename(), ".png");
        Path target = request.outputDirectory().resolve(request.filenamePrefix() + extension);
        Files.write(target, artifact.bytes());
        int deliveryWidth = positiveInt(request.options().get("deliveryWidth"), request.width());
        int deliveryHeight = positiveInt(request.options().get("deliveryHeight"), request.height());
        if (deliveryWidth != request.width() || deliveryHeight != request.height()) {
            Path normalized = target.resolveSibling(target.getFileName() + ".delivery.partial.png");
            try {
                ImageDeliveryNormalizer.normalize(target, normalized, deliveryWidth, deliveryHeight, false);
                Files.move(normalized, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(normalized);
            }
        }
        return new ImageGenerationResult(List.of(target), Map.of(
                "engineId", ID.value(), "presetId", preset.value(), "promptId", artifact.promptId(),
                "seed", Long.toString(request.seed())));
    }

    private ImageGenerationResult generateConditioned(ImageGenerationRequest request,
                                                       ExecutionContext context,
                                                       PresetResources resources)
            throws IOException, InterruptedException {
        if (request.mediaReferences().stream().noneMatch(reference ->
                MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))) {
            throw new IOException("La intervención contextual requiere referencias regionales de identidad.");
        }
        EngineReadiness readiness = inspectPresetReadiness(SD15_REGIONAL_IDENTITY);
        if (!readiness.ready()) {
            throw new IOException(readiness.summary() + " "
                    + String.join(" ", readiness.issues()) + " "
                    + String.join(" ", readiness.recommendedActions()));
        }
        boolean syntheticComposition = request.mediaReferences().stream().anyMatch(reference ->
                MediaReferenceRole.COMPOSITION_GUIDE.equals(reference.role())
                        && "environment-bootstrap".equalsIgnoreCase(
                        reference.metadata().getOrDefault("activeVariant", "")));
        List<MediaReference> effectiveReferences = request.mediaReferences();
        Path bootstrap = null;
        if (syntheticComposition) {
            Files.createDirectories(request.outputDirectory());
            bootstrap = request.outputDirectory().resolve(
                    request.filenamePrefix() + "-context-bootstrap.partial.png");
            new TheatreReferenceCompositionBootstrapper().compose(
                    request.mediaReferences(), request.width(), request.height(), bootstrap);
            MediaReference composed = new MediaReference(
                    "CONTEXT-COMPOSITION-BOOTSTRAP",
                    bootstrap,
                    MediaReferenceRole.COMPOSITION_GUIDE,
                    0.92,
                    Map.of(
                            "label", "Composición construida desde escenario, utilería y personajes",
                            "activeVariant", "context-assets-bootstrap",
                            "compositionDenoise", "0.08"));
            ArrayList<MediaReference> replaced = new ArrayList<>(request.mediaReferences().stream()
                    .filter(reference -> !MediaReferenceRole.COMPOSITION_GUIDE.equals(reference.role()))
                    .toList());
            replaced.add(composed);
            effectiveReferences = List.copyOf(replaced);
        }

        ArrayList<ComfyConditionedWorkflow.Uploaded> uploaded = new ArrayList<>();
        try {
            int index = 0;
            for (MediaReference reference : effectiveReferences) {
                context.cancellation().throwIfCancellationRequested();
                context.progress().report("UPLOADING_REFERENCES",
                        Math.min(0.20, 0.04 + index * 0.02),
                        "Subiendo referencia " + reference.metadata()
                                .getOrDefault("label", reference.id()) + ".");
                uploaded.add(new ComfyConditionedWorkflow.Uploaded(
                        reference,
                        transport.uploadImage(reference.file(), ++index, context.policy().timeout())));
            }
        } finally {
            if (bootstrap != null) Files.deleteIfExists(bootstrap);
        }
        context.progress().report("CONDITIONING", 0.24,
                "Aplicando identidad, vestuario y composición por regiones.");
        int workWidth = request.width();
        int workHeight = request.height();
        boolean memoryRetry = false;
        ComfyUiTransport.DownloadedArtifact artifact;
        if (syntheticComposition) {
            List<ComfyConditionedWorkflow.Uploaded> embeddedComposition = uploaded.stream()
                    .filter(item -> !MediaReferenceRole.REGIONAL_IDENTITY.equals(item.reference().role()))
                    .toList();
            artifact = executeConditioned(
                    request, context, resources, embeddedComposition, workWidth, workHeight, false);
        } else {
        List<ComfyConditionedWorkflow.Uploaded> sceneReferences = uploaded.stream()
                .filter(item -> !MediaReferenceRole.REGIONAL_IDENTITY.equals(item.reference().role()))
                .toList();
        try {
            artifact = executeConditioned(
                    request, context, resources, sceneReferences, workWidth, workHeight, true);
        } catch (IOException firstFailure) {
            if (!isOutOfMemory(firstFailure)) throw firstFailure;
            memoryRetry = true;
            workWidth = multipleOfEight(Math.max(512, request.width() * 3 / 4));
            workHeight = multipleOfEight(Math.max(288, request.height() * 3 / 4));
            context.progress().report("MEMORY_RETRY", 0.30,
                    "Memoria GPU insuficiente; reintentando una vez con una carga más restrictiva y RAM de apoyo.");
            artifact = executeConditioned(
                    request, context, resources, sceneReferences, workWidth, workHeight, true);
        }
        if (request.mediaReferences().stream().anyMatch(reference ->
                MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))) {
            Files.createDirectories(request.outputDirectory());
            Path base = request.outputDirectory().resolve(
                    request.filenamePrefix() + "-scene-base.partial.png");
            try {
                Files.write(base, artifact.bytes());
                String engineBase = transport.uploadImage(
                        base, 10_000, context.policy().timeout());
                context.progress().report("CHARACTER_INPAINT", 0.62,
                        "Integrando cada personaje en su ROI mediante inpainting secuencial.");
                artifact = executeSequentialCharacterInpaint(
                        request, context, resources, uploaded, workWidth, workHeight, engineBase);
            } finally {
                Files.deleteIfExists(base);
            }
        }
        }
        Files.createDirectories(request.outputDirectory());
        String extension = extension(artifact.filename(), ".png");
        Path target = request.outputDirectory().resolve(request.filenamePrefix() + extension);
        Files.write(target, artifact.bytes());
        if (syntheticComposition) {
            new TheatreReferenceCompositionBootstrapper()
                    .restoreIdentityFaces(request.mediaReferences(), target);
        }
        int deliveryWidth = positiveInt(request.options().get("deliveryWidth"), request.width());
        int deliveryHeight = positiveInt(request.options().get("deliveryHeight"), request.height());
        if (deliveryWidth != request.width() || deliveryHeight != request.height()) {
            Path normalized = target.resolveSibling(target.getFileName() + ".delivery.partial.png");
            try {
                ImageDeliveryNormalizer.normalize(target, normalized, deliveryWidth, deliveryHeight, false);
                Files.move(normalized, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(normalized);
            }
        }
        return new ImageGenerationResult(List.of(target), Map.ofEntries(
                Map.entry("engineId", ID.value()),
                Map.entry("presetId", SD15_REGIONAL_IDENTITY.value()),
                Map.entry("promptId", artifact.promptId()),
                Map.entry("seed", Long.toString(request.seed())),
                Map.entry("conditioningWorkflow", syntheticComposition
                        ? "context-assets-img2img-face-roi"
                        : "regional-ipadapter-sd15"),
                Map.entry("referenceCount", Integer.toString(request.mediaReferences().size())),
                Map.entry("computeMode", "gpu-first-adaptive-offload"),
                Map.entry("workDimensions", workWidth + "x" + workHeight),
                Map.entry("memoryRetry", Boolean.toString(memoryRetry))));
    }

    private ComfyUiTransport.DownloadedArtifact executeConditioned(
            ImageGenerationRequest request,
            ExecutionContext context,
            PresetResources resources,
            List<ComfyConditionedWorkflow.Uploaded> uploaded,
            int width,
            int height,
            boolean sceneOnly) throws IOException, InterruptedException {
        String workflow = new ComfyConditionedWorkflow().render(
                resources.modelName(),
                fileName(configuredPath("scribbleControlNet")),
                sceneOnly
                        ? "empty theatrical aviation hangar set before the actors enter, "
                        + "required scenic objects, assigned environment, cinematic lighting"
                        : request.prompt(),
                sceneOnly
                        ? request.negativePrompt() + ", person, people, human, actor, character"
                        : request.negativePrompt(),
                width,
                height,
                request.seed(),
                positiveInt(request.options().get("steps"), 24),
                6.0,
                request.filenamePrefix(),
                uploaded);
        return transport.execute(workflow, context, "intervención teatral condicionada");
    }

    private ComfyUiTransport.DownloadedArtifact executeSequentialCharacterInpaint(
            ImageGenerationRequest request,
            ExecutionContext context,
            PresetResources resources,
            List<ComfyConditionedWorkflow.Uploaded> uploaded,
            int width,
            int height,
            String engineBase) throws IOException, InterruptedException {
        String workflow = new ComfySequentialCharacterInpaintWorkflow().render(
                resources.modelName(),
                request.prompt(),
                request.negativePrompt(),
                width,
                height,
                request.seed(),
                positiveInt(request.options().get("characterSteps"), 28),
                request.filenamePrefix() + "-characters",
                engineBase,
                uploaded);
        return transport.execute(workflow, context, "personajes teatrales por ROI");
    }

    private static boolean isOutOfMemory(IOException failure) {
        String message = failure.getMessage() == null
                ? "" : failure.getMessage().toLowerCase(java.util.Locale.ROOT);
        return message.contains("out of memory")
                || message.contains("cuda")
                && (message.contains("memory") || message.contains("allocation"));
    }

    private static int multipleOfEight(int value) {
        return Math.max(8, value - Math.floorMod(value, 8));
    }

    private static MediaReference reference(ImageGenerationRequest request, String role) throws IOException {
        return request.mediaReferences().stream()
                .filter(item -> item.role().value().equals(role)).findFirst()
                .orElseThrow(() -> new IOException("La interpolacion requiere referencia " + role + "."));
    }

    private EnginePresetDescriptor preset(EnginePresetId id, String name, String description, boolean primary) {
        PresetResources resources = resourcesFor(id);
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        metadata.put("resourceState", resources.executable() ? "available" : "missing");
        metadata.put("reason", resources.reason());
        metadata.put("modelResource", resources.modelResource().toString());
        metadata.put("workflowResource", resources.flux() ? "adapter-built-in:flux-v1"
                : resources.workflow().toString());
        if (SD15_REGIONAL_IDENTITY.equals(id)) {
            metadata.put("ipAdapterResource", configuredPath("ipAdapterModel").toString());
            metadata.put("clipVisionResource", configuredPath("clipVisionModel").toString());
            metadata.put("scribbleResource", configuredPath("scribbleControlNet").toString());
            metadata.put("customNodeResource", configuredPath("ipAdapterNode").toString());
        }
        return new EnginePresetDescriptor(id, name, description,
                SD15_REGIONAL_IDENTITY.equals(id)
                        ? Set.of(EngineFeature.CONDITIONING_IMAGE,
                        EngineFeature.COMPOSITION_PRESERVING,
                        EngineFeature.HARDWARE_ACCELERATION)
                        : Set.of(), metadata, primary);
    }

    private PresetResources resourcesFor(EnginePresetId id) {
        Path draftWorkflow = configuredPath("draftWorkflow");
        if (SD15_REGIONAL_IDENTITY.equals(id)) {
            Path dreamShaper = configuredPath("dreamshaperModel");
            Path checkpoint = Files.isRegularFile(dreamShaper)
                    ? dreamShaper : configuredPath("sd15Model");
            boolean ready = Files.isRegularFile(checkpoint)
                    && exactSize(configuredPath("ipAdapterModel"), IP_ADAPTER_SD15_BYTES)
                    && exactSize(configuredPath("clipVisionModel"), CLIP_VISION_VIT_H_BYTES)
                    && exactSize(configuredPath("scribbleControlNet"), CONTROLNET_SCRIBBLE_BYTES)
                    && Files.isRegularFile(configuredPath("ipAdapterNode").resolve("IPAdapterPlus.py"))
                    && Files.isRegularFile(configuredPath("ipAdapterNode").resolve("__init__.py"));
            return new PresetResources(draftWorkflow, checkpoint, fileName(checkpoint),
                    false, ready, ready ? "" : "conditioned-components-missing");
        }
        if (DRAFT.equals(id)) return checkpoint(draftWorkflow, configuredPath("sd15Model"));
        if (DREAMSHAPER.equals(id)) return checkpoint(draftWorkflow, configuredPath("dreamshaperModel"));
        if (SDXL_REFERENCE.equals(id)) return checkpoint(draftWorkflow, configuredPath("sdxlModel"));
        if (FLUX_KONTEXT.equals(id) || FLUX_HIGH_QUALITY.equals(id)) {
            Path model = configuredPath("fluxModel");
            boolean ready = Files.isRegularFile(model)
                    && Files.isRegularFile(configuredPath("fluxClipL"))
                    && Files.isRegularFile(configuredPath("fluxT5"))
                    && Files.isRegularFile(configuredPath("fluxVae"));
            return new PresetResources(Path.of(""), model, fileName(model), true, ready,
                    ready ? "" : "flux-components-missing");
        }
        return new PresetResources(Path.of(""), Path.of(""), "", false, false,
                "custom-workflow-not-configured");
    }

    private static PresetResources checkpoint(Path workflow, Path model) {
        boolean ready = Files.isRegularFile(workflow) && Files.isRegularFile(model);
        String reason = ready ? "" : !Files.isRegularFile(model) ? "model-missing" : "workflow-missing";
        return new PresetResources(workflow, model, fileName(model), false, ready, reason);
    }

    private static String humanReason(String reason) {
        return switch (reason == null ? "" : reason) {
            case "model-missing" -> "Importa el modelo requerido desde Motores y dependencias.";
            case "workflow-missing" -> "Importa un workflow ejecutable desde Motores y dependencias.";
            case "flux-components-missing" ->
                    "Importa el modelo, los dos codificadores de texto y el VAE de FLUX.";
            case "conditioned-components-missing" ->
                    "Prepara IP-Adapter Plus, CLIP Vision y ControlNet Scribble desde Motores y dependencias.";
            case "custom-workflow-not-configured" -> "Importa y configura un workflow ComfyUI real.";
            default -> reason == null ? "" : reason;
        };
    }

    private Path configuredPath(String key) {
        String value = configuration.value(key);
        return value.isBlank() ? Path.of("") : Path.of(value).toAbsolutePath().normalize();
    }

    private static String fileName(Path path) {
        return path == null || path.getFileName() == null ? "" : path.getFileName().toString();
    }

    private static String extension(String filename, String fallback) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? fallback : filename.substring(dot);
    }

    private static int positiveInt(String value, int fallback) {
        try {
            int parsed = value == null || value.isBlank() ? fallback : Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean exactSize(Path file, long expected) {
        try {
            return Files.isRegularFile(file) && Files.size(file) == expected;
        } catch (IOException ignored) {
            return false;
        }
    }

    private record PresetResources(Path workflow, Path modelResource, String modelName,
                                   boolean flux, boolean executable, String reason) { }
}
