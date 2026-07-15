package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Objects;

/** Product dialog explaining that the project uses its own portable source-document copy. */
public final class ProjectSourceCopyNoticeDialog {
    public boolean show(Window owner, Path canonicalSource) {
        String source = Objects.toString(canonicalSource, "source/<documento>");
        Label message = new Label("DocuPodcast guardó una copia del documento fuente dentro de la carpeta del proyecto. "
                + "A partir de ahora, Refrescar contenido leerá esa copia, no el archivo externo original.\n\n"
                + "Copia usada por el proyecto:\n" + source + "\n\n"
                + "Si quieres actualizar la lectura, edita ese archivo dentro de la carpeta source del proyecto y luego usa Refrescar contenido.");
        message.setWrapText(true);
        message.setMinWidth(680);
        message.setPrefWidth(760);
        message.setMaxWidth(820);
        message.setMinHeight(Region.USE_PREF_SIZE);
        CheckBox dontShowAgain = new CheckBox("No volver a mostrar este aviso");
        VBox content = new VBox(14, message, dontShowAgain);
        content.setPadding(new Insets(20, 24, 18, 24));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Documento fuente guardado en el proyecto");
        alert.setHeaderText("El proyecto ya es portable con su propia copia del documento.");
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinWidth(860);
        alert.getDialogPane().setPrefWidth(900);
        DialogStyler.apply(alert, owner);
        alert.showAndWait();
        return dontShowAgain.isSelected();
    }
}
