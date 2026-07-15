package com.marcosmoreiradev.docupodcaststudio.domain.process;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Cross-engine job snapshot used to present and reason about long-running local processes.
 *
 * <p>T94 intentionally does not replace the mature audio job storage. It introduces a common
 * facade so TTS, media preparation, engine setup and future video render jobs can converge on one
 * progress/cancellation/log contract.</p>
 */
public record ProcessJobSnapshot(
        String jobId,
        ProcessJobKind kind,
        String displayName,
        ProcessJobState state,
        ProcessJobStage stage,
        double progress,
        String currentItemId,
        String currentItemLabel,
        long estimatedRemainingSeconds,
        String message,
        String jobRelativeDirectory,
        boolean cancellable,
        boolean recoverable,
        List<ProcessJobArtifact> artifacts,
        ProcessJobLogReference logs,
        Instant createdAt,
        Instant updatedAt
) {
    public ProcessJobSnapshot {
        jobId = token(jobId, "jobId");
        kind = Objects.requireNonNull(kind, "kind");
        displayName = normalize(displayName).isBlank() ? kind.displayName() : normalize(displayName);
        state = Objects.requireNonNullElse(state, ProcessJobState.IDLE);
        stage = Objects.requireNonNullElse(stage, ProcessJobStage.NONE);
        progress = clamp(progress);
        currentItemId = normalize(currentItemId);
        currentItemLabel = normalize(currentItemLabel);
        estimatedRemainingSeconds = Math.max(0L, estimatedRemainingSeconds);
        message = normalize(message);
        jobRelativeDirectory = portableOptionalPath(jobRelativeDirectory);
        cancellable = cancellable && state.cancellable();
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        logs = logs == null ? new ProcessJobLogReference("", "", "", "") : logs;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public boolean running() {
        return state.running();
    }

    public boolean terminal() {
        return state.terminal();
    }

    public String progressLabel() {
        return Math.round(progress * 100.0) + "%";
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
            throw new IllegalArgumentException("Job path must be project-relative: " + value);
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
