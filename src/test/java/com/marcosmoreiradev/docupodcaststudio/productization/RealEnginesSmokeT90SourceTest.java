package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RealEnginesSmokeT90SourceTest {
    @Test
    void t90DefinesOptInSmokeForRealVoiceAndMediaEnginesOnly() throws Exception {
        String test = Files.readString(Path.of("src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/RealEnginesSmokeScenarioTest.java"));
        String script = Files.readString(Path.of("scripts/19-smoke-motores-reales.bat"));
        String doc = Files.readString(Path.of("docs/productizacion/T90_SMOKE_REAL_MOTORES.md"));
        assertTrue(test.contains("Coqui/XTTS"));
        assertTrue(test.contains("Piper genera WAV"));
        assertTrue(test.contains("FFmpeg normaliza y extrae audio"));
        assertTrue(test.contains("Assumptions.assumeTrue(false"));
        assertTrue(script.contains("docupodcast.realEnginesSmoke.enabled=true"));
        assertTrue(doc.contains("target/docupodcast-real-engines-smoke"));
        assertFalse(doc.contains("whisper.cpp"));
        assertFalse(doc.contains("Audio a texto"));
    }

    @Test
    void handoffAndValidationReferenceT90WithoutTurningItIntoFinalRc() throws Exception {
        String readme = Files.readString(Path.of("README.md"));
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));
        String validation = Files.readString(Path.of("VALIDATION.md"));
        assertTrue(readme.contains("DocuPodcast Studio — Tanda 90"));
        assertTrue(handoff.contains("T90_SMOKE_REAL_MOTORES.md"));
        assertTrue(validation.contains("Piper genera un WAV real"));
        assertTrue(validation.contains("Coqui/XTTS genera un WAV real"));
        assertTrue(validation.contains("FFmpeg normaliza audio y extrae audio de video"));
        assertTrue(validation.contains("no es Release Candidate final"));
        assertFalse(validation.contains("whisper.cpp transcribe"));
    }
}
