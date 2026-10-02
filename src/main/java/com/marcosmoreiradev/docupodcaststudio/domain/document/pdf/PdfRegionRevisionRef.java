package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Stable source reference used by downstream audio and navigation. */
public record PdfRegionRevisionRef(String regionId, int pageNumber, long revision) {
    public PdfRegionRevisionRef {
        regionId = regionId == null ? "" : regionId.strip();
        if (regionId.isBlank()) throw new IllegalArgumentException("regionId is required");
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        revision = Math.max(1L, revision);
    }
}
