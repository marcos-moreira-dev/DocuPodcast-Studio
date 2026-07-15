package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Shell for the modular theatre AI workspace. */
final class TheatreAiWorkspaceShell extends BorderPane {
    private final VBox moduleHost = new VBox(12);

    TheatreAiWorkspaceShell(Node navigation) {
        getStyleClass().add("voice-workspace-shell");
        setLeft(navigation);
        moduleHost.getStyleClass().add("voice-module-content");
        VBox.setVgrow(moduleHost, Priority.ALWAYS);
        ScrollPane scroll = new ScrollPane(moduleHost);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("voice-library-scroll");
        setCenter(scroll);
    }

    void setModuleContent(Node content) {
        moduleHost.getChildren().setAll(content);
    }
}
