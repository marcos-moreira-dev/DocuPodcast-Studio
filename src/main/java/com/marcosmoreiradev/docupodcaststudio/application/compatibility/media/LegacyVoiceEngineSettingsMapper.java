package com.marcosmoreiradev.docupodcaststudio.application.compatibility.media;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;

/** Keeps format-v1 TTS aliases synchronized with neutral engine selections. */
public final class LegacyVoiceEngineSettingsMapper {
    private LegacyVoiceEngineSettingsMapper() { }

    public static OperationalSettings select(OperationalSettings current, EngineDescriptor engine) {
        OperationalSettings safe = current == null ? OperationalSettings.defaults() : current;
        if (engine == null) return safe;
        String legacyMode = engine.diagnosticOnly() ? TtsEngineModes.TEST
                : engine.supports(EngineFeature.REFERENCE_VOICE) ? TtsEngineModes.ADVANCED_AI
                : TtsEngineModes.LOCAL_SIMPLE;
        OperationalSettings.TtsEngineSettings previous = safe.tts();
        OperationalSettings.TtsEngineSettings tts = new OperationalSettings.TtsEngineSettings(
                legacyMode, previous.commandTemplate(), engine.displayName(), previous.language(),
                previous.voiceProfileId(), previous.timeoutSeconds(), previous.maxRetries(),
                previous.xttsDownloadBaseUrl(), previous.piperRuntimeZipUrl(),
                previous.piperDefaultVoiceUrl(), previous.piperDefaultVoiceMetadataUrl());
        OperationalSettings.MediaEngineSelectionSettings selected = safe.mediaEngines();
        return new OperationalSettings(safe.readingDocument(), safe.playbackBuffer(), tts, safe.video(),
                safe.imageGeneration(), new OperationalSettings.MediaEngineSelectionSettings(
                engine.id().value(), selected.imageEngineId(), selected.videoGenerationEngineId(),
                selected.videoRenderEngineId()), safe.frameGeneration(), safe.compute(), safe.ocr(),
                safe.storage(), safe.diagnostics());
    }

    public static OperationalSettings withVoiceProfile(OperationalSettings current, String voiceProfileId) {
        OperationalSettings safe = current == null ? OperationalSettings.defaults() : current;
        OperationalSettings.TtsEngineSettings previous = safe.tts();
        OperationalSettings.TtsEngineSettings tts = new OperationalSettings.TtsEngineSettings(
                previous.engineMode(), previous.commandTemplate(), previous.displayName(), previous.language(),
                voiceProfileId, previous.timeoutSeconds(), previous.maxRetries(), previous.xttsDownloadBaseUrl(),
                previous.piperRuntimeZipUrl(), previous.piperDefaultVoiceUrl(), previous.piperDefaultVoiceMetadataUrl());
        return new OperationalSettings(safe.readingDocument(), safe.playbackBuffer(), tts, safe.video(),
                safe.imageGeneration(), safe.mediaEngines(), safe.frameGeneration(), safe.compute(), safe.ocr(),
                safe.storage(), safe.diagnostics());
    }
}
