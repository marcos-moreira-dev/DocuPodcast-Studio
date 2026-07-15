package com.marcosmoreiradev.docupodcaststudio.domain.script;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Narratable unit generated from one or more imported document blocks.
 *
 * <p>Audio generation will use these segments as the stable unit for WAV clips,
 * retries and playback highlighting.</p>
 */
public record NarrationSegment(
        String id,
        NarrationSegmentType type,
        String title,
        String narrationText,
        List<String> sourceBlockIds,
        String characterId,
        String voiceProfileId,
        String performanceStyleId,
        Map<String, String> metadata
) {
    public NarrationSegment {
        id = requireToken(id, "id");
        type = Objects.requireNonNullElse(type, NarrationSegmentType.PARAGRAPH);
        title = title == null ? "" : title.strip();
        narrationText = narrationText == null ? "" : narrationText.strip();
        sourceBlockIds = sourceBlockIds == null ? List.of() : List.copyOf(sourceBlockIds);
        characterId = normalizeTokenOrDefault(characterId, "CHR-NARRATOR");
        voiceProfileId = normalizeTokenOrDefault(voiceProfileId, "VOC-NARRATOR");
        performanceStyleId = normalizeTokenOrDefault(performanceStyleId, "STY-NEUTRAL");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static NarrationSegment of(String id, NarrationSegmentType type, String title, String narrationText, List<String> sourceBlockIds) {
        return new NarrationSegment(id, type, title, narrationText, sourceBlockIds, "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL", Map.of());
    }

    public boolean narratable() {
        return !narrationText.isBlank();
    }

    public int characterCount() {
        return narrationText.length();
    }

    public int wordCount() {
        if (narrationText.isBlank()) {
            return 0;
        }
        return narrationText.split("\\s+").length;
    }

    public String preview(int maxCharacters) {
        if (narrationText.length() <= maxCharacters) {
            return narrationText;
        }
        return narrationText.substring(0, Math.max(0, maxCharacters)).strip() + "…";
    }

    public NarrationSegment withNarrationText(String nextText) {
        return new NarrationSegment(id, type, title, nextText, sourceBlockIds, characterId, voiceProfileId, performanceStyleId, metadata);
    }

    public NarrationSegment withVoice(String nextCharacterId, String nextVoiceProfileId, String nextStyleId) {
        return new NarrationSegment(id, type, title, narrationText, sourceBlockIds,
                nextCharacterId, nextVoiceProfileId, nextStyleId, metadata);
    }

    private static String requireToken(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalizeTokenOrDefault(String value, String defaultValue) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return defaultValue;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("token must not contain whitespace: " + normalized);
        }
        return normalized;
    }
}
