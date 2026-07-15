package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentPlaybackProcessesT110RSourceTest {
    @Test
    void playbarAndMenuGuideProjectSaveAndAudioProcessCancellation() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(shell.contains("ensureProjectSavedForDocumentAudio"));
        assertTrue(shell.contains("Guardar proyecto antes de continuar"));
        assertTrue(shell.contains("handleListenDocument"));
        assertTrue(viewModel.contains("cancelActiveAudioJobSilently"));
        assertTrue(viewModel.contains("pausa segura solicitada"));
        assertTrue(viewModel.contains("cancelación segura solicitada"));
        assertTrue(viewModel.contains("resumeMostRecentRecoverableAudioJob"));
    }
}
