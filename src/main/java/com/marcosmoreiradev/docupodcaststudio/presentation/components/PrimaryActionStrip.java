package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Compact reusable operation strip for the product's main actions.
 *
 * <p>Workspaces should use this instead of hand-building independent JavaFX button+hint rows.
 * It keeps the visible product surface consistent while the underlying workflow can remain rich.</p>
 */
public final class PrimaryActionStrip extends HBox {
    public PrimaryActionStrip(String actionLabel, String hint, Runnable action) {
        this(new SimpleStringProperty(actionLabel), new SimpleStringProperty(hint), action);
    }

    public PrimaryActionStrip(ObservableValue<String> actionLabel, ObservableValue<String> hint, Runnable action) {
        this(actionLabel, hint, action, true);
    }

    public PrimaryActionStrip(ObservableValue<String> actionLabel, ObservableValue<String> hint, Runnable action, boolean showHintText) {
        super(10);
        getStyleClass().add(AppStyles.UI_PRIMARY_ACTION_STRIP);
        setAlignment(Pos.CENTER_LEFT);

        Button button = ActionButtonFactory.primary("");
        button.textProperty().bind(actionLabel);
        button.getStyleClass().add(AppStyles.UI_PRIMARY_ACTION_BUTTON);
        button.setOnAction(event -> action.run());
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(hint);
        Tooltip.install(button, tooltip);

        if (showHintText) {
            Label hintLabel = new Label();
            hintLabel.textProperty().bind(hint);
            hintLabel.setWrapText(true);
            hintLabel.getStyleClass().add(AppStyles.UI_ACTION_HINT);
            HBox.setHgrow(hintLabel, Priority.ALWAYS);
            getChildren().addAll(button, hintLabel);
        } else {
            getChildren().add(button);
        }
    }
}
