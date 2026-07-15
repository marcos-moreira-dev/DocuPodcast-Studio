package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Product confirmation shown when document audio needs an unavailable voice engine. */
public final class AudioEngineUnavailableDialog {
    public boolean show(Window owner, String detail) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Motor de voz no disponible");
        alert.setHeaderText("DocuPodcast necesita preparar un motor de voz antes de generar audio.");
        alert.setContentText((detail == null || detail.isBlank() ? "El motor de voz actual no está listo." : detail.strip())
                + "\n\n¿Quieres abrir Configuración para gestionarlo ahora?");
        ButtonType accept = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(accept, ButtonType.CANCEL);
        DialogStyler.apply(alert, owner);
        return alert.showAndWait().filter(accept::equals).isPresent();
    }
}
