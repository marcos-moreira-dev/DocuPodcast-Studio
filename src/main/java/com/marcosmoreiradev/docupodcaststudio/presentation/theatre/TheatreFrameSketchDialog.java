package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.TheatreDrawingVaultItem;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.InkEditorSession;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.LucideIconView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCanvasToolbar;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioAccordion;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions;
import com.marcosmoreiradev.docupodcaststudio.ink.InkRealtimeStrokeEngine;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasViewport;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasZoomPane;
import com.marcosmoreiradev.docupodcaststudio.ink.controls.InkPressureIndicator;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.NoopInkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelFormat;
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

import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkImageFileStore;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

/** Drawn-frame editor reused by theatre storyboard fragments. */
public final class TheatreFrameSketchDialog extends Dialog<TheatreFrameSketchDialog.Result> {
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
    private final DrawingProfile drawingProfile;
    private final double frameWidth;
    private final double frameHeight;
    private final int exportScale;
    private final InkWorkspaceState restoredInkState;
    private final InkCanvasSurface surface = new InkCanvasSurface();
    private final CheckBox mergeWithStage = StudioFormControls.checkBox("Fusionar dibujo con escenario");
    private final ImageView mergedBackdrop = new ImageView();
    private Image stageImage;
    private boolean pointerOverControls;
    private final ToggleButton fillMode = StudioFormControls.toggle("Rellenar", "Haz clic dentro de una figura cerrada para rellenarla con el color del lápiz.");
    private final javafx.scene.shape.Rectangle fillTarget = new javafx.scene.shape.Rectangle();
    private final java.util.List<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill> fills = new java.util.ArrayList<>();
    private final List<TheatreDrawingVaultItem> drawingVault = new ArrayList<>();
    private final VBox drawingVaultCards = new VBox(8);
    private boolean restoringSketch;
    private final javafx.scene.control.ToggleButton selectStrokes = StudioFormControls.toggle(
            "Seleccionar dibujos", "Clic selecciona un trazo; Mayús+clic añade trazos a la selección.");
    private com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection strokeSelection;
    private final InkInputProvider inputProvider;
    private final InkCanvasViewport inkViewport;
    private final InkEditorSession<SketchState> editorSession;
    private final ColorPicker penColor = StudioFormControls.colorPicker(Color.BLACK);
    private final ColorPicker backgroundColor = StudioFormControls.colorPicker(Color.WHITE);
    private final Slider strokeWidth = StudioFormControls.slider(1, 48, 6);
    private final Slider canvasZoom = StudioFormControls.slider(25, 200, 100);
    private final Slider cameraGuideOpacity = StudioFormControls.slider(0, 45, 18);
    private final Label canvasZoomValue = new Label("100%");
    private final Label cameraGuideOpacityValue = new Label("18%");
    private final InkPressureIndicator pressureIndicator;
    private final Label saveFeedback = new Label();
    private final ProgressIndicator saveProgress = StudioFeedbackControls.progressIndicator();
    private final Circle penWidthPreview = new Circle(3);
    private final ToggleButton drawMode = StudioFormControls.toggle("Panear/dibujar: dibujar",
            "Alternar entre dibujar trazos y panear el lienzo sin dibujar.");
    private final ToggleButton eraser = StudioFormControls.toggle("Borrador",
            "Borrar solo la tinta del frame.");
    private final CheckBox activateDrawn = StudioFormControls.checkBox("Usar frame dibujado al guardar");
    private final CheckBox useCameraGuide = StudioFormControls.checkBox("Utilizar plano asignado");
    private final CheckBox showFragmentTitle = StudioFormControls.checkBox("Mostrar fragmento como titulo en el lienzo");
    private final ButtonType saveButtonType = NativeDialogResponse.button("Guardar frame", ButtonBar.ButtonData.OK_DONE);

    private ScrollPane canvasScroll;
    private InkCanvasZoomPane canvasZoomPane;
    private Label canvasTitleLabel;
    private InkRealtimeStrokeEngine inkEngine;
    private FrameTitleState frameTitleState;
    private boolean strokeActive;
    private Point2D lastInkPoint = Point2D.ZERO;
    private Node leftEditorContent;
    private Node canvasEditorContent;
    private Button saveButton;
    private Task<Result> saveTask;
    private boolean saveCaptureQueued;
    private final AtomicReference<Path> pendingSaveTarget = new AtomicReference<>();
    private volatile boolean saveAbandoned;
    private Button cancelButton;
    private final EventHandler<ActionEvent> saveActionFilter = event -> {
        event.consume();
        beginSave();
    };
    private final EventHandler<ActionEvent> cancelActionFilter = event -> {
        event.consume();
        cancelAndClose();
    };

