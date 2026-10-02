package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.Objects;

/**
 * One complete non-prose unit to be explained during document listening.
 * It may contain several OCR labels but produces exactly one narration cue.
 */
public record DocumentTechnicalElement(
        String id,
        Type type,
        int pageNumber,
        List<String> sourceRegionIds,
        PdfPageRegion roi,
        String caption,
        String nearbyContext,
        String anchorRegionId,
        int readingOrder
) {
    public enum Type {
        INLINE_FORMULA,
        BLOCK_FORMULA,
        /** Compatibility value for persisted callers predating formula layout roles. */
        FORMULA,
        FIGURE,
        GRAPH,
        TABLE,
        TECHNICAL_REGION
    }

    public DocumentTechnicalElement {
        id = Objects.requireNonNull(id, "id").strip();
        type = Objects.requireNonNullElse(type, Type.TECHNICAL_REGION);
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber");
        sourceRegionIds = sourceRegionIds == null ? List.of()
                : sourceRegionIds.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (sourceRegionIds.isEmpty()) {
            throw new IllegalArgumentException("sourceRegionIds");
        }
        roi = Objects.requireNonNull(roi, "roi");
        caption = caption == null ? "" : caption.strip();
        nearbyContext = nearbyContext == null ? "" : nearbyContext.strip();
        anchorRegionId = anchorRegionId == null || anchorRegionId.isBlank()
                ? sourceRegionIds.getFirst() : anchorRegionId.strip();
        readingOrder = Math.max(0, readingOrder);
    }
}
