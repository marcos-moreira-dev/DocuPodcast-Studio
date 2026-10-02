package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Application-layer mapper between persistable audio job snapshots and UI-oriented status DTOs.
 *
 * <p>This keeps the domain snapshot independent from application DTOs while preserving the
 * persistence/recovery contract used by audio gateways and the shell.</p>
 */
public final class AudioJobSnapshotMapper {
    private AudioJobSnapshotMapper() {
    }

    public static AudioJobSnapshot fromStatus(AudioJobStatusDto status, String jobRelativeDirectory,
                                              List<AudioSegmentSnapshot> segments, Instant createdAt) {
        Objects.requireNonNull(status, "status");
        return new AudioJobSnapshot(
                status.jobId(),
                status.documentName(),
                status.state(),
                status.stage(),
                status.completedSegments(),
                status.totalSegments(),
                status.failedSegments(),
                status.progress(),
                status.currentSegmentId(),
                status.currentSegmentTitle(),
                status.estimatedRemainingSeconds(),
                status.message(),
                jobRelativeDirectory,
                status.finalAudioPath(),
                status.manifestPath(),
                segments,
                createdAt,
                Instant.now()
        );
    }

    public static AudioJobStatusDto toStatusDto(AudioJobSnapshot snapshot, Path projectDirectory) {
        Objects.requireNonNull(snapshot, "snapshot");
        Path root = projectDirectory == null ? Path.of("") : projectDirectory.toAbsolutePath().normalize();
        String outputDirectory = root.resolve(snapshot.jobRelativeDirectory()).normalize().toString();
        return new AudioJobStatusDto(
                snapshot.jobId(),
                snapshot.documentName(),
                snapshot.state(),
                snapshot.stage(),
                snapshot.completedSegments(),
                snapshot.totalSegments(),
                snapshot.failedSegments(),
                snapshot.progress(),
                snapshot.currentSegmentId(),
                snapshot.currentSegmentTitle(),
                snapshot.estimatedRemainingSeconds(),
                0L,
                snapshot.message(),
                outputDirectory,
                snapshot.finalAudioPath(),
                snapshot.manifestPath()
        );
    }
}