    /** Injection seam used by the launcher and input contract tests. */
    public TheatreFrameSketchDialog(Window owner, TheatreFrameSketchContext context,
                                    Runnable historyBoardAction, InkInputProvider inputProvider,
                                    DrawingProfile drawingProfile) {
        this.context = context;
        this.historyBoardAction = historyBoardAction;
        this.inputProvider = inputProvider == null ? NoopInkInputProvider.INSTANCE : inputProvider;
        this.drawingProfile = java.util.Objects.requireNonNull(drawingProfile, "drawing profile");
        this.frameWidth = drawingProfile.logicalWidth();
        this.frameHeight = drawingProfile.logicalHeight();
        this.exportScale = drawingProfile.exportProfile().scale();
        this.inkViewport = new InkCanvasViewport(surface, drawingProfile);
        this.editorSession = new InkEditorSession<>(drawingProfile, this.inputProvider, this::sketchState,
                this::restoreSketchState,
                (state, destination, exportProfile) -> destination);
        pressureIndicator = new InkPressureIndicator(editorSession.inputStatusProperty());
        this.restoredInkState = readExistingInkState(context);
        this.drawingVault.addAll(context.drawingVault());
        this.frameTitleState = initialTitleState();
        initOwner(owner);
        setTitle("Dibujar/editar frame");
        // Bind each response as it is created, including skin/button-bar rebuilds.
        // Binding only the buttons found before/after show leaves later instances inert.
        setDialogPane(new DialogPane() {
            @Override protected Node createButton(ButtonType type) {
                Node node = super.createButton(type);
                if (type == saveButtonType) node.addEventFilter(ActionEvent.ACTION, saveActionFilter);
                else if (type == ButtonType.CANCEL) node.addEventFilter(ActionEvent.ACTION, cancelActionFilter);
                return node;
            }
        });
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
        // Native response buttons remain in the ButtonBar, but result creation is
        // asynchronous so the JavaFX thread never blocks while encoding the PNG.
        setResultConverter(button -> null);
        // Install eagerly and again after the native DialogPane is shown. Some
        // JavaFX skins rebuild their ButtonBar while attaching to a window.
        installDialogButtonHandlers();
        setOnShown(event -> Platform.runLater(() -> {
            configureStageWindow();
            installDialogButtonHandlers();
            resetInkCoordinateState();
        }));
        setOnHidden(event -> {
            abandonPendingSave();
            disposeInk();
        });
    }

    private BorderPane content() {
        BorderPane root = new BorderPane();
        root.setMinSize(0, 0);
        // BorderPane does not clip overflowing children. At smaller window sizes,
        // the editor's minimum-size children can cover the DialogPane ButtonBar
        // and intercept clicks even though the response buttons remain visible.
        javafx.scene.shape.Rectangle contentClip = new javafx.scene.shape.Rectangle();
        contentClip.widthProperty().bind(root.widthProperty());
        contentClip.heightProperty().bind(root.heightProperty());
        root.setClip(contentClip);
        leftEditorContent = leftPanel();
        canvasEditorContent = canvasPanel();
        root.setLeft(leftEditorContent);
        root.setCenter(canvasEditorContent);
        saveProgress.setPrefSize(20, 20);
        saveProgress.setMinSize(20, 20);
        saveProgress.setMaxSize(20, 20);
        saveProgress.setVisible(false);
        saveProgress.setManaged(false);
        saveFeedback.setVisible(false);
        saveFeedback.setManaged(false);
        saveFeedback.setWrapText(true);
        saveFeedback.getStyleClass().add("theatre-frame-save-feedback");
        HBox feedbackRow = new HBox(8, saveProgress, saveFeedback);
        feedbackRow.setAlignment(Pos.CENTER_LEFT);
        feedbackRow.setPadding(new Insets(6, 12, 6, 12));
        root.setBottom(feedbackRow);
        return root;
    }

    private VBox leftPanel() {
        Label fragmentPreview = new Label(context.interventionText());
        fragmentPreview.getStyleClass().add("theatre-frame-fragment-preview");
        fragmentPreview.setWrapText(true);
        fragmentPreview.setMaxWidth(Double.MAX_VALUE);
        fragmentPreview.setMinHeight(Region.USE_PREF_SIZE);
        showFragmentTitle.setSelected(frameTitleState.enabled());
        StudioFormControls.installTooltip(showFragmentTitle,
                "Cuando esta activo, el texto del fragmento se muestra como titulo dentro del frame.");
        showFragmentTitle.selectedProperty().addListener((obs, oldValue, newValue) -> updateCanvasTitle());

        Accordion accordion = StudioAccordion.accordion(objectsPane(), drawingVaultPane(), storyboardPane());
        accordion.setExpandedPane(accordion.getPanes().get(2));

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
        return StudioAccordion.pane("Objetos de la escena", list);
    }

    private TitledPane drawingVaultPane() {
        rebuildDrawingVaultCards();
        Label help = new Label("Reutiliza figuras vectoriales en cualquier frame de esta obra.");
        help.setWrapText(true);
        return StudioAccordion.pane("Baúl de dibujos", new VBox(8, help, drawingVaultCards));
    }

    private void rebuildDrawingVaultCards() {
        drawingVaultCards.getChildren().clear();
        if (drawingVault.isEmpty()) {
            Label empty = new Label("Aún no hay dibujos guardados.");
            empty.setWrapText(true);
            drawingVaultCards.getChildren().add(empty);
            return;
        }
        for (TheatreDrawingVaultItem item : List.copyOf(drawingVault)) {
            drawingVaultCards.getChildren().add(drawingVaultCard(item));
        }
    }

    private Node drawingVaultCard(TheatreDrawingVaultItem item) {
        ImageView preview = new ImageView(vaultPreview(item));
        preview.setFitWidth(86);
        preview.setFitHeight(64);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        TextField name = new TextField(item.name().isBlank() ? "Dibujo" : item.name());
        name.setPromptText("Nombre del dibujo");
        name.focusedProperty().addListener((obs, before, focused) -> {
            if (!focused) renameVaultItem(item.id(), name.getText());
        });
        name.setOnAction(event -> renameVaultItem(item.id(), name.getText()));
        Button insert = ActionButtonFactory.secondary("Añadir al lienzo", () -> insertVaultItem(item));
        Button delete = iconToolButton("trash-2", "Eliminar del baúl.", () -> deleteVaultItem(item.id()), true);
        HBox actions = new HBox(6, insert, delete);
        VBox details = new VBox(6, name, actions);
        HBox.setHgrow(details, Priority.ALWAYS);
        HBox card = new HBox(10, preview, details);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("document-media-object-card");
        return card;
    }

