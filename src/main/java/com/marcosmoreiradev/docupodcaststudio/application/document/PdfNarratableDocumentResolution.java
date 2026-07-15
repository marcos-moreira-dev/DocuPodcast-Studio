package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.util.List;

/** Result of resolving OCR/native PDF text into persistent document blocks. */
public record PdfNarratableDocumentResolution(
        ReadableDocument document,
        boolean changed,
        List<Integer> pagesProcessed,
        List<DocumentImportIssue> issues
) {
    public PdfNarratableDocumentResolution {
        pagesProcessed = pagesProcessed == null ? List.of() : List.copyOf(pagesProcessed);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
