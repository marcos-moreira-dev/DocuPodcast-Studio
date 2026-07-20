package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemDetail;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceProjection;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkImageCrop;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPlacedImage;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.LucideIconView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.ink.InkRealtimeStrokeEngine;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasViewportCoordinateMapper;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.NoopInkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Modal resolver for technical problems. */
public final class TechnicalProblemDialog {
    private static final double DIALOG_PREF_WIDTH = 1220;
    private static final double DIALOG_PREF_HEIGHT = 760;
    private static final double CANVAS_TITLE_BAND_HEIGHT = 64.0;

    public record TechnicalProblemResult(String title, String solutionText, WritableImage canvasSnapshot,
                                         Map<String, Path> sourceCropPaths, String notes, Path externalPngTarget,
                                         WritableImage externalCanvasSnapshot, List<String> exportWarnings,
                                         String canvasStateJson, List<StudyProblemSourceDraft> additionalSources) {
        public TechnicalProblemResult {
            exportWarnings = exportWarnings == null ? List.of() : List.copyOf(exportWarnings);
            canvasStateJson = canvasStateJson == null ? "" : canvasStateJson;
            additionalSources = additionalSources == null ? List.of() : List.copyOf(additionalSources);
        }
    }

    private final Dialog<TechnicalProblemResult> dialog = new Dialog<>();
    private final DrawingProfile drawingProfile;
    private final List<StatementSource> statementSources;
    private final List<StudyProblemSourceDraft> additionalSourceDrafts = new ArrayList<>();
    private final Map<String, Path> sourceCropPaths;
    private final Path existingSolutionImagePath;
    private final Path existingCanvasStatePath;
    private final boolean editMode;
    private final boolean expressMode;
    private final TextField title = new TextField();
    private final TextArea solutionText = new TextArea();
    private final TextArea notes = new TextArea();
    private final StudyProblemCanvasSurface drawingSurface = new StudyProblemCanvasSurface();
    private final InkInputProvider inkInputProvider;
    private final ColorPicker penColor = new ColorPicker(Color.BLACK);
    private final ColorPicker backgroundColor = new ColorPicker(Color.WHITE);
    private final Slider penWidth = new Slider(1, 24, 3);
    private final Circle penWidthPreview = new Circle(3);
    private final Slider canvasZoom = new Slider(50, 200, 100);
    private final Label canvasZoomValue = new Label("100%");
    private final ToggleButton eraser = new ToggleButton("Borrador");
    private final Deque<CanvasUndoSnapshot> undo = new ArrayDeque<>();
    private final Deque<CanvasUndoSnapshot> redo = new ArrayDeque<>();
    private final BooleanProperty canvasTouched = new SimpleBooleanProperty(false);
    private final BooleanProperty canvasMode = new SimpleBooleanProperty(true);
    private final BooleanProperty drawMode = new SimpleBooleanProperty(true);
    private final BooleanProperty canvasRegionSelectionMode = new SimpleBooleanProperty(false);
    private final BooleanProperty imageInteractionMode = new SimpleBooleanProperty(false);
    private final List<CanvasImageItem> canvasImages = new ArrayList<>();
    private final List<Label> imageResizeHandles = new ArrayList<>();
    private SplitPane problemSplit;
    private BorderPane rootPane;
    private Node statementNode;
    private Node resolverNode;
    private Node modeBarNode;
    private Node existingSolutionNode;
    private Node toolBarNode;
    private Node titleNode;
    private Label inputDiagnosticLabel;
    private VBox statementVisuals;
    private TextArea statementTextArea;
    private Button transferAllButton;
    private HBox statementActions;
    private Label canvasTitleLabel;
    private ScrollPane canvasScroll;
    private Button collapseStatementButton;
    private Button restoreStatementButton;
    private Button fullscreenButton;
    private Button deleteImageButton;
    private Button enlargeImageButton;
    private Button shrinkImageButton;
    private Button cropImageButton;
    private Button restoreImageCropButton;
    private Button copyRegionButton;
    private Button pasteRegionButton;
    private Button deleteRegionButton;
    private Button moveRegionButton;
    private ToggleButton selectRegionButton;
    private CanvasImageItem selectedCanvasImage;
    private Rectangle canvasRegionSelectionRectangle;
    private CanvasRegionSelection activeCanvasRegionSelection;
    private WritableImage canvasRegionClipboard;
    private Point2D lastCanvasPointer = new Point2D(96, CANVAS_TITLE_BAND_HEIGHT + 96);
    private double canvasSelectionStartX;
    private double canvasSelectionStartY;
    private boolean canvasGrowScheduled;
    private boolean suppressCanvasGrowEvents;
    private boolean pendingCanvasGrowWidth;
    private boolean pendingCanvasGrowHeight;
    private final PauseTransition canvasGrowDebounce = new PauseTransition(Duration.millis(160));
    private boolean statementCollapsed;
    private boolean resolverFullscreen;
    private boolean statementCollapsedBeforeFullscreen;
    private boolean stageMaximizedBeforeFullscreen;
    private double stageXBeforeFullscreen;
    private double stageYBeforeFullscreen;
    private double stageWidthBeforeFullscreen;
    private double stageHeightBeforeFullscreen;
    private double restoredDivider = 0.38;
    private InkRealtimeStrokeEngine inkEngine;
    private double currentInputPressure = 1.0;
    private double currentInputRawPressure = Double.NaN;
    private double lastComparableRawPressure = Double.NaN;
    private boolean inputPressureVaried;
    private InkInputCursor currentInputCursor = InkInputCursor.UNKNOWN;
    private String currentInputSource = "";
    private boolean currentInputEraser;
    private boolean imageCropMode;
    private Rectangle imageCropSelectionRectangle;
    private double imageCropStartX;
    private double imageCropStartY;
    private String dialogHeaderText = "";
    private ButtonType saveButtonType;
    private ButtonType saveAndExportButtonType;
    private Path externalPngTarget;

    private TechnicalProblemDialog(Window owner, List<DocumentBlock> sourceBlocks, Map<String, Path> sourceCropPaths,
                                   InkInputProvider inkInputProvider, DrawingProfile drawingProfile) {
        this.drawingProfile = java.util.Objects.requireNonNull(drawingProfile, "drawing profile");
        this.inkInputProvider = inkInputProvider == null ? NoopInkInputProvider.INSTANCE : inkInputProvider;
        this.statementSources = new ArrayList<>(statementSources(sourceBlocks, sourceCropPaths));
        this.sourceCropPaths = sourceCropPaths == null ? Map.of() : Map.copyOf(sourceCropPaths);
        this.existingSolutionImagePath = null;
        this.existingCanvasStatePath = null;
        this.editMode = false;
        this.expressMode = false;
        initialize(owner, "Problema tecnico", "Problema tecnico", "Guardar");
    }

    private TechnicalProblemDialog(Window owner, List<StudyProblemSourceDraft> sourceDrafts, boolean draftMode,
                                   InkInputProvider inkInputProvider, DrawingProfile drawingProfile) {
        this.drawingProfile = java.util.Objects.requireNonNull(drawingProfile, "drawing profile");
        this.inkInputProvider = inkInputProvider == null ? NoopInkInputProvider.INSTANCE : inkInputProvider;
        this.statementSources = new ArrayList<>(statementSourcesFromDrafts(sourceDrafts));
        this.sourceCropPaths = sourceCropPathsFromDrafts(sourceDrafts);
        this.existingSolutionImagePath = null;
        this.existingCanvasStatePath = null;
        this.editMode = false;
        this.expressMode = false;
        initialize(owner, "Problema tecnico", "Problema tecnico", "Guardar");
    }

    private TechnicalProblemDialog(Window owner, StudyProblemDetail detail, InkInputProvider inkInputProvider,
                                   DrawingProfile drawingProfile) {
        this.drawingProfile = java.util.Objects.requireNonNull(drawingProfile, "drawing profile");
        this.inkInputProvider = inkInputProvider == null ? NoopInkInputProvider.INSTANCE : inkInputProvider;
        this.statementSources = new ArrayList<>(statementSources(detail));
        this.sourceCropPaths = Map.of();
        this.existingSolutionImagePath = detail == null ? null : detail.solutionImagePath();
        this.existingCanvasStatePath = detail == null ? null : detail.canvasStatePath();
        this.editMode = true;
        this.expressMode = false;
        initialize(owner, "Problema tecnico", "Problema tecnico", "Guardar cambios");
        if (detail != null) {
            title.setEditable(false);
            title.setText(detail.title());
            solutionText.setText(detail.solutionText());
            notes.setText(detail.notes());
        }
    }

    private TechnicalProblemDialog(Window owner, boolean expressMode, InkInputProvider inkInputProvider,
                                   DrawingProfile drawingProfile) {
        this.drawingProfile = java.util.Objects.requireNonNull(drawingProfile, "drawing profile");
        this.inkInputProvider = inkInputProvider == null ? NoopInkInputProvider.INSTANCE : inkInputProvider;
        this.statementSources = new ArrayList<>();
        this.sourceCropPaths = Map.of();
        this.existingSolutionImagePath = null;
        this.existingCanvasStatePath = null;
        this.editMode = false;
        this.expressMode = expressMode;
        initialize(owner, "Problema Técnico Express", "Problema Técnico Express", "Cerrar");
    }

