package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnginePreflightDocumentationSourceTest {
    @Test
    void t88cDocumentsCoquiPiperFfmpegAsProductScope() throws Exception {
        String docs = Files.readString(Path.of("docs/productizacion/T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md"));
        assertTrue(docs.contains("Coqui/XTTS"));
        assertTrue(docs.contains("Piper"));
        assertTrue(docs.contains("FFmpeg"));
        assertTrue(docs.contains("no se incrustan modelos pesados dentro del .docupodcast"));
        assertFalse(docs.contains("STT como requisito"));
    }

    @Test
    void preflightUseCaseIsWiredIntoSettingsServicesWithoutWhisperInProductList() throws Exception {
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));
        String preflight = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/InspectAiEnginesPreflightUseCase.java"));
        assertTrue(services.contains("InspectAiEnginesPreflightUseCase"));
        assertTrue(factory.contains("new InspectAiEnginesPreflightUseCase()"));
        assertFalse(preflight.contains("inspectWhisper"));
    }
}
