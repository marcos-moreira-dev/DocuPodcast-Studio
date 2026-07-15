package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioJobSnapshotTest {
    @Test
    void rejectsDuplicateSegments() {
        AudioSegmentSnapshot one = AudioSegmentSnapshot.pending("SEG-001", "Uno");
        assertThrows(IllegalArgumentException.class, () -> new AudioJobSnapshot(
                "JOB-001", "Doc", AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                0, 2, 0, 0.0, "", "", 0, "", "jobs/JOB-001", "", "",
                List.of(one, one), Instant.now(), Instant.now()
        ));
    }

    @Test
    void reportsResumableCancelledAndFailedJobs() {
        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-001", "Doc", AudioJobState.CANCELLED, AudioGenerationStage.CANCELLED,
                1, 2, 0, 0.5, "SEG-002", "Dos", 0, "Cancelado", "jobs/JOB-001", "", "",
                List.of(
                        new AudioSegmentSnapshot("SEG-001", "Uno", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 1.0, 1, ""),
                        AudioSegmentSnapshot.pending("SEG-002", "Dos")
                ), Instant.now(), Instant.now()
        );

        assertTrue(snapshot.resumable());
        assertEquals(1, snapshot.pendingSegments());
    }
}
