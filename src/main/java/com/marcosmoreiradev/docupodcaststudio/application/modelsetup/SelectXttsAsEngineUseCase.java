package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

/** Creates updated operational settings that select the advanced AI voice as the high-quality TTS engine. */
public final class SelectXttsAsEngineUseCase {
    public OperationalSettings select(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return new OperationalSettings(
                current.readingDocument(),
                current.playbackBuffer(),
                new OperationalSettings.TtsEngineSettings(
                        "xtts",
                        "",
                        "Voz IA avanzada — lectura de calidad alta",
                        current.tts().language(),
                        current.tts().voiceProfileId(),
                        Math.max(current.tts().timeoutSeconds(), 900),
                        current.tts().maxRetries(),
                        current.tts().xttsDownloadBaseUrl(),
                        current.tts().piperRuntimeZipUrl(),
                        current.tts().piperDefaultVoiceUrl(),
                        current.tts().piperDefaultVoiceMetadataUrl()),
                current.video(),
                current.imageGeneration(),
                current.frameGeneration(),
                current.compute(),
                current.ocr(),
                current.storage(),
                current.diagnostics());
    }
}
