package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;

import java.util.Locale;
import java.util.Objects;

/** DTO consumed by the UI to render progress, current segment and ETA. */
public record AudioJobStatusDto(
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
        long estimatedRemainingErrorSeconds,
        String message,
        String outputDirectory,
        String finalAudioPath,
        String manifestPath
) {
    public AudioJobStatusDto {
        jobId = normalize(jobId);
        documentName = blankTo(documentName, "Proyecto DocuPodcast");
        state = Objects.requireNonNullElse(state, AudioJobState.IDLE);
        stage = Objects.requireNonNullElse(stage, AudioGenerationStage.NONE);
        completedSegments = Math.max(0, completedSegments);
        totalSegments = Math.max(0, totalSegments);
        failedSegments = Math.max(0, failedSegments);
        progress = totalSegments == 0 ? clamp(progress) : clamp((double) completedSegments / Math.max(1, totalSegments));
        currentSegmentId = normalize(currentSegmentId);
        currentSegmentTitle = normalize(currentSegmentTitle);
        estimatedRemainingSeconds = Math.max(0, estimatedRemainingSeconds);
        estimatedRemainingErrorSeconds = Math.max(0, estimatedRemainingErrorSeconds);
        message = normalize(message);
        outputDirectory = normalize(outputDirectory);
        finalAudioPath = normalize(finalAudioPath);
        manifestPath = normalize(manifestPath);
    }

    /** Source-compatible constructor for status producers that do not expose uncertainty. */
    public AudioJobStatusDto(String jobId, String documentName, AudioJobState state,
                             AudioGenerationStage stage, int completedSegments,
                             int totalSegments, int failedSegments, double progress,
                             String currentSegmentId, String currentSegmentTitle,
                             long estimatedRemainingSeconds, String message,
                             String outputDirectory, String finalAudioPath,
                             String manifestPath) {
        this(jobId, documentName, state, stage, completedSegments, totalSegments,
                failedSegments, progress, currentSegmentId, currentSegmentTitle,
                estimatedRemainingSeconds, 0L, message, outputDirectory,
                finalAudioPath, manifestPath);
    }

    public static AudioJobStatusDto idle() {
        return new AudioJobStatusDto("", "", AudioJobState.IDLE, AudioGenerationStage.NONE,
                0, 0, 0, 0.0, "", "", 0, 0,
                "Sin generación de audio activa.", "", "", "");
    }

    public String progressPercentLabel() {
        return String.format(Locale.ROOT, "%.0f%%", progress * 100.0);
    }

    public String segmentCounterLabel() {
        if (totalSegments <= 0) {
            return "0/0 segmentos";
        }
        return completedSegments + "/" + totalSegments + " segmentos";
    }

    public String etaLabel() {
        if (!state.running() || completedSegments == 0 || estimatedRemainingSeconds <= 0) {
            return state.running() ? "Calculando tiempo restante…" : "Sin ETA";
        }
        String estimate = "Faltan aproximadamente " + durationLabel(estimatedRemainingSeconds);
        if (estimatedRemainingErrorSeconds <= 0) {
            return estimate + ".";
        }
        return estimate + " ± " + durationLabel(estimatedRemainingErrorSeconds) + ".";
    }

    private static String durationLabel(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long rest = seconds % 60L;
        if (hours > 0L) {
            return hours + " h " + minutes + " min " + rest + " s";
        }
        if (minutes > 0L) {
            return minutes + " min " + rest + " s";
        }
        return rest + " s";
    }

    public String statusLine() {
        if (state == AudioJobState.IDLE) {
            return message.isBlank() ? "Sin generación activa." : message;
        }
        String segment = currentSegmentTitle.isBlank() ? "" : " — " + currentSegmentTitle;
        String activity = message.isBlank() ? "" : " · " + message;
        return state.displayName() + " · " + stage.displayName() + " · "
                + segmentCounterLabel() + segment + activity;
    }

    public boolean running() {
        return state.running();
    }

    public boolean completed() {
        return state == AudioJobState.COMPLETED;
    }

    public boolean failed() {
        return state == AudioJobState.FAILED;
    }

    private static String blankTo(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
