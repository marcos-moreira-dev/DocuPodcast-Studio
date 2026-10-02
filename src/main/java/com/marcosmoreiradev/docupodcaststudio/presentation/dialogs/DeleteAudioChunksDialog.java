package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Confirmation dialog for deleting generated audio chunks from the current project. */
public final class DeleteAudioChunksDialog {
    public boolean confirm(Window owner) {
        ButtonType delete = NativeDialogResponse.button("Eliminar chunks", ButtonBar.ButtonData.YES);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = NativeDecisionDialog.create(owner, Alert.AlertType.CONFIRMATION,
                "Se borrarán los jobs, chunks y manifiestos de audio generados del proyecto actual. "
                        + "No se borrarán el documento, voces, imágenes ni el proyecto.",
                delete,
                cancel);
        alert.setTitle("Eliminar audio generado");
        alert.setHeaderText("Eliminar todos los chunks de audio del proyecto actual");
        return alert.showAndWait().orElse(cancel).getButtonData() == ButtonBar.ButtonData.YES;
    }
}
