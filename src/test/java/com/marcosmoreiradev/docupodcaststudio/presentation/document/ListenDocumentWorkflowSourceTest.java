package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ListenDocumentWorkflowSourceTest {
    @Test
    void documentWorkspaceShowsCompleteWordToListenFlowWithoutTechnicalWorkspaceDetour() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String flow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentListenFlowState.java"));
        String applicationState = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/ListeningSessionState.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));

        assertTrue(viewModel.contains("public void listenToDocument()"));
        assertTrue(viewModel.contains("buildNarrationScriptFromDocument();"));
        assertTrue(viewModel.contains("submitAudioGeneration();"));
        assertTrue(viewModel.contains("tryStartBufferedPlayback"));
        assertTrue(viewModel.contains("activeWorkspace.set(WorkspaceKind.DOCUMENT_READER)"));
        assertTrue(viewModel.contains("refreshDocumentListenFlow"));
        assertTrue(viewModel.contains("documentListenFlowTitleProperty"));
        assertTrue(document.contains("documentModeLabel"));
        assertTrue(document.contains("\"Vista documento\""));
        assertTrue(document.contains("\"Mapa textual\""));
        assertTrue(viewModel.contains("documentListenFlowDetailProperty"));
        assertTrue(flow.contains("ListeningSessionState"));
        assertTrue(applicationState.contains("Lectura preparada al escuchar"));
        assertTrue(applicationState.contains("Audio listo al escuchar"));
        assertTrue(applicationState.contains("Listo para reproducir"));
        assertTrue(css.contains("document-listen-flow-status"));
    }
}
