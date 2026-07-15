package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;

import java.util.Objects;

/** Presentation-facing result of refreshing a read-only source document. */
public record SourceDocumentRefreshOutcome(
        ReadableDocument refreshedDocument,
        SourceDocumentChangeReport report,
        boolean projectContentChanged
) {
    public SourceDocumentRefreshOutcome {
        report = Objects.requireNonNull(report, "report");
    }

    public boolean refreshedDocumentAvailable() {
        return refreshedDocument != null;
    }
}
