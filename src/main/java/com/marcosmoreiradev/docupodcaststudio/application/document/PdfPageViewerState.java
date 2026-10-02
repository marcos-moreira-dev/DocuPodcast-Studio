package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Passive, persisted state shown by the PDF viewer. */
public enum PdfPageViewerState {
    PREPARED,
    TECHNICAL_FAILURE,
    SEMANTIC_REJECTION,
    CANCELLED,
    NOT_PREPARED
}
