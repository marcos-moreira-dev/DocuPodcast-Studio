package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

public record VoiceSynthesisBatchResult(
        List<VoiceSynthesisResult> units,
        Map<String, String> diagnostics) {
    public VoiceSynthesisBatchResult {
        units = units == null ? List.of() : List.copyOf(units);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
