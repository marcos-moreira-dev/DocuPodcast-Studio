package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Horizontal split for module content where one region can fold away and leave
 * the main work region with more room.
 */
public final class CollapsibleModuleSplitPane extends BorderPane {
    private final BooleanProperty primaryVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty secondaryVisible = new SimpleBooleanProperty(true);
    private final String primaryLabel;
    private final String secondaryLabel;
    private final BorderPane primarySlot = new BorderPane();
    private final Node primary;
    private final Node secondary;
    private final SplitPane split = StudioViewportControls.splitPane();
    private final HBox collapsedContent = new HBox();
    private final double dividerPosition;
    private final boolean showPrimaryCollapsedStrip;

    public CollapsibleModuleSplitPane(String primaryLabel, Node primary, Node secondary, double dividerPosition) {
        this(primaryLabel, primary, "Panel derecho", secondary, dividerPosition);
    }

    public CollapsibleModuleSplitPane(
            String primaryLabel,
            Node primary,
            String secondaryLabel,
            Node secondary,
            double dividerPosition) {
        this(primaryLabel, primary, secondaryLabel, secondary, dividerPosition, true);
    }

    public CollapsibleModuleSplitPane(
            String primaryLabel,
            Node primary,
            String secondaryLabel,
            Node secondary,
            double dividerPosition,
            boolean showPrimaryCollapsedStrip) {
        this.primaryLabel = primaryLabel == null || primaryLabel.isBlank() ? "Panel" : primaryLabel;
        this.secondaryLabel = secondaryLabel == null || secondaryLabel.isBlank() ? "Panel derecho" : secondaryLabel;
        this.primary = primary;
        this.secondary = secondary;
        this.dividerPosition = dividerPosition <= 0 || dividerPosition >= 1 ? 0.45 : dividerPosition;
        this.showPrimaryCollapsedStrip = showPrimaryCollapsedStrip;

        getStyleClass().add("ui-collapsible-module-split");
        primarySlot.getStyleClass().add("ui-collapsible-module-split-primary");
        collapsedContent.getStyleClass().add("ui-collapsible-module-split-collapsed-content");

        primaryVisible.addListener((obs, oldValue, newValue) -> render());
        secondaryVisible.addListener((obs, oldValue, newValue) -> render());
        render();
    }

    public BooleanProperty primaryVisibleProperty() {
        return primaryVisible;
    }

    public BooleanProperty secondaryVisibleProperty() {
        return secondaryVisible;
    }

    public void showPrimary() {
        primaryVisible.set(true);
    }

    private void render() {
        split.getItems().clear();
        collapsedContent.getChildren().clear();
        if (primaryVisible.get() && secondaryVisible.get()) {
            primarySlot.setTop(toggleRow());
            primarySlot.setCenter(primary);
            split.getItems().setAll(primarySlot, secondary);
            split.setDividerPositions(dividerPosition);
            SplitPane.setResizableWithParent(primarySlot, Boolean.TRUE);
            SplitPane.setResizableWithParent(secondary, Boolean.TRUE);
            setCenter(split);
            return;
        }

        if (!primaryVisible.get() && secondaryVisible.get()) {
            primarySlot.setTop(null);
            primarySlot.setCenter(null);
            if (!showPrimaryCollapsedStrip) {
                setCenter(secondary);
                return;
            }
            VBox strip = collapsedStrip(primaryLabel, () -> primaryVisible.set(true));
            collapsedContent.getChildren().setAll(strip, secondary);
            HBox.setHgrow(secondary, Priority.ALWAYS);
            setCenter(collapsedContent);
            return;
        }

        if (primaryVisible.get()) {
            primarySlot.setTop(toggleRow());
            primarySlot.setCenter(primary);
            VBox strip = collapsedStrip(secondaryLabel, () -> secondaryVisible.set(true));
            collapsedContent.getChildren().setAll(primarySlot, strip);
            HBox.setHgrow(primarySlot, Priority.ALWAYS);
            setCenter(collapsedContent);
            return;
        }

        primarySlot.setTop(null);
        primarySlot.setCenter(null);
        VBox primaryStrip = collapsedStrip(primaryLabel, () -> primaryVisible.set(true));
        VBox secondaryStrip = collapsedStrip(secondaryLabel, () -> secondaryVisible.set(true));
        collapsedContent.getChildren().setAll(primaryStrip, secondaryStrip);
        setCenter(collapsedContent);
    }

    private HBox toggleRow() {
        SidePanelToggleButton toggle = new SidePanelToggleButton(
                AppIcon.COLLAPSE,
                "Ocultar " + primaryLabel,
                () -> primaryVisible.set(false));
        HBox row = new HBox(toggle);
        row.getStyleClass().add("ui-collapsible-module-split-toggle-row");
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    private VBox collapsedStrip(String labelText, Runnable action) {
        SidePanelToggleButton toggle = new SidePanelToggleButton(
                AppIcon.EXPAND,
                "Mostrar " + labelText,
                action);
        Label label = new Label(labelText);
        label.getStyleClass().add("ui-collapsible-module-split-strip-label");
        label.setWrapText(true);
        VBox strip = new VBox(8, toggle, label);
        strip.getStyleClass().add("ui-collapsible-module-split-strip");
        strip.setAlignment(Pos.TOP_CENTER);
        return strip;
    }
}
