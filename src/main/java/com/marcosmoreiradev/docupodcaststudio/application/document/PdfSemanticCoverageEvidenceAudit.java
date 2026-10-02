package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

/** Auditable decision for one native/OCR line used as semantic-coverage evidence. */
public record PdfSemanticCoverageEvidenceAudit(
        String evidenceId,
        String text,
        PdfPageRegion bbox,
        double areaPoints,
        String source,
        String alreadyCoveredByRegionId,
        PdfRegionType semanticRegionType,
        double overlap,
        double textSimilarity,
        boolean mathematical,
        boolean headerFooter,
        Classification classification,
        boolean significantMissingContent
) {
    public PdfSemanticCoverageEvidenceAudit {
        evidenceId = evidenceId == null ? "" : evidenceId;
        text = text == null ? "" : text;
        source = source == null ? "" : source;
        alreadyCoveredByRegionId = alreadyCoveredByRegionId == null
                ? "" : alreadyCoveredByRegionId;
        areaPoints = Double.isFinite(areaPoints) ? Math.max(0.0, areaPoints) : 0.0;
        overlap = finiteUnit(overlap);
        textSimilarity = finiteUnit(textSimilarity);
        classification = classification == null
                ? Classification.AMBIGUOUS : classification;
    }

    private static double finiteUnit(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    public enum Classification {
        TRUE_MISSING_CONTENT,
        COVERED_DIFFERENT_REPRESENTATION,
        MATH_FRAGMENT,
        HEADER_FOOTER,
        OCR_NOISE,
        EDITORIAL_MARGIN,
        AMBIGUOUS
    }
}
