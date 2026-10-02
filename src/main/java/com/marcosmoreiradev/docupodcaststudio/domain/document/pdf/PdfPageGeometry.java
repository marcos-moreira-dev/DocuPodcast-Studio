package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Geometry in the canonical displayed crop-box coordinate system. */
public record PdfPageGeometry(
        double xMin,
        double yMin,
        double xMax,
        double yMax,
        double pageWidth,
        double pageHeight,
        String coordinateSpace
) {
    public static final String CANONICAL_SPACE = "CANONICAL_CROP_TOP_LEFT_POINTS";

    public PdfPageGeometry {
        coordinateSpace = coordinateSpace == null || coordinateSpace.isBlank()
                ? CANONICAL_SPACE : coordinateSpace.strip();
        if (!(pageWidth > 0) || !(pageHeight > 0)
                || !Double.isFinite(xMin) || !Double.isFinite(yMin)
                || !Double.isFinite(xMax) || !Double.isFinite(yMax)
                || xMin < 0 || yMin < 0 || xMax <= xMin || yMax <= yMin
                || xMax > pageWidth + 0.01 || yMax > pageHeight + 0.01) {
            throw new IllegalArgumentException("Invalid PdfPageMap geometry");
        }
    }

    public double intersectionOverUnion(PdfPageGeometry other) {
        if (other == null || !coordinateSpace.equals(other.coordinateSpace)) return 0.0;
        double intersection = Math.max(0, Math.min(xMax, other.xMax) - Math.max(xMin, other.xMin))
                * Math.max(0, Math.min(yMax, other.yMax) - Math.max(yMin, other.yMin));
        double union = area() + other.area() - intersection;
        return union <= 0 ? 0.0 : intersection / union;
    }

    public double area() {
        return (xMax - xMin) * (yMax - yMin);
    }
}
