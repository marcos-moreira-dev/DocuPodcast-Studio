package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.nio.file.Path;

/** Search request for PDF visual text layers. */
public record PdfTextSearchRequest(
        ReadableDocument document,
        String query,
        boolean includeOcr,
        int maxResults,
        int maxOcrPages,
        Path cacheDirectory
) {
    public PdfTextSearchRequest {
        query = query == null ? "" : query.strip();
        maxResults = maxResults <= 0 ? 80 : maxResults;
        maxOcrPages = maxOcrPages <= 0 ? 20 : maxOcrPages;
    }
}
