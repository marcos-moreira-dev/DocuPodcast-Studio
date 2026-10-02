package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;
import java.util.Objects;

/** Traceable source and policy for the final narration of one PDF object. */
public record PdfNarrationBinding(
        int pageNumber,
        List<String> sourceRegionIds,
        PdfSemanticTextLayer sourceLayer,
        PdfObjectNarrationPolicy policy,
        String interpretationId,
        long sourceRevision,
        String sourceFingerprint
) {
    public PdfNarrationBinding {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be 1-based");
        }
        sourceRegionIds = sourceRegionIds == null ? List.of()
                : sourceRegionIds.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (sourceRegionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "A narration binding requires source regions");
        }
        sourceLayer = Objects.requireNonNull(sourceLayer, "sourceLayer");
        policy = Objects.requireNonNull(policy, "policy");
        interpretationId = interpretationId == null ? "" : interpretationId.strip();
        sourceRevision = Math.max(1L, sourceRevision);
        sourceFingerprint = sourceFingerprint == null
                ? "" : sourceFingerprint.strip();
        if (sourceFingerprint.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceFingerprint must not be blank");
        }
        if (sourceLayer == PdfSemanticTextLayer.INTERPRETATION
                && interpretationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Interpretive narration requires an interpretation id");
        }
        if (sourceLayer != PdfSemanticTextLayer.INTERPRETATION
                && !interpretationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Only interpretive narration may reference an interpretation id");
        }
    }
}
