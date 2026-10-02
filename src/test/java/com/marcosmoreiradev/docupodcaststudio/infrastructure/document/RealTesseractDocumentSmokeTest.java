package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import org.apache.pdfbox.Loader;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrRequest;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in smoke for native and scanned PDFs through the product OCR infrastructure. */
final class RealTesseractDocumentSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void readsNativePdfAndRecognizesScannedPdfWithManagedTesseract() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("docupodcast.realTesseractSmoke.enabled"),
                "real Tesseract smoke is opt-in");
        Path applicationRoot = applicationRoot();
        Path executable = applicationRoot.resolve("tools/tesseract/bin/tesseract.exe").normalize();
        assertTrue(Files.isRegularFile(executable), "Managed Tesseract executable is missing: " + executable);

        Path nativePdf = tempDir.resolve("native.pdf");
        writeNativePdf(nativePdf);
        try (PDDocument nativeDocument = Loader.loadPDF(nativePdf.toFile())) {
            assertTrue(new PDFTextStripper().getText(nativeDocument).contains("DocuPodcast native PDF"),
                    "Native PDF text must remain available without OCR");
        }

        Path scannedPdf = tempDir.resolve("scanned.pdf");
        writeScannedPdf(scannedPdf);
        TesseractPdfOcrEngine engine = new TesseractPdfOcrEngine(
                new PdfBoxRenderEngine(), new DefaultExternalProcessRunner(), executable::toString);
        PdfOcrPageResult result = engine.recognize(new PdfOcrRequest(
                scannedPdf, 1, 200, PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                "spa+eng", true, tempDir.resolve("ocr-cache")));

        String recognized = result.textLayer().lines().stream()
                .map(line -> line.text())
                .reduce("", (left, right) -> left + " " + right)
                .toUpperCase(Locale.ROOT);
        assertFalse(result.words().isEmpty(), "Tesseract did not publish OCR words");
        assertTrue(recognized.contains("DOCUPODCAST") && recognized.contains("OCR"),
                () -> "Unexpected OCR output: " + recognized);
    }

    private static Path applicationRoot() {
        String configured = System.getProperty("docupodcast.app.root");
        if (configured == null || configured.isBlank()) configured = System.getenv("DOCUPODCAST_APP_ROOT");
        return Path.of(configured == null || configured.isBlank() ? "." : configured)
                .toAbsolutePath().normalize();
    }

    private static void writeNativePdf(Path target) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                content.newLineAtOffset(72, 720);
                content.showText("DocuPodcast native PDF keeps readable text for narration and search.");
                content.endText();
            }
            document.save(target.toFile());
        }
    }

    private static void writeScannedPdf(Path target) throws Exception {
        BufferedImage image = new BufferedImage(1400, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 82));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.drawString("DOCUPODCAST OCR TEST", 80, 270);
        } finally {
            graphics.dispose();
        }
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            var pdfImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(pdfImage, 36, 285, 540, 193);
            }
            document.save(target.toFile());
        }
    }
}
