package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

public record VideoRenderResult(Path videoFile, double durationSeconds, Map<String, String> diagnostics) {
    public VideoRenderResult {
        if (videoFile == null) throw new IllegalArgumentException("video file is required");
        durationSeconds = Math.max(0.0, durationSeconds);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
