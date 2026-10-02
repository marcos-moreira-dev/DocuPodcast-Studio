package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.Objects;

/** One reviewable cell in a derived table structure. */
public record PdfTableCell(
        String id,
        int row,
        int column,
        int rowSpan,
        int columnSpan,
        double xMin,
        double yMin,
        double xMax,
        double yMax,
        String text,
        double confidence,
        PdfTableGeometryEvidence geometryEvidence
) {
    public PdfTableCell {
        id = Objects.toString(id, "").strip();
        if (id.isBlank()) throw new IllegalArgumentException("cell id is required");
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("cell coordinates must be zero-based");
        }
        rowSpan = Math.max(1, rowSpan);
        columnSpan = Math.max(1, columnSpan);
        if (!Double.isFinite(xMin) || !Double.isFinite(yMin)
                || !Double.isFinite(xMax) || !Double.isFinite(yMax)
                || xMax < xMin || yMax < yMin) {
            throw new IllegalArgumentException("invalid cell geometry");
        }
        text = Objects.toString(text, "").strip();
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        geometryEvidence = Objects.requireNonNullElse(
                geometryEvidence, PdfTableGeometryEvidence.INFERRED);
    }

    /** Compatibility constructor for callers whose uniform grid was inferred. */
    public PdfTableCell(String id, int row, int column, int rowSpan,
                        int columnSpan, double xMin, double yMin,
                        double xMax, double yMax, String text,
                        double confidence) {
        this(id, row, column, rowSpan, columnSpan, xMin, yMin, xMax,
                yMax, text, confidence, PdfTableGeometryEvidence.INFERRED);
    }
}
