package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Compact action button for the tabbed ribbon surface. */
public final class RibbonButton extends Button {
    public RibbonButton(String marker, String text, String tooltip, Runnable action, boolean primary) {
        this(markerIcon(marker), new SimpleStringProperty(text == null ? "" : text),
                new SimpleStringProperty(tooltip == null ? "" : tooltip), action, primary);
    }

    public RibbonButton(String marker, ObservableValue<String> text, String tooltip, Runnable action, boolean primary) {
        this(markerIcon(marker), text, new SimpleStringProperty(tooltip == null ? "" : tooltip), action, primary);
    }

    public RibbonButton(AppIcon icon, String text, String tooltip, Runnable action, boolean primary) {
        this(IconView.ribbon(icon), new SimpleStringProperty(text == null ? "" : text),
                new SimpleStringProperty(tooltip == null ? "" : tooltip), action, primary);
    }

    public RibbonButton(AppIcon icon, ObservableValue<String> text, String tooltip, Runnable action, boolean primary) {
        this(IconView.ribbon(icon), text, new SimpleStringProperty(tooltip == null ? "" : tooltip), action, primary);
    }

    public RibbonButton(AppIcon icon, ObservableValue<String> text, ObservableValue<String> tooltip, Runnable action, boolean primary) {
        this(IconView.ribbon(icon), text, tooltip, action, primary);
    }

    private RibbonButton(Node iconNode, ObservableValue<String> text, ObservableValue<String> tooltip, Runnable action, boolean primary) {
        super();
        getStyleClass().add(AppStyles.UI_RIBBON_BUTTON);
        if (primary) {
            getStyleClass().add(AppStyles.UI_RIBBON_BUTTON_PRIMARY);
        }
        setFocusTraversable(false);
        setMinHeight(66);
        setPrefHeight(72);
        setMaxHeight(84);
        setMinWidth(216);
        setPrefWidth(232);
        setMaxWidth(328);

        Node markerNode = iconNode == null ? IconView.ribbon(AppIcon.DEFAULT) : iconNode;
        markerNode.getStyleClass().add(AppStyles.UI_RIBBON_BUTTON_ICON);
        Label textLabel = new Label(text == null || text.getValue() == null ? "" : text.getValue());
        textLabel.getStyleClass().add(AppStyles.UI_RIBBON_BUTTON_TEXT);
        textLabel.setWrapText(true);
        textLabel.setTextOverrun(OverrunStyle.CLIP);
        textLabel.setMinWidth(150);
        textLabel.setPrefWidth(176);
        textLabel.setMaxWidth(236);
        textLabel.setMinHeight(Region.USE_PREF_SIZE);
        if (text != null) {
            textLabel.textProperty().bind(Bindings.createStringBinding(
                    () -> text.getValue() == null ? "" : text.getValue(),
                    text));
        }

        HBox content = new HBox(8, markerNode, textLabel);
        content.getStyleClass().add(AppStyles.UI_RIBBON_BUTTON_CONTENT);
        content.setAlignment(Pos.CENTER);
        setGraphic(content);

        Tooltip tooltipNode = new Tooltip();
        tooltipNode.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    String value = tooltip == null || tooltip.getValue() == null ? "" : tooltip.getValue().strip();
                    return value.isBlank() ? textLabel.getText() : value;
                },
                tooltip == null ? textLabel.textProperty() : tooltip,
                textLabel.textProperty()));
        Tooltip.install(this, tooltipNode);
        if (action != null) {
            setOnAction(event -> action.run());
        }
    }

    private static Node markerIcon(String marker) {
        Label markerLabel = new Label(marker == null ? "" : marker);
        markerLabel.getStyleClass().add(AppStyles.UI_RIBBON_BUTTON_ICON);
        markerLabel.setMinWidth(34);
        markerLabel.setPrefWidth(38);
        markerLabel.setMaxWidth(44);
        return markerLabel;
    }
}
