package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OperationalSettingsProductizationSourceTest {
    @Test
    void settingsAreOperationalBrainNotDocumentEditingOrReaderScaffolding() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettings.java"));
        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/settings/PropertiesOperationalSettingsRepository.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String formModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String roadmap = Files.readString(Path.of("docs/productizacion/ROADMAP_POST_T67_CONFIGURACION_OPERATIVA.md"));

        assertTrue(settings.contains("Persistent operational settings"));
        assertTrue(settings.contains("PlaybackBufferSettings"));
        assertTrue(settings.contains("TtsEngineSettings"));
        assertFalse(settings.contains("SttEngineSettings"));
        assertTrue(settings.contains("VideoRenderSettings"));
        assertTrue(repository.contains("operational-settings.properties"));
        assertTrue(repository.contains("Word/PDF/Markdown/TXT remain read-only"));
        assertTrue(factory.contains("OperationalSettings operationalSettings"));
        assertTrue(factory.contains("OperationalSettings operationalSettings"));
        assertFalse(dialog.contains("Whisper"));
        assertFalse(dialog.contains("STT"));
        assertTrue(dialog.contains("Ajustes del programa para lectura, voz, audio y video")
                || dialog.contains("Configuración operativa persistente")
                || dialog.contains("Configuración operativa editable y persistente"));
        assertTrue(formModel.contains("settings.playbackBuffer().initialReadySegments()")
                || formModel.contains("current.playbackBuffer().initialReadySegments()"));
        assertTrue(roadmap.contains("T68 — rediseño UI aplicado"));
        assertFalse(document.contains("operational-settings.properties"));
    }
}
