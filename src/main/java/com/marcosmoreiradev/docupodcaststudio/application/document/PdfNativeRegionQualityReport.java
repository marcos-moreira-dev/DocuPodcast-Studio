package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Native-text quality for one geometrically independent line/region candidate. */
public record PdfNativeRegionQualityReport(
        int lineIndex,
        PdfNativeTextQuality quality,
        double score,
        List<String> reasons
) {
    public PdfNativeRegionQualityReport {
        if (lineIndex < 0) throw new IllegalArgumentException("lineIndex must be non-negative");
        quality = quality == null ? PdfNativeTextQuality.UNUSABLE : quality;
        score = Double.isFinite(score) ? Math.max(0.0, Math.min(1.0, score)) : 0.0;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
