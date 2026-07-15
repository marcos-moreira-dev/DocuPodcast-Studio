package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRecoverySummary;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MockAudioGenerationGatewayResumeTest {
    @TempDir
    Path tempDir;

    @Test
    void resumesPersistedJobWithoutRegeneratingCompletedSegments() throws Exception {
        AudioJobFileRepository repository = new AudioJobFileRepository();
        MockAudioGenerationGateway gateway = new MockAudioGenerationGateway(new InMemoryAudioJobQueue(), repository, 0);
        Path jobDir = tempDir.resolve("jobs/JOB-RESUME");
        Files.createDirectories(jobDir.resolve("audio"));
        Path completedClip = jobDir.resolve("audio/SEG-001.wav");
        Files.writeString(completedClip, "already generated");
        long previousSize = Files.size(completedClip);

        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-RESUME", "Guion", AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                1, 3, 0, 0.33, "SEG-002", "Dos", 0, "Cancelado", "jobs/JOB-RESUME", "", "",
                List.of(
                        new AudioSegmentSnapshot("SEG-001", "Uno", AudioSegmentStatus.COMPLETED, "jobs/JOB-RESUME/audio/SEG-001.wav", 1.0, 1, ""),
                        new AudioSegmentSnapshot("SEG-002", "Dos", AudioSegmentStatus.CANCELLED, "", 0.0, 0, ""),
                        new AudioSegmentSnapshot("SEG-003", "Tres", AudioSegmentStatus.PENDING, "", 0.0, 0, "")
                ), Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:01:00Z"));
        repository.save(tempDir, snapshot);

        NarrationScriptDocument script = NarrationScriptDocument.create("Prueba", "es", "doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Texto uno", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Texto dos", List.of("B002")),
                NarrationSegment.of("SEG-003", NarrationSegmentType.PARAGRAPH, "Tres", "Texto tres", List.of("B003"))
        ));
        CountDownLatch completed = new CountDownLatch(1);

        gateway.resume(new AudioGenerationRequest(script, tempDir, "Prueba"), snapshot, status -> {
            if (status.state() == AudioJobState.COMPLETED) {
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        AudioJobSnapshot loaded = repository.load(tempDir, "JOB-RESUME").orElseThrow();
        assertEquals(AudioJobState.COMPLETED, loaded.state());
        assertEquals(3, loaded.completedSegments());
        assertEquals(previousSize, Files.size(completedClip), "Completed clip should not be overwritten during resume");
        assertTrue(Files.exists(jobDir.resolve("audio/SEG-002.wav")));
        assertTrue(Files.exists(jobDir.resolve("audio/SEG-003.wav")));
        assertTrue(Files.readString(jobDir.resolve("logs/generation-log.jsonl")).contains("job_resuming"));
    }

    @Test
    void recoverySummaryCountsRecoverableSegments() {
        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-001", "Guion", AudioJobState.FAILED, AudioGenerationStage.FAILED,
                1, 3, 1, 0.33, "SEG-002", "Dos", 0, "Falló", "jobs/JOB-001", "", "",
                List.of(
                        new AudioSegmentSnapshot("SEG-001", "Uno", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 1.0, 1, ""),
                        new AudioSegmentSnapshot("SEG-002", "Dos", AudioSegmentStatus.FAILED, "", 0.0, 1, "error"),
                        new AudioSegmentSnapshot("SEG-003", "Tres", AudioSegmentStatus.PENDING, "", 0.0, 0, "")
                ), Instant.now(), Instant.now());

        AudioJobRecoverySummary summary = AudioJobRecoverySummary.from(snapshot);

        assertTrue(summary.resumable());
        assertEquals(1, summary.completedSegments());
        assertEquals(1, summary.failedSegments());
        assertEquals(1, summary.pendingSegments());
    }
}
