package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.nio.file.Path;

/** Read-only source captured for a saved technical problem. */
public record StudyProblemSourceProjection(
        String blockId,
        String selectedText,
        String sourcePage,
        String bbox,
        String sourceCropAssetId,
        Path sourceCropPath
) {
    public StudyProblemSourceProjection {
        blockId = normalizeRequired(blockId, "blockId");
        selectedText = normalize(selectedText);
        sourcePage = normalize(sourcePage);
        bbox = normalize(bbox);
        sourceCropAssetId = normalize(sourceCropAssetId);
        sourceCropPath = sourceCropPath == null ? null : sourceCropPath.toAbsolutePath().normalize();
    }

    public boolean hasCropPath() {
        return sourceCropPath != null;
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
