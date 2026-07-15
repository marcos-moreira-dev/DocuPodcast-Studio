package com.marcosmoreiradev.docupodcaststudio.domain.playback;

/**
 * Time cue connecting synchronized playback to one narration segment, one render unit,
 * one audio clip and an optional storyboard image.
 */
public record PlaybackCue(
        String segmentId,
        String unitId,
        double startSeconds,
        double endSeconds,
        String audioClipId,
        String audioRelativePath,
        String imageAssetId,
        String title,
        String spokenText,
        PlaybackCueKind cueKind
) {
    public PlaybackCue {
        segmentId = token(segmentId, "segmentId");
        unitId = normalize(unitId);
        if (unitId.isBlank()) {
            unitId = segmentId;
        }
        if (unitId.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("unitId must not contain whitespace");
        }
        startSeconds = Math.max(0.0, startSeconds);
        if (endSeconds < startSeconds) {
            throw new IllegalArgumentException("endSeconds must be >= startSeconds");
        }
        audioClipId = normalize(audioClipId);
        audioRelativePath = portableOptionalPath(audioRelativePath);
        imageAssetId = normalize(imageAssetId);
        title = title == null || title.isBlank() ? unitId : title.strip();
        spokenText = spokenText == null || spokenText.isBlank() ? title : spokenText.strip();
        cueKind = cueKind == null ? PlaybackCueKind.NARRATION : cueKind;
    }

    public PlaybackCue(String segmentId,
                       String unitId,
                       double startSeconds,
                       double endSeconds,
                       String audioClipId,
                       String audioRelativePath,
                       String imageAssetId,
                       String title,
                       String spokenText) {
        this(segmentId, unitId, startSeconds, endSeconds, audioClipId, audioRelativePath, imageAssetId,
                title, spokenText, PlaybackCueKind.NARRATION);
    }

    public PlaybackCue(String segmentId,
                       String unitId,
                       double startSeconds,
                       double endSeconds,
                       String audioClipId,
                       String audioRelativePath,
                       String imageAssetId,
                       String title) {
        this(segmentId, unitId, startSeconds, endSeconds, audioClipId, audioRelativePath, imageAssetId, title, title);
    }

    /** Backwards-compatible constructor for segment-level cues. */
    public PlaybackCue(String segmentId, double startSeconds, double endSeconds,
                       String audioClipId, String audioRelativePath, String imageAssetId, String title) {
        this(segmentId, segmentId, startSeconds, endSeconds, audioClipId, audioRelativePath, imageAssetId, title);
    }


    /** Backwards-compatible constructor used by earlier tests and simple cues. */
    public PlaybackCue(String segmentId, double startSeconds, double endSeconds, String audioClipId, String imageAssetId) {
        this(segmentId, segmentId, startSeconds, endSeconds, audioClipId, "", imageAssetId, segmentId);
    }

    public double durationSeconds() {
        return Math.max(0.0, endSeconds - startSeconds);
    }

    public boolean contains(double positionSeconds) {
        return positionSeconds >= startSeconds && positionSeconds <= endSeconds;
    }

    public double relativePosition(double absolutePositionSeconds) {
        return Math.max(0.0, absolutePositionSeconds - startSeconds);
    }

    public boolean hasAudio() {
        return !audioRelativePath.isBlank();
    }

    public boolean hasImage() {
        return !imageAssetId.isBlank();
    }

    public boolean structuralText() {
        return cueKind.structuralText();
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

    private static String portableOptionalPath(String value) {
        String normalized = normalize(value).replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")) {
            throw new IllegalArgumentException("audioRelativePath must be portable and relative: " + value);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
