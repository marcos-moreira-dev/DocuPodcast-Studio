package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Search results and diagnostics for visual PDF text. */
public record PdfTextSearchProjection(
        String query,
        List<PdfTextSearchResult> results,
        int scannedPages,
        List<String> warnings
) {
    public PdfTextSearchProjection {
        query = query == null ? "" : query.strip();
        results = results == null ? List.of() : List.copyOf(results);
        scannedPages = Math.max(0, scannedPages);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
