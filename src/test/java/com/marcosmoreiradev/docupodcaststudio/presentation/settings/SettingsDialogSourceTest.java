package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsDialogSourceTest {
    @Test
    void settingsDialogIsCleanOperationalSurfaceAndWelcomeOnlyShowsInitialSetup() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String formModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/settings.css"));
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));

        assertTrue(dialog.contains("Ajustes operativos persistentes"));
        assertTrue(dialog.contains("READING(\"Lectura\")"));
        assertTrue(dialog.contains("PLAYBACK(\"Reproducción\")"));
        assertTrue(dialog.contains("ENGINES(\"Motores y dependencias\")"));
        assertTrue(dialog.contains("OCR PDF local"));
        assertTrue(dialog.contains("preflight.ocrReady()"));
        assertTrue(dialog.indexOf("page.addNode(ocrSettingsCard.create") < dialog.indexOf("page.addNode(voiceRuntimeSettings"));
        assertTrue(dialog.contains("SUPPORT(\"Soporte y diagnóstico\")"));
        assertFalse(dialog.contains("TTS_VOICE(\"Voz y lectura\")"));
        assertFalse(dialog.contains("AUDIO(\"Audio\")"));
        assertFalse(dialog.contains("STT"));
        assertFalse(dialog.contains("Whisper"));
        assertTrue(dialog.contains("VIDEO_FINAL(\"Video final\")"));
        assertTrue(formModel.contains("DEFAULT_PREBUFFER_SENTENCES"));
        assertTrue(formModel.contains("DEFAULT_LOOKAHEAD_SENTENCES"));
        assertTrue(shell.contains("Menu configuracion = new Menu(\"Configuración\")"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_SETTINGS)"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_SETTINGS, this::handleOpenSettings)"));
        assertTrue(css.contains("settings-sidebar"));
        assertTrue(css.contains("settings-page"));
        assertTrue(imports.contains("settings.css"));
        assertFalse(toolbar.contains("Configuración"));
        assertTrue(welcome.contains("Configuración inicial"));
        assertFalse(welcome.contains("Diagnóstico avanzado"));
    }
}
