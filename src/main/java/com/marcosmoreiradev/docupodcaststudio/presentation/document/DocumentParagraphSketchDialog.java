package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas.InkCanvasSurface;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Styled ink editor for one documentary paragraph image. */
public final class DocumentParagraphSketchDialog extends Dialog<DocumentParagraphSketchDialog.Result> {
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;
    private final InkCanvasSurface surface = new InkCanvasSurface();
    private final Slider width = new Slider(1, 30, 7);
    private final ToggleButton eraser = StudioFormControls.toggle("Borrador", "Alternar entre lapiz y borrador.");
    private final List<InkCanvasSurface.InkPointState> points = new ArrayList<>();
    private final ButtonType saveType = new ButtonType("Guardar dibujo", ButtonBar.ButtonData.OK_DONE);

    public DocumentParagraphSketchDialog(Window owner, String paragraphText, Path existingState) {
        initOwner(owner);
        setTitle("Dibujar imagen del parrafo");
        setResizable(true);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        surface.resetForFixedEditableState(WIDTH, HEIGHT, Color.WHITE);
        restore(existingState);
        configurePointer();
        StudioFormControls.slider(width, "Grosor del trazo.");
        width.setPrefWidth(180);

        Button clear = ActionButtonFactory.secondary("Limpiar", surface::clearStrokes);
        Label preview = new Label(paragraphText == null ? "" : paragraphText);
        preview.setWrapText(true);
        preview.setMaxWidth(Double.MAX_VALUE);
        preview.getStyleClass().add("document-study-video-paragraph-preview");

        HBox toolbar = new HBox(10, new Label("Grosor"), width, eraser, clear);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8));
        StackPane canvasHost = new StackPane(surface);
        canvasHost.setAlignment(Pos.CENTER);
        canvasHost.getStyleClass().add("document-study-video-sketch-host");
        ScrollPane canvasScroll = new ScrollPane(canvasHost);
        canvasScroll.setPannable(false);
        canvasScroll.setFitToWidth(false);
        canvasScroll.setFitToHeight(false);
        canvasScroll.setMinHeight(360);
        canvasScroll.setPrefViewportWidth(1120);
        canvasScroll.setPrefViewportHeight(480);
        canvasScroll.getStyleClass().add("technical-problem-canvas-scroll");
        canvasHost.minWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(canvasScroll.getViewportBounds().getWidth(), WIDTH + 36.0),
                canvasScroll.viewportBoundsProperty()));
        canvasHost.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(canvasScroll.getViewportBounds().getHeight(), HEIGHT + 36.0),
                canvasScroll.viewportBoundsProperty()));
        canvasHost.prefWidthProperty().bind(canvasHost.minWidthProperty());
        canvasHost.prefHeightProperty().bind(canvasHost.minHeightProperty());

        VBox header = new VBox(4, toolbar, preview);
        BorderPane root = new BorderPane(canvasScroll, header, null, null, null);
        root.setMinSize(760, 500);
        root.setPrefSize(1200, 590);
        getDialogPane().setContent(root);
        getDialogPane().setMinSize(820, 570);
        getDialogPane().setPrefSize(1240, 650);
        setResultConverter(button -> button == saveType ? saveResult() : null);
        setOnShown(event -> Platform.runLater(() -> {
            if (getDialogPane().getScene() != null) {
                Window window = getDialogPane().getScene().getWindow();
                Rectangle2D bounds = screenBounds(owner);
                double targetWidth = Math.min(1240.0, Math.max(760.0, bounds.getWidth() - 32.0));
                double targetHeight = Math.min(680.0, Math.max(560.0, bounds.getHeight() - 32.0));
                window.setWidth(targetWidth);
                window.setHeight(targetHeight);
                window.setX(bounds.getMinX() + Math.max(0.0, (bounds.getWidth() - targetWidth) / 2.0));
                window.setY(bounds.getMinY() + Math.max(0.0, (bounds.getHeight() - targetHeight) / 2.0));
            }
        }));
        Button save = (Button) getDialogPane().lookupButton(saveType);
        Tooltip.install(save, new Tooltip("Guardar PNG y estado editable dentro del proyecto."));
    }

    private void configurePointer() {
        surface.inkInputLayer().setMouseTransparent(false);
        surface.inkInputLayer().setPickOnBounds(true);
        surface.inkInputTarget().setMouseTransparent(false);
        surface.inkInputTarget().setVisible(true);
        surface.inkInputTarget().setDisable(false);
        surface.inkInputTarget().setCursor(Cursor.CROSSHAIR);
        surface.inkInputTarget().addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            points.clear();
            points.add(point(event));
            surface.beginLiveStroke();
            event.consume();
        });
        surface.inkInputTarget().addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            InkCanvasSurface.InkPointState next = point(event);
            if (!points.isEmpty()) {
                InkCanvasSurface.InkPointState previous = points.get(points.size() - 1);
                surface.previewLine(previous.x(), previous.y(), next.x(), next.y(), Color.BLACK,
                        width.getValue(), eraser.isSelected());
            }
            points.add(next);
            event.consume();
        });
        surface.inkInputTarget().addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (points.size() == 1) points.add(point(event));
            surface.commitInkStroke(new InkCanvasSurface.InkStrokeState(
                    eraser.isSelected() ? "ERASE" : "DRAW",
                    eraser.isSelected() ? "#00000000" : "#000000ff",
                    width.getValue(), List.copyOf(points)));
            points.clear();
            event.consume();
        });
    }

    private static Rectangle2D screenBounds(Window owner) {
        if (owner != null) {
            return Screen.getScreensForRectangle(
                            owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
                    .stream()
                    .findFirst()
                    .orElse(Screen.getPrimary())
                    .getVisualBounds();
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private InkCanvasSurface.InkPointState point(MouseEvent event) {
        return new InkCanvasSurface.InkPointState(event.getX(), event.getY(), System.nanoTime());
    }

    private void restore(Path stateFile) {
        if (stateFile == null || !Files.isRegularFile(stateFile)) return;
        try {
            InkWorkspaceState state = InkWorkspaceStateSerializer.fromJson(
                    Files.readString(stateFile, StandardCharsets.UTF_8));
            surface.restoreApplicationInkStrokes(state.strokes());
        } catch (Exception ignored) {
            // A corrupt sidecar must not prevent replacing the drawing.
        }
    }

    private Result saveResult() {
        try {
            Path png = Files.createTempFile("docupodcast-documentary-drawing-", ".png");
            writePng(surface.snapshotDrawing(), png);
            String state = InkWorkspaceStateSerializer.toJson(InkWorkspaceState.create(
                    surface.logicalWidth(), surface.logicalHeight(), "#ffffffff",
                    surface.applicationInkStrokes(), List.of(), Map.of("consumer", "documentary.paragraph")));
            return new Result(png, state);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el dibujo: " + ex.getMessage(), ex);
        }
    }

    private static void writePng(WritableImage image, Path target) throws IOException {
        int imageWidth = Math.max(1, (int) Math.ceil(image.getWidth()));
        int imageHeight = Math.max(1, (int) Math.ceil(image.getHeight()));
        PixelReader reader = image.getPixelReader();
        BufferedImage output = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                output.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        if (!ImageIO.write(output, "png", target.toFile())) {
            throw new IOException("No se pudo escribir el PNG del dibujo.");
        }
    }

    public record Result(Path png, String inkStateJson) { }
}
