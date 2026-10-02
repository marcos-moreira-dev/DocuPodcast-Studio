package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkImageCrop;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPlacedImage;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke;
import com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkSelectionGeometry;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCanvasToolbar;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions;
import com.marcosmoreiradev.docupodcaststudio.ink.InkRealtimeStrokeEngine;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.JavaFxMouseInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import com.marcosmoreiradev.docupodcaststudio.ink.InkEditorSession;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingExportProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.InkInputPolicy;
import com.marcosmoreiradev.docupodcaststudio.ink.ViewportMode;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasViewport;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasZoomPane;
import com.marcosmoreiradev.docupodcaststudio.ink.controls.InkPressureIndicator;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkImageFileStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reusable guided composition surface built on the product ink infrastructure. */
public final class InkCompositionWorkspace extends BorderPane implements AutoCloseable {
    private final InkCompositionProfile profile;
    private final InkCanvasSurface surface = new InkCanvasSurface();
    private final InkInputProvider inputProvider;
    private final StackPane centeredCanvas;
    private final InkCanvasZoomPane canvasZoomPane;
    private final ScrollPane canvasScroll;
    private final ToggleButton drawMode = StudioFormControls.toggle("Dibujar", "Dibujar sobre la ilustracion.");
    private final ToggleButton panMode = StudioFormControls.toggle(
            "Panear", "Mover la vista del lienzo sin modificar la ilustracion.");
    private final ToggleButton organizeMode = StudioFormControls.toggle(
            "Organizar imagenes", "Mover, cambiar de tamano o eliminar imagenes.");
    private final ToggleButton selectInkMode = StudioFormControls.toggle(
            "Seleccionar trazos", "Seleccionar, mover y transformar un trazo vectorial.");
    private final ToggleButton eraser = StudioFormControls.toggle("Borrador", "Borrar trazos del lapiz.");
    private final ColorPicker penColor = StudioFormControls.colorPicker(StudioFormControls.colorPicker(Color.BLACK), "Color del lapiz.");
    private final ColorPicker backgroundColor;
    private final Slider strokeWidth = StudioFormControls.slider(StudioFormControls.slider(1, 36, 7), "Grosor del trazo.");
    private final Slider zoom = StudioFormControls.slider(StudioFormControls.slider(0.20, 2.0, 1.0), "Zoom del lienzo.");
    private final Label zoomLabel = new Label("100%");
    private final Label selectionLabel = new Label("Selecciona una imagen para organizarla.");
    private final Label inkSelectionLabel = new Label("Selecciona un trazo para transformarlo.");
    private final Button shrinkImage;
    private final Button growImage;
    private final Button deleteImage;
    private Button shrinkStroke;
    private Button growStroke;
    private Button rotateStrokeLeft;
    private Button rotateStrokeRight;
    private Button copyStroke;
    private Button pasteStroke;
    private final Button undo;
    private final Button redo;
    private final List<PlacedImageItem> images = new ArrayList<>();
    private final List<Label> imageResizeHandles = new ArrayList<>();
    private final Rectangle strokeSelectionBounds = new Rectangle();
    private final Rectangle strokeSelectionHitArea = new Rectangle();
    private final Map<String, Path> stagedSources = new LinkedHashMap<>();
    private final InkCanvasViewport inkViewport;
    private final InkEditorSession<InkWorkspaceState> editorSession;
    private final InkPressureIndicator pressureIndicator;
    private InkRealtimeStrokeEngine inkEngine;
    private PlacedImageItem selectedImage;
    private int selectedStrokeIndex = -1;
    private List<InkStroke> strokeDragSnapshot = List.of();
    private Point2D strokeDragStart = Point2D.ZERO;
    private InkStroke strokeClipboard;
    private boolean strokeActive;
    private Point2D lastInkPoint = Point2D.ZERO;
    private boolean restoring;
    private boolean fittedInitially;
    private boolean inputAttached;
    private InkInputListener inputListener;
    private long coordinateStateEpoch;
    private final EventHandler<KeyEvent> workspaceShortcutHandler = this::handleWorkspaceShortcut;
    private Scene shortcutScene;

    public InkCompositionWorkspace(InkCompositionProfile profile, InkWorkspaceState initialState) {
        this(profile, initialState, new JavaFxMouseInputProvider());
    }

