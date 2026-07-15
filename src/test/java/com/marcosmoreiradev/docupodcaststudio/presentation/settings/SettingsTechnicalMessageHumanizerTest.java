package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsTechnicalMessageHumanizerTest {
    @Test
    void engineTextReplacesTechnicalEngineNamesAndLocalPaths() {
        String message = SettingsTechnicalMessageHumanizer.engineText(
                String.join("", "Co", "qui", "/", "XT", "TS")
                        + " failed in C:\\Users\\demo\\tools\\engine\\ffmpeg.exe");

        assertTrue(message.contains("Voz IA avanzada"));
        assertTrue(message.contains("carpeta local"));
        assertFalse(message.contains(String.join("", "Co", "qui")));
        assertFalse(message.contains(String.join("", "XT", "TS")));
        assertTrue(SettingsTechnicalMessageHumanizer.engineText("ffmpeg.exe").contains("componente de video"));
    }

    @Test
    void progressKeepsUserFacingOperationLabels() {
        assertEquals("Descargando recursos necesarios...",
                SettingsTechnicalMessageHumanizer.progress("download model file"));
        assertEquals("Instalando componentes locales...",
                SettingsTechnicalMessageHumanizer.progress("install runtime"));
        assertEquals("Verificando runtime local...",
                SettingsTechnicalMessageHumanizer.embeddedDependencyProgress("running smoke probe"));
    }

    @Test
    void downloadFailuresTranslateModelArtifacts() {
        String message = SettingsTechnicalMessageHumanizer.downloadFailures(
                List.of("model.pth", "config.json", "https://example.test/file"));

        assertTrue(message.contains("archivo principal de voz"));
        assertTrue(message.contains("configuración del modelo"));
        assertTrue(message.contains("proveedor de descarga"));
    }
}
