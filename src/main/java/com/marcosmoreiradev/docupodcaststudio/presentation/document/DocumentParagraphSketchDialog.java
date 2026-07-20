package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleSection;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.InkCompositionProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.InkCompositionResult;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.InkCompositionWorkspace;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Guided documentary illustration editor for one Word paragraph. */
public final class DocumentParagraphSketchDialog extends Dialog<DocumentParagraphSketchDialog.Result> {
    private final ButtonType saveType = new ButtonType("Guardar ilustracion", ButtonBar.ButtonData.OK_DONE);
    private final InkCompositionWorkspace workspace;
    private boolean saved;

    public DocumentParagraphSketchDialog(Window owner, String paragraphText, Path existingState,
                                         DrawingProfile drawingProfile) {
        initOwner(owner);
        setTitle("Ilustrar parrafo");
        setResizable(true);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        workspace = new InkCompositionWorkspace(
                InkCompositionProfile.documentaryIllustration(drawingProfile), readState(existingState));
        Label reference = new Label(paragraphText == null ? "" : paragraphText);
        reference.setWrapText(true);
        reference.setMaxWidth(Double.MAX_VALUE);
        reference.getStyleClass().add("document-study-video-paragraph-preview");
        CollapsibleSection referencePane = new CollapsibleSection("Texto de referencia", reference, false);
        referencePane.setPadding(new Insets(0, 0, 6, 0));

        BorderPane root = new BorderPane(workspace);
        root.setTop(referencePane);
        root.setMinSize(820, 520);
        root.setPrefSize(1460, 720);
        getDialogPane().setContent(root);
        getDialogPane().setMinSize(860, 580);
        getDialogPane().setPrefSize(1500, 790);
        setResultConverter(button -> null);
        setOnShown(event -> configureWindow(owner));
        setOnCloseRequest(event -> {
            if (!saved) workspace.cleanupStaging();
        });
        setOnHidden(event -> workspace.close());
    }

    private void configureWindow(Window owner) {
        Window window = getDialogPane().getScene() == null ? null : getDialogPane().getScene().getWindow();
        if (window != null) {
            Rectangle2D bounds = screenBounds(owner);
            double targetWidth = Math.min(1540, Math.max(900, bounds.getWidth() - 28));
            double targetHeight = Math.min(880, Math.max(620, bounds.getHeight() - 28));
            window.setWidth(targetWidth);
            window.setHeight(targetHeight);
            window.setX(bounds.getMinX() + Math.max(0, (bounds.getWidth() - targetWidth) / 2));
            window.setY(bounds.getMinY() + Math.max(0, (bounds.getHeight() - targetHeight) / 2));
        }
        Button save = (Button) getDialogPane().lookupButton(saveType);
        Tooltip.install(save, new Tooltip("Guardar el PNG y su estado editable dentro del proyecto."));
        save.setOnAction(event -> {
            event.consume();
            try {
                setResult(saveResult());
                saved = true;
                close();
            } catch (RuntimeException ex) {
                showError(ex.getMessage());
            }
        });
        Button cancel = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancel.setOnAction(event -> {
            event.consume();
            workspace.cleanupStaging();
            setResult(null);
            close();
        });
        Platform.runLater(workspace::activateInput);
    }

    private Result saveResult() {
        try {
            InkCompositionResult composition = workspace.result();
            Path png = Files.createTempFile("docupodcast-documentary-illustration-", ".png");
            writePng(composition.image(), png);
            return new Result(png, InkWorkspaceStateSerializer.toJson(composition.state()),
                    composition.stagedSources());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la ilustracion: " + ex.getMessage(), ex);
        }
    }

    private static InkWorkspaceState readState(Path stateFile) {
        if (stateFile == null || !Files.isRegularFile(stateFile)) return null;
        try {
            return InkWorkspaceStateSerializer.fromJson(Files.readString(stateFile, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(getOwner());
        alert.setTitle("Ilustrar parrafo");
        alert.setHeaderText("No se pudo guardar la ilustracion");
        alert.setContentText(message == null ? "Error de guardado." : message);
        alert.showAndWait();
    }

    private static Rectangle2D screenBounds(Window owner) {
        if (owner != null) {
            return Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
                    .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private static void writePng(WritableImage image, Path target) throws IOException {
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        PixelReader reader = image.getPixelReader();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) output.setRGB(x, y, reader.getArgb(x, y));
        }
        if (!ImageIO.write(output, "png", target.toFile())) {
            throw new IOException("No se pudo escribir el PNG de la ilustracion.");
        }
    }

    public record Result(Path png, String inkStateJson, Map<String, Path> stagedSources) {
        public Result {
            stagedSources = stagedSources == null ? Map.of() : Map.copyOf(stagedSources);
        }
    }
}
