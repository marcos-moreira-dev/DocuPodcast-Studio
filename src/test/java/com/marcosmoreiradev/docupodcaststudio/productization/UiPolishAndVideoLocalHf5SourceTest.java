package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HF5 cleans visible controls and makes local video preparation concrete. */
final class UiPolishAndVideoLocalHf5SourceTest {
    @Test
    void playbarUsesIconOnlySecondaryControls() throws Exception {
        String bar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/TransportControls.java"));
        assertTrue(bar.contains("transportIcon(AppIcon.PREVIOUS_FRAGMENT"));
        assertTrue(bar.contains("transportIcon(AppIcon.NEXT_FRAGMENT"));
        assertTrue(bar.contains("transportIcon(AppIcon.REFRESH"));
        assertTrue(transport.contains("AppIcon.PAUSE"));
        assertTrue(transport.contains("AppIcon.RESUME"));
        assertTrue(transport.contains("AppIcon.STOP"));
    }

    @Test
    void settingsDialogsAreWideAndVideoHasPrepareAction() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        assertTrue(settings.contains("confirm.getDialogPane().setPrefWidth(820)"));
        assertTrue(settings.contains("confirmAndPrepareVideoLocal"));
        assertTrue(settings.contains("Button prepare = ActionButtonFactory.primary(\"Preparar\""));
        assertFalse(settings.contains("Importar video local..."));
        assertFalse(settings.contains("Verificar Voz IA ava..."));
    }

    @Test
    void welcomeHasTransparentProductWatermark() throws Exception {
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        assertTrue(welcome.contains("docupodcast-watermark.png"));
        assertTrue(Files.exists(Path.of("src/main/resources/branding/docupodcast-watermark.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/branding/docupodcast-logo-transparent.png")));
    }
}
