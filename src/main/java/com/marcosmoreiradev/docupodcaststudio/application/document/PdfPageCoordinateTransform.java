package com.marcosmoreiradev.docupodcaststudio.application.document;

/**
 * Converts the canonical displayed PDF coordinate space (top-left, PDF points)
 * to raster or viewport coordinates without losing the page rotation contract.
 */
public record PdfPageCoordinateTransform(
        double pageWidthPoints,
        double pageHeightPoints,
        int rotationDegrees,
        double rasterWidth,
        double rasterHeight
) {
    public PdfPageCoordinateTransform {
        pageWidthPoints = positive(pageWidthPoints, "pageWidthPoints");
        pageHeightPoints = positive(pageHeightPoints, "pageHeightPoints");
        rotationDegrees = normalizeRotation(rotationDegrees);
        rasterWidth = positive(rasterWidth, "rasterWidth");
        rasterHeight = positive(rasterHeight, "rasterHeight");
    }

    public static PdfPageCoordinateTransform fromSourcePage(
            double sourceWidthPoints,
            double sourceHeightPoints,
            int rotationDegrees,
            double rasterWidth,
            double rasterHeight
    ) {
        int rotation = normalizeRotation(rotationDegrees);
        boolean swapsAxes = rotation == 90 || rotation == 270;
        return new PdfPageCoordinateTransform(
                swapsAxes ? sourceHeightPoints : sourceWidthPoints,
                swapsAxes ? sourceWidthPoints : sourceHeightPoints,
                rotation,
                rasterWidth,
                rasterHeight);
    }

    public RasterBox toRaster(PdfPageRegion region) {
        requireSamePageSize(region);
        return new RasterBox(
                region.xMinPoints() * rasterWidth / pageWidthPoints,
                region.yMinPoints() * rasterHeight / pageHeightPoints,
                region.xMaxPoints() * rasterWidth / pageWidthPoints,
                region.yMaxPoints() * rasterHeight / pageHeightPoints);
    }

    public ViewportBox toViewport(
            PdfPageRegion region,
            double viewportX,
            double viewportY,
            double viewportWidth,
            double viewportHeight
    ) {
        requireSamePageSize(region);
        double width = positive(viewportWidth, "viewportWidth");
        double height = positive(viewportHeight, "viewportHeight");
        return new ViewportBox(
                viewportX + region.xMinPoints() * width / pageWidthPoints,
                viewportY + region.yMinPoints() * height / pageHeightPoints,
                viewportX + region.xMaxPoints() * width / pageWidthPoints,
                viewportY + region.yMaxPoints() * height / pageHeightPoints);
    }

    public PagePoint rasterToPage(double rasterX, double rasterY) {
        return new PagePoint(
                clamp(rasterX, 0.0, rasterWidth) * pageWidthPoints / rasterWidth,
                clamp(rasterY, 0.0, rasterHeight) * pageHeightPoints / rasterHeight);
    }

    public static int normalizeRotation(int rotationDegrees) {
        int normalized = Math.floorMod(rotationDegrees, 360);
        if (normalized % 90 != 0) {
            throw new IllegalArgumentException("PDF rotation must be a multiple of 90 degrees");
        }
        return normalized;
    }

    private void requireSamePageSize(PdfPageRegion region) {
        if (region == null) {
            throw new IllegalArgumentException("region is required");
        }
        if (Math.abs(region.pageWidthPoints() - pageWidthPoints) > 0.01
                || Math.abs(region.pageHeightPoints() - pageHeightPoints) > 0.01) {
            throw new IllegalArgumentException("region and page coordinate spaces do not match");
        }
    }

    private static double positive(double value, String field) {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public record PagePoint(double xPoints, double yPoints) {
    }

    public record RasterBox(double xMin, double yMin, double xMax, double yMax) {
    }

    public record ViewportBox(double xMin, double yMin, double xMax, double yMax) {
        public double width() {
            return xMax - xMin;
        }

        public double height() {
            return yMax - yMin;
        }
    }
}
