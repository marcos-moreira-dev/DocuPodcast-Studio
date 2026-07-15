package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;

/** User-facing recovery summary for a persisted audio job. */
public record AudioJobRecoverySummary(
        String jobId,
        String stateLabel,
        int completedSegments,
        int pendingSegments,
        int failedSegments,
        int cancelledSegments,
        boolean resumable,
        String message
) {
    public static AudioJobRecoverySummary from(AudioJobSnapshot snapshot) {
        int pending = (int) snapshot.segments().stream().filter(s -> s.status() == AudioSegmentStatus.PENDING).count();
        int failed = (int) snapshot.segments().stream().filter(s -> s.status() == AudioSegmentStatus.FAILED).count();
        int cancelled = (int) snapshot.segments().stream().filter(s -> s.status() == AudioSegmentStatus.CANCELLED).count();
        int completed = (int) snapshot.segments().stream().filter(s -> s.status() == AudioSegmentStatus.COMPLETED).count();
        boolean resumable = pending + failed + cancelled > 0 && snapshot.totalSegments() > completed;
        String message = resumable
                ? "Reanudable: se conservarán los WAV completados y se procesarán pendientes/fallidos/cancelados."
                : "No requiere reanudación.";
        return new AudioJobRecoverySummary(snapshot.jobId(), snapshot.state().displayName(), completed, pending, failed, cancelled, resumable, message);
    }

    public String label() {
        return jobId + " · " + stateLabel + " · completados " + completedSegments
                + " · pendientes " + pendingSegments + " · fallidos " + failedSegments
                + " · cancelados " + cancelledSegments + " · " + message;
    }
}
