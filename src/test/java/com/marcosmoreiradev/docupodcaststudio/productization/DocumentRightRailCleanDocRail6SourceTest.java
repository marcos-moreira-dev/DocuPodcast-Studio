package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-RAIL6 removes the redundant generic Images section from the document right rail. */
final class DocumentRightRailCleanDocRail6SourceTest {
    @Test
    void rightRailShowsOnlyFragmentVisualCards() throws Exception {
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        assertTrue(rail.contains("Fragmentos visuales"));
        assertFalse(rail.contains("railTitle(\"Imágenes\")"));
        assertFalse(rail.contains("looseImageItems"));
        assertFalse(rail.contains("renderImportedImages"));
        assertFalse(rail.contains("imageCard("));
    }

    @Test
    void documentRailSummaryDoesNotDuplicateImagesSection() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        assertFalse(document.contains("Fragmentos con imagen"));
        assertFalse(document.contains("Capas e imágenes"));
    }
}
