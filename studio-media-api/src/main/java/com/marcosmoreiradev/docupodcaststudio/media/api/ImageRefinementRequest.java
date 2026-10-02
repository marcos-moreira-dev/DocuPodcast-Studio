package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** Provider-neutral request for conservative image-to-image refinement. */
public record ImageRefinementRequest(
        Path source,
        Path outputDirectory,
        String filenamePrefix,
        String prompt,
        EnginePresetId presetId,
        long seed,
        Map<String, String> options) {

    public static final EnginePresetId CONSERVATIVE = new EnginePresetId("conservative");
    public static final EnginePresetId BALANCED = new EnginePresetId("balanced");

    public ImageRefinementRequest {
        if (source == null) throw new IllegalArgumentException("source image is required");
        if (outputDirectory == null) throw new IllegalArgumentException("output directory is required");
        filenamePrefix = filenamePrefix == null || filenamePrefix.isBlank()
                ? "refined" : filenamePrefix.strip();
        prompt = prompt == null ? "" : prompt.strip();
        presetId = presetId == null || EnginePresetId.AUTO.equals(presetId)
                ? CONSERVATIVE : presetId;
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
