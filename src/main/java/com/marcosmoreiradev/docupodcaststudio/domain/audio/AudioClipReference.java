package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import java.util.Objects;

/** Reference to a generated audio clip associated with one narration segment. */
public record AudioClipReference(
        String id,
        String segmentId,
        String relativePath,
        double durationSeconds,
        int characterCount
) {
    public AudioClipReference {
        id = token(id, "id");
        segmentId = token(segmentId, "segmentId");
        relativePath = portablePath(relativePath);
        if (durationSeconds < 0.0) {
            throw new IllegalArgumentException("durationSeconds must be >= 0");
        }
        if (characterCount < 0) {
            throw new IllegalArgumentException("characterCount must be >= 0");
        }
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String portablePath(String value) {
        String normalized = Objects.requireNonNull(value, "relativePath").replace('\\', '/').strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("relativePath is required");
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.contains("../")
                || normalized.startsWith("../") || normalized.startsWith("./") || normalized.contains("://")) {
            throw new IllegalArgumentException("relativePath must be portable and relative: " + value);
        }
        return normalized;
    }
}
