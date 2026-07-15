package com.marcosmoreiradev.docupodcaststudio.domain.study;

/** One project-owned soundtrack item. List order defines playlist order. */
public record DocumentStudyMusicTrack(
        String id,
        String assetId,
        double durationSeconds,
        double volume
) {
    public DocumentStudyMusicTrack {
        id = token(id, "id");
        assetId = token(assetId, "assetId");
        durationSeconds = Double.isFinite(durationSeconds) ? Math.max(0.0, durationSeconds) : 0.0;
        volume = Double.isFinite(volume) ? Math.max(0.0, Math.min(1.0, volume)) : 0.20;
    }

    public DocumentStudyMusicTrack withVolume(double nextVolume) {
        return new DocumentStudyMusicTrack(id, assetId, durationSeconds, nextVolume);
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " is required and must not contain whitespace");
        }
        return normalized;
    }
}
