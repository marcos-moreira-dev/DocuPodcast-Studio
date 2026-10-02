package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Search request for PDF visual text layers. */
public record PdfTextSearchRequest(
        PreparedPdfWorkspaceRef workspace,
        String query,
        boolean includeOcr,
        int maxResults,
        int maxOcrPages
) {
    public PdfTextSearchRequest {
        query = query == null ? "" : query.strip();
        maxResults = maxResults <= 0 ? 80 : maxResults;
        maxOcrPages = maxOcrPages <= 0 ? 20 : maxOcrPages;
    }
}
