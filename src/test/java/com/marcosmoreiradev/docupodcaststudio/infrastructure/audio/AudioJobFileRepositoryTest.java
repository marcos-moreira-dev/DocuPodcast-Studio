package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioJobFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsJobSnapshotWithSegmentStatuses() throws Exception {
        AudioJobFileRepository repository = new AudioJobFileRepository();
        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-001", "Guion", AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                1, 2, 0, 0.5, "SEG-002", "Dos", 0, "Cancelado", "jobs/JOB-001", "", "",
                List.of(
                        new AudioSegmentSnapshot("SEG-001", "Uno", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 1.2, 1, ""),
                        new AudioSegmentSnapshot("SEG-002", "Dos", AudioSegmentStatus.CANCELLED, "", 0.0, 0, "")
                ), Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:01:00Z")
        );

        repository.save(tempDir, snapshot);

        assertTrue(Files.exists(tempDir.resolve("jobs/JOB-001/job.json")));
        assertTrue(Files.exists(tempDir.resolve("jobs/JOB-001/segments-status.json")));
        AudioJobSnapshot loaded = repository.load(tempDir, "JOB-001").orElseThrow();
        assertEquals(AudioJobState.CANCELLED, loaded.state());
        assertEquals(2, loaded.segments().size());
        assertEquals(AudioSegmentStatus.COMPLETED, loaded.segments().get(0).status());
        assertEquals(1, repository.list(tempDir).size());
    }

    @Test
    void deletesAllPersistedJobsBeforeFreshVoiceRender() throws Exception {
        AudioJobFileRepository repository = new AudioJobFileRepository();
        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-DELETE", "Guion", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                1, 1, 0, 1.0, "SEG-001", "Uno", 0, "Listo", "jobs/JOB-DELETE", "", "",
                List.of(new AudioSegmentSnapshot("SEG-001", "Uno", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-DELETE/audio/SEG-001.wav", 1.2, 1, "")),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:01:00Z")
        );
        repository.save(tempDir, snapshot);
        Files.writeString(tempDir.resolve("jobs").resolve("scratch.tmp"), "x");

        repository.deleteAll(tempDir);

        assertFalse(Files.exists(tempDir.resolve("jobs")));
        assertTrue(repository.list(tempDir).isEmpty());
    }

    @Test
    void deletionWaitsUntilActiveGatewayWriterReleasesWorkspace() throws Exception {
        AudioJobFileRepository repository = new AudioJobFileRepository();
        Path segments = tempDir.resolve("jobs/JOB-ACTIVE/segments");
        Files.createDirectories(segments);
        Files.writeString(segments.resolve("SEG-001.txt"), "en escritura");
        Lock writer = AudioJobWorkspaceLockRegistry.writer(tempDir);
        writer.lock();
        boolean writerReleased = false;
        try {
            CompletableFuture<Void> deletion = CompletableFuture.runAsync(() -> {
                try { repository.deleteAll(tempDir); }
                catch (Exception ex) { throw new RuntimeException(ex); }
            });
            Thread.sleep(150L);
            assertFalse(deletion.isDone());
            assertTrue(Files.exists(segments.resolve("SEG-001.txt")));
            writer.unlock();
            writerReleased = true;
            deletion.get(2, TimeUnit.SECONDS);
            assertFalse(Files.exists(tempDir.resolve("jobs")));
        } finally {
            if (!writerReleased) writer.unlock();
        }
    }
}
