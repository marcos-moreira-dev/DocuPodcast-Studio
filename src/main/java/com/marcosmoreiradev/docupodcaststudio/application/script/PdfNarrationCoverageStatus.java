package com.marcosmoreiradev.docupodcaststudio.application.script;

/** Auditable terminal state for every canonical PDF region. */
public enum PdfNarrationCoverageStatus {
    NARRATED,
    COVERED_BY_EQUIVALENT,
    INTENTIONALLY_SKIPPED,
    INCOMPLETE
}
