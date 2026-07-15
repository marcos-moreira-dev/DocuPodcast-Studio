package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Layout primitives for the theatre AI workspace. */
final class TheatreAiWorkspaceLayout {
    private TheatreAiWorkspaceLayout() {
    }

    static VBox moduleRoot(String title, String eyebrowText, String summaryText) {
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

    static VBox section(String title) {
        VBox box = new VBox(8);
        Label label = new Label(title);
        label.getStyleClass().add("document-side-section-title");
        box.getChildren().add(label);
        return box;
    }

    static HBox masterDetail(Node master, Node detail) {
        HBox row = new HBox(14);
        row.getStyleClass().add("voice-master-detail");
        row.getChildren().addAll(master, detail);
        if (master instanceof Region region) {
            region.getStyleClass().add("voice-master-pane");
            region.setMinWidth(320);
            region.setPrefWidth(420);
            region.setMaxWidth(520);
        }
        if (detail instanceof Region region) {
            region.getStyleClass().add("voice-detail-pane");
            HBox.setHgrow(region, Priority.ALWAYS);
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    static HBox balancedMasterDetail(Node master, Node detail) {
        HBox row = masterDetail(master, detail);
        row.getStyleClass().add("voice-master-detail-balanced");
        if (master instanceof Region region) {
            region.setMinWidth(360);
            region.setPrefWidth(520);
            region.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(region, Priority.ALWAYS);
        }
        if (detail instanceof Region region) {
            region.setMinWidth(360);
            region.setPrefWidth(520);
            region.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(region, Priority.ALWAYS);
        }
        return row;
    }

    static VBox detailStack(Node... children) {
        VBox box = new VBox(10);
        box.getChildren().addAll(children);
        return box;
    }

    static void detachNode(Node node) {
        if (node == null) {
            return;
        }
        Parent parent = node.getParent();
        if (parent instanceof Pane pane) {
            pane.getChildren().remove(node);
        }
    }
}
