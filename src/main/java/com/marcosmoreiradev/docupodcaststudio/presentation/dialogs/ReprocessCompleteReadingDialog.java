package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Confirms an explicit full-document semantic/listening reprocessing request. */
public final class ReprocessCompleteReadingDialog {
    public boolean confirm(Window owner) {
        ButtonType reprocess = NativeDialogResponse.button(
                "Reprocesar", ButtonBar.ButtonData.YES);
        ButtonType cancel = NativeDialogResponse.button(
                "Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = NativeDecisionDialog.create(
                owner,
                Alert.AlertType.CONFIRMATION,
                "Reprocesar lectura completa",
                "La lectura completa ya está preparada",
                "¿Quieres volver a procesarla?\n\nSe conservarán y reutilizarán "
                        + "los derivados que sigan siendo compatibles.",
                "",
                reprocess,
                cancel);
        return alert.showAndWait().orElse(cancel).getButtonData()
                == ButtonBar.ButtonData.YES;
    }
}
