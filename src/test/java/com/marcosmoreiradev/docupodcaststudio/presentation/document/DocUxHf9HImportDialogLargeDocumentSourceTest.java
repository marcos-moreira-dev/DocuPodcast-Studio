package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocUxHf9HImportDialogLargeDocumentSourceTest {
    @Test
    void importDialogCanBeHiddenAndClosesBeforeHeavyRendering() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/DocumentImportProgressDialog.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(dialog.contains("ButtonType.CLOSE"));
        assertTrue(dialog.contains("Ocultar"));
        assertTrue(dialog.contains("dialog.hide()"));
        assertTrue(shell.contains("progress.close()"));
        assertTrue(shell.contains("PauseTransition"));
    }

    @Test
    void largeDocumentsUseBoundedInitialRenderUntilVirtualizationArrives() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/large-document.css"));

        assertTrue(view.contains("LARGE_DOCUMENT_INITIAL_RENDER_LIMIT"));
        assertTrue(view.contains("visibleBlocksForInitialRender"));
        assertTrue(view.contains("Documento grande abierto"));
        assertTrue(css.contains("document-large-preview-notice"));
    }
}
