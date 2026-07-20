package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Objects;

/** Shared visual representation for empty, loading and recoverable operational states. */
public class UiStateView extends VBox {
    private final UiState state;

    public UiStateView(UiState state, String eyebrow, String title, String summary) {
        super(8);
        this.state = Objects.requireNonNull(state, "state");
        getStyleClass().addAll(AppStyles.UI_EMPTY_STATE,
                "ui-state-" + state.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        setAccessibleText(title + ". " + summary);
        if (eyebrow != null && !eyebrow.isBlank()) {
            Label eyebrowLabel = new Label(eyebrow);
            eyebrowLabel.getStyleClass().add(AppStyles.UI_EYEBROW);
            getChildren().add(eyebrowLabel);
        }
        Label titleLabel = new Label(title == null ? "" : title);
        titleLabel.getStyleClass().add(AppStyles.UI_TITLE);
        Label summaryLabel = new Label(summary == null ? "" : summary);
        summaryLabel.setWrapText(true);
        summaryLabel.getStyleClass().add(AppStyles.UI_SUMMARY);
        getChildren().addAll(titleLabel, summaryLabel);
    }

    public UiState state() { return state; }
}
