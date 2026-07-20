package com.marcosmoreiradev.docupodcaststudio.presentation.components;

/** Standard reusable empty state for friendly, non-technical workspace surfaces. */
public final class EmptyStateView extends UiStateView {
    public EmptyStateView(String eyebrow, String title, String summary) {
        super(UiState.EMPTY, eyebrow, title, summary);
    }
}
