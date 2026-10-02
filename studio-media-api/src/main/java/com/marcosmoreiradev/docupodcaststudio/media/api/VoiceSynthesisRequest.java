package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

public record VoiceSynthesisRequest(
        String text,
        String language,
        String voiceId,
        Path referenceAudio,
        Path outputFile,
        Map<String, String> options) {
    public VoiceSynthesisRequest {
        text = text == null ? "" : text;
        language = language == null || language.isBlank() ? "es" : language.strip();
        voiceId = voiceId == null ? "" : voiceId.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
        if (text.isBlank()) throw new IllegalArgumentException("voice text is required");
        if (outputFile == null) throw new IllegalArgumentException("voice output file is required");
    }
}
