package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PdfTextLayerBlockMapperTest {

    @Test
    void separatesDistantProseAndStandaloneFormulaAfterTallTextBox() {
        PdfTextLayer layer = new PdfTextLayer(15, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(
                        line("Intuición. Una condición de un if no es solamente sintaxis.",
                                95, 75, 512, 134),
                        line("Muchos enunciados algorítmicos hablan de todos los elementos.",
                                79, 208, 527, 233),
                        line("a i ≥ 0", 332, 280, 361, 291)),
                List.of());

        PreparedPdfPage page = new PdfTextLayerBlockMapper().preparedPage(
                layer, 595, 842, "source-sha", null,
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin.OCR_LOCAL,
                "test", List.of());

        assertEquals(3, page.regions().size());
        assertEquals("Intuición. Una condición de un if no es solamente sintaxis.",
                page.regions().get(0).text());
        assertEquals("a i ≥ 0", page.regions().get(2).text());
        assertFalse(page.regions().get(1).text().contains("a i ≥ 0"));
    }

    @Test
    void keepsNearbyLinesInsideOneParagraph() {
        PdfTextLayer layer = new PdfTextLayer(15, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(
                        line("Primera línea de una explicación.", 70, 100, 500, 112),
                        line("Segunda línea de la misma explicación.", 70, 116, 500, 128)),
                List.of());

        PreparedPdfPage page = new PdfTextLayerBlockMapper().preparedPage(
                layer, 595, 842, "source-sha", null,
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin.OCR_LOCAL,
                "test", List.of());

        assertEquals(1, page.regions().size());
        assertEquals("Primera línea de una explicación. Segunda línea de la misma explicación.",
                page.regions().getFirst().text());
    }

    @Test
    void separatesSemanticCalloutFromFollowingBodyParagraphs() {
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.OCR_LOCAL,
                List.of(
                        lineOnPage(1,
                                "La pregunta. Al sustituir directamente, aparece una indeterminación.",
                                45, 89, 550, 122),
                        lineOnPage(1,
                                "En cálculo, algunos límites actúan como piezas estructurales.",
                                45, 136, 550, 160),
                        lineOnPage(1,
                                "La palabra decisiva es radianes y explica la construcción.",
                                45, 188, 550, 212)),
                List.of());

        PreparedPdfPage page = new PdfTextLayerBlockMapper().preparedPage(
                layer, 595, 842, "source-sha", null,
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin.OCR_LOCAL,
                "test", List.of());

        assertEquals(3, page.regions().size());
        assertEquals(
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType.SIDEBAR,
                page.regions().getFirst().automaticType());
        assertEquals("En cálculo, algunos límites actúan como piezas estructurales.",
                page.regions().get(1).text());
    }

    private static PdfTextLine line(String text, double xMin, double yMin,
                                    double xMax, double yMax) {
        return lineOnPage(15, text, xMin, yMin, xMax, yMax);
    }

    private static PdfTextLine lineOnPage(int page, String text,
                                          double xMin, double yMin,
                                          double xMax, double yMax) {
        return new PdfTextLine(page, text,
                new PdfPageRegion(page, xMin, yMin, xMax, yMax, 595, 842),
                List.of(), 0.96);
    }
}
