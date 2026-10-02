package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Deterministic visual anchor for hover, fixed selection and playback over a PDF page. */
public record PdfVisualTextTarget(
        String id,
        String regionId,
        int startOffset,
        int endOffset,
        int pageNumber,
        PdfPageRegion region,
        List<PdfPageRegion> highlightRegions,
        List<String> sourceRegionIds,
        String text,
        PdfTextLayerOrigin origin,
        PdfVisualTextTargetKind kind
) {
    public PdfVisualTextTarget {
        regionId = normalizeToken(regionId);
        id = normalizeToken(id).isBlank() ? regionId : normalizeToken(id);
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
        text = text == null ? "" : text.strip();
        origin = origin == null ? PdfTextLayerOrigin.UNAVAILABLE : origin;
        kind = kind == null ? PdfVisualTextTargetKind.BLOCK : kind;
        highlightRegions = highlightRegions == null || highlightRegions.isEmpty()
                ? (region == null ? List.of() : List.of(region))
                : highlightRegions.stream().filter(java.util.Objects::nonNull).toList();
        sourceRegionIds = sourceRegionIds == null || sourceRegionIds.isEmpty()
                ? (regionId.isBlank() ? List.of() : List.of(regionId))
                : sourceRegionIds.stream().filter(java.util.Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (pageNumber <= 0) {
            pageNumber = region == null ? 0 : region.pageNumber();
        }
    }

    public PdfVisualTextTarget(
            String id, String regionId, int startOffset, int endOffset,
            int pageNumber, PdfPageRegion region, String text,
            PdfTextLayerOrigin origin, PdfVisualTextTargetKind kind) {
        this(id, regionId, startOffset, endOffset, pageNumber, region,
                region == null ? List.of() : List.of(region),
                regionId == null ? List.of() : List.of(regionId),
                text, origin, kind);
    }

    public PdfVisualTextTarget(
            String id, String regionId, int startOffset, int endOffset,
            int pageNumber, PdfPageRegion region,
            List<PdfPageRegion> highlightRegions, String text,
            PdfTextLayerOrigin origin, PdfVisualTextTargetKind kind) {
        this(id, regionId, startOffset, endOffset, pageNumber, region,
                highlightRegions,
                regionId == null ? List.of() : List.of(regionId),
                text, origin, kind);
    }

    public boolean available() {
        return pageNumber > 0 && region != null && origin != PdfTextLayerOrigin.UNAVAILABLE && !regionId.isBlank();
    }

    public boolean contains(double xPoints, double yPoints, double tolerancePoints) {
        if (!available()) {
            return false;
        }
        double tolerance = Math.max(0.0, tolerancePoints);
        return highlightRegions.stream().anyMatch(box ->
                xPoints >= box.xMinPoints() - tolerance
                        && xPoints <= box.xMaxPoints() + tolerance
                        && yPoints >= box.yMinPoints() - tolerance
                        && yPoints <= box.yMaxPoints() + tolerance);
    }

    public double areaPoints() {
        if (region == null) {
            return Double.MAX_VALUE;
        }
        return highlightRegions.stream().mapToDouble(box ->
                        Math.max(0.001, box.xMaxPoints() - box.xMinPoints())
                                * Math.max(0.001,
                                box.yMaxPoints() - box.yMinPoints()))
                .sum();
    }

    public PdfVisualTextHighlight highlight() {
        return new PdfVisualTextHighlight(
                pageNumber, highlightRegions, text, origin);
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.strip();
    }
}
