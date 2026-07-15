package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentExactTextSelectionSourceTest {
    @Test
    void documentWorkspaceSupportsSentenceLevelSelectionWithoutWritingIntoWord() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String audioPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String layerCoordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerCoordinator.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document-reader.css"))
                + Files.readString(Path.of("src/main/resources/css/document/document-page.css"));

        assertTrue(workspace.contains("DocumentSentenceSplitter.split"));
        assertTrue(workspace.contains("selectSentence"));
        assertTrue(workspace.contains("TextFlow"));
        assertTrue(workspace.contains("document-sentence-selected"));
        assertTrue(viewModel.contains("selectDocumentTextRange"));
        assertTrue(viewModel.contains("narrativeLayerWorkflow.scriptRangeForLayer"));
        assertTrue(layerCoordinator.contains("scriptRangeForLayer"));
        assertTrue(viewModel.contains("se guardarán en el proyecto, no dentro del Word"));
        assertTrue(audioPanel.contains("selectedDocumentRangeLabelProperty"));
        assertTrue(css.contains("document-sentence-selected"));
    }
}
