package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Explicit source decision used after the project name/mode step. */
public final class ProjectInitialSourceDialog {
    public enum Decision {
        SELECT_SOURCE,
        CREATE_WITHOUT_SOURCE,
        CANCEL
    }

    public Decision show(Window owner) {
        ButtonType selectSource = NativeDialogResponse.button("Elegir fuente", ButtonBar.ButtonData.YES);
        ButtonType withoutSource = NativeDialogResponse.button("Crear sin fuente", ButtonBar.ButtonData.NO);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = NativeDecisionDialog.create(owner, Alert.AlertType.CONFIRMATION,
                "Puedes asociar ahora el documento principal del proyecto o crear el proyecto sin fuente por ahora.",
                selectSource,
                withoutSource,
                cancel);
        alert.setTitle("Fuente inicial del proyecto");
        alert.setHeaderText("Define la fuente primaria antes de crear el proyecto.");
        ButtonType result = alert.showAndWait().orElse(cancel);
        if (result == selectSource) {
            return Decision.SELECT_SOURCE;
        }
        if (result == withoutSource) {
            return Decision.CREATE_WITHOUT_SOURCE;
        }
        return Decision.CANCEL;
    }
}
