package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

public record VoiceSynthesisBatchRequest(
        List<VoiceSynthesisUnit> units,
        String language,
        Map<String, String> options) {
    public VoiceSynthesisBatchRequest {
        units = units == null ? List.of() : List.copyOf(units);
        language = language == null || language.isBlank() ? "es" : language.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
        if (units.isEmpty()) throw new IllegalArgumentException("voice synthesis units are required");
    }
}
