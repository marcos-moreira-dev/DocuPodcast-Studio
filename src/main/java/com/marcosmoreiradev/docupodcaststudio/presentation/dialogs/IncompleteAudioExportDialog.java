package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Product confirmation shown when export needs missing audio chunks first. */
public final class IncompleteAudioExportDialog {
    public boolean confirmRenderAndExport(Window owner) {
        return confirmRenderAndExport(owner,
                "Faltan fragmentos de audio sin renderizar. ¿Renderizar y luego exportar?",
                "No todos los fragmentos tienen audio");
    }

    public boolean confirmRenderAndExport(Window owner, String detail, String header) {
        ButtonType renderAndExport = NativeDialogResponse.button("Renderizar y exportar", ButtonBar.ButtonData.YES);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = NativeDecisionDialog.create(owner, Alert.AlertType.CONFIRMATION,
                detail,
                renderAndExport,
                cancel);
        alert.setTitle("Audio incompleto");
        alert.setHeaderText(header);
        return alert.showAndWait().orElse(cancel).getButtonData() == ButtonBar.ButtonData.YES;
    }
}
