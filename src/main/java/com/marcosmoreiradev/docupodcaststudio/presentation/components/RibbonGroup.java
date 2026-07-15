package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Group of related commands inside the desktop ribbon. */
public final class RibbonGroup extends VBox {
    private final HBox actions = new HBox(9);

    public RibbonGroup(String title, Node... actionNodes) {
        super(5);
        getStyleClass().add(AppStyles.UI_RIBBON_GROUP);
        setAlignment(Pos.CENTER);
        setMinHeight(104);
        setPrefHeight(110);
        setMaxHeight(118);

        actions.getStyleClass().add(AppStyles.UI_RIBBON_GROUP_ACTIONS);
        actions.setAlignment(Pos.CENTER);
        actions.getChildren().addAll(actionNodes);

        Label titleLabel = new Label(title == null ? "Herramientas" : title);
        titleLabel.getStyleClass().add(AppStyles.UI_RIBBON_GROUP_TITLE);

        getChildren().addAll(actions, titleLabel);
    }

    public HBox actions() {
        return actions;
    }
}
