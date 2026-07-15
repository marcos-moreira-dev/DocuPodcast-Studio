package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrErrorCode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrLine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOcrWord;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayer;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfDocumentImporterTest {
    @Test
    void importsNativePdfAsVisualOnlyWithDeferredOcr() throws Exception {
        Path pdf = Files.createTempFile("docupodcast-native-text", ".pdf");
        Files.writeString(pdf, nativeTextPdf(), StandardCharsets.ISO_8859_1);

        var document = importerWithFailingOcr().importDocument(pdf);

        assertEquals(SourceDocumentFormat.PDF, document.format());
        assertEquals(0, document.wordCount());
        assertTrue(document.blocks().stream().noneMatch(block -> block.narratable()));
        assertTrue(document.blocks().stream().allMatch(block -> block.sourceVisual()));
        assertTrue(document.blocks().stream().noneMatch(block -> "true".equals(block.metadata().get("nativeText"))));
        assertTrue(document.blocks().stream().anyMatch(block -> block.metadata().containsKey("sourcePage")));
        assertTrue(document.blocks().stream().anyMatch(block -> block.metadata().containsKey("extractionMode")));
        assertTrue(document.blocks().stream().noneMatch(block -> "true".equals(block.metadata().get("ocr"))));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-deferred")));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-visual-only")));
    }

    @Test
    void nativePdfDoesNotImportPopplerBboxMetadata() throws Exception {
        Path pdf = Files.createTempFile("docupodcast-bbox-text", ".pdf");
        Files.writeString(pdf, nativeTextPdf(), StandardCharsets.ISO_8859_1);

        var document = importerWithFailingOcr().importDocument(pdf);

        assertTrue(document.blocks().stream().allMatch(block -> "visual-fallback".equals(block.metadata().get("extractionMode"))));
        assertTrue(document.blocks().stream().noneMatch(block -> block.narratable()));
        assertFalse(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-poppler-bbox-layout")));
    }

    @Test
    void opensImageOnlyOrScannedPdfAsVisualFallback() throws Exception {
        Path pdf = Files.createTempFile("docupodcast-scanned", ".pdf");
        Files.writeString(pdf, imageOnlyPdf(), StandardCharsets.ISO_8859_1);

        var document = importerWithFailingOcr().importDocument(pdf);

        assertEquals(SourceDocumentFormat.PDF, document.format());
        assertEquals(1, document.blocks().size());
        assertTrue(document.blocks().getFirst().sourceVisual());
        assertEquals("false", document.blocks().getFirst().metadata().get("nativeText"));
        assertEquals("visual-fallback", document.blocks().getFirst().metadata().get("extractionMode"));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-deferred")));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-visual-only")));
        assertFalse(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-pending")));
    }

    @Test
    void imageOnlyPdfDefersOcrEvenWhenLocalOcrCanProduceText() throws Exception {
        Path pdf = Files.createTempFile("docupodcast-scanned-ocr", ".pdf");
        Files.writeString(pdf, imageOnlyPdf(), StandardCharsets.ISO_8859_1);

        var document = importerWithOcrText("""
                Metodo de biseccion para ecuaciones no lineales.
                El intervalo se actualiza segun el cambio de signo.
                """).importDocument(pdf);

        assertEquals(SourceDocumentFormat.PDF, document.format());
        assertEquals(0, document.wordCount());
        assertTrue(document.blocks().stream().noneMatch(block -> block.narratable()));
        var block = document.blocks().getFirst();
        assertEquals("false", block.metadata().get("ocr"));
        assertEquals("false", block.metadata().get("nativeText"));
        assertEquals("visual-fallback", block.metadata().get("extractionMode"));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-deferred")));
        assertFalse(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-applied")));
        assertFalse(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-ocr-pending")));
    }

    @Test
    void visualPdfUsesEmbeddedRendererForRealPageCount() throws Exception {
        Path pdf = Files.createTempFile("docupodcast-visual-render", ".pdf");
        createPdfBoxPdf(pdf, 2, false);

        var document = importerWithFailingOcr().importDocument(pdf);

        assertEquals(2, document.blocks().size());
        assertTrue(document.blocks().stream().allMatch(block -> block.sourceVisual()));
        assertTrue(document.blocks().stream().allMatch(block -> "2".equals(block.metadata().get("sourcePageCount"))));
        assertTrue(document.blocks().stream().allMatch(block -> "true".equals(block.metadata().get("visualRenderAvailable"))));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-embedded-renderer-available")));
    }

    @Test
    void nativeTextPdfStillDeclaresVisualRenderingAvailabilityWithoutNativeBlocks() throws Exception {
        Path pdf = Files.createTempFile("docupodcast-native-render", ".pdf");
        createPdfBoxPdf(pdf, 1, true);

        var document = importerWithFailingOcr().importDocument(pdf);

        assertEquals(0, document.wordCount());
        assertTrue(document.blocks().stream().allMatch(block -> block.sourceVisual()));
        assertTrue(document.blocks().stream().noneMatch(block -> "true".equals(block.metadata().get("nativeText"))));
        assertTrue(document.blocks().stream().allMatch(block -> "true".equals(block.metadata().get("visualRenderAvailable"))));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("pdf-embedded-renderer-available")));
    }

    private static String nativeTextPdf() {
        String content = "BT /F1 12 Tf 72 720 Td (Documento PDF con texto nativo para DocuPodcast Studio H O L A) Tj "
                + "0 -16 Td (Este contenido puede leerse y convertirse en documento narrable sin OCR.) Tj ET\n";
        String[] objects = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + content.length() + " >> stream\n" + content + "endstream"
        };
        StringBuilder out = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = out.length();
            out.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        int xref = out.length();
        out.append("xref\n0 ").append(objects.length + 1).append("\n");
        out.append("0000000000 65535 f \n");
        for (int i = 1; i < offsets.length; i++) {
            out.append(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        }
        out.append("trailer << /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\n");
        out.append("startxref\n").append(xref).append("\n%%EOF\n");
        return out.toString();
    }

    private static String imageOnlyPdf() {
        return "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /Resources << /XObject << /Im1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                + "4 0 obj << /Type /XObject /Subtype /Image /Width 10 /Height 10 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Length 0 >> stream\n\nendstream endobj\n"
                + "5 0 obj << /Length 30 >> stream\nq 10 0 0 10 0 0 cm /Im1 Do Q\nendstream endobj\n%%EOF";
    }

    private static void createPdfBoxPdf(Path target, int pageCount, boolean withText) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.setNonStrokingColor(Color.BLACK);
                    content.addRect(72, 620, 120, 80);
                    content.fill();
                    if (withText) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(72, 720);
                        content.showText("Documento PDF nativo renderizable con texto suficiente para lectura tecnica.");
                        content.newLineAtOffset(0, -16);
                        content.showText("Este contenido conserva bloques narrables y soporte visual embebido.");
                        content.endText();
                    }
                }
            }
            document.save(target.toFile());
        }
    }

    private static PdfDocumentImporter importerWithFailingOcr() {
        return new PdfDocumentImporter(new PdfBoxRenderEngine(), new BuildPdfOcrTextLayerUseCase(request -> {
            throw new PdfOcrException(PdfOcrErrorCode.TESSERACT_NOT_FOUND, "OCR no disponible en prueba.");
        }));
    }

    private static PdfDocumentImporter importerWithOcrText(String text) {
        return new PdfDocumentImporter(new PdfBoxRenderEngine(), new BuildPdfOcrTextLayerUseCase(request -> {
            List<PdfOcrLine> lines = new ArrayList<>();
            int y = 90;
            for (String raw : text.split("\\R")) {
                if (raw.isBlank()) {
                    continue;
                }
                PdfPageRegion region = new PdfPageRegion(request.pageNumber(), 72, y, 520, y + 18, 612, 792);
                PdfOcrWord word = new PdfOcrWord(raw, region, 0.92);
                lines.add(new PdfOcrLine(request.pageNumber(), raw, region, List.of(word), 0.92));
                y += 22;
            }
            PdfTextLayer layer = new PdfTextLayer(request.pageNumber(), PdfTextLayerOrigin.OCR_LOCAL,
                    lines.stream().map(PdfOcrLine::toTextLine).toList(), List.of());
            return new PdfOcrPageResult(request.pageNumber(), request.dpi(), 1200, 1600, 612, 792,
                    lines, lines.stream().flatMap(line -> line.words().stream()).toList(), layer, List.of());
        }));
    }
}
