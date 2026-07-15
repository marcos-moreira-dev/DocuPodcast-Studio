package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTtsProcessPreflightTest {
    @Test
    void rejectsMissingInputOrOutputPlaceholders() {
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                "tts-worker --text literal --lang {language}",
                "TTS Worker",
                "es",
                "VOC-NARRATOR",
                30
        );

        LocalTtsPreflightReport report = configuration.preflightReport();

        assertFalse(report.ready());
        assertTrue(report.userMessage().contains("{textFile}"));
        assertTrue(report.userMessage().contains("{outputFile}"));
    }

    @Test
    void acceptsPathResolvedCommandWithRequiredPlaceholders() {
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                "tts-worker --text-file {textFile} --output-file {outputFile} --lang {language} --voice {voice}",
                "TTS Worker",
                "es",
                "VOC-NARRATOR",
                30
        );

        LocalTtsPreflightReport report = configuration.preflightReport();

        assertTrue(report.ready());
        assertTrue(report.diagnosticLine().contains("PATH"));
    }
}
