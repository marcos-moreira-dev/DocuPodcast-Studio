package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisualFlowT113SourceTest {
    @Test
    void pdfViewerUsesVisibleZoomSharpDpiAndBoundedRenderQueue() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String renderer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBoxRenderEngine.java");

        assertTrue(pdfView.contains("visiblePageWidthForZoom"));
        assertTrue(pdfView.contains("dpiForVisibleWidth"));
        assertTrue(pdfView.contains("SHARP_RENDER_PIXEL_MARGIN"));
        assertTrue(pdfView.contains("PDF_RENDER_POOL_SIZE = 2"));
        assertTrue(pdfView.contains("MAX_RENDER_QUEUE_SIZE"));
        assertTrue(pdfView.contains("renderRevision"));
        assertTrue(pdfView.contains("trimRenderQueue"));
        assertTrue(pdfView.contains("PDF_RENDER_DIAGNOSTICS = false"));
        assertTrue(pdfView.contains("reportPdfRenderDiagnostics"));
        assertTrue(pdfView.contains("scroll.setFitToWidth(false)"));
        assertTrue(pdfView.contains("ScrollPane.ScrollBarPolicy.AS_NEEDED"));
        assertTrue(renderer.contains("setSubsamplingAllowed(false)"));
    }

    @Test
    void pdfFlowAvoidsTechnicalLabelsInPrimaryUi() throws Exception {
        String indexPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java");
        String problemPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java");
        String outline = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/BuildDocumentOutlineUseCase.java");

        assertTrue(indexPanel.contains("Mejorar busqueda"));
        assertTrue(indexPanel.contains("Paginas del PDF"));
        assertTrue(outline.contains("No se encontro temario confiable"));
        assertTrue(problemPanel.contains("Seleccionar capturas del PDF para asociar a problema"));
        assertTrue(problemPanel.contains("captura(s) PDF seleccionadas"));
        assertFalse(indexPanel.contains("Buscar con OCR local"));
    }

    @Test
    void pdfRegionSelectionUsesPageOverlayNotOnlyImageNode() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String css = read("src/main/resources/css/document/study-problem.css")
                + read("src/main/resources/css/audio-jobs.css");

        assertTrue(pdfView.contains("selectionOverlay.addEventFilter(MouseEvent.MOUSE_PRESSED"));
        assertTrue(pdfView.contains("imagePointFromOverlay"));
        assertTrue(pdfView.contains("selectionOverlay.resizeRelocate"));
        assertFalse(pdfView.contains("imageView.addEventHandler(MouseEvent.MOUSE_PRESSED"));
        assertFalse(css.contains("-fx-alignment: stretch"));
    }

    @Test
    void pdfDocumentProgressFollowsVisualScrollInsteadOfSelectedBlock() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");

        assertTrue(pdfView.contains("documentProgressProperty"));
        assertTrue(pdfView.contains("scroll.vvalueProperty().addListener((obs, oldValue, newValue) -> updateScrollProgress())"));
        assertTrue(workspace.contains("updatePdfVisualDocumentProgress"));
        assertTrue(viewModel.contains("pdfVisualDocumentProgressProperty"));
        assertTrue(status.contains("document.format() == SourceDocumentFormat.PDF"));
        assertTrue(status.contains("pdfVisualProgress(pdfVisualDocumentProgress)"));
        assertTrue(status.contains("En PDF se calcula por la posicion del scroll"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
