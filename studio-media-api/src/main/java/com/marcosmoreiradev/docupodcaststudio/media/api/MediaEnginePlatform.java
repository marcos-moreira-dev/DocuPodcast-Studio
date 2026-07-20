package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/** Complete engine platform passed explicitly by the composition root. */
public record MediaEnginePlatform(
        EngineRegistry<VoiceSynthesisEngine> voiceEngines,
        EngineRegistry<ImageGenerationEngine> imageEngines,
        EngineRegistry<VideoRenderEngine> videoRenderEngines) {
    public MediaEnginePlatform {
        voiceEngines = Objects.requireNonNullElseGet(voiceEngines,
                () -> new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS));
        imageEngines = Objects.requireNonNullElseGet(imageEngines,
                () -> new EngineRegistry<>(CapabilityId.IMAGE_GENERATION));
        videoRenderEngines = Objects.requireNonNullElseGet(videoRenderEngines,
                () -> new EngineRegistry<>(CapabilityId.VIDEO_RENDERING));
    }

    public static MediaEnginePlatform empty() { return new MediaEnginePlatform(null, null, null); }
}
