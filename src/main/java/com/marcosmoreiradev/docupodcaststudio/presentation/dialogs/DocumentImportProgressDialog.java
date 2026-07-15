package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Window;

/** Non-blocking progress dialog for source-document imports. */
public final class DocumentImportProgressDialog {
    private final Dialog<Void> dialog = new Dialog<>();
    private final Label detail = new Label("Preparando documento...");

    public DocumentImportProgressDialog(Window owner, String filename) {
        dialog.setTitle("Abriendo fuente documental");
        dialog.setHeaderText("DocuPodcast está abriendo la fuente documental. Puedes ocultar esta ventana; la lectura continúa en segundo plano.");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (close != null) {
            close.setText("Ocultar");
        }
        detail.setWrapText(true);
        detail.setText("Leyendo " + (filename == null || filename.isBlank() ? "la fuente documental" : filename) + " para mostrarla en Documento.");
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(48, 48);
        HBox progressLine = new HBox(18, indicator, detail);
        progressLine.setAlignment(Pos.CENTER_LEFT);
        detail.setMaxWidth(430);
        VBox content = new VBox(14, progressLine);
        content.setPadding(new Insets(18, 22, 18, 22));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(560);
        DialogStyler.apply(dialog, owner);
    }

    public void bind(Task<?> task) {
        detail.textProperty().bind(task.messageProperty());
    }

    public void show() { dialog.show(); }

    public void close() {
        detail.textProperty().unbind();
        dialog.hide();
        dialog.close();
    }
}
