package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import java.util.Objects;

/**
 * Persistable status of one narration segment inside an audio generation job.
 *
 * <p>This is intentionally independent from the current TTS engine. A real XTTS/Piper gateway
 * and the mock gateway can both write the same segment state contract.</p>
 */
public record AudioSegmentSnapshot(
        String segmentId,
        String title,
        AudioSegmentStatus status,
        String audioRelativePath,
        double durationSeconds,
        int attempts,
        String errorMessage,
        AudioSourceFingerprint sourceFingerprint
) {
    public AudioSegmentSnapshot {
        segmentId = token(segmentId, "segmentId");
        title = normalize(title).isBlank() ? segmentId : normalize(title);
        status = Objects.requireNonNullElse(status, AudioSegmentStatus.PENDING);
        audioRelativePath = portableOptionalPath(audioRelativePath);
        if (durationSeconds < 0.0) {
            throw new IllegalArgumentException("durationSeconds must be >= 0");
        }
        attempts = Math.max(0, attempts);
        errorMessage = normalize(errorMessage);
        sourceFingerprint = sourceFingerprint == null ? AudioSourceFingerprint.untraceable() : sourceFingerprint;
    }

    public static AudioSegmentSnapshot pending(String segmentId, String title) {
        return pending(segmentId, title, AudioSourceFingerprint.untraceable());
    }

    public static AudioSegmentSnapshot pending(String segmentId, String title,
                                               AudioSourceFingerprint fingerprint) {
        return new AudioSegmentSnapshot(segmentId, title, AudioSegmentStatus.PENDING, "", 0.0, 0, "", fingerprint);
    }

    public AudioSegmentSnapshot(String segmentId, String title, AudioSegmentStatus status,
                                String audioRelativePath, double durationSeconds, int attempts,
                                String errorMessage) {
        this(segmentId, title, status, audioRelativePath, durationSeconds, attempts,
                errorMessage, AudioSourceFingerprint.untraceable());
    }

    public AudioSegmentSnapshot generating() {
        return new AudioSegmentSnapshot(segmentId, title, AudioSegmentStatus.GENERATING, audioRelativePath, durationSeconds,
                Math.max(1, attempts), errorMessage, sourceFingerprint);
    }

    public AudioSegmentSnapshot completed(String relativeAudioPath, double durationSeconds) {
        return new AudioSegmentSnapshot(segmentId, title, AudioSegmentStatus.COMPLETED, relativeAudioPath, durationSeconds,
                Math.max(1, attempts), "", sourceFingerprint);
    }

    public AudioSegmentSnapshot failed(String errorMessage) {
        return new AudioSegmentSnapshot(segmentId, title, AudioSegmentStatus.FAILED, audioRelativePath, durationSeconds,
                Math.max(1, attempts), errorMessage, sourceFingerprint);
    }

    public AudioSegmentSnapshot cancelled() {
        return new AudioSegmentSnapshot(segmentId, title, AudioSegmentStatus.CANCELLED, audioRelativePath, durationSeconds,
                attempts, errorMessage, sourceFingerprint);
    }

    public AudioSegmentSnapshot skipped(String reason) {
        return new AudioSegmentSnapshot(segmentId, title, AudioSegmentStatus.SKIPPED, "", 0.0,
                attempts, reason, sourceFingerprint);
    }

    public boolean completed() {
        return status == AudioSegmentStatus.COMPLETED;
    }

    public boolean terminalWithoutFailure() {
        return status == AudioSegmentStatus.COMPLETED || status == AudioSegmentStatus.SKIPPED;
    }

    public boolean reusableFor(AudioSourceFingerprint current) {
        return completed() && sourceFingerprint.reusableFor(current);
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
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase().startsWith("file:")) {
            throw new IllegalArgumentException("audioRelativePath must be project-relative: " + value);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
