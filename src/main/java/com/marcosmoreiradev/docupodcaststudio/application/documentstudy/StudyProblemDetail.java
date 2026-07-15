package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Full read-only problem statement plus editable solution fields for the saved-problem manager. */
public record StudyProblemDetail(
        String id,
        String title,
        List<StudyProblemSourceProjection> sources,
        String problemText,
        String solutionText,
        String solutionImageAssetId,
        Path solutionImagePath,
        Path canvasStatePath,
        Instant createdAt,
        Instant updatedAt,
        String notes
) {
    public StudyProblemDetail {
        id = normalizeRequired(id, "id");
        title = normalize(title);
        if (title.isBlank()) {
            title = id;
        }
        sources = sources == null ? List.of() : List.copyOf(sources);
        problemText = normalize(problemText);
        solutionText = normalize(solutionText);
        solutionImageAssetId = normalize(solutionImageAssetId);
        solutionImagePath = solutionImagePath == null ? null : solutionImagePath.toAbsolutePath().normalize();
        canvasStatePath = canvasStatePath == null ? null : canvasStatePath.toAbsolutePath().normalize();
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        notes = normalize(notes);
    }

    public boolean hasSolutionText() {
        return !solutionText.isBlank();
    }

    public boolean hasSolutionImage() {
        return solutionImagePath != null;
    }

    public boolean hasEditableCanvasState() {
        return canvasStatePath != null;
    }

    public boolean hasSourceCrops() {
        return sources.stream().anyMatch(StudyProblemSourceProjection::hasCropPath);
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
