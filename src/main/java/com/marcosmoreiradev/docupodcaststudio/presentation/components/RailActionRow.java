package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.layout.HBox;

/** Compact reusable row for SideDock/rail actions. */
public final class RailActionRow extends HBox {
    public RailActionRow(Node... actions) {
        super(6);
        getStyleClass().add(AppStyles.UI_RAIL_ACTION_ROW);
        getChildren().addAll(actions);
    }
}
