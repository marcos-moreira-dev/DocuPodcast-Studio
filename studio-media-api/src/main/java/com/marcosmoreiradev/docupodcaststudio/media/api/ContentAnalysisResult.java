package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

/** Structured provider-neutral result with raw evidence retained. */
public record ContentAnalysisResult(
        String text,
        String structuredJson,
        double confidence,
        List<String> uncertainties,
        Map<String, String> diagnostics
) {
    public ContentAnalysisResult {
        text = text == null ? "" : text.strip();
        structuredJson = structuredJson == null ? "" : structuredJson.strip();
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        uncertainties = uncertainties == null ? List.of() : uncertainties.stream()
                .filter(java.util.Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).toList();
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
