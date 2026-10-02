package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

/** Repeated native-text evidence that identifies editorial page furniture. */
public record PdfRepeatedMarginEvidence(
        String text,
        PdfPageRegion region,
        PdfRegionType type,
        int matchingPages,
        double geometrySimilarity,
        double relativeY
) {
    public PdfRepeatedMarginEvidence {
        text = text == null ? "" : text.strip();
        type = type == null ? PdfRegionType.UNKNOWN : type;
        matchingPages = Math.max(0, matchingPages);
        geometrySimilarity = finiteUnit(geometrySimilarity);
        relativeY = finiteUnit(relativeY);
    }

    private static double finiteUnit(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }
}
