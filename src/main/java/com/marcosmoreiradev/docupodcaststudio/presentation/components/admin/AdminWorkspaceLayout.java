package com.marcosmoreiradev.docupodcaststudio.presentation.components.admin;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Reusable layout primitives for administrative mini-applications. */
public final class AdminWorkspaceLayout {
    private AdminWorkspaceLayout() { }

    public static VBox moduleRoot(String title, String eyebrowText, String summaryText) {
        VBox root = new VBox(12);
        root.getStyleClass().add("voice-module-root");
        Label eyebrow = new Label(eyebrowText);
        eyebrow.getStyleClass().add("voice-library-eyebrow");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("voice-library-title");
        Label summary = new Label(summaryText);
        summary.setWrapText(true);
        summary.getStyleClass().add("voice-library-summary");
        root.getChildren().addAll(eyebrow, titleLabel, summary);
        return root;
    }

    public static VBox section(String title) {
        VBox box = new VBox(8);
        Label label = new Label(title);
        label.getStyleClass().add("document-side-section-title");
        box.getChildren().add(label);
        return box;
    }

    public static HBox masterDetail(Node master, Node detail, double min, double pref, double max) {
        HBox row = new HBox(14);
        row.getStyleClass().add("voice-master-detail");
        row.getChildren().addAll(master, detail);
        if (master instanceof Region region) {
            region.getStyleClass().add("voice-master-pane");
            region.setMinWidth(min);
            region.setPrefWidth(pref);
            region.setMaxWidth(max);
        }
        if (detail instanceof Region region) {
            region.getStyleClass().add("voice-detail-pane");
            HBox.setHgrow(region, Priority.ALWAYS);
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    public static HBox balancedMasterDetail(Node master, Node detail) {
        HBox row = masterDetail(master, detail, 360, 520, Double.MAX_VALUE);
        row.getStyleClass().add("voice-master-detail-balanced");
        if (master instanceof Region region) HBox.setHgrow(region, Priority.ALWAYS);
        return row;
    }

    public static VBox detailStack(Node... children) {
        VBox box = new VBox(10);
        box.getChildren().addAll(children);
        return box;
    }

    public static void detachNode(Node node) {
        if (node == null) return;
        Parent parent = node.getParent();
        if (parent instanceof Pane pane) pane.getChildren().remove(node);
    }
}
