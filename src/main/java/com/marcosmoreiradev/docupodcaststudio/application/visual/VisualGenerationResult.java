package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Auditable result returned by the shared visual-generation infrastructure. */
public record VisualGenerationResult(
        Path outputPath,
        int width,
        int height,
        String promptId,
        VisualGenerationProfile profile,
        String modelId,
        String workflowId,
        VisualComputeBinding computeBinding,
        String precision,
        long seed,
        List<VisualConditioningReference> references,
        Duration elapsed,
        Map<String, String> metadata
) {
    public VisualGenerationResult {
        outputPath = outputPath == null ? null : outputPath.toAbsolutePath().normalize();
        width = Math.max(0, width);
        height = Math.max(0, height);
        promptId = clean(promptId);
        profile = profile == null ? VisualGenerationProfile.DIAGNOSTIC_SD15 : profile;
        modelId = clean(modelId);
        workflowId = clean(workflowId);
        precision = clean(precision);
        seed = Math.max(0L, seed);
        references = references == null ? List.of() : List.copyOf(references);
        elapsed = elapsed == null ? Duration.ZERO : elapsed;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