    public InkCompositionWorkspace(InkCompositionProfile profile,
                                   InkWorkspaceState initialState,
                                   InkInputProvider inputProvider) {
        this.profile = profile == null ? InkCompositionProfile.documentaryIllustration() : profile;
        this.inputProvider = inputProvider == null
                ? new JavaFxMouseInputProvider()
                : inputProvider;
        this.backgroundColor = StudioFormControls.colorPicker(
                StudioFormControls.colorPicker(this.profile.initialBackground()), "Color de fondo de la ilustracion.");
        surface.resetForFixedEditableState(this.profile.logicalWidth(), this.profile.logicalHeight(),
                this.profile.initialBackground());
        centeredCanvas = new StackPane(surface);
        canvasZoomPane = new InkCanvasZoomPane(centeredCanvas,
                () -> this.profile.logicalWidth(),
                () -> this.profile.logicalHeight());
        canvasScroll = canvasZoomPane.scrollPane();
        DrawingProfile sessionProfile = new DrawingProfile("composition", "Composicion", ViewportMode.FIXED,
                this.profile.logicalWidth(), this.profile.logicalHeight(), InkInputPolicy.MOUSE_AND_NATIVE,
                List.of(), this.profile.undoLimit(), true, new DrawingExportProfile(1, false, true));
        inkViewport = new InkCanvasViewport(surface, sessionProfile);
        editorSession = new InkEditorSession<>(sessionProfile, this.inputProvider, this::currentState, this::restore,
                (state, destination, exportProfile) -> destination);
        pressureIndicator = new InkPressureIndicator(editorSession.inputStatusProperty());
        configureCanvas();
        installImageResizeHandles();
        configureStrokeSelection();
        configureInk();

        ToggleGroup modes = new ToggleGroup();
        drawMode.setToggleGroup(modes);
        panMode.setToggleGroup(modes);
        organizeMode.setToggleGroup(modes);
        selectInkMode.setToggleGroup(modes);
        drawMode.setSelected(true);
        modes.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                drawMode.setSelected(true);
            }
            cancelTransientStrokeAndResetCoordinates();
            refreshInteractionMode();
        });

        Button addImage = ActionButtonFactory.primary("Agregar imagen", "Colocar una o varias imagenes en el lienzo.",
                () -> chooseImages(getScene() == null ? null : getScene().getWindow()));
        undo = ActionButtonFactory.secondary("Deshacer", this::undo);
        redo = ActionButtonFactory.secondary("Rehacer", this::redo);
        Button clear = ActionButtonFactory.secondary("Limpiar trazos", this::clearStrokes);
        Button fit = ActionButtonFactory.secondary("Ajustar", "Mostrar el lienzo completo y centrado.",
                () -> fitCanvas(canvasScroll.getViewportBounds().getWidth(),
                        canvasScroll.getViewportBounds().getHeight()));
        Button actualSize = ActionButtonFactory.secondary("100%", "Mostrar el lienzo a tamano real.",
                () -> zoom.setValue(1.0));
        shrinkImage = ActionButtonFactory.secondary("Reducir", () -> resizeSelected(0.85));
        growImage = ActionButtonFactory.secondary("Ampliar", () -> resizeSelected(1.15));
        deleteImage = ActionButtonFactory.danger("Eliminar imagen", this::deleteSelectedImage);

        StudioCanvasToolbar toolbar = new StudioCanvasToolbar("Herramientas de composición del lienzo");
        FlowPane firstRow = toolbar.addRow("Modo", addImage, drawMode, panMode, organizeMode, selectInkMode, undo, redo);
        FlowPane drawingRow = toolbar.addRow("Apariencia", new Label("Lapiz"), penColor,
                new Label("Fondo"), backgroundColor, new Label("Grosor"), strokeWidth,
                eraser, clear, new Label("Zoom"), zoom, zoomLabel, fit, actualSize, pressureIndicator);
        shrinkStroke = ActionButtonFactory.secondary("Reducir trazo", "Reducir el trazo seleccionado.", () -> transformSelectedStroke(0.9, 0.9, 0));
        growStroke = ActionButtonFactory.secondary("Ampliar trazo", "Ampliar el trazo seleccionado.", () -> transformSelectedStroke(1.1, 1.1, 0));
        rotateStrokeLeft = ActionButtonFactory.secondary("Girar izquierda", "Girar el trazo seleccionado 15 grados a la izquierda.", () -> transformSelectedStroke(1, 1, -15));
        rotateStrokeRight = ActionButtonFactory.secondary("Girar derecha", "Girar el trazo seleccionado 15 grados a la derecha.", () -> transformSelectedStroke(1, 1, 15));
        copyStroke = ActionButtonFactory.secondary("Copiar trazo", "Copiar el trazo seleccionado al portapapeles interno.", this::copySelectedStroke);
        pasteStroke = ActionButtonFactory.secondary("Pegar trazo", "Pegar el trazo copiado como una nueva capa vectorial.", this::pasteStroke);
        FlowPane imageRow = toolbar.addRow("Imagen seleccionada", selectionLabel, shrinkImage, growImage, deleteImage);
        FlowPane strokeRow = toolbar.addRow("Trazo seleccionado", inkSelectionLabel,
                shrinkStroke, growStroke, rotateStrokeLeft, rotateStrokeRight, copyStroke, pasteStroke);
        firstRow.getStyleClass().add("ink-composition-mode-row");
        drawingRow.getStyleClass().add("ink-composition-drawing-row");
        imageRow.getStyleClass().add("ink-composition-image-row");
        strokeRow.getStyleClass().add("ink-composition-stroke-row");
        toolbar.getStyleClass().add("ink-composition-toolbar");
        setTop(toolbar);
        setCenter(canvasZoomPane);
        getStyleClass().add("ink-composition-workspace");
        sceneProperty().addListener((observable, oldScene, newScene) -> installWorkspaceShortcuts(newScene));

        backgroundColor.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (restoring || newValue == null || newValue.equals(oldValue)) return;
            rememberUndo();
            surface.fillBackground(newValue);
        });
        zoom.valueProperty().addListener((observable, oldValue, newValue) -> applyZoom(newValue.doubleValue()));

        if (initialState != null) restore(initialState);
        refreshInteractionMode();
        refreshCommandState();
    }

    private void configureCanvas() {
        surface.setManaged(false);
        surface.relocate(0, 0);
        centeredCanvas.setAlignment(Pos.CENTER);
        centeredCanvas.getStyleClass().add("ink-composition-canvas-host");
        // Let ScrollPane expand the centering host when the scaled canvas is smaller than
        // the viewport. The host's computed minimum still forces scrollbars when it is larger.
        // Do not derive the host minimum from viewportBounds: that creates a feedback loop in
        // which scrollbar visibility changes the viewport and moves the complete composition.
        canvasScroll.setFitToWidth(true);
        canvasScroll.setFitToHeight(true);
        canvasScroll.setPannable(false);
        canvasScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        canvasScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        canvasScroll.setMinHeight(300);
        canvasScroll.setPrefViewportHeight(520);
        canvasScroll.getStyleClass().add("technical-problem-canvas-scroll");
        canvasZoomPane.setOnZoomApplied(appliedZoom -> {
            if (editorSession.closed()) return;
            cancelTransientStroke(false);
            editorSession.zoomTo(appliedZoom);
            zoomLabel.setText(Math.round(appliedZoom * 100) + "%");
            resetCoordinatesAfterCurrentInputEvent();
        });
        // Moving a cached parent forces JavaFX to rebuild the complete image layer on every pulse.
        // Keep each image as a stable scene-graph node, matching Problema Tecnico Express.
        surface.imageLayer().setCache(false);
        canvasScroll.viewportBoundsProperty().addListener((observable, oldValue, bounds) -> {
            if (!fittedInitially && bounds.getWidth() > 100 && bounds.getHeight() > 100) {
                fittedInitially = true;
                fitCanvas(bounds.getWidth(), bounds.getHeight());
            }
        });
    }

    private void configureInk() {
        inkEngine = new InkRealtimeStrokeEngine(new InkRealtimeStrokeEngine.Sink() {
            @Override public void beginLiveStroke() { surface.beginLiveStroke(); }
            @Override public void previewLine(double x1, double y1, double x2, double y2,
                                              Color color, double width, boolean erase) {
                surface.previewLine(x1, y1, x2, y2, color, width, erase);
            }
            @Override public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                                                   double endX, double endY, Color color, double width, boolean erase) {
                surface.previewQuadratic(startX, startY, controlX, controlY, endX, endY, color, width, erase);
            }
            @Override public void commitStroke(InkRealtimeStrokeEngine.CommittedStroke stroke) {
                surface.commitInkStroke(new InkCanvasSurface.InkStrokeState(
                        stroke.erase() ? "ERASE" : "DRAW", colorToHex(stroke.color()), stroke.width(),
                        stroke.points().stream().map(point -> new InkCanvasSurface.InkPointState(
                                point.x(), point.y(), point.nanos(), point.pressure())).toList()));
                surface.clearLiveStroke();
                refreshCommandState();
            }
        });
        inkEngine.start();
        attachInput();
    }

    /** Installs the non-destructive vector selection surface used by composition editors. */
    private void configureStrokeSelection() {
        strokeSelectionBounds.setManaged(false);
        strokeSelectionBounds.setMouseTransparent(true);
        strokeSelectionBounds.setFill(Color.TRANSPARENT);
        strokeSelectionBounds.getStyleClass().add("studio-canvas-selection-bounds");
        strokeSelectionHitArea.setManaged(false);
        strokeSelectionHitArea.setFill(Color.TRANSPARENT);
        strokeSelectionHitArea.setMouseTransparent(true);
        strokeSelectionHitArea.getStyleClass().add("studio-canvas-selection-hit-area");
        surface.inkInputLayer().getChildren().addAll(strokeSelectionHitArea, strokeSelectionBounds);
        strokeSelectionHitArea.setOnMousePressed(event -> {
            if (!selectInkMode.isSelected() || event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            Point2D point = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
            List<InkStroke> strokes = surface.applicationInkStrokes();
            selectedStrokeIndex = -1;
            for (int i = strokes.size() - 1; i >= 0; i--) {
                if (InkSelectionGeometry.hitTest(strokes.get(i), point.getX(), point.getY())) {
                    selectedStrokeIndex = i;
                    break;
                }
            }
            if (selectedStrokeIndex < 0) {
                strokeDragSnapshot = List.of();
                updateStrokeSelectionOverlay();
                event.consume();
                return;
            }
            strokeDragSnapshot = List.copyOf(strokes);
            strokeDragStart = point;
            rememberUndo();
            updateStrokeSelectionOverlay();
            event.consume();
        });
        strokeSelectionHitArea.setOnMouseDragged(event -> {
            if (!selectInkMode.isSelected() || selectedStrokeIndex < 0 || strokeDragSnapshot.isEmpty()) return;
            Point2D point = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
            double dx = point.getX() - strokeDragStart.getX();
            double dy = point.getY() - strokeDragStart.getY();
            List<InkStroke> transformed = new ArrayList<>(strokeDragSnapshot);
            transformed.set(selectedStrokeIndex, InkSelectionGeometry.transform(
                    strokeDragSnapshot.get(selectedStrokeIndex),
                    new InkSelectionGeometry.Transform(dx, dy, 1, 1, 0)));
            surface.replaceInkStrokeStates(toStrokeStates(transformed));
            updateStrokeSelectionOverlay();
            refreshCommandState();
            event.consume();
        });
        strokeSelectionHitArea.setOnMouseReleased(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                strokeDragSnapshot = List.of();
                event.consume();
            }
        });
    }

    private void transformSelectedStroke(double scaleX, double scaleY, double rotationDegrees) {
        List<InkStroke> strokes = surface.applicationInkStrokes();
        if (selectedStrokeIndex < 0 || selectedStrokeIndex >= strokes.size()) return;
        rememberUndo();
        List<InkStroke> transformed = new ArrayList<>(strokes);
        transformed.set(selectedStrokeIndex, InkSelectionGeometry.transform(strokes.get(selectedStrokeIndex),
                new InkSelectionGeometry.Transform(0, 0, scaleX, scaleY, rotationDegrees)));
        surface.replaceInkStrokeStates(toStrokeStates(transformed));
        updateStrokeSelectionOverlay();
        refreshCommandState();
    }

    private void copySelectedStroke() {
        List<InkStroke> strokes = surface.applicationInkStrokes();
        if (selectedStrokeIndex >= 0 && selectedStrokeIndex < strokes.size()) {
            strokeClipboard = strokes.get(selectedStrokeIndex);
            refreshCommandState();
        }
    }

    private void pasteStroke() {
        if (strokeClipboard == null) return;
        rememberUndo();
        List<InkStroke> strokes = new ArrayList<>(surface.applicationInkStrokes());
        strokes.add(InkSelectionGeometry.transform(strokeClipboard,
                new InkSelectionGeometry.Transform(24, 24, 1, 1, 0)));
        selectedStrokeIndex = strokes.size() - 1;
        surface.replaceInkStrokeStates(toStrokeStates(strokes));
        updateStrokeSelectionOverlay();
        refreshCommandState();
    }

    private List<InkCanvasSurface.InkStrokeState> toStrokeStates(List<InkStroke> strokes) {
        if (strokes == null) return List.of();
        return strokes.stream().map(stroke -> new InkCanvasSurface.InkStrokeState(
                stroke.tool().name(), stroke.color(), stroke.width(),
                stroke.points().stream().map(point -> new InkCanvasSurface.InkPointState(
                        point.x(), point.y(), point.nanos(), point.pressure())).toList())).toList();
    }

    private void updateStrokeSelectionOverlay() {
        boolean visible = selectInkMode.isSelected() && selectedStrokeIndex >= 0;
        strokeSelectionBounds.setVisible(visible);
        strokeSelectionBounds.setManaged(visible);
        if (!visible) {
            inkSelectionLabel.setText("Selecciona un trazo para transformarlo.");
            return;
        }
        List<InkStroke> strokes = surface.applicationInkStrokes();
        if (selectedStrokeIndex >= strokes.size()) {
            selectedStrokeIndex = -1;
            updateStrokeSelectionOverlay();
            return;
        }
        InkSelectionGeometry.Bounds bounds = InkSelectionGeometry.bounds(strokes.get(selectedStrokeIndex));
        strokeSelectionBounds.setX(bounds.minX());
        strokeSelectionBounds.setY(bounds.minY());
        strokeSelectionBounds.setWidth(Math.max(1, bounds.width()));
        strokeSelectionBounds.setHeight(Math.max(1, bounds.height()));
        strokeSelectionBounds.toFront();
        inkSelectionLabel.setText("Trazo seleccionado");
    }

    private void attachInput() {
        if (inputAttached) {
            return;
        }
        if (inputListener == null) {
            inputListener = new InkInputListener() {
            @Override public boolean onStrokeStart(InkInputSample sample) { return startStroke(sample); }
            @Override public boolean onStrokeMove(InkInputSample sample) { return moveStroke(sample); }
            @Override public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
                boolean consumed = false;
                for (InkInputSample sample : samples) consumed |= moveStroke(sample);
                return consumed;
            }
            @Override public boolean onStrokeEnd(InkInputSample sample) { return endStroke(sample); }
            };
        }
        editorSession.attach(inkViewport, inputListener);
        inputAttached = true;
    }

    private boolean startStroke(InkInputSample sample) {
        if (!drawMode.isSelected()) return false;
        Point2D point = pointInside(sample);
        if (point == null) return false;
        coordinateStateEpoch++;
        rememberUndo();
        strokeActive = true;
        lastInkPoint = point;
        inkEngine.begin(point.getX(), point.getY(), sample.nanos(), penColor.getValue(), strokeWidth.getValue(),
                eraser.isSelected() || sample.requestsEraser(), sample.pressure());
        return true;
    }

    private boolean moveStroke(InkInputSample sample) {
        if (!strokeActive || !drawMode.isSelected()) return false;
        Point2D point = pointInside(sample);
        if (point == null) return false;
        lastInkPoint = point;
        inkEngine.move(point.getX(), point.getY(), sample.nanos(), penColor.getValue(), strokeWidth.getValue(),
                eraser.isSelected() || sample.requestsEraser(), sample.pressure());
        return true;
    }

    private boolean endStroke(InkInputSample sample) {
        if (!strokeActive || !drawMode.isSelected()) return false;
        Point2D point = pointInside(sample);
        if (point == null) point = lastInkPoint;
        inkEngine.end(point.getX(), point.getY(), sample.nanos(), penColor.getValue(), strokeWidth.getValue(),
                eraser.isSelected() || sample.requestsEraser(), sample.pressure());
        strokeActive = false;
        lastInkPoint = Point2D.ZERO;
        resetCoordinatesAfterCurrentInputEvent();
        return true;
    }

    private void handleWorkspaceShortcut(KeyEvent event) {
        if (event == null || !event.isShortcutDown() || event.isAltDown() || textInputOwnsShortcut(event)) {
            return;
        }
        if (event.getCode() == KeyCode.Z) {
            if (event.isShiftDown()) {
                redo();
            } else {
                undo();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.Y) {
            redo();
            event.consume();
        } else if (selectInkMode.isSelected() && event.getCode() == KeyCode.C) {
            copySelectedStroke();
            event.consume();
        } else if (selectInkMode.isSelected() && event.getCode() == KeyCode.V) {
            pasteStroke();
            event.consume();
        }
    }

    private void installWorkspaceShortcuts(Scene scene) {
        if (shortcutScene == scene) return;
        if (shortcutScene != null) {
            shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, workspaceShortcutHandler);
        }
        shortcutScene = scene;
        if (shortcutScene != null) {
            shortcutScene.addEventFilter(KeyEvent.KEY_PRESSED, workspaceShortcutHandler);
        }
    }

    private boolean textInputOwnsShortcut(KeyEvent event) {
        if (event.getTarget() instanceof Node target && isInsideTextInput(target)) {
            return true;
        }
        return getScene() != null && isInsideTextInput(getScene().getFocusOwner());
    }

    private static boolean isInsideTextInput(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof TextInputControl) return true;
            current = current.getParent();
        }
        return false;
    }

    private Point2D pointInside(InkInputSample sample) {
        if (sample == null) return null;
        return sample.x() >= 0 && sample.y() >= 0
                && sample.x() <= surface.logicalWidth() && sample.y() <= surface.logicalHeight()
                ? new Point2D(sample.x(), sample.y()) : null;
    }

    public void chooseImages(Window owner) {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Agregar imagenes a la ilustracion");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagenes", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp"));
        List<java.io.File> selected = chooser.showOpenMultipleDialog(owner);
        if (selected == null || selected.isEmpty()) return;
        rememberUndo();
        for (java.io.File file : selected) {
            try {
                Path staged = stageSource(file.toPath());
                Image image = new Image(staged.toUri().toString(), false);
                if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    Files.deleteIfExists(staged);
                    continue;
                }
                addImage(image, staged, 42 + images.size() * 28.0, 36 + images.size() * 20.0,
                        Math.min(620, profile.logicalWidth() * 0.42), null);
            } catch (IOException ignored) {
                // The caller can keep working with successfully imported images.
            }
        }
        organizeMode.setSelected(true);
        refreshCommandState();
    }

    private Path stageSource(Path source) throws IOException {
        String name = source.getFileName() == null ? "image" : source.getFileName().toString();
        String extension = name.lastIndexOf('.') >= 0 ? name.substring(name.lastIndexOf('.')) : ".png";
        Path staged = Files.createTempFile("docupodcast-illustration-source-", extension);
        Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING);
        return staged;
    }

    private PlacedImageItem addImage(Image image, Path staged, double x, double y, double requestedWidth,
                                     InkPlacedImage restored) {
        String id = restored == null || restored.id().isBlank()
                ? "IMG-" + UUID.randomUUID().toString().replace("-", "") : restored.id();
        ImageView view = new ImageView(image);
        view.setManaged(false);
        view.setPreserveRatio(true);
        view.setCache(false);
        view.getStyleClass().add("technical-problem-canvas-image");
        double heightLimitedWidth = Math.max(40, (profile.logicalHeight() - Math.max(0, y) - 18)
                * image.getWidth() / Math.max(1.0, image.getHeight()));
        view.setFitWidth(Math.max(40, Math.min(requestedWidth, heightLimitedWidth)));
        view.setLayoutX(clamp(x, 0, profile.logicalWidth() - view.getFitWidth()));
        view.setLayoutY(clamp(y, 0, profile.logicalHeight() - estimatedHeight(image, view.getFitWidth())));
        view.setCursor(Cursor.MOVE);
        String inlineImageData = restored != null
                && restored.inlineImageData() != null
                && !restored.inlineImageData().isBlank()
                ? restored.inlineImageData()
                : imageToBase64(image);
        PlacedImageItem item = new PlacedImageItem(id, view, image, staged, inlineImageData);
        configureImageInteraction(item);
        images.add(item);
        surface.imageLayer().getChildren().add(view);
        if (staged != null) stagedSources.put(id, staged);
        selectImage(item);
        refreshInteractionMode();
        return item;
    }

    private void configureImageInteraction(PlacedImageItem item) {
        item.view().setOnMousePressed(event -> {
            if (!organizeMode.isSelected()) return;
            rememberUndo();
            selectImage(item);
            Point2D pointer = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
            item.dragPointerX = pointer.getX();
            item.dragPointerY = pointer.getY();
            item.dragLayoutX = item.view().getLayoutX();
            item.dragLayoutY = item.view().getLayoutY();
            event.consume();
        });
        item.view().setOnMouseDragged(event -> {
            if (!organizeMode.isSelected() || selectedImage != item) return;
            Point2D pointer = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
            double x = item.dragLayoutX + pointer.getX() - item.dragPointerX;
            double y = item.dragLayoutY + pointer.getY() - item.dragPointerY;
            item.view().setLayoutX(clamp(x, 0, profile.logicalWidth() - item.view().getFitWidth()));
            item.view().setLayoutY(clamp(y, 0, profile.logicalHeight() - item.height()));
            updateImageResizeHandles();
            event.consume();
        });
        item.view().setOnMouseReleased(event -> {
            if (!organizeMode.isSelected() || selectedImage != item) return;
            updateImageResizeHandles();
            event.consume();
        });
    }

    private void selectImage(PlacedImageItem item) {
        if (selectedImage != null) {
            selectedImage.view().getStyleClass().remove("technical-problem-canvas-image-selected");
        }
        selectedImage = item;
        if (item != null) {
            if (!item.view().getStyleClass().contains("technical-problem-canvas-image-selected")) {
                item.view().getStyleClass().add("technical-problem-canvas-image-selected");
            }
            selectionLabel.setText("Imagen seleccionada");
        } else {
            selectionLabel.setText("Selecciona una imagen para organizarla.");
        }
        updateImageResizeHandles();
        refreshCommandState();
    }

    private void resizeSelected(double factor) {
        if (selectedImage == null) return;
        rememberUndo();
        ImageView view = selectedImage.view();
        double maxWidth = profile.logicalWidth() - view.getLayoutX();
        double maxByHeight = (profile.logicalHeight() - view.getLayoutY())
                * selectedImage.image().getWidth() / Math.max(1.0, selectedImage.image().getHeight());
        view.setFitWidth(clamp(view.getFitWidth() * factor, 50, Math.max(50, Math.min(maxWidth, maxByHeight))));
        updateImageResizeHandles();
        refreshCommandState();
    }

    private void installImageResizeHandles() {
        if (!imageResizeHandles.isEmpty()) return;
        for (String arrow : List.of("\u2196", "\u2197", "\u2199", "\u2198")) {
            Label handle = new Label(arrow);
            handle.setManaged(false);
            handle.setCursor(Cursor.SE_RESIZE);
            handle.setVisible(false);
            handle.getStyleClass().add("technical-problem-image-resize-handle");
            StudioFormControls.installTooltip(handle,
                    "Arrastra para redimensionar la imagen manteniendo proporcion.");
            handle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (selectedImage == null || !organizeMode.isSelected()) return;
                rememberUndo();
                event.consume();
            });
            handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
                if (selectedImage == null || !organizeMode.isSelected()) return;
                resizeSelectedImageFromHandle(event.getSceneX(), event.getSceneY());
                event.consume();
            });
            imageResizeHandles.add(handle);
        }
        surface.inkInputLayer().getChildren().addAll(imageResizeHandles);
    }

    private void resizeSelectedImageFromHandle(double sceneX, double sceneY) {
        if (selectedImage == null) return;
        ImageView view = selectedImage.view();
        Bounds bounds = view.getBoundsInParent();
        double centerX = bounds.getMinX() + bounds.getWidth() / 2.0;
        double centerY = bounds.getMinY() + bounds.getHeight() / 2.0;
        Point2D pointer = surface.sceneToLocal(sceneX, sceneY);
        double ratio = selectedImage.image().getHeight()
                / Math.max(1.0, selectedImage.image().getWidth());
        double maxWidth = Math.min(profile.logicalWidth(), profile.logicalHeight() / Math.max(0.01, ratio));
        double nextWidth = clamp(Math.abs(pointer.getX() - centerX) * 2.0, 48.0, maxWidth);
        double nextHeight = nextWidth * ratio;
        double nextX = clamp(centerX - nextWidth / 2.0, 0, profile.logicalWidth() - nextWidth);
        double nextY = clamp(centerY - nextHeight / 2.0, 0, profile.logicalHeight() - nextHeight);
        view.setFitWidth(nextWidth);
        view.setLayoutX(nextX);
        view.setLayoutY(nextY);
        updateImageResizeHandles();
        refreshCommandState();
    }

    private void updateImageResizeHandles() {
        boolean visible = organizeMode.isSelected() && selectedImage != null;
        for (Label handle : imageResizeHandles) {
            handle.setVisible(visible);
            handle.setMouseTransparent(!visible);
        }
        if (!visible) return;
        Bounds bounds = selectedImage.view().getBoundsInParent();
        double size = 18.0;
        double[][] positions = {
                {bounds.getMinX() - size / 2.0, bounds.getMinY() - size / 2.0},
                {bounds.getMaxX() - size / 2.0, bounds.getMinY() - size / 2.0},
                {bounds.getMinX() - size / 2.0, bounds.getMaxY() - size / 2.0},
                {bounds.getMaxX() - size / 2.0, bounds.getMaxY() - size / 2.0}
        };
        for (int i = 0; i < imageResizeHandles.size(); i++) {
            Label handle = imageResizeHandles.get(i);
            handle.resizeRelocate(positions[i][0], positions[i][1], size, size);
            handle.toFront();
        }
    }

    private void deleteSelectedImage() {
        if (selectedImage == null) return;
        rememberUndo();
        surface.imageLayer().getChildren().remove(selectedImage.view());
        images.remove(selectedImage);
        selectImage(null);
    }

    private void clearStrokes() {
        cancelTransientStrokeAndResetCoordinates();
        if (surface.applicationInkStrokes().isEmpty()) return;
        rememberUndo();
        surface.clearStrokes();
        selectedStrokeIndex = -1;
        strokeDragSnapshot = List.of();
        updateStrokeSelectionOverlay();
        refreshCommandState();
    }

    private void rememberUndo() {
        if (restoring) return;
        editorSession.checkpoint();
        refreshCommandState();
    }

    private void undo() {
        cancelTransientStrokeAndResetCoordinates();
        editorSession.undo();
        refreshCommandState();
    }

    private void redo() {
        cancelTransientStrokeAndResetCoordinates();
        editorSession.redo();
        refreshCommandState();
    }

    private void restore(InkWorkspaceState state) {
        cancelTransientStrokeAndResetCoordinates();
        selectedStrokeIndex = -1;
        strokeDragSnapshot = List.of();
        restoring = true;
        try {
            Color background = parseColor(state.background(), profile.initialBackground());
            surface.resetForFixedEditableState(profile.logicalWidth(), profile.logicalHeight(), background);
            backgroundColor.setValue(background);
            surface.restoreApplicationInkStrokes(state.strokes());
            surface.imageLayer().getChildren().clear();
            images.clear();
            selectImage(null);
            for (InkPlacedImage placed : state.images()) {
                Image image = imageFromBase64(placed.inlineImageData());
                if (image == null || image.isError()) continue;
                PlacedImageItem item = addImage(image, stagedSources.get(placed.id()), placed.x(), placed.y(),
                        placed.fitWidth(), placed);
                item.view().setFitWidth(placed.fitWidth());
            }
            selectImage(null);
            updateStrokeSelectionOverlay();
        } finally {
            restoring = false;
        }
    }

    private InkWorkspaceState currentState() {
        if (inkEngine != null) {
            inkEngine.flushAll();
            if (strokeActive) {
                strokeActive = false;
                lastInkPoint = Point2D.ZERO;
                resetCoordinatesAfterCurrentInputEvent();
            }
        }
        List<InkPlacedImage> placed = images.stream().map(item -> new InkPlacedImage(
                item.id(), "", "", item.inlineImageData(), item.inlineImageData(),
                item.view().getLayoutX(), item.view().getLayoutY(), item.view().getFitWidth(), item.height(),
                item.view().getLayoutX(), item.view().getLayoutY(), item.view().getFitWidth(), InkImageCrop.none()
        )).toList();
        return InkWorkspaceState.create(profile.logicalWidth(), profile.logicalHeight(),
                colorToHex(surface.backgroundColor()), surface.applicationInkStrokes(), placed,
                Map.of("consumer", "documentary.paragraph-illustration", "aspectRatio",
                        Math.round(profile.logicalWidth()) + ":" + Math.round(profile.logicalHeight())));
    }

    public InkCompositionResult result() {
        InkWorkspaceState state = currentState();
        WritableImage image = surface.exportWithImages(images.stream().map(PlacedImageItem::view).toList(),
                new InkCanvasExportOptions(1, 24_000_000L, false, true, 0,
                        profile.logicalWidth(), profile.logicalHeight())).image();
        return new InkCompositionResult(image, state, stagedSources);
    }

    /** Rebinds native input after the containing window has a final HWND and geometry. */
    public void activateInput() {
        cancelTransientStroke(false);
        refreshInteractionMode();
        applyCss();
        layout();
        surface.applyCss();
        surface.layout();
        if (inputAttached) {
            editorSession.detach();
            inputAttached = false;
        }
        attachInput();
        coordinateStateEpoch++;
        editorSession.resetInputCoordinates();
        surface.inkInputTarget().requestFocus();
    }

    Node inputTargetForTesting() {
        return surface.inkInputTarget();
    }

    InkInputCapabilities inputCapabilitiesForTesting() {
        return editorSession.inputCapabilities();
    }

    private void refreshInteractionMode() {
        boolean arranging = organizeMode.isSelected();
        boolean panning = panMode.isSelected();
        boolean inkActive = drawMode.isSelected();
        boolean selecting = selectInkMode.isSelected();
        surface.configureInputCapture(inkActive || arranging || selecting, inkActive);
        surface.inkInputTarget().setCursor(inkActive ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        strokeSelectionHitArea.setWidth(surface.logicalWidth());
        strokeSelectionHitArea.setHeight(surface.logicalHeight());
        strokeSelectionHitArea.setMouseTransparent(!selecting);
        strokeSelectionHitArea.setVisible(selecting);
        centeredCanvas.setCursor(panning ? Cursor.OPEN_HAND : Cursor.DEFAULT);
        canvasScroll.setPannable(panning);
        for (PlacedImageItem item : images) item.view().setMouseTransparent(!arranging);
        if (!arranging) selectImage(null);
        if (!selecting) {
            selectedStrokeIndex = -1;
            strokeDragSnapshot = List.of();
        }
        shrinkImage.setDisable(!arranging || selectedImage == null);
        growImage.setDisable(!arranging || selectedImage == null);
        deleteImage.setDisable(!arranging || selectedImage == null);
        updateImageResizeHandles();
        updateStrokeSelectionOverlay();
    }

    private void refreshCommandState() {
        undo.setDisable(!editorSession.canUndo());
        redo.setDisable(!editorSession.canRedo());
        boolean disabled = !organizeMode.isSelected() || selectedImage == null;
        shrinkImage.setDisable(disabled);
        growImage.setDisable(disabled);
        deleteImage.setDisable(disabled);
        boolean strokeDisabled = !selectInkMode.isSelected() || selectedStrokeIndex < 0;
        if (shrinkStroke != null) shrinkStroke.setDisable(strokeDisabled);
        if (growStroke != null) growStroke.setDisable(strokeDisabled);
        if (rotateStrokeLeft != null) rotateStrokeLeft.setDisable(strokeDisabled);
        if (rotateStrokeRight != null) rotateStrokeRight.setDisable(strokeDisabled);
        if (copyStroke != null) copyStroke.setDisable(strokeDisabled);
        if (pasteStroke != null) pasteStroke.setDisable(strokeClipboard == null);
    }

    private void fitCanvas(double viewportWidth, double viewportHeight) {
        double fitted = Math.min((viewportWidth - 42) / profile.logicalWidth(),
                (viewportHeight - 42) / profile.logicalHeight());
        zoom.setValue(clamp(fitted, zoom.getMin(), 1.0));
        canvasZoomPane.centerContent();
    }

    private void applyZoom(double value) {
        double safe = clamp(value, zoom.getMin(), zoom.getMax());
        zoomLabel.setText(Math.round(safe * 100) + "%");
        canvasZoomPane.setZoom(safe);
    }

    private void cancelTransientStrokeAndResetCoordinates() {
        cancelTransientStroke(false);
        if (inputAttached) {
            coordinateStateEpoch++;
            editorSession.resetInputCoordinates();
        }
    }

    private void cancelTransientStroke(boolean preserveCoordinateReset) {
        if (!preserveCoordinateReset) coordinateStateEpoch++;
        strokeActive = false;
        lastInkPoint = Point2D.ZERO;
        if (inkEngine != null) inkEngine.cancelActiveStroke();
        surface.clearLiveStroke();
    }

    private void resetCoordinatesAfterCurrentInputEvent() {
        long requestedEpoch = ++coordinateStateEpoch;
        Platform.runLater(() -> {
            if (requestedEpoch != coordinateStateEpoch || strokeActive || !inputAttached) return;
            editorSession.resetInputCoordinates();
        });
    }

    void setZoomForTesting(double value) {
        zoom.setValue(value);
    }

    Pane zoomHostForTesting() {
        return canvasZoomPane.contentHost();
    }

    StackPane centeredCanvasForTesting() {
        return centeredCanvas;
    }

    ScrollPane canvasScrollForTesting() {
        return canvasScroll;
    }

    void selectDrawModeForTesting() {
        drawMode.setSelected(true);
    }

    void selectPanModeForTesting() {
        panMode.setSelected(true);
    }

    void selectOrganizeModeForTesting() {
        organizeMode.setSelected(true);
    }

    ImageView addImageForTesting(Image image, double x, double y, double width) {
        return addImage(image, null, x, y, width, null).view();
    }

    Pane imageLayerForTesting() {
        return surface.imageLayer();
    }

    List<Label> imageResizeHandlesForTesting() {
        return List.copyOf(imageResizeHandles);
    }

    public void cleanupStaging() {
        for (Path path : stagedSources.values()) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) { }
        }
        stagedSources.clear();
    }

    @Override public void close() {
        installWorkspaceShortcuts(null);
        if (inkEngine != null) {
            inkEngine.cancelActiveStroke();
            inkEngine.stop();
        }
        pressureIndicator.close();
        editorSession.close();
        inputAttached = false;
    }

    private static double estimatedHeight(Image image, double width) {
        return Math.max(1.0, width * image.getHeight() / Math.max(1.0, image.getWidth()));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(Math.max(min, max), value));
    }

    private static Color parseColor(String value, Color fallback) {
        try { return Color.web(value); } catch (RuntimeException ex) { return fallback; }
    }

    private static String colorToHex(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return String.format("#%02x%02x%02x%02x", Math.round(safe.getRed() * 255),
                Math.round(safe.getGreen() * 255), Math.round(safe.getBlue() * 255),
                Math.round(safe.getOpacity() * 255));
    }

    private static String imageToBase64(Image image) {
        if (image == null || image.getPixelReader() == null) return "";
        try {
            return InkImageFileStore.encodePngBase64(image);
        } catch (IOException ex) {
            return "";
        }
    }

    private static Image imageFromBase64(String data) {
        return InkImageFileStore.decodePngBase64(data);
    }

    private static final class PlacedImageItem {
        private final String id;
        private final ImageView view;
        private final Image image;
        private final Path stagedSource;
        private final String inlineImageData;
        private double dragPointerX;
        private double dragPointerY;
        private double dragLayoutX;
        private double dragLayoutY;

        private PlacedImageItem(String id, ImageView view, Image image, Path stagedSource,
                                String inlineImageData) {
            this.id = id;
            this.view = view;
            this.image = image;
            this.stagedSource = stagedSource;
            this.inlineImageData = inlineImageData == null ? "" : inlineImageData;
        }

        String id() { return id; }
        ImageView view() { return view; }
        Image image() { return image; }
        String inlineImageData() { return inlineImageData; }
        double height() { return estimatedHeight(image, view.getFitWidth()); }
    }
}
