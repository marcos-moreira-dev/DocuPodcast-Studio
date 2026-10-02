package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.media.administration.CapabilityAdministrationService;
import com.marcosmoreiradev.docupodcaststudio.application.media.administration.CapabilityRequirement;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Provider-neutral controller for configuration, readiness, presets and maintenance actions. */
public final class EngineAdministrationController {
    private final CapabilityAdministrationService capabilities;

    public EngineAdministrationController(MediaEnginePlatform platform) {
        this(new CapabilityAdministrationService(platform));
    }

    public EngineAdministrationController(CapabilityAdministrationService capabilities) {
        this.capabilities = java.util.Objects.requireNonNull(capabilities,
                "capability administration service");
    }

    public List<EngineView> engines() {
        MediaEnginePlatform platform = capabilities.platform();
        ArrayList<MediaEngine> engines = new ArrayList<>();
        engines.addAll(platform.voiceEngines().engines());
        engines.addAll(platform.imageEngines().engines());
        engines.addAll(platform.imageSuperResolutionEngines().engines());
        engines.addAll(platform.imageRefinementEngines().engines());
        engines.addAll(platform.videoGenerationEngines().engines());
        engines.addAll(platform.videoRenderEngines().engines());
        engines.addAll(platform.contentAnalysisEngines().engines());
        return engines.stream().map(engine -> new EngineView(engine.descriptor(), engine.configurationSchema(),
                        engine.presets(), platform.administration().find(engine.descriptor().id())
                                .map(EngineAdministration::actions).orElse(List.of())))
                .sorted(Comparator.comparing(view -> view.descriptor().capability().value()
                        + ":" + view.descriptor().displayName())).toList();
    }

    public EngineReadiness readiness(EngineId id) {
        MediaEngine engine = engine(id);
        var report = capabilities.inspect(new CapabilityRequirement(
                engine.descriptor().capability(), id, null, null, Map.of()));
        EngineReadiness original = engine.inspectReadiness(null);
        return new EngineReadiness(id, original.state(), report.summary(), report.issues(),
                report.recommendedActions(), report.technicalDetails());
    }

    public EngineActionResult execute(EngineId engineId, EngineActionId actionId, Map<String, String> inputs,
                                      ExecutionContext context) throws IOException, InterruptedException {
        return capabilities.execute(new EngineActionRequest(engineId, actionId, inputs), context);
    }

    public boolean isManagedDownload(EngineId engineId, EngineActionId actionId) {
        return capabilities.isManagedDownload(engineId, actionId);
    }

    public ManagedDownloadPreflight inspectDownload(
            EngineId engineId,
            EngineActionId actionId,
            Map<String, String> inputs) throws IOException {
        return capabilities.inspectDownload(new EngineActionRequest(engineId, actionId, inputs));
    }

    public EngineActionResult executeDownload(
            EngineId engineId,
            EngineActionId actionId,
            Map<String, String> inputs,
            ManagedDownloadDecision decision,
            ExecutionContext context) throws IOException, InterruptedException {
        return capabilities.executeDownload(
                new EngineActionRequest(engineId, actionId, inputs), decision, context);
    }

    public ComputeQueueSnapshot computeQueue() {
        return capabilities.computeQueue();
    }

    public boolean cancelCompute(String admissionId) {
        return capabilities.cancelCompute(admissionId);
    }

    private MediaEngine engine(EngineId id) {
        for (MediaEngine engine : allEngines()) if (engine.descriptor().id().equals(id)) return engine;
        throw new IllegalArgumentException("engine not registered: " + id);
    }

    private List<MediaEngine> allEngines() {
        MediaEnginePlatform platform = capabilities.platform();
        ArrayList<MediaEngine> result = new ArrayList<>();
        result.addAll(platform.voiceEngines().engines());
        result.addAll(platform.imageEngines().engines());
        result.addAll(platform.imageSuperResolutionEngines().engines());
        result.addAll(platform.imageRefinementEngines().engines());
        result.addAll(platform.videoGenerationEngines().engines());
        result.addAll(platform.videoRenderEngines().engines());
        result.addAll(platform.contentAnalysisEngines().engines());
        return result;
    }

    public record EngineView(EngineDescriptor descriptor, EngineConfigurationSchema configuration,
                             List<EnginePresetDescriptor> presets, List<EngineActionDescriptor> actions) {
        public EngineView {
            presets = presets == null ? List.of() : List.copyOf(presets);
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }
}
