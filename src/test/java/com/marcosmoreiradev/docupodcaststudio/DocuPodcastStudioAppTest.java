package com.marcosmoreiradev.docupodcaststudio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastStudioAppTest {
    @Test
    void compatibilityEntryPointDirectsUsersToTheProductiveLauncher() {
        String message = DocuPodcastStudioApp.launchGuardMessage();

        assertTrue(message.contains("studio-launcher"));
        assertTrue(message.contains("01-ejecutar-app.bat"));
        assertTrue(message.contains("motores de voz"));
    }
}
