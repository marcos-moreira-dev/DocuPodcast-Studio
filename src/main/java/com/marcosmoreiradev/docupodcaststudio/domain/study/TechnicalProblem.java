package com.marcosmoreiradev.docupodcaststudio.domain.study;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** User-created technical exercise assembled from document blocks plus a draft solution. */
public record TechnicalProblem(
        String id,
        String title,
        List<StudySourceReference> sources,
        String problemText,
        String solutionText,
        String solutionImageAssetId,
        Instant createdAt,
        Instant updatedAt,
        String notes
) {
    public TechnicalProblem {
        id = requireToken(id, "id");
        title = normalize(title);
        if (title.isBlank()) {
            title = id;
        }
        sources = sources == null ? List.of() : sources.stream()
                .map(source -> Objects.requireNonNull(source, "source"))
                .toList();
        problemText = normalize(problemText);
        solutionText = normalize(solutionText);
        solutionImageAssetId = normalize(solutionImageAssetId);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        notes = normalize(notes);
    }

    public TechnicalProblem withUpdatedSolution(String text, String imageAssetId) {
        return new TechnicalProblem(id, title, sources, problemText, text, imageAssetId, createdAt, Instant.now(), notes);
    }

    public TechnicalProblem withUpdatedSolution(String text, String imageAssetId, String notes) {
        return new TechnicalProblem(id, title, sources, problemText, text, imageAssetId, createdAt, Instant.now(), notes);
    }

    private static String requireToken(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
