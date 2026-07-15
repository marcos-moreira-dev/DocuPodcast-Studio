package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.time.Instant;
import java.util.Objects;

/** A managed or referenced audio sample for a specific voice tone. */
public record VoiceReferenceSample(
        String id,
        String voiceProfileId,
        VoiceReferenceTone tone,
        String fileUri,
        VoiceSampleOrigin origin,
        VoiceFileOwnership ownership,
        long durationMillis,
        Instant createdAt,
        String notes
) {
    public VoiceReferenceSample {
        id = token(id, "id");
        voiceProfileId = token(voiceProfileId, "voiceProfileId");
        tone = Objects.requireNonNullElse(tone, VoiceReferenceTone.NEUTRAL);
        fileUri = requiredText(fileUri, "fileUri");
        origin = Objects.requireNonNullElse(origin, VoiceSampleOrigin.IMPORTED_FILE);
        ownership = Objects.requireNonNullElse(ownership, VoiceFileOwnership.EXTERNAL_REFERENCE);
        durationMillis = Math.max(0, durationMillis);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        notes = normalize(notes);
    }

    public boolean isNeutral() {
        return tone == VoiceReferenceTone.NEUTRAL;
    }

    public boolean canDeleteManagedFile() {
        return ownership.managedDeletable();
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
