package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** HF4: a completed voice model download must not be reported as a download failure. */
final class AdvancedVoiceDownloadCompletionHf4SourceTest {
    @Test
    void settingsSeparatesModelDownloadFromRuntimePreparation() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String progress = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java"));
        assertTrue(settings.contains("downloadedModelIsUsableButRuntimePending"));
        assertTrue(settings.contains("Modelo de voz descargado correctamente"));
        assertTrue(settings.contains("Falta preparar el entorno local"));
        assertTrue(settings.contains("Reporte de prepar"));
        assertTrue(progress.contains("TextArea detail = new TextArea"));
        assertTrue(progress.contains("Copia el detalle de arriba"));
    }
}
