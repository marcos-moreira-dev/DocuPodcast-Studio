package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualBlocksNonNarratableTi4SourceTest {
    @Test
    void ti4DocumentsAndImplementsSourceVisualBlockContract() throws Exception {
        String blockType = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/DocumentBlockType.java"));
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String component = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java"));
        String doc = Files.readString(Path.of("docs/productizacion/TI4_BLOQUES_VISUALES_NO_NARRABLES.md"));

        assertTrue(blockType.contains("MATH_NOTICE"));
        assertTrue(blockType.contains("sourceVisual()"));
        assertTrue(importer.contains("MATH_BLOCK_DETECTED"));
        assertTrue(importer.contains("visualBlock"));
        assertTrue(workspace.contains("sourceVisualPreview"));
        assertTrue(component.contains("sourceNotice"));
        assertTrue(doc.contains("imagen / tabla / fórmula"));
        assertTrue(doc.contains("no se narran por defecto"));
    }
}
