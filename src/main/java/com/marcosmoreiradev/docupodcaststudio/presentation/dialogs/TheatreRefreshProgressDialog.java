package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Shared-styled, non-blocking progress surface for package refresh work. */
public final class TheatreRefreshProgressDialog {
    private static final ButtonType HIDE = NativeDialogResponse.button(
            "Ocultar", ButtonBar.ButtonData.CANCEL_CLOSE);
    private static final ButtonType CANCEL = NativeDialogResponse.button(
            "Cancelar actualización", ButtonBar.ButtonData.OTHER);

    private final Dialog<Void> dialog = StudioDialogShell.dialog();
    private final Label detail = new Label("Preparando la actualización…");
    private final ProgressBar progress = StudioFeedbackControls.progressBar();
    private Task<?> task;

    public TheatreRefreshProgressDialog(Window owner) {
        this(owner, true);
    }

    public TheatreRefreshProgressDialog(Window owner, boolean cancellable) {
        dialog.setTitle("Actualizar obra teatral");
        dialog.setHeaderText(cancellable
                ? "DocuPodcast compara la carpeta vinculada antes de modificar el proyecto."
                : "DocuPodcast está aplicando la transacción confirmada. Puedes ocultar esta ventana.");
        dialog.getDialogPane().getButtonTypes().add(HIDE);
        if (cancellable) dialog.getDialogPane().getButtonTypes().add(CANCEL);
        detail.setWrapText(true);
        detail.setMaxWidth(540);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setProgress(-1);
        VBox content = new VBox(12, detail, progress);
        content.setPadding(new Insets(18, 22, 18, 22));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(600);
        if (cancellable) {
            Button cancel = (Button) dialog.getDialogPane().lookupButton(CANCEL);
            cancel.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                if (task != null && !task.isDone()) {
                    cancel.setDisable(true);
                    detail.setText("Cancelando de forma segura…");
                    task.cancel(true);
                }
            });
        }
        DialogStyler.apply(dialog, owner);
    }

    public void bind(Task<?> task) {
        this.task = task;
        detail.textProperty().bind(task.messageProperty());
        progress.progressProperty().bind(task.progressProperty());
    }

    public void show() { dialog.show(); }

    public void close() {
        detail.textProperty().unbind();
        progress.progressProperty().unbind();
        dialog.hide();
        dialog.close();
    }
}
