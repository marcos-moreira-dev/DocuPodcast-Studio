package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

/** Creates updated operational settings that select the simple local voice as the lightweight TTS engine. */
public final class SelectPiperAsEngineUseCase {
    public OperationalSettings select(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        String voice = current.tts().voiceProfileId();
        if (voice == null || voice.isBlank() || "VOC-NARRATOR".equalsIgnoreCase(voice) || voice.endsWith(".onnx")) {
            voice = "voz-local-simple";
        }
        return new OperationalSettings(
                current.readingDocument(),
                current.playbackBuffer(),
                new OperationalSettings.TtsEngineSettings(
                        "piper",
                        "",
                        "Voz local simple — lectura liviana",
                        current.tts().language(),
                        voice,
                        current.tts().timeoutSeconds(),
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
