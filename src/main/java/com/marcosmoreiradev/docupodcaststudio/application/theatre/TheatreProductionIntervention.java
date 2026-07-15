package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.List;
import java.util.Objects;

/** Theatre intervention enriched with fragment, audio, visual and scene state. */
public record TheatreProductionIntervention(
        String interventionId,
        int sequenceIndex,
        String blockId,
        FragmentId fragmentId,
        String segmentId,
        String sceneId,
        String characterId,
        String characterName,
        String textPreview,
        boolean fragmentLinked,
        boolean audioReady,
        boolean visualReady,
        boolean hasTextPlacement,
        boolean hasPosition,
        boolean hasAction,
        List<String> diagnostics
) {
    public TheatreProductionIntervention {
        interventionId = token(interventionId);
        sequenceIndex = Math.max(1, sequenceIndex);
        blockId = normalizeToken(blockId);
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        segmentId = normalizeToken(segmentId);
        sceneId = normalizeToken(sceneId);
        characterId = normalizeToken(characterId);
        characterName = normalize(characterName);
        textPreview = normalize(textPreview);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    private static String token(String value) {
        String normalized = normalizeToken(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("interventionId is required");
        }
        return normalized;
    }

    private static String normalizeToken(String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank() && normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
