package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Terminal state of one persisted PDF processing attempt. */
public enum PdfOperationAttemptState {
    IN_FLIGHT,
    INTERRUPTED,
    COMPLETED,
    FAILED,
    CANCELLED,
    TRUNCATED,
    INSUFFICIENT_EVIDENCE
}
