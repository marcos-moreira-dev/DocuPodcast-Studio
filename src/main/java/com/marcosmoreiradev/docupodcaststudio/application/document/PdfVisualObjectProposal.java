package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Geometry proposal from deterministic evidence; never a generative authority. */
public record PdfVisualObjectProposal(
        String id,
        Type type,
        PdfPageRegion geometry,
        List<String> sourceRegionIds,
        List<String> internalLabelRegionIds,
        String captionRegionId,
        double confidence,
        String detector
) {
    public enum Type { FIGURE, DIAGRAM, GRAPH, TABLE, FORMULA, DECORATION, UNKNOWN }

    public PdfVisualObjectProposal {
        if (id == null || id.isBlank() || type == null || geometry == null) {
            throw new IllegalArgumentException("Visual object identity is required");
        }
        id = id.strip();
        sourceRegionIds = sourceRegionIds == null ? List.of()
                : sourceRegionIds.stream().filter(value -> value != null && !value.isBlank())
                .map(String::strip).distinct().toList();
        internalLabelRegionIds = internalLabelRegionIds == null ? List.of()
                : internalLabelRegionIds.stream().filter(value -> value != null && !value.isBlank())
                .map(String::strip).distinct().toList();
        captionRegionId = captionRegionId == null ? "" : captionRegionId.strip();
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        detector = detector == null ? "" : detector.strip();
    }
}
