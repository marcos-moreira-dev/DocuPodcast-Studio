package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Highlight target for a PDF text line/block over the visual PDF page. */
public record PdfVisualTextHighlight(
        int pageNumber,
        PdfPageRegion region,
        String text,
        PdfTextLayerOrigin origin
) {
    public PdfVisualTextHighlight {
        if (pageNumber <= 0) {
            pageNumber = region == null ? 0 : region.pageNumber();
        }
        text = text == null ? "" : text.strip();
        origin = origin == null ? PdfTextLayerOrigin.UNAVAILABLE : origin;
    }

    public boolean available() {
        return pageNumber > 0 && region != null && origin != PdfTextLayerOrigin.UNAVAILABLE;
    }
}
