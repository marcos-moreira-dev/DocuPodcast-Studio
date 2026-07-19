package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Shared compact disclosure section for dialogs, inspectors and workspaces. */
public final class CollapsibleSection extends VBox {
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private final Node content;
    private final SidePanelToggleButton toggle;

    public CollapsibleSection(String title, Node content, boolean initiallyExpanded) {
        super(0);
        this.content = content;
        this.toggle = new SidePanelToggleButton(
                initiallyExpanded ? AppIcon.COLLAPSE : AppIcon.EXPAND,
                initiallyExpanded ? "Ocultar " + title : "Mostrar " + title,
                null);
        getStyleClass().add("ui-collapsible-section");

        DocumentSidePanelChrome.Header chrome = DocumentSidePanelChrome.header(title, "", toggle);
        HBox header = chrome.node();
        header.getStyleClass().add("ui-collapsible-section-header");
        chrome.copy().setOnMouseClicked(event -> setExpanded(!expanded()));
        toggle.setOnAction(event -> setExpanded(!expanded()));

        content.getStyleClass().add("ui-collapsible-section-content");
        getChildren().addAll(header, content);
        expanded.addListener((observable, oldValue, newValue) -> refresh());
        setExpanded(initiallyExpanded);
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

    private void refresh() {
        boolean visible = expanded();
        content.setVisible(visible);
        content.setManaged(visible);
        toggle.setIcon(visible ? AppIcon.COLLAPSE : AppIcon.EXPAND,
                visible ? "Ocultar seccion" : "Mostrar seccion");
    }
}
