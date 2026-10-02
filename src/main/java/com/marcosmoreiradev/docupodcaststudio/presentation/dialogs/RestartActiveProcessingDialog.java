package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Confirms replacing an active document-processing operation with a fresh full run. */
public final class RestartActiveProcessingDialog {
    public boolean confirm(Window owner) {
        ButtonType restart = NativeDialogResponse.button(
                "Cancelar e iniciar de nuevo", ButtonBar.ButtonData.YES);
        ButtonType keepCurrent = NativeDialogResponse.button(
                "Mantener proceso actual", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = NativeDecisionDialog.create(
                owner,
                Alert.AlertType.CONFIRMATION,
                "Reiniciar procesamiento",
                "Ya hay un procesamiento en ejecución",
                "¿Quieres cancelarlo e iniciar nuevamente la lectura completa con la "
                        + "configuración actual?\n\nLos resultados terminados y compatibles se conservarán.",
                "",
                restart,
                keepCurrent);
        return alert.showAndWait().orElse(keepCurrent).getButtonData()
                == ButtonBar.ButtonData.YES;
    }
}
