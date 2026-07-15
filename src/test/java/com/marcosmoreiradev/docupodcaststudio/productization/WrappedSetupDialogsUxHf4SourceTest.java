package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-HF4 keeps setup dialogs readable and chains advanced + local voice setup without technical scaffolding. */
final class WrappedSetupDialogsUxHf4SourceTest {
    @Test
    void settingsUsesWrappedConfirmationDialogsAndChainedLocalVoiceOffer() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        assertTrue(settings.contains("confirmUser"));
        assertTrue(settings.contains("setWrapText(true)"));
        assertTrue(settings.contains("setPrefWidth(700)") || settings.contains("setPrefWidth(560)"));
        assertTrue(settings.contains("promptPiperAfterAdvancedIfMissing"));
        assertTrue(settings.contains("¿Quieres preparar también la voz liviana?"));
        assertTrue(settings.contains("runPiperPreparation"));
    }
}