    private void initialize(Window owner, String dialogTitle, String header, String saveLabel) {
        dialog.setTitle(dialogTitle);
        dialog.setHeaderText(header);
        dialogHeaderText = header == null ? "" : header;
        if (owner != null) {
            dialog.initOwner(owner);
        }
        saveButtonType = new ButtonType(saveLabel, ButtonBar.ButtonData.OK_DONE);
        if (editMode) {
            saveAndExportButtonType = null;
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        } else {
            saveAndExportButtonType = new ButtonType(expressMode ? "Exportar PNG..." : "Guardar + exportar PNG...", ButtonBar.ButtonData.APPLY);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, saveAndExportButtonType, ButtonType.CANCEL);
        }
        dialog.getDialogPane().getStyleClass().add("technical-problem-dialog");
        dialog.getDialogPane().setContent(content());
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(DIALOG_PREF_WIDTH, DIALOG_PREF_HEIGHT);
        dialog.getDialogPane().setMinSize(900, 560);
        dialog.setResultConverter(button -> button == saveButtonType || (saveAndExportButtonType != null && button == saveAndExportButtonType)
                ? resultFor(button)
                : null);
        dialog.setOnShown(event -> {
            Window window = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
            if (window instanceof javafx.stage.Stage stage) {
                stage.setMinWidth(940);
                stage.setMinHeight(600);
                stage.setResizable(true);
            }
            installDialogButtonHandlers();
            Platform.runLater(this::resetInkCoordinateState);
        });
        dialog.setOnHidden(event -> disposeCanvasInput());
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, this::handleDialogKeyPressed);
        canvasGrowDebounce.setOnFinished(event -> runPendingCanvasGrowth());
        initializeCanvas();
    }

    public static Optional<TechnicalProblemResult> show(Window owner, List<DocumentBlock> sourceBlocks,
                                                        Map<String, Path> sourceCropPaths,
                                                        InkInputProvider inputProvider,
                                                        DrawingProfile drawingProfile) {
        return new TechnicalProblemDialog(owner, sourceBlocks, sourceCropPaths, inputProvider, drawingProfile).dialog.showAndWait();
    }

    public static Optional<TechnicalProblemResult> showForDrafts(Window owner,
                                                                 List<StudyProblemSourceDraft> sourceDrafts,
                                                                 InkInputProvider inputProvider,
                                                                 DrawingProfile drawingProfile) {
        return new TechnicalProblemDialog(owner, sourceDrafts, true, inputProvider, drawingProfile).dialog.showAndWait();
    }

    public static Optional<TechnicalProblemResult> showForEdit(Window owner, StudyProblemDetail detail,
                                                               InkInputProvider inputProvider,
                                                               DrawingProfile drawingProfile) {
        return new TechnicalProblemDialog(owner, detail, inputProvider, drawingProfile).dialog.showAndWait();
    }

    public static Optional<TechnicalProblemResult> showExpress(Window owner, InkInputProvider inputProvider,
                                                               DrawingProfile drawingProfile) {
        return new TechnicalProblemDialog(owner, true, inputProvider, drawingProfile).dialog.showAndWait();
    }

    private boolean shouldExportCanvas(ButtonType button) {
        return canvasTouched.get() || (saveAndExportButtonType != null && button == saveAndExportButtonType) || externalPngTarget != null;
    }

    private TechnicalProblemResult resultFor(ButtonType button) {
        flushInk();
        markTitleContentBounds();
        boolean needsCanvas = shouldExportCanvas(button);
        boolean externalRequested = saveAndExportButtonType != null && button == saveAndExportButtonType && externalPngTarget != null;
        InkCanvasExportResult internalExport = needsCanvas ? exportCanvas(InkCanvasExportOptions.internalPersistence()) : null;
        InkCanvasExportResult externalExport = externalRequested ? exportCanvas(InkCanvasExportOptions.premiumExternal()) : null;
        String canvasStateJson = shouldPersistCanvasState(needsCanvas) ? canvasStateJson() : "";
        List<String> warnings = new ArrayList<>();
        if (internalExport != null) {
            warnings.addAll(internalExport.warnings());
        }
        if (externalExport != null) {
            warnings.addAll(externalExport.warnings());
        }
        return new TechnicalProblemResult(
                title.getText(),
                solutionText.getText(),
                internalExport == null ? null : internalExport.image(),
                this.sourceCropPaths,
                notes.getText(),
                externalPngTarget,
                externalExport == null ? null : externalExport.image(),
                warnings,
                canvasStateJson,
                additionalSourceDrafts);
    }

    private void installDialogButtonHandlers() {
        if (saveAndExportButtonType == null) {
            return;
        }
        Node exportButton = dialog.getDialogPane().lookupButton(saveAndExportButtonType);
        if (exportButton == null || Boolean.TRUE.equals(exportButton.getProperties().get("technicalProblemExportHandlerInstalled"))) {
            return;
        }
        exportButton.getProperties().put("technicalProblemExportHandlerInstalled", Boolean.TRUE);
        exportButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Path target = chooseExternalPngTarget();
            if (target == null) {
                event.consume();
                return;
            }
            externalPngTarget = target;
            canvasTouched.set(true);
        });
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        if (saveButton != null) {
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> externalPngTarget = null);
        }
    }

    private void handleDialogKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE && resolverFullscreen) {
            toggleResolverFullscreen();
            event.consume();
            return;
        }
        if (!event.isShortcutDown() || event.isAltDown() || textInputOwnsShortcut(event)) {
            return;
        }
        if (event.getCode() == KeyCode.Z) {
            undo();
            event.consume();
        } else if (event.getCode() == KeyCode.Y) {
            redo();
            event.consume();
        }
    }

    private boolean textInputOwnsShortcut(KeyEvent event) {
        if (event.getTarget() instanceof Node target && isTextInputNode(target)) {
            return true;
        }
        Node focusOwner = dialog.getDialogPane().getScene() == null
                ? null
                : dialog.getDialogPane().getScene().getFocusOwner();
        return focusOwner != null && isTextInputNode(focusOwner);
    }

    private static boolean isTextInputNode(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof TextInputControl) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Path chooseExternalPngTarget() {
        Window owner = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar solucion PNG");
        chooser.setInitialFileName(safePngFileName(title.getText()));
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"));
        File file = chooser.showSaveDialog(owner);
        return file == null ? null : withPngExtension(file.toPath());
    }

    private static String safePngFileName(String value) {
        String base = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return (base.isBlank() ? "problema-tecnico" : base) + "-solucion.png";
    }

    private static Path withPngExtension(Path path) {
        if (path == null || path.getFileName() == null) {
            return path;
        }
        String name = path.getFileName().toString();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            return path;
        }
        Path parent = path.getParent();
        Path next = Path.of(name + ".png");
        return parent == null ? next : parent.resolve(next);
    }

    private BorderPane content() {
        title.setPromptText("Titulo del problema");
        if (!editMode) {
            title.setText(expressMode ? "Problema Técnico Express" : defaultTitle());
        }
        title.getStyleClass().add("technical-problem-title-field");

        BorderPane root = new BorderPane();
        rootPane = root;
        root.getStyleClass().add("technical-problem-root");
        Label titleLabel = new Label("Titulo del problema");
        titleLabel.getStyleClass().add("technical-problem-section-title");
        VBox titleBox = new VBox(4, titleLabel, title);
        titleBox.getStyleClass().add("technical-problem-title-box");
        titleNode = titleBox;
        root.setTop(titleBox);
        BorderPane.setMargin(titleBox, new Insets(0, 0, 10, 0));

        statementNode = statementPane();
        resolverNode = resolverPane();
        problemSplit = new SplitPane(statementNode, resolverNode);
        problemSplit.getStyleClass().add("technical-problem-split");
        problemSplit.setDividerPositions(restoredDivider);
        SplitPane.setResizableWithParent(statementNode, Boolean.TRUE);
        SplitPane.setResizableWithParent(resolverNode, Boolean.TRUE);
        root.setCenter(problemSplit);
        return root;
    }

    private BorderPane statementPane() {
        Label heading = new Label(expressMode ? "Banco de imagenes" : "Enunciado");
        heading.getStyleClass().add("technical-problem-section-title");
        collapseStatementButton = ActionButtonFactory.secondary(
                expressMode ? "Ocultar banco" : "Ocultar enunciado",
                expressMode ? "Ocultar el banco de imagenes y dejar mas espacio para dibujar." : "Ocultar el enunciado y dejar mas espacio para resolver.",
                this::toggleStatementCollapsed);
        Button loadExternalImage = ActionButtonFactory.secondary(
                "Cargar imagen externa",
                expressMode ? "Agregar una imagen al banco de referencia." : "Agregar una imagen del disco como fuente visual del problema.",
                this::loadExternalImage);
        statementActions = new HBox(8, loadExternalImage);
        if (hasTransferableSourceImages()) {
            transferAllButton = ActionButtonFactory.secondary(
                    "Transferir todas al lienzo",
                    "Colocar todas las capturas e imagenes del enunciado en el lienzo.",
                    this::transferAllSourceImages);
            transferAllButton.getStyleClass().add("technical-problem-transfer-all");
            statementActions.getChildren().add(transferAllButton);
        }
        statementActions.getChildren().add(collapseStatementButton);
        statementActions.setAlignment(Pos.CENTER_LEFT);
        statementActions.getStyleClass().add("technical-problem-statement-actions");
        HBox header = new HBox(8, heading);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("technical-problem-statement-header");
        VBox headerBox = new VBox(6, statementActions, header);
        headerBox.getStyleClass().add("technical-problem-statement-header-box");

        statementTextArea = new TextArea(expressMode
                ? "Importa imagenes para usarlas como referencia o transferirlas al lienzo."
                : combinedStatementText());
        statementTextArea.setEditable(false);
        statementTextArea.setWrapText(true);
        statementTextArea.getStyleClass().add("technical-problem-statement-text");
        statementTextArea.setMinHeight(180);
        statementVisuals = new VBox(10);
        statementSources.stream()
                .map(this::sourceVisualNode)
                .flatMap(Optional::stream)
                .forEach(statementVisuals.getChildren()::add);
        VBox content = new VBox(10, statementTextArea, statementVisuals);
        content.getStyleClass().add("technical-problem-statement");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(460);
        scroll.setPrefViewportHeight(560);
        scroll.getStyleClass().add("technical-problem-statement-scroll");
        BorderPane pane = new BorderPane(scroll);
        pane.setTop(headerBox);
        pane.getStyleClass().add("technical-problem-statement-pane");
        return pane;
    }

    private void loadExternalImage() {
        Window owner = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Cargar imagenes externas");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        List<File> files = chooser.showOpenMultipleDialog(owner);
        if (files == null || files.isEmpty()) {
            return;
        }
        int loaded = 0;
        List<String> failures = new ArrayList<>();
        for (File file : files) {
            try {
                Path temp = copyExternalImageAsPng(file.toPath());
                String id = "EXTIMG-" + String.format(java.util.Locale.ROOT, "%04d", additionalSourceDrafts.size() + 1);
                StudyProblemSourceDraft draft = StudyProblemSourceDraft.externalImage(id, temp);
                StatementSource source = new StatementSource(draft.sourceId(), "Imagen externa del problema.",
                        "", temp, "");
                additionalSourceDrafts.add(draft);
                statementSources.add(source);
                if (statementVisuals != null) {
                    sourceVisualNode(source).ifPresent(statementVisuals.getChildren()::add);
                }
                loaded++;
            } catch (IOException | RuntimeException ex) {
                failures.add(file.getName() + ": " + ex.getMessage());
            }
        }
        if (statementTextArea != null) {
            statementTextArea.setText(combinedStatementText());
        }
        if (loaded > 0) {
            ensureTransferAllButtonVisible();
        }
        if (failures.isEmpty()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING, "", ButtonType.OK);
        alert.setTitle("Cargar imagenes externas");
        alert.setHeaderText("Algunas imagenes no estan disponibles");
        Label message = new Label("Se cargaron " + loaded + " imagen(es). No se pudieron cargar:\n"
                + String.join("\n", failures));
        message.setWrapText(true);
        message.setPrefWidth(480);
        alert.getDialogPane().setContent(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private static Path copyExternalImageAsPng(Path source) throws IOException {
        BufferedImage input = ImageIO.read(source.toFile());
        if (input == null || input.getWidth() <= 0 || input.getHeight() <= 0) {
            throw new IOException("El archivo no parece ser una imagen compatible.");
        }
        Path temp = Files.createTempFile("docupodcast-study-external-", ".png");
        if (!ImageIO.write(input, "png", temp.toFile())) {
            throw new IOException("No se pudo convertir la imagen a PNG.");
        }
        return temp.toAbsolutePath().normalize();
    }

    private void ensureTransferAllButtonVisible() {
        if (transferAllButton != null || statementActions == null) {
            return;
        }
        transferAllButton = ActionButtonFactory.secondary(
                "Transferir todas al lienzo",
                "Colocar todas las capturas e imagenes del enunciado en el lienzo.",
                this::transferAllSourceImages);
        transferAllButton.getStyleClass().add("technical-problem-transfer-all");
        int index = Math.max(0, statementActions.getChildren().size() - 1);
        statementActions.getChildren().add(index, transferAllButton);
    }

    private Optional<Node> sourceVisualNode(StatementSource source) {
        Image image = sourceImage(source);
        if (image == null || image.isError()) {
            return Optional.empty();
        }
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setManaged(true);
        imageView.getStyleClass().add("technical-problem-source-crop");
        Button transfer = ActionButtonFactory.secondary(
                "Transferir imagen al lienzo",
                "Colocar esta imagen en el lienzo para moverla, escalarla o escribir encima.",
                () -> transferSourceImage(image));
        VBox box = new VBox(7, imageView, transfer);
        imageView.fitWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(180.0, Math.min(720.0, box.getWidth() <= 0 ? 560.0 : box.getWidth() - 18.0)),
                box.widthProperty()));
        box.getStyleClass().add("technical-problem-source-image-box");
        return Optional.of(box);
    }

    private Image sourceImage(StatementSource source) {
        if (source == null) {
            return null;
        }
        if (source.cropPath() != null && Files.isRegularFile(source.cropPath())) {
            return new Image(source.cropPath().toUri().toString(), true);
        }
        if (source.imageBase64() == null || source.imageBase64().isBlank()) {
            return null;
        }
        try {
            return new Image(new ByteArrayInputStream(Base64.getDecoder().decode(source.imageBase64())));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private VBox resolverPane() {
        ToggleButton modeSwitch = StudioFormControls.toggle("Lienzo/texto: lienzo",
                "Cambiar entre escritura textual y lienzo manuscrito para resolver el problema.");
        modeSwitch.selectedProperty().bindBidirectional(canvasMode);
        modeSwitch.textProperty().bind(javafx.beans.binding.Bindings.when(canvasMode)
                .then("Lienzo/texto: lienzo")
                .otherwise("Lienzo/texto: texto"));
        ToggleButton drawSwitch = StudioFormControls.toggle("Panear/dibujar: dibujar",
                "Alternar entre dibujar trazos y panear el lienzo sin dibujar.");
        drawSwitch.selectedProperty().bindBidirectional(drawMode);
        drawSwitch.textProperty().bind(javafx.beans.binding.Bindings.when(drawMode)
                .then("Panear/dibujar: dibujar")
                .otherwise("Panear/dibujar: panear"));
        canvasMode.addListener((obs, oldValue, newValue) -> updateImageInteractionMode());
        drawMode.addListener((obs, oldValue, newValue) -> {
            if (!Boolean.TRUE.equals(newValue)) {
                imageInteractionMode.set(false);
            } else if (canvasRegionSelectionMode.get()) {
                canvasRegionSelectionMode.set(false);
            }
            updateImageInteractionMode();
            resetInkCoordinateState();
        });
        canvasRegionSelectionMode.addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                drawMode.set(false);
                imageInteractionMode.set(false);
                selectCanvasImage(null);
            } else {
                hideCanvasRegionSelection();
            }
            updateCanvasScrollMode();
            updateImageInteractionMode();
            updateCanvasRegionButtons();
        });
        fullscreenButton = ActionButtonFactory.secondary(
                "Pantalla completa",
                "Maximizar el area de solucion y ocultar temporalmente el enunciado.",
                this::toggleResolverFullscreen);
        selectRegionButton = StudioFormControls.toggle("Seleccionar region",
                "Seleccionar una zona del lienzo para copiar, mover o eliminar tinta e imagenes.");
        selectRegionButton.selectedProperty().bindBidirectional(canvasRegionSelectionMode);
        restoreStatementButton = ActionButtonFactory.secondary(
                "Mostrar enunciado",
                "Restaurar el panel del enunciado.",
                this::toggleStatementCollapsed);
        restoreStatementButton.getStyleClass().add("technical-problem-statement-restore");
        setNodeVisible(restoreStatementButton, false);
        modeSwitch.setMinWidth(168);
        drawSwitch.setMinWidth(188);
        selectRegionButton.setMinWidth(156);
        fullscreenButton.setMinWidth(156);
        restoreStatementButton.setMinWidth(156);
        inputDiagnosticLabel = new Label(inputDiagnosticText());
        inputDiagnosticLabel.getStyleClass().add("technical-problem-input-diagnostic");
        StudioFormControls.installTooltip(inputDiagnosticLabel, inputDiagnosticTooltip());
        FlowPane mode = new FlowPane(8, 8, modeSwitch, drawSwitch, selectRegionButton,
                fullscreenButton, restoreStatementButton, inputDiagnosticLabel);
        mode.setAlignment(Pos.CENTER_LEFT);
        mode.getStyleClass().addAll("technical-problem-mode-toggle", "technical-problem-mode-flow");
        modeBarNode = mode;

        solutionText.setWrapText(true);
        solutionText.setPromptText("Escribe la solucion, notas o pasos algebraicos aqui.");
        solutionText.getStyleClass().add("technical-problem-text-solution");
        notes.setWrapText(true);
        notes.setPromptText("Notas internas del estudio.");
        notes.setPrefRowCount(3);
        notes.getStyleClass().add("technical-problem-notes");

        ScrollPane canvasScroll = canvasPane();
        StackPane resolverStack = new StackPane(solutionText, canvasScroll);
        VBox.setVgrow(resolverStack, Priority.ALWAYS);
        solutionText.visibleProperty().bind(canvasMode.not());
        solutionText.managedProperty().bind(solutionText.visibleProperty());
        canvasScroll.visibleProperty().bind(canvasMode);
        canvasScroll.managedProperty().bind(canvasScroll.visibleProperty());

        existingSolutionNode = existingSolutionImageNode();
        toolBarNode = toolBar();
        VBox resolver = new VBox(8, mode, existingSolutionNode, toolBarNode, resolverStack, notes);
        resolver.getStyleClass().add("technical-problem-resolver");
        resolver.setPrefWidth(620);
        resolver.setMinWidth(420);
        return resolver;
    }

    private String inputDiagnosticText() {
        InkInputCapabilities capabilities = inkInputProvider.capabilities();
        String provider = capabilities.providerName() == null || capabilities.providerName().isBlank()
                ? "Proveedor de tinta desconocido"
                : capabilities.providerName();
        if (!currentInputSource.isBlank()) {
            if (currentInputSource.startsWith("LectureStudio") || currentInputSource.startsWith("Windows Pointer")) {
                if (currentInputCursor == InkInputCursor.MOUSE) {
                    return "Entrada: " + currentInputSource + " MOUSE - sin presion variable";
                }
                if (currentInputCursor == InkInputCursor.PEN || currentInputCursor == InkInputCursor.ERASER) {
                    String cursor = currentInputCursor == InkInputCursor.ERASER ? "ERASER" : "PEN";
                    return "Entrada: " + currentInputSource + " " + cursor
                            + " - raw " + formatDiagnosticNumber(currentInputRawPressure)
                            + " - presion " + Math.round(normalizedInputPressure() * 100.0) + "%"
                            + (inputPressureVaried ? " - variable" : " - fija");
                }
            }
        }
        if (capabilities.nativeProvider()) {
            return "Entrada: " + provider;
        }
        String pressure = capabilities.nativeProvider() && capabilities.pressure()
                ? " · presion " + Math.round(normalizedInputPressure() * 100.0) + "%"
                : "";
        return "Entrada: " + provider + pressure;
    }

    private static String formatDiagnosticNumber(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.3f", value) : "n/a";
    }

    private void refreshInputDiagnostic() {
        if (inputDiagnosticLabel == null) {
            return;
        }
        String text = inputDiagnosticText();
        if (!text.equals(inputDiagnosticLabel.getText())) {
            inputDiagnosticLabel.setText(text);
            StudioFormControls.installTooltip(inputDiagnosticLabel, inputDiagnosticTooltip());
        }
    }

    private String inputDiagnosticTooltip() {
        InkInputCapabilities capabilities = inkInputProvider.capabilities();
        StringBuilder text = new StringBuilder("Proveedor: ")
                .append(capabilities.providerName())
                .append(".");
        if (capabilities.nativeProvider()) {
            text.append(" Presion: ").append(capabilities.pressure() ? "si" : "no")
                    .append("; borrador: ").append(capabilities.eraserCursor() ? "si" : "no")
                    .append("; tilt: ").append(capabilities.tilt() ? "si" : "no")
                    .append(".");
        } else {
            text.append(" Sin presion ni borrador nativo.");
            if ("JavaFX mouse".equals(capabilities.providerName())) {
                text.append(" La entrada llega por JavaFX y Windows puede coalescer eventos, reduciendo puntos por segundo.");
            } else {
                text.append(" Esta pantalla usa solo LectureStudio stylus; no hay fallback de mouse para escribir.");
            }
        }
        if (!capabilities.fallbackReason().isBlank()) {
            text.append(" ").append(capabilities.fallbackReason());
        }
        return text.toString();
    }

    private Node existingSolutionImageNode() {
        Label label = new Label(existingSolutionImagePath == null
                ? ""
                : "Solucion guardada cargada en el lienzo; puedes continuar escribiendo encima.");
        label.setWrapText(true);
        label.getStyleClass().add("technical-problem-existing-solution-note");
        label.setVisible(existingSolutionImagePath != null);
        label.setManaged(existingSolutionImagePath != null);
        return label;
    }

    private FlowPane toolBar() {
        Button undoButton = iconToolButton("undo-2", "Deshacer (Ctrl+Z).", this::undo, false);
        Button redoButton = iconToolButton("redo-2", "Rehacer (Ctrl+Y).", this::redo, false);
        Button clearStrokesButton = ActionButtonFactory.secondary(
                "Limpiar trazos",
                "Limpiar solo trazos; conserva imagenes.",
                this::clearInkStrokes);
        Button clearCanvasButton = iconToolButton("trash-2", "Limpiar lienzo completo; elimina trazos e imagenes.", this::clearCanvas, true);
        CheckBox imageInteraction = new CheckBox("Interactuar con imagenes");
        imageInteraction.selectedProperty().bindBidirectional(imageInteractionMode);
        imageInteraction.disableProperty().bind(drawMode.not().or(canvasRegionSelectionMode));
        imageInteraction.getStyleClass().add("technical-problem-image-interaction");
        StudioFormControls.installTooltip(imageInteraction,
                "Activado: clic y arrastre seleccionan imagenes; la tinta no dibuja. Desactivado: puedes escribir encima.");
        shrinkImageButton = iconToolButton("zoom-out", "Reducir la imagen seleccionada.", () -> resizeSelectedImage(0.85), false);
        enlargeImageButton = iconToolButton("zoom-in", "Ampliar la imagen seleccionada.", () -> resizeSelectedImage(1.15), false);
        deleteImageButton = iconToolButton("trash-2", "Eliminar la imagen seleccionada.", this::deleteSelectedImage, true);
        cropImageButton = ActionButtonFactory.secondary("Recortar imagen", "Recortar la imagen seleccionada trazando un rectangulo sobre ella.", this::startImageCropMode);
        restoreImageCropButton = ActionButtonFactory.secondary("Restaurar recorte", "Volver a mostrar la imagen completa seleccionada.", this::restoreSelectedImageCrop);
        copyRegionButton = iconToolButton("copy", "Copiar la region seleccionada.", this::copyCanvasRegionSelection, false);
        pasteRegionButton = iconToolButton("clipboard-paste", "Pegar la ultima region copiada como imagen editable.", this::pasteCanvasRegionSelection, false);
        moveRegionButton = iconToolButton("move-horizontal", "Mover la region seleccionada.", this::moveCanvasRegionSelection, false);
        deleteRegionButton = iconToolButton("trash-2", "Eliminar la region seleccionada.", this::deleteCanvasRegionSelection, true);
        Label widthLabel = new Label("Grosor");
        StudioFormControls.colorPicker(penColor, "Color del lapiz.");
        StudioFormControls.colorPicker(backgroundColor, "Color de fondo del lienzo.");
        configurePenWidthSlider();
        configureCanvasZoomSlider();
        eraser.getStyleClass().addAll(StudioFormControls.FORM_CONTROL, StudioFormControls.FORM_TOGGLE);
        eraser.setGraphic(LucideIconView.of("eraser"));
        eraser.setContentDisplay(ContentDisplay.LEFT);
        StudioFormControls.installTooltip(eraser, "Borrar solo la tinta del lienzo sin afectar imagenes ni fondo.");
        for (Button button : List.of(cropImageButton, restoreImageCropButton)) {
            button.setMinWidth(96);
        }
        FlowPane tools = new FlowPane(8, 8,
                new Label("Lapiz"), penColor,
                new Label("Fondo"), backgroundColor,
                widthLabel, penWidth, penWidthPreview,
                new Label("Zoom lienzo"), canvasZoom, canvasZoomValue,
                eraser,
                undoButton, redoButton, clearStrokesButton, clearCanvasButton,
                imageInteraction, shrinkImageButton, enlargeImageButton, deleteImageButton,
                cropImageButton, restoreImageCropButton,
                copyRegionButton, pasteRegionButton, moveRegionButton, deleteRegionButton);
        tools.setAlignment(Pos.CENTER_LEFT);
        tools.getStyleClass().addAll("technical-problem-tools", "technical-problem-tools-flow");
        imageInteractionMode.addListener((obs, oldValue, newValue) -> updateImageInteractionMode());
        updateImageButtons();
        updateCanvasRegionButtons();
        backgroundColor.setOnAction(event -> {
            rememberUndo();
            fillBackground(backgroundColor.getValue());
            canvasTouched.set(true);
        });
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

    private void configurePenWidthSlider() {
        penWidth.setBlockIncrement(1);
        penWidth.setMajorTickUnit(4);
        penWidth.setMinorTickCount(0);
        penWidth.setShowTickMarks(true);
        penWidth.setSnapToTicks(false);
        penWidth.setPrefWidth(160);
        StudioFormControls.slider(penWidth, "Grosor del trazo en pixeles.");
        penWidthPreview.setFill(Color.BLACK);
        penWidthPreview.setStroke(Color.web("#dbe3f1"));
        penWidthPreview.setStrokeWidth(1);
        penWidthPreview.radiusProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(2.0, penStrokeWidth() / 2.0),
                penWidth.valueProperty()));
        penWidthPreview.fillProperty().bind(penColor.valueProperty());
        StudioFormControls.installTooltip(penWidthPreview, "Vista previa del grosor del lapiz.");
    }

    private void configureCanvasZoomSlider() {
        canvasZoom.setBlockIncrement(10);
        canvasZoom.setMajorTickUnit(25);
        canvasZoom.setMinorTickCount(0);
        canvasZoom.setShowTickMarks(true);
        canvasZoom.setSnapToTicks(false);
        canvasZoom.setPrefWidth(150);
        StudioFormControls.slider(canvasZoom, "Acercar o alejar el lienzo sin cambiar el tamano exportado.");
        canvasZoomValue.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> Math.round(canvasZoom.getValue()) + "%",
                canvasZoom.valueProperty()));
        canvasZoomValue.getStyleClass().add("technical-problem-canvas-zoom-value");
        StudioFormControls.installTooltip(canvasZoomValue, "Zoom visual del lienzo.");
        canvasZoom.valueProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(() -> {
            ensureCanvasTilesCoverViewport();
        }));
    }

    private ScrollPane canvasPane() {
        drawingSurface.getStyleClass().add("technical-problem-canvas-surface");
        installImageResizeHandles();
        installCanvasRegionSelectionOverlay();
        updateImageInteractionMode();
        canvasTitleLabel = new Label();
        canvasTitleLabel.textProperty().bind(title.textProperty());
        canvasTitleLabel.setMouseTransparent(true);
        canvasTitleLabel.getStyleClass().add("technical-problem-canvas-title-band");
        canvasTitleLabel.maxWidthProperty().bind(drawingSurface.widthProperty().subtract(48));
        StackPane.setAlignment(canvasTitleLabel, Pos.TOP_LEFT);
        StackPane.setMargin(canvasTitleLabel, new Insets(18, 24, 0, 24));
        StackPane canvasFrame = new StackPane(drawingSurface, canvasTitleLabel);
        canvasFrame.getStyleClass().add("technical-problem-canvas-frame");
        canvasFrame.setPadding(new Insets(16));
        Group scaledCanvas = new Group(canvasFrame);
        scaledCanvas.scaleXProperty().bind(canvasZoom.valueProperty().divide(100.0));
        scaledCanvas.scaleYProperty().bind(canvasZoom.valueProperty().divide(100.0));
        Pane zoomHost = new Pane(scaledCanvas);
        zoomHost.getStyleClass().add("technical-problem-canvas-zoom-host");
        zoomHost.prefWidthProperty().bind(drawingSurface.widthProperty().add(32).multiply(canvasZoom.valueProperty().divide(100.0)));
        zoomHost.prefHeightProperty().bind(drawingSurface.heightProperty().add(32).multiply(canvasZoom.valueProperty().divide(100.0)));
        zoomHost.minWidthProperty().bind(zoomHost.prefWidthProperty());
        zoomHost.minHeightProperty().bind(zoomHost.prefHeightProperty());
        ScrollPane scroll = new ScrollPane(zoomHost);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setPannable(false);
        scroll.setPrefViewportHeight(520);
        scroll.getStyleClass().add("technical-problem-canvas-scroll");
        scroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if ((drawMode.get() || canvasRegionSelectionMode.get())
                    && event.getTarget() instanceof Node target && descendantOf(target, drawingSurface)) {
                event.consume();
            }
        });
        scroll.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            maybeGrowCanvasForScroll();
        });
        scroll.hvalueProperty().addListener((obs, oldValue, newValue) -> {
            maybeGrowCanvasForScroll();
        });
        scroll.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> {
            ensureCanvasTilesCoverViewport();
        });
        canvasScroll = scroll;
        drawMode.addListener((obs, oldValue, newValue) -> updateCanvasScrollMode());
        updateCanvasScrollMode();
        Platform.runLater(this::ensureCanvasTilesCoverViewport);
        return scroll;
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

    private void initializeCanvas() {
        fillBackground(Color.WHITE);
        startInkEngine();
        inkInputProvider.attach(drawingSurface.inkInputTarget(), new InkInputListener() {
            @Override
            public void onHover(InkInputSample sample) {
                updateInputState(sample);
                Point2D point = pointInsideCanvas(sample);
                if (point != null) {
                    lastCanvasPointer = point;
                }
            }

            @Override
            public boolean onStrokeStart(InkInputSample sample) {
                return handleInkStrokeStart(sample);
            }

            @Override
            public boolean onStrokeMove(InkInputSample sample) {
                return handleInkStrokeMove(sample);
            }

            @Override
            public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
                boolean consumed = false;
                for (InkInputSample sample : samples) {
                    consumed |= handleInkStrokeMove(sample);
                }
                return consumed;
            }

            @Override
            public boolean onStrokeEnd(InkInputSample sample) {
                return handleInkStrokeEnd(sample);
            }
        });
        refreshInputDiagnostic();
        Platform.runLater(this::refreshInputDiagnostic);
        Platform.runLater(this::loadExistingSolutionIntoCanvas);
    }

    private void disposeCanvasInput() {
        inkInputProvider.detach();
        if (inkEngine != null) {
            inkEngine.stop();
            inkEngine = null;
        }
    }

    private void resetInkCoordinateState() {
        inkInputProvider.resetCoordinateState();
    }

    private boolean handleInkStrokeStart(InkInputSample sample) {
        updateInputState(sample);
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            return false;
        }
        lastCanvasPointer = point;
        if (canvasRegionSelectionMode.get()) {
            beginCanvasRegionSelection(point);
            return true;
        }
        if (!drawMode.get()) {
            return false;
        }
        if (!canCaptureInkInput()) {
            return false;
        }
        if (pointerOnImageResizeHandle(point)) {
            return false;
        }
        selectCanvasImageUnderPointerForDrawing(point);
        rememberFastInkUndo();
        redo.clear();
        if (insideTitleBand(point)) {
            return true;
        }
        if (inkEngine != null) {
            inkEngine.begin(
                    point.getX(),
                    point.getY(),
                    sample.nanos(),
                    penColor.getValue(),
                    penStrokeWidth(),
                    eraser.isSelected() || currentInputEraser,
                    normalizedInputPressure());
        }
        canvasTouched.set(true);
        return true;
    }

    private boolean handleInkStrokeMove(InkInputSample sample) {
        updateInputState(sample);
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            return false;
        }
        lastCanvasPointer = point;
        if (canvasRegionSelectionMode.get()) {
            dragCanvasRegionSelection(point);
            return true;
        }
        if (!drawMode.get()) {
            return false;
        }
        if (!canCaptureInkInput()) {
            return false;
        }
        if (inkEngine != null) {
            inkEngine.move(
                    point.getX(),
                    point.getY(),
                    sample.nanos(),
                    penColor.getValue(),
                    penStrokeWidth(),
                    eraser.isSelected() || currentInputEraser,
                    normalizedInputPressure());
        }
        canvasTouched.set(true);
        return true;
    }

    private boolean handleInkStrokeEnd(InkInputSample sample) {
        updateInputState(sample);
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            point = lastCanvasPointer;
        }
        lastCanvasPointer = point;
        if (!canvasRegionSelectionMode.get()) {
            if (!drawMode.get()) {
                return false;
            }
            if (!canCaptureInkInput()) {
                return false;
            }
            if (inkEngine != null) {
                inkEngine.end(
                        point.getX(),
                        point.getY(),
                        sample.nanos(),
                        penColor.getValue(),
                        penStrokeWidth(),
                        eraser.isSelected() || currentInputEraser,
                        normalizedInputPressure());
            }
            canvasTouched.set(true);
            return true;
        }
        finishCanvasRegionSelection(point);
        return true;
    }

    private void updateInputState(InkInputSample sample) {
        if (sample == null) {
            currentInputPressure = 1.0;
            currentInputRawPressure = Double.NaN;
            lastComparableRawPressure = Double.NaN;
            inputPressureVaried = false;
            currentInputCursor = InkInputCursor.UNKNOWN;
            currentInputSource = "";
            currentInputEraser = false;
            return;
        }
        currentInputPressure = Math.max(0.0, Math.min(1.0, sample.pressure()));
        currentInputRawPressure = sample.rawPressure();
        currentInputCursor = sample.cursor();
        currentInputSource = sample.inputSource();
        if (Double.isFinite(currentInputRawPressure)) {
            if (Double.isFinite(lastComparableRawPressure)
                    && Math.abs(currentInputRawPressure - lastComparableRawPressure) > 0.01) {
                inputPressureVaried = true;
            }
            lastComparableRawPressure = currentInputRawPressure;
        }
        currentInputEraser = sample.requestsEraser() || sample.cursor() == InkInputCursor.ERASER;
        refreshInputDiagnostic();
    }

    private Point2D pointInsideCanvas(InkInputSample sample) {
        if (sample == null) {
            return null;
        }
        return InkCanvasViewportCoordinateMapper.mapInside(
                        drawingSurface.inkInputTarget(),
                        drawingSurface,
                        sample.x(),
                        sample.y(),
                        drawingSurface.logicalWidth(),
                        drawingSurface.logicalHeight())
                .orElse(null);
    }

    private void startInkEngine() {
        if (inkEngine != null) {
            return;
        }
        inkEngine = new InkRealtimeStrokeEngine(new InkRealtimeStrokeEngine.Sink() {
            @Override
            public void beginLiveStroke() {
                drawingSurface.beginLiveStroke();
            }

            @Override
            public void previewLine(double x1, double y1, double x2, double y2,
                                    Color color, double width, boolean erase) {
                if (insideTitleBand(new Point2D(x1, y1)) && insideTitleBand(new Point2D(x2, y2))) {
                    return;
                }
                drawingSurface.previewLine(x1, y1, x2, y2, color, width, erase);
            }

            @Override
            public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                                         double endX, double endY, Color color, double width, boolean erase) {
                if (insideTitleBand(new Point2D(startX, startY))
                        && insideTitleBand(new Point2D(controlX, controlY))
                        && insideTitleBand(new Point2D(endX, endY))) {
                    return;
                }
                drawingSurface.previewQuadratic(startX, startY, controlX, controlY, endX, endY, color, width, erase);
            }

            @Override
            public void commitStroke(InkRealtimeStrokeEngine.CommittedStroke stroke) {
                drawingSurface.commitInkStroke(toCanvasStrokeState(stroke));
            }

            @Override
            public boolean acceptsPoint(double x, double y) {
                return !insideTitleBand(new Point2D(x, y));
            }
        });
        inkEngine.start();
    }

    private static StudyProblemCanvasSurface.InkStrokeState toCanvasStrokeState(InkRealtimeStrokeEngine.CommittedStroke stroke) {
        List<StudyProblemCanvasSurface.InkPointState> points = stroke.points().stream()
                .map(point -> new StudyProblemCanvasSurface.InkPointState(
                        point.x(),
                        point.y(),
                        point.nanos(),
                        point.pressure()))
                .toList();
        return new StudyProblemCanvasSurface.InkStrokeState(
                stroke.erase() ? "ERASE" : "DRAW",
                cssColor(stroke.color()),
                stroke.width(),
                points);
    }

    private double normalizedInputPressure() {
        if (!Double.isFinite(currentInputPressure)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, currentInputPressure));
    }

    private double effectiveStrokeWidth(double pressure) {
        return com.marcosmoreiradev.docupodcaststudio.ink.InkBrushMath
                .pressureWidth(penStrokeWidth(), pressure);
    }

    private boolean pressureSensitiveInputActive() {
        InkInputCapabilities capabilities = inkInputProvider.capabilities();
        return capabilities.nativeProvider() && capabilities.pressure();
    }

    private void flushInk() {
        if (inkEngine != null) {
            inkEngine.flushAll();
        }
    }

    private boolean insideTitleBand(Point2D point) {
        return point != null && !title.getText().isBlank() && point.getY() >= 0 && point.getY() < CANVAS_TITLE_BAND_HEIGHT;
    }

    private double penStrokeWidth() {
        return Math.max(1.0, Math.round(penWidth.getValue()));
    }

    private void clearInkStrokes() {
        flushInk();
        rememberUndo();
        redo.clear();
        drawingSurface.clearStrokes();
        canvasTouched.set(true);
    }

    private void clearCanvas() {
        flushInk();
        rememberUndo();
        redo.clear();
        drawingSurface.clearStrokes();
        for (CanvasImageItem item : List.copyOf(canvasImages)) {
            drawingSurface.imageLayer().getChildren().remove(item.view());
        }
        canvasImages.clear();
        canvasRegionClipboard = null;
        imageCropMode = false;
        clearImageCropSelectionRectangle();
        hideCanvasRegionSelection();
        selectCanvasImage(null);
        imageInteractionMode.set(false);
        updateImageInteractionMode();
        updateCanvasRegionButtons();
        canvasTouched.set(true);
    }

    private void fillBackground(Color color) {
        drawingSurface.fillBackground(color == null ? Color.WHITE : color);
    }

    private void updateCanvasScrollMode() {
        if (canvasScroll == null) {
            return;
        }
        boolean canvasConsumesPointer = drawMode.get() || canvasRegionSelectionMode.get();
        canvasScroll.setPannable(!canvasConsumesPointer);
        canvasScroll.setHbarPolicy(canvasConsumesPointer ? ScrollPane.ScrollBarPolicy.NEVER : ScrollPane.ScrollBarPolicy.AS_NEEDED);
        canvasScroll.setVbarPolicy(canvasConsumesPointer ? ScrollPane.ScrollBarPolicy.NEVER : ScrollPane.ScrollBarPolicy.AS_NEEDED);
        if (!canvasConsumesPointer) {
            resetInkCoordinateState();
        }
    }

    private void updateImageInteractionMode() {
        boolean imageCropPicking = imageCropMode && selectedCanvasImage != null;
        boolean imagePicking = imageCropPicking || canInteractWithCanvasImages();
        drawingSurface.imageLayer().setMouseTransparent(!imagePicking);
        drawingSurface.imageLayer().setPickOnBounds(false);
        updateInkInputLayerMode();
        if (!imageInteractionMode.get()) {
            selectCanvasImage(null);
        }
        updateResizeHandles();
        updateImageButtons();
    }

    private boolean canInteractWithCanvasImages() {
        return canvasMode.get()
                && drawMode.get()
                && imageInteractionMode.get()
                && !canvasRegionSelectionMode.get();
    }

    private boolean canCaptureInkInput() {
        return canvasMode.get()
                && !imageCropMode
                && !imageInteractionMode.get()
                && (canvasRegionSelectionMode.get() || drawMode.get());
    }

    private void updateInkInputLayerMode() {
        boolean inkActive = canCaptureInkInput();
        boolean imageToolsActive = imageCropMode || canInteractWithCanvasImages();
        boolean layerInteractive = inkActive || imageToolsActive;
        drawingSurface.configureInputCapture(layerInteractive, inkActive);
    }

    private boolean pointerOnImageResizeHandle(Point2D point) {
        if (point == null || imageResizeHandles.isEmpty()) {
            return false;
        }
        return imageResizeHandles.stream()
                .anyMatch(handle -> handle.isVisible() && handle.getBoundsInParent().contains(point));
    }

    private void maybeGrowCanvasForPoint(double x, double y) {
        drawingSurface.growForPoint(x, y, StudyProblemCanvasSurface.EDGE_GROW_THRESHOLD);
    }

    private void maybeGrowCanvasForScroll() {
        if (canvasScroll == null || drawMode.get() || canvasRegionSelectionMode.get()
                || canvasGrowScheduled || suppressCanvasGrowEvents) {
            return;
        }
        boolean growWidth = canvasScroll.getHvalue() >= 0.965;
        boolean growHeight = canvasScroll.getVvalue() >= 0.965;
        if (!growWidth && !growHeight) {
            return;
        }
        growWidth = growWidth && drawingSurface.canGrowHorizontally();
        growHeight = growHeight && drawingSurface.canGrowVertically();
        if (!growWidth && !growHeight) {
            return;
        }
        pendingCanvasGrowWidth = pendingCanvasGrowWidth || growWidth;
        pendingCanvasGrowHeight = pendingCanvasGrowHeight || growHeight;
        canvasGrowDebounce.playFromStart();
    }

    private void runPendingCanvasGrowth() {
        if (canvasScroll == null || canvasGrowScheduled) {
            return;
        }
        boolean growWidth = pendingCanvasGrowWidth && drawingSurface.canGrowHorizontally();
        boolean growHeight = pendingCanvasGrowHeight && drawingSurface.canGrowVertically();
        pendingCanvasGrowWidth = false;
        pendingCanvasGrowHeight = false;
        if (!growWidth && !growHeight) {
            return;
        }
        canvasGrowScheduled = true;
        try {
            boolean grew = drawingSurface.growByScroll(growWidth, growHeight);
            if (grew) {
                suppressCanvasGrowEvents = true;
                if (growWidth) {
                    canvasScroll.setHvalue(0.78);
                }
                if (growHeight) {
                    canvasScroll.setVvalue(0.78);
                }
                ensureCanvasTilesCoverViewport();
                PauseTransition release = new PauseTransition(Duration.millis(220));
                release.setOnFinished(event -> Platform.runLater(() -> suppressCanvasGrowEvents = false));
                release.play();
            }
        } finally {
            canvasGrowScheduled = false;
        }
    }

    private void ensureCanvasTilesCoverViewport() {
        if (canvasScroll == null || canvasScroll.getViewportBounds().getWidth() <= 0
                || canvasScroll.getViewportBounds().getHeight() <= 0) {
            return;
        }
        double zoom = Math.max(0.1, canvasZoom.getValue() / 100.0);
        drawingSurface.ensureLogicalSize(
                Math.max(drawingProfile.logicalWidth(), (canvasScroll.getViewportBounds().getWidth() - 36) / zoom),
                Math.max(drawingProfile.logicalHeight(), (canvasScroll.getViewportBounds().getHeight() - 36) / zoom));
    }

    private void installCanvasRegionSelectionOverlay() {
        if (canvasRegionSelectionRectangle != null) {
            return;
        }
        canvasRegionSelectionRectangle = new Rectangle();
        canvasRegionSelectionRectangle.setManaged(false);
        canvasRegionSelectionRectangle.setMouseTransparent(true);
        canvasRegionSelectionRectangle.setVisible(false);
        canvasRegionSelectionRectangle.getStyleClass().add("technical-problem-canvas-region-selection");
        drawingSurface.getChildren().add(canvasRegionSelectionRectangle);
    }

    private void beginCanvasRegionSelection(Point2D point) {
        if (point == null) {
            return;
        }
        canvasSelectionStartX = clampCanvasX(point.getX());
        canvasSelectionStartY = clampCanvasY(point.getY());
        activeCanvasRegionSelection = null;
        updateCanvasRegionSelectionRectangle(canvasSelectionStartX, canvasSelectionStartY, canvasSelectionStartX, canvasSelectionStartY);
        updateCanvasRegionButtons();
    }

    private void dragCanvasRegionSelection(Point2D point) {
        if (point == null || canvasRegionSelectionRectangle == null || !canvasRegionSelectionRectangle.isVisible()) {
            return;
        }
        updateCanvasRegionSelectionRectangle(canvasSelectionStartX, canvasSelectionStartY, clampCanvasX(point.getX()), clampCanvasY(point.getY()));
    }

    private void finishCanvasRegionSelection(Point2D point) {
        if (point == null || canvasRegionSelectionRectangle == null || !canvasRegionSelectionRectangle.isVisible()) {
            return;
        }
        double endX = clampCanvasX(point.getX());
        double endY = clampCanvasY(point.getY());
        CanvasRegionSelection selection = CanvasRegionSelection.from(canvasSelectionStartX, canvasSelectionStartY, endX, endY);
        if (selection.width() < 12 || selection.height() < 12) {
            hideCanvasRegionSelection();
            return;
        }
        activeCanvasRegionSelection = selection;
        updateCanvasRegionSelectionRectangle(selection.x(), selection.y(), selection.x() + selection.width(), selection.y() + selection.height());
        updateCanvasRegionButtons();
    }

    private void updateCanvasRegionSelectionRectangle(double x1, double y1, double x2, double y2) {
        if (canvasRegionSelectionRectangle == null) {
            return;
        }
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);
        canvasRegionSelectionRectangle.setX(minX);
        canvasRegionSelectionRectangle.setY(minY);
        canvasRegionSelectionRectangle.setWidth(width);
        canvasRegionSelectionRectangle.setHeight(height);
        canvasRegionSelectionRectangle.setVisible(true);
        canvasRegionSelectionRectangle.toFront();
    }

    private void hideCanvasRegionSelection() {
        if (canvasRegionSelectionRectangle != null) {
            canvasRegionSelectionRectangle.setVisible(false);
        }
        activeCanvasRegionSelection = null;
        updateCanvasRegionButtons();
    }

    private void copyCanvasRegionSelection() {
        CanvasRegionSelection selection = activeCanvasRegionSelection;
        if (selection == null) {
            return;
        }
        canvasRegionClipboard = drawingSurface.snapshotRegion(canvasImageViews(), selection.x(), selection.y(), selection.width(), selection.height());
        updateCanvasRegionButtons();
    }

    private void pasteCanvasRegionSelection() {
        if (canvasRegionClipboard == null) {
            return;
        }
        Point2D target = pasteTarget();
        addCanvasImage(canvasRegionClipboard, target.getX(), target.getY(),
                Math.min(canvasRegionClipboard.getWidth(), drawingSurface.logicalWidth() * 0.6), false);
        canvasRegionSelectionMode.set(false);
        imageInteractionMode.set(false);
        selectCanvasImage(null);
        canvasTouched.set(true);
    }

    private void deleteCanvasRegionSelection() {
        CanvasRegionSelection selection = activeCanvasRegionSelection;
        if (selection == null) {
            return;
        }
        rememberUndo();
        redo.clear();
        drawingSurface.eraseRegion(selection.x(), selection.y(), selection.width(), selection.height());
        removeImagesInside(selection);
        canvasTouched.set(true);
        hideCanvasRegionSelection();
    }

    private void moveCanvasRegionSelection() {
        CanvasRegionSelection selection = activeCanvasRegionSelection;
        if (selection == null) {
            return;
        }
        canvasRegionClipboard = drawingSurface.snapshotRegion(canvasImageViews(), selection.x(), selection.y(), selection.width(), selection.height());
        rememberUndo();
        redo.clear();
        drawingSurface.eraseRegion(selection.x(), selection.y(), selection.width(), selection.height());
        removeImagesInside(selection);
        addCanvasImage(canvasRegionClipboard, selection.x(), selection.y(), selection.width(), false);
        canvasRegionSelectionMode.set(false);
        imageInteractionMode.set(false);
        selectCanvasImage(null);
        canvasTouched.set(true);
        hideCanvasRegionSelection();
    }

    private Point2D pasteTarget() {
        Point2D pointer = lastCanvasPointer;
        if (pointer != null && Double.isFinite(pointer.getX()) && Double.isFinite(pointer.getY())) {
            return new Point2D(clampCanvasX(pointer.getX()), clampCanvasY(pointer.getY()));
        }
        if (canvasScroll != null) {
            Bounds viewport = canvasScroll.getViewportBounds();
            Point2D scene = canvasScroll.localToScene(viewport.getWidth() / 2.0, viewport.getHeight() / 2.0);
            if (scene != null) {
                Point2D local = drawingSurface.sceneToLocal(scene);
                return new Point2D(clampCanvasX(local.getX()), clampCanvasY(local.getY()));
            }
        }
        return new Point2D(96, CANVAS_TITLE_BAND_HEIGHT + 96);
    }

    private void removeImagesInside(CanvasRegionSelection selection) {
        List<CanvasImageItem> removed = canvasImages.stream()
                .filter(item -> selection.contains(item.view().getBoundsInParent()))
                .toList();
        for (CanvasImageItem item : removed) {
            drawingSurface.imageLayer().getChildren().remove(item.view());
            canvasImages.remove(item);
            if (selectedCanvasImage == item) {
                selectedCanvasImage = null;
            }
        }
        updateResizeHandles();
        updateImageButtons();
    }

    private void updateCanvasRegionButtons() {
        boolean hasSelection = activeCanvasRegionSelection != null;
        if (copyRegionButton != null) {
            setNodeVisible(copyRegionButton, canvasRegionSelectionMode.get() && hasSelection);
            copyRegionButton.setDisable(!hasSelection);
        }
        if (deleteRegionButton != null) {
            setNodeVisible(deleteRegionButton, canvasRegionSelectionMode.get() && hasSelection);
            deleteRegionButton.setDisable(!hasSelection);
        }
        if (moveRegionButton != null) {
            setNodeVisible(moveRegionButton, canvasRegionSelectionMode.get() && hasSelection);
            moveRegionButton.setDisable(!hasSelection);
        }
        if (pasteRegionButton != null) {
            setNodeVisible(pasteRegionButton, canvasRegionClipboard != null);
            pasteRegionButton.setDisable(canvasRegionClipboard == null);
        }
        updateImageButtons();
    }

    private double clampCanvasX(double x) {
        return Math.max(0.0, Math.min(drawingSurface.logicalWidth(), x));
    }

    private double clampCanvasY(double y) {
        return Math.max(0.0, Math.min(drawingSurface.logicalHeight(), y));
    }

    private void rememberUndo() {
        flushInk();
        rememberUndoSnapshot(snapshotUndoState(false));
    }

    private void rememberFastInkUndo() {
        if (drawingSurface.vectorInkReliable()) {
            rememberUndoSnapshot(snapshotUndoState(false));
        } else {
            rememberUndo();
        }
    }

    private void rememberUndoSnapshot(CanvasUndoSnapshot snapshot) {
        undo.push(snapshot);
        while (undo.size() > drawingProfile.historyLimit()) {
            undo.removeLast();
        }
    }

    private void undo() {
        if (undo.isEmpty()) {
            return;
        }
        flushInk();
        redo.push(snapshotUndoState(false));
        restoreUndoSnapshot(undo.pop());
        canvasTouched.set(true);
    }

    private void redo() {
        if (redo.isEmpty()) {
            return;
        }
        flushInk();
        undo.push(snapshotUndoState(false));
        restoreUndoSnapshot(redo.pop());
        canvasTouched.set(true);
    }

    private CanvasUndoSnapshot snapshotUndoState(boolean flush) {
        if (flush) {
            flushInk();
        }
        if (drawingSurface.vectorInkReliable()) {
            return CanvasUndoSnapshot.vector(drawingSurface.inkStrokeStates(), drawingSurface.inkCommandStates());
        }
        return CanvasUndoSnapshot.raster(drawingSurface.snapshotDrawing());
    }

    private void restoreUndoSnapshot(CanvasUndoSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!snapshot.vector()) {
            drawSnapshot(snapshot.raster());
            return;
        }
        drawingSurface.restoreInkUndoState(snapshot.strokes(), snapshot.commands());
    }

    private WritableImage snapshotCanvas() {
        flushInk();
        return drawingSurface.snapshotWithImages(canvasImageViews());
    }

    private InkCanvasExportResult exportCanvas(InkCanvasExportOptions options) {
        flushInk();
        markTitleContentBounds();
        return burnTitleIntoCanvas(drawingSurface.exportWithImages(canvasImageViews(), options), title.getText());
    }

    private InkCanvasExportResult burnTitleIntoCanvas(InkCanvasExportResult base, String titleText) {
        String normalizedTitle = titleText == null ? "" : titleText.strip();
        if (base == null || base.image() == null || normalizedTitle.isBlank()) {
            return base;
        }
        int scale = Math.max(1, base.scale());
        int width = Math.max(1, (int) Math.ceil(base.image().getWidth()));
        int height = Math.max(1, (int) Math.ceil(base.image().getHeight()));
        int titleHeight = Math.max((int) Math.ceil(CANVAS_TITLE_BAND_HEIGHT * scale), (int) Math.ceil(44.0 * scale));
        BufferedImage outputImage = writableToBuffered(base.image(), backgroundColor.getValue());
        Graphics2D graphics = outputImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            java.awt.Color bg = awtColor(backgroundColor.getValue() == null ? Color.WHITE : backgroundColor.getValue());
            graphics.setColor(bg);
            graphics.fillRect(0, 0, width, Math.min(height, titleHeight));
            graphics.setColor(new java.awt.Color(17, 24, 39));
            graphics.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD,
                    Math.max(15, (int) Math.round(18.0 * scale))));
            graphics.drawString(normalizedTitle, (int) Math.round(24.0 * scale),
                    Math.max((int) Math.round(34.0 * scale), (int) Math.round(titleHeight * 0.62)));
        } finally {
            graphics.dispose();
        }
        WritableImage output = bufferedToWritable(outputImage);
        return new InkCanvasExportResult(
                output,
                scale,
                base.cropped(),
                base.logicalWidth(),
                base.logicalHeight(),
                base.warnings());
    }

    private static BufferedImage writableToBuffered(WritableImage image, Color background) {
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Color bg = awtColor(background == null ? Color.WHITE : background);
        Graphics2D graphics = output.createGraphics();
        graphics.setColor(bg);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return output;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.setRGB(x, y, blendArgb(output.getRGB(x, y), reader.getArgb(x, y)));
            }
        }
        return output;
    }

    private static WritableImage bufferedToWritable(BufferedImage image) {
        WritableImage output = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter writer = output.getPixelWriter();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                writer.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return output;
    }

    private static java.awt.Color awtColor(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return new java.awt.Color(
                clampColor(safe.getRed()),
                clampColor(safe.getGreen()),
                clampColor(safe.getBlue()),
                255);
    }

    private static int clampColor(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value * 255.0)));
    }

    private static int blendArgb(int dst, int src) {
        int alpha = (src >>> 24) & 0xff;
        if (alpha >= 255) {
            return src;
        }
        if (alpha <= 0) {
            return dst;
        }
        int inv = 255 - alpha;
        int r = (((src >>> 16) & 0xff) * alpha + ((dst >>> 16) & 0xff) * inv) / 255;
        int g = (((src >>> 8) & 0xff) * alpha + ((dst >>> 8) & 0xff) * inv) / 255;
        int b = ((src & 0xff) * alpha + (dst & 0xff) * inv) / 255;
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private void loadExistingSolutionIntoCanvas() {
        if (existingCanvasStatePath != null && Files.isRegularFile(existingCanvasStatePath)
                && restoreCanvasState(existingCanvasStatePath)) {
            canvasTouched.set(false);
            return;
        }
        if (existingSolutionImagePath == null || !Files.isRegularFile(existingSolutionImagePath)) {
            return;
        }
        Image image = new Image(existingSolutionImagePath.toUri().toString(), true);
        WritableImage writable = writableCopy(image);
        if (writable == null) {
            return;
        }
        drawingSurface.drawBackgroundImage(writable, Color.WHITE);
        canvasTouched.set(false);
    }

    private void markTitleContentBounds() {
        if (title.getText() == null || title.getText().isBlank()) {
            return;
        }
        drawingSurface.markContentBounds(0, 0, Math.max(420.0, drawingSurface.logicalWidth() * 0.55),
                CANVAS_TITLE_BAND_HEIGHT);
    }

    private boolean shouldPersistCanvasState(boolean needsCanvas) {
        return needsCanvas
                || canvasTouched.get()
                || !canvasImages.isEmpty()
                || !drawingSurface.inkStrokeStates().isEmpty()
                || !drawingSurface.inkCommandStates().isEmpty()
                || (existingCanvasStatePath != null && Files.isRegularFile(existingCanvasStatePath));
    }

    private String canvasStateJson() {
        flushInk();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("consumer", "document-study.technical-problem");
        metadata.put("title", title.getText() == null ? "" : title.getText().strip());
        return InkWorkspaceStateSerializer.toJson(InkWorkspaceState.create(
                drawingSurface.logicalWidth(),
                drawingSurface.logicalHeight(),
                cssColor(drawingSurface.backgroundColor()),
                drawingSurface.applicationInkStrokes(),
                canvasPlacedImages(),
                metadata));
    }

    private List<InkPlacedImage> canvasPlacedImages() {
        List<InkPlacedImage> images = new ArrayList<>();
        int index = 1;
        for (CanvasImageItem item : canvasImages) {
            String imageData = imageToBase64(item.view().getImage());
            String originalData = imageToBase64(item.originalImage());
            if (imageData.isBlank()) {
                continue;
            }
            Bounds bounds = item.view().getBoundsInParent();
            images.add(new InkPlacedImage(
                    "IMG-" + index++,
                    "",
                    "",
                    imageData,
                    originalData,
                    item.view().getLayoutX(),
                    item.view().getLayoutY(),
                    item.view().getFitWidth(),
                    bounds == null ? Math.max(1.0, item.view().getFitHeight()) : bounds.getHeight(),
                    item.originalLayoutX(),
                    item.originalLayoutY(),
                    item.originalFitWidth(),
                    item.isCropped() ? new InkImageCrop(true, 0, 0,
                            Math.max(1.0, item.originalImage().getWidth()),
                            Math.max(1.0, item.originalImage().getHeight())) : InkImageCrop.none()));
        }
        return images;
    }

    private boolean restoreCanvasState(Path path) {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return false;
            }
            double width = jsonDoubleValue(json, "width", drawingProfile.logicalWidth());
            double height = jsonDoubleValue(json, "height", drawingProfile.logicalHeight());
            Color restoredBackground = parseColorValue(jsonStringValue(json, "background", "#ffffffff"));
            drawingSurface.resetForEditableState(width, height, restoredBackground);
            backgroundColor.setValue(restoredBackground);
            clearCanvasImages();
            for (String imageObject : jsonObjectsInArray(json, "images")) {
                Image image = imageFromBase64(jsonStringValue(imageObject, "image", ""));
                if (image == null || image.isError()) {
                    continue;
                }
                double x = jsonDoubleValue(imageObject, "x", 48.0);
                double y = jsonDoubleValue(imageObject, "y", CANVAS_TITLE_BAND_HEIGHT + 24.0);
                double fitWidth = jsonDoubleValue(imageObject, "fitWidth", Math.max(160.0, image.getWidth()));
                CanvasImageItem item = addCanvasImage(image, x, y, fitWidth, false);
                Image original = imageFromBase64(jsonStringValue(imageObject, "originalImage", ""));
                item.restoreCropMetadata(
                        original == null || original.isError() ? image : original,
                        jsonDoubleValue(imageObject, "originalLayoutX", x),
                        jsonDoubleValue(imageObject, "originalLayoutY", y),
                        jsonDoubleValue(imageObject, "originalFitWidth", fitWidth),
                        jsonBooleanValue(imageObject, "cropActive", false));
            }
            List<StudyProblemCanvasSurface.InkStrokeState> editableStrokes = new ArrayList<>();
            for (String strokeObject : jsonObjectsInArray(json, "inkStrokes")) {
                List<StudyProblemCanvasSurface.InkPointState> points = new ArrayList<>();
                for (String pointObject : jsonObjectsInArray(strokeObject, "points")) {
                    points.add(new StudyProblemCanvasSurface.InkPointState(
                            jsonDoubleValue(pointObject, "x", 0),
                            jsonDoubleValue(pointObject, "y", 0),
                            (long) jsonDoubleValue(pointObject, "nanos", 0),
                            jsonDoubleValue(pointObject, "pressure", 1)));
                }
                editableStrokes.add(new StudyProblemCanvasSurface.InkStrokeState(
                        jsonStringValue(strokeObject, "type", "DRAW"),
                        jsonStringValue(strokeObject, "color", "#000000ff"),
                        jsonDoubleValue(strokeObject, "width", 1),
                        points));
            }
            if (!editableStrokes.isEmpty()) {
                drawingSurface.restoreInkStrokeStates(editableStrokes);
            } else {
                List<StudyProblemCanvasSurface.InkCommandState> strokes = new ArrayList<>();
                for (String strokeObject : jsonObjectsInArray(json, "strokes")) {
                    strokes.add(new StudyProblemCanvasSurface.InkCommandState(
                            jsonStringValue(strokeObject, "type", "DRAW"),
                            jsonDoubleValue(strokeObject, "x1", 0),
                            jsonDoubleValue(strokeObject, "y1", 0),
                            jsonDoubleValue(strokeObject, "x2", 0),
                            jsonDoubleValue(strokeObject, "y2", 0),
                            jsonDoubleValue(strokeObject, "controlX", 0),
                            jsonDoubleValue(strokeObject, "controlY", 0),
                            jsonStringValue(strokeObject, "color", "#000000ff"),
                            jsonDoubleValue(strokeObject, "width", 1),
                            jsonBooleanValue(strokeObject, "quadratic", false)));
                }
                drawingSurface.restoreInkCommandStates(strokes);
            }
            canvasTouched.set(false);
            return true;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    private void clearCanvasImages() {
        for (CanvasImageItem item : List.copyOf(canvasImages)) {
            drawingSurface.imageLayer().getChildren().remove(item.view());
        }
        canvasImages.clear();
        selectedCanvasImage = null;
        updateResizeHandles();
        updateImageButtons();
    }

    private static StringBuilder appendJsonField(StringBuilder json, String name, String value) {
        json.append('"').append(name).append("\":");
        appendJsonString(json, value == null ? "" : value);
        return json;
    }

    private static void appendJsonString(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> json.append("\\\\");
                case '"' -> json.append("\\\"");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> json.append(c);
            }
        }
        json.append('"');
    }

    private static String number(double value) {
        return Double.isFinite(value) ? String.format(java.util.Locale.ROOT, "%.3f", value) : "0";
    }

    private static String cssColor(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x%02x",
                clampColor(safe.getRed()),
                clampColor(safe.getGreen()),
                clampColor(safe.getBlue()),
                clampColor(safe.getOpacity()));
    }

    private static Color parseColorValue(String value) {
        try {
            return Color.web(value == null || value.isBlank() ? "#ffffffff" : value);
        } catch (IllegalArgumentException ex) {
            return Color.WHITE;
        }
    }

    private static List<String> jsonObjectsInArray(String json, String field) {
        int fieldIndex = json.indexOf("\"" + field + "\"");
        if (fieldIndex < 0) {
            return List.of();
        }
        int arrayStart = json.indexOf('[', fieldIndex);
        if (arrayStart < 0) {
            return List.of();
        }
        List<String> objects = new ArrayList<>();
        boolean quoted = false;
        boolean escaped = false;
        int depth = 0;
        int objectStart = -1;
        for (int i = arrayStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && quoted) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (quoted) {
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(json.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return objects;
    }

    private static String jsonStringValue(String json, String field, String fallback) {
        int start = json.indexOf("\"" + field + "\"");
        if (start < 0) {
            return fallback;
        }
        int colon = json.indexOf(':', start);
        if (colon < 0) {
            return fallback;
        }
        int quote = json.indexOf('"', colon + 1);
        if (quote < 0) {
            return fallback;
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> c;
                });
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return fallback;
    }

    private static double jsonDoubleValue(String json, String field, double fallback) {
        int start = json.indexOf("\"" + field + "\"");
        if (start < 0) {
            return fallback;
        }
        int colon = json.indexOf(':', start);
        if (colon < 0) {
            return fallback;
        }
        int end = colon + 1;
        while (end < json.length() && Character.isWhitespace(json.charAt(end))) {
            end++;
        }
        int valueEnd = end;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (!(Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'E' || c == 'e')) {
                break;
            }
            valueEnd++;
        }
        try {
            return Double.parseDouble(json.substring(end, valueEnd));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean jsonBooleanValue(String json, String field, boolean fallback) {
        int start = json.indexOf("\"" + field + "\"");
        if (start < 0) {
            return fallback;
        }
        int colon = json.indexOf(':', start);
        if (colon < 0) {
            return fallback;
        }
        String tail = json.substring(colon + 1).stripLeading();
        if (tail.startsWith("true")) {
            return true;
        }
        if (tail.startsWith("false")) {
            return false;
        }
        return fallback;
    }

    private static String imageToBase64(Image image) {
        BufferedImage buffered = imageToBufferedArgb(image);
        if (buffered == null) {
            return "";
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(buffered, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            return "";
        }
    }

    private static Image imageFromBase64(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return new Image(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static BufferedImage imageToBufferedArgb(Image image) {
        WritableImage writable = writableCopy(image);
        if (writable == null) {
            return null;
        }
        int width = Math.max(1, (int) Math.ceil(writable.getWidth()));
        int height = Math.max(1, (int) Math.ceil(writable.getHeight()));
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = writable.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return output;
    }

    private static WritableImage writableCopy(Image image) {
        if (image == null || image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return null;
        }
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        WritableImage writable = new WritableImage(width, height);
        writable.getPixelWriter().setPixels(0, 0, width, height, reader, 0, 0);
        return writable;
    }

    private List<ImageView> canvasImageViews() {
        return canvasImages.stream()
                .map(CanvasImageItem::view)
                .toList();
    }

    private WritableImage snapshotDrawingCanvas() {
        flushInk();
        return drawingSurface.snapshotDrawing();
    }

    private void drawSnapshot(WritableImage image) {
        drawingSurface.drawSnapshot(image);
    }

    private void toggleStatementCollapsed() {
        if (problemSplit == null || statementNode == null) {
            return;
        }
        if (!statementCollapsed) {
            double[] dividers = problemSplit.getDividerPositions();
            if (dividers.length > 0 && dividers[0] > 0.05 && dividers[0] < 0.95) {
                restoredDivider = dividers[0];
            }
            problemSplit.getItems().remove(statementNode);
            statementCollapsed = true;
        } else {
            if (!problemSplit.getItems().contains(statementNode)) {
                problemSplit.getItems().add(0, statementNode);
            }
            statementCollapsed = false;
            Platform.runLater(() -> problemSplit.setDividerPositions(restoredDivider));
        }
        updateStatementButtonText();
    }

    private void toggleResolverFullscreen() {
        Window window = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
        if (!(window instanceof Stage stage)) {
            return;
        }
        if (!resolverFullscreen) {
            stageMaximizedBeforeFullscreen = stage.isMaximized();
            stageXBeforeFullscreen = stage.getX();
            stageYBeforeFullscreen = stage.getY();
            stageWidthBeforeFullscreen = stage.getWidth();
            stageHeightBeforeFullscreen = stage.getHeight();
            statementCollapsedBeforeFullscreen = statementCollapsed;
            if (!statementCollapsed) {
                toggleStatementCollapsed();
            }
            stage.setMaximized(true);
            resolverFullscreen = true;
        } else {
            if (!statementCollapsedBeforeFullscreen && statementCollapsed) {
                toggleStatementCollapsed();
            }
            stage.setMaximized(stageMaximizedBeforeFullscreen);
            if (!stageMaximizedBeforeFullscreen && stageWidthBeforeFullscreen > 0 && stageHeightBeforeFullscreen > 0) {
                stage.setX(stageXBeforeFullscreen);
                stage.setY(stageYBeforeFullscreen);
                stage.setWidth(stageWidthBeforeFullscreen);
                stage.setHeight(stageHeightBeforeFullscreen);
            }
            resolverFullscreen = false;
        }
        applyResolverFullscreenState();
        updateFullscreenButtonText();
        Platform.runLater(() -> {
            ensureCanvasTilesCoverViewport();
            if (problemSplit != null && !statementCollapsed && !resolverFullscreen) {
                problemSplit.setDividerPositions(restoredDivider);
            }
        });
    }

    private void applyResolverFullscreenState() {
        setNodeVisible(titleNode, !resolverFullscreen);
        setNodeVisible(notes, !resolverFullscreen);
        setNodeVisible(existingSolutionNode, !resolverFullscreen);
        setNodeVisible(toolBarNode, true);
        if (rootPane != null) {
            rootPane.setTop(resolverFullscreen ? null : titleNode);
        }
        dialog.setHeaderText(resolverFullscreen ? "" : dialogHeaderText);
    }

    private static void setNodeVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateStatementButtonText() {
        if (collapseStatementButton != null) {
            collapseStatementButton.setText(statementCollapsed ? "Mostrar enunciado" : "Ocultar enunciado");
        }
        setNodeVisible(restoreStatementButton, statementCollapsed);
    }

    private void updateFullscreenButtonText() {
        if (fullscreenButton != null) {
            fullscreenButton.setText(resolverFullscreen ? "Salir de pantalla completa" : "Pantalla completa");
        }
    }

    private boolean hasTransferableSourceImages() {
        return statementSources.stream()
                .map(this::sourceImage)
                .anyMatch(image -> image != null && !image.isError());
    }

    private void transferAllSourceImages() {
        statementSources.stream()
                .map(this::sourceImage)
                .filter(image -> image != null && !image.isError())
                .forEach(this::transferSourceImage);
    }

    private void transferSourceImage(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }
        transferSourceImage(new Image(path.toUri().toString(), true));
    }

    private void transferSourceImage(Image image) {
        if (image == null || image.isError()) {
            return;
        }
        double offset = 32.0 * canvasImages.size();
        CanvasImageItem item = addCanvasImage(
                image,
                48 + offset,
                CANVAS_TITLE_BAND_HEIGHT + 24 + offset,
                Math.min(420, Math.max(160, drawingSurface.logicalWidth() * 0.42)),
                false);
        canvasMode.set(true);
        imageInteractionMode.set(false);
        selectCanvasImage(null);
        canvasTouched.set(true);
    }

    private CanvasImageItem addCanvasImage(Image image, double x, double y, double fitWidth, boolean select) {
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setFitWidth(Math.max(24.0, fitWidth));
        view.getStyleClass().add("technical-problem-canvas-image");
        view.setLayoutX(Math.max(0.0, x));
        view.setLayoutY(Math.max(0.0, y));
        CanvasImageItem item = new CanvasImageItem(view);
        installCanvasImageHandlers(item);
        canvasImages.add(item);
        drawingSurface.imageLayer().getChildren().add(view);
        drawingSurface.markContent(view.getLayoutX() + view.getBoundsInParent().getWidth(),
                view.getLayoutY() + view.getBoundsInParent().getHeight());
        if (select) {
            selectCanvasImage(item);
        }
        return item;
    }

    private void installCanvasImageHandlers(CanvasImageItem item) {
        ImageView view = item.view();
        view.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (imageCropMode && selectedCanvasImage == item) {
                beginImageCropSelection(item, event);
                event.consume();
                return;
            }
            if (!canInteractWithCanvasImages()) {
                return;
            }
            selectCanvasImage(item);
            item.beginDrag(event.getSceneX(), event.getSceneY());
            event.consume();
        });
        view.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (imageCropMode && selectedCanvasImage == item) {
                dragImageCropSelection(item, event);
                event.consume();
                return;
            }
            if (!canInteractWithCanvasImages() || selectedCanvasImage != item) {
                return;
            }
            item.dragTo(event.getSceneX(), event.getSceneY());
            maybeGrowCanvasForPoint(view.getLayoutX() + view.getBoundsInParent().getWidth(),
                    view.getLayoutY() + view.getBoundsInParent().getHeight());
            drawingSurface.markContent(view.getLayoutX() + view.getBoundsInParent().getWidth(),
                    view.getLayoutY() + view.getBoundsInParent().getHeight());
            updateResizeHandles();
            canvasTouched.set(true);
            event.consume();
        });
        view.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (imageCropMode && selectedCanvasImage == item) {
                finishImageCropSelection(item, event);
                event.consume();
            }
        });
    }

    private void selectCanvasImage(CanvasImageItem item) {
        if (selectedCanvasImage != null) {
            selectedCanvasImage.view().getStyleClass().remove("technical-problem-canvas-image-selected");
        }
        selectedCanvasImage = item;
        if (selectedCanvasImage != null
                && !selectedCanvasImage.view().getStyleClass().contains("technical-problem-canvas-image-selected")) {
            selectedCanvasImage.view().getStyleClass().add("technical-problem-canvas-image-selected");
        }
        updateResizeHandles();
        updateImageButtons();
    }

    private void selectCanvasImageUnderPointerForDrawing(Point2D point) {
        if (!canInteractWithCanvasImages() || point == null) {
            return;
        }
        CanvasImageItem item = canvasImageAt(point);
        if (item != null) {
            selectCanvasImage(item);
        }
    }

    private CanvasImageItem canvasImageAt(Point2D point) {
        for (int i = canvasImages.size() - 1; i >= 0; i--) {
            CanvasImageItem item = canvasImages.get(i);
            if (item.view().getBoundsInParent().contains(point)) {
                return item;
            }
        }
        return null;
    }

    private void resizeSelectedImage(double factor) {
        if (selectedCanvasImage == null) {
            return;
        }
        ImageView view = selectedCanvasImage.view();
        double nextWidth = Math.max(48, Math.min(drawingSurface.logicalWidth(), view.getFitWidth() * factor));
        view.setFitWidth(nextWidth);
        maybeGrowCanvasForPoint(view.getLayoutX() + view.getBoundsInParent().getWidth(),
                view.getLayoutY() + view.getBoundsInParent().getHeight());
        drawingSurface.markContent(view.getLayoutX() + view.getBoundsInParent().getWidth(),
                view.getLayoutY() + view.getBoundsInParent().getHeight());
        updateResizeHandles();
        canvasTouched.set(true);
    }

    private void deleteSelectedImage() {
        if (selectedCanvasImage == null) {
            return;
        }
        drawingSurface.imageLayer().getChildren().remove(selectedCanvasImage.view());
        canvasImages.remove(selectedCanvasImage);
        selectCanvasImage(null);
        canvasTouched.set(true);
    }

    private void startImageCropMode() {
        if (selectedCanvasImage == null || !canInteractWithCanvasImages()) {
            return;
        }
        imageCropMode = !imageCropMode;
        if (!imageCropMode) {
            clearImageCropSelectionRectangle();
        }
        updateImageInteractionMode();
    }

    private void beginImageCropSelection(CanvasImageItem item, MouseEvent event) {
        if (item == null) {
            return;
        }
        Point2D point = clampPointToImage(item, event);
        imageCropStartX = point.getX();
        imageCropStartY = point.getY();
        if (imageCropSelectionRectangle == null) {
            imageCropSelectionRectangle = new Rectangle();
            imageCropSelectionRectangle.getStyleClass().add("technical-problem-canvas-region-selection");
            imageCropSelectionRectangle.setMouseTransparent(true);
            drawingSurface.getChildren().add(imageCropSelectionRectangle);
        }
        updateImageCropSelectionRectangle(imageCropStartX, imageCropStartY, imageCropStartX, imageCropStartY);
        imageCropSelectionRectangle.toFront();
    }

    private void dragImageCropSelection(CanvasImageItem item, MouseEvent event) {
        if (item == null || imageCropSelectionRectangle == null) {
            return;
        }
        Point2D point = clampPointToImage(item, event);
        updateImageCropSelectionRectangle(imageCropStartX, imageCropStartY, point.getX(), point.getY());
    }

    private void finishImageCropSelection(CanvasImageItem item, MouseEvent event) {
        if (item == null || imageCropSelectionRectangle == null) {
            cancelImageCropMode();
            return;
        }
        Point2D point = clampPointToImage(item, event);
        double minX = Math.min(imageCropStartX, point.getX());
        double minY = Math.min(imageCropStartY, point.getY());
        double width = Math.abs(point.getX() - imageCropStartX);
        double height = Math.abs(point.getY() - imageCropStartY);
        if (width >= 8.0 && height >= 8.0) {
            applyImageCrop(item, minX, minY, width, height);
        }
        cancelImageCropMode();
    }

    private void cancelImageCropMode() {
        imageCropMode = false;
        clearImageCropSelectionRectangle();
        updateImageInteractionMode();
    }

    private void clearImageCropSelectionRectangle() {
        if (imageCropSelectionRectangle != null) {
            drawingSurface.getChildren().remove(imageCropSelectionRectangle);
            imageCropSelectionRectangle = null;
        }
    }

    private Point2D clampPointToImage(CanvasImageItem item, MouseEvent event) {
        Point2D local = drawingSurface.sceneToLocal(event.getSceneX(), event.getSceneY());
        Bounds bounds = item.view().getBoundsInParent();
        return new Point2D(
                clamp(local.getX(), bounds.getMinX(), bounds.getMaxX()),
                clamp(local.getY(), bounds.getMinY(), bounds.getMaxY()));
    }

    private void updateImageCropSelectionRectangle(double x1, double y1, double x2, double y2) {
        if (imageCropSelectionRectangle == null) {
            return;
        }
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        imageCropSelectionRectangle.setX(minX);
        imageCropSelectionRectangle.setY(minY);
        imageCropSelectionRectangle.setWidth(Math.abs(x2 - x1));
        imageCropSelectionRectangle.setHeight(Math.abs(y2 - y1));
    }

    private void applyImageCrop(CanvasImageItem item, double minX, double minY, double cropWidthCanvas, double cropHeightCanvas) {
        if (item == null) {
            return;
        }
        selectedCanvasImage = item;
        ImageView view = item.view();
        Image image = view.getImage();
        if (image == null || image.getPixelReader() == null) {
            return;
        }
        Bounds imageBounds = view.getBoundsInParent();
        double maxX = Math.min(imageBounds.getMaxX(), minX + cropWidthCanvas);
        double maxY = Math.min(imageBounds.getMaxY(), minY + cropHeightCanvas);
        minX = Math.max(imageBounds.getMinX(), minX);
        minY = Math.max(imageBounds.getMinY(), minY);
        cropWidthCanvas = maxX - minX;
        cropHeightCanvas = maxY - minY;
        if (cropWidthCanvas < 8.0 || cropHeightCanvas < 8.0
                || imageBounds.getWidth() <= 0.0 || imageBounds.getHeight() <= 0.0) {
            return;
        }
        int imageWidth = Math.max(1, (int) Math.round(image.getWidth()));
        int imageHeight = Math.max(1, (int) Math.round(image.getHeight()));
        int sourceX = clampInt((int) Math.floor((minX - imageBounds.getMinX()) / imageBounds.getWidth() * imageWidth), 0, imageWidth - 1);
        int sourceY = clampInt((int) Math.floor((minY - imageBounds.getMinY()) / imageBounds.getHeight() * imageHeight), 0, imageHeight - 1);
        int sourceWidth = clampInt((int) Math.ceil(cropWidthCanvas / imageBounds.getWidth() * imageWidth), 1, imageWidth - sourceX);
        int sourceHeight = clampInt((int) Math.ceil(cropHeightCanvas / imageBounds.getHeight() * imageHeight), 1, imageHeight - sourceY);
        WritableImage cropped = new WritableImage(image.getPixelReader(), sourceX, sourceY, sourceWidth, sourceHeight);
        item.cropTo(cropped, minX, minY, cropWidthCanvas);
        drawingSurface.markContentBounds(minX, minY, minX + cropWidthCanvas, minY + cropHeightCanvas);
        hideCanvasRegionSelection();
        selectCanvasImage(item);
        canvasTouched.set(true);
    }

    private void restoreSelectedImageCrop() {
        if (selectedCanvasImage == null || !selectedCanvasImage.isCropped()) {
            return;
        }
        selectedCanvasImage.restoreCrop();
        drawingSurface.markContent(selectedCanvasImage.view().getLayoutX() + selectedCanvasImage.view().getBoundsInParent().getWidth(),
                selectedCanvasImage.view().getLayoutY() + selectedCanvasImage.view().getBoundsInParent().getHeight());
        updateResizeHandles();
        updateImageButtons();
        canvasTouched.set(true);
    }

    private void updateImageButtons() {
        boolean enabled = canInteractWithCanvasImages() && selectedCanvasImage != null;
        boolean imageToolsVisible = canInteractWithCanvasImages();
        if (shrinkImageButton != null) {
            setNodeVisible(shrinkImageButton, imageToolsVisible);
            shrinkImageButton.setDisable(!enabled);
        }
        if (enlargeImageButton != null) {
            setNodeVisible(enlargeImageButton, imageToolsVisible);
            enlargeImageButton.setDisable(!enabled);
        }
        if (deleteImageButton != null) {
            setNodeVisible(deleteImageButton, imageToolsVisible);
            deleteImageButton.setDisable(!enabled);
        }
        if (cropImageButton != null) {
            setNodeVisible(cropImageButton, imageToolsVisible);
            cropImageButton.setText(imageCropMode ? "Cancelar recorte" : "Recortar imagen");
            cropImageButton.setDisable(!enabled);
        }
        if (restoreImageCropButton != null) {
            setNodeVisible(restoreImageCropButton, imageToolsVisible && selectedCanvasImage != null && selectedCanvasImage.isCropped());
            restoreImageCropButton.setDisable(!enabled || selectedCanvasImage == null || !selectedCanvasImage.isCropped());
        }
    }

    private void installImageResizeHandles() {
        if (!imageResizeHandles.isEmpty()) {
            return;
        }
        for (String arrow : List.of("\u2196", "\u2197", "\u2199", "\u2198")) {
            Label handle = new Label(arrow);
            handle.setManaged(false);
            handle.setCursor(Cursor.SE_RESIZE);
            handle.setVisible(false);
            handle.getStyleClass().add("technical-problem-image-resize-handle");
            StudioFormControls.installTooltip(handle, "Arrastra para redimensionar la imagen manteniendo proporcion.");
            handle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (selectedCanvasImage == null || !canInteractWithCanvasImages()) {
                    return;
                }
                event.consume();
            });
            handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
                if (selectedCanvasImage == null || !canInteractWithCanvasImages()) {
                    return;
                }
                resizeSelectedImageFromHandle(event.getSceneX(), event.getSceneY());
                event.consume();
            });
            imageResizeHandles.add(handle);
        }
        drawingSurface.inkInputLayer().getChildren().addAll(imageResizeHandles);
    }

    private void resizeSelectedImageFromHandle(double sceneX, double sceneY) {
        if (selectedCanvasImage == null) {
            return;
        }
        ImageView view = selectedCanvasImage.view();
        Bounds bounds = view.getBoundsInParent();
        double width = Math.max(1.0, bounds.getWidth());
        double height = Math.max(1.0, bounds.getHeight());
        double centerX = bounds.getMinX() + width / 2.0;
        double centerY = bounds.getMinY() + height / 2.0;
        Point2D local = drawingSurface.sceneToLocal(sceneX, sceneY);
        double imageRatio = imageRatio(view, width, height);
        double nextWidth = Math.max(48.0, Math.min(drawingSurface.logicalWidth(),
                Math.abs(local.getX() - centerX) * 2.0));
        double nextHeight = nextWidth * imageRatio;
        view.setFitWidth(nextWidth);
        view.setLayoutX(Math.max(0.0, centerX - nextWidth / 2.0));
        view.setLayoutY(Math.max(0.0, centerY - nextHeight / 2.0));
        maybeGrowCanvasForPoint(view.getLayoutX() + nextWidth, view.getLayoutY() + nextHeight);
        drawingSurface.markContent(view.getLayoutX() + nextWidth, view.getLayoutY() + nextHeight);
        updateResizeHandles();
        canvasTouched.set(true);
    }

    private static double imageRatio(ImageView view, double fallbackWidth, double fallbackHeight) {
        Image image = view == null ? null : view.getImage();
        if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
            return image.getHeight() / image.getWidth();
        }
        return fallbackWidth <= 0 ? 1.0 : Math.max(0.1, fallbackHeight / fallbackWidth);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateResizeHandles() {
        boolean visible = canInteractWithCanvasImages() && selectedCanvasImage != null;
        for (Label handle : imageResizeHandles) {
            handle.setVisible(visible);
            handle.setMouseTransparent(!visible);
        }
        if (!visible) {
            return;
        }
        Bounds bounds = selectedCanvasImage.view().getBoundsInParent();
        double size = 18.0;
        double left = bounds.getMinX() - size / 2.0;
        double top = bounds.getMinY() - size / 2.0;
        double right = bounds.getMaxX() - size / 2.0;
        double bottom = bounds.getMaxY() - size / 2.0;
        double[][] positions = {{left, top}, {right, top}, {left, bottom}, {right, bottom}};
        for (int i = 0; i < imageResizeHandles.size(); i++) {
            Label handle = imageResizeHandles.get(i);
            handle.resizeRelocate(positions[i][0], positions[i][1], size, size);
            handle.toFront();
        }
    }

    private String defaultTitle() {
        return "Problema tecnico";
    }

    private String combinedStatementText() {
        String text = statementSources.stream()
                .map(StatementSource::displayText)
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .reduce("", (left, right) -> left.isBlank() ? right : left + System.lineSeparator() + System.lineSeparator() + right);
        return text.isBlank() ? "Sin enunciado seleccionado." : text;
    }

    private static List<StatementSource> statementSources(List<DocumentBlock> sourceBlocks, Map<String, Path> sourceCropPaths) {
        List<DocumentBlock> blocks = sourceBlocks == null ? List.of() : List.copyOf(sourceBlocks);
        Map<String, Path> crops = sourceCropPaths == null ? Map.of() : sourceCropPaths;
        return blocks.stream()
                .map(block -> new StatementSource(
                        block.id(),
                        block.text(),
                        block.metadata().getOrDefault("sourcePage", ""),
                        crops.get(block.id()),
                        block.metadata().getOrDefault("embeddedImageBase64", "")))
                .toList();
    }

    private static List<StatementSource> statementSourcesFromDrafts(List<StudyProblemSourceDraft> sourceDrafts) {
        return (sourceDrafts == null ? List.<StudyProblemSourceDraft>of() : List.copyOf(sourceDrafts)).stream()
                .map(source -> new StatementSource(
                        source.sourceId(),
                        source.selectedText(),
                        source.sourcePage(),
                        source.sourceCropPath(),
                        source.embeddedImageBase64()))
                .toList();
    }

    private static Map<String, Path> sourceCropPathsFromDrafts(List<StudyProblemSourceDraft> sourceDrafts) {
        Map<String, Path> paths = new java.util.LinkedHashMap<>();
        for (StudyProblemSourceDraft source : sourceDrafts == null ? List.<StudyProblemSourceDraft>of() : sourceDrafts) {
            if (source != null && source.sourceCropPath() != null && Files.isRegularFile(source.sourceCropPath())) {
                paths.put(source.sourceId(), source.sourceCropPath());
            }
        }
        return Map.copyOf(paths);
    }

    private static List<StatementSource> statementSources(StudyProblemDetail detail) {
        if (detail == null) {
            return List.of();
        }
        return detail.sources().stream()
                .map(TechnicalProblemDialog::statementSource)
                .toList();
    }

    private static StatementSource statementSource(StudyProblemSourceProjection source) {
        return new StatementSource(source.blockId(), source.selectedText(), source.sourcePage(), source.sourceCropPath(), "");
    }

    private record StatementSource(String id, String text, String sourcePage, Path cropPath, String imageBase64) {
        private boolean hasVisual() {
            return (cropPath != null && Files.isRegularFile(cropPath)) || (imageBase64 != null && !imageBase64.isBlank());
        }

        private String displayText() {
            String normalized = text == null ? "" : text.strip();
            if (hasVisual() && normalized.toLowerCase(java.util.Locale.ROOT).contains("imagen detectada")) {
                return "";
            }
            return normalized;
        }
    }

    private record CanvasRegionSelection(double x, double y, double width, double height) {
        private static CanvasRegionSelection from(double x1, double y1, double x2, double y2) {
            double minX = Math.min(x1, x2);
            double minY = Math.min(y1, y2);
            return new CanvasRegionSelection(minX, minY, Math.abs(x2 - x1), Math.abs(y2 - y1));
        }

        private boolean contains(Bounds bounds) {
            return bounds != null
                    && bounds.getMinX() >= x
                    && bounds.getMinY() >= y
                    && bounds.getMaxX() <= x + width
                    && bounds.getMaxY() <= y + height;
        }
    }

    private record CanvasUndoSnapshot(WritableImage raster,
                                      List<StudyProblemCanvasSurface.InkStrokeState> strokes,
                                      List<StudyProblemCanvasSurface.InkCommandState> commands) {
        private CanvasUndoSnapshot {
            strokes = strokes == null ? List.of() : List.copyOf(strokes);
            commands = commands == null ? List.of() : List.copyOf(commands);
        }

        private static CanvasUndoSnapshot raster(WritableImage raster) {
            return new CanvasUndoSnapshot(raster, null, null);
        }

        private static CanvasUndoSnapshot vector(List<StudyProblemCanvasSurface.InkStrokeState> strokes,
                                                 List<StudyProblemCanvasSurface.InkCommandState> commands) {
            return new CanvasUndoSnapshot(null, strokes, commands);
        }

        private boolean vector() {
            return raster == null;
        }
    }

    private static final class CanvasImageItem {
        private final ImageView view;
        private Image originalImage;
        private double originalLayoutX;
        private double originalLayoutY;
        private double originalFitWidth;
        private boolean cropActive;
        private double dragSceneX;
        private double dragSceneY;
        private double dragLayoutX;
        private double dragLayoutY;

        private CanvasImageItem(ImageView view) {
            this.view = view;
            this.originalImage = view.getImage();
            this.originalLayoutX = view.getLayoutX();
            this.originalLayoutY = view.getLayoutY();
            this.originalFitWidth = view.getFitWidth();
        }

        private ImageView view() {
            return view;
        }

        private Image originalImage() {
            return originalImage;
        }

        private double originalLayoutX() {
            return originalLayoutX;
        }

        private double originalLayoutY() {
            return originalLayoutY;
        }

        private double originalFitWidth() {
            return originalFitWidth;
        }

        private void beginDrag(double sceneX, double sceneY) {
            dragSceneX = sceneX;
            dragSceneY = sceneY;
            dragLayoutX = view.getLayoutX();
            dragLayoutY = view.getLayoutY();
        }

        private void dragTo(double sceneX, double sceneY) {
            view.setLayoutX(Math.max(0, dragLayoutX + sceneX - dragSceneX));
            view.setLayoutY(Math.max(0, dragLayoutY + sceneY - dragSceneY));
        }

        private boolean isCropped() {
            return cropActive;
        }

        private void cropTo(Image cropped, double layoutX, double layoutY, double fitWidth) {
            if (!cropActive) {
                originalLayoutX = view.getLayoutX();
                originalLayoutY = view.getLayoutY();
                originalFitWidth = view.getFitWidth();
            }
            view.setImage(cropped);
            view.setLayoutX(Math.max(0.0, layoutX));
            view.setLayoutY(Math.max(0.0, layoutY));
            view.setFitWidth(Math.max(24.0, fitWidth));
            cropActive = true;
        }

        private void restoreCrop() {
            view.setImage(originalImage);
            view.setLayoutX(Math.max(0.0, originalLayoutX));
            view.setLayoutY(Math.max(0.0, originalLayoutY));
            view.setFitWidth(Math.max(24.0, originalFitWidth));
            cropActive = false;
        }

        private void restoreCropMetadata(Image originalImage, double originalLayoutX, double originalLayoutY,
                                         double originalFitWidth, boolean cropActive) {
            this.originalImage = originalImage == null ? view.getImage() : originalImage;
            this.originalLayoutX = originalLayoutX;
            this.originalLayoutY = originalLayoutY;
            this.originalFitWidth = originalFitWidth;
            this.cropActive = cropActive;
        }
    }
}
