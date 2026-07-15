package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioJobSnapshotMapperTest {
    @Test
    void mapsStatusDtoToDomainSnapshotWithoutDomainDependingOnApplication() {
        AudioJobStatusDto status = new AudioJobStatusDto(
                "JOB-100", "Documento", AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                1, 3, 0, 0.33, "SEG-002", "Segundo", 42,
                "Generando", "ignored", "jobs/JOB-100/audio/SEG-001.wav", "jobs/JOB-100/audio-manifest.json"
        );

        AudioJobSnapshot snapshot = AudioJobSnapshotMapper.fromStatus(status, "jobs/JOB-100",
                List.of(AudioSegmentSnapshot.pending("SEG-002", "Segundo")), Instant.parse("2026-05-29T00:00:00Z"));

        assertEquals("JOB-100", snapshot.jobId());
        assertEquals("jobs/JOB-100", snapshot.jobRelativeDirectory());
        assertEquals("SEG-002", snapshot.currentSegmentId());
        assertEquals(1, snapshot.segments().size());
    }

    @Test
    void mapsSnapshotBackToUiStatusWithAbsoluteOutputDirectory() {
        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-101", "Documento", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                2, 2, 0, 1.0, "", "", 0, "Listo", "jobs/JOB-101",
                "jobs/JOB-101/final/podcast.wav", "jobs/JOB-101/audio-manifest.json",
                List.of(), Instant.now(), Instant.now()
        );

        AudioJobStatusDto dto = AudioJobSnapshotMapper.toStatusDto(snapshot, Path.of("build/project"));

        assertEquals("JOB-101", dto.jobId());
        assertEquals(AudioJobState.COMPLETED, dto.state());
        assertTrue(dto.outputDirectory().replace('\\', '/').endsWith("build/project/jobs/JOB-101"));
    }
}
