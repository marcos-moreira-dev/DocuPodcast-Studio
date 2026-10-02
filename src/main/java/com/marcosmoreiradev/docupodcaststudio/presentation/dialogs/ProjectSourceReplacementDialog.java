package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.nio.file.Path;

/** Confirmation dialog before replacing the active project's primary source. */
public final class ProjectSourceReplacementDialog {
    public boolean confirm(Window owner, Path currentSource) {
        ButtonType replace = NativeDialogResponse.button("Reemplazar fuente", ButtonBar.ButtonData.YES);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        String current = currentSource == null || currentSource.getFileName() == null
                ? "la fuente actual"
                : currentSource.getFileName().toString();
        Alert alert = NativeDecisionDialog.create(owner, Alert.AlertType.CONFIRMATION,
                "Se reemplazara " + current + " como fuente primaria. La lectura preparada, storyboard y audio derivado dejaran de ser validos para la nueva fuente.",
                replace,
                cancel);
        alert.setTitle("Reemplazar fuente del proyecto");
        alert.setHeaderText("Esta accion cambia el documento principal del proyecto.");
        return alert.showAndWait().filter(replace::equals).isPresent();
    }
}
