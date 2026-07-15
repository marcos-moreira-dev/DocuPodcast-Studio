package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;

/** Deterministic visual anchor for hover, fixed selection and playback over a PDF page. */
public record PdfVisualTextTarget(
        String id,
        String blockId,
        DocumentTextRange range,
        int pageNumber,
        PdfPageRegion region,
        String text,
        PdfTextLayerOrigin origin,
        PdfVisualTextTargetKind kind
) {
    public PdfVisualTextTarget {
        blockId = normalizeToken(blockId);
        id = normalizeToken(id).isBlank() ? blockId : normalizeToken(id);
        text = text == null ? "" : text.strip();
        origin = origin == null ? PdfTextLayerOrigin.UNAVAILABLE : origin;
        kind = kind == null ? PdfVisualTextTargetKind.BLOCK : kind;
        if (pageNumber <= 0) {
            pageNumber = region == null ? 0 : region.pageNumber();
        }
    }

    public boolean available() {
        return pageNumber > 0 && region != null && origin != PdfTextLayerOrigin.UNAVAILABLE && !blockId.isBlank();
    }

    public boolean contains(double xPoints, double yPoints, double tolerancePoints) {
        if (!available()) {
            return false;
        }
        double tolerance = Math.max(0.0, tolerancePoints);
        return xPoints >= region.xMinPoints() - tolerance
                && xPoints <= region.xMaxPoints() + tolerance
                && yPoints >= region.yMinPoints() - tolerance
                && yPoints <= region.yMaxPoints() + tolerance;
    }

    public double areaPoints() {
        if (region == null) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.001, region.xMaxPoints() - region.xMinPoints())
                * Math.max(0.001, region.yMaxPoints() - region.yMinPoints());
    }

    public PdfVisualTextHighlight highlight() {
        return new PdfVisualTextHighlight(pageNumber, region, text, origin);
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.strip();
    }
}
