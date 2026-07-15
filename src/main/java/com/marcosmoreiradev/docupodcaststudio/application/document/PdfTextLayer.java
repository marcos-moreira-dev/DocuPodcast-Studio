package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Future contract for PDF text used independently from faithful page rendering. */
public record PdfTextLayer(
        int pageNumber,
        PdfTextLayerOrigin origin,
        List<PdfTextLine> lines,
        List<String> warnings
) {
    public PdfTextLayer {
        pageNumber = Math.max(0, pageNumber);
        origin = origin == null ? PdfTextLayerOrigin.UNAVAILABLE : origin;
        lines = lines == null ? List.of() : List.copyOf(lines);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean available() {
        return origin != PdfTextLayerOrigin.UNAVAILABLE && !lines.isEmpty();
    }
}
