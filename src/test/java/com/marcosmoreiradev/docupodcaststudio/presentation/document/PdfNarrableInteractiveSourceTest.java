package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfNarrableInteractiveSourceTest {
    @Test
    void pdfViewerHasHoverPinnedAndPlaybackOverlaysWithoutWordBoxes() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String css = read("src/main/resources/css/document/pdf-visual-viewer.css");

        assertTrue(pdfView.contains("setReadingProjection"));
        assertTrue(pdfView.contains("setTextTargetSelectionHandler"));
        assertTrue(pdfView.contains("setTextPreparationRequestHandler"));
        assertTrue(pdfView.contains("visiblePageNumberProperty"));
        assertTrue(pdfView.contains("PendingTextTargetRequest"));
        assertTrue(pdfView.contains("retryPendingTextTargetSelection"));
        assertTrue(pdfView.contains("MouseEvent.MOUSE_MOVED"));
        assertTrue(pdfView.contains("targetAt(page.pageNumber()"));
        assertTrue(pdfView.contains("showPinnedTextTarget"));
        assertTrue(workspace.contains("selectPdfTextTarget"));
        assertTrue(workspace.contains("PdfVisibleTextPreparationCoordinator"));
        assertTrue(workspace.contains("targetForRange"));
        assertTrue(workspace.contains("PlaybackCue"));
        assertTrue(css.contains(".pdf-visual-text-hover"));
        assertTrue(css.contains(".pdf-visual-text-selected"));
        assertTrue(css.contains(".pdf-visual-text-highlight-underline"));
        assertFalse(css.contains("-fx-stroke: rgba(109, 40, 217"));
        assertFalse(pdfView.contains("fragmento n"));
        assertFalse(pdfView.contains("Intervencion"));
    }

    @Test
    void pdfReadingAndAudioDoesNotRunOcrAutomatically() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PdfNarratablePreparationCoordinator.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(shell.contains("PdfNarratablePreparationCoordinator"));
        assertTrue(shell.contains("prepareThenRun(viewModel::buildNarrationScriptFromDocument)"));
        assertTrue(shell.contains("prepareThenRun(viewModel::runDocumentPrimaryAction)"));
        assertTrue(shell.contains("prepareThenRun(viewModel::playFromSelectedSegment)"));
        assertTrue(shell.contains("prepareForwardThenRun(() -> viewModel.generateAudioChunksWithoutPlayback())"));
        assertTrue(coordinator.contains("runContinuation(continuation);"));
        assertTrue(coordinator.contains("PDF OCR is intentionally click-driven"));
        assertFalse(coordinator.contains("Task<PdfNarratableDocumentResolution>"));
        assertFalse(coordinator.contains("resolvePdfNarratableDocument()"));
        assertFalse(coordinator.contains("PdfNarratableDocumentRequest"));
        assertTrue(viewModel.contains("PdfVisiblePageAudioStartSelector.firstSegmentAtOrAfterVisiblePage"));
        assertTrue(viewModel.contains("PdfVisiblePageAudioStartSelector.isPdf(document)"));
        assertTrue(viewModel.contains("Haz clic en la hoja para analizar OCR antes de generar audio desde la pagina PDF"));
        assertFalse(coordinator.contains("false, 200"));
        assertFalse(viewModel.contains("ensurePdfNarratableDocument"));
        assertFalse(viewModel.substring(viewModel.indexOf("currentSourceDocumentPath()"),
                Math.min(viewModel.length(), viewModel.indexOf("currentSourceDocumentPath()") + 260)).contains("resolvePdfNarratableDocument"));
    }

    @Test
    void visiblePagePreparationRunsPerPageWithoutBlockingPdfOpen() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisibleTextPreparationCoordinator.java");
        String importer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfDocumentImporter.java");

        assertTrue(workspace.contains("preparePdfVisibleTextPage"));
        assertTrue(workspace.contains("preparePdfTextPageNow"));
        assertTrue(workspace.contains("retryVisiblePdfTextPreparation"));
        assertTrue(workspace.contains("samePdfVisualSource"));
        assertTrue(workspace.contains("Haz clic en la hoja para analizar OCR"));
        assertFalse(workspace.contains("pdfVisibleTextPreparation.prepareVisibleWindow"));
        assertTrue(pdfView.contains("textPreparationRequestHandler.accept(page.pageNumber())"));
        assertTrue(coordinator.contains("prepareVisibleWindow"));
        assertTrue(coordinator.contains("opening or scrolling a PDF must not enqueue analysis"));
        assertTrue(coordinator.contains("preparePageNow"));
        assertTrue(coordinator.contains("new PdfNarratableDocumentRequest(document, cacheDirectorySupplier.get(), false, 1, List.of(page))"));
        assertTrue(coordinator.contains("Analizando pagina PDF \" + page + \" por clic"));
        assertTrue(importer.contains("pdf-ocr-deferred"));
        assertFalse(importer.contains("if (!hasEnoughNativeText"));
        assertFalse(importer.contains("buildOcrBlocks(sourceFile"));
    }

    @Test
    void documentUiDoesNotImportTesseractOrPdfboxDirectly() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PdfNarratablePreparationCoordinator.java");
        String statusBar = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");

        assertFalse(workspace.contains("TesseractPdfOcrEngine"));
        assertFalse(workspace.contains("PdfBoxRenderEngine"));
        assertFalse(pdfView.contains("TesseractPdfOcrEngine"));
        assertFalse(pdfView.contains("PdfBoxRenderEngine"));
        assertFalse(coordinator.contains("TesseractPdfOcrEngine"));
        assertFalse(coordinator.contains("PdfBoxRenderEngine"));
        assertTrue(statusBar.contains("Configurar OCR"));
        assertTrue(statusBar.contains("ocr no configurado"));
        assertTrue(shell.contains("handleOpenOcrSettings"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
