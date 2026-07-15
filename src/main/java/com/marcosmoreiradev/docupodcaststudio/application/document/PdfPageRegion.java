package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Rectangular PDF page region in PDF points, page number is 1-based. */
public record PdfPageRegion(
        int pageNumber,
        double xMinPoints,
        double yMinPoints,
        double xMaxPoints,
        double yMaxPoints,
        double pageWidthPoints,
        double pageHeightPoints
) {
    public PdfPageRegion {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be 1-based");
        }
        pageWidthPoints = positive(pageWidthPoints, "pageWidthPoints");
        pageHeightPoints = positive(pageHeightPoints, "pageHeightPoints");
        xMinPoints = clamp(xMinPoints, 0.0, pageWidthPoints);
        xMaxPoints = clamp(xMaxPoints, 0.0, pageWidthPoints);
        yMinPoints = clamp(yMinPoints, 0.0, pageHeightPoints);
        yMaxPoints = clamp(yMaxPoints, 0.0, pageHeightPoints);
        if (!(xMaxPoints > xMinPoints) || !(yMaxPoints > yMinPoints)) {
            throw new IllegalArgumentException("PDF page region must have positive width and height");
        }
    }

    public String bbox() {
        return format(xMinPoints) + "," + format(yMinPoints) + "," + format(xMaxPoints) + "," + format(yMaxPoints);
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

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
