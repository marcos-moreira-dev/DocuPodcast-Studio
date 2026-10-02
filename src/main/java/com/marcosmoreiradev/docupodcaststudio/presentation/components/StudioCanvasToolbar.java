package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Compact, transversal toolbar surface for canvas editors.
 *
 * <p>The component owns only the visual grouping and accessibility contract. Domain actions
 * remain in the owning editor, which lets Documentary, Theatre and image composition share the
 * same interaction language without coupling their state machines.</p>
 */
public final class StudioCanvasToolbar extends VBox {
    /** Keeps a category caption and its controls together when the surrounding strip wraps. */
    public static javafx.scene.layout.HBox group(String caption, Node... controls) {
        Label label = new Label(caption);
        label.getStyleClass().add(AppStyles.UI_CANVAS_TOOLBAR_GROUP_LABEL);
        label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        javafx.scene.layout.HBox group = new javafx.scene.layout.HBox(6, label);
        group.getChildren().addAll(controls);
        group.setAlignment(Pos.CENTER_LEFT);
        group.setPadding(new Insets(2, 10, 2, 2));
        group.setStyle("-fx-border-color: transparent #dbe3f1 transparent transparent; -fx-border-width: 0 1 0 0;");
        group.setAccessibleText(caption + ", grupo de herramientas");
        group.getStyleClass().add("studio-canvas-tool-group");
        return group;
    }
    public StudioCanvasToolbar(String accessibleDescription) {
        setSpacing(5);
        setPadding(new Insets(8, 10, 7, 10));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add(AppStyles.UI_CANVAS_TOOLBAR);
        setAccessibleRoleDescription(accessibleDescription == null || accessibleDescription.isBlank()
                ? "Herramientas del lienzo"
                : accessibleDescription);
    }

    /**
     * Adds one wrapping row. A short semantic label is optional; when present it is rendered as a
     * muted group marker and exposed to assistive technology together with the row controls.
     */
    public FlowPane addRow(String semanticLabel, Node... controls) {
        FlowPane row = new FlowPane(8, 6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add(AppStyles.UI_CANVAS_TOOLBAR_ROW);
        if (semanticLabel != null && !semanticLabel.isBlank()) {
            Label label = new Label(semanticLabel);
            label.getStyleClass().add(AppStyles.UI_CANVAS_TOOLBAR_GROUP_LABEL);
            label.setAccessibleText(semanticLabel + ", grupo de herramientas");
            row.getChildren().add(label);
        }
        if (controls != null) {
            row.getChildren().addAll(controls);
        }
        getChildren().add(row);
        return row;
    }

    /** Adds an unlabeled contextual row. */
    public FlowPane addRow(Node... controls) {
        return addRow(null, controls);
    }
}
