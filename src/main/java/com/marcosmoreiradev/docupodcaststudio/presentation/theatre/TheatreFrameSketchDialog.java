package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.LucideIconView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.StudyProblemCanvasExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.InkRealtimeStrokeEngine;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas.InkCanvasSurface;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas.InkCanvasViewportCoordinateMapper;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputProviderFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputSample;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Drawn-frame editor reused by theatre storyboard fragments. */
public final class TheatreFrameSketchDialog extends Dialog<TheatreFrameSketchDialog.Result> {
    private static final int MAX_UNDO = 50;
    private static final double FRAME_WIDTH = 1280;
    private static final double FRAME_HEIGHT = 720;
    private static final int EXPORT_SCALE = 2;
    private static final String METADATA_SHOW_FRAGMENT_TITLE = "showFragmentTitle";
    private static final String METADATA_FRAGMENT_TITLE_TEXT = "fragmentTitleText";
    private static final String METADATA_TITLE_BAND_HEIGHT = "fragmentTitleBandHeight";
    private static final String METADATA_TITLE_MARGIN = "fragmentTitleMargin";
    private static final double TITLE_MARGIN = 18.0;
    private static final double TITLE_FONT_SIZE = 16.0;
    private static final double TITLE_LINE_HEIGHT = 22.0;
    private static final double TITLE_MIN_BAND_HEIGHT = 58.0;
    private static final double TITLE_MAX_BAND_HEIGHT = 140.0;

    private final TheatreFrameSketchContext context;
    private final Runnable historyBoardAction;
    private final InkWorkspaceState restoredInkState;
    private final InkCanvasSurface surface = new InkCanvasSurface();
    private final InkInputProvider inputProvider = InkInputProviderFactory.createLectureStudioOnly();
    private final ArrayDeque<List<InkCanvasSurface.InkStrokeState>> undo = new ArrayDeque<>();
    private final ArrayDeque<List<InkCanvasSurface.InkStrokeState>> redo = new ArrayDeque<>();
    private final ColorPicker penColor = new ColorPicker(Color.BLACK);
    private final ColorPicker backgroundColor = new ColorPicker(Color.WHITE);
    private final Slider strokeWidth = new Slider(1, 48, 6);
    private final Slider canvasZoom = new Slider(25, 200, 100);
    private final Slider cameraGuideOpacity = new Slider(0, 45, 18);
    private final Label canvasZoomValue = new Label("100%");
    private final Label cameraGuideOpacityValue = new Label("18%");
    private final Circle penWidthPreview = new Circle(3);
    private final ToggleButton drawMode = StudioFormControls.toggle("Panear/dibujar: dibujar",
            "Alternar entre dibujar trazos y panear el lienzo sin dibujar.");
    private final ToggleButton eraser = StudioFormControls.toggle("Borrador",
            "Borrar solo la tinta del frame.");
    private final CheckBox activateDrawn = new CheckBox("Usar frame dibujado al guardar");
    private final CheckBox useCameraGuide = new CheckBox("Utilizar plano asignado");
    private final CheckBox showFragmentTitle = new CheckBox("Mostrar fragmento como titulo en el lienzo");
    private final ButtonType saveButtonType = new ButtonType("Guardar frame", ButtonBar.ButtonData.OK_DONE);

    private ScrollPane canvasScroll;
    private Label canvasTitleLabel;
    private InkRealtimeStrokeEngine inkEngine;
    private FrameTitleState frameTitleState;
    private boolean strokeActive;
    private Point2D lastInkPoint = Point2D.ZERO;

    public TheatreFrameSketchDialog(Window owner, TheatreFrameSketchContext context) {
        this(owner, context, null);
    }

    public TheatreFrameSketchDialog(Window owner, TheatreFrameSketchContext context, Runnable historyBoardAction) {
        this.context = context;
        this.historyBoardAction = historyBoardAction;
        this.restoredInkState = readExistingInkState(context);
        this.frameTitleState = initialTitleState();
        initOwner(owner);
        setTitle("Dibujar/editar frame");
        getDialogPane().getButtonTypes().setAll(
                saveButtonType,
                ButtonType.CANCEL);
        getDialogPane().setContent(content());
        getDialogPane().setPrefSize(1480, 860);
        getDialogPane().setMinSize(980, 620);
        setResizable(true);
        activateDrawn.setSelected(true);
        drawMode.setSelected(true);
        configureKeyboardShortcuts();
        initializeCanvas();
        setResultConverter(button -> button == saveButtonType ? saveResult() : null);
        setOnShown(event -> Platform.runLater(() -> {
            configureStageWindow();
            installDialogButtonHandlers();
            resetInkCoordinateState();
        }));
        setOnHidden(event -> disposeInk());
    }

    private BorderPane content() {
        BorderPane root = new BorderPane();
        root.setLeft(leftPanel());
        root.setCenter(canvasPanel());
        return root;
    }

