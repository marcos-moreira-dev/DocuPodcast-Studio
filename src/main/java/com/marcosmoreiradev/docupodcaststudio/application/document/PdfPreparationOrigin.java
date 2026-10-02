package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Explicit initiator of PDF semantic work; navigation itself is never an initiator. */
public enum PdfPreparationOrigin {
    LISTEN_DOCUMENT(false),
    LISTEN_FROM_HERE(false),
    PROCESS_FRAGMENT(true),
    PROCESS_FROM_HERE(true),
    PROCESS_INTERVAL(true),
    PROCESS_COMPLETE(true),
    EXPLICIT_RETRY(true),
    ACTIVE_JOB_PREFETCH(false),
    ACTIVE_JOB_REPRIORITIZATION(false),
    PROJECT_RESTORE(false),
    NAVIGATION(false);

    private final boolean retryAuthority;

    PdfPreparationOrigin(boolean retryAuthority) {
        this.retryAuthority = retryAuthority;
    }

    public boolean retryAuthority() {
        return retryAuthority;
    }
}
