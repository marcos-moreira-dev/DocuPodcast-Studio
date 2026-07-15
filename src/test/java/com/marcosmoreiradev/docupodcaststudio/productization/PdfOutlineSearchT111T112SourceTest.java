package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfOutlineSearchT111T112SourceTest {
    @Test
    void pdfOutlineAndSearchUseTextLayerBoundariesWithoutPdfBoxInPresentation() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String indexPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");

        assertTrue(services.contains("BuildPdfEnhancedOutlineUseCase buildPdfEnhancedOutline"));
        assertTrue(services.contains("BuildPdfResolvedTextLayerUseCase buildPdfResolvedTextLayer"));
        assertTrue(services.contains("SearchPdfTextUseCase searchPdfText"));
        assertTrue(services.contains("ResolvePdfNarratableDocumentUseCase resolvePdfNarratableDocument"));
        assertTrue(factory.contains("new BuildPdfEnhancedOutlineUseCase(buildDocumentOutline, buildPdfResolvedTextLayer)"));
        assertTrue(factory.contains("new SearchPdfTextUseCase(buildPdfResolvedTextLayer)"));
        assertTrue(factory.contains("new ResolvePdfNarratableDocumentUseCase(buildPdfNativeTextLayer, buildPdfOcrTextLayer, null, ocrSettings)"));
        assertTrue(indexPanel.contains("Mejorar busqueda"));
        assertTrue(indexPanel.contains("runPdfSearch(true)"));
        assertTrue(indexPanel.contains("PdfNarratableDocumentRequest"));
        assertTrue(indexPanel.contains("buildEnhancedOutline.build"));
        assertTrue(workspace.contains("showPdfSearchHighlight"));
        assertTrue(workspace.contains(".docupodcast-cache"));
        assertFalse(indexPanel.contains("PdfBoxRenderEngine"));
        assertFalse(indexPanel.contains("TesseractPdfOcrEngine"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
