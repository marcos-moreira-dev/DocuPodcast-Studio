package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

public record EngineActionResult(
        boolean success,
        String message,
        EngineReadiness readiness,
        List<GenerationArtifact> artifacts,
        Map<String, String> diagnostics) {
    public EngineActionResult {
        message = message == null ? "" : message.strip();
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
