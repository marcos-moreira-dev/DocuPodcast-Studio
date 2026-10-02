package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingToolId;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportResult;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkImageFileStore;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemDetail;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceProjection;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkImageCrop;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPlacedImage;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppStyles;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.LucideIconView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCanvasToolbar;
import com.marcosmoreiradev.docupodcaststudio.ink.InkRealtimeStrokeEngine;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasViewport;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasZoomPane;
import com.marcosmoreiradev.docupodcaststudio.ink.controls.InkPressureIndicator;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.NoopInkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.control.ToggleGroup;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Modal resolver for technical problems. */
public final class TechnicalProblemDialog {
    private static final double DIALOG_PREF_WIDTH = 1220;
    private static final double DIALOG_PREF_HEIGHT = 760;
    private static final double CANVAS_TITLE_BAND_HEIGHT = 64.0;

    private enum CanvasTool {
        NONE, PEN, STRAIGHT_LINE, ANGLE_MEASURE, ERASER, PAN, REGION, TEXT
    }

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

    private final Dialog<TechnicalProblemResult> dialog = StudioDialogShell.dialog();
    private final DrawingProfile drawingProfile;
    private final Map<String, javafx.beans.property.DoubleProperty> sourceOpacities = new HashMap<>();
    private final List<StatementSource> statementSources;
    private final List<StudyProblemSourceDraft> additionalSourceDrafts = new ArrayList<>();
    private final Map<String, Path> sourceCropPaths;
    private final Path existingSolutionImagePath;
    private final Path existingCanvasStatePath;
    private final boolean editMode;
    private final boolean expressMode;
    private final TextField title = StudioFormControls.textField();
    private final TextArea solutionText = StudioFormControls.textArea();
    private final TextArea notes = StudioFormControls.textArea();
    private final StudyProblemCanvasSurface drawingSurface = new StudyProblemCanvasSurface();
    private final InkInputProvider inkInputProvider;
    private TechnicalProblemEditorController<CanvasUndoSnapshot> editorController;
    private InkCanvasViewport inkViewport;
    private final ColorPicker penColor = StudioFormControls.colorPicker(Color.BLACK);
    private final ColorPicker backgroundColor = StudioFormControls.colorPicker(Color.WHITE);
    private final Slider penWidth = StudioFormControls.slider(1, 24, 3);
    private final Circle penWidthPreview = new Circle(3);
    private final Slider canvasZoom = StudioFormControls.slider(50, 200, 100);
    private final Label canvasZoomValue = new Label("100%");
    private final ToggleButton eraser = StudioFormControls.toggleButton("Borrador");
    private final BooleanProperty canvasTouched = new SimpleBooleanProperty(false);
    private final BooleanProperty canvasMode = new SimpleBooleanProperty(true);
    private final BooleanProperty drawMode = new SimpleBooleanProperty(true);
    private final BooleanProperty canvasRegionSelectionMode = new SimpleBooleanProperty(false);
    private final BooleanProperty imageInteractionMode = new SimpleBooleanProperty(false);
    private final ObjectProperty<CanvasTool> activeCanvasTool = new SimpleObjectProperty<>(CanvasTool.PEN);
    private final List<CanvasImageItem> canvasImages = new ArrayList<>();
    private com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection vectorSelection;
    private boolean vectorClipboard;
    private Button imageRotateHandle;
    private final List<Label> imageResizeHandles = new ArrayList<>();
    private SplitPane problemSplit;
    private BorderPane rootPane;
    private Node statementNode;
    private Node resolverNode;
    private Node modeBarNode;
    private Node existingSolutionNode;
    private Node toolBarNode;
    private Node titleNode;
    private InkPressureIndicator inputDiagnosticLabel;
    private VBox statementVisuals;
    private TextArea statementTextArea;
    private Button transferAllButton;
    private HBox statementActions;
    private Label canvasTitleLabel;
    private ScrollPane canvasScroll;
    private InkCanvasZoomPane canvasZoomPane;
    private Button collapseStatementButton;
    private Button restoreStatementButton;
    private Button fullscreenButton;
    private final javafx.scene.control.ComboBox<String> textFont = StudioFormControls.comboBox();
    private final javafx.scene.control.ComboBox<String> textEffect = StudioFormControls.comboBox();
    private final ColorPicker shapeFillColor = StudioFormControls.colorPicker(Color.LIGHTBLUE);
    private HBox shapeFillActions;
    private final BooleanProperty objectSelectionVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty regionSelectionVisible = new SimpleBooleanProperty(false);
    private Button fillShapeButton;
    private Button clearShapeFillButton;
    private final ColorPicker textColor = StudioFormControls.colorPicker(Color.BLACK);
    private final ColorPicker textEffectColor = StudioFormControls.colorPicker(Color.BLACK);
    private FlowPane textStyleBar;
    private boolean syncingTextStyle;
    private TextArea canvasTextEditor;

