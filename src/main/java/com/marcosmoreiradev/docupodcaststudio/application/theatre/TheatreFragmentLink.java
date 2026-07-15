package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.Objects;

/** Derived link from a theatre intervention to the canonical fragment projection. */
public record TheatreFragmentLink(
        String interventionId,
        String blockId,
        FragmentId fragmentId,
        String segmentId,
        String sceneId,
        boolean linked
) {
    public TheatreFragmentLink {
        interventionId = token(interventionId);
        blockId = normalizeToken(blockId);
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        segmentId = normalizeToken(segmentId);
        sceneId = normalizeToken(sceneId);
        linked = linked && !blockId.isBlank();
    }

    private static String token(String value) {
        String normalized = normalizeToken(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("interventionId is required");
        }
        return normalized;
    }

    private static String normalizeToken(String value) {
        String normalized = value == null ? "" : value.strip();
        if (!normalized.isBlank() && normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }
}
