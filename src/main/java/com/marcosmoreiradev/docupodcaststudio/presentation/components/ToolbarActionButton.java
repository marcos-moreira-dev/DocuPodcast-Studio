package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Reusable toolbar action with icon + short text.
 *
 * <p>The main toolbar is a product surface, not a place for ad-hoc JavaFX buttons. This component
 * gives each frequent action a generous clickable area, a clear icon, a short label and a tooltip,
 * while the toolbar keeps only routing and enablement decisions.</p>
 */
public final class ToolbarActionButton extends Button {
    private final Label textLabel = new Label();

    public ToolbarActionButton(String icon, String text, Runnable action) {
        this(icon, new SimpleStringProperty(text), text, action, false);
    }

    public ToolbarActionButton(String icon, ObservableValue<String> text, String tooltip, Runnable action, boolean primary) {
        super();
        getStyleClass().add(AppStyles.UI_TOOLBAR_ACTION);
        if (primary) {
            getStyleClass().add(AppStyles.UI_TOOLBAR_ACTION_PRIMARY);
        }
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setMaxWidth(Region.USE_PREF_SIZE);
        setMinWidth(Region.USE_PREF_SIZE);
        setFocusTraversable(false);

        Label iconLabel = new Label(icon == null ? "" : icon);
        iconLabel.getStyleClass().add(AppStyles.UI_TOOLBAR_ACTION_ICON);
        textLabel.getStyleClass().add(AppStyles.UI_TOOLBAR_ACTION_TEXT);
        textLabel.textProperty().bind(text);
        textProperty().bind(text);

        HBox graphic = new HBox(7, iconLabel, textLabel);
        graphic.setAlignment(Pos.CENTER);
        setGraphic(graphic);

        Tooltip.install(this, new Tooltip(tooltip == null || tooltip.isBlank() ? "Acción" : tooltip));
        if (action != null) {
            setOnAction(event -> action.run());
        }
    }
}
