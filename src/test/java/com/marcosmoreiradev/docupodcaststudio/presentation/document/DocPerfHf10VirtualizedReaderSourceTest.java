package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocPerfHf10VirtualizedReaderSourceTest {
    @Test
    void largeDocumentsRenderBoundedWindowInsteadOfAllBlocks() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String window = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentRenderWindow.java"));

        assertTrue(view.contains("LARGE_DOCUMENT_WINDOW_SIZE"));
        assertTrue(view.contains("DocumentRenderWindow"));
        assertTrue(view.contains("renderWindowAroundIndex"));
        assertTrue(view.contains("blockIndex(document, blockId)"));
        assertTrue(window.contains("around(int totalBlocks, int pivotIndex, int windowSize)"));
    }

    @Test
    void userCanMoveThroughLargeDocumentWindowsWithoutRenderingEverything() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/large-document.css"));

        assertTrue(view.contains("Bloques anteriores"));
        assertTrue(view.contains("Siguientes bloques"));
        assertTrue(view.contains("Inicio del documento"));
        assertTrue(view.contains("Final del documento"));
        assertTrue(css.contains("document-large-preview-actions"));
    }

    @Test
    void playbackOrRailSelectionCanBringOffscreenBlockIntoWindow() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(view.contains("ensureBlockRendered(nextBlockId)"));
        assertTrue(view.contains("blockNodes.get(blockId)"));
        assertTrue(view.contains("scrollNodeNearReadingTop(rendered)"));
    }
}
