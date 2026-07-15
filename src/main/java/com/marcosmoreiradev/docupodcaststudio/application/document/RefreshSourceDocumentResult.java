package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;

import java.util.Objects;

/** Result of attempting to refresh the external read-only source document. */
public record RefreshSourceDocumentResult(
        ReadableDocument refreshedDocument,
        SourceDocumentChangeReport report
) {
    public RefreshSourceDocumentResult {
        report = Objects.requireNonNull(report, "report");
    }

    public boolean refreshedDocumentAvailable() {
        return refreshedDocument != null;
    }
}
