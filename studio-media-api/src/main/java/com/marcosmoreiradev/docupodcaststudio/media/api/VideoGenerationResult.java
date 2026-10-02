package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** Generated clip plus optional continuation frame for chained production. */
public record VideoGenerationResult(
        Path videoFile,
        Path continuationFrame,
        double durationSeconds,
        Map<String, String> diagnostics) {
    public VideoGenerationResult {
        if (videoFile == null) throw new IllegalArgumentException("generated video file is required");
        videoFile = videoFile.toAbsolutePath().normalize();
        continuationFrame = continuationFrame == null ? null : continuationFrame.toAbsolutePath().normalize();
        durationSeconds = Math.max(0.0, durationSeconds);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
