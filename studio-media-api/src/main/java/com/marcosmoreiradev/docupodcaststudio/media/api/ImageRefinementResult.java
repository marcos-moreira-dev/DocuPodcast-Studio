package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** Verified result of an existing-image refinement pass. */
public record ImageRefinementResult(
        Path source,
        Path image,
        int width,
        int height,
        boolean refined,
        String warning,
        EnginePresetId presetId,
        Map<String, String> diagnostics) {
    public ImageRefinementResult {
        if (source == null) throw new IllegalArgumentException("source image is required");
        if (image == null) throw new IllegalArgumentException("result image is required");
        width = Math.max(1, width);
        height = Math.max(1, height);
        warning = warning == null ? "" : warning.strip();
        presetId = presetId == null ? ImageRefinementRequest.CONSERVATIVE : presetId;
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
