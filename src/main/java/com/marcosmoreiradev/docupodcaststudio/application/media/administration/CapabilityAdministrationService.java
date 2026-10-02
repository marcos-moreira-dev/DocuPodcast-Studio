package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Application facade for discovery, readiness and all administrative actions.
 * Product workspaces depend on use cases built on this facade, never on an
 * adapter's START/STOP contract directly.
 */
public final class CapabilityAdministrationService {
    private final MediaEnginePlatform platform;
    private final ResourceScheduler scheduler;

    public CapabilityAdministrationService(MediaEnginePlatform platform) {
        this(platform, null);
    }

    public CapabilityAdministrationService(MediaEnginePlatform platform,
                                           ResourceScheduler scheduler) {
        this.platform = Objects.requireNonNullElseGet(platform, MediaEnginePlatform::empty);
        this.scheduler = scheduler;
        validatePublishedActions();
    }

    public List<ManagedComponentDescriptor> components() {
        return allEngines().stream().map(this::component).toList();
    }

    public CapabilityReadinessReport inspect(CapabilityRequirement requirement) {
        MediaEngine engine = resolve(requirement);
        EngineReadiness readiness = engine.inspectReadiness(null);
        return CapabilityReadinessReport.from(requirement, readiness);
    }

    public CapabilityPreparationPlan plan(CapabilityRequirement requirement) {
        CapabilityReadinessReport readiness = inspect(requirement);
        if (readiness.ready()) {
            return new CapabilityPreparationPlan(requirement, List.of(), 0L,
                    List.of(), List.of(), false);
        }
        ManagedComponentDescriptor component = component(resolve(requirement));
        List<String> downloads = component.actions().stream()
                .filter(action -> Boolean.parseBoolean(
                        action.metadata().getOrDefault("managedDownload", "false"))
                        || EngineActionId.INSTALL.equals(action.id())
                        && action.inputs().stream().anyMatch(input ->
                        input.type() == ConfigurationFieldType.URL))
                .map(EngineActionDescriptor::displayName).toList();
        List<String> imports = component.actions().stream()
                .filter(action -> action.inputs().stream().anyMatch(input ->
                        input.type() == ConfigurationFieldType.FILE
                                || input.type() == ConfigurationFieldType.DIRECTORY))
                .map(EngineActionDescriptor::displayName).toList();
        return new CapabilityPreparationPlan(requirement, List.of(component),
                component.approximateBytes(), downloads, imports,
                component.approximateBytes() > 0 || !downloads.isEmpty());
    }

    public EngineActionResult execute(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        return platform.administration().require(request.engineId()).execute(request, context);
    }

    public ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException {
        EngineAdministration administration = platform.administration().require(request.engineId());
        if (!(administration instanceof ManagedDownloadAdministration managed)) {
            throw new IllegalArgumentException("La acción no publica un preflight de descarga administrada.");
        }
        return managed.inspectDownload(request);
    }

    public EngineActionResult executeDownload(
            EngineActionRequest request,
            ManagedDownloadDecision decision,
            ExecutionContext context) throws IOException, InterruptedException {
        EngineAdministration administration = platform.administration().require(request.engineId());
        if (!(administration instanceof ManagedDownloadAdministration managed)) {
            throw new IllegalArgumentException("La acción no es una descarga administrada.");
        }
        return managed.executeDownload(request, decision, context);
    }

    public boolean isManagedDownload(EngineId engineId, EngineActionId actionId) {
        return platform.administration().find(engineId)
                .filter(ManagedDownloadAdministration.class::isInstance)
                .flatMap(administration -> administration.actions().stream()
                        .filter(action -> action.id().equals(actionId)).findFirst())
                .map(action -> Boolean.parseBoolean(
                        action.metadata().getOrDefault("managedDownload", "false")))
                .orElse(false);
    }

    public MediaEnginePlatform platform() {
        return platform;
    }

    public ComputeQueueSnapshot computeQueue() {
        return scheduler == null
                ? new ComputeQueueSnapshot(Map.of(), Map.of(),
                List.of(), List.of())
                : scheduler.snapshot();
    }

    public boolean cancelCompute(String admissionId) {
        return scheduler != null && scheduler.cancel(admissionId);
    }

