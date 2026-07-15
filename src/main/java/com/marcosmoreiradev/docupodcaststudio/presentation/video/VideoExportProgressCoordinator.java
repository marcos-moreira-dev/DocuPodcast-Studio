package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns the blocking progress dialog and background worker for final MP4 export. */
public final class VideoExportProgressCoordinator {
    private final FxBackgroundTaskRunner backgroundTaskRunner;

    public VideoExportProgressCoordinator() {
        this(new FxBackgroundTaskRunner());
    }

    VideoExportProgressCoordinator(FxBackgroundTaskRunner backgroundTaskRunner) {
        this.backgroundTaskRunner = Objects.requireNonNull(backgroundTaskRunner, "backgroundTaskRunner");
    }

    public void export(Window owner,
                       Path targetFile,
                       VideoExportOptions options,
                       VideoExportAction exportAction,
                       Consumer<Throwable> failurePresenter) {
        Objects.requireNonNull(targetFile, "targetFile");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(exportAction, "exportAction");
        Consumer<Throwable> onFailure = failurePresenter == null ? ignored -> { } : failurePresenter;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Exportando video");
        dialog.setHeaderText("Renderizando MP4 final");
        DialogStyler.apply(dialog, owner);
        VideoRenderProgressView progressView = new VideoRenderProgressView();
        AtomicBoolean cancelRequested = new AtomicBoolean(false);
        progressView.cancelButton().setOnAction(event -> {
            cancelRequested.set(true);
            progressView.update(VideoRenderProgress.cancelled(0, 0));
        });
        dialog.getDialogPane().setContent(progressView);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setDisable(true);
        dialog.setOnCloseRequest(event -> {
            if (!cancelRequested.get() && closeButton.isDisable()) {
                event.consume();
            }
        });

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                exportAction.export(targetFile, options, progress -> Platform.runLater(() -> progressView.update(progress)),
                        cancelRequested::get);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            progressView.update(VideoRenderProgress.completed(1, targetFile.toString()));
            closeButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            closeButton.setDisable(false);
            Throwable ex = task.getException();
            if (cancelRequested.get()) {
                progressView.update(VideoRenderProgress.cancelled(0, 0));
                return;
            }
            Throwable safe = actionableFailure(ex);
            progressView.update(VideoRenderProgress.failed(0, 0, safe.getMessage()));
            onFailure.accept(safe);
        });
        backgroundTaskRunner.start("docupodcast-final-video-export", task);
        dialog.showAndWait();
    }

    static Throwable actionableFailure(Throwable error) {
        Throwable safe = error == null ? new RuntimeException("Error desconocido") : error;
        Throwable cursor = safe;
        for (int depth = 0; cursor != null && depth < 32; depth++, cursor = cursor.getCause()) {
            if (cursor instanceof LinkageError) {
                return new IllegalStateException(
                        "La aplicación abierta está usando una compilación anterior o incompleta. "
                                + "Cierra DocuPodcast Studio, inicia una instancia nueva y repite la exportación. "
                                + "El proyecto y los archivos de origen no se han modificado.",
                        safe);
            }
        }
        return safe;
    }

    @FunctionalInterface
    public interface VideoExportAction {
        void export(Path targetFile,
                    VideoExportOptions options,
                    Consumer<VideoRenderProgress> progress,
                    BooleanSupplier cancellationRequested) throws Exception;
    }
}
