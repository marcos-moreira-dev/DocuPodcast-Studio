package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

/** Confirmation dialog for dirty project transitions. */
public final class UnsavedChangesDialog {
    public enum Decision {
        SAVE,
        DISCARD,
        CANCEL
    }

    public Decision show(Window owner) {
        ButtonType save = NativeDialogResponse.button("Guardar", ButtonBar.ButtonData.YES);
        ButtonType discard = NativeDialogResponse.button("Descartar", ButtonBar.ButtonData.NO);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = NativeDecisionDialog.create(owner, Alert.AlertType.CONFIRMATION,
                "El proyecto activo tiene cambios sin guardar.", save, discard, cancel);
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("¿Qué quieres hacer antes de continuar?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancel) {
            return Decision.CANCEL;
        }
        if (result.get() == save) {
            return Decision.SAVE;
        }
        return Decision.DISCARD;
    }
}
