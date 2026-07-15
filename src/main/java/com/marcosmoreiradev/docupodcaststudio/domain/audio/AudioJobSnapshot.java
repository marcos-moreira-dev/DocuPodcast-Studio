package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Persistable snapshot of an audio job.
 *
 * <p>The snapshot is stored in jobs/JOB-xxx/job.json and accompanied by segments-status.json.
 * It allows the application to reopen the project and know what was completed, cancelled,
 * failed or ready to retry.</p>
 */
public record AudioJobSnapshot(
        String jobId,
        String documentName,
        AudioJobState state,
        AudioGenerationStage stage,
        int completedSegments,
        int totalSegments,
        int failedSegments,
        double progress,
        String currentSegmentId,
        String currentSegmentTitle,
        long estimatedRemainingSeconds,
        String message,
        String jobRelativeDirectory,
        String finalAudioPath,
        String manifestPath,
        List<AudioSegmentSnapshot> segments,
        Instant createdAt,
        Instant updatedAt
) {
    public AudioJobSnapshot {
        jobId = token(jobId, "jobId");
        documentName = normalize(documentName).isBlank() ? "Proyecto DocuPodcast" : normalize(documentName);
        state = Objects.requireNonNullElse(state, AudioJobState.IDLE);
        stage = Objects.requireNonNullElse(stage, AudioGenerationStage.NONE);
        completedSegments = Math.max(0, completedSegments);
        totalSegments = Math.max(0, totalSegments);
        failedSegments = Math.max(0, failedSegments);
        progress = clamp(progress);
        currentSegmentId = normalize(currentSegmentId);
        currentSegmentTitle = normalize(currentSegmentTitle);
        estimatedRemainingSeconds = Math.max(0L, estimatedRemainingSeconds);
        message = normalize(message);
        jobRelativeDirectory = portablePath(jobRelativeDirectory, "jobRelativeDirectory");
        finalAudioPath = portableOptionalPath(finalAudioPath);
        manifestPath = portableOptionalPath(manifestPath);
        segments = segments == null ? List.of() : List.copyOf(segments);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        validateSegments(segments);
        if (totalSegments == 0 && !segments.isEmpty()) {
            totalSegments = segments.size();
        }
    }

    public boolean resumable() {
        return recoverableSegments() > 0 && completedSegments() < totalSegments();
    }

    public int pendingSegments() {
        return (int) segments.stream().filter(segment -> segment.status() == AudioSegmentStatus.PENDING).count();
    }

    public int failedSegmentCount() {
        return (int) segments.stream().filter(segment -> segment.status() == AudioSegmentStatus.FAILED).count();
    }

    public int cancelledSegmentCount() {
        return (int) segments.stream().filter(segment -> segment.status() == AudioSegmentStatus.CANCELLED).count();
    }

    public int recoverableSegments() {
        return (int) segments.stream().filter(segment -> segment.status() == AudioSegmentStatus.PENDING
                || segment.status() == AudioSegmentStatus.FAILED
                || segment.status() == AudioSegmentStatus.CANCELLED
                || segment.status() == AudioSegmentStatus.GENERATING).count();
    }

    public String recoveryLabel() {
        if (!resumable()) {
            return "No requiere reanudación";
        }
        return "Reanudable: " + recoverableSegments() + " segmentos pendientes/fallidos/cancelados";
    }

    private static void validateSegments(List<AudioSegmentSnapshot> segments) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (AudioSegmentSnapshot segment : segments) {
            if (!ids.add(segment.segmentId())) {
                throw new IllegalArgumentException("Segmento duplicado en job snapshot: " + segment.segmentId());
            }
        }
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

    private static String portablePath(String value, String field) {
        String normalized = portableOptionalPath(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
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
            throw new IllegalArgumentException("Path must be project-relative: " + value);
        }
        return normalized;
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
