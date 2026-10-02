package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Observable scheduler snapshot, independent from JavaFX. */
public record PdfPreparationProgress(
        State state,
        int currentPage,
        int completed,
        int requested,
        int queued,
        String message
) {
    public enum State { IDLE, RUNNING, PAUSED, CANCELLED, FAILED }

    public PdfPreparationProgress {
        state = state == null ? State.IDLE : state;
        currentPage = Math.max(0, currentPage);
        completed = Math.max(0, completed);
        requested = Math.max(completed, requested);
        queued = Math.max(0, queued);
        message = message == null ? "" : message.strip();
    }

    public double fraction() {
        return requested == 0 ? 0.0 : Math.min(1.0, completed / (double) requested);
    }
}
