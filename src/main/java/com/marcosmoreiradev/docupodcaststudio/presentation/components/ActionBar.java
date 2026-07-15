package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/** Reusable horizontal action row for workspaces and cards. */
public final class ActionBar extends HBox {
    public ActionBar(Node... actions) {
        super(8);
        getStyleClass().add(AppStyles.UI_ACTION_BAR);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(actions);
    }
}
