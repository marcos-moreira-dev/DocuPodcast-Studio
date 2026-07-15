package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceSampleImportUiSourceTest {
    @Test
    void voiceSampleImportStaysAvailableWithoutReturningToMainNavigationNoise() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String actionProvider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String actions = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java"));
        String capabilities = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceCapability.java"));

        assertTrue(shell.contains("handleImportVoiceSample"));
        assertTrue(capabilities.contains("IMPORT_VOICE_SAMPLE"));
        assertTrue(actionProvider.contains("Importar voz"));
        assertTrue(actionProvider.contains("WorkspaceCapability.IMPORT_VOICE_SAMPLE"));
        assertTrue((workspace + actions).contains("Importar muestra"));
        assertFalse(shell.contains("new Menu(\"Voz\")"));
        assertTrue(toolbar.contains("WorkspaceToolbarActionProvider"),
                "La toolbar contextual se seguirá simplificando en T81F, pero no debe inventar una acción fuera del proveedor transversal.");
    }
}
