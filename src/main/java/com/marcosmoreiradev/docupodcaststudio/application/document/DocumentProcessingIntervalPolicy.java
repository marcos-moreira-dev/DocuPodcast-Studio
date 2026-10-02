package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;

/** Predictable UI policy for initializing and editing inclusive PDF intervals. */
public final class DocumentProcessingIntervalPolicy {
    public DocumentProcessingInterval initial(int currentPage, int pageCount) {
        if (pageCount < 1) {
            throw new IllegalArgumentException("PDF page count must be positive");
        }
        int start = Math.max(1, Math.min(pageCount, currentPage));
        return DocumentProcessingInterval.pages(start,
                Math.min(pageCount, start + 1), pageCount);
    }

    public DocumentProcessingInterval afterStartChange(
            int newStart, int currentEnd, int pageCount) {
        if (pageCount < 1) {
            throw new IllegalArgumentException("PDF page count must be positive");
        }
        int start = Math.max(1, Math.min(pageCount, newStart));
        int end = currentEnd >= start && currentEnd <= pageCount
                ? currentEnd : Math.min(pageCount, start + 1);
        return DocumentProcessingInterval.pages(start, end, pageCount);
    }
}
