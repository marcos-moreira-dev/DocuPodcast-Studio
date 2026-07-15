package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisualReadingOcrT109T110SourceTest {
    @Test
    void pdfVisualReadingHighlightUsesApplicationProjection() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String css = read("src/main/resources/css/document/pdf-visual-viewer.css");

        assertTrue(services.contains("BuildPdfVisualReadingProjectionUseCase buildPdfVisualReadingProjection"));
        assertTrue(factory.contains("new BuildPdfVisualReadingProjectionUseCase(buildPdfNativeTextLayer)"));
        assertTrue(workspace.contains("updatePdfVisualReadingHighlight"));
        assertTrue(workspace.contains("buildPdfVisualReadingProjection()"));
        assertTrue(pdfView.contains("showTextHighlight"));
        assertTrue(pdfView.contains("clearTextHighlight"));
        assertTrue(pdfView.contains("pdf-visual-text-highlight"));
        assertTrue(css.contains(".pdf-visual-text-highlight"));
        assertTrue(css.contains("109, 40, 217"));
        assertFalse(css.contains("250, 204, 21"));
        assertFalse(workspace.contains("PdfBoxRenderEngine"));
    }

    @Test
    void localOcrBackendIsTesseractThroughCommonRunnerAndFeedsTextLayer() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/TesseractPdfOcrEngine.java");
        String parser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfOcrTsvParser.java");

        assertTrue(services.contains("BuildPdfOcrTextLayerUseCase buildPdfOcrTextLayer"));
        assertTrue(factory.contains("TesseractRuntimeLocator"));
        assertTrue(factory.contains("new TesseractPdfOcrEngine(pdfRenderEngine, processRunner, tesseractCommand)"));
        assertTrue(engine.contains("ExternalProcessRunner"));
        assertTrue(engine.contains("Supplier<String> tesseractCommand"));
        assertTrue(engine.contains("resolvedTesseractCommand()"));
        assertTrue(engine.contains("\"--tessdata-dir\""));
        assertTrue(engine.contains("resolvedTessdataDirectory(command)"));
        assertTrue(engine.contains("\"tsv\""));
        assertTrue(engine.contains("pdf-ocr-text"));
        assertTrue(engine.contains("writePlainTextPageCache"));
        assertTrue(engine.contains("cacheDirectory.resolve(\"text\")"));
        assertTrue(engine.contains("deletePlainTextCacheDirectory"));
        assertTrue(engine.contains("PdfTextLayerOrigin.OCR_LOCAL") || parser.contains("PdfTextLayerOrigin.OCR_LOCAL"));
        assertFalse(engine.contains("new ProcessBuilder"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