    private VBox leftPanel() {
        Label fragmentPreview = new Label(context.interventionText());
        fragmentPreview.getStyleClass().add("theatre-frame-fragment-preview");
        fragmentPreview.setWrapText(true);
        fragmentPreview.setMaxWidth(Double.MAX_VALUE);
        fragmentPreview.setMinHeight(Region.USE_PREF_SIZE);
        showFragmentTitle.setSelected(frameTitleState.enabled());
        showFragmentTitle.getStyleClass().addAll(StudioFormControls.FORM_CONTROL, StudioFormControls.FORM_TOGGLE);
        StudioFormControls.installTooltip(showFragmentTitle,
                "Cuando esta activo, el texto del fragmento se muestra como titulo dentro del frame.");
        showFragmentTitle.selectedProperty().addListener((obs, oldValue, newValue) -> updateCanvasTitle());

        Accordion accordion = new Accordion(objectsPane(), storyboardPane());
        accordion.setExpandedPane(accordion.getPanes().get(1));

        VBox panel = new VBox(12, fragmentPreview, showFragmentTitle, accordion);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(390);
        panel.setMinWidth(340);
        return panel;
    }

    private TitledPane objectsPane() {
        VBox list = new VBox(10);
        if (!context.sceneObjects().isEmpty()) {
            context.sceneObjects().forEach(object -> list.getChildren().add(objectCard(object)));
        } else {
            Label empty = new Label("Sin imagenes de objetos en esta escena.");
            empty.setWrapText(true);
            list.getChildren().add(empty);
        }
        return new TitledPane("Objetos de la escena", list);
    }

    private Node objectCard(TheatreFrameSketchContext.ObjectPreview object) {
        ImageView thumbnail = new ImageView();
        thumbnail.setFitWidth(82);
        thumbnail.setFitHeight(62);
        thumbnail.setPreserveRatio(true);
        thumbnail.setSmooth(true);
        if (!object.imageUri().isBlank()) {
            thumbnail.setImage(new Image(object.imageUri(), 82, 62, true, true, true));
        }

        Label name = new Label(object.name().isBlank() ? object.id() : object.name());
        name.getStyleClass().add("document-media-frame-title");
        name.setWrapText(true);
        Label description = new Label(object.description().isBlank() ? "Objeto de la escena." : object.description());
        description.setWrapText(true);

        Button fullscreen = iconToolButton("zoom-in", "Ver objeto en pantalla completa.", () -> showObjectFullscreen(object), false);
        fullscreen.setDisable(object.imageUri().isBlank());
        VBox text = new VBox(4, name, description);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(10, thumbnail, text, fullscreen);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("document-media-object-card");
        return row;
    }

    private void showObjectFullscreen(TheatreFrameSketchContext.ObjectPreview object) {
        ImageFullscreenViewer.show(
                object.imageUri(),
                getDialogPane().getScene() == null ? null : getDialogPane().getScene().getWindow(),
                getDialogPane().getScene() == null ? List.of() : getDialogPane().getScene().getStylesheets(),
                object.name().isBlank() ? "Objeto teatral" : object.name(),
                "No se pudo mostrar el objeto",
                ignored -> {
                });
    }

    private TitledPane storyboardPane() {
        VBox list = new VBox(10);
        list.getChildren().add(variantPreview("Imagen oficial/generada", context.officialImageUri(), "Sin imagen oficial"));
        list.getChildren().add(variantPreview("Frame dibujado", context.drawnFrameUri(), "Boceto pendiente"));
        Button historyBoard = ActionButtonFactory.secondary("Ver history board",
                "Abrir el mapa visual completo de la obra.",
                this::showHistoryBoard);
        historyBoard.setDisable(historyBoardAction == null);
        list.getChildren().add(historyBoard);
        useCameraGuide.setSelected(context.cameraGuideUri() != null && !context.cameraGuideUri().isBlank());
        useCameraGuide.setDisable(context.cameraGuideUri() == null || context.cameraGuideUri().isBlank());
        useCameraGuide.getStyleClass().addAll(StudioFormControls.FORM_CONTROL, StudioFormControls.FORM_TOGGLE);
        StudioFormControls.installTooltip(useCameraGuide,
                "Mostrar el plano asignado como referencia translucida para dibujar encima.");
        activateDrawn.getStyleClass().addAll(StudioFormControls.FORM_CONTROL, StudioFormControls.FORM_TOGGLE);
        StudioFormControls.installTooltip(activateDrawn,
                "Si esta activo, el frame dibujado queda como variante visual para reproduccion y exportacion.");
        list.getChildren().add(useCameraGuide);
        list.getChildren().add(activateDrawn);
        return new TitledPane("Storyboard", list);
    }

    private void showHistoryBoard() {
        if (historyBoardAction != null) {
            historyBoardAction.run();
        }
    }

