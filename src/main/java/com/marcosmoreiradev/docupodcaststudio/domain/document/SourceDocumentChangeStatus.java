package com.marcosmoreiradev.docupodcaststudio.domain.document;

/** Result of comparing the imported snapshot with the refreshed external source. */
public enum SourceDocumentChangeStatus {
    UNCHANGED,
    CHANGED,
    MISSING,
    UNSUPPORTED
}
