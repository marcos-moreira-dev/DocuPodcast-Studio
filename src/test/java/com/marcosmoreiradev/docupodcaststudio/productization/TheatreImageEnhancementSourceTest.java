package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheatreImageEnhancementSourceTest {
    @Test
    void generationWorkspaceHidesEnhancementButtonsButKeepsBackendAvailable() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreImageGenerationWorkspaceView.java"));

        assertFalse(source.contains("\"Mejorar imagen\""));
        assertFalse(source.contains("\"Escalar a 720p\""));
        assertFalse(source.contains("\"Escalar a 1080p\""));
        assertFalse(source.contains("\"Escalar a 2K\""));
        assertFalse(source.contains("\"Escalar a 4K\""));
        assertFalse(source.contains("\"Expandir a 16:9\""));
        assertFalse(source.contains("\"Pipeline profesional\""));
        assertTrue(source.contains("enhanceTheatreImageCandidate"));
    }

    @Test
    void settingsExposeProfessionalImageComponentsIncludingLora() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsCard.java"));

        assertTrue(source.contains("ControlNet Tile"));
        assertTrue(source.contains("Real-ESRGAN"));
        assertTrue(source.contains("SwinIR"));
        assertTrue(source.contains("SUPIR"));
        assertTrue(source.contains("Workflows profesionales"));
        assertTrue(source.contains("LoRA de consistencia"));
    }

    @Test
    void applicationImagePackageIsExported() throws Exception {
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(moduleInfo.contains("exports com.marcosmoreiradev.docupodcaststudio.application.image;"));
    }
}
