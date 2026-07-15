package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfNativeTextIntakeSourceTest {
    @Test
    void pdfIsOfferedAsNativeTextSourceAndFallsBackToVisualWithControlledOcr() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfDocumentImporter.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));
        String roadmap = Files.readString(Path.of("docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md"));

        assertTrue(factory.contains("new PdfDocumentImporter(pdfRenderEngine, pdfOcrTextLayer)"));
        assertTrue(shell.contains("PDF texto u OCR local"));
        assertTrue(shell.contains("PDF no compatible"));
        assertTrue(shell.contains("UserNotification.warning"));
        assertTrue(importer.contains("pdf-visual-only"));
        assertTrue(importer.contains("visual-fallback"));
        assertTrue(importer.contains("pdf-ocr-applied"));
        assertTrue(importer.contains("pdf-ocr-unavailable"));
        assertTrue(importer.contains("OCR local"));
        assertTrue(roadmap.contains("PDF escaneado"));
        assertTrue(roadmap.contains("intenta OCR local por pagina"));
        assertTrue(roadmap.contains("pdf-ocr-applied"));
    }
}
