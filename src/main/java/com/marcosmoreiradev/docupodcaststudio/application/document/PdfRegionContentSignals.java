package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

/** Shared conservative signals used by layout analysis and narration. */
public final class PdfRegionContentSignals {
    private static final double EDGE_BAND = 0.12;

    private PdfRegionContentSignals() {
    }

    /**
     * Identifies isolated labels inside a diagram or formula. They remain
     * visible and searchable, but must be reviewed before entering narration.
     */
    public static boolean looksLikeSparseTechnicalLabel(
            PreparedPdfPage page, PdfRegion region) {
        if (page == null || region == null) return false;
        String text = region.effectiveText().strip();
        if (text.isBlank() || text.length() > 28
                || text.matches("(?s).*[.!?,;:]\\s*$")) {
            return false;
        }
        boolean insideBody = region.yMin()
                > page.heightPoints() * EDGE_BAND
                && region.yMax()
                < page.heightPoints() * (1.0 - EDGE_BAND);
        return insideBody
                && region.xMax() - region.xMin()
                < page.widthPoints() * 0.22;
    }
}
