package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfVisualWorkspaceT102SourceTest {
    @Test
    void pdfDocumentsUseVisualWorkspaceThroughApplicationBoundary() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String css = read("src/main/resources/css/document/pdf-visual-viewer.css");

        assertTrue(workspace.contains("SourceDocumentFormat.PDF"));
        assertTrue(workspace.contains("PdfVisualDocumentView"));
        assertTrue(workspace.contains("new StackPane(documentScroll, pdfVisualView, readingControls)"));
        assertTrue(workspace.contains("documentScroll.setVisible(false)"));
        assertTrue(workspace.contains("pdfVisualView.showDocument(document)"));
        assertTrue(workspace.contains("showBlockDocumentReader()"));
        assertTrue(workspace.contains("sourcePageForBlock(document, blockId).ifPresent(pdfVisualView::scrollToPage)"));

        assertTrue(pdfView.contains("BuildPdfVisualDocumentUseCase"));
        assertTrue(pdfView.contains("RenderPdfVisualPageUseCase"));
        assertTrue(pdfView.contains("PAGE_RENDER_CACHE_LIMIT = 8"));
        assertTrue(pdfView.contains("LinkedHashMap<PageKey, Image> pageCache"));
        assertTrue(pdfView.contains("removeEldestEntry"));
        assertTrue(pdfView.contains("renderVisiblePages()"));
        assertTrue(pdfView.contains("unloadDistantPages"));
        assertTrue(pdfView.contains("Task<PdfPageRenderResult>"));
        assertTrue(pdfView.contains("No se pudo renderizar la pagina PDF"));
        assertTrue(pdfView.contains("scroll.setFitToWidth(false)"));
        assertTrue(pdfView.contains("setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED)"));

        assertTrue(services.contains("BuildPdfVisualDocumentUseCase buildPdfVisualDocument"));
        assertTrue(services.contains("RenderPdfVisualPageUseCase renderPdfVisualPage"));
        assertTrue(factory.contains("new PdfBoxRenderEngine()"));
        assertTrue(factory.contains("new BuildPdfVisualDocumentUseCase(pdfRenderEngine)"));
        assertTrue(factory.contains("new RenderPdfVisualPageUseCase(pdfRenderEngine)"));
        assertTrue(css.contains(".pdf-visual-workspace"));
        assertFalse(workspace.contains("PdfBoxRenderEngine"));
        assertFalse(pdfView.contains("PdfBoxRenderEngine"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
