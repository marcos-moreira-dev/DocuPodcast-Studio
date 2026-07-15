package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Placeholder used until a real workspace is implemented. */
public final class PlaceholderWorkspaceView extends VBox {
    public PlaceholderWorkspaceView(WorkspaceKind kind) {
        getStyleClass().add("placeholder-workspace");
        setPadding(new Insets(32));
        setSpacing(12);

        Label title = new Label(kind.displayName());
        title.getStyleClass().add("workspace-title");

        Label message = new Label("Workspace planificado. Se implementará en una tanda posterior siguiendo el onboarding técnico.");
        message.getStyleClass().add("workspace-subtitle");
        message.setWrapText(true);

        getChildren().addAll(title, message);
    }
}
