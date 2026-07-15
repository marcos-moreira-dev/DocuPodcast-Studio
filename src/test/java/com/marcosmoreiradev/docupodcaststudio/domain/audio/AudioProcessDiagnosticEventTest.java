package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioProcessDiagnosticEventTest {
    @Test
    void successEventHasCompactLabelAndPortableOutputPath() {
        AudioProcessDiagnosticEvent event = AudioProcessDiagnosticEvent.success(
                "TTS-JOB-001", "SEG-001", 2, "XTTS", "worker --input SEG-001.txt",
                1500L, "jobs/TTS-JOB-001/audio/SEG-001.wav", 4096L, "ok");

        assertTrue(event.successful());
        assertTrue(event.compactLabel().contains("SEG-001"));
        assertEquals(2, event.attempt());
    }

    @Test
    void rejectsAbsoluteOutputPaths() {
        assertThrows(IllegalArgumentException.class, () -> AudioProcessDiagnosticEvent.failure(
                "TTS-JOB-001", "SEG-001", 1, "XTTS", "cmd", 1, 10L, false,
                "C:/tmp/out.wav", 0L, "error", "tail"));
    }

    @Test
    void timeoutIsNotSuccessful() {
        AudioProcessDiagnosticEvent event = AudioProcessDiagnosticEvent.failure(
                "TTS-JOB-001", "SEG-001", 1, "XTTS", "cmd", -1, 180000L, true,
                "", 0L, "timeout", "");

        assertFalse(event.successful());
        assertTrue(event.compactLabel().contains("TIMEOUT"));
    }
}
