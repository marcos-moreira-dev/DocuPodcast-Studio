package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.util.Map;

/** Character or narrator role used by the script. */
public record CharacterProfile(
        String id,
        String displayName,
        String defaultVoiceProfileId,
        String defaultPerformanceStyleId,
        String description,
        Map<String, String> metadata
) {
    public CharacterProfile {
        id = token(id, "id");
        displayName = requiredText(displayName, "displayName");
        defaultVoiceProfileId = tokenOrDefault(defaultVoiceProfileId, "VOC-NARRATOR");
        defaultPerformanceStyleId = tokenOrDefault(defaultPerformanceStyleId, "STY-NEUTRAL");
        description = normalize(description);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static CharacterProfile narrator() {
        return new CharacterProfile(
                "CHR-NARRATOR",
                "Narrador",
                "VOC-NARRATOR",
                "STY-NEUTRAL",
                "Voz neutral por defecto para documentos académicos y narración general.",
                Map.of("builtIn", "true")
        );
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String tokenOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }

    private static String requiredText(String value, String field) {
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
