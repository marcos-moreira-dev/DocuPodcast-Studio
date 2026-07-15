package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.List;

/** Scene-level theatre readiness derived from the theatre layer and fragment state. */
public record TheatreProductionScene(
        String sceneId,
        String displayName,
        String actId,
        int interventionCount,
        int audioReadyCount,
        int visualReadyCount,
        boolean hasSpatialMapAsset,
        boolean hasTextPlacements,
        boolean hasPositions,
        boolean hasActions,
        List<String> diagnostics
) {
    public TheatreProductionScene {
        sceneId = token(sceneId, "sceneId");
        displayName = normalize(displayName).isBlank() ? sceneId : normalize(displayName);
        actId = normalizeToken(actId);
        interventionCount = Math.max(0, interventionCount);
        audioReadyCount = Math.max(0, audioReadyCount);
        visualReadyCount = Math.max(0, visualReadyCount);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    private static String token(String value, String field) {
        String normalized = normalizeToken(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
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
