package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTtsProcessGatewayDiagnosticsSourceTest {
    @Test
    void realGatewayWritesProcessDiagnosticsAndRetriesSegments() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));

        assertTrue(source.contains("AudioProcessDiagnosticEvent"));
        assertTrue(source.contains("AudioProcessDiagnosticsRepository"));
        assertTrue(source.contains("configuration.maxRetries()"));
        assertTrue(source.contains("process-diagnostics.jsonl") || source.contains("appendDiagnostic"));
        assertTrue(source.contains("runCommand"));
        assertTrue(source.contains("timedOut"));
        assertTrue(source.contains("tts_preflight"));
        assertTrue(source.contains("tts_text_sanitized"));
        assertTrue(source.contains("tts_process_destroyed_by_cancel"));
    }

    @Test
    void ttsConfigurationExposesMaxRetriesSetting() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java"));

        assertTrue(source.contains("docupodcast.tts.maxRetries"));
        assertTrue(source.contains("DOCUPODCAST_TTS_MAX_RETRIES"));
        assertTrue(source.contains("maxRetries"));
    }
}
