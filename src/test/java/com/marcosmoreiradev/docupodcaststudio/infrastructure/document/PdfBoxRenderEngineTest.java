package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfCropRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfOpenOptions;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderErrorCode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRenderException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfBoxRenderEngineTest {
    @TempDir
    Path tempDir;

    private final PdfBoxRenderEngine engine = new PdfBoxRenderEngine();

    @Test
    void rendersMinimalPdfPageAsNonEmptyImage() throws Exception {
        Path pdf = createPdf("minimal.pdf", 1, true);

        var result = engine.renderPage(new PdfPageRenderRequest(pdf, 1, 72, 4_000_000, Color.WHITE, true));

        assertEquals(1, result.pageNumber());
        assertEquals(1, result.pageCount());
        assertTrue(result.image().getWidth() > 0);
        assertTrue(result.image().getHeight() > 0);
        assertNotEquals(Color.WHITE.getRGB(), result.image().getRGB(90, 120));
    }

    @Test
    void inspectsMultipagePdfPageCount() throws Exception {
        Path pdf = createPdf("multipage.pdf", 3, false);

        var info = engine.inspect(pdf, PdfOpenOptions.empty());

        assertEquals(3, info.pageCount());
        assertEquals(3, info.pages().size());
        assertTrue(info.visuallyRenderable());
    }

    @Test
    void rejectsOutOfRangePage() throws Exception {
        Path pdf = createPdf("one-page.pdf", 1, false);

        PdfRenderException ex = assertThrows(PdfRenderException.class,
                () -> engine.renderPage(new PdfPageRenderRequest(pdf, 2, 72, 4_000_000, Color.WHITE, true)));

        assertEquals(PdfRenderErrorCode.PAGE_OUT_OF_RANGE, ex.code());
    }

    @Test
    void rejectsCorruptPdfAsInvalidPdf() throws Exception {
        Path pdf = tempDir.resolve("corrupt.pdf");
        Files.writeString(pdf, "not a pdf");

        PdfRenderException ex = assertThrows(PdfRenderException.class,
                () -> engine.inspect(pdf, PdfOpenOptions.empty()));

        assertEquals(PdfRenderErrorCode.INVALID_PDF, ex.code());
    }

    @Test
    void reportsPasswordRequiredForEncryptedPdf() throws Exception {
        Path pdf = createEncryptedPdf();

        PdfRenderException ex = assertThrows(PdfRenderException.class,
                () -> engine.inspect(pdf, PdfOpenOptions.empty()));

        assertEquals(PdfRenderErrorCode.PASSWORD_REQUIRED, ex.code());
    }

    @Test
    void reducesDpiToRespectPixelLimit() throws Exception {
        Path pdf = createPdf("dpi-limit.pdf", 1, false);

        var result = engine.renderPage(new PdfPageRenderRequest(pdf, 1, 300, 1_000_000, Color.WHITE, true));

        assertTrue(result.dpi() < 300);
        assertTrue((long) result.image().getWidth() * result.image().getHeight() <= 1_000_000);
    }

    @Test
    void failsWhenMinimumDpiStillExceedsPixelLimit() throws Exception {
        Path pdf = createPdf("too-large.pdf", 1, false);

        PdfRenderException ex = assertThrows(PdfRenderException.class,
                () -> engine.renderPage(new PdfPageRenderRequest(pdf, 1, 300, 10, Color.WHITE, true)));

        assertEquals(PdfRenderErrorCode.TOO_LARGE, ex.code());
    }

    @Test
    void rendersCropWithinPageBounds() throws Exception {
        Path pdf = createPdf("crop.pdf", 1, true);

        var crop = engine.renderCrop(new PdfCropRenderRequest(pdf, 1,
                60, 90, 210, 240, 0, 72, 4_000_000, Color.WHITE, true));

        assertEquals("pdfbox-crop", crop.renderMode());
        assertTrue(crop.image().getWidth() < 612);
        assertTrue(crop.image().getHeight() < 792);
        assertTrue(crop.image().getWidth() > 0);
        assertTrue(crop.image().getHeight() > 0);
    }

    @Test
    void reportsDisplayedDimensionsForRotatedPageAndCropsInThatSpace() throws Exception {
        Path pdf = tempDir.resolve("rotated.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(600, 800));
            page.setRotation(90);
            document.addPage(page);
            document.save(pdf.toFile());
        }

        var rendered = engine.renderPage(new PdfPageRenderRequest(
                pdf, 1, 72, 4_000_000, Color.WHITE, true));
        var crop = engine.renderCrop(new PdfCropRenderRequest(
                pdf, 1, 100, 60, 300, 180, 0, 72, 4_000_000, Color.WHITE, true));

        assertEquals(800, rendered.pageWidthPoints());
        assertEquals(600, rendered.pageHeightPoints());
        assertEquals(800, rendered.image().getWidth());
        assertEquals(600, rendered.image().getHeight());
        assertEquals(200, crop.image().getWidth());
        assertEquals(120, crop.image().getHeight());
    }

    @Test
    void usesDisplacedCropBoxAsCanonicalPageOrigin() throws Exception {
        Path pdf = tempDir.resolve("displaced-crop-box.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(600, 800));
            page.setCropBox(new PDRectangle(50, 40, 400, 500));
            document.addPage(page);
            document.save(pdf.toFile());
        }

        var info = engine.inspect(pdf, PdfOpenOptions.empty());
        var rendered = engine.renderPage(new PdfPageRenderRequest(
                pdf, 1, 72, 4_000_000, Color.WHITE, true));

        assertEquals(400, info.pages().getFirst().widthPoints());
        assertEquals(500, info.pages().getFirst().heightPoints());
        assertEquals(400, rendered.pageWidthPoints());
        assertEquals(500, rendered.pageHeightPoints());
        assertEquals(400, rendered.image().getWidth());
        assertEquals(500, rendered.image().getHeight());
    }

    private Path createPdf(String name, int pageCount, boolean drawBlackBox) throws Exception {
        Path pdf = tempDir.resolve(name);
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 720);
                    content.showText("DocuPodcast PDF page " + (i + 1));
                    content.endText();
                    if (drawBlackBox) {
                        content.setNonStrokingColor(Color.BLACK);
                        content.addRect(80, 650, 80, 80);
                        content.fill();
                    }
                }
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }

    private Path createEncryptedPdf() throws Exception {
        Path pdf = tempDir.resolve("encrypted.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            AccessPermission permissions = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-pass", "user-pass", permissions);
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(pdf.toFile());
        }
        return pdf;
    }
}
