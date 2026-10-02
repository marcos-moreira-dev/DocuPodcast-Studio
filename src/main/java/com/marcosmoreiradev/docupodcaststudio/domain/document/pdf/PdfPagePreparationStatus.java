package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Persisted terminal state. Missing page JSON means UNPREPARED. */
public enum PdfPagePreparationStatus {
    READY,
    READY_WITH_WARNINGS,
    FAILED
}
