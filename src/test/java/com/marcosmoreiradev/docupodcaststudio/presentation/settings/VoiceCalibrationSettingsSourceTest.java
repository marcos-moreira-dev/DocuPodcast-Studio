package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceCalibrationSettingsSourceTest {
    @Test
    void settingsExposeVoiceCalibrationWithoutPuttingItOnMainReader() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(settings.contains("Voz IA avanzada"));
        assertTrue(settings.contains("VoiceEngineUsabilityPolicy.xttsHighQuality"));
        assertTrue(settings.contains("VoiceEngineUsabilityPolicy.piperLightweight"));
        assertTrue(settings.contains("Ritmo base") || settings.contains("Velocidad de habla"));
        assertTrue(settings.contains("Voz predeterminada"));
        assertTrue(settings.contains("La emoción solo aplica") || settings.contains("estilos expresivos"));
        assertTrue(settings.contains("Modo de motor") || settings.contains("Voz IA avanzada"));
        assertTrue(!document.contains("XTTS / Coqui"));
        assertTrue(!document.contains("Haz clic para grabar tu voz"));
    }
}
