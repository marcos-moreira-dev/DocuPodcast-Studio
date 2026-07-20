package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Provider-neutral controller for configuration, readiness, presets and maintenance actions. */
public final class EngineAdministrationController {
    private final MediaEnginePlatform platform;

    public EngineAdministrationController(MediaEnginePlatform platform) {
        this.platform = java.util.Objects.requireNonNull(platform, "media engine platform");
    }

    public List<EngineView> engines() {
        ArrayList<MediaEngine> engines = new ArrayList<>();
        engines.addAll(platform.voiceEngines().engines());
        engines.addAll(platform.imageEngines().engines());
        engines.addAll(platform.videoGenerationEngines().engines());
        engines.addAll(platform.videoRenderEngines().engines());
        return engines.stream().map(engine -> new EngineView(engine.descriptor(), engine.configurationSchema(),
                        engine.presets(), platform.administration().find(engine.descriptor().id())
                                .map(EngineAdministration::actions).orElse(List.of())))
                .sorted(Comparator.comparing(view -> view.descriptor().capability().value()
                        + ":" + view.descriptor().displayName())).toList();
    }

    public EngineReadiness readiness(EngineId id) {
        return engine(id).inspectReadiness(null);
    }

    public EngineActionResult execute(EngineId engineId, EngineActionId actionId, Map<String, String> inputs,
                                      ExecutionContext context) throws IOException, InterruptedException {
        return platform.administration().require(engineId)
                .execute(new EngineActionRequest(engineId, actionId, inputs), context);
    }

    private MediaEngine engine(EngineId id) {
        for (MediaEngine engine : allEngines()) if (engine.descriptor().id().equals(id)) return engine;
        throw new IllegalArgumentException("engine not registered: " + id);
    }

    private List<MediaEngine> allEngines() {
        ArrayList<MediaEngine> result = new ArrayList<>();
        result.addAll(platform.voiceEngines().engines());
        result.addAll(platform.imageEngines().engines());
        result.addAll(platform.videoGenerationEngines().engines());
        result.addAll(platform.videoRenderEngines().engines());
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
