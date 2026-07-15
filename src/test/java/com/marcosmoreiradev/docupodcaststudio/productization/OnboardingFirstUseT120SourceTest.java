package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T120 guards that first-use onboarding is clean and actionable. */
final class OnboardingFirstUseT120SourceTest {
    @Test
    void welcomeOffersDocumentVoiceAndProjectActionsWithoutLegacyNoise() throws Exception {
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(welcome.contains("Abrir documento"));
        assertTrue(welcome.contains("Configuración inicial"));
        assertTrue(welcome.contains("showFirstUseSetup") || shell.contains("handleOpenFirstUseSetup"));
        assertTrue(welcome.contains("Escucha rápido"));
        assertTrue(welcome.contains("Documento protegido"));
        assertTrue(shell.contains("handleOpenFirstUseSetup"));
        assertFalse(welcome.contains("Guion"));
        assertFalse(welcome.contains("Whisper"));
        assertFalse(welcome.contains("STT"));
        assertFalse(welcome.contains("Storyboard"));
        assertFalse(welcome.contains("Whisper"));
    }
}
