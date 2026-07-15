package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfSourceCropRendererTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsPdfPointBoxToPixelCropAreaWithPaddingAndClamp() {
        PdfSourceCropRenderer.CropArea area = PdfSourceCropRenderer.cropArea(
                new PdfBox(10, 20, 60, 80), 100, 100, 200, 300, 5).orElseThrow();

        assertEquals(10, area.x());
        assertEquals(45, area.y());
        assertEquals(120, area.width());
        assertEquals(210, area.height());
    }

    @Test
    void writesPngCropFromRenderedPageImage() throws Exception {
        BufferedImage source = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 100, 80);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(20, 10, 40, 30);
        graphics.dispose();
        Path output = tempDir.resolve("crop.png");

        PdfSourceCropRenderer.writeCrop(source, new PdfSourceCropRenderer.CropArea(20, 10, 40, 30), output);

        assertTrue(Files.isRegularFile(output));
        BufferedImage crop = ImageIO.read(output.toFile());
        assertEquals(40, crop.getWidth());
        assertEquals(30, crop.getHeight());
    }

    @Test
    void rendersCropWithEmbeddedPdfBoxBeforePopplerFallback() throws Exception {
        Path pdf = tempDir.resolve("embedded-crop.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setNonStrokingColor(Color.BLACK);
                content.addRect(80, 650, 100, 80);
                content.fill();
            }
            document.save(pdf.toFile());
        }
        DocumentBlock block = DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "crop", "PDF",
                Map.of("sourcePage", "1", "bbox", "70,60,190,160"));
        Path output = tempDir.resolve("embedded.png");
        ExternalProcessRunner failingPoppler = request -> {
            throw new AssertionError("Poppler fallback should not run when PDFBox renders the crop.");
        };

        Optional<Path> rendered = new PdfSourceCropRenderer(failingPoppler).renderBlockCrop(pdf, block, output);

        assertTrue(rendered.isPresent());
        assertTrue(Files.isRegularFile(output));
        BufferedImage crop = ImageIO.read(output.toFile());
        assertTrue(crop.getWidth() > 0);
        assertTrue(crop.getHeight() > 0);
    }
}
