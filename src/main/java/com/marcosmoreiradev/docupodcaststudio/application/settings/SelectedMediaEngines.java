package com.marcosmoreiradev.docupodcaststudio.application.settings;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.application.compatibility.media.LegacyEngineAliases;

/** Provider-neutral projection of legacy operational settings during format-v1 migration. */
public record SelectedMediaEngines(EngineId voice, EngineId image,
                                   EngineId videoGeneration, EngineId videoRender) {
    public static SelectedMediaEngines from(OperationalSettings settings) {
        OperationalSettings safe = settings == null ? OperationalSettings.defaults() : settings;
        OperationalSettings.MediaEngineSelectionSettings selected = safe.mediaEngines();
        return new SelectedMediaEngines(
                id(or(selected.voiceEngineId(), safe.tts().engineMode())),
                id(or(selected.imageEngineId(), LegacyEngineAliases.imageEngine(safe.imageGeneration().engineMode()))),
                id(or(selected.videoGenerationEngineId(), LegacyEngineAliases.videoGenerationEngine())),
                id(or(selected.videoRenderEngineId(), LegacyEngineAliases.videoRenderEngine())));
    }

    private static EngineId id(String value) { return value == null || value.isBlank() ? null : new EngineId(value); }
    private static String or(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
