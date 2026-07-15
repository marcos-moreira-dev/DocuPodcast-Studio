package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AUDIO-RESUME-HF1: Seguir generando must retry failed chunks instead of instantly failing again. */
final class AudioResumeRetryResetSourceTest {
    @Test
    void localTtsResumeResetsNonCompletedSegmentsAndAttemptsFromOne() throws IOException {
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));
        assertTrue(gateway.contains("merged.add(AudioSegmentSnapshot.pending(unit.id(), unit.effectiveTitle()))"));
        assertTrue(gateway.contains("int firstAttempt = 1"));
        assertTrue(gateway.contains("Files.deleteIfExists(outputFile)"));
        assertFalse(gateway.contains("for (int attempt = Math.max(1, current.attempts() + 1)"));
    }
}
