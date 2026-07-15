package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AiEngineShortlistSourceTest {
    @Test
    void officialProductEnginesAreDeliberatelyFewAndDoNotIncludeWhisper() throws Exception {
        String scope = Files.readString(Path.of("docs/productizacion/T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md"));
        assertTrue(scope.contains("Coqui/XTTS"));
        assertTrue(scope.contains("Piper"));
        assertTrue(scope.contains("FFmpeg"));
        assertTrue(scope.contains("No agregar más motores"));
        assertTrue(scope.contains("Whisper no pertenece al producto DocuPodcast"));
    }

    @Test
    void handoffAndValidationPointToTheCurrentEngineScope() throws Exception {
        String readme = Files.readString(Path.of("README.md"));
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));
        String validation = Files.readString(Path.of("VALIDATION.md"));
        assertTrue(readme.contains("Tanda 88C"));
        assertTrue(handoff.contains("T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md"));
        assertTrue(validation.contains("Piper genera un WAV real"));
        assertFalse(validation.contains("whisper.cpp transcribe"));
    }
}
