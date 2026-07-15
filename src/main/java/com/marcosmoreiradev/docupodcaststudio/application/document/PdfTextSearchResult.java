package com.marcosmoreiradev.docupodcaststudio.application.document;

/** One PDF search hit with visual coordinates. */
public record PdfTextSearchResult(
        String id,
        int pageNumber,
        String blockId,
        String snippet,
        PdfPageRegion region,
        PdfTextLayerOrigin origin,
        double confidence
) {
    public PdfTextSearchResult {
        id = id == null || id.isBlank() ? "pdf-search-" + pageNumber : id.strip();
        blockId = blockId == null ? "" : blockId.strip();
        snippet = snippet == null ? "" : snippet.strip();
        origin = origin == null ? PdfTextLayerOrigin.UNAVAILABLE : origin;
        confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
    }

    public PdfVisualTextHighlight toHighlight() {
        return new PdfVisualTextHighlight(pageNumber, region, snippet, origin);
    }
}
