package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Standard reusable empty state for friendly, non-technical workspace surfaces. */
public final class EmptyStateView extends VBox {
    public EmptyStateView(String eyebrow, String title, String summary) {
        super(8);
        getStyleClass().add(AppStyles.UI_EMPTY_STATE);
        if (eyebrow != null && !eyebrow.isBlank()) {
            Label eyebrowLabel = new Label(eyebrow);
            eyebrowLabel.getStyleClass().add(AppStyles.UI_EYEBROW);
            getChildren().add(eyebrowLabel);
        }
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(AppStyles.UI_TITLE);
        Label summaryLabel = new Label(summary);
        summaryLabel.setWrapText(true);
        summaryLabel.getStyleClass().add(AppStyles.UI_SUMMARY);
        getChildren().addAll(titleLabel, summaryLabel);
    }
}
