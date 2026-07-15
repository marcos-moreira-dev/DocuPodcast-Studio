package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuidedEngineSettingsSourceTest {
    @Test
    void settingsExposeGuidedEngineSetupWithoutTechnicalScaffoldingOnReader() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(settings.contains("Motores y dependencias"));
        assertTrue(settings.contains("guidedEnginesPage"));
        assertTrue(settings.contains("voiceRuntimeSettings"));
        assertTrue(settings.contains("Voz IA avanzada"));
        assertTrue(settings.contains("Voz local simple"));
        assertTrue(settings.contains("Preparar") && settings.contains("Configuración inicial"));
        assertTrue(settings.contains("Preparar Voz local simple") || settings.contains("Voz local simple"));
        assertFalse(settings.contains("Generar checklist smoke GUI"));
        assertFalse(settings.contains("Inventario local de motores"));
        assertFalse(settings.contains("Auditar artefactos locales"));
        assertFalse(document.contains("XTTS / Coqui"));
        assertFalse(document.contains("Coqui/XTTS"));
        assertFalse(document.contains("Whisper local"));
    }
}