    private Image vaultPreview(TheatreDrawingVaultItem item) {
        try {
            InkWorkspaceState state = InkWorkspaceStateSerializer.fromJson(item.inkStateJson());
            if (state.strokes().isEmpty()) return new WritableImage(172, 128);
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (var stroke : state.strokes()) for (var point : stroke.points()) {
                minX = Math.min(minX, point.x()); minY = Math.min(minY, point.y());
                maxX = Math.max(maxX, point.x()); maxY = Math.max(maxY, point.y());
            }
            double scale = Math.min(152.0 / Math.max(1, maxX - minX), 108.0 / Math.max(1, maxY - minY));
            double offsetX = (172 - (maxX - minX) * scale) / 2.0;
            double offsetY = (128 - (maxY - minY) * scale) / 2.0;
            final double sourceMinX = minX, sourceMinY = minY;
            List<com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke> previewStrokes = state.strokes().stream()
                    .map(stroke -> new com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke(
                            stroke.tool(), stroke.color(), Math.max(1, stroke.width() * scale), stroke.points().stream()
                            .map(point -> com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint.of(
                                    offsetX + (point.x() - sourceMinX) * scale,
                                    offsetY + (point.y() - sourceMinY) * scale,
                                    point.nanos(), point.pressure())).toList())).toList();
            List<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill> previewFills =
                    fillsFromMetadata(state.metadata()).stream().map(fill ->
                            new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill(
                                    offsetX + (fill.x() - sourceMinX) * scale,
                                    offsetY + (fill.y() - sourceMinY) * scale, fill.color())).toList();
            return com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.renderPreview(
                    172, 128, previewStrokes, Color.TRANSPARENT, previewFills);
        } catch (IOException | RuntimeException invalid) {
            return new WritableImage(86, 64);
        }
    }

    private void renameVaultItem(String id, String value) {
        String name = value == null || value.isBlank() ? "Dibujo" : value.strip();
        for (int i = 0; i < drawingVault.size(); i++) {
            TheatreDrawingVaultItem current = drawingVault.get(i);
            if (current.id().equals(id)) {
                drawingVault.set(i, new TheatreDrawingVaultItem(current.id(), name, current.inkStateJson()));
                break;
            }
        }
    }

    private void deleteVaultItem(String id) {
        drawingVault.removeIf(item -> item.id().equals(id));
        rebuildDrawingVaultCards();
    }

    private void insertVaultItem(TheatreDrawingVaultItem item) {
        try {
            InkWorkspaceState state = InkWorkspaceStateSerializer.fromJson(item.inkStateJson());
            strokeSelection.insert(new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.SelectionSnapshot(
                    state.strokes(), fillsFromMetadata(state.metadata())));
            selectStrokes.setSelected(true);
            showTransientFeedback("Dibujo añadido al lienzo. Puedes moverlo con el marco de selección.");
        } catch (IOException | RuntimeException invalid) {
            showTransientFeedback("El dibujo guardado no se pudo abrir.");
        }
    }

