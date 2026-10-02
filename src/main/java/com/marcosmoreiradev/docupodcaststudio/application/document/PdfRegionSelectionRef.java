package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Stable PDF V2 selection pointing directly at a canonical prepared region. */
public record PdfRegionSelectionRef(
        int pageNumber,
        String regionId,
        int startOffset,
        int endOffset
) implements DocumentSelectionRef {
    public PdfRegionSelectionRef {
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be positive");
        regionId = regionId == null ? "" : regionId.strip();
        if (regionId.isBlank()) throw new IllegalArgumentException("regionId is required");
        startOffset = Math.max(0, startOffset);
        endOffset = Math.max(startOffset, endOffset);
    }

    public boolean wholeRegion() {
        return startOffset == 0;
    }

    public static PdfRegionSelectionRef from(PdfVisualTextTarget target) {
        if (target == null || !target.available()) {
            throw new IllegalArgumentException("A valid PDF text target is required");
        }
        return new PdfRegionSelectionRef(target.pageNumber(), target.regionId(),
                target.startOffset(), target.endOffset());
    }
}
