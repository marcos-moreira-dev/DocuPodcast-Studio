package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNativeTextQuality;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNativeTextQualityAssessor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfBoxNativeTextEvidenceExtractorTest {
    @TempDir Path temp;

    @Test
    void rotatedAndCroppedPagesKeepTextInsideRenderedPageCoordinates() throws Exception {
        Path pdf = temp.resolve("rotated.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int rotation : new int[]{0, 90, 180, 270}) {
                PDPage page = new PDPage();
                page.setCropBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(20, 30, 500, 700));
                page.setRotation(rotation);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(80, 650);
                    content.showText("Texto legible: posición 3, valor 8.");
                    content.endText();
                }
            }
            document.save(pdf.toFile());
        }
        var extractor = new PdfBoxNativeTextEvidenceExtractor();
        var original = extractor.extract(pdf, 1).lines().getFirst().region();
        for (int number = 1; number <= 4; number++) {
            var layer = extractor.extract(pdf, number);
            assertTrue(layer.available());
            assertTrue(layer.lines().getFirst().text().contains("valor 8"));
            var box = layer.lines().getFirst().region();
            assertEquals(number % 2 == 0 ? 700 : 500, box.pageWidthPoints());
            assertEquals(number % 2 == 0 ? 500 : 700, box.pageHeightPoints());
            assertTrue(box.xMinPoints() >= 0 && box.yMinPoints() >= 0);
            assertTrue(box.xMaxPoints() <= box.pageWidthPoints());
            assertTrue(box.yMaxPoints() <= box.pageHeightPoints());
            if (number == 2) {
                assertEquals(700 - original.yMaxPoints(), box.xMinPoints(), 0.01);
                assertEquals(original.xMinPoints(), box.yMinPoints(), 0.01);
            }
        }
    }

    @Test
    void extractsObservableNativeEvidenceAndReusesQualityPolicy() throws Exception {
        Path pdf = temp.resolve("native.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(60, 700);
                content.showText("Titulo principal de cobertura semantica");
                content.newLineAtOffset(0, -30);
                content.showText("Este parrafo contiene suficiente texto nativo fiable para validar una pagina.");
                content.endText();
            }
            document.save(pdf.toFile());
        }

        var layer = new PdfBoxNativeTextEvidenceExtractor().extract(pdf, 1);
        var report = new PdfNativeTextQualityAssessor().assess(layer);

        assertTrue(layer.available());
        assertFalse(layer.lines().isEmpty());
        assertTrue(layer.lines().stream().allMatch(line ->
                line.region().pageWidthPoints() > 0
                        && line.region().pageHeightPoints() > 0));
        assertTrue(layer.lines().stream().anyMatch(line ->
                line.text().equals("Titulo principal de cobertura semantica")));
        assertTrue(layer.lines().stream().anyMatch(line ->
                line.text().startsWith("Este parrafo contiene suficiente texto")));
        assertEquals(PdfNativeTextQuality.RELIABLE, report.quality());
    }
}
