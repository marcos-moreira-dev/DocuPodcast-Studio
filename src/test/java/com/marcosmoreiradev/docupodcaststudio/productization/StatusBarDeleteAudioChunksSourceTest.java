package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Status bar deletion action for generated audio chunks. */
final class StatusBarDeleteAudioChunksSourceTest {
    @Test
    void statusBarExposesDangerButtonOnlyWhenPersistedAudioExistsAndGenerationIsIdle() throws IOException {
        String status = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java"));

        assertTrue(status.contains("Runnable deleteAllAudioChunks"));
        assertTrue(status.contains("ActionButtonFactory.danger(\"Eliminar todos los chunks de audio\""));
        assertTrue(status.contains("&& !dto.running()"));
        assertTrue(status.contains("&& deleteAllAudioChunks != null"));
        assertTrue(status.contains("&& !dto.jobId().isBlank()"));
        assertTrue(status.contains("&& dto.totalSegments() > 0"));
        assertTrue(status.contains("deleteChunksButton.setVisible(canDeleteChunks)"));
        assertTrue(status.contains("deleteChunksButton.setManaged(canDeleteChunks)"));
    }

    @Test
    void shellConfirmsBeforeDeletingGeneratedAudioChunks() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/DeleteAudioChunksDialog.java"));

        assertTrue(shell.contains("this::handleDeleteAllAudioChunksFromStatusBar"));
        assertTrue(shell.contains("deleteAudioChunksDialog.confirm(owner())"));
        assertTrue(dialog.contains("Alert.AlertType.CONFIRMATION"));
        assertTrue(dialog.contains("Eliminar audio generado"));
        assertTrue(dialog.contains("Eliminar todos los chunks de audio del proyecto actual"));
        assertTrue(dialog.contains("Eliminar chunks"));
        assertTrue(dialog.contains("Cancelar"));
        assertTrue(shell.contains("viewModel.deleteAllPersistedAudioChunks()"));
    }

    @Test
    void viewModelDeletesAudioJobsAndResetsPlaybackWithoutTouchingDocumentState() throws IOException {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        int start = viewModel.indexOf("public void deleteAllPersistedAudioChunks()");
        int end = viewModel.indexOf("private void invalidatePersistedAudioForNarrationChange", start);
        String method = viewModel.substring(start, end);

        assertTrue(method.contains("deletePersistedAudioAsync(projectDirectory.get()"));
        assertTrue(viewModel.contains("audioWorkflow.prepareFreshWorkspaceAsync"));
        assertTrue(Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java")).contains("deletePersistedJobs(projectDirectory)"));
        assertTrue(viewModel.contains("activeAudioJobStatus.set(AudioJobStatusDto.idle())"));
        assertTrue(viewModel.contains("currentPlaybackManifest.set(PlaybackManifest.empty())"));
        assertTrue(viewModel.contains("Chunks de audio eliminados. Puedes reconstruirlos desde la barra de estado."));
        assertFalse(method.contains("currentDocument.set"));
        assertFalse(method.contains("currentScript.set"));
        assertFalse(method.contains("currentStoryboard.set"));
        assertFalse(method.contains("registerProjectAsset"));
        assertFalse(viewModel.contains("Optional.of(projectDirectory)"));
    }
}
