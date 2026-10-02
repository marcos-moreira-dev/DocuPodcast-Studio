package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Progress for a semantic multi-page preparation request. */
public record PdfPreparationScopeProgress(
        int completedPages,
        int totalPages,
        int currentPage,
        long estimatedRemainingSeconds,
        String message,
        int acceptedPages,
        int rejectedPages,
        int failedPages,
        int cancelledPages
) {
    public PdfPreparationScopeProgress {
        completedPages = Math.max(0, completedPages);
        totalPages = Math.max(completedPages, totalPages);
        currentPage = Math.max(0, currentPage);
        estimatedRemainingSeconds = Math.max(-1L, estimatedRemainingSeconds);
        message = message == null ? "" : message.strip();
        acceptedPages = Math.max(0, acceptedPages);
        rejectedPages = Math.max(0, rejectedPages);
        failedPages = Math.max(0, failedPages);
        cancelledPages = Math.max(0, cancelledPages);
        if (acceptedPages + rejectedPages + failedPages + cancelledPages
                > completedPages) {
            throw new IllegalArgumentException(
                    "terminal outcome counters exceed completedPages");
        }
    }

    public PdfPreparationScopeProgress(int completedPages, int totalPages,
                                       int currentPage,
                                       long estimatedRemainingSeconds,
                                       String message) {
        this(completedPages, totalPages, currentPage, estimatedRemainingSeconds,
                message, 0, 0, 0, 0);
    }

    public PdfPreparationScopeProgress(int completedPages, int totalPages,
                                       int currentPage,
                                       long estimatedRemainingSeconds) {
        this(completedPages, totalPages, currentPage,
                estimatedRemainingSeconds, "", 0, 0, 0, 0);
    }
}
