package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Shared fullscreen image viewer for document and theatre visual previews. */
public final class ImageFullscreenViewer {
    private ImageFullscreenViewer() {
    }

    public static void show(
            String uri,
            Window owner,
            Collection<String> stylesheets,
            String title,
            String errorPrefix,
            Consumer<String> errorReporter) {
        show(uri, owner, stylesheets, title, errorPrefix, errorReporter, null, null, null);
    }

    public static void show(
            String uri,
            Window owner,
            Collection<String> stylesheets,
            String title,
            String errorPrefix,
            Consumer<String> errorReporter,
            String escapeHint,
            Runnable beforeShow,
            Runnable afterClose) {
        String normalized = uri == null ? "" : uri.strip();
        if (normalized.isBlank()) {
            return;
        }
        try {
            Image image = new Image(normalized, false);
            if (image.isError()) {
                throw new IllegalArgumentException("No se pudo cargar la imagen.");
            }
            show(image, owner, stylesheets, title, errorPrefix, errorReporter, escapeHint, beforeShow, afterClose);
        } catch (RuntimeException ex) {
            if (errorReporter != null) {
                String message = ex.getMessage() == null ? "" : ex.getMessage();
                String prefix = errorPrefix == null || errorPrefix.isBlank()
                        ? "No se pudo mostrar la imagen completa"
                        : errorPrefix;
                errorReporter.accept(prefix + ": " + message);
            }
        }
    }

    public static void show(
            Image image,
            Window owner,
            Collection<String> stylesheets,
            String title,
            String errorPrefix,
            Consumer<String> errorReporter) {
        show(image, owner, stylesheets, title, errorPrefix, errorReporter, null, null, null);
    }

    public static void show(
            Image image,
            Window owner,
            Collection<String> stylesheets,
            String title,
            String errorPrefix,
            Consumer<String> errorReporter,
            String escapeHint,
            Runnable beforeShow,
            Runnable afterClose) {
        if (image == null || image.isError()) {
            if (errorReporter != null) {
                String prefix = errorPrefix == null || errorPrefix.isBlank()
                        ? "No se pudo mostrar la imagen completa"
                        : errorPrefix;
                errorReporter.accept(prefix + ": imagen no disponible");
            }
            return;
        }
        try {
            ImageView fullImage = new ImageView(image);
            fullImage.setPreserveRatio(true);
            fullImage.setSmooth(true);
            fullImage.getStyleClass().add("document-image-fullscreen-image");

            Label hint = new Label(escapeHint == null || escapeHint.isBlank()
                    ? "Escape para cerrar imagen"
                    : escapeHint.strip());
            hint.getStyleClass().add("document-image-fullscreen-hint");
            hint.setWrapText(true);
            hint.setMaxWidth(720);

            StackPane root = new StackPane(fullImage, hint);
            root.getStyleClass().add("document-image-fullscreen-root");
            StackPane.setAlignment(hint, javafx.geometry.Pos.TOP_CENTER);
            fullImage.fitWidthProperty().bind(root.widthProperty().subtract(64));
            fullImage.fitHeightProperty().bind(root.heightProperty().subtract(112));

            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setTitle(title == null || title.isBlank() ? "Imagen completa" : title);
            Scene scene = new Scene(root, 960, 640);
            scene.getStylesheets().addAll(stylesheets == null ? List.of() : stylesheets);
            AtomicBoolean closeHandled = new AtomicBoolean(false);
            stage.setOnHidden(event -> {
                if (closeHandled.compareAndSet(false, true)) {
                    safeRun(afterClose, errorReporter, "No se pudo reanudar la lectura");
                }
            });
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    stage.close();
                }
            });
            stage.setScene(scene);
            stage.setFullScreenExitHint("");
            stage.fullScreenProperty().addListener((obs, wasFullScreen, fullScreen) -> {
                if (Boolean.TRUE.equals(wasFullScreen) && !Boolean.TRUE.equals(fullScreen)) {
                    stage.close();
                }
            });
            safeRun(beforeShow, errorReporter, "No se pudo pausar la lectura");
            stage.show();
            stage.setFullScreen(true);
        } catch (RuntimeException ex) {
            if (errorReporter != null) {
                String message = ex.getMessage() == null ? "" : ex.getMessage();
                String prefix = errorPrefix == null || errorPrefix.isBlank()
                        ? "No se pudo mostrar la imagen completa"
                        : errorPrefix;
                errorReporter.accept(prefix + ": " + message);
            }
        }
    }

    private static void safeRun(Runnable action, Consumer<String> errorReporter, String context) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException ex) {
            if (errorReporter != null) {
                String message = ex.getMessage() == null || ex.getMessage().isBlank()
                        ? ex.getClass().getSimpleName()
                        : ex.getMessage();
                errorReporter.accept(context + ": " + message);
            }
        }
    }
}
