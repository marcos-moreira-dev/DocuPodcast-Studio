package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentSnapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectAudioJobMaintenanceUseCaseTest {
    @Test
    void completedJobWithPhysicalAudioCanBeReused() throws Exception {
        Path project = Files.createTempDirectory("docupodcast-audio-ready");
        Files.createDirectories(project.resolve("jobs/JOB-1/audio"));
        Files.writeString(project.resolve("jobs/JOB-1/audio/SEG-1.wav"), "wav");

        AudioJobSnapshot snapshot = snapshot(List.of(
                AudioSegmentSnapshot.pending("SEG-1", "Intro").completed("jobs/JOB-1/audio/SEG-1.wav", 1.2)
        ), 1, 1);

        AudioJobMaintenanceReport report = new InspectAudioJobMaintenanceUseCase().inspect(project, snapshot);

        assertEquals(AudioJobHealthStatus.READY, report.status());
        assertTrue(report.canReuseAudio());
        assertEquals(0, report.missingAudioFiles());
    }

    @Test
    void sourceChangesMakeAudioStaleEvenWhenWavExists() throws Exception {
        Path project = Files.createTempDirectory("docupodcast-audio-stale");
        Files.createDirectories(project.resolve("jobs/JOB-1/audio"));
        Files.writeString(project.resolve("jobs/JOB-1/audio/SEG-1.wav"), "wav");

        AudioJobSnapshot snapshot = snapshot(List.of(
                AudioSegmentSnapshot.pending("SEG-1", "Intro").completed("jobs/JOB-1/audio/SEG-1.wav", 1.2)
        ), 1, 1);
        SourceDocumentSnapshot previous = new SourceDocumentSnapshot(SourceDocumentFormat.TXT, project.resolve("doc.txt"), 1, 1, 10, "hash-a");
        SourceDocumentSnapshot current = new SourceDocumentSnapshot(SourceDocumentFormat.TXT, project.resolve("doc.txt"), 1, 1, 12, "hash-b");
        SourceDocumentChangeReport sourceReport = SourceDocumentChangeReport.compare(previous, current);

        AudioJobMaintenanceReport report = new InspectAudioJobMaintenanceUseCase().inspect(project, snapshot, sourceReport);

        assertEquals(AudioJobHealthStatus.STALE_SOURCE, report.status());
        assertTrue(report.mustRegenerateAudio());
    }

    @Test
    void missingCompletedAudioFileRequiresRegenerationOrRepair() throws Exception {
        Path project = Files.createTempDirectory("docupodcast-audio-missing");
        AudioJobSnapshot snapshot = snapshot(List.of(
                AudioSegmentSnapshot.pending("SEG-1", "Intro").completed("jobs/JOB-1/audio/SEG-1.wav", 1.2)
        ), 1, 1);

        AudioJobMaintenanceReport report = new InspectAudioJobMaintenanceUseCase().inspect(project, snapshot);

        assertEquals(AudioJobHealthStatus.MISSING_AUDIO, report.status());
        assertEquals(1, report.missingAudioFiles());
        assertEquals(List.of("SEG-1"), report.affectedSegmentIds());
    }

    @Test
    void failedOrCancelledSegmentsRemainResumable() throws Exception {
        Path project = Files.createTempDirectory("docupodcast-audio-resume");
        AudioJobSnapshot snapshot = snapshot(List.of(
                AudioSegmentSnapshot.pending("SEG-1", "Intro").failed("falló motor"),
                AudioSegmentSnapshot.pending("SEG-2", "Cierre").cancelled()
        ), 0, 2);

        AudioJobMaintenanceReport report = new InspectAudioJobMaintenanceUseCase().inspect(project, snapshot);

        assertEquals(AudioJobHealthStatus.RESUMABLE, report.status());
        assertTrue(report.canResume());
        assertEquals(2, report.recoverableSegments());
    }

    private static AudioJobSnapshot snapshot(List<AudioSegmentSnapshot> segments, int completed, int total) {
        return new AudioJobSnapshot(
                "JOB-1",
                "Documento",
                completed >= total ? AudioJobState.COMPLETED : AudioJobState.FAILED,
                AudioGenerationStage.GENERATING_SEGMENTS,
                completed,
                total,
                (int) segments.stream().filter(segment -> segment.status().name().equals("FAILED")).count(),
                total == 0 ? 0.0 : (double) completed / total,
                "",
                "",
                0,
                "snapshot test",
                "jobs/JOB-1",
                "",
                "jobs/JOB-1/manifest.json",
                segments,
                Instant.now(),
                Instant.now()
        );
    }
}
