package com.marcosmoreiradev.docupodcaststudio.presentation.components.admin;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Shared navigation/content shell for Voices, Image administration and Settings. */
public class AdminWorkspaceShell<M> extends BorderPane {
    private final VBox moduleHost = new VBox(12);

    public AdminWorkspaceShell(Node navigation) {
        getStyleClass().add("voice-workspace-shell");
        setLeft(navigation);
        moduleHost.getStyleClass().add("voice-module-content");
        VBox.setVgrow(moduleHost, Priority.ALWAYS);
        ScrollPane scroll = new ScrollPane(moduleHost);
        scroll.setFitToWidth(true);
        scroll.setAccessibleText("Contenido del módulo administrativo seleccionado");
        scroll.getStyleClass().add("voice-library-scroll");
        setCenter(scroll);
    }

    public final void setModuleContent(Node content) {
        moduleHost.getChildren().setAll(content);
    }
}