    private Node toolbarShellNode;
    private boolean secondaryPanning;
    private boolean canvasDisposed;
    private final PauseTransition canvasTrimDebounce = new PauseTransition(Duration.seconds(3));
    private double panSceneX;
    private double panSceneY;
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
    private final List<Node> measurementOverlayNodes = new ArrayList<>();
    private int draggedAnglePoint = -1;
    private final List<Point2D> angleMeasurementPoints = new ArrayList<>();
    private final Label measurementStatus = new Label("Medición: selecciona Medir ángulo para comenzar.");
    private Point2D straightLineStart;
    private boolean statementCollapsed;
    private boolean resolverFullscreen;
    private boolean statementCollapsedBeforeFullscreen;
    private double restoredDivider = 0.25;
    private InkRealtimeStrokeEngine inkEngine;
    private double currentInputPressure = 1.0;
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
        editorController = new TechnicalProblemEditorController<>(drawingProfile, inkInputProvider,
                () -> snapshotUndoState(false), this::restoreUndoSnapshot,
                (state, destination, exportProfile) -> destination);
        inkViewport = new InkCanvasViewport(drawingSurface, drawingProfile);
        dialog.setTitle(dialogTitle);
        dialogHeaderText = expressMode ? null : header;
        dialog.setHeaderText(dialogHeaderText);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        saveButtonType = NativeDialogResponse.button(saveLabel,
                expressMode ? ButtonBar.ButtonData.CANCEL_CLOSE : ButtonBar.ButtonData.OK_DONE);
        if (editMode) {
            saveAndExportButtonType = null;
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        } else {
            saveAndExportButtonType = NativeDialogResponse.button(expressMode ? "Exportar PNG..." : "Guardar + exportar PNG...", ButtonBar.ButtonData.APPLY);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, saveAndExportButtonType);
            if (!expressMode) dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
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
        title.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) event.consume();
        });
        dialog.setOnCloseRequest(event -> {
            ButtonType close = new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE);
            Alert confirmation = com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDecisionDialog.create(
                    dialog.getDialogPane().getScene().getWindow(), Alert.AlertType.CONFIRMATION,
                    "¿Seguro que quieres cerrar el ejercicio?", close, ButtonType.CANCEL);
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != close) event.consume();
        });
        dialog.setOnHidden(event -> disposeCanvasInput());
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, this::handleDialogKeyPressed);
        canvasGrowDebounce.setOnFinished(event -> runPendingCanvasGrowth());
        canvasTrimDebounce.setOnFinished(event -> trimUnusedCanvas());
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
        if (event.getCode() == KeyCode.DELETE && !textInputOwnsShortcut(event)
                && canvasRegionSelectionMode.get() && vectorSelection != null && vectorSelection.hasSelection()) {
            vectorSelection.deleteSelection(); event.consume(); return;
        }
        if (event.getCode() == KeyCode.DELETE && !textInputOwnsShortcut(event)
                && canInteractWithCanvasImages() && selectedCanvasImage != null) {
            deleteSelectedImage();
            event.consume();
            return;
        }
        if ((event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.F11) && resolverFullscreen) {
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
        FileChooser chooser = NativeSourceChooser.fileChooser();
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

        BorderPane.setMargin(titleBox, new Insets(0, 0, 10, 0));

        statementNode = statementPane();
        resolverNode = resolverPane();
        problemSplit = StudioViewportControls.splitPane(statementNode, resolverNode);
        problemSplit.getStyleClass().add("technical-problem-split");
        problemSplit.setDividerPositions(restoredDivider);
        SplitPane.setResizableWithParent(statementNode, Boolean.TRUE);
        SplitPane.setResizableWithParent(resolverNode, Boolean.TRUE);
        root.setCenter(problemSplit);
        HBox restorePanelRow = new HBox(restoreStatementButton);
        restorePanelRow.setAlignment(Pos.CENTER_LEFT);
        root.setBottom(restorePanelRow);
        return root;
    }

    private Button panelFoldButton(String symbol, String description) {
        Button button = ActionButtonFactory.secondary("");
        button.setText(symbol);
        button.setGraphic(null);
        button.getStyleClass().add(AppStyles.UI_RIBBON_COLLAPSE_TOGGLE);
        button.setStyle("-fx-min-width: 22; -fx-pref-width: 22; -fx-max-width: 22; -fx-padding: 0;");
        button.setAccessibleText(description);
        StudioFormControls.installTooltip(button, description);
        button.setOnAction(event -> toggleStatementCollapsed());
        return button;
    }

    private BorderPane statementPane() {
        Label heading = new Label(expressMode ? "Banco de imagenes" : "Enunciado");
        heading.getStyleClass().add("technical-problem-section-title");
        collapseStatementButton = panelFoldButton("◂", "Plegar recursos y páginas");
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

        statementActions.setAlignment(Pos.CENTER_LEFT);
        statementActions.getStyleClass().add("technical-problem-statement-actions");
        HBox header = new HBox(8, heading);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("technical-problem-statement-header");
        VBox headerBox = new VBox(8, statementActions, header);
        headerBox.getStyleClass().add("technical-problem-statement-header-box");

        statementTextArea = StudioFormControls.textArea(expressMode
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
        VBox content = expressMode ? new VBox(10, statementVisuals)
                : new VBox(10, statementTextArea, statementVisuals);
        content.getStyleClass().add("technical-problem-statement");
        ScrollPane scroll = StudioViewportControls.scrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(460);
        scroll.setPrefViewportHeight(560);
        scroll.getStyleClass().add("technical-problem-statement-scroll");
        BorderPane resources = new BorderPane(scroll);
        resources.setTop(headerBox);
        javafx.scene.control.Tab resourcesTab = StudioNavigationControls.tab("Recursos", resources);
        javafx.scene.control.Tab pagesTab = StudioNavigationControls.tab("Páginas", pageNavigator());
        resourcesTab.setClosable(false);
        pagesTab.setClosable(false);
        javafx.scene.control.TabPane tabs = StudioNavigationControls.tabPane(resourcesTab, pagesTab, canvasSettingsTab());
        tabs.setMinWidth(0);
        tabs.getStyleClass().add("technical-notebook-tabs");
        pagesTab.setOnSelectionChanged(event -> {
            if (pagesTab.isSelected()) Platform.runLater(this::refreshPageThumbnail);
        });
        BorderPane pane = new BorderPane(tabs);
        HBox fold = new HBox(collapseStatementButton);
        fold.setAlignment(Pos.CENTER_RIGHT);
        pane.setBottom(fold);
        pane.getStyleClass().add("technical-problem-statement-pane");
        return pane;
    }

    private javafx.scene.control.Tab canvasSettingsTab() {
        javafx.scene.control.ComboBox<String> paper = new javafx.scene.control.ComboBox<>();
        paper.getItems().setAll("En blanco", "A líneas", "A cuadros", "Isométrico", "Polar");
        paper.setValue("En blanco");
        List<String> patterns = List.of("blank", "ruled", "grid", "isometric", "polar");
        paper.setOnAction(event -> {
            int index = paper.getSelectionModel().getSelectedIndex();
            if (index >= 0 && drawingSurface != null && !patterns.get(index).equals(drawingSurface.paperPattern())) {
                drawingSurface.setPaperPattern(patterns.get(index));
                canvasTouched.set(true);
                refreshPageThumbnail();
            }
        });
        Label help = new Label("La plantilla es el fondo de la hoja. Se guarda con el lienzo y aparece en la exportación.");
        help.setWrapText(true);
        VBox body = new VBox(10, titleNode, new Label("Notas internas del estudio"), notes,
                new Label("Plantilla de hoja"), paper, help);
        body.setPadding(new Insets(12));
        ScrollPane settingsScroll = StudioViewportControls.scrollPane(body);
        settingsScroll.setFitToWidth(true);
        javafx.scene.control.Tab tab = StudioNavigationControls.tab("Lienzo", settingsScroll);
        tab.setClosable(false);
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected() && drawingSurface != null) {
                int index = patterns.indexOf(drawingSurface.paperPattern());
                paper.getSelectionModel().select(Math.max(0, index));
            }
        });
        return tab;
    }

    private ImageView pageThumbnail;
    private final List<String> notebookPages = new ArrayList<>();
    private final List<Image> notebookThumbnails = new ArrayList<>();
    private int currentPage;
    private VBox pageList;

    private Node pageNavigator() {
        pageList = new VBox(8);
        Button add = ActionButtonFactory.secondary("Nueva página", "Crear una hoja vacía.", this::addNotebookPage);
        Button previous = ActionButtonFactory.secondary("Anterior", "Ir a la página anterior.", () -> selectNotebookPage(currentPage - 1));
        Button next = ActionButtonFactory.secondary("Siguiente", "Ir a la página siguiente.", () -> selectNotebookPage(currentPage + 1));
        FlowPane navigation = new FlowPane(6, 6, previous, next);
        navigation.setPadding(new Insets(8));
        VBox content = new VBox(10, add, pageList);
        content.setPadding(new Insets(12));
        ScrollPane viewport = StudioViewportControls.scrollPane(content);
        viewport.setFitToWidth(true);
        BorderPane pane = new BorderPane(viewport);
        pane.setBottom(navigation);
        return pane;
    }

    private void ensureNotebook() {
        if (notebookPages.isEmpty()) {
            notebookPages.add("");
            notebookThumbnails.add(null);
        }
    }

    private void addNotebookPage() {
        ensureNotebook();
        notebookPages.set(currentPage, canvasPageStateJson(false));
        refreshPageThumbnail();
        notebookPages.add("");
        notebookThumbnails.add(null);
        selectNotebookPage(notebookPages.size() - 1);
    }

    private void selectNotebookPage(int index) {
        ensureNotebook();
        if (index < 0 || index >= notebookPages.size() || index == currentPage) return;
        notebookPages.set(currentPage, canvasPageStateJson(false));
        refreshPageThumbnail();
        currentPage = index;
        String state = notebookPages.get(index);
        clearAngleMeasurement();
        if (state.isBlank()) {
            clearCanvasImages();
            drawingSurface.resetForEditableState(1024, 1024, backgroundColor.getValue());
        } else if (!restoreCanvasJson(state)) {
            throw new IllegalStateException("No se pudo recuperar la página " + (index + 1));
        }
        editorController.restore(snapshotUndoState(false));
        canvasScroll.setVvalue(0);
        canvasScroll.setHvalue(0);
        canvasTouched.set(true);
        refreshPageThumbnail();
    }

    private void rebuildPageList() {
        if (pageList == null) return;
        pageList.getChildren().clear();
        for (int i = 0; i < notebookPages.size(); i++) {
            final int index = i;
            ImageView thumb = new ImageView(notebookThumbnails.get(i));
            thumb.setPreserveRatio(true);
            thumb.setFitWidth(140);
            thumb.setFitHeight(170);
            Button page = ActionButtonFactory.secondary("Página " + (i + 1), "Abrir página " + (i + 1), () -> selectNotebookPage(index));
            page.setGraphic(thumb);
            page.setContentDisplay(ContentDisplay.TOP);
            page.setMaxWidth(180);
            if (i == currentPage) page.setStyle("-fx-border-color: -docu-accent;");
            pageList.getChildren().add(page);
        }
    }

    private void refreshPageThumbnail() {
        if (drawingSurface == null) return;
        ensureNotebook();
        pageThumbnail = new ImageView();
        flushInk();
        double scale = Math.min(200.0 / drawingSurface.logicalWidth(), 240.0 / drawingSurface.logicalHeight());
        javafx.scene.SnapshotParameters parameters = new javafx.scene.SnapshotParameters();
        parameters.setTransform(new javafx.scene.transform.Scale(scale, scale));
        parameters.setViewport(new javafx.geometry.Rectangle2D(0, 0,
                Math.ceil(drawingSurface.logicalWidth() * scale), Math.ceil(drawingSurface.logicalHeight() * scale)));
        pageThumbnail.setImage(drawingSurface.snapshot(parameters, null));
        notebookThumbnails.set(currentPage, pageThumbnail.getImage());
        rebuildPageList();
    }

    private void loadExternalImage() {
        Window owner = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
        FileChooser chooser = NativeSourceChooser.fileChooser();
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
        Alert alert = NativeDialogResponse.alert(Alert.AlertType.WARNING, "", ButtonType.OK);
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
        return InkImageFileStore.normalizedTempPng(source, "docupodcast-study-external-");
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
        int index = statementActions.getChildren().size();
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
                () -> transferSourceImage(source, image));
        Slider opacity = StudioFormControls.slider(0, 100, sourceOpacity(source.id()).get() * 100);
        opacity.setAccessibleText("Opacidad de la imagen");
        StudioFormControls.installTooltip(opacity, "0%: transparente. 100%: totalmente visible.");
        opacity.setMinWidth(60);
        opacity.setMaxWidth(Double.MAX_VALUE);
        Label percent = new Label();
        percent.textProperty().bind(opacity.valueProperty().asString("%.0f%%"));
        percent.setMinWidth(40);
        imageView.opacityProperty().bind(sourceOpacity(source.id()));
        sourceOpacity(source.id()).addListener((o, before, value) -> opacity.setValue(value.doubleValue() * 100));
        opacity.valueChangingProperty().addListener((o, before, dragging) -> { if (dragging) rememberUndo(); });
        opacity.valueProperty().addListener((o, before, value) -> {
            double alpha = value.doubleValue() / 100;
            if (Math.abs(sourceOpacity(source.id()).get() - alpha) < 0.000001) return;
            if (!opacity.isValueChanging()) rememberUndo();
            sourceOpacity(source.id()).set(alpha);
            for (CanvasImageItem item : canvasImages) {
                if (source.id().equals(item.sourceId)) item.view.setOpacity(alpha);
            }
            canvasTouched.set(true);
        });
        HBox opacityRow = new HBox(6, new Label("Opacidad"), opacity, percent);
        opacityRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(opacity, Priority.ALWAYS);
        VBox box = new VBox(7, imageView, opacityRow, transfer);
        imageView.fitWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> 0.7 * Math.max(180.0, Math.min(720.0, box.getWidth() <= 0 ? 560.0 : box.getWidth() - 18.0)),
                box.widthProperty()));
        box.getStyleClass().add("technical-problem-source-image-box");
        return Optional.of(box);
    }

    private javafx.beans.property.DoubleProperty sourceOpacity(String id) {
        return sourceOpacities.computeIfAbsent(id, key -> new javafx.beans.property.SimpleDoubleProperty(1));
    }

    private static double readOpacity(String value) {
        try { double alpha = Double.parseDouble(value); return Double.isFinite(alpha) ? Math.max(0, Math.min(1, alpha)) : 1; }
        catch (RuntimeException invalid) { return 1; }
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
        return InkImageFileStore.decodePngBase64(source.imageBase64());
    }

    private VBox resolverPane() {
        canvasMode.set(true);
        canvasMode.addListener((obs, oldValue, newValue) -> updateImageInteractionMode());
        drawMode.addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue) && canvasRegionSelectionMode.get()) {
                canvasRegionSelectionMode.set(false);
            }
            updateImageInteractionMode();
            resetInkCoordinateState();
        });
        canvasRegionSelectionMode.addListener((obs, oldValue, newValue) -> {
            if (vectorSelection != null) vectorSelection.setActive(Boolean.TRUE.equals(newValue));
            if (Boolean.TRUE.equals(newValue)) {
                drawMode.set(false);
                imageInteractionMode.set(true);
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
        restoreStatementButton = panelFoldButton("▸", "Mostrar recursos y páginas");
        restoreStatementButton.getStyleClass().add("technical-problem-statement-restore");
        setNodeVisible(restoreStatementButton, false);
        selectRegionButton.setMinWidth(156);
        fullscreenButton.setMinWidth(156);

        inputDiagnosticLabel = new InkPressureIndicator(editorController.inputStatusProperty());
        FlowPane mode = new FlowPane(8, 8, fullscreenButton);
        mode.setAlignment(Pos.CENTER_LEFT);
        mode.getStyleClass().addAll("technical-problem-mode-toggle", "technical-problem-mode-flow");
        mode.getChildren().add(inputDiagnosticLabel);
        VBox modeSection = new VBox(0, mode);

        modeBarNode = modeSection;

        solutionText.setWrapText(true);
        solutionText.setPromptText("Escribe la solucion, notas o pasos algebraicos aqui.");
        solutionText.getStyleClass().add("technical-problem-text-solution");
        notes.setWrapText(true);
        notes.setPromptText("Notas internas del estudio.");
        notes.setPrefRowCount(3);
        notes.getStyleClass().add("technical-problem-notes");

        Node canvasEditor = canvasPane();
        StackPane resolverStack = new StackPane(canvasEditor);
        VBox.setVgrow(resolverStack, Priority.ALWAYS);
        solutionText.visibleProperty().bind(canvasMode.not());
        solutionText.managedProperty().bind(solutionText.visibleProperty());
        canvasEditor.visibleProperty().bind(canvasMode);
        canvasEditor.managedProperty().bind(canvasEditor.visibleProperty());

        existingSolutionNode = existingSolutionImageNode();
        mode.getChildren().clear();
        toolBarNode = toolBar();
        textStyleBar = textStyleToolbar();
        ((VBox) toolBarNode).getChildren().add(textStyleBar);
        VBox toolbarBody = new VBox(0, toolBarNode);
        Button foldTools = ActionButtonFactory.secondary("");
        foldTools.setText("▲");
        foldTools.getStyleClass().add(AppStyles.UI_RIBBON_COLLAPSE_TOGGLE);
        foldTools.setAccessibleText("Ocultar o mostrar herramientas");
        StudioFormControls.installTooltip(foldTools, "Plegar o desplegar herramientas para ampliar el lienzo.");
        foldTools.setOnAction(event -> {
            boolean show = !toolbarBody.isVisible();
            setNodeVisible(toolbarBody, show);
            foldTools.setText(show ? "▲" : "▼");
        });
        javafx.scene.layout.Region foldSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(foldSpacer, Priority.ALWAYS);
        HBox foldRow = new HBox(foldSpacer, foldTools);
        foldRow.setAlignment(Pos.CENTER_RIGHT);
        VBox toolbarShell = new VBox(0, toolbarBody, foldRow);
        toolbarShellNode = toolbarShell;
        toolbarShell.getStyleClass().add("technical-problem-toolbar-shell");
        VBox resolver = new VBox(4, toolbarShell, existingSolutionNode, resolverStack);
        resolver.getStyleClass().add("technical-problem-resolver");
        resolver.setPrefWidth(620);
        resolver.setMinWidth(420);
        return resolver;
    }

    private HBox textFieldGroup(String caption, Node... controls) {
        Label label = new Label(caption);
        label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox field = new HBox(6, label);
        field.getChildren().addAll(controls);
        field.setAlignment(Pos.CENTER_LEFT);
        return field;
    }

    private FlowPane textStyleToolbar() {
        com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFontControls.configure(textFont);
        textFont.setPrefWidth(170);
        textEffect.getItems().setAll("Sin efecto", "Sombra", "Borde sólido");
        textEffect.setValue("Sin efecto");
        textEffect.setPrefWidth(150);
        FlowPane bar = new FlowPane(10, 4,
                StudioCanvasToolbar.group("Formato de texto", textFieldGroup("Fuente", textFont)),
                textFieldGroup("Color", textColor), textFieldGroup("Estilo", textEffect),
                textFieldGroup("Color del efecto", textEffectColor));
        bar.setPadding(new Insets(4, 8, 4, 8));
        textFont.setOnAction(e -> applySelectedTextStyle());
        textEffect.setOnAction(e -> applySelectedTextStyle());
        textColor.setOnAction(e -> applySelectedTextStyle());
        textEffectColor.setOnAction(e -> applySelectedTextStyle());
        setNodeVisible(bar, false);
        return bar;
    }

    private void updateTextStyleVisibility() {
        setNodeVisible(textStyleBar, activeCanvasTool.get() == CanvasTool.TEXT
                || (selectedCanvasImage != null && selectedCanvasImage.text != null));
    }

    private void syncTextStyle() {
        if (selectedCanvasImage != null && selectedCanvasImage.text != null) {
            syncingTextStyle = true;
            try {
                var spec = selectedCanvasImage.text;
                textFont.setValue(spec.family()); textColor.setValue(Color.web(spec.color()));
                textEffect.setValue(spec.effect()); textEffectColor.setValue(Color.web(spec.effectColor()));
            } finally { syncingTextStyle = false; }
        }
        updateTextStyleVisibility();
    }

    private com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject textSpec(String value, double angle) {
        return new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject(value,
                textFont.getValue() == null ? "System" : textFont.getValue(), cssColor(textColor.getValue()),
                textEffect.getValue() == null ? "Sin efecto" : textEffect.getValue(), cssColor(textEffectColor.getValue()), angle);
    }

    private void applySelectedTextStyle() {
        if (syncingTextStyle || selectedCanvasImage == null || selectedCanvasImage.text == null) return;
        rememberUndo();
        updateTextObject(selectedCanvasImage, textSpec(selectedCanvasImage.text.text(), selectedCanvasImage.text.angle()));
    }

    private void updateTextObject(CanvasImageItem item,
            com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject spec) {
        double scale = item.view.getFitWidth() / item.view.getImage().getWidth();
        item.text = spec;
        Image rendered = spec.render();
        item.view.setImage(rendered);
        item.view.setFitWidth(rendered.getWidth() * scale);
        item.originalImage = rendered;
        item.cropActive = false;
        updateResizeHandles();
        canvasTouched.set(true);
    }

    private CanvasImageItem addTextObject(String value, double x, double y) {
        var spec = textSpec(value, 0);
        Image rendered = spec.render();
        CanvasImageItem item = addCanvasImage(rendered, x, y, rendered.getWidth()/2, false);
        item.text = spec;
        canvasTouched.set(true);
        return item;
    }

    private void handleCanvasTextClick(MouseEvent event) {
        if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY || canvasTextEditor != null) return;
        Point2D point = drawingSurface.sceneToLocal(event.getSceneX(), event.getSceneY());
        if (point.getX() < 0 || point.getY() < CANVAS_TITLE_BAND_HEIGHT
                || point.getX() > drawingSurface.logicalWidth() || point.getY() > drawingSurface.logicalHeight()) return;
        CanvasImageItem hit = canvasImageAt(point);
        if (hit != null && hit.text != null && (imageInteractionMode.get() || activeCanvasTool.get() == CanvasTool.TEXT)) {
            selectCanvasImage(hit);
            if (event.getClickCount() == 2) editCanvasText(hit, point);
            event.consume();
        } else if (activeCanvasTool.get() == CanvasTool.TEXT && hit == null) {
            editCanvasText(null, point);
            event.consume();
        } else if (activeCanvasTool.get() == CanvasTool.NONE && hit == null
                && event.isStillSincePress() && !imageCropMode) {
            // Resize/rotation handles and scroll controls are not empty canvas.
            Node target = event.getTarget() instanceof Node node ? node : null;
            for (Node current = target; current != null && current != drawingSurface; current = current.getParent()) {
                if (current instanceof javafx.scene.control.Control) return;
            }
            if (target != null && descendantOf(target, drawingSurface)) {
                selectCanvasImage(null);
            }
        }
    }

    private void editCanvasText(CanvasImageItem item, Point2D point) {
        TextArea editor = StudioFormControls.textArea(item == null ? "" : item.text.text());
        canvasTextEditor = editor;
        editor.setWrapText(true);
        editor.setPromptText("Escribe aquí · Ctrl+Enter para aceptar · Escape para cancelar");
        editor.setManaged(false);
        editor.resizeRelocate(item == null ? point.getX() : item.view.getLayoutX(),
                item == null ? point.getY() : item.view.getLayoutY(), 420, 140);
        drawingSurface.inkInputLayer().getChildren().add(editor);
        editor.toFront();
        java.util.function.Consumer<Boolean> finish = accept -> {
            if (canvasTextEditor != editor) return;
            canvasTextEditor = null;
            String value = editor.getText();
            drawingSurface.inkInputLayer().getChildren().remove(editor);
            if (accept && !value.isBlank() && (item == null || !value.equals(item.text.text()))) {
                rememberUndo();
                CanvasImageItem result = item;
                if (result == null) result = addTextObject(value, point.getX(), point.getY());
                else updateTextObject(result, new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject(
                        value, item.text.family(), item.text.color(), item.text.effect(), item.text.effectColor(), item.text.angle()));
                imageInteractionMode.set(true);
                selectCanvasImage(result);
            }
        };
        editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) { finish.accept(false); e.consume(); }
            else if (e.getCode() == KeyCode.ENTER && e.isControlDown()) { finish.accept(true); e.consume(); }
        });
        editor.focusedProperty().addListener((obs, before, focused) -> { if (before && !focused) finish.accept(true); });
        Platform.runLater(() -> { editor.requestFocus(); editor.selectAll(); });
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

    private VBox toolBar() {
        ToggleButton textTool = canvasToolButton("type", "Agregar texto", "Clic para insertar texto; doble clic para editarlo.", CanvasTool.TEXT);
        ToggleButton penTool = canvasToolButton("pencil", "Lápiz",
                "Dibujar a mano alzada con ratón o tableta.", CanvasTool.PEN);
        ToggleButton lineTool = canvasToolButton("minus", "Línea",
                "Trazar una línea recta desde el punto inicial hasta el final.", CanvasTool.STRAIGHT_LINE);
        ToggleButton angleTool = canvasToolButton("ruler", "Medir ángulo",
                "Medir un ángulo arrastrando tres puntos. La guía es temporal y no se exporta.", CanvasTool.ANGLE_MEASURE);
        ToggleButton panTool = canvasToolButton("hand", "Mover lienzo",
                "Desplazar y ampliar el lienzo. Al volver a dibujar se retira el espacio sobrante, conservando un margen de 300 px.", CanvasTool.PAN);
        lineTool.setDisable(!drawingProfile.supports(DrawingToolId.STRAIGHT_LINE));
        angleTool.setDisable(!drawingProfile.supports(DrawingToolId.ANGLE_MEASURE));
        Button undoButton = iconToolButton("undo-2", "Deshacer (Ctrl+Z).", this::undo, false);
        Button redoButton = iconToolButton("redo-2", "Rehacer (Ctrl+Y).", this::redo, false);
        Button clearStrokesButton = ActionButtonFactory.secondary(
                "Limpiar trazos",
                "Limpiar solo trazos; conserva imagenes.",
                this::clearInkStrokes);
        Button clearCanvasButton = namedToolButton("trash-2", "Vaciar lienzo", "Eliminar todos los trazos y objetos; se puede deshacer.", this::clearCanvas, true);
        CheckBox imageInteraction = StudioFormControls.checkBox("Editar objetos");
        imageInteraction.selectedProperty().bindBidirectional(imageInteractionMode);

        imageInteraction.getStyleClass().add("technical-problem-image-interaction");
        StudioFormControls.installTooltip(imageInteraction,
                "Incluye imágenes y formas al seleccionar una región de tinta. Fuera de Seleccionar región, permite editar objetos individualmente; desactiva para dibujar encima.");
        shrinkImageButton = namedToolButton("minimize-2", "Reducir", "Reducir el objeto seleccionado al 85%.", () -> resizeSelectedImage(0.85), false);
        enlargeImageButton = namedToolButton("maximize-2", "Ampliar", "Ampliar el objeto seleccionado al 115%.", () -> resizeSelectedImage(1.15), false);
        deleteImageButton = namedToolButton("trash-2", "Eliminar objeto", "Eliminar el objeto seleccionado.", this::deleteSelectedImage, true);
        cropImageButton = ActionButtonFactory.secondary("Recortar imagen", "Recortar la imagen seleccionada trazando un rectangulo sobre ella.", this::startImageCropMode);
        restoreImageCropButton = ActionButtonFactory.secondary("Restaurar recorte", "Volver a mostrar la imagen completa seleccionada.", this::restoreSelectedImageCrop);
        copyRegionButton = iconToolButton("copy", "Copiar la region seleccionada.", this::copyCanvasRegionSelection, false);
        pasteRegionButton = iconToolButton("clipboard-paste", "Pegar los trazos copiados como tinta editable.", this::pasteCanvasRegionSelection, false);
        moveRegionButton = namedToolButton("move", "Desplazar región", "Desplazar la selección 24 px a la derecha y abajo. También puedes arrastrarla.", this::moveCanvasRegionSelection, false);
        deleteRegionButton = namedToolButton("trash-2", "Eliminar región", "Eliminar el contenido seleccionado.", this::deleteCanvasRegionSelection, true);
        Label widthLabel = new Label("Grosor");
        StudioFormControls.colorPicker(penColor, "Color del lapiz.");
        StudioFormControls.colorPicker(backgroundColor, "Color de fondo del lienzo.");
        configurePenWidthSlider();
        configureCanvasZoomSlider();
        eraser.setGraphic(LucideIconView.of("eraser"));
        eraser.setContentDisplay(ContentDisplay.LEFT);
        eraser.setText("Borrador");
        StudioFormControls.installTooltip(eraser, "Borrar solo la tinta del lienzo sin afectar imagenes ni fondo.");
        eraser.setUserData(CanvasTool.ERASER);
        selectRegionButton.setGraphic(LucideIconView.of("scan"));
        selectRegionButton.setContentDisplay(ContentDisplay.LEFT);
        selectRegionButton.setText("Seleccionar región");
        selectRegionButton.setUserData(CanvasTool.REGION);
        measurementStatus.getStyleClass().add("technical-problem-measurement-status");
        measurementStatus.setAccessibleText("Resultado de la medición angular");
        ToggleGroup toolGroup = new ToggleGroup();
        for (ToggleButton tool : List.of(penTool, lineTool, angleTool, eraser, panTool, selectRegionButton, textTool)) {
            tool.setToggleGroup(toolGroup);
            tool.setMinHeight(36);
        }
        penTool.setSelected(true);
        toolGroup.selectedToggleProperty().addListener((obs, previous, selected) -> {
            if (selected == null) {
                activateCanvasTool(CanvasTool.NONE);
                return;
            }
            Object value = selected.getUserData();
            if (value instanceof CanvasTool tool) activateCanvasTool(tool);
        });
        for (Button button : List.of(cropImageButton, restoreImageCropButton)) {
            button.setMinWidth(96);
        }
        StudioCanvasToolbar tools = new StudioCanvasToolbar("Herramientas del lienzo de problema técnico");
        javafx.scene.control.ComboBox<TechnicalShape> shapes = new javafx.scene.control.ComboBox<>();
        shapes.getItems().setAll(TechnicalShape.values());
        shapes.setPromptText("Formas…");
        shapes.setAccessibleText("Insertar forma");
        shapes.setPrefWidth(175);
        shapes.setCellFactory(list -> shapeCell());
        shapes.setButtonCell(shapeCell());
        shapes.setOnAction(event -> {
            TechnicalShape selected = shapes.getValue();
            if (selected != null) {
                insertShape(selected);
                shapes.getSelectionModel().clearSelection();
            }
        });
        penWidth.setMinWidth(90); penWidth.setPrefWidth(90); penWidth.setMaxWidth(90);
        canvasZoom.setMinWidth(90); canvasZoom.setPrefWidth(90); canvasZoom.setMaxWidth(90);
        Button rotateLeft = namedToolButton("rotate-ccw", "Girar izquierda", "Girar objeto 15° a la izquierda.", () -> rotateSelectedImage(-15), false);
        Button rotateRight = namedToolButton("rotate-cw", "Girar derecha", "Girar objeto 15° a la derecha.", () -> rotateSelectedImage(15), false);
        for (Button rotate : List.of(rotateLeft, rotateRight)) {
            rotate.visibleProperty().bind(deleteImageButton.visibleProperty());
            rotate.managedProperty().bind(rotate.visibleProperty());
            rotate.disableProperty().bind(deleteImageButton.disableProperty());
        }

        measurementStatus.visibleProperty().bind(activeCanvasTool.isEqualTo(CanvasTool.ANGLE_MEASURE));
        measurementStatus.managedProperty().bind(measurementStatus.visibleProperty());
        fillShapeButton = ActionButtonFactory.secondary("Rellenar figura", "Selecciona una forma cerrada y aplica el color de relleno.", this::fillSelectedShape);
        fillShapeButton.setGraphic(LucideIconView.of("paint-bucket"));
        clearShapeFillButton = ActionButtonFactory.secondary("Sin relleno", "Dejar transparente el interior de la figura seleccionada.", () -> applyShapeFill("transparent"));
        shapeFillColor.setAccessibleText("Color de relleno de figura");
        shapeFillActions = StudioCanvasToolbar.group("Relleno", fillShapeButton, shapeFillColor, clearShapeFillButton);
        clearStrokesButton.setGraphic(LucideIconView.of("eraser"));
        restoreImageCropButton.setGraphic(LucideIconView.of("refresh-cw"));
        StudioFormControls.installTooltip(inputDiagnosticLabel, "Presión detectada de la tableta; no es una acción.");
        tools.addRow(StudioCanvasToolbar.group("Seleccionar", selectRegionButton, imageInteraction),
                StudioCanvasToolbar.group("Dibujar", penTool, lineTool, eraser), shapes, textTool, angleTool, panTool,
                StudioCanvasToolbar.group("Historial", undoButton, redoButton));
        var strokeProperties = StudioCanvasToolbar.group("Trazo", textFieldGroup("Color", penColor), textFieldGroup("Grosor", penWidth, penWidthPreview));
        strokeProperties.visibleProperty().bind(activeCanvasTool.isEqualTo(CanvasTool.PEN)
                .or(activeCanvasTool.isEqualTo(CanvasTool.STRAIGHT_LINE)).or(activeCanvasTool.isEqualTo(CanvasTool.ERASER)));
        strokeProperties.managedProperty().bind(strokeProperties.visibleProperty());
        tools.addRow(strokeProperties, StudioCanvasToolbar.group("Lienzo", textFieldGroup("Fondo", backgroundColor)),
                StudioCanvasToolbar.group("Vista", textFieldGroup("Zoom", canvasZoom, canvasZoomValue), fullscreenButton), inputDiagnosticLabel,
                StudioCanvasToolbar.group("Limpiar", clearStrokesButton, clearCanvasButton));
        var objectActions = tools.addRow("Objeto seleccionado", shrinkImageButton, enlargeImageButton, deleteImageButton,
                cropImageButton, restoreImageCropButton, rotateLeft, rotateRight,
                namedToolButton("arrow-up", "Subir", "Subir una capa los objetos seleccionados.", () -> reorderCanvasObjects(1), false),
                namedToolButton("arrow-down", "Bajar", "Bajar una capa los objetos seleccionados.", () -> reorderCanvasObjects(-1), false),
                namedToolButton("layers", "Al frente", "Traer los objetos seleccionados al frente.", () -> reorderCanvasObjects(2), false),
                namedToolButton("layers", "Al fondo", "Enviar los objetos seleccionados al fondo.", () -> reorderCanvasObjects(-2), false));
        objectActions.visibleProperty().bind(objectSelectionVisible);
        objectActions.managedProperty().bind(objectActions.visibleProperty());
        var regionActions = tools.addRow("Región", copyRegionButton, pasteRegionButton, moveRegionButton, deleteRegionButton);
        regionActions.visibleProperty().bind(regionSelectionVisible.or(pasteRegionButton.visibleProperty()));
        regionActions.managedProperty().bind(regionActions.visibleProperty());
        var fillRow = tools.addRow(shapeFillActions);
        fillRow.visibleProperty().bind(shapeFillActions.visibleProperty());
        fillRow.managedProperty().bind(fillRow.visibleProperty());
        var measureRow = tools.addRow(measurementStatus);
        measureRow.visibleProperty().bind(measurementStatus.visibleProperty());
        measureRow.managedProperty().bind(measureRow.visibleProperty());
        tools.getStyleClass().add("technical-problem-tools");
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

    private javafx.scene.control.ListCell<TechnicalShape> shapeCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TechnicalShape shape, boolean empty) {
                super.updateItem(shape, empty);
                setText(empty || shape == null ? "Formas…" : shape.label);
                setGraphic(null);
                if (!empty && shape != null) {
                    javafx.scene.shape.SVGPath graphic = shape.graphic(Color.web("#20232A"), 4);
                    graphic.setScaleX(0.20);
                    graphic.setScaleY(0.20);
                    setGraphic(new javafx.scene.Group(graphic));
                }
            }
        };
    }

    private void insertShape(TechnicalShape shape) {
        flushInk();
        rememberUndo();
        canvasMode.set(true);
        canvasRegionSelectionMode.set(false);
        drawMode.set(activeCanvasTool.get() != CanvasTool.NONE && activeCanvasTool.get() != CanvasTool.PAN);
        imageInteractionMode.set(true);
        double x = Math.max(24, Math.min(lastCanvasPointer.getX(), drawingSurface.logicalWidth() - 260));
        double y = Math.max(CANVAS_TITLE_BAND_HEIGHT + 24, lastCanvasPointer.getY());
        var spec = shape.object(penColor.getValue(), penStrokeWidth());
        CanvasImageItem item = addCanvasImage(spec.render(), x, y, 240, false);
        item.shape = spec;
        selectCanvasImage(item);
        updateImageInteractionMode();
        canvasTouched.set(true);
    }

    private void reorderCanvasObjects(int direction) {
        var picked = new java.util.HashSet<CanvasImageItem>();
        if (canvasRegionSelectionMode.get() && vectorSelection != null) {
            for (var object : vectorSelection.selectedObjects()) if (object instanceof CanvasImageItem item) picked.add(item);
        } else if (selectedCanvasImage != null) picked.add(selectedCanvasImage);
        if (picked.isEmpty()) return;
        var order = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasObjectOrder.reorder(canvasImages, picked, direction);
        if (order.equals(canvasImages)) return;
        rememberUndo();
        canvasImages.clear(); canvasImages.addAll(order);
        for (CanvasImageItem item : canvasImages) item.view.toFront();
        canvasTouched.set(true);
        if (vectorSelection != null) vectorSelection.refresh();
        updateResizeHandles();
    }

    private void fillSelectedShape() { applyShapeFill(shapeFillColor.getValue().toString()); }

    private void applyShapeFill(String color) {
        if (selectedCanvasImage == null || selectedCanvasImage.shape == null || !selectedCanvasImage.shape.closed()) return;
        CanvasImageItem item = selectedCanvasImage;
        if (item.shape.fill().equals(color)) return;
        rememberUndo();
        double scale = item.view.getFitWidth() / item.view.getImage().getWidth();
        item.shape = item.shape.filled(color);
        Image rendered = item.shape.render();
        item.view.setImage(rendered);
        item.view.setFitWidth(rendered.getWidth() * scale);
        updateResizeHandles();
        canvasTouched.set(true);
    }

    private Button namedToolButton(String icon, String text, String tooltip, Runnable action, boolean warning) {
        Button button = iconToolButton(icon, tooltip, action, warning);
        button.setText(text);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.getStyleClass().remove("technical-problem-icon-button");
        button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        button.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return button;
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

    private ToggleButton canvasToolButton(String iconName, String text, String tooltip, CanvasTool tool) {
        ToggleButton button = StudioFormControls.toggleButton(text);
        button.setGraphic(LucideIconView.of(iconName));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setUserData(tool);
        StudioFormControls.installTooltip(button, tooltip);
        return button;
    }

    private void updateToolCursor() {
        String icon = switch (activeCanvasTool.get()) {
            case PEN -> "pencil";
            case STRAIGHT_LINE -> "minus";
            case TEXT -> "type";
            case ERASER -> "eraser";
            case REGION -> "scan";
            case ANGLE_MEASURE -> "ruler";
            case PAN -> "hand";
            case NONE -> null;
        };
        drawingSurface.setCursor(icon == null ? Cursor.DEFAULT
                : com.marcosmoreiradev.docupodcaststudio.presentation.components.ToolIconCursor.of(icon));
    }

    private void activateCanvasTool(CanvasTool tool) {
        canvasTrimDebounce.stop();
        CanvasTool selected = tool == null ? CanvasTool.NONE : tool;
        activeCanvasTool.set(selected);
        updateToolCursor();
        straightLineStart = null;
        drawingSurface.clearLiveStroke();
        boolean region = selected == CanvasTool.REGION;
        boolean drawing = selected != CanvasTool.NONE && selected != CanvasTool.PAN && !region;
        canvasRegionSelectionMode.set(region);
        drawMode.set(drawing);
        imageInteractionMode.set(region || selected == CanvasTool.TEXT || selected == CanvasTool.NONE);
        updateTextStyleVisibility();
        if (selected != CanvasTool.ANGLE_MEASURE) {
            clearAngleMeasurement();
            measurementStatus.setText("Medición: selecciona Medir ángulo para comenzar.");
        } else {
            clearAngleMeasurement();
            Point2D center = new Point2D(drawingSurface.logicalWidth() / 2, CANVAS_TITLE_BAND_HEIGHT + 180);
            if (canvasScroll != null && canvasScroll.getViewportBounds().getWidth() > 0) {
                Bounds viewport = canvasScroll.getViewportBounds();
                center = drawingSurface.sceneToLocal(canvasScroll.localToScene(viewport.getWidth() / 2, viewport.getHeight() / 2));
            }
            angleMeasurementPoints.addAll(List.of(center.add(-90, 0), center, center.add(0, -90)));
            renderAngleMeasurement(null);

        }
        updateCanvasScrollMode();
        updateImageInteractionMode();
        if (selected != CanvasTool.PAN) canvasTrimDebounce.playFromStart();
    }

    private void configurePenWidthSlider() {
        penWidth.setBlockIncrement(1);
        penWidth.setMajorTickUnit(4);
        penWidth.setMinorTickCount(0);
        penWidth.setShowTickMarks(false);
        penWidth.setSnapToTicks(false);
        penWidth.setMinWidth(160);
        penWidth.setPrefWidth(160);
        penWidth.setMaxWidth(160);
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
        canvasZoom.setShowTickMarks(false);
        canvasZoom.setSnapToTicks(false);
        canvasZoom.setMinWidth(150);
        canvasZoom.setPrefWidth(150);
        canvasZoom.setMaxWidth(150);
        StudioFormControls.slider(canvasZoom, "Acercar o alejar el lienzo sin cambiar el tamano exportado.");
        canvasZoomValue.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> Math.round(canvasZoom.getValue()) + "%",
                canvasZoom.valueProperty()));
        canvasZoomValue.getStyleClass().add("technical-problem-canvas-zoom-value");
        StudioFormControls.installTooltip(canvasZoomValue, "Zoom visual del lienzo.");
        canvasZoom.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (canvasZoomPane != null) canvasZoomPane.setZoom(newValue.doubleValue() / 100.0);
        });
    }

    private Node canvasPane() {
        updateToolCursor();
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
        canvasZoomPane = new InkCanvasZoomPane(canvasFrame,
                () -> drawingSurface.logicalWidth() + 32.0,
                () -> drawingSurface.logicalHeight() + 32.0);
        canvasZoomPane.setOnZoomApplied(zoom -> {
            editorController.zoomTo(zoom);
            if (!ensureCanvasTilesCoverViewport()) editorController.resetInputCoordinates();
        });
        ScrollPane scroll = canvasZoomPane.scrollPane();
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
        scroll.addEventFilter(MouseEvent.ANY, this::handleSecondaryPan);
        scroll.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleCanvasTextClick);
        scroll.addEventFilter(javafx.scene.input.ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> event.consume());
        drawMode.addListener((obs, oldValue, newValue) -> updateCanvasScrollMode());
        updateCanvasScrollMode();
        Platform.runLater(this::ensureCanvasTilesCoverViewport);
        return canvasZoomPane;
    }

    private void handleSecondaryPan(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) canvasTrimDebounce.stop();
        if (event.getEventType() == MouseEvent.MOUSE_RELEASED && activeCanvasTool.get() != CanvasTool.PAN)
            canvasTrimDebounce.playFromStart();
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED
                && event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
            secondaryPanning = true;
            canvasTrimDebounce.stop();
            panSceneX = event.getSceneX();
            panSceneY = event.getSceneY();
            canvasScroll.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            drawingSurface.setCursor(Cursor.CLOSED_HAND);
            event.consume();
        } else if (secondaryPanning && event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            Bounds content = canvasScroll.getContent().getBoundsInLocal();
            Bounds viewport = canvasScroll.getViewportBounds();
            double width = content.getWidth() - viewport.getWidth();
            double height = content.getHeight() - viewport.getHeight();
            if (width > 0) canvasScroll.setHvalue(Math.max(canvasScroll.getHmin(), Math.min(canvasScroll.getHmax(),
                    canvasScroll.getHvalue() - (event.getSceneX() - panSceneX) / width
                            * (canvasScroll.getHmax() - canvasScroll.getHmin()))));
            if (height > 0) canvasScroll.setVvalue(Math.max(canvasScroll.getVmin(), Math.min(canvasScroll.getVmax(),
                    canvasScroll.getVvalue() - (event.getSceneY() - panSceneY) / height
                            * (canvasScroll.getVmax() - canvasScroll.getVmin()))));
            panSceneX = event.getSceneX();
            panSceneY = event.getSceneY();
            event.consume();
        } else if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY
                && (event.getEventType() == MouseEvent.MOUSE_RELEASED || event.getEventType() == MouseEvent.MOUSE_CLICKED)) {
            secondaryPanning = false;
            canvasScroll.setCursor(null);
            updateToolCursor();
            canvasTrimDebounce.playFromStart();
            event.consume();
        }
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
        vectorSelection = new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection(drawingSurface, this::rememberUndo);
        vectorSelection.setObjectAccess(() -> imageInteractionMode.get() ? List.copyOf(canvasImages) : List.of());
        vectorSelection.setOnChanged(() -> { canvasTouched.set(true); updateCanvasRegionButtons(); });
        startInkEngine();
        editorController.attach(inkViewport, new InkInputListener() {
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
        Platform.runLater(this::loadExistingSolutionIntoCanvas);
    }

    private void disposeCanvasInput() {
        canvasDisposed = true;
        canvasGrowDebounce.stop();
        canvasTrimDebounce.stop();
        if (inputDiagnosticLabel != null) inputDiagnosticLabel.close();
        editorController.detach();
        if (inkEngine != null) {
            inkEngine.stop();
            inkEngine = null;
        }
        editorController.close();
    }

    private void resetInkCoordinateState() {
        editorController.resetInputCoordinates();
    }

    private boolean handleInkStrokeStart(InkInputSample sample) {
        canvasTrimDebounce.stop();
        updateInputState(sample);
        Point2D point = pointInsideCanvas(sample);
        if (point == null) {
            return false;
        }
        lastCanvasPointer = point;
        if (activeCanvasTool.get() == CanvasTool.TEXT) return true;
        if (canvasRegionSelectionMode.get() && vectorSelection != null) return false;
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
        if (activeCanvasTool.get() == CanvasTool.ANGLE_MEASURE) {
            draggedAnglePoint = -1;
            for (int i = 0; i < angleMeasurementPoints.size(); i++) {
                if (angleMeasurementPoints.get(i).distance(point) <= 16) draggedAnglePoint = i;
            }
            return true;
        }
        if (pointerOnImageResizeHandle(point)) {
            return false;
        }
        selectCanvasImageUnderPointerForDrawing(point);
        rememberFastInkUndo();
        if (insideTitleBand(point)) {
            return true;
        }
        if (activeCanvasTool.get() == CanvasTool.STRAIGHT_LINE) {
            straightLineStart = point;
            drawingSurface.beginLiveStroke();
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
        if (canvasRegionSelectionMode.get() && vectorSelection != null) return false;
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
        if (activeCanvasTool.get() == CanvasTool.ANGLE_MEASURE) {
            if (draggedAnglePoint >= 0) {
                angleMeasurementPoints.set(draggedAnglePoint, point);
                renderAngleMeasurement(null);
            }
            return true;
        }
        if (activeCanvasTool.get() == CanvasTool.STRAIGHT_LINE && straightLineStart != null) {
            drawingSurface.clearLiveStroke();
            drawingSurface.previewLine(straightLineStart.getX(), straightLineStart.getY(),
                    point.getX(), point.getY(), penColor.getValue(), penStrokeWidth(), false);
            return true;
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
        if (activeCanvasTool.get() != CanvasTool.PAN) canvasTrimDebounce.playFromStart();
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
            if (activeCanvasTool.get() == CanvasTool.ANGLE_MEASURE) {
                draggedAnglePoint = -1;
                return true;
            }
            if (activeCanvasTool.get() == CanvasTool.STRAIGHT_LINE) {
                commitStraightLine(point);
                return true;
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
        if (vectorSelection != null && canvasRegionSelectionMode.get()) return false;
        finishCanvasRegionSelection(point);
        return true;
    }

    private void commitStraightLine(Point2D end) {
        Point2D start = straightLineStart;
        straightLineStart = null;
        drawingSurface.clearLiveStroke();
        if (start == null || end == null || insideTitleBand(start) || insideTitleBand(end)
                || start.distance(end) < 1.0) return;
        long now = System.nanoTime();
        drawingSurface.commitInkStroke(new StudyProblemCanvasSurface.InkStrokeState(
                "DRAW", cssColor(penColor.getValue()), penStrokeWidth(),
                List.of(new StudyProblemCanvasSurface.InkPointState(start.getX(), start.getY(), now, 1.0),
                        new StudyProblemCanvasSurface.InkPointState(end.getX(), end.getY(), now + 1, 1.0))));
        canvasTouched.set(true);
    }

    private void addAngleMeasurementPoint(Point2D point) {
        if (point == null || insideTitleBand(point)) return;
        if (angleMeasurementPoints.size() >= 3) return;
        angleMeasurementPoints.add(point);
        renderAngleMeasurement(null);
        measurementStatus.setText(switch (angleMeasurementPoints.size()) {
            case 1 -> "Medición: marca el vértice.";
            case 2 -> "Medición: marca el segundo lado.";
            default -> "Ángulo: " + Math.round(measuredAngleDegrees()) + "° · Haz clic para medir otro.";
        });
    }

    private void previewAngleMeasurement(Point2D pointer) {
        if (angleMeasurementPoints.size() == 2) renderAngleMeasurement(pointer);
    }

    private void renderAngleMeasurement(Point2D preview) {
        removeMeasurementOverlayNodes();
        for (int index = 0; index < angleMeasurementPoints.size(); index++) {
            final int pointIndex = index;
            Point2D point = angleMeasurementPoints.get(index);
            Circle marker = new Circle(point.getX(), point.getY(), 5, Color.web("#4F46E5"));
            marker.setRadius(7);
            marker.setCursor(Cursor.MOVE);
            marker.setOnMousePressed(event -> event.consume());
            marker.setOnMouseDragged(event -> {
                Point2D moved = drawingSurface.sceneToLocal(event.getSceneX(), event.getSceneY());
                angleMeasurementPoints.set(pointIndex, moved);
                marker.setCenterX(moved.getX());
                marker.setCenterY(moved.getY());
                refreshAngleLines();
                event.consume();
            });
            measurementOverlayNodes.add(marker);
        }
        if (angleMeasurementPoints.size() >= 2) {
            measurementOverlayNodes.add(measurementLine(angleMeasurementPoints.get(0), angleMeasurementPoints.get(1)));
        }
        Point2D third = angleMeasurementPoints.size() >= 3 ? angleMeasurementPoints.get(2) : preview;
        if (angleMeasurementPoints.size() >= 2 && third != null) {
            measurementOverlayNodes.add(measurementLine(angleMeasurementPoints.get(1), third));
        }
        Label angleLabel = new Label();
        angleLabel.setMouseTransparent(true);
        angleLabel.setStyle("-fx-background-color: white; -fx-text-fill: #302891; -fx-padding: 3;");
        measurementOverlayNodes.add(angleLabel);
        drawingSurface.inkInputLayer().getChildren().addAll(measurementOverlayNodes);
        refreshAngleLines();
    }

    private void refreshAngleLines() {
        if (angleMeasurementPoints.size() != 3) return;
        int side = 0;
        for (Node node : measurementOverlayNodes) {
            if (node instanceof Line line) {
                Point2D a = angleMeasurementPoints.get(side++ == 0 ? 0 : 2);
                Point2D b = angleMeasurementPoints.get(1);
                line.setStartX(a.getX()); line.setStartY(a.getY());
                line.setEndX(b.getX()); line.setEndY(b.getY());
            }
        }
        double degrees = measuredAngleDegrees();
        String angle = String.format(java.util.Locale.ROOT, "Ángulo: %.1f°; %.3f rad", degrees, Math.toRadians(degrees));
        measurementOverlayNodes.removeIf(node -> {
            if (node instanceof javafx.scene.shape.Arc) {
                drawingSurface.inkInputLayer().getChildren().remove(node);
                return true;
            }
            return false;
        });
        Point2D vertex = angleMeasurementPoints.get(1);
        Point2D a = angleMeasurementPoints.get(0).subtract(vertex);
        Point2D b = angleMeasurementPoints.get(2).subtract(vertex);
        double start = Math.toDegrees(Math.atan2(-a.getY(), a.getX()));
        double finish = Math.toDegrees(Math.atan2(-b.getY(), b.getX()));
        double sweep = ((finish - start + 540) % 360) - 180;
        double radius = Math.min(30, Math.min(a.magnitude(), b.magnitude()) * 0.3);
        javafx.scene.shape.Arc arc = new javafx.scene.shape.Arc(vertex.getX(), vertex.getY(), radius, radius, start, sweep);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#4F46E5"));
        arc.setStrokeWidth(1);
        arc.setMouseTransparent(true);
        measurementOverlayNodes.add(arc);
        drawingSurface.inkInputLayer().getChildren().add(arc);
        measurementStatus.setText(angle);
        for (Node node : measurementOverlayNodes) {
            if (node instanceof Label label) {
                label.setText(angle);
                label.relocate(angleMeasurementPoints.get(1).getX() + 12, angleMeasurementPoints.get(1).getY() + 12);
            }
        }
    }

    private Line measurementLine(Point2D start, Point2D end) {
        Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
        line.setStroke(Color.web("#4F46E5"));
        line.setStrokeWidth(1.0);
        line.getStrokeDashArray().setAll(8.0, 6.0);
        line.setMouseTransparent(true);
        return line;
    }

    private double measuredAngleDegrees() {
        if (angleMeasurementPoints.size() < 3) return 0.0;
        Point2D first = angleMeasurementPoints.get(0);
        Point2D vertex = angleMeasurementPoints.get(1);
        Point2D third = angleMeasurementPoints.get(2);
        Point2D a = first.subtract(vertex);
        Point2D b = third.subtract(vertex);
        double denominator = a.magnitude() * b.magnitude();
        if (denominator <= 0.0001) return 0.0;
        double cosine = Math.max(-1.0, Math.min(1.0, a.dotProduct(b) / denominator));
        return Math.toDegrees(Math.acos(cosine));
    }

    private void clearAngleMeasurement() {
        angleMeasurementPoints.clear();
        removeMeasurementOverlayNodes();
    }

    private void removeMeasurementOverlayNodes() {
        drawingSurface.inkInputLayer().getChildren().removeAll(measurementOverlayNodes);
        measurementOverlayNodes.clear();
    }

    private void updateInputState(InkInputSample sample) {
        if (sample == null) {
            currentInputPressure = 1.0;
            currentInputEraser = false;
            return;
        }
        currentInputPressure = Math.max(0.0, Math.min(1.0, sample.pressure()));
        currentInputEraser = sample.requestsEraser() || sample.cursor() == InkInputCursor.ERASER;
    }

    private Point2D pointInsideCanvas(InkInputSample sample) {
        if (sample == null) {
            return null;
        }
        return sample.x() >= 0 && sample.y() >= 0
                && sample.x() <= drawingSurface.logicalWidth()
                && sample.y() <= drawingSurface.logicalHeight()
                ? new Point2D(sample.x(), sample.y()) : null;
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
        InkInputCapabilities capabilities = editorController.inputCapabilities();
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
        drawingSurface.clearStrokes();
        canvasTouched.set(true);
    }

    private void clearCanvas() {
        flushInk();
        rememberUndo();
        drawingSurface.clearStrokes();
        for (CanvasImageItem item : List.copyOf(canvasImages)) {
            drawingSurface.imageLayer().getChildren().remove(item.view());
        }
        canvasImages.clear();
        canvasRegionClipboard = null;
        imageCropMode = false;
        clearImageCropSelectionRectangle();
        hideCanvasRegionSelection();
        clearAngleMeasurement();
        measurementStatus.setText("Medición: selecciona Medir ángulo para comenzar.");
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
        canvasScroll.setPannable(activeCanvasTool.get() == CanvasTool.PAN);
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
                && (drawMode.get() || activeCanvasTool.get() == CanvasTool.NONE)
                && imageInteractionMode.get()
                && !canvasRegionSelectionMode.get();
    }

    private boolean canCaptureInkInput() {
        return canvasMode.get()
                && !imageCropMode
                && (canvasRegionSelectionMode.get() || (!imageInteractionMode.get() && drawMode.get()));
    }

    private void updateInkInputLayerMode() {
        if (vectorSelection != null) vectorSelection.refresh();
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
        if (canvasScroll == null || canvasDisposed || (!secondaryPanning && activeCanvasTool.get() != CanvasTool.PAN)
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
        if (canvasDisposed || canvasScroll == null || canvasGrowScheduled) {
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
                if (canvasZoomPane != null) canvasZoomPane.refreshContentExtent();
                suppressCanvasGrowEvents = true;
                if (growWidth) {
                    canvasScroll.setHvalue(0.78);
                }
                if (growHeight) {
                    canvasScroll.setVvalue(0.78);
                }
                if (!ensureCanvasTilesCoverViewport()) {
                    Platform.runLater(editorController::resetInputCoordinates);
                }
                PauseTransition release = new PauseTransition(Duration.millis(220));
                release.setOnFinished(event -> Platform.runLater(() -> suppressCanvasGrowEvents = false));
                release.play();
            }
        } finally {
            canvasGrowScheduled = false;
        }
    }

    private boolean ensureCanvasTilesCoverViewport() {
        if (canvasDisposed || canvasScroll == null || canvasScroll.getViewportBounds().getWidth() <= 0
                || canvasScroll.getViewportBounds().getHeight() <= 0) {
            return false;
        }
        boolean grew = editorController.ensureViewportCoverage(
                Math.max(1.0, canvasScroll.getViewportBounds().getWidth() - 36),
                Math.max(1.0, canvasScroll.getViewportBounds().getHeight() - 36));
        if (grew) {
            if (canvasZoomPane != null) canvasZoomPane.refreshContentExtent();
            editorController.resetInputCoordinates();
        }
        return grew;
    }

    private void trimUnusedCanvas() {
        if (canvasDisposed || secondaryPanning || activeCanvasTool.get() == CanvasTool.PAN) return;
        flushInk();
        double zoom = canvasZoomPane.appliedZoom();
        double width = Math.max(StudyProblemCanvasSurface.DEFAULT_WIDTH, canvasScroll.getViewportBounds().getWidth()/zoom);
        double height = Math.max(StudyProblemCanvasSurface.DEFAULT_HEIGHT, canvasScroll.getViewportBounds().getHeight()/zoom);
        double offsetX = canvasScroll.getHvalue() * Math.max(0, drawingSurface.logicalWidth()*zoom-canvasScroll.getViewportBounds().getWidth());
        double offsetY = canvasScroll.getVvalue() * Math.max(0, drawingSurface.logicalHeight()*zoom-canvasScroll.getViewportBounds().getHeight());
        suppressCanvasGrowEvents = true;
        try {
            if (drawingSurface.trimUnusedSpace(width, height, 300)) {
                canvasZoomPane.refreshContentExtent();
                canvasScroll.setHvalue(Math.min(1, offsetX/Math.max(1, drawingSurface.logicalWidth()*zoom-canvasScroll.getViewportBounds().getWidth())));
                canvasScroll.setVvalue(Math.min(1, offsetY/Math.max(1, drawingSurface.logicalHeight()*zoom-canvasScroll.getViewportBounds().getHeight())));
                editorController.resetInputCoordinates();
                if (vectorSelection != null) vectorSelection.refresh();
            }
        } finally { suppressCanvasGrowEvents = false; }
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
        if (vectorSelection == null) return;
        vectorSelection.copy(); vectorClipboard = true; updateCanvasRegionButtons();
    }

    private void pasteCanvasRegionSelection() {
        if (vectorSelection != null) vectorSelection.paste();
    }

    private void deleteCanvasRegionSelection() {
        if (vectorSelection != null) vectorSelection.deleteSelection();
    }

    private void moveCanvasRegionSelection() {
        if (vectorSelection != null) vectorSelection.translate(24, 24);
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
        boolean hasSelection = vectorSelection != null ? vectorSelection.hasSelection() : activeCanvasRegionSelection != null;
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
            setNodeVisible(pasteRegionButton, vectorClipboard || canvasRegionClipboard != null);
            pasteRegionButton.setDisable(!vectorClipboard && canvasRegionClipboard == null);
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
        editorController.checkpoint();
    }

    private void rememberFastInkUndo() {
        if (drawingSurface.vectorInkReliable()) {
            editorController.checkpoint();
        } else {
            rememberUndo();
        }
    }

    private void undo() {
        flushInk();
        if (editorController.undo()) canvasTouched.set(true);
    }

    private void redo() {
        flushInk();
        if (editorController.redo()) canvasTouched.set(true);
    }

    private CanvasUndoSnapshot snapshotUndoState(boolean flush) {
        if (flush) {
            flushInk();
        }
        if (drawingSurface.vectorInkReliable()) {
            return new CanvasUndoSnapshot(null, drawingSurface.inkStrokeStates(), drawingSurface.inkCommandStates(), snapshotImages(), sourceOpacities.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
        }
        return new CanvasUndoSnapshot(drawingSurface.snapshotDrawing(), null, null, snapshotImages(), sourceOpacities.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));
    }

    private void restoreUndoSnapshot(CanvasUndoSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (!snapshot.vector()) {
            drawSnapshot(snapshot.raster());
        } else {
            drawingSurface.restoreInkUndoState(snapshot.strokes(), snapshot.commands());
        }
        clearCanvasImages();
        sourceOpacities.forEach((id, alpha) -> alpha.set(snapshot.sourceOpacity().getOrDefault(id, 1.0)));
        for (ImageUndoState image : snapshot.images()) {
            CanvasImageItem item = addCanvasImage(image.image(), image.x(), image.y(), image.width(), false);
            item.text = image.text();
            item.shape = image.shape();
            item.view.setOpacity(image.opacity());
            item.sourceId = image.sourceId();
            item.restoreCropMetadata(image.original(), image.originalX(), image.originalY(), image.originalWidth(), image.cropped());
        }
        if (vectorSelection != null) vectorSelection.refresh();
    }

    private List<ImageUndoState> snapshotImages() {
        return canvasImages.stream().map(item -> new ImageUndoState(
                item.view().getImage(), item.view().getLayoutX(), item.view().getLayoutY(),
                item.view().getFitWidth(), item.originalImage(), item.originalLayoutX(),
                item.originalLayoutY(), item.originalFitWidth(), item.isCropped(), item.text, item.shape, item.view.getOpacity(), item.sourceId)).toList();
    }

    private WritableImage snapshotCanvas() {
        flushInk();
        return drawingSurface.snapshotWithImages(canvasImageViews());
    }

    private InkCanvasExportResult exportCanvas(InkCanvasExportOptions options) {
        flushInk();
        drawingSurface.trimUnusedSpace(StudyProblemCanvasSurface.DEFAULT_WIDTH, StudyProblemCanvasSurface.DEFAULT_HEIGHT, 300);
        markTitleContentBounds();
        if (options.fullLogicalCanvas()) {
            options = new InkCanvasExportOptions(options.preferredScale(), options.maxPixelCount(), false, false,
                    300, options.minWidth(), options.minHeight());
        }
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
        javafx.scene.text.Text titleMeasure = new javafx.scene.text.Text(title.getText());
        titleMeasure.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 18));
        drawingSurface.markContentBounds(0, 0, Math.max(420.0, titleMeasure.getLayoutBounds().getWidth() + 48),
                CANVAS_TITLE_BAND_HEIGHT);
    }

    private boolean shouldPersistCanvasState(boolean needsCanvas) {
        return needsCanvas
                || canvasTouched.get()
                || !notes.getText().isBlank()
                || !canvasImages.isEmpty()
                || !drawingSurface.inkStrokeStates().isEmpty()
                || !drawingSurface.inkCommandStates().isEmpty()
                || (existingCanvasStatePath != null && Files.isRegularFile(existingCanvasStatePath));
    }

    private String canvasStateJson() { return canvasPageStateJson(true); }

    private String canvasPageStateJson(boolean includeNotebook) {
        flushInk();
        Map<String, String> metadata = new HashMap<>();
        if (includeNotebook && notebookPages.size() > 1) {
            metadata.put("notebook.count", Integer.toString(notebookPages.size()));
            metadata.put("notebook.current", Integer.toString(currentPage));
            for (int i = 0; i < notebookPages.size(); i++) {
                if (i != currentPage) metadata.put("notebook.page." + i, notebookPages.get(i));
                if (notebookThumbnails.get(i) != null) metadata.put("notebook.thumb." + i, imageToBase64(notebookThumbnails.get(i)));
            }
        }
        for (int i = 0; i < canvasImages.size(); i++) {
            if (canvasImages.get(i).text != null) metadata.put("textObject." + (i+1), canvasImages.get(i).text.encode());
        }
        for (int i = 0; i < canvasImages.size(); i++) {
            if (canvasImages.get(i).shape != null) metadata.put("shapeObject." + (i+1), canvasImages.get(i).shape.encode());
        }
        sourceOpacities.forEach((id, alpha) -> metadata.put("sourceOpacity." + id, Double.toString(alpha.get())));
        for (int i = 0; i < canvasImages.size(); i++) {
            metadata.put("imageOpacity." + (i+1), Double.toString(canvasImages.get(i).view.getOpacity()));
            metadata.put("imageSource." + (i+1), canvasImages.get(i).sourceId);
        }
        metadata.put("paperPattern", drawingSurface.paperPattern());
        metadata.put("consumer", "document-study.technical-problem");
        metadata.put("title", title.getText() == null ? "" : title.getText().strip());
        metadata.put("notes", notes.getText() == null ? "" : notes.getText());
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
            if (!restoreCanvasJson(json)) return false;
            var metadata = InkWorkspaceStateSerializer.fromJson(json).metadata();
            if (metadata.containsKey("title")) title.setText(metadata.get("title"));
            if (metadata.containsKey("notes")) notes.setText(metadata.get("notes"));
            int count = Math.max(1, Integer.parseInt(metadata.getOrDefault("notebook.count", "1")));
            currentPage = Math.max(0, Math.min(count - 1, Integer.parseInt(metadata.getOrDefault("notebook.current", "0"))));
            notebookPages.clear(); notebookThumbnails.clear();
            for (int i = 0; i < count; i++) {
                notebookPages.add(i == currentPage ? canvasPageStateJson(false) : metadata.getOrDefault("notebook.page." + i, ""));
                notebookThumbnails.add(imageFromBase64(metadata.getOrDefault("notebook.thumb." + i, "")));
            }
            rebuildPageList();
            return true;
        } catch (IOException | RuntimeException ex) { return false; }
    }

    private boolean restoreCanvasJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return false;
            }
            InkWorkspaceState state = InkWorkspaceStateSerializer.fromJson(json);
            Color restoredBackground = parseColorValue(state.background());
            drawingSurface.resetForEditableState(state.logicalWidth(), state.logicalHeight(), restoredBackground);
            backgroundColor.setValue(restoredBackground);
            clearCanvasImages();
            state.metadata().forEach((key, value) -> {
                if (key.startsWith("sourceOpacity.")) sourceOpacity(key.substring("sourceOpacity.".length())).set(readOpacity(value));
            });
            for (InkPlacedImage placedImage : state.images()) {
                Image image = imageFromBase64(placedImage.inlineImageData());
                if (image == null || image.isError()) {
                    continue;
                }
                double x = placedImage.x();
                double y = placedImage.y();
                double fitWidth = placedImage.fitWidth();
                CanvasImageItem item = addCanvasImage(image, x, y, fitWidth, false);
                item.view.setOpacity(readOpacity(state.metadata().get("imageOpacity." + canvasImages.size())));
                item.sourceId = state.metadata().getOrDefault("imageSource." + canvasImages.size(), "");
                item.text = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject.decode(
                        state.metadata().get("textObject." + canvasImages.size()));
                item.shape = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject.decode(
                        state.metadata().get("shapeObject." + canvasImages.size()));
                Image original = imageFromBase64(placedImage.inlineOriginalImageData());
                item.restoreCropMetadata(
                        original == null || original.isError() ? image : original,
                        placedImage.originalLayoutX(),
                        placedImage.originalLayoutY(),
                        placedImage.originalFitWidth(),
                        placedImage.crop().active());
            }
            drawingSurface.setPaperPattern(state.metadata().getOrDefault("paperPattern", "blank"));
            drawingSurface.restoreApplicationInkStrokes(state.strokes());
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

    private static String imageToBase64(Image image) {
        try {
            return InkImageFileStore.encodePngBase64(image);
        } catch (IOException ex) {
            return "";
        }
    }

    private static Image imageFromBase64(String data) {
        return InkImageFileStore.decodePngBase64(data);
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
            statementCollapsedBeforeFullscreen = statementCollapsed;
            if (!statementCollapsed) {
                toggleStatementCollapsed();
            }
            canvasMode.set(true);
            resolverFullscreen = true;
            // Route Escape through the editor so it restores the chrome without closing the dialog.
            stage.setFullScreenExitKeyCombination(new javafx.scene.input.KeyCodeCombination(KeyCode.ESCAPE));
            stage.setFullScreenExitHint("Presione Escape o F11 para salir de pantalla completa");
            if (!Boolean.TRUE.equals(stage.getProperties().get("technicalCanvasFullscreenListener"))) {
                stage.getProperties().put("technicalCanvasFullscreenListener", true);
                stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleDialogKeyPressed);
                stage.fullScreenProperty().addListener((obs, before, full) -> {
                    if (!full && resolverFullscreen) toggleResolverFullscreen();
                });
            }
            applyResolverFullscreenState();
            stage.setFullScreen(true);
            Platform.runLater(() -> canvasScroll.requestFocus());
        } else {
            resolverFullscreen = false;
            stage.setFullScreen(false);
            if (!statementCollapsedBeforeFullscreen && statementCollapsed) {
                toggleStatementCollapsed();
            }
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
        setNodeVisible(existingSolutionNode, !resolverFullscreen && existingSolutionImagePath != null);
        setNodeVisible(toolbarShellNode, !resolverFullscreen);
        if (rootPane != null) {
            setNodeVisible(rootPane.getBottom(), !resolverFullscreen);
        }
        dialog.setHeaderText(resolverFullscreen ? null : dialogHeaderText);
        setNodeVisible(dialog.getDialogPane().lookup(".button-bar"), !resolverFullscreen);
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
            collapseStatementButton.setText("◂");
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
        for (StatementSource source : statementSources) transferSourceImage(source, sourceImage(source));
    }

    private void transferSourceImage(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }
        transferSourceImage(new Image(path.toUri().toString(), true));
    }

    private void transferSourceImage(StatementSource source, Image image) {
        if (image == null || image.isError()) return;
        transferSourceImage(image);
        CanvasImageItem item = canvasImages.get(canvasImages.size() - 1);
        item.sourceId = source.id();
        item.view.setOpacity(sourceOpacity(source.id()).get());
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
            rememberUndo();
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
        syncTextStyle();
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

    private void rotateSelectedImage(double degrees) {
        if (selectedCanvasImage == null) return;
        rememberUndo();
        ImageView view = selectedCanvasImage.view();
        double oldWidth = view.getFitWidth();
        double centerX = view.getLayoutX() + view.getBoundsInLocal().getWidth() / 2;
        double centerY = view.getLayoutY() + view.getBoundsInLocal().getHeight() / 2;
        Image source = view.getImage();
        if (selectedCanvasImage.text != null) selectedCanvasImage.text = selectedCanvasImage.text.rotated(degrees);
        if (selectedCanvasImage.shape != null) selectedCanvasImage.shape = selectedCanvasImage.shape.rotated(degrees);
        Image rotated = selectedCanvasImage.shape != null ? selectedCanvasImage.shape.render() : selectedCanvasImage.text == null
                ? com.marcosmoreiradev.docupodcaststudio.presentation.components.CanvasImageTransforms.rotate(source, degrees)
                : selectedCanvasImage.text.render();
        double scale = oldWidth / source.getWidth();
        view.setImage(rotated);
        view.setFitWidth(rotated.getWidth() * scale);
        view.setLayoutX(centerX - rotated.getWidth() * scale / 2);
        view.setLayoutY(centerY - rotated.getHeight() * scale / 2);
        updateResizeHandles();
        canvasTouched.set(true);
    }

    private void resizeSelectedImage(double factor) {
        if (selectedCanvasImage == null) {
            return;
        }
        rememberUndo();
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
        rememberUndo();
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
        boolean mixedObjects = canvasRegionSelectionMode.get() && vectorSelection != null && !vectorSelection.selectedObjects().isEmpty();
        objectSelectionVisible.set(enabled || mixedObjects);
        regionSelectionVisible.set(canvasRegionSelectionMode.get() && vectorSelection != null && vectorSelection.hasSelection());
        boolean fillable = enabled && selectedCanvasImage.shape != null && selectedCanvasImage.shape.closed();
        if (shapeFillActions != null) setNodeVisible(shapeFillActions, fillable);
        if (fillShapeButton != null) fillShapeButton.setDisable(!fillable);
        if (clearShapeFillButton != null) clearShapeFillButton.setDisable(!fillable);
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
            cropImageButton.setDisable(!enabled || selectedCanvasImage.text != null || selectedCanvasImage.shape != null);
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
            StudioFormControls.installTooltip(handle, "Arrastra para redimensionar la imagen o forma manteniendo su proporción.");
            handle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (selectedCanvasImage == null || !canInteractWithCanvasImages()) {
                    return;
                }
                rememberUndo();
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
        imageRotateHandle = iconToolButton("rotate-cw", "Arrastra alrededor del objeto para girarlo libremente.", () -> { }, false);
        imageRotateHandle.setCursor(Cursor.HAND);
        new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasObjectRotationGesture(
                imageRotateHandle, drawingSurface,
                () -> canInteractWithCanvasImages() ? selectedCanvasImage : null,
                degrees -> { if (selectedCanvasImage != null) selectedCanvasImage.view.setRotate(degrees); },
                this::rememberUndo, () -> {
                    canvasTouched.set(true);
                    updateResizeHandles();
                });
        imageRotateHandle.setManaged(false);
        imageRotateHandle.setVisible(false);
        drawingSurface.inkInputLayer().getChildren().add(imageRotateHandle);
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
        if (imageRotateHandle != null) imageRotateHandle.setVisible(visible);
        for (Label handle : imageResizeHandles) {
            handle.setVisible(visible);
            handle.setMouseTransparent(!visible);
        }
        if (!visible) {
            return;
        }
        Bounds bounds = selectedCanvasImage.view().getBoundsInParent();
        if (imageRotateHandle != null) {
            imageRotateHandle.resizeRelocate((bounds.getMinX() + bounds.getMaxX()) / 2 - 16,
                    Math.max(0, bounds.getMinY() - 40), 32, 32);
            imageRotateHandle.toFront();
        }
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

    private record ImageUndoState(Image image, double x, double y, double width,
                                  Image original, double originalX, double originalY,
                                  double originalWidth, boolean cropped, com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject text, com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject shape, double opacity, String sourceId) { }

    private record CanvasUndoSnapshot(WritableImage raster,
                                      List<StudyProblemCanvasSurface.InkStrokeState> strokes,
                                      List<StudyProblemCanvasSurface.InkCommandState> commands,
                                      List<ImageUndoState> images, Map<String, Double> sourceOpacity) {
        private CanvasUndoSnapshot {
            strokes = strokes == null ? List.of() : List.copyOf(strokes);
            commands = commands == null ? List.of() : List.copyOf(commands);
            images = List.copyOf(images);
        }

        private boolean vector() {
            return raster == null;
        }
    }

    private final class CanvasImageItem implements com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.EditableObject {
        @Override public javafx.geometry.Rectangle2D bounds() {
            Bounds b = view.getBoundsInParent();
            return new javafx.geometry.Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
        }

        @Override public java.util.function.Consumer<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.ObjectTransform> captureTransform() {
            Image source = view.getImage();
            var sourceText = text;
            var sourceShape = shape;
            double width = view.getFitWidth();
            javafx.geometry.Rectangle2D b = bounds();
            double x = b.getMinX() + b.getWidth()/2, y = b.getMinY() + b.getHeight()/2;
            return t -> {
                double radians = Math.toRadians(t.degrees());
                double dx = (x-t.cx())*t.scale(), dy = (y-t.cy())*t.scale();
                double cx = t.cx()+t.dx()+dx*Math.cos(radians)-dy*Math.sin(radians);
                double cy = t.cy()+t.dy()+dx*Math.sin(radians)+dy*Math.cos(radians);
                if (sourceText != null) text = sourceText.rotated(t.degrees());
                if (sourceShape != null) shape = sourceShape.rotated(t.degrees());
                Image rendered = Math.abs(t.degrees()) < 0.0001 ? source : sourceShape != null ? shape.render() : sourceText != null ? text.render()
                        : com.marcosmoreiradev.docupodcaststudio.presentation.components.CanvasImageTransforms.rotate(source, t.degrees());
                double factor = width/source.getWidth()*t.scale();
                view.setImage(rendered);
                view.setFitWidth(rendered.getWidth()*factor);
                view.setLayoutX(cx-rendered.getWidth()*factor/2);
                view.setLayoutY(cy-rendered.getHeight()*factor/2);
            };
        }

        @Override public java.util.function.Supplier<com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.EditableObject> copyFactory() {
            ImageUndoState copy = new ImageUndoState(view.getImage(), view.getLayoutX(), view.getLayoutY(), view.getFitWidth(),
                    originalImage, originalLayoutX, originalLayoutY, originalFitWidth, cropActive, text, shape, view.getOpacity(), sourceId);
            return () -> {
                CanvasImageItem item = addCanvasImage(copy.image(), copy.x()+24, copy.y()+24, copy.width(), false);
                item.text = copy.text();
                item.shape = copy.shape();
                item.view.setOpacity(copy.opacity());
                item.sourceId = copy.sourceId();
                item.restoreCropMetadata(copy.original(), copy.originalX()+24, copy.originalY()+24, copy.originalWidth(), copy.cropped());
                return item;
            };
        }

        @Override public void remove() {
            drawingSurface.imageLayer().getChildren().remove(view);
            canvasImages.remove(this);
            if (selectedCanvasImage == this) selectCanvasImage(null);
        }

        private com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject text;
        private com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject shape;
        private String sourceId = "";
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
