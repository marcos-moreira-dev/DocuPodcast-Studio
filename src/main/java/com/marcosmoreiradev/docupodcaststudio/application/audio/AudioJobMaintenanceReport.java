package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.List;
import java.util.Objects;

/**
 * Brain-level report for audio job recovery, reuse and regeneration decisions.
 * It is intentionally independent from JavaFX and from the visible audio workspace.
 */
public record AudioJobMaintenanceReport(
        String jobId,
        AudioJobHealthStatus status,
        int totalSegments,
        int completedSegments,
        int recoverableSegments,
        int failedSegments,
        int cancelledSegments,
        int missingAudioFiles,
        boolean sourceChanged,
        List<String> affectedSegmentIds,
        String recommendation
) {
    public AudioJobMaintenanceReport {
        jobId = normalize(jobId).isBlank() ? "sin-job" : normalize(jobId);
        status = Objects.requireNonNullElse(status, AudioJobHealthStatus.REVIEW_REQUIRED);
        totalSegments = Math.max(0, totalSegments);
        completedSegments = Math.max(0, completedSegments);
        recoverableSegments = Math.max(0, recoverableSegments);
        failedSegments = Math.max(0, failedSegments);
        cancelledSegments = Math.max(0, cancelledSegments);
        missingAudioFiles = Math.max(0, missingAudioFiles);
        affectedSegmentIds = affectedSegmentIds == null ? List.of() : List.copyOf(affectedSegmentIds);
        recommendation = normalize(recommendation).isBlank() ? defaultRecommendation(status) : normalize(recommendation);
    }

    public boolean canReuseAudio() {
        return status == AudioJobHealthStatus.READY && !sourceChanged && missingAudioFiles == 0;
    }

    public boolean canResume() {
        return status == AudioJobHealthStatus.RESUMABLE && recoverableSegments > 0;
    }

    public boolean mustRegenerateAudio() {
        return status == AudioJobHealthStatus.STALE_SOURCE || status == AudioJobHealthStatus.MISSING_AUDIO;
    }

    public String compactLabel() {
        return status.displayName() + " · " + recommendation;
    }

    private static String defaultRecommendation(AudioJobHealthStatus status) {
        return switch (status) {
            case READY -> "Audio vigente; puede reutilizarse.";
            case RESUMABLE -> "Reanudar job para completar segmentos pendientes/fallidos/cancelados.";
            case STALE_SOURCE -> "Regenerar audio porque el documento fuente cambió.";
            case MISSING_AUDIO -> "Regenerar o reparar porque faltan WAV asociados al job.";
            case EMPTY_JOB -> "Generar audio desde la narración interna antes de reproducir.";
            case REVIEW_REQUIRED -> "Revisar el job antes de reproducir o exportar.";
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
