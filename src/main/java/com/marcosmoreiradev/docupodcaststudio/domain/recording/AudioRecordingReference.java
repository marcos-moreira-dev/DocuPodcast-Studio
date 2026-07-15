package com.marcosmoreiradev.docupodcaststudio.domain.recording;

import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

import java.time.Instant;

/** Reference to a recorded/imported human audio asset. */
public record AudioRecordingReference(
        String id,
        RecordingPurpose purpose,
        String audioAssetId,
        ScriptTextRange relatedTextRange,
        Instant createdAt,
        String notes
) {
    public AudioRecordingReference {
        id = token(id, "id");
        purpose = purpose == null ? RecordingPurpose.NOTE_OR_REFERENCE : purpose;
        audioAssetId = token(audioAssetId, "audioAssetId");
        createdAt = createdAt == null ? Instant.now() : createdAt;
        notes = notes == null ? "" : notes.strip();
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
}
