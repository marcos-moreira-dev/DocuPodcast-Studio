package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Shell for the modular Voices workspace: navigation left, selected module right. */
public final class VoiceWorkspaceShell extends BorderPane {
    private final VBox moduleHost = new VBox(12);

    public VoiceWorkspaceShell(Node navigation) {
        getStyleClass().add("voice-workspace-shell");
        setLeft(navigation);
        moduleHost.getStyleClass().add("voice-module-content");
        VBox.setVgrow(moduleHost, Priority.ALWAYS);
        ScrollPane scroll = new ScrollPane(moduleHost);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("voice-library-scroll");
        setCenter(scroll);
    }

    public void setModuleContent(Node content) {
        moduleHost.getChildren().setAll(content);
    }
}
