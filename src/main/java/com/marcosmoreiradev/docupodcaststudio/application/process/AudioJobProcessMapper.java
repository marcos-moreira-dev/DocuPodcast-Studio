package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobArtifact;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobLogReference;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobStage;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Maps mature audio-generation jobs into the new common process-job contract. */
public final class AudioJobProcessMapper {
    public ProcessJobSnapshot map(AudioJobSnapshot audioJob) {
        Objects.requireNonNull(audioJob, "audioJob");
        return new ProcessJobSnapshot(
                audioJob.jobId(),
                ProcessJobKind.TTS_AUDIO,
                "Generación de voz: " + audioJob.documentName(),
                mapState(audioJob.state()),
                mapStage(audioJob.stage()),
                audioJob.progress(),
                audioJob.currentSegmentId(),
                audioJob.currentSegmentTitle(),
                audioJob.estimatedRemainingSeconds(),
                audioJob.message().isBlank() ? audioJob.recoveryLabel() : audioJob.message(),
                audioJob.jobRelativeDirectory(),
                audioJob.state().cancellable(),
                audioJob.resumable(),
                artifacts(audioJob),
                logs(audioJob),
                audioJob.createdAt(),
                audioJob.updatedAt()
        );
    }

    public List<ProcessJobSnapshot> mapAll(List<AudioJobSnapshot> audioJobs) {
        if (audioJobs == null || audioJobs.isEmpty()) {
            return List.of();
        }
        return audioJobs.stream().map(this::map).toList();
    }

    private static ProcessJobState mapState(AudioJobState state) {
        return switch (state == null ? AudioJobState.IDLE : state) {
            case IDLE -> ProcessJobState.IDLE;
            case QUEUED -> ProcessJobState.QUEUED;
            case PREPARING -> ProcessJobState.PREPARING;
            case GENERATING_AUDIO, MERGING_AUDIO -> ProcessJobState.RUNNING;
            case CANCELLATION_REQUESTED -> ProcessJobState.CANCELLATION_REQUESTED;
            case CANCELLED -> ProcessJobState.CANCELLED;
            case COMPLETED -> ProcessJobState.COMPLETED;
            case FAILED -> ProcessJobState.FAILED;
        };
    }

    private static ProcessJobStage mapStage(AudioGenerationStage stage) {
        return switch (stage == null ? AudioGenerationStage.NONE : stage) {
            case NONE -> ProcessJobStage.NONE;
            case PREPARING_WORKSPACE -> ProcessJobStage.PREPARING_WORKSPACE;
            case WAITING_FOR_RESOURCES -> ProcessJobStage.PREPARING_ENGINE;
            case GENERATING_SEGMENTS -> ProcessJobStage.RUNNING_ENGINE;
            case MERGING_SEGMENTS -> ProcessJobStage.WRITING_ARTIFACTS;
            case EXPORT_READY -> ProcessJobStage.EXPORT_READY;
            case CANCELLED -> ProcessJobStage.CANCELLED;
            case FAILED -> ProcessJobStage.FAILED;
        };
    }

    private static List<ProcessJobArtifact> artifacts(AudioJobSnapshot audioJob) {
        List<ProcessJobArtifact> artifacts = new ArrayList<>();
        if (!audioJob.finalAudioPath().isBlank()) {
            artifacts.add(new ProcessJobArtifact("final-audio", audioJob.finalAudioPath(), "Audio final del job"));
        }
        if (!audioJob.manifestPath().isBlank()) {
            artifacts.add(new ProcessJobArtifact("audio-manifest", audioJob.manifestPath(), "Manifest de audio"));
        }
        return List.copyOf(artifacts);
    }

    private static ProcessJobLogReference logs(AudioJobSnapshot audioJob) {
        String base = audioJob.jobRelativeDirectory();
        if (base == null || base.isBlank()) {
            return new ProcessJobLogReference("", "", "", "");
        }
        return new ProcessJobLogReference(
                base + "/logs/generation-log.jsonl",
                base + "/logs/process-diagnostics.jsonl",
                "",
                ""
        );
    }
}