    private VBox variantPreview(String title, String imageUri, String emptyText) {
        Label label = new Label(title);
        label.getStyleClass().add("document-media-frame-title");
        StackPane preview = new StackPane();
        preview.setMinSize(300, 170);
        preview.setPrefSize(300, 170);
        preview.getStyleClass().add("document-media-thumbnail-empty");
        if (imageUri == null || imageUri.isBlank()) {
            Label empty = new Label(emptyText);
            empty.setTextFill(Color.BLACK);
            preview.getChildren().add(empty);
        } else {
            ImageView imageView = new ImageView(new Image(imageUri, 300, 170, true, true, true));
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setFitWidth(300);
            imageView.setFitHeight(170);
            preview.getChildren().add(imageView);
        }
        return new VBox(4, label, preview);
    }

    private BorderPane canvasPanel() {
        surface.resetForFixedEditableState(FRAME_WIDTH, FRAME_HEIGHT, backgroundColor.getValue());
        surface.setFocusTraversable(true);
        loadExistingFrame();

        canvasTitleLabel = new Label();
        canvasTitleLabel.setWrapText(true);
        canvasTitleLabel.setMouseTransparent(true);
        canvasTitleLabel.getStyleClass().add("technical-problem-canvas-title-band");
        updateCanvasTitle();

        StackPane canvasFrame = new StackPane();
        canvasFrame.getChildren().add(surface);
        java.util.Optional<List<Node>> cameraGuideLayer = cameraGuideLayer();
        if (cameraGuideLayer.isPresent()) {
            cameraGuideLayer.get().forEach(node -> node.visibleProperty().bind(useCameraGuide.selectedProperty()));
            canvasFrame.getChildren().addAll(cameraGuideLayer.get());
        }
        canvasFrame.getChildren().add(canvasTitleLabel);
        canvasFrame.setPadding(new Insets(16));
        canvasFrame.getStyleClass().add("technical-problem-canvas-frame");
        StackPane.setAlignment(canvasTitleLabel, Pos.TOP_LEFT);
        StackPane.setMargin(canvasTitleLabel, new Insets(18, 18, 0, 18));

        Group scaledCanvas = new Group(canvasFrame);
        scaledCanvas.scaleXProperty().bind(canvasZoom.valueProperty().divide(100.0));
        scaledCanvas.scaleYProperty().bind(canvasZoom.valueProperty().divide(100.0));

        StackPane zoomHost = new StackPane(scaledCanvas);
        zoomHost.setAlignment(Pos.CENTER);

        canvasScroll = new ScrollPane(zoomHost);
        canvasScroll.setPannable(false);
        canvasScroll.setFitToWidth(false);
        canvasScroll.setFitToHeight(false);
        canvasScroll.getStyleClass().add("technical-problem-canvas-scroll");
        zoomHost.minWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(canvasScroll.getViewportBounds().getWidth(),
                        (surface.logicalWidth() + 32.0) * canvasZoom.getValue() / 100.0),
                canvasScroll.viewportBoundsProperty(), canvasZoom.valueProperty()));
        zoomHost.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(canvasScroll.getViewportBounds().getHeight(),
                        (surface.logicalHeight() + 32.0) * canvasZoom.getValue() / 100.0),
                canvasScroll.viewportBoundsProperty(), canvasZoom.valueProperty()));
        zoomHost.prefWidthProperty().bind(zoomHost.minWidthProperty());
        zoomHost.prefHeightProperty().bind(zoomHost.minHeightProperty());
        canvasScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (drawMode.isSelected() && event.getTarget() instanceof Node target && descendantOf(target, surface)) {
                event.consume();
            }
        });

        BorderPane panel = new BorderPane();
        panel.setTop(toolbar());
        panel.setCenter(canvasScroll);
        updateInputMode();
        return panel;
    }

    private java.util.Optional<List<Node>> cameraGuideLayer() {
        String uri = context.cameraGuideUri();
        if (uri == null || uri.isBlank()) {
            return java.util.Optional.empty();
        }
        ImageView guide = new ImageView(new Image(uri, FRAME_WIDTH, FRAME_HEIGHT, false, true, true));
        guide.setFitWidth(FRAME_WIDTH);
        guide.setFitHeight(FRAME_HEIGHT);
        guide.setPreserveRatio(false);
        guide.setSmooth(true);
        guide.opacityProperty().bind(cameraGuideOpacity.valueProperty().divide(100.0));
        guide.setMouseTransparent(true);
        return java.util.Optional.of(List.of(guide));
    }

    private void updateCanvasTitle() {
        frameTitleState = FrameTitleState.create(showFragmentTitle.isSelected(),
                frameTitleState == null ? context.interventionText() : frameTitleState.text(),
                surface.logicalWidth());
        if (canvasTitleLabel == null) {
            return;
        }
        boolean visible = frameTitleState.visible();
        canvasTitleLabel.setVisible(visible);
        canvasTitleLabel.setManaged(visible);
        canvasTitleLabel.setText(visible ? frameTitleState.text() : "");
        canvasTitleLabel.setMaxWidth(Math.max(1.0, surface.logicalWidth() - frameTitleState.margin() * 2.0));
        canvasTitleLabel.setPrefHeight(Math.max(1.0, frameTitleState.bandHeight() - frameTitleState.margin()));
        StackPane.setMargin(canvasTitleLabel, new Insets(frameTitleState.margin(), frameTitleState.margin(), 0,
                frameTitleState.margin()));
    }

    private FlowPane toolbar() {
        configureStrokeWidth();
        configureCanvasZoom();
        configureCameraGuideOpacity();
        StudioFormControls.colorPicker(penColor, "Color del lapiz.");
        StudioFormControls.colorPicker(backgroundColor, "Color de fondo del frame.");
        Button undoButton = iconToolButton("undo-2", "Deshacer (Ctrl+Z).", this::undo, false);
        Button redoButton = iconToolButton("redo-2", "Rehacer (Ctrl+Y).", this::redo, false);
        Button clearStrokesButton = ActionButtonFactory.secondary(
                "Limpiar trazos",
                "Limpiar solo trazos; conserva el fondo.",
                this::clearInkStrokes);
        Button clearCanvasButton = iconToolButton("trash-2", "Limpiar frame completo.", this::clearCanvas, true);

        drawMode.textProperty().bind(Bindings.when(drawMode.selectedProperty())
                .then("Panear/dibujar: dibujar")
                .otherwise("Panear/dibujar: panear"));
        drawMode.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (!Boolean.TRUE.equals(newValue)) {
                cancelActiveInkStroke();
            }
            updateInputMode();
            resetInkCoordinateState();
        });
        backgroundColor.setOnAction(event -> {
            rememberUndo();
            redo.clear();
            surface.fillBackground(backgroundColor.getValue());
        });
        eraser.setGraphic(LucideIconView.of("eraser"));
        eraser.setContentDisplay(ContentDisplay.LEFT);

        Label cameraGuideOpacityLabel = new Label("Transparencia del plano");
        cameraGuideOpacityLabel.visibleProperty().bind(cameraGuideOpacity.visibleProperty());
        cameraGuideOpacityLabel.managedProperty().bind(cameraGuideOpacity.visibleProperty());

        FlowPane tools = new FlowPane(8, 8,
                drawMode,
                new Label("Lapiz"), penColor,
                new Label("Fondo"), backgroundColor,
                new Label("Grosor"), strokeWidth, penWidthPreview,
                new Label("Zoom lienzo"), canvasZoom, canvasZoomValue,
                cameraGuideOpacityLabel, cameraGuideOpacity, cameraGuideOpacityValue,
                eraser, undoButton, redoButton, clearStrokesButton, clearCanvasButton);
        tools.setAlignment(Pos.CENTER_LEFT);
        tools.getStyleClass().addAll("technical-problem-tools", "technical-problem-tools-flow");
        return tools;
    }

    private Button iconToolButton(String iconName, String tooltip, Runnable action, boolean warning) {
        Button button = warning
                ? ActionButtonFactory.warning("", tooltip, action)
                : ActionButtonFactory.secondary("", tooltip, action);
        button.setGraphic(LucideIconView.of(iconName));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("technical-problem-icon-button");
        button.setMinWidth(40);
        button.setPrefWidth(40);
        button.setMaxWidth(40);
        return button;
    }

    private void configureStrokeWidth() {
        strokeWidth.setBlockIncrement(1);
        strokeWidth.setMajorTickUnit(4);
        strokeWidth.setMinorTickCount(0);
        strokeWidth.setShowTickMarks(true);
        strokeWidth.setSnapToTicks(false);
        strokeWidth.setPrefWidth(170);
        StudioFormControls.slider(strokeWidth, "Grosor maximo del trazo en pixeles.");
        penWidthPreview.setFill(Color.BLACK);
        penWidthPreview.setStroke(Color.web("#dbe3f1"));
        penWidthPreview.setStrokeWidth(1);
        penWidthPreview.radiusProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(2.0, strokeWidth.getValue() / 2.0),
                strokeWidth.valueProperty()));
        StudioFormControls.installTooltip(penWidthPreview, "Vista previa del grosor del lapiz.");
    }

    private void configureCanvasZoom() {
        canvasZoom.setBlockIncrement(5);
        canvasZoom.setMajorTickUnit(25);
        canvasZoom.setMinorTickCount(4);
        canvasZoom.setShowTickMarks(true);
        canvasZoom.setSnapToTicks(false);
        canvasZoom.setPrefWidth(170);
        StudioFormControls.slider(canvasZoom, "Acercar o alejar el lienzo sin cambiar el tamano exportado.");
        canvasZoomValue.textProperty().bind(Bindings.createStringBinding(
                () -> Math.round(canvasZoom.getValue()) + "%",
                canvasZoom.valueProperty()));
        StudioFormControls.installTooltip(canvasZoomValue, "Zoom visual del lienzo.");
    }

    private void configureCameraGuideOpacity() {
        cameraGuideOpacity.setBlockIncrement(1);
        cameraGuideOpacity.setMajorTickUnit(15);
        cameraGuideOpacity.setMinorTickCount(2);
        cameraGuideOpacity.setShowTickMarks(true);
        cameraGuideOpacity.setSnapToTicks(false);
        cameraGuideOpacity.setPrefWidth(170);
        cameraGuideOpacity.visibleProperty().bind(useCameraGuide.selectedProperty().and(useCameraGuide.disabledProperty().not()));
        cameraGuideOpacity.managedProperty().bind(cameraGuideOpacity.visibleProperty());
        cameraGuideOpacityValue.visibleProperty().bind(cameraGuideOpacity.visibleProperty());
        cameraGuideOpacityValue.managedProperty().bind(cameraGuideOpacity.visibleProperty());
        StudioFormControls.slider(cameraGuideOpacity, "Transparencia de la guia de plano. No se exporta en el PNG.");
        cameraGuideOpacityValue.textProperty().bind(Bindings.createStringBinding(
                () -> Math.round(cameraGuideOpacity.getValue()) + "%",
                cameraGuideOpacity.valueProperty()));
        StudioFormControls.installTooltip(cameraGuideOpacityValue,
                "Nivel visible del plano asignado mientras dibujas.");
    }

    private void installDialogButtonHandlers() {
        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        if (saveButton instanceof Button button) {
            button.setOnAction(event -> {
                event.consume();
                setResult(saveResult());
                close();
            });
        }
        Node cancelButton = getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton instanceof Button button) {
            button.setOnAction(event -> {
                event.consume();
                setResult(null);
                close();
            });
        }
    }

    private void initializeCanvas() {
        startInkEngine();
        inputProvider.attach(surface.inkInputTarget(), new InkInputListener() {
            @Override
            public boolean onStrokeStart(InkInputSample sample) {
                return handleStrokeStart(sample);
            }

            @Override
            public boolean onStrokeMove(InkInputSample sample) {
                return handleStrokeMove(sample);
            }

            @Override
            public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
                boolean consumed = false;
                for (InkInputSample sample : samples == null ? List.<InkInputSample>of() : samples) {
                    consumed |= handleStrokeMove(sample);
                }
                return consumed;
            }

            @Override
            public boolean onStrokeEnd(InkInputSample sample) {
                return handleStrokeEnd(sample);
            }
        });
    }

    private boolean handleStrokeStart(InkInputSample sample) {
        if (!drawMode.isSelected()) {
            return false;
        }
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            strokeActive = false;
            return false;
        }
        rememberUndo();
        redo.clear();
        strokeActive = true;
        lastInkPoint = point;
        inkEngine.begin(point.getX(), point.getY(), sample == null ? 0L : sample.nanos(), penColor.getValue(),
                strokeWidth.getValue(), eraser.isSelected(), sample == null ? 1.0 : sample.pressure());
        return true;
    }

    private boolean handleStrokeMove(InkInputSample sample) {
        if (!strokeActive || !drawMode.isSelected()) {
            return false;
        }
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            return false;
        }
        lastInkPoint = point;
        inkEngine.move(point.getX(), point.getY(), sample == null ? 0L : sample.nanos(), penColor.getValue(),
                strokeWidth.getValue(), eraser.isSelected(), sample == null ? 1.0 : sample.pressure());
        return true;
    }

    private boolean handleStrokeEnd(InkInputSample sample) {
        if (!strokeActive || !drawMode.isSelected()) {
            strokeActive = false;
            return false;
        }
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            point = lastInkPoint;
        }
        inkEngine.end(point.getX(), point.getY(), sample == null ? 0L : sample.nanos(), penColor.getValue(),
                strokeWidth.getValue(), eraser.isSelected(), sample == null ? 1.0 : sample.pressure());
        strokeActive = false;
        return true;
    }

    private void cancelActiveInkStroke() {
        strokeActive = false;
        lastInkPoint = new Point2D(0, 0);
        surface.clearLiveStroke();
    }

    private void startInkEngine() {
        inkEngine = new InkRealtimeStrokeEngine(new InkRealtimeStrokeEngine.Sink() {
            @Override
            public void beginLiveStroke() {
                surface.beginLiveStroke();
            }

            @Override
            public void previewLine(double x1, double y1, double x2, double y2, Color color, double width, boolean erase) {
                surface.previewLine(x1, y1, x2, y2, color, width, erase);
            }

            @Override
            public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                                         double endX, double endY, Color color, double width, boolean erase) {
                surface.previewQuadratic(startX, startY, controlX, controlY, endX, endY, color, width, erase);
            }

            @Override
            public void commitStroke(InkRealtimeStrokeEngine.CommittedStroke stroke) {
                surface.commitInkStroke(new InkCanvasSurface.InkStrokeState(
                        stroke.erase() ? "ERASE" : "DRAW",
                        colorToHex(stroke.color()),
                        stroke.width(),
                        stroke.points().stream()
                                .map(point -> new InkCanvasSurface.InkPointState(
                                        point.x(), point.y(), point.nanos(), point.pressure()))
                                .toList()));
                surface.clearLiveStroke();
            }

            @Override
            public boolean acceptsPoint(double x, double y) {
                return insideCanvas(x, y) && !insideTitleBand(new Point2D(x, y));
            }
        });
        inkEngine.start();
    }

    private Point2D pointInsideCanvas(InkInputSample sample) {
        if (sample == null) {
            return null;
        }
        return InkCanvasViewportCoordinateMapper.mapInside(
                        surface.inkInputTarget(),
                        surface,
                        sample.x(),
                        sample.y(),
                        surface.logicalWidth(),
                        surface.logicalHeight())
                .filter(point -> !insideTitleBand(point))
                .orElse(null);
    }

    private boolean insideCanvas(double x, double y) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && x >= 0.0
                && y >= 0.0
                && x <= FRAME_WIDTH
                && y <= FRAME_HEIGHT;
    }

    private boolean insideTitleBand(Point2D point) {
        return frameTitleState.visible()
                && point != null
                && point.getY() >= 0.0
                && point.getY() <= frameTitleState.bandHeight();
    }

    private void updateInputMode() {
        boolean inkActive = drawMode.isSelected();
        if (canvasScroll != null) {
            canvasScroll.setPannable(!inkActive);
        }
        surface.inkInputLayer().setMouseTransparent(!inkActive);
        surface.inkInputLayer().setPickOnBounds(inkActive);
        surface.inkInputTarget().setMouseTransparent(!inkActive);
        surface.inkInputTarget().setVisible(inkActive);
        surface.inkInputTarget().setDisable(!inkActive);
        if (!inkActive) {
            resetInkCoordinateState();
        }
    }

    private void resetInkCoordinateState() {
        inputProvider.resetCoordinateState();
    }

    private static boolean descendantOf(Node target, Node ancestor) {
        Node current = target;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void configureKeyboardShortcuts() {
        getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isControlDown()) {
                return;
            }
            if (event.getCode() == KeyCode.Z) {
                undo();
                event.consume();
            } else if (event.getCode() == KeyCode.Y) {
                redo();
                event.consume();
            }
        });
    }

    private void loadExistingFrame() {
        if (restoredInkState != null) {
            Color restoredBackground = parseFxColor(restoredInkState.background(), backgroundColor.getValue());
            backgroundColor.setValue(restoredBackground);
            surface.resetForFixedEditableState(FRAME_WIDTH, FRAME_HEIGHT, restoredBackground);
            surface.restoreApplicationInkStrokes(restoredInkState.strokes());
            surface.setFixedLogicalViewport(FRAME_WIDTH, FRAME_HEIGHT);
            return;
        }
        String uri = context.drawnFrameUri();
        if (uri.isBlank()) {
            return;
        }
        Platform.runLater(() -> {
            Image image = new Image(uri, FRAME_WIDTH, FRAME_HEIGHT, false, true, false);
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return;
            }
            surface.drawBackgroundImage(toWritable(image), backgroundColor.getValue());
            surface.setFixedLogicalViewport(FRAME_WIDTH, FRAME_HEIGHT);
        });
    }

    private WritableImage toWritable(Image image) {
        WritableImage writable = new WritableImage((int) Math.ceil(image.getWidth()), (int) Math.ceil(image.getHeight()));
        PixelReader reader = image.getPixelReader();
        if (reader != null) {
            writable.getPixelWriter().setPixels(0, 0, (int) writable.getWidth(), (int) writable.getHeight(), reader, 0, 0);
        }
        return writable;
    }

    private void rememberUndo() {
        undo.addLast(surface.inkStrokeStates());
        while (undo.size() > MAX_UNDO) {
            undo.removeFirst();
        }
    }

    private void undo() {
        if (undo.isEmpty()) {
            return;
        }
        redo.addLast(surface.inkStrokeStates());
        surface.restoreInkUndoState(undo.removeLast(), List.of());
    }

    private void redo() {
        if (redo.isEmpty()) {
            return;
        }
        undo.addLast(surface.inkStrokeStates());
        while (undo.size() > MAX_UNDO) {
            undo.removeFirst();
        }
        surface.restoreInkUndoState(redo.removeLast(), List.of());
    }

    private void clearInkStrokes() {
        rememberUndo();
        redo.clear();
        surface.clearStrokes();
    }

    private void clearCanvas() {
        rememberUndo();
        redo.clear();
        surface.resetForFixedEditableState(FRAME_WIDTH, FRAME_HEIGHT, backgroundColor.getValue());
        updateInputMode();
        updateCanvasTitle();
    }

    private Result saveResult() {
        try {
            updateCanvasTitle();
            Path temp = Files.createTempFile("docupodcast-drawn-frame-", ".png");
            writePng(exportFrameImage(), temp);
            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
            metadata.put("consumer", "theatre.storyboard-frame");
            metadata.put("segmentId", context.segmentId());
            metadata.put("interventionId", context.interventionId());
            metadata.put(METADATA_SHOW_FRAGMENT_TITLE, Boolean.toString(frameTitleState.enabled()));
            metadata.put(METADATA_FRAGMENT_TITLE_TEXT, frameTitleState.text());
            metadata.put(METADATA_TITLE_BAND_HEIGHT, String.format(java.util.Locale.ROOT, "%.3f",
                    frameTitleState.bandHeight()));
            metadata.put(METADATA_TITLE_MARGIN, String.format(java.util.Locale.ROOT, "%.3f",
                    frameTitleState.margin()));
            String inkState = InkWorkspaceStateSerializer.toJson(InkWorkspaceState.create(
                    surface.logicalWidth(),
                    surface.logicalHeight(),
                    colorToHex(surface.backgroundColor()),
                    surface.applicationInkStrokes(),
                    List.of(),
                    metadata
            ));
            return new Result(context.segmentId(), temp, inkState, activateDrawn.isSelected());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el frame dibujado: " + ex.getMessage(), ex);
        }
    }

    private WritableImage exportFrameImage() {
        WritableImage image = surface.exportWithImages(List.of(), new StudyProblemCanvasExportOptions(
                EXPORT_SCALE,
                24_000_000L,
                false,
                0,
                FRAME_WIDTH,
                FRAME_HEIGHT)).image();
        int width = Math.min((int) Math.round(FRAME_WIDTH * 2.0), (int) image.getWidth());
        int height = Math.min((int) Math.round(FRAME_HEIGHT * 2.0), (int) image.getHeight());
        if (width == (int) image.getWidth() && height == (int) image.getHeight()) {
            return image;
        }
        return new WritableImage(image.getPixelReader(), 0, 0, width, height);
    }

    private void configureStageWindow() {
        Window window = getDialogPane().getScene() == null ? null : getDialogPane().getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.setResizable(true);
            stage.setMinWidth(980);
            stage.setMinHeight(620);
            stage.setMaximized(true);
        }
    }

    private void writePng(WritableImage image, Path target) throws IOException {
        int width = (int) Math.ceil(image.getWidth());
        int height = (int) Math.ceil(image.getHeight());
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = buffered.createGraphics();
        try {
            graphics.setColor(toAwtColor(surface.backgroundColor()));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = reader.getArgb(x, y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha <= 0) {
                    continue;
                }
                if (alpha >= 255) {
                    buffered.setRGB(x, y, argb);
                } else {
                    buffered.setRGB(x, y, blendOver(buffered.getRGB(x, y), argb, alpha));
                }
            }
        }
        graphics = buffered.createGraphics();
        try {
            drawFrameTitle(graphics, width, height, frameTitleState);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(buffered, "png", target.toFile());
    }

    private void drawFrameTitle(Graphics2D graphics, int width, int height, FrameTitleState title) {
        if (graphics == null || title == null || !title.visible()) {
            return;
        }
        double scaleX = width / FRAME_WIDTH;
        double scaleY = height / FRAME_HEIGHT;
        double scale = Math.min(scaleX, scaleY);
        int margin = Math.max(1, (int) Math.round(title.margin() * scale));
        int bandHeight = Math.max(1, (int) Math.round(title.bandHeight() * scaleY));
        int x = margin;
        int y = margin;
        int boxWidth = Math.max(1, width - margin * 2);
        int boxHeight = Math.max(1, bandHeight - margin);
        int paddingX = Math.max(8, (int) Math.round(12 * scale));
        int paddingY = Math.max(6, (int) Math.round(8 * scale));

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(toAwtColor(parseFxColor(title.background(), Color.rgb(255, 255, 255, 0.96)), true));
        int arc = Math.max(4, (int) Math.round(4 * scale));
        graphics.fillRoundRect(x, y, boxWidth, boxHeight, arc, arc);
        graphics.setColor(toAwtColor(parseFxColor("#dbe3f1", Color.web("#dbe3f1")), true));
        graphics.drawRoundRect(x, y, boxWidth, boxHeight, arc, arc);

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, (int) Math.round(title.fontSize() * scale)));
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        List<String> lines = wrappedTitleLines(title.text(), metrics, boxWidth - paddingX * 2);
        graphics.setColor(toAwtColor(parseFxColor(title.foreground(), Color.web("#111827")), true));
        int baseline = y + paddingY + metrics.getAscent();
        int bottom = y + boxHeight - paddingY;
        for (String line : lines) {
            if (baseline > bottom) {
                break;
            }
            graphics.drawString(line, x + paddingX, baseline);
            baseline += metrics.getHeight();
        }
    }

    private static int blendOver(int backgroundRgb, int foregroundArgb, int alpha) {
        double a = alpha / 255.0;
        int bgR = (backgroundRgb >> 16) & 0xff;
        int bgG = (backgroundRgb >> 8) & 0xff;
        int bgB = backgroundRgb & 0xff;
        int fgR = (foregroundArgb >> 16) & 0xff;
        int fgG = (foregroundArgb >> 8) & 0xff;
        int fgB = foregroundArgb & 0xff;
        int r = (int) Math.round(fgR * a + bgR * (1.0 - a));
        int g = (int) Math.round(fgG * a + bgG * (1.0 - a));
        int b = (int) Math.round(fgB * a + bgB * (1.0 - a));
        return (r << 16) | (g << 8) | b;
    }

    private static java.awt.Color toAwtColor(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return new java.awt.Color(
                channel(safe.getRed()),
                channel(safe.getGreen()),
                channel(safe.getBlue()));
    }

    private static java.awt.Color toAwtColor(Color color, boolean withAlpha) {
        Color safe = color == null ? Color.WHITE : color;
        return new java.awt.Color(
                channel(safe.getRed()),
                channel(safe.getGreen()),
                channel(safe.getBlue()),
                withAlpha ? channel(safe.getOpacity()) : 255);
    }

    private void disposeInk() {
        inputProvider.detach();
        if (inkEngine != null) {
            inkEngine.stop();
            inkEngine = null;
        }
    }

    private static String colorToHex(Color color) {
        Color safe = color == null ? Color.BLACK : color;
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x%02x",
                channel(safe.getRed()), channel(safe.getGreen()), channel(safe.getBlue()), channel(safe.getOpacity()));
    }

    private static int channel(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value * 255.0)));
    }

    private InkWorkspaceState readExistingInkState(TheatreFrameSketchContext context) {
        String uri = context == null ? "" : context.drawnFrameStateUri();
        if (uri == null || uri.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(URI.create(uri));
            if (!Files.isRegularFile(path)) {
                return null;
            }
            return InkWorkspaceStateSerializer.fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    private FrameTitleState initialTitleState() {
        if (restoredInkState == null) {
            return FrameTitleState.create(false, context.interventionText(), FRAME_WIDTH);
        }
        Map<String, String> metadata = restoredInkState.metadata();
        boolean enabled = Boolean.parseBoolean(metadata.getOrDefault(METADATA_SHOW_FRAGMENT_TITLE, "false"));
        String text = metadata.getOrDefault(METADATA_FRAGMENT_TITLE_TEXT, context.interventionText());
        return FrameTitleState.create(enabled, text, FRAME_WIDTH);
    }

    private static Color parseFxColor(String value, Color fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? Color.WHITE : fallback;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ex) {
            return fallback == null ? Color.WHITE : fallback;
        }
    }

    private static List<String> wrappedTitleLines(String text, FontMetrics metrics, int maxWidth) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        ArrayList<String> lines = new ArrayList<>();
        String[] explicitLines = text.split("\\R", -1);
        for (String explicit : explicitLines) {
            if (explicit.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : explicit.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth || current.isEmpty()) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        return List.copyOf(lines);
    }

    private record FrameTitleState(boolean enabled, String text, double bandHeight, double margin, double fontSize,
                                   String foreground, String background) {
        private static FrameTitleState create(boolean enabled, String text, double canvasWidth) {
            String safeText = text == null ? "" : text.strip();
            double availableWidth = Math.max(1.0, canvasWidth - TITLE_MARGIN * 2.0 - 24.0);
            int lineCount = estimateLineCount(safeText, availableWidth);
            double height = Math.max(TITLE_MIN_BAND_HEIGHT,
                    Math.min(TITLE_MAX_BAND_HEIGHT, TITLE_MARGIN + 16.0 + lineCount * TITLE_LINE_HEIGHT));
            return new FrameTitleState(enabled, safeText, height, TITLE_MARGIN, TITLE_FONT_SIZE,
                    "#111827ff", "#fffffff5");
        }

        private boolean visible() {
            return enabled && !text.isBlank();
        }

        private static int estimateLineCount(String text, double availableWidth) {
            if (text == null || text.isBlank()) {
                return 1;
            }
            int charsPerLine = Math.max(24, (int) Math.floor(availableWidth / 8.3));
            int count = 0;
            for (String line : text.split("\\R", -1)) {
                count += Math.max(1, (int) Math.ceil((double) Math.max(1, line.length()) / charsPerLine));
            }
            return Math.max(1, count);
        }
    }

    public record Result(String segmentId, Path framePng, String inkStateJson, boolean activateDrawn) {
    }
}
