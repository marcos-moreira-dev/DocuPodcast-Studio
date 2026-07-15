package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedVoicePlaybackConfirmationSourceTest {
    @Test
    void settingsCanPlayAndConfirmAdvancedVoiceSmokeInsideTheApp() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ConfirmXttsSmokePlaybackUseCase.java"));

        assertTrue(settings.contains("Reproducir prueba"));
        assertTrue(settings.contains("playAndConfirmXttsSmoke"));
        assertTrue(services.contains("ConfirmXttsSmokePlaybackUseCase confirmXttsSmokePlayback"));
        assertTrue(useCase.contains("setOnPlaybackFinished"));
        assertTrue(useCase.contains("markPlaybackConfirmed"));
        assertTrue(useCase.contains("playbackConfirmedAt"));
        assertFalse(useCase.contains("Desktop.getDesktop"), "La confirmación no debe delegar en reproductores externos.");
    }
}
