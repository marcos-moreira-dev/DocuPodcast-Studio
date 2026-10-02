package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfRegionalQualityAndAdaptiveOcrTest {
    @Test
    void reportsReliableProseAndDamagedFormulaIndependently() {
        PdfTextLayer layer = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX, List.of(
                line("This is a reliable paragraph with enough ordinary words.", 30, 80, 520, 110),
                line("\uFFFD \uFFFD x = = \uFFFD", 30, 150, 300, 175)), List.of());

        PdfNativeTextQualityReport report = new PdfNativeTextQualityAssessor().assess(layer);

        assertEquals(2, report.regions().size());
        assertEquals(PdfNativeTextQuality.RELIABLE, report.regions().getFirst().quality());
        assertNotEquals(PdfNativeTextQuality.RELIABLE, report.regions().getLast().quality());
    }

    @Test
    void choosesColumnAndSparseSegmentationModesFromGeometry() {
        PdfAdaptiveOcrPolicy policy = new PdfAdaptiveOcrPolicy();
        PdfTextLayer columns = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX, List.of(
                line("left one", 20, 40, 250, 60), line("left two", 20, 80, 250, 100),
                line("left three", 20, 120, 250, 140), line("right one", 340, 40, 580, 60),
                line("right two", 340, 80, 580, 100), line("right three", 340, 120, 580, 140)), List.of());
        PdfTextLayer sparse = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(line("label", 40, 40, 90, 55), line("other", 500, 700, 560, 720)), List.of());

        assertEquals(4, policy.pageSegmentationMode(columns));
        assertEquals(11, policy.pageSegmentationMode(sparse));
    }

    private static PdfTextLine line(String text, double x1, double y1, double x2, double y2) {
        return new PdfTextLine(1, text,
                new PdfPageRegion(1, x1, y1, x2, y2, 612, 792), List.of(), 0.9);
    }
}
