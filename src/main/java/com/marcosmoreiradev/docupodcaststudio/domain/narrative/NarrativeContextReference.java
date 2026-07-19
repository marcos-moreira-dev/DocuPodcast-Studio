package com.marcosmoreiradev.docupodcaststudio.domain.narrative;

import java.util.Objects;

/** One project-owned image in the global narrative context bank. */
public record NarrativeContextReference(
        String id,
        NarrativeContextRole role,
        String assetId,
        String displayName,
        boolean enabled,
        double strength,
        String notes
) {
    public NarrativeContextReference {
        id = token(id, "id");
        role = Objects.requireNonNullElse(role, NarrativeContextRole.STYLE);
        assetId = token(assetId, "assetId");
        displayName = optional(displayName);
        if (displayName.isBlank()) {
            displayName = id;
        }
        strength = Math.max(0.0, Math.min(1.0, strength));
        notes = optional(notes);
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
