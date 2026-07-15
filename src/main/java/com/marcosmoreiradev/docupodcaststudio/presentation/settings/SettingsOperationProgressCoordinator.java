package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Builds and owns the shared progress dialog used by long-running settings operations. */
final class SettingsOperationProgressCoordinator {
    OperationProgress show(Node anchor, String title, String header, String message) {
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(header);
        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) progressDialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setDisable(true);
        progressDialog.setOnCloseRequest(event -> {
            if (close.isDisabled()) {
                event.consume();
            }
        });

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 18, 24));
        Label explanation = new Label(message);
        explanation.setWrapText(true);
        explanation.setMinWidth(680);
        explanation.setPrefWidth(760);
        explanation.setMaxWidth(820);
        explanation.getStyleClass().add("settings-engine-status-message");

        ProgressBar progress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        progress.setMaxWidth(Double.MAX_VALUE);
        TextArea detail = new TextArea("Iniciando operación...");
        detail.setEditable(false);
        detail.setWrapText(true);
        detail.setPrefRowCount(4);
        detail.setMinHeight(Region.USE_PREF_SIZE);
        detail.setPrefHeight(118);
        detail.setMaxHeight(150);
        detail.setFocusTraversable(false);
        detail.getStyleClass().add("settings-engine-status-action");

        Label hint = new Label("Estado en vivo: si la operación tarda, esta línea cambiará con la última etapa reportada.");
        hint.setWrapText(true);
        hint.getStyleClass().add("settings-engine-status-folder");
        body.getChildren().addAll(explanation, progress, detail, hint);

        progressDialog.getDialogPane().setContent(body);
        progressDialog.getDialogPane().setMinWidth(860);
        progressDialog.getDialogPane().setPrefWidth(900);
        Window owner = anchor != null && anchor.getScene() != null ? anchor.getScene().getWindow() : null;
        DialogStyler.apply(progressDialog, owner);
        progressDialog.show();
        return new OperationProgress(progress, detail, hint, close);
    }
}

final class OperationProgress {
    private final ProgressBar progressBar;
    private final TextArea detail;
    private final Label hint;
    private final Button closeButton;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicLong lastUpdateMillis = new AtomicLong(System.currentTimeMillis());
    private final long startedMillis = System.currentTimeMillis();

    OperationProgress(ProgressBar progressBar, TextArea detail, Label hint, Button closeButton) {
        this.progressBar = progressBar;
        this.detail = detail;
        this.hint = hint;
        this.closeButton = closeButton;
        startHeartbeat();
    }

    void update(String message) {
        Runnable action = () -> {
            String text = message == null || message.isBlank() ? "Operación en curso..." : message.strip();
            detail.setText(text);
            lastUpdateMillis.set(System.currentTimeMillis());
            if (progressBar.getProgress() != ProgressBar.INDETERMINATE_PROGRESS) {
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            }
            hint.setText("Última actualización: " + LocalTime.now().withNano(0)
                    + ". Tiempo transcurrido: " + elapsedLabel(System.currentTimeMillis() - startedMillis)
                    + ". La operación sigue activa mientras este diálogo no muestre completado o error.");
        };
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    void finish(String message) {
        finished.set(true);
        progressBar.setProgress(1.0);
        String text = message == null || message.isBlank() ? "Operación completada." : message;
        detail.setText(text);
        if (text.contains("CUDA") && text.contains("no disponible")) {
            hint.setText("Prueba completada: CUDA no quedó disponible para Voz IA avanzada. En Automático se usará CPU; en Dispositivo específico se intentará el dispositivo solicitado.");
        } else {
            hint.setText("Completado. Ya puedes cerrar esta ventana y volver a verificar el motor.");
        }
        closeButton.setDisable(false);
        closeButton.setText("Cerrar");
    }

    void fail(String message) {
        finished.set(true);
        progressBar.setProgress(0.0);
        detail.setText(message == null || message.isBlank() ? "La operación no se completó." : message);
        String text = message == null ? "" : message;
        if (text.contains("Reporte técnico") || text.contains("Reporte de preparación local")) {
            hint.setText("No se completó. Copia el detalle de arriba o adjunta el reporte indicado para revisar exactamente qué recurso quedó pendiente.");
        } else if (text.contains("CUDA") && (text.contains("CPU") || text.contains("no disponible"))) {
            hint.setText("Prueba completada: CUDA no quedó disponible para Voz IA avanzada. En Automático se usará CPU; en Dispositivo específico se intentará el dispositivo solicitado.");
        } else {
            hint.setText("Error o interrupción. Copia el detalle de arriba y vuelve a intentar. Si necesitas más datos, usa Soporte y diagnóstico.");
        }
        closeButton.setDisable(false);
        closeButton.setText("Cerrar");
    }

    private void startHeartbeat() {
        Thread heartbeat = new Thread(() -> {
            while (!finished.get()) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (finished.get()) {
                    return;
                }
                Platform.runLater(() -> {
                    if (finished.get()) {
                        return;
                    }
                    long idleMillis = System.currentTimeMillis() - lastUpdateMillis.get();
                    hint.setText("Operación activa. Tiempo transcurrido: " + elapsedLabel(System.currentTimeMillis() - startedMillis)
                            + "; última señal hace " + elapsedLabel(idleMillis)
                            + ". Si prepara recursos grandes, puede tardar varios minutos.");
                });
            }
        }, "settings-operation-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    private static String elapsedLabel(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return minutes <= 0L ? remainingSeconds + " s" : minutes + " min " + remainingSeconds + " s";
    }
}
