package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Result returned by a category-neutral visual clip workflow. */
public record VisualClipGenerationResult(
        Path clipPath,
        Path lastFramePath,
        int width,
        int height,
        int framesPerSecond,
        double durationSeconds,
        String modelId,
        String workflowId,
        String promptId,
        VisualComputeBinding computeBinding,
        long seed,
        Duration elapsed,
        Map<String, String> metadata
) {
    public VisualClipGenerationResult {
        clipPath = Objects.requireNonNull(clipPath, "clipPath").toAbsolutePath().normalize();
        lastFramePath = lastFramePath == null ? null : lastFramePath.toAbsolutePath().normalize();
        width = Math.max(1, width);
        height = Math.max(1, height);
        framesPerSecond = Math.max(1, framesPerSecond);
        durationSeconds = Math.max(0.0, durationSeconds);
        modelId = optional(modelId);
        workflowId = optional(workflowId);
        promptId = optional(promptId);
        computeBinding = Objects.requireNonNull(computeBinding, "computeBinding");
        elapsed = Objects.requireNonNullElse(elapsed, Duration.ZERO);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }
}
