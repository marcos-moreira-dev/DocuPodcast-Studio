package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.time.Instant;
import java.util.List;

/** Compact row for the saved technical-problems list. */
public record StudyProblemListItem(
        String id,
        String title,
        String preview,
        int sourceCount,
        List<String> sourcePages,
        String firstSourceBlockId,
        String firstSourcePage,
        String solutionPreview,
        String notesPreview,
        boolean hasSolutionText,
        boolean hasSolutionImage,
        boolean hasSourceCrops,
        Instant createdAt,
        Instant updatedAt
) {
    public StudyProblemListItem(
            String id,
            String title,
            String preview,
            int sourceCount,
            boolean hasSolutionText,
            boolean hasSolutionImage,
            boolean hasSourceCrops,
            Instant createdAt,
            Instant updatedAt) {
        this(id, title, preview, sourceCount, List.of(), "", "", "", "",
                hasSolutionText, hasSolutionImage, hasSourceCrops, createdAt, updatedAt);
    }

    public StudyProblemListItem {
        id = normalizeRequired(id, "id");
        title = normalize(title);
        if (title.isBlank()) {
            title = id;
        }
        preview = normalize(preview);
        sourceCount = Math.max(0, sourceCount);
        sourcePages = sourcePages == null ? List.of() : sourcePages.stream()
                .map(StudyProblemListItem::normalize)
                .filter(page -> !page.isBlank())
                .distinct()
                .toList();
        firstSourceBlockId = normalize(firstSourceBlockId);
        firstSourcePage = normalize(firstSourcePage);
        solutionPreview = normalize(solutionPreview);
        notesPreview = normalize(notesPreview);
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public String badges() {
        StringBuilder builder = new StringBuilder(sourceCount + " fuente(s)");
        if (hasSourceCrops) {
            builder.append(" · crops");
        }
        if (hasSolutionText) {
            builder.append(" · texto");
        }
        if (hasSolutionImage) {
            builder.append(" · lienzo");
        }
        return builder.toString();
    }

    public String sourcePageSummary() {
        if (sourcePages.isEmpty()) {
            return "";
        }
        if (sourcePages.size() == 1) {
            return "p. " + sourcePages.getFirst();
        }
        return "p. " + sourcePages.getFirst() + "-" + sourcePages.getLast();
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
