package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/** Request to ensure a PDF has persistent narratable text blocks before reading/search/playback. */
public record PdfNarratableDocumentRequest(
        ReadableDocument document,
        Path cacheDirectory,
        boolean forceOcr,
        int maxPages,
        List<Integer> targetPages
) {
    public static final int DEFAULT_MAX_PAGES = 200;

    public PdfNarratableDocumentRequest(ReadableDocument document, Path cacheDirectory, boolean forceOcr, int maxPages) {
        this(document, cacheDirectory, forceOcr, maxPages, List.of());
    }

    public PdfNarratableDocumentRequest {
        maxPages = maxPages <= 0 ? DEFAULT_MAX_PAGES : maxPages;
        targetPages = normalizePages(targetPages);
    }

    private static List<Integer> normalizePages(List<Integer> pages) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        for (Integer page : pages) {
            if (page != null && page > 0) {
                normalized.add(page);
            }
        }
        return List.copyOf(normalized);
    }
}
