package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;

/** Official progress controls with shared visual states. */
public final class StudioFeedbackControls {
    private StudioFeedbackControls() { }

    public static ProgressBar progressBar() { return progressBar(ProgressBar.INDETERMINATE_PROGRESS); }
    public static ProgressBar progressBar(double progress) {
        ProgressBar control = new ProgressBar(progress);
        control.getStyleClass().add("ui-progress-bar");
        return StudioControlContract.mark(control, StudioControlFamily.FEEDBACK,
                StudioControlVariant.LONG_RUNNING, StudioControlDensity.REGULAR);
    }

    public static ProgressIndicator progressIndicator() {
        return progressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    public static ProgressIndicator progressIndicator(double progress) {
        ProgressIndicator control = new ProgressIndicator(progress);
        control.getStyleClass().add("ui-progress-indicator");
        return StudioControlContract.mark(control, StudioControlFamily.FEEDBACK,
                StudioControlVariant.INLINE, StudioControlDensity.COMPACT);
    }
}
