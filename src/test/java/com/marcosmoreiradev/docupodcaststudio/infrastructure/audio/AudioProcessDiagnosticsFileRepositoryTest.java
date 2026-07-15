package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioProcessDiagnosticsFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void appendsAndReadsProcessDiagnosticsAsJsonl() throws Exception {
        AudioProcessDiagnosticsFileRepository repository = new AudioProcessDiagnosticsFileRepository();
        AudioProcessDiagnosticEvent first = AudioProcessDiagnosticEvent.failure(
                "TTS-JOB-001", "SEG-001", 1, "XTTS", "worker", 2, 500L, false,
                "", 0L, "falló", "stack trace");
        AudioProcessDiagnosticEvent second = AudioProcessDiagnosticEvent.success(
                "TTS-JOB-001", "SEG-001", 2, "XTTS", "worker", 450L,
                "jobs/TTS-JOB-001/audio/SEG-001.wav", 2048L, "ok");

        repository.append(tempDir, "TTS-JOB-001", first);
        repository.append(tempDir, "TTS-JOB-001", second);

        Path file = tempDir.resolve("jobs/TTS-JOB-001/logs/process-diagnostics.jsonl");
        assertTrue(Files.exists(file));
        assertEquals(2, repository.list(tempDir, "TTS-JOB-001").size());
        assertTrue(repository.list(tempDir, "TTS-JOB-001").get(1).successful());
    }
}
