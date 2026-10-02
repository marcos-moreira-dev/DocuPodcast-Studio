package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** Verified output of one dedicated super-resolution pass. */
public record ImageSuperResolutionResult(
        Path source,
        Path image,
        int width,
        int height,
        String modelName,
        Map<String, String> diagnostics) {
    public ImageSuperResolutionResult {
        if (source == null) throw new IllegalArgumentException("source image is required");
        if (image == null) throw new IllegalArgumentException("result image is required");
        width = Math.max(1, width);
        height = Math.max(1, height);
        modelName = modelName == null ? "" : modelName.strip();
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
