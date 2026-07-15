package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Small metric card for readiness summaries without duplicating ad-hoc VBox/Label styling. */
public final class MetricBadge extends VBox {
    public MetricBadge(String title, String value, String caption, String stateClass) {
        super(2);
        getStyleClass().add(AppStyles.UI_METRIC_BADGE);
        if (stateClass != null && !stateClass.isBlank()) {
            getStyleClass().add(stateClass);
        }
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(AppStyles.UI_METRIC_TITLE);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add(AppStyles.UI_METRIC_VALUE);
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add(AppStyles.UI_METRIC_CAPTION);
        captionLabel.setWrapText(true);
        getChildren().addAll(titleLabel, valueLabel, captionLabel);
    }
}
