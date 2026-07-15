package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable title/summary header for settings pages, panels and workspaces. */
public final class SectionHeader extends VBox {
    public SectionHeader(String title, String summary) {
        super(4);
        getStyleClass().add(AppStyles.UI_SECTION_HEADER);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(AppStyles.UI_SECTION_TITLE);
        Label summaryLabel = new Label(summary);
        summaryLabel.setWrapText(true);
        summaryLabel.getStyleClass().add(AppStyles.UI_SECTION_SUMMARY);
        getChildren().addAll(titleLabel, summaryLabel);
    }
}
