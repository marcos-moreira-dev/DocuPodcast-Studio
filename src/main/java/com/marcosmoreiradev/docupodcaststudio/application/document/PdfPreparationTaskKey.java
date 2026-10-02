package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Stable deduplication identity for one source revision and page. */
public record PdfPreparationTaskKey(String sourceSha256, int pageNumber) {
    public PdfPreparationTaskKey {
        sourceSha256 = sourceSha256 == null ? "" : sourceSha256.strip().toLowerCase(java.util.Locale.ROOT);
        if (sourceSha256.isBlank()) throw new IllegalArgumentException("sourceSha256 is required");
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
    }
}
