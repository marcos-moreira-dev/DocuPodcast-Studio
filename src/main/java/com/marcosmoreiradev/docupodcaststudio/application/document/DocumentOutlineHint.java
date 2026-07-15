package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/** Optional external hint, for example a PDF bookmark mapped to a page. */
public record DocumentOutlineHint(
        String title,
        int level,
        String sourcePage,
        DocumentOutlineOrigin origin
) {
    public DocumentOutlineHint {
        title = title == null ? "" : title.strip();
        if (title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        level = Math.min(3, Math.max(1, level));
        sourcePage = sourcePage == null ? "" : sourcePage.strip();
        origin = Objects.requireNonNullElse(origin, DocumentOutlineOrigin.PDF_BOOKMARKS);
    }
}
