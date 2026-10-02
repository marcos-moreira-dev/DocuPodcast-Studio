package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cross-product request. A PDF page, theatre frame, imported image, or future
 * module can provide evidence without leaking its domain types to the engine.
 */
public record ContentAnalysisRequest(
        ContentAnalysisOperation operation,
        List<AnalysisVisualInput> visualInputs,
        String instruction,
        String nearbyContext,
        String language,
        String responseSchema,
        Map<String, String> options
) {
    public ContentAnalysisRequest {
        operation = Objects.requireNonNull(operation, "operation");
        visualInputs = visualInputs == null ? List.of() : List.copyOf(visualInputs);
        instruction = instruction == null ? "" : instruction.strip();
        nearbyContext = nearbyContext == null ? "" : nearbyContext.strip();
        language = language == null || language.isBlank() ? "und" : language.strip();
        responseSchema = responseSchema == null ? "" : responseSchema.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
        if (instruction.isBlank()) {
            throw new IllegalArgumentException("analysis instruction is required");
        }
        if ((operation == ContentAnalysisOperation.PAGE_SEMANTIC_READING
                || operation == ContentAnalysisOperation.IMAGE_DESCRIPTION
                || operation == ContentAnalysisOperation.IMAGE_QUALITY_REVIEW
                || operation == ContentAnalysisOperation.LAYOUT_ANALYSIS
                || operation == ContentAnalysisOperation.TABLE_STRUCTURE_RECOGNITION
                || operation == ContentAnalysisOperation.MATH_RECOGNITION)
                && visualInputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "visual input is required for " + operation);
        }
    }
}
