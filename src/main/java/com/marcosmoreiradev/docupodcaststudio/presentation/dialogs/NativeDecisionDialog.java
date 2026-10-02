package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Explicit exception for short native decisions. The dialog content receives product chrome,
 * while JavaFX keeps ownership of the conventional confirmation button bar and keyboard semantics.
 */
public final class NativeDecisionDialog {
    private NativeDecisionDialog() { }

    public static Alert create(Window owner, Alert.AlertType type, String content, ButtonType... buttons) {
        return StudioMessageDialog.create(
                owner,
                type,
                "",
                "",
                content,
                "",
                buttons == null ? new ButtonType[0] : buttons);
    }

    public static Alert create(
            Window owner,
            Alert.AlertType type,
            String title,
            String header,
            String content,
            String technicalDetail,
            ButtonType... buttons) {
        return StudioMessageDialog.create(
                owner, type, title, header, content, technicalDetail,
                buttons == null ? new ButtonType[0] : buttons);
    }
}
