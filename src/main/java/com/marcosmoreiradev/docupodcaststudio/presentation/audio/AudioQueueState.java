package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Projection of the complete Audio workspace queue: active job, persisted history and diagnostics. */
public record AudioQueueState(
        String engineLabel,
        boolean projectSaved,
        AudioJobRow activeJob,
        List<AudioJobRow> persistedJobs,
        List<String> selectedJobDetails,
        List<String> processDiagnostics,
        List<String> playbackCues,
        String emptyQueueMessage
) {
    public AudioQueueState {
        engineLabel = normalize(engineLabel);
        activeJob = activeJob == null ? AudioJobRow.active(AudioJobStatusDto.idle()) : activeJob;
        persistedJobs = persistedJobs == null ? List.of() : List.copyOf(persistedJobs);
        selectedJobDetails = selectedJobDetails == null ? List.of() : List.copyOf(selectedJobDetails);
        processDiagnostics = processDiagnostics == null ? List.of() : List.copyOf(processDiagnostics);
        playbackCues = playbackCues == null ? List.of() : List.copyOf(playbackCues);
        emptyQueueMessage = normalize(emptyQueueMessage);
    }

    public static AudioQueueState unsaved(String engineLabel, AudioJobStatusDto activeStatus, List<String> playbackCues) {
        return new AudioQueueState(
                engineLabel,
                false,
                AudioJobRow.active(activeStatus),
                List.of(),
                List.of("Guarda el proyecto para crear y consultar jobs persistidos."),
                List.of("Guarda el proyecto para ver diagnósticos del proceso TTS."),
                playbackCues,
                "Guarda el proyecto antes de generar audio."
        );
    }

    public static AudioQueueState saved(String engineLabel, AudioJobStatusDto activeStatus, Path projectDirectory,
                                        List<AudioJobSnapshot> snapshots, List<String> selectedJobDetails,
                                        List<String> processDiagnostics, List<String> playbackCues) {
        AudioJobStatusDto active = activeStatus == null ? AudioJobStatusDto.idle() : activeStatus;
        List<AudioJobSnapshot> orderedSnapshots = snapshots == null ? List.of() : snapshots.stream()
                .sorted(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed())
                .toList();
        List<AudioJobRow> rows = orderedSnapshots.stream()
                .map(snapshot -> AudioJobRow.persisted(snapshot, projectDirectory, active.jobId()))
                .toList();
        return new AudioQueueState(
                engineLabel,
                true,
                AudioJobRow.active(active),
                rows,
                selectedJobDetails,
                processDiagnostics,
                playbackCues,
                rows.isEmpty() ? "No hay jobs persistidos todavía. Genera audio para crear la cola." : ""
        );
    }

    public boolean hasRunningJob() {
        return activeJob.running() || persistedJobs.stream().anyMatch(AudioJobRow::running);
    }

    public boolean hasRecoverableJob() {
        return persistedJobs.stream().anyMatch(AudioJobRow::resumable);
    }

    public List<String> queueLabels() {
        if (persistedJobs.isEmpty()) {
            return emptyQueueMessage.isBlank() ? List.of("No hay jobs persistidos todavía.") : List.of(emptyQueueMessage);
        }
        return persistedJobs.stream()
                .map(row -> row.headline() + "\n    " + row.supportLine() + "\n    " + row.actionHint())
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
