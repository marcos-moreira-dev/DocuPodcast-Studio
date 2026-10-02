package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Product confirmation shown when document audio needs an unavailable voice engine. */
public final class AudioEngineUnavailableDialog {
    public boolean show(Window owner, String detail) {
        ButtonType accept = NativeDialogResponse.button("Aceptar", ButtonBar.ButtonData.OK_DONE);
        Alert alert = NativeDecisionDialog.create(
                owner,
                Alert.AlertType.CONFIRMATION,
                "Motor de voz no disponible",
                "DocuPodcast necesita preparar un motor de voz antes de generar audio.",
                "El motor de voz actual no está listo.\n\n"
                        + "¿Quieres abrir Configuración para gestionarlo ahora?",
                detail == null ? "" : detail,
                accept,
                ButtonType.CANCEL);
        return alert.showAndWait().filter(accept::equals).isPresent();
    }
}
