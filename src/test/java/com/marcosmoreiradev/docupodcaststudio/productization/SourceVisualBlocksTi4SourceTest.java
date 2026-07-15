package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVisualBlocksTi4SourceTest {
    @Test
    void sourceVisualBlocksAreNotNarratableByDefaultAndIncludeMathNotice() throws Exception {
        String type = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/DocumentBlockType.java"));
        assertTrue(type.contains("MATH_NOTICE"));
        assertTrue(type.contains("sourceVisual()"));
        assertTrue(type.contains("!sourceVisual()"));
    }

    @Test
    void docxImporterIdentifiesMathWithoutRenderingLatexEngine() throws Exception {
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));
        assertTrue(importer.contains("hasMathDescendant"));
        assertTrue(importer.contains("MATH_BLOCK_DETECTED"));
        assertTrue(importer.contains("identified-only"));
    }

    @Test
    void documentWorkspaceUsesReusableSourceVisualComponentForTableMathAndImages() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String component = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java"));
        assertTrue(view.contains("sourceVisualPreview"));
        assertTrue(view.contains("DocumentBlockType.MATH_NOTICE"));
        assertTrue(component.contains("placeholder(String title, String detail, String marker)"));
    }

    @Test
    void roadmapDocumentsTheDeliberateLatexLimitation() throws Exception {
        String doc = Files.readString(Path.of("docs/productizacion/TI4_BLOQUES_VISUALES_NO_NARRABLES.md"));
        assertTrue(doc.contains("no renderiza LaTeX"));
        assertTrue(doc.contains("OMML"));
        assertTrue(doc.contains("bloque visual fuente"));
    }
}
