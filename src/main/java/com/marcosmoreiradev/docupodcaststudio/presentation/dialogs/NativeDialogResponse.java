package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** Explicit boundary for the deliberately native response chrome of JavaFX dialogs. */
public final class NativeDialogResponse {
    private NativeDialogResponse() { }

    public static Alert alert(Alert.AlertType type) {
        return prepare(new Alert(type));
    }

    public static Alert alert(Alert.AlertType type, String content, ButtonType... buttons) {
        return prepare(new Alert(type, content, buttons));
    }

    public static ButtonType button(String text, ButtonBar.ButtonData data) {
        return new ButtonType(text, data);
    }

    private static Alert prepare(Alert alert) {
        alert.getDialogPane().getStyleClass().add("product-dialog");
        return alert;
    }
}
