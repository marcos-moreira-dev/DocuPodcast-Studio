package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/** Complete engine platform passed explicitly by the composition root. */
public record MediaEnginePlatform(
        EngineRegistry<VoiceSynthesisEngine> voiceEngines,
        EngineRegistry<ImageGenerationEngine> imageEngines,
        EngineRegistry<VideoGenerationEngine> videoGenerationEngines,
        EngineRegistry<VideoRenderEngine> videoRenderEngines,
        EngineAdministrationRegistry administration) {
    public MediaEnginePlatform {
        voiceEngines = Objects.requireNonNullElseGet(voiceEngines,
                () -> new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS));
        imageEngines = Objects.requireNonNullElseGet(imageEngines,
                () -> new EngineRegistry<>(CapabilityId.IMAGE_GENERATION));
        videoGenerationEngines = Objects.requireNonNullElseGet(videoGenerationEngines,
                () -> new EngineRegistry<>(CapabilityId.VIDEO_GENERATION));
        videoRenderEngines = Objects.requireNonNullElseGet(videoRenderEngines,
                () -> new EngineRegistry<>(CapabilityId.VIDEO_RENDERING));
        administration = Objects.requireNonNullElseGet(administration, EngineAdministrationRegistry::new);
    }

    public MediaEnginePlatform(EngineRegistry<VoiceSynthesisEngine> voiceEngines,
                               EngineRegistry<ImageGenerationEngine> imageEngines,
                               EngineRegistry<VideoRenderEngine> videoRenderEngines) {
        this(voiceEngines, imageEngines, null, videoRenderEngines, null);
    }

    public static MediaEnginePlatform empty() { return new MediaEnginePlatform(null, null, null, null, null); }
}
