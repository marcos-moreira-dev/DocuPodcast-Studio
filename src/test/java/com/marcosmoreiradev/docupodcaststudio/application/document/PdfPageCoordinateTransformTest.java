package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PdfPageCoordinateTransformTest {
    private static final double EPSILON = 0.001;

    @Test
    void derivesDisplayedDimensionsForEveryQuarterTurn() {
        assertDimensions(0, 600, 800);
        assertDimensions(90, 800, 600);
        assertDimensions(180, 600, 800);
        assertDimensions(270, 800, 600);
    }

    @Test
    void roundTripsRasterPointAndKeepsBoxInvariantAcrossZoom() {
        PdfPageCoordinateTransform transform = new PdfPageCoordinateTransform(
                800, 600, 90, 1600, 1200);
        PdfPageRegion region = new PdfPageRegion(1, 80, 120, 400, 300, 800, 600);

        PdfPageCoordinateTransform.RasterBox raster = transform.toRaster(region);
        PdfPageCoordinateTransform.PagePoint point = transform.rasterToPage(raster.xMin(), raster.yMin());
        PdfPageCoordinateTransform.ViewportBox atOne = transform.toViewport(region, 0, 0, 800, 600);
        PdfPageCoordinateTransform.ViewportBox atTwo = transform.toViewport(region, 15, 25, 1600, 1200);

        assertEquals(region.xMinPoints(), point.xPoints(), EPSILON);
        assertEquals(region.yMinPoints(), point.yPoints(), EPSILON);
        assertEquals(atOne.width() * 2, atTwo.width(), EPSILON);
        assertEquals(atOne.height() * 2, atTwo.height(), EPSILON);
        assertEquals(1.0, intersectionOverUnion(
                atOne.xMin(), atOne.yMin(), atOne.xMax(), atOne.yMax(),
                (atTwo.xMin() - 15) / 2, (atTwo.yMin() - 25) / 2,
                (atTwo.xMax() - 15) / 2, (atTwo.yMax() - 25) / 2), EPSILON);
    }

    @Test
    void normalizedPlacementIsInvariantAt75To150PercentZoom() {
        PdfPageCoordinateTransform transform = new PdfPageCoordinateTransform(
                612, 792, 0, 1224, 1584);
        PdfPageRegion region = new PdfPageRegion(1, 72, 100, 420, 142, 612, 792);

        for (double zoom : new double[]{0.75, 1.0, 1.25, 1.5}) {
            var box = transform.toViewport(region, 0, 0, 612 * zoom, 792 * zoom);
            assertEquals(region.xMinPoints() / 612, box.xMin() / (612 * zoom), EPSILON);
            assertEquals(region.yMinPoints() / 792, box.yMin() / (792 * zoom), EPSILON);
            assertEquals(region.xMaxPoints() / 612, box.xMax() / (612 * zoom), EPSILON);
            assertEquals(region.yMaxPoints() / 792, box.yMax() / (792 * zoom), EPSILON);
        }
    }

    private static void assertDimensions(int rotation, double expectedWidth, double expectedHeight) {
        PdfPageCoordinateTransform transform = PdfPageCoordinateTransform.fromSourcePage(
                600, 800, rotation, expectedWidth * 2, expectedHeight * 2);
        assertEquals(expectedWidth, transform.pageWidthPoints(), EPSILON);
        assertEquals(expectedHeight, transform.pageHeightPoints(), EPSILON);
        assertEquals(rotation, transform.rotationDegrees());
    }

    private static double intersectionOverUnion(
            double ax1, double ay1, double ax2, double ay2,
            double bx1, double by1, double bx2, double by2
    ) {
        double intersection = Math.max(0, Math.min(ax2, bx2) - Math.max(ax1, bx1))
                * Math.max(0, Math.min(ay2, by2) - Math.max(ay1, by1));
        double areaA = (ax2 - ax1) * (ay2 - ay1);
        double areaB = (bx2 - bx1) * (by2 - by1);
        return intersection / (areaA + areaB - intersection);
    }
}
