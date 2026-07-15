package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable card for operational/diagnostic panels. */
public final class DiagnosticCard extends VBox {
    public DiagnosticCard(String title, Node... children) {
        super(8);
        getStyleClass().add(AppStyles.UI_DIAGNOSTIC_CARD);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(AppStyles.UI_DIAGNOSTIC_TITLE);
        getChildren().add(titleLabel);
        getChildren().addAll(children);
    }
}
