package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentAudioVoiceChangeInvalidatesChunksSourceTest {
    @Test
    void documentVoiceSelectionAndFreshRenderDeletePersistedChunks() throws Exception {
        String documentPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        assertTrue(documentPanel.contains("viewModel.selectDocumentAudioSource(newValue)"));
        assertTrue(documentPanel.contains("Usar voz en todo el documento"));
        assertTrue(documentPanel.contains("Asignar voz al fragmento seleccionado"));
        assertTrue(documentPanel.contains("Asignar voz general"));
        assertTrue(documentPanel.contains("ActionButtonFactory.warning"));
        assertTrue(documentPanel.contains("removePrimaryAssignmentForSelectedDocumentRange"));
        assertTrue(documentPanel.contains("Eliminar voces específicas del documento"));
        assertTrue(documentPanel.contains("useSelectedVoiceForWholeDocument"));
        assertTrue(documentPanel.contains("assignSelectedVoiceToSelectedFragment"));

        String actionFactory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ActionButtonFactory.java"));
        String appStyles = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java"));
        String actionsCss = Files.readString(Path.of("src/main/resources/css/components/actions.css"));
        assertTrue(actionFactory.contains("warning(String text"));
        assertTrue(appStyles.contains("UI_ACTION_BUTTON_WARNING"));
        assertTrue(actionsCss.contains(".ui-action-button-warning"));

        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(viewModel.contains("refreshChunksAfterDocumentVoiceChange"));
        assertTrue(viewModel.contains("generateAudioChunksWithoutPlayback()"));
        assertTrue(viewModel.contains("invalidatePersistedAudioAfterVoiceSelection()"));
        assertTrue(viewModel.contains("removeAllSpecificVoicesFromDocument"));
        assertTrue(viewModel.contains("NarrativeLayerKind.VOICE"));
        assertTrue(viewModel.contains("NarrativeLayerKind.EMOTION"));
        assertTrue(viewModel.contains("submitFreshAudioRequestAsync(projectDirectory"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java"));
        assertTrue(workflow.contains("prepareFreshWorkspaceAsync"));
        assertTrue(workflow.contains("cancelAndAwait"));
        assertTrue(workflow.contains("deletePersistedJobs"));

        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioJobRepository.java"));
        assertTrue(repository.contains("void deleteAll(Path projectDirectory)"));
    }
}
