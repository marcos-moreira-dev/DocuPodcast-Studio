package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

/** Request for generating a short voice test from an editable phrase. */
public record VoiceGeneratedTestRequest(
        String voiceProfileId,
        VoiceReferenceTone tone,
        String phrase,
        AudioEngineDescriptor engineDescriptor
) {
    public static final String DEFAULT_PHRASE = "Esta es una prueba de lectura con la voz seleccionada.";

    public VoiceGeneratedTestRequest {
        voiceProfileId = voiceProfileId == null ? "" : voiceProfileId.strip();
        tone = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        phrase = normalizePhrase(phrase);
        engineDescriptor = engineDescriptor == null ? AudioEngineDescriptor.mock() : engineDescriptor;
    }

    private static String normalizePhrase(String value) {
        String normalized = value == null ? "" : value.strip().replaceAll("\\s+", " ");
        return normalized.isBlank() ? DEFAULT_PHRASE : normalized;
    }
}
