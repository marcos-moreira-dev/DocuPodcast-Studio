package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

public record PdfNativeTextQualityReport(
        PdfNativeTextQuality quality,
        double score,
        List<String> reasons,
        List<PdfNativeRegionQualityReport> regions
) {
    public PdfNativeTextQualityReport {
        quality = quality == null ? PdfNativeTextQuality.UNUSABLE : quality;
        score = Double.isFinite(score) ? Math.max(0.0, Math.min(1.0, score)) : 0.0;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        regions = regions == null ? List.of() : List.copyOf(regions);
    }

    public PdfNativeTextQualityReport(PdfNativeTextQuality quality, double score, List<String> reasons) {
        this(quality, score, reasons, List.of());
    }

    public double reliableRegionRatio() {
        if (regions.isEmpty()) return quality == PdfNativeTextQuality.RELIABLE ? 1.0 : 0.0;
        long reliable = regions.stream()
                .filter(region -> region.quality() == PdfNativeTextQuality.RELIABLE).count();
        return reliable / (double) regions.size();
    }
}