    private void copySelectionToVault() {
        var selection = strokeSelection.selectionSnapshot();
        if (selection.strokes().isEmpty()) {
            showTransientFeedback("Selecciona uno o varios dibujos antes de enviarlos al baúl.");
            return;
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        addFillMetadata(metadata, selection.fills());
        String state = InkWorkspaceStateSerializer.toJson(InkWorkspaceState.create(frameWidth, frameHeight,
                "#00000000", selection.strokes(), List.of(), metadata));
        drawingVault.add(new TheatreDrawingVaultItem("drawing-" + UUID.randomUUID().toString().replace("-", ""),
                "Dibujo " + (drawingVault.size() + 1), state));
        rebuildDrawingVaultCards();
        showTransientFeedback("Copia añadida al baúl. Se conservará al guardar el frame.");
    }

    private void showTransientFeedback(String message) {
        saveFeedback.setText(message);
        saveFeedback.setVisible(true);
        saveFeedback.setManaged(true);
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
        StudioFormControls.installTooltip(useCameraGuide,
                "Mostrar el plano asignado como referencia translucida para dibujar encima.");
        StudioFormControls.installTooltip(activateDrawn,
                "Si esta activo, el frame dibujado queda como variante visual para reproduccion y exportacion.");
        list.getChildren().add(useCameraGuide);
        list.getChildren().add(activateDrawn);
        return StudioAccordion.pane("Storyboard", list);
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
        // Two physical preview pixels per logical ink pixel keeps pen edges
        // crisp at the common 100-125% theatre zoom without changing geometry.
        surface.setInkPreviewScale(2.0);
        surface.resetForFixedEditableState(frameWidth, frameHeight, backgroundColor.getValue());
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
            cameraGuideLayer.get().forEach(node -> node.visibleProperty().bind(useCameraGuide.selectedProperty().and(mergeWithStage.selectedProperty().not())));
            canvasFrame.getChildren().addAll(cameraGuideLayer.get());
        }
        canvasFrame.getChildren().add(canvasTitleLabel);
        canvasFrame.setPadding(new Insets(16));
        canvasFrame.getStyleClass().add("technical-problem-canvas-frame");
        StackPane.setAlignment(canvasTitleLabel, Pos.TOP_LEFT);
        StackPane.setMargin(canvasTitleLabel, new Insets(18, 18, 0, 18));

        canvasZoomPane = new InkCanvasZoomPane(canvasFrame,
                () -> surface.logicalWidth() + 32.0,
                () -> surface.logicalHeight() + 32.0);
        canvasZoomPane.setOnZoomApplied(zoom -> {
            editorSession.zoomTo(zoom);
            editorSession.resetInputCoordinates();
        });
        canvasScroll = canvasZoomPane.scrollPane();
        canvasScroll.getStyleClass().add("technical-problem-canvas-scroll");
        canvasScroll.setMinSize(0, 0);
        canvasScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (drawMode.isSelected() && event.getTarget() instanceof Node target && descendantOf(target, surface)) {
                event.consume();
            }
        });

        BorderPane panel = new BorderPane();
        panel.setMinSize(0, 0);
        panel.setTop(toolbar());
        panel.setCenter(canvasZoomPane);
        updateInputMode();
        return panel;
    }

    private java.util.Optional<List<Node>> cameraGuideLayer() {
        String uri = context.cameraGuideUri();
        if (uri == null || uri.isBlank()) {
            return java.util.Optional.empty();
        }
        ImageView guide = new ImageView(new Image(uri, frameWidth, frameHeight, false, true, true));
        guide.setFitWidth(frameWidth);
        guide.setFitHeight(frameHeight);
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

    private StudioCanvasToolbar toolbar() {
        fillMode.setText("Rellenar");
        fillTarget.setManaged(false);
        fillTarget.setFill(Color.TRANSPARENT);
        surface.inkInputLayer().getChildren().add(fillTarget);
        fillTarget.setOnMouseClicked(event -> {
            if (!fillMode.isSelected() || event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            Point2D p=surface.sceneToLocal(event.getSceneX(),event.getSceneY());
            var regions=com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkRegions.detect(surface.applicationInkStrokes(),frameWidth,frameHeight);
            if(regions.regionAt(p.getX(),p.getY())>0) {
                rememberUndo();
                int selectedRegion=regions.regionAt(p.getX(),p.getY());
                fills.removeIf(fill -> regions.regionAt(fill.x(),fill.y())==selectedRegion);
                fills.add(new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill(p.getX(),p.getY(),colorToHex(penColor.getValue())));
                refreshMergedBackdrop();
                saveFeedback.setVisible(false); saveFeedback.setManaged(false);
            } else {
                saveFeedback.setText("No hay una zona cerrada aquí. Cierra el contorno para rellenarla.");
                saveFeedback.setVisible(true); saveFeedback.setManaged(true);
            }
            event.consume();
        });
        fillMode.selectedProperty().addListener((obs,before,active) -> {
            cancelActiveInkStroke();
            if(active) selectStrokes.setSelected(false);
            updateInputMode();
        });
        mergedBackdrop.setMouseTransparent(true);
        mergedBackdrop.setManaged(false);
        mergedBackdrop.opacityProperty().bind(Bindings.when(mergeWithStage.selectedProperty())
                .then(cameraGuideOpacity.valueProperty().divide(100.0)).otherwise(1.0));
        mergedBackdrop.setFitWidth(frameWidth);
        mergedBackdrop.setFitHeight(frameHeight);
        surface.imageLayer().getChildren().add(mergedBackdrop);
        mergeWithStage.setDisable(context.cameraGuideUri() == null || context.cameraGuideUri().isBlank());
        StudioFormControls.installTooltip(mergeWithStage,
                "Guarda el escenario a color. Las figuras cerradas se rellenan con Fondo; los trazos y colores quedan encima. Cierra las aberturas grandes para rellenarlas.");
        if (restoredInkState != null && !mergeWithStage.isDisabled()) {
            mergeWithStage.setSelected(Boolean.parseBoolean(restoredInkState.metadata().getOrDefault("mergeWithStage", "false")));
        }
        if(restoredInkState!=null) restoredInkState.metadata().entrySet().stream()
                .filter(e->e.getKey().startsWith("regionFill."))
                .sorted(java.util.Map.Entry.comparingByKey()).forEach(e->{
                    try {
                        String[] parts=e.getValue().split(",",3);
                        double x=Double.parseDouble(parts[0]),y=Double.parseDouble(parts[1]);
                        Color.web(parts[2]);
                        if(Double.isFinite(x)&&Double.isFinite(y)) fills.add(new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill(x,y,parts[2]));
                    } catch(RuntimeException invalid) { /* Ignore malformed optional fill metadata. */ }
                });
        mergeWithStage.selectedProperty().addListener((obs, before, active) -> refreshMergedBackdrop());
        strokeSelection = new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection(surface, this::rememberUndo);
        strokeSelection.setOnChanged(this::refreshMergedBackdrop);
        strokeSelection.setFillAccess(() -> List.copyOf(fills), updated -> { fills.clear(); fills.addAll(updated); });
        if (restoredInkState != null) {
            strokeSelection.restoreGroups(restoredInkState.metadata().getOrDefault("strokeGroups", ""));
        }
        selectStrokes.selectedProperty().addListener((obs, before, active) -> {
            cancelActiveInkStroke();
            if(active) fillMode.setSelected(false);
            updateInputMode();
        });
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

        drawMode.textProperty().bind(Bindings.createStringBinding(() ->
                fillMode.isSelected() || selectStrokes.isSelected() ? "Volver a dibujar"
                        : drawMode.isSelected() ? "Panear/dibujar: dibujar" : "Panear/dibujar: panear",
                drawMode.selectedProperty(), fillMode.selectedProperty(), selectStrokes.selectedProperty()));
        drawMode.setOnAction(event -> {
            if (fillMode.isSelected() || selectStrokes.isSelected()) {
                fillMode.setSelected(false);
                selectStrokes.setSelected(false);
                drawMode.setSelected(true);
                updateInputMode();
            }
        });
        drawMode.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (!Boolean.TRUE.equals(newValue)) {
                cancelActiveInkStroke();
            }
            updateInputMode();
            resetInkCoordinateState();
        });
        backgroundColor.setOnAction(event -> {
            if(restoringSketch) return;
            rememberUndo();
            surface.fillBackground(backgroundColor.getValue());
            refreshMergedBackdrop();
        });
        eraser.setGraphic(LucideIconView.of("eraser"));
        eraser.setContentDisplay(ContentDisplay.LEFT);

        Label cameraGuideOpacityLabel = new Label("Transparencia del plano");
        cameraGuideOpacityLabel.visibleProperty().bind(cameraGuideOpacity.visibleProperty());
        cameraGuideOpacityLabel.managedProperty().bind(cameraGuideOpacity.visibleProperty());

        StudioCanvasToolbar tools = new StudioCanvasToolbar("Herramientas del frame teatral");
        FlowPane modeRow = tools.addRow("Modo e historial", drawMode, selectStrokes, fillMode, eraser, undoButton, redoButton,
                clearStrokesButton, clearCanvasButton, mergeWithStage);
        FlowPane selectionRow = tools.addRow("Dibujos seleccionados",
                ActionButtonFactory.secondary("Seleccionar todos", () -> strokeSelection.selectAll()),
                ActionButtonFactory.secondary("Agrupar", () -> strokeSelection.groupSelection()),
                ActionButtonFactory.secondary("Desagrupar", () -> strokeSelection.ungroupSelection()),
                ActionButtonFactory.secondary("Copiar al baúl", this::copySelectionToVault),
                ActionButtonFactory.secondary("Copiar", () -> strokeSelection.copy()),
                ActionButtonFactory.secondary("Pegar", () -> strokeSelection.paste()));
        selectionRow.visibleProperty().bind(selectStrokes.selectedProperty());
        selectionRow.managedProperty().bind(selectionRow.visibleProperty());
        FlowPane appearanceRow = tools.addRow("Apariencia",
                new Label("Lapiz"), penColor,
                new Label("Fondo"), backgroundColor,
                new Label("Grosor"), strokeWidth, penWidthPreview);
        FlowPane viewRow = tools.addRow("Vista",
                new Label("Zoom lienzo"), canvasZoom, canvasZoomValue,
                cameraGuideOpacityLabel, cameraGuideOpacity, cameraGuideOpacityValue,
                pressureIndicator);
        modeRow.getStyleClass().add("technical-problem-mode-tools");
        appearanceRow.getStyleClass().add("technical-problem-drawing-tools");
        viewRow.getStyleClass().add("technical-problem-view-tools");
        tools.getStyleClass().addAll("technical-problem-tools", "technical-problem-tools-flow");
        getDialogPane().addEventFilter(javafx.scene.input.MouseEvent.ANY, event -> {
            if (event.getEventType()!=javafx.scene.input.MouseEvent.MOUSE_MOVED
                    && event.getEventType()!=javafx.scene.input.MouseEvent.MOUSE_PRESSED
                    && event.getEventType()!=javafx.scene.input.MouseEvent.MOUSE_DRAGGED) return;
            boolean blocked=!(event.getTarget() instanceof Node node) || !descendantOf(node,surface);
            if(blocked!=pointerOverControls) {
                pointerOverControls=blocked;
                cancelActiveInkStroke();
                // A hover transition does not change the coordinate transform.
                // Resetting here discarded the native pen calibration unnecessarily.
            }
        });
        refreshMergedBackdrop();
        return tools;
    }

    private void refreshMergedBackdrop() {
        boolean merge = mergeWithStage.isSelected() && !mergeWithStage.isDisabled();
        if (merge && stageImage == null) {
            // Retain the original pixels for UHD frame export; preview rendering
            // below still fits it to the logical 1280x720 theatre canvas.
            stageImage = new Image(context.cameraGuideUri(), false);
            if (stageImage.isError()) {
                mergeWithStage.setSelected(false);
                saveFeedback.setText("No se pudo cargar el escenario; comprueba el archivo del plano.");
                saveFeedback.setVisible(true);
                saveFeedback.setManaged(true);
                stageImage = null;
                return;
            }
        }
        mergedBackdrop.setVisible(merge || !fills.isEmpty());
        surface.fillBackground(merge ? Color.TRANSPARENT : backgroundColor.getValue());
        if (merge || !fills.isEmpty()) mergedBackdrop.setImage(
                com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.render(
                        merge ? stageImage : null, frameWidth, frameHeight, surface.applicationInkStrokes(), backgroundColor.getValue(), fills));
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
        canvasZoom.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (canvasZoomPane != null) canvasZoomPane.setZoom(newValue.doubleValue() / 100.0);
        });
    }

    private void configureCameraGuideOpacity() {
        cameraGuideOpacity.setBlockIncrement(1);
        cameraGuideOpacity.setMajorTickUnit(15);
        cameraGuideOpacity.setMinorTickCount(2);
        cameraGuideOpacity.setShowTickMarks(true);
        cameraGuideOpacity.setSnapToTicks(false);
        cameraGuideOpacity.setPrefWidth(170);
        cameraGuideOpacity.visibleProperty().bind(useCameraGuide.selectedProperty().or(mergeWithStage.selectedProperty()).and(useCameraGuide.disabledProperty().not()));
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

    void installDialogButtonHandlers() {
        Node resolvedSaveButton = getDialogPane().lookupButton(saveButtonType);
        if (resolvedSaveButton instanceof Button button) {
            if (saveButton != null) saveButton.removeEventFilter(ActionEvent.ACTION, saveActionFilter);
            saveButton = button;
            saveButton.removeEventFilter(ActionEvent.ACTION, saveActionFilter);
            saveButton.addEventFilter(ActionEvent.ACTION, saveActionFilter);
        }
        Node resolvedCancelButton = getDialogPane().lookupButton(ButtonType.CANCEL);
        if (resolvedCancelButton instanceof Button button) {
            if (cancelButton != null) cancelButton.removeEventFilter(ActionEvent.ACTION, cancelActionFilter);
            cancelButton = button;
            cancelButton.removeEventFilter(ActionEvent.ACTION, cancelActionFilter);
            cancelButton.addEventFilter(ActionEvent.ACTION, cancelActionFilter);
        }
    }

    private void cancelAndClose() {
        saveAbandoned = true;
        cancelPendingSave();
        setResult(null);
        close();
    }

    private void beginSave() {
        if (saveTask != null || saveCaptureQueued) return;
        installDialogButtonHandlers();
        saveAbandoned = false;
        cancelActiveInkStroke();
        saveCaptureQueued = true;
        setSaving(true, "Guardando frame...");
        // Yield once so the native ButtonBar and progress feedback repaint
        // before the immutable JavaFX snapshot is captured.
        Platform.runLater(this::captureAndStartSave);
    }

    private void captureAndStartSave() {
        saveCaptureQueued = false;
        if (saveAbandoned) return;
        final FrameSaveSnapshot snapshot;
        try {
            snapshot = captureSaveSnapshot();
        } catch (RuntimeException failure) {
            showSaveFailure(failure);
            return;
        }
        Task<Result> task = new Task<>() {
            @Override protected Result call() throws Exception {
                Path target = InkImageFileStore.createTemporaryPng("docupodcast-drawn-frame-");
                pendingSaveTarget.set(target);
                try {
                    if (isCancelled() || saveAbandoned) throw new InterruptedException("Guardado cancelado");
                    writePng(snapshot, target);
                    if (isCancelled() || saveAbandoned) throw new InterruptedException("Guardado cancelado");
                    return new Result(context.segmentId(), target, snapshot.inkState(), snapshot.activateDrawn(),
                            List.copyOf(drawingVault));
                } catch (Exception | Error failure) {
                    InkImageFileStore.deleteTemporaryPng(target);
                    pendingSaveTarget.compareAndSet(target, null);
                    throw failure;
                }
            }
        };
        saveTask = task;
        task.setOnSucceeded(event -> {
            Result saved = task.getValue();
            saveTask = null;
            pendingSaveTarget.set(null);
            if (saveAbandoned || saved == null) {
                if (saved != null) InkImageFileStore.deleteTemporaryPng(saved.framePng());
                return;
            }
            setSaving(false, "");
            setResult(saved);
            close();
        });
        task.setOnFailed(event -> {
            saveTask = null;
            pendingSaveTarget.set(null);
            if (!saveAbandoned) showSaveFailure(task.getException());
        });
        task.setOnCancelled(event -> {
            saveTask = null;
            Path target = pendingSaveTarget.getAndSet(null);
            InkImageFileStore.deleteTemporaryPng(target);
            if (!saveAbandoned) setSaving(false, "");
        });
        Thread worker = Thread.ofVirtual().name("theatre-frame-save").unstarted(task);
        worker.start();
    }

    private void setSaving(boolean saving, String message) {
        if (saveButton != null) saveButton.setDisable(saving);
        if (leftEditorContent != null) leftEditorContent.setDisable(saving);
        if (canvasEditorContent != null) canvasEditorContent.setDisable(saving);
        saveProgress.setVisible(saving);
        saveProgress.setManaged(saving);
        if (saving) saveFeedback.getStyleClass().remove("error");
        saveFeedback.setText(message == null ? "" : message);
        saveFeedback.setVisible(saving || (message != null && !message.isBlank()));
        saveFeedback.setManaged(saveFeedback.isVisible());
    }

    private void showSaveFailure(Throwable failure) {
        String detail = failure == null || failure.getMessage() == null || failure.getMessage().isBlank()
                ? "Error desconocido"
                : failure.getMessage();
        setSaving(false, "No se pudo guardar el frame dibujado: " + detail);
        saveFeedback.getStyleClass().remove("error");
        saveFeedback.getStyleClass().add("error");
    }

    private void cancelPendingSave() {
        Task<Result> current = saveTask;
        if (current != null) current.cancel(true);
        InkImageFileStore.deleteTemporaryPng(pendingSaveTarget.getAndSet(null));
    }

    private void abandonPendingSave() {
        if (saveTask == null && !saveCaptureQueued) return;
        saveAbandoned = true;
        cancelPendingSave();
    }

    private void initializeCanvas() {
        startInkEngine();
        editorSession.attach(inkViewport, new InkInputListener() {
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
        if (!drawMode.isSelected() || selectStrokes.isSelected() || fillMode.isSelected()) {
            return false;
        }
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            strokeActive = false;
            return false;
        }
        rememberUndo();
        strokeActive = true;
        lastInkPoint = point;
        inkEngine.begin(point.getX(), point.getY(), sample == null ? 0L : sample.nanos(), penColor.getValue(),
                strokeWidth.getValue(), eraser.isSelected(), sample == null ? 1.0 : sample.pressure());
        return true;
    }

    private boolean handleStrokeMove(InkInputSample sample) {
        if (!strokeActive || !drawMode.isSelected() || selectStrokes.isSelected() || fillMode.isSelected()) {
            return false;
        }
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            cancelActiveInkStroke();
            return false;
        }
        lastInkPoint = point;
        inkEngine.move(point.getX(), point.getY(), sample == null ? 0L : sample.nanos(), penColor.getValue(),
                strokeWidth.getValue(), eraser.isSelected(), sample == null ? 1.0 : sample.pressure());
        return true;
    }

    private boolean handleStrokeEnd(InkInputSample sample) {
        if (!strokeActive || !drawMode.isSelected() || selectStrokes.isSelected() || fillMode.isSelected()) {
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
        if(inkEngine!=null) inkEngine.cancelActiveStroke();
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
                refreshMergedBackdrop();
            }

            @Override
            public boolean acceptsPoint(double x, double y) {
                return insideCanvas(x, y) && !insideTitleBand(new Point2D(x, y));
            }
        });
        inkEngine.start();
    }

    private Point2D pointInsideCanvas(InkInputSample sample) {
        if (sample == null || (pointerOverControls
                && sample.cursor() == com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor.MOUSE)) {
            return null;
        }
        Point2D point = new Point2D(sample.x(), sample.y());
        if(canvasScroll!=null && surface.getScene()!=null) {
            Node viewport=canvasScroll.lookup(".viewport");
            Point2D scenePoint=surface.localToScene(point);
            if(viewport!=null && !viewport.contains(viewport.sceneToLocal(scenePoint))) return null;
        }
        return insideCanvas(point.getX(), point.getY()) && !insideTitleBand(point) ? point : null;
    }

    private boolean insideCanvas(double x, double y) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && x >= 0.0
                && y >= 0.0
                && x <= frameWidth
                && y <= frameHeight;
    }

    private boolean insideTitleBand(Point2D point) {
        return frameTitleState.visible()
                && point != null
                && point.getY() >= 0.0
                && point.getY() <= frameTitleState.bandHeight();
    }

    private void updateInputMode() {
        boolean selecting = selectStrokes.isSelected();
        boolean filling = fillMode.isSelected();
        boolean inkActive = drawMode.isSelected() && !selecting && !filling;
        if (canvasScroll != null) {
            canvasScroll.setPannable(!inkActive && !selecting && !filling);
        }
        surface.configureInputCapture(inkActive || selecting || filling, inkActive);
        fillTarget.setWidth(frameWidth); fillTarget.setHeight(frameHeight);
        fillTarget.setVisible(filling); fillTarget.setMouseTransparent(!filling);
        if (strokeSelection != null) strokeSelection.setActive(selecting);
        if (!inkActive) {
            resetInkCoordinateState();
        }
    }

    private void resetInkCoordinateState() {
        editorSession.resetInputCoordinates();
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
            surface.resetForFixedEditableState(frameWidth, frameHeight, restoredBackground);
            surface.restoreApplicationInkStrokes(restoredInkState.strokes());
            surface.setFixedLogicalViewport(frameWidth, frameHeight);
            return;
        }
        String uri = context.drawnFrameUri();
        if (uri.isBlank()) {
            return;
        }
        Platform.runLater(() -> {
            Image image = new Image(uri, frameWidth, frameHeight, false, true, false);
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return;
            }
            surface.drawBackgroundImage(toWritable(image), backgroundColor.getValue());
            surface.setFixedLogicalViewport(frameWidth, frameHeight);
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
        editorSession.checkpoint();
    }

    private record SketchState(List<InkCanvasSurface.InkStrokeState> strokes,
                               List<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill> fills,
                               Color background) {}
    private SketchState sketchState() { return new SketchState(surface.inkStrokeStates(),List.copyOf(fills),backgroundColor.getValue()); }
    private void restoreSketchState(SketchState state) {
        restoringSketch=true;
        try {
            surface.restoreInkUndoState(state.strokes(),List.of());
            fills.clear(); fills.addAll(state.fills());
            backgroundColor.setValue(state.background());
        } finally { restoringSketch=false; }
        refreshMergedBackdrop();
    }

    private void undo() {
        editorSession.undo();
        if (strokeSelection != null) strokeSelection.refresh();
        refreshMergedBackdrop();
    }

    private void redo() {
        editorSession.redo();
        if (strokeSelection != null) strokeSelection.refresh();
        refreshMergedBackdrop();
    }

    private void clearInkStrokes() {
        rememberUndo();
        fills.clear();
        surface.clearStrokes();
        refreshMergedBackdrop();
    }

    private void clearCanvas() {
        rememberUndo();
        fills.clear();
        surface.resetForFixedEditableState(frameWidth, frameHeight, backgroundColor.getValue());
        updateInputMode();
        updateCanvasTitle();
        refreshMergedBackdrop();
    }

    private FrameSaveSnapshot captureSaveSnapshot() {
        updateCanvasTitle();
        WritableImage image = exportFrameImage();
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        PixelReader reader = image.getPixelReader();
        if (reader == null) throw new IllegalStateException("El lienzo no expone pixeles para guardar.");
        int[] pixels = new int[Math.multiplyExact(width, height)];
        reader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("consumer", "theatre.storyboard-frame");
        metadata.put("mergeWithStage", Boolean.toString(mergeWithStage.isSelected()));
        addFillMetadata(metadata, fills);
        metadata.put("strokeGroups", strokeSelection == null ? "" : strokeSelection.groupsMetadata());
        metadata.put("segmentId", context.segmentId());
        metadata.put("interventionId", context.interventionId());
        metadata.put(METADATA_SHOW_FRAGMENT_TITLE, Boolean.toString(frameTitleState.enabled()));
        metadata.put(METADATA_FRAGMENT_TITLE_TEXT, frameTitleState.text());
        metadata.put(METADATA_TITLE_BAND_HEIGHT, String.format(Locale.ROOT, "%.3f", frameTitleState.bandHeight()));
        metadata.put(METADATA_TITLE_MARGIN, String.format(Locale.ROOT, "%.3f", frameTitleState.margin()));
        String inkState = InkWorkspaceStateSerializer.toJson(InkWorkspaceState.create(
                surface.logicalWidth(), surface.logicalHeight(), colorToHex(backgroundColor.getValue()),
                surface.applicationInkStrokes(), List.of(), metadata));
        return new FrameSaveSnapshot(width, height, pixels, surface.backgroundColor(), frameTitleState,
                inkState, activateDrawn.isSelected());
    }

    private WritableImage exportFrameImage() {
        List<ImageView> exportImages = List.of();
        if (mergedBackdrop.isVisible()) {
            Image exportBackdrop = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.renderScaled(
                    mergeWithStage.isSelected() ? stageImage : null,
                    frameWidth, frameHeight, surface.applicationInkStrokes(),
                    backgroundColor.getValue(), fills, exportScale);
            ImageView exportView = new ImageView(exportBackdrop);
            exportView.setFitWidth(frameWidth);
            exportView.setFitHeight(frameHeight);
            exportView.setPreserveRatio(false);
            exportImages = List.of(exportView);
        }
        return surface.exportWithImages(exportImages, new InkCanvasExportOptions(
                exportScale,
                24_000_000L,
                false,
                0,
                frameWidth,
                frameHeight)).image();
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

    private void writePng(FrameSaveSnapshot snapshot, Path target) throws IOException, InterruptedException {
        int width = snapshot.width();
        int height = snapshot.height();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Guardado cancelado");
        BufferedImage overlay = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        overlay.setRGB(0, 0, width, height, snapshot.argb(), 0, width);
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = buffered.createGraphics();
        try {
            graphics.setColor(toAwtColor(snapshot.background()));
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(overlay, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Guardado cancelado");
        graphics = buffered.createGraphics();
        try {
            drawFrameTitle(graphics, width, height, snapshot.title());
        } finally {
            graphics.dispose();
        }
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Guardado cancelado");
        InkImageFileStore.writePng(buffered, target);
    }

    private void drawFrameTitle(Graphics2D graphics, int width, int height, FrameTitleState title) {
        if (graphics == null || title == null || !title.visible()) {
            return;
        }
        double scaleX = width / frameWidth;
        double scaleY = height / frameHeight;
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
        pressureIndicator.close();
        editorSession.close();
        if (inkEngine != null) {
            inkEngine.stop();
            inkEngine = null;
        }
        surface.releasePreviewResources();
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

    private static void addFillMetadata(Map<String, String> metadata,
                                        List<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill> source) {
        for (int i = 0; i < source.size(); i++) {
            var fill = source.get(i);
            metadata.put(String.format(Locale.ROOT, "regionFill.%08d", i),
                    fill.x() + "," + fill.y() + "," + fill.color());
        }
    }

    private static List<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill> fillsFromMetadata(
            Map<String, String> metadata) {
        ArrayList<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill> parsed = new ArrayList<>();
        if (metadata == null) return List.of();
        metadata.entrySet().stream().filter(entry -> entry.getKey().startsWith("regionFill."))
                .sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    try {
                        String[] parts = entry.getValue().split(",", 3);
                        double x = Double.parseDouble(parts[0]);
                        double y = Double.parseDouble(parts[1]);
                        Color.web(parts[2]);
                        if (Double.isFinite(x) && Double.isFinite(y)) parsed.add(
                                new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdrop.Fill(x, y, parts[2]));
                    } catch (RuntimeException ignored) { }
                });
        return List.copyOf(parsed);
    }

    private FrameTitleState initialTitleState() {
        if (restoredInkState == null) {
            return FrameTitleState.create(false, context.interventionText(), frameWidth);
        }
        Map<String, String> metadata = restoredInkState.metadata();
        boolean enabled = Boolean.parseBoolean(metadata.getOrDefault(METADATA_SHOW_FRAGMENT_TITLE, "false"));
        String text = metadata.getOrDefault(METADATA_FRAGMENT_TITLE_TEXT, context.interventionText());
        return FrameTitleState.create(enabled, text, frameWidth);
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

    private record FrameSaveSnapshot(int width, int height, int[] argb, Color background,
                                     FrameTitleState title, String inkState, boolean activateDrawn) {
        private FrameSaveSnapshot {
            argb = argb == null ? new int[0] : argb;
            inkState = inkState == null ? "" : inkState;
        }
    }

    public record Result(String segmentId, Path framePng, String inkStateJson, boolean activateDrawn,
                         List<TheatreDrawingVaultItem> drawingVault) {
        public Result {
            drawingVault = drawingVault == null ? List.of() : List.copyOf(drawingVault);
        }
    }
}
