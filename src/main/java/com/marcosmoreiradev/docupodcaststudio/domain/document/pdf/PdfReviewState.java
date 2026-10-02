package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Human-review projection kept separate from automatic evidence. */
public record PdfReviewState(boolean humanOverride, String state) {
    public PdfReviewState {
        state = state == null || state.isBlank() ? "UNREVIEWED" : state.strip();
    }
}
