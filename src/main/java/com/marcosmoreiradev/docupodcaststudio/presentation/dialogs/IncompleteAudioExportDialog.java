package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Product confirmation shown when export needs missing audio chunks first. */
public final class IncompleteAudioExportDialog {
    public boolean confirmRenderAndExport(Window owner) {
        ButtonType renderAndExport = new ButtonType("Renderizar y exportar", ButtonBar.ButtonData.YES);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Faltan fragmentos de audio sin renderizar. ¿Renderizar y luego exportar?",
                renderAndExport,
                cancel);
        alert.setTitle("Audio incompleto");
        alert.setHeaderText("No todos los fragmentos tienen audio");
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait().orElse(cancel).getButtonData() == ButtonBar.ButtonData.YES;
    }
}
