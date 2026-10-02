package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact visual and revision anchor for one admitted PDF content owner. */
public record PdfContentAnchor(
        int pageNumber,
        double pageWidthPoints,
        double pageHeightPoints,
        DocumentContentRectangle roi,
        List<String> sourceRegionIds,
        Map<String, DocumentContentRectangle> sourceRegionBounds,
        Map<String, String> sourceRegionTexts,
        List<String> unreliableSourceRegionIds,
        long revision,
        String visualFingerprint
) implements DocumentContentAnchor {
    public PdfContentAnchor {
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        if (!(pageWidthPoints > 0.0) || !(pageHeightPoints > 0.0)
                || !Double.isFinite(pageWidthPoints) || !Double.isFinite(pageHeightPoints)) {
            throw new IllegalArgumentException("PDF page dimensions must be positive");
        }
        if (roi == null) throw new IllegalArgumentException("roi is required");
        sourceRegionIds = sourceRegionIds == null ? List.of() : sourceRegionIds.stream()
                .filter(java.util.Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
        if (sourceRegionIds.isEmpty()) {
            throw new IllegalArgumentException("sourceRegionIds are required");
        }
        LinkedHashMap<String, DocumentContentRectangle> normalizedBounds = new LinkedHashMap<>();
        List<String> allowedSourceRegionIds = sourceRegionIds;
        if (sourceRegionBounds != null) {
            sourceRegionBounds.forEach((id, bounds) -> {
                String normalized = id == null ? "" : id.strip();
                if (!normalized.isBlank() && bounds != null && allowedSourceRegionIds.contains(normalized)) {
                    normalizedBounds.put(normalized, bounds);
                }
            });
        }
        sourceRegionBounds = Map.copyOf(normalizedBounds);
        LinkedHashMap<String, String> normalizedTexts = new LinkedHashMap<>();
        if (sourceRegionTexts != null) {
            sourceRegionTexts.forEach((id, text) -> {
                String normalized = id == null ? "" : id.strip();
                if (!normalized.isBlank() && allowedSourceRegionIds.contains(normalized)) {
                    normalizedTexts.put(normalized, text == null ? "" : text.strip());
                }
            });
        }
        sourceRegionTexts = Map.copyOf(normalizedTexts);
        unreliableSourceRegionIds = unreliableSourceRegionIds == null ? List.of()
                : unreliableSourceRegionIds.stream().filter(java.util.Objects::nonNull)
                .map(String::strip).filter(allowedSourceRegionIds::contains).distinct().toList();
        revision = Math.max(1L, revision);
        visualFingerprint = visualFingerprint == null ? "" : visualFingerprint.strip();
        if (visualFingerprint.isBlank()) {
            throw new IllegalArgumentException("visualFingerprint is required");
        }
    }

    public PdfContentAnchor(int pageNumber, double pageWidthPoints,
                            double pageHeightPoints, DocumentContentRectangle roi,
                            List<String> sourceRegionIds, long revision,
                            String visualFingerprint) {
        this(pageNumber, pageWidthPoints, pageHeightPoints, roi, sourceRegionIds,
                Map.of(), Map.of(), List.of(), revision, visualFingerprint);
    }

    public PdfContentAnchor(int pageNumber, double pageWidthPoints,
                            double pageHeightPoints, DocumentContentRectangle roi,
                            List<String> sourceRegionIds,
                            Map<String, DocumentContentRectangle> sourceRegionBounds,
                            long revision, String visualFingerprint) {
        this(pageNumber, pageWidthPoints, pageHeightPoints, roi, sourceRegionIds,
                sourceRegionBounds, Map.of(), List.of(), revision, visualFingerprint);
    }
}
