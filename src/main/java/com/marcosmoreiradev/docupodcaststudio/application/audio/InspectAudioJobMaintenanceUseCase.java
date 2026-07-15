package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inspects a persisted audio job and decides whether existing audio can be reused,
 * resumed or regenerated after a source-document refresh.
 */
public final class InspectAudioJobMaintenanceUseCase {
    public AudioJobMaintenanceReport inspect(Path projectDirectory, AudioJobSnapshot snapshot) {
        return inspect(projectDirectory, snapshot, null);
    }

    public AudioJobMaintenanceReport inspect(Path projectDirectory, AudioJobSnapshot snapshot, SourceDocumentChangeReport sourceReport) {
        Objects.requireNonNull(snapshot, "snapshot");
        Path root = Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
        boolean sourceChanged = sourceReport != null && sourceReport.audioShouldBeRegenerated();
        List<String> affected = new ArrayList<>();
        int missingAudioFiles = countMissingCompletedAudio(root, snapshot, affected);
        int recoverable = snapshot.recoverableSegments();
        int failed = snapshot.failedSegmentCount();
        int cancelled = snapshot.cancelledSegmentCount();

        AudioJobHealthStatus status;
        String recommendation;
        if (snapshot.segments().isEmpty() || snapshot.totalSegments() == 0) {
            status = AudioJobHealthStatus.EMPTY_JOB;
            recommendation = "Generar audio desde la narración interna; el job no contiene segmentos.";
        } else if (sourceChanged) {
            status = AudioJobHealthStatus.STALE_SOURCE;
            recommendation = "Regenerar audio: el documento fuente cambió y el audio queda obsoleto.";
        } else if (missingAudioFiles > 0) {
            status = AudioJobHealthStatus.MISSING_AUDIO;
            recommendation = "Regenerar o reparar " + missingAudioFiles + " archivo(s) de audio faltante(s).";
        } else if (recoverable > 0 && snapshot.completedSegments() < snapshot.totalSegments()) {
            status = AudioJobHealthStatus.RESUMABLE;
            recommendation = "Reanudar job: hay " + recoverable + " segmento(s) pendientes/fallidos/cancelados.";
        } else if (snapshot.completedSegments() >= snapshot.totalSegments() && snapshot.failedSegmentCount() == 0) {
            status = AudioJobHealthStatus.READY;
            recommendation = "Audio vigente; los WAV existentes pueden reutilizarse.";
        } else {
            status = AudioJobHealthStatus.REVIEW_REQUIRED;
            recommendation = "Revisar job antes de reproducir o exportar.";
        }

        return new AudioJobMaintenanceReport(
                snapshot.jobId(),
                status,
                snapshot.totalSegments(),
                snapshot.completedSegments(),
                recoverable,
                failed,
                cancelled,
                missingAudioFiles,
                sourceChanged,
                affected,
                recommendation
        );
    }

    private static int countMissingCompletedAudio(Path projectDirectory, AudioJobSnapshot snapshot, List<String> affected) {
        int missing = 0;
        for (AudioSegmentSnapshot segment : snapshot.segments()) {
            if (!segment.completed() || segment.audioRelativePath().isBlank()) {
                continue;
            }
            Path audioPath = projectDirectory.resolve(segment.audioRelativePath()).normalize();
            if (!audioPath.startsWith(projectDirectory) || !Files.isRegularFile(audioPath)) {
                missing++;
                affected.add(segment.segmentId());
            }
        }
        return missing;
    }
}
