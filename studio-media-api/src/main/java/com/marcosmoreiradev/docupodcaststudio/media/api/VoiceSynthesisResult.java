package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

public record VoiceSynthesisResult(Path audioFile, double durationSeconds, Map<String, String> diagnostics) {
    public VoiceSynthesisResult {
        if (audioFile == null) throw new IllegalArgumentException("audio file is required");
        durationSeconds = Math.max(0.0, durationSeconds);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
