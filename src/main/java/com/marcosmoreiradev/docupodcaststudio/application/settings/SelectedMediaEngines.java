package com.marcosmoreiradev.docupodcaststudio.application.settings;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;

/** Provider-neutral projection of legacy operational settings during format-v1 migration. */
public record SelectedMediaEngines(EngineId voice, EngineId image, EngineId videoRender) {
    public static SelectedMediaEngines from(OperationalSettings settings) {
        OperationalSettings safe = settings == null ? OperationalSettings.defaults() : settings;
        String image = "managed-local".equalsIgnoreCase(safe.imageGeneration().engineMode())
                ? "comfyui" : safe.imageGeneration().engineMode();
        return new SelectedMediaEngines(
                new EngineId(safe.tts().engineMode()),
                new EngineId(image),
                new EngineId("ffmpeg"));
    }
}
