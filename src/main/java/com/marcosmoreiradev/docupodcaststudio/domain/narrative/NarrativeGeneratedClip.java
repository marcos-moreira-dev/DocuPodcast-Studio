package com.marcosmoreiradev.docupodcaststudio.domain.narrative;

import java.util.Map;

/** Persisted result of one image-to-video generation step. */
public record NarrativeGeneratedClip(
        String id,
        String assetId,
        int order,
        double durationSeconds,
        String lastFrameAssetId,
        String modelId,
        String workflowId,
        long seed,
        String sourceFingerprint,
        Map<String, String> metadata
) {
    public NarrativeGeneratedClip {
        id = token(id, "id");
        assetId = token(assetId, "assetId");
        order = Math.max(0, order);
        durationSeconds = Math.max(0.0, durationSeconds);
        lastFrameAssetId = optional(lastFrameAssetId);
        modelId = optional(modelId);
        workflowId = optional(workflowId);
        sourceFingerprint = optional(sourceFingerprint);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String token(String value, String field) {
        String normalized = optional(value);
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must be a non-blank token");
        }
        return normalized;
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }
}
