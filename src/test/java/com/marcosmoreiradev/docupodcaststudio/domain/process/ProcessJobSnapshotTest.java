package com.marcosmoreiradev.docupodcaststudio.domain.process;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessJobSnapshotTest {
    @Test
    void clampsProgressAndKeepsOnlyPortablePaths() {
        ProcessJobSnapshot snapshot = new ProcessJobSnapshot(
                "JOB-001",
                ProcessJobKind.TTS_AUDIO,
                "",
                ProcessJobState.RUNNING,
                ProcessJobStage.RUNNING_ENGINE,
                2.0,
                "SEG-001",
                "Introducción",
                12,
                "Generando voz",
                "jobs/JOB-001",
                true,
                true,
                List.of(new ProcessJobArtifact("wav", "jobs/JOB-001/audio/SEG-001.wav", "Audio")),
                new ProcessJobLogReference("jobs/JOB-001/logs/generation-log.jsonl", "", "", ""),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:01Z")
        );

        assertEquals(1.0, snapshot.progress());
        assertEquals("100%", snapshot.progressLabel());
        assertEquals("Generación de voz", snapshot.displayName());
        assertEquals("jobs/JOB-001", snapshot.jobRelativeDirectory());
    }

    @Test
    void rejectsAbsoluteArtifactAndJobPaths() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessJobArtifact("bad", "C:/tmp/audio.wav", ""));
        assertThrows(IllegalArgumentException.class, () -> new ProcessJobSnapshot(
                "JOB-002",
                ProcessJobKind.TTS_AUDIO,
                "Audio",
                ProcessJobState.COMPLETED,
                ProcessJobStage.EXPORT_READY,
                1,
                "",
                "",
                0,
                "",
                "C:/jobs/JOB-002",
                false,
                false,
                List.of(),
                null,
                null,
                null
        ));
    }

    @Test
    void disablesCancellationForTerminalStates() {
        ProcessJobSnapshot snapshot = new ProcessJobSnapshot(
                "JOB-003",
                ProcessJobKind.TTS_AUDIO,
                "Audio",
                ProcessJobState.COMPLETED,
                ProcessJobStage.EXPORT_READY,
                1,
                "",
                "",
                0,
                "",
                "jobs/JOB-003",
                true,
                false,
                List.of(),
                null,
                null,
                null
        );

        assertFalse(snapshot.cancellable());
    }
}
