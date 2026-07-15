package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Compact and foldable rail for media/storyboard assignments. It keeps the document
 * surface clean while still exposing production layers when needed.
 */
public final class CollapsibleMediaRail extends BorderPane {
    private static final double DEFAULT_EXPANDED_WIDTH = 286.0;
    private static final double COLLAPSED_WIDTH = 48.0;

    private final BooleanProperty expanded = new SimpleBooleanProperty(true);
    private final Node content;
    private final VBox copy;
    private final Region resizeHandle = new Region();
    private final SidePanelToggleButton toggle = new SidePanelToggleButton(
            AppIcon.COLLAPSE,
            "Ocultar o mostrar el panel visual",
            null);

    public CollapsibleMediaRail(String title, String summary, Node content) {
        this.content = content;
        getStyleClass().add("ui-collapsible-rail");
        setPadding(new Insets(0));
        setRailWidth(DEFAULT_EXPANDED_WIDTH);

        DocumentSidePanelChrome.Header chrome = DocumentSidePanelChrome.header(title == null ? "Capas" : title, summary, toggle);
        copy = chrome.copy();

        configureResizeHandle();
        toggle.getStyleClass().addAll("ui-rail-toggle", AppStyles.UI_RAIL_TOGGLE_BUTTON);
        toggle.setOnAction(event -> setExpanded(!expanded.get()));
        HBox header = chrome.node();
        header.getStyleClass().add("ui-rail-header");
        setLeft(resizeHandle);
        setTop(header);
        setCenter(content);

        expanded.addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public boolean expanded() {
        return expanded.get();
    }

    public void setExpanded(boolean value) {
        expanded.set(value);
    }

    private void configureResizeHandle() {
        resizeHandle.getStyleClass().add("ui-rail-resize-handle");
        resizeHandle.setMinWidth(0);
        resizeHandle.setPrefWidth(0);
        resizeHandle.setMaxWidth(0);
        resizeHandle.setMouseTransparent(true);
    }

    private void refresh() {
        if (expanded.get()) {
            setCenter(content);
            toggle.setIcon(AppIcon.COLLAPSE, "Ocultar panel visual");
            copy.setVisible(true);
            copy.setManaged(true);
            resizeHandle.setVisible(true);
            resizeHandle.setManaged(true);
            setRailWidth(DEFAULT_EXPANDED_WIDTH);
            if (!getStyleClass().contains("ui-collapsible-rail-expanded")) {
                getStyleClass().add("ui-collapsible-rail-expanded");
            }
            getStyleClass().remove("ui-collapsible-rail-collapsed");
        } else {
            setCenter(null);
            toggle.setIcon(AppIcon.EXPAND, "Mostrar panel visual");
            copy.setVisible(false);
            copy.setManaged(false);
            resizeHandle.setVisible(false);
            resizeHandle.setManaged(false);
            setRailWidth(COLLAPSED_WIDTH);
            if (!getStyleClass().contains("ui-collapsible-rail-collapsed")) {
                getStyleClass().add("ui-collapsible-rail-collapsed");
            }
            getStyleClass().remove("ui-collapsible-rail-expanded");
        }
    }

    private void setRailWidth(double width) {
        setMinWidth(width);
        setPrefWidth(width);
        setMaxWidth(width);
    }
}