    private ManagedComponentDescriptor component(MediaEngine engine) {
        EngineDescriptor descriptor = engine.descriptor();
        List<EngineActionDescriptor> actions = platform.administration().find(descriptor.id())
                .map(EngineAdministration::actions).orElse(List.of());
        List<String> dependencies = engine.presets().stream()
                .flatMap(preset -> preset.metadata().entrySet().stream())
                .filter(entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT).contains("resource"))
                .map(Map.Entry::getValue).filter(value -> value != null && !value.isBlank())
                .distinct().toList();
        long bytes = actions.stream()
                .filter(action -> Boolean.parseBoolean(
                        action.metadata().getOrDefault("managedDownload", "false")))
                .mapToLong(EngineActionDescriptor::approximateBytes)
                .sum();
        if (bytes == 0L) {
            bytes = actions.stream().mapToLong(EngineActionDescriptor::approximateBytes).max().orElse(0L);
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("runtime", descriptor.runtimeKind());
        metadata.put("version", descriptor.version());
        metadata.put("presetCount", Integer.toString(engine.presets().size()));
        String category = switch (descriptor.capability().value()) {
            case "voice-synthesis" -> "Voz";
            case "image-generation", "image-super-resolution", "image-refinement" -> "Procesamiento visual";
            case "video-generation", "video-rendering" -> "Video";
            case "content-layout-analysis", "content-math-recognition", "math-speech",
                 "visual-content-description", "content-context-correction",
                 "content-table-analysis" -> "Análisis de contenido";
            default -> "Herramientas";
        };
        return new ManagedComponentDescriptor(descriptor.id().value(), descriptor.displayName(), category,
                descriptor.capability(), descriptor.id(), dependencies, bytes,
                metadataValue(actions, "license"), metadataValue(actions, "source"), actions, metadata);
    }

    private MediaEngine resolve(CapabilityRequirement requirement) {
        if (requirement == null) throw new IllegalArgumentException("capability requirement is required");
        List<? extends MediaEngine> engines = engines(requirement.capability());
        if (requirement.engineId() != null) {
            return engines.stream().filter(engine -> engine.descriptor().id().equals(requirement.engineId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "No hay un motor registrado para " + requirement.engineId().value() + "."));
        }
        return engines.stream().findFirst().orElseThrow(() -> new IllegalArgumentException(
                "No hay motores registrados para " + requirement.capability().value() + "."));
    }

    private List<? extends MediaEngine> engines(CapabilityId capability) {
        if (CapabilityId.VOICE_SYNTHESIS.equals(capability)) return platform.voiceEngines().engines();
        if (CapabilityId.IMAGE_GENERATION.equals(capability)) return platform.imageEngines().engines();
        if (CapabilityId.IMAGE_SUPER_RESOLUTION.equals(capability)) {
            return platform.imageSuperResolutionEngines().engines();
        }
        if (CapabilityId.IMAGE_REFINEMENT.equals(capability)) {
            return platform.imageRefinementEngines().engines();
        }
        if (CapabilityId.VIDEO_GENERATION.equals(capability)) return platform.videoGenerationEngines().engines();
        if (CapabilityId.VIDEO_RENDERING.equals(capability)) return platform.videoRenderEngines().engines();
        if (CapabilityId.CONTENT_LAYOUT_ANALYSIS.equals(capability)
                || CapabilityId.CONTENT_MATH_RECOGNITION.equals(capability)
                || CapabilityId.MATH_SPEECH.equals(capability)
                || CapabilityId.VISUAL_CONTENT_DESCRIPTION.equals(capability)
                || CapabilityId.CONTENT_CONTEXT_CORRECTION.equals(capability)
                || CapabilityId.CONTENT_TABLE_ANALYSIS.equals(capability)) {
            return platform.contentAnalysisEngines().engines().stream()
                    .filter(engine -> capability.equals(engine.descriptor().capability())).toList();
        }
        return List.of();
    }

    private List<MediaEngine> allEngines() {
        ArrayList<MediaEngine> result = new ArrayList<>();
        result.addAll(platform.voiceEngines().engines());
        result.addAll(platform.imageEngines().engines());
        result.addAll(platform.imageSuperResolutionEngines().engines());
        result.addAll(platform.imageRefinementEngines().engines());
        result.addAll(platform.videoGenerationEngines().engines());
        result.addAll(platform.videoRenderEngines().engines());
        result.addAll(platform.contentAnalysisEngines().engines());
        return List.copyOf(result);
    }

    private void validatePublishedActions() {
        for (EngineAdministration administration : platform.administration().entries()) {
            if (allEngines().stream().noneMatch(engine ->
                    engine.descriptor().id().equals(administration.engineId()))) {
                throw new IllegalStateException("Hay acciones publicadas sin motor ejecutable: "
                        + administration.engineId().value());
            }
            for (EngineActionDescriptor action : administration.actions()) {
                if (action.displayName().toLowerCase(java.util.Locale.ROOT).startsWith("instalar")
                        && !EngineActionId.INSTALL.equals(action.id())) {
                    throw new IllegalStateException("La acción visible como Instalar no usa el contrato INSTALL: "
                            + administration.engineId().value() + "/" + action.id().value());
                }
                if (Boolean.parseBoolean(action.metadata().getOrDefault("managedDownload", "false"))
                        && !(administration instanceof ManagedDownloadAdministration)) {
                    throw new IllegalStateException("La descarga visible no publica preflight ni ejecutor protegido: "
                            + administration.engineId().value() + "/" + action.id().value());
                }
            }
        }
    }

    private static String metadataValue(List<EngineActionDescriptor> actions, String key) {
        return actions.stream().map(EngineActionDescriptor::metadata)
                .map(map -> map.getOrDefault(key, "")).filter(value -> !value.isBlank())
                .findFirst().orElse("");
    }
}
