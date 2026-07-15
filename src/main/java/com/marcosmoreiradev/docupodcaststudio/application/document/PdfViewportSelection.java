package com.marcosmoreiradev.docupodcaststudio.application.document;

/**
 * User-drawn rectangular selection over a rendered PDF page viewport.
 *
 * <p>Viewport coordinates are in pixels relative to the rendered page image, not the whole scroll view.</p>
 */
public record PdfViewportSelection(
        int pageNumber,
        double xMin,
        double yMin,
        double xMax,
        double yMax,
        double viewportWidth,
        double viewportHeight,
        double pageWidthPoints,
        double pageHeightPoints
) {
    public PdfViewportSelection {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be 1-based");
        }
        viewportWidth = positive(viewportWidth, "viewportWidth");
        viewportHeight = positive(viewportHeight, "viewportHeight");
        pageWidthPoints = positive(pageWidthPoints, "pageWidthPoints");
        pageHeightPoints = positive(pageHeightPoints, "pageHeightPoints");
        double left = Math.min(xMin, xMax);
        double right = Math.max(xMin, xMax);
        double top = Math.min(yMin, yMax);
        double bottom = Math.max(yMin, yMax);
        xMin = clamp(left, 0.0, viewportWidth);
        xMax = clamp(right, 0.0, viewportWidth);
        yMin = clamp(top, 0.0, viewportHeight);
        yMax = clamp(bottom, 0.0, viewportHeight);
        if (!(xMax > xMin) || !(yMax > yMin)) {
            throw new IllegalArgumentException("Viewport selection must have positive width and height");
        }
    }

    public PdfPageRegion toPageRegion() {
        double scaleX = pageWidthPoints / viewportWidth;
        double scaleY = pageHeightPoints / viewportHeight;
        return new PdfPageRegion(
                pageNumber,
                xMin * scaleX,
                yMin * scaleY,
                xMax * scaleX,
                yMax * scaleY,
                pageWidthPoints,
                pageHeightPoints);
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
}
