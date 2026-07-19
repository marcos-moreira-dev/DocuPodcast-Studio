package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionCaptureRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionCaptureResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextHighlight;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfViewportSelection;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSpan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.EmptyStateView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.FloatingReadingControlBar;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RailReadingControlBar;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SourceVisualBlockView;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockContext;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleId;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleRegistry;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.StaticSideDockModule;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.WorkspaceSideDock;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionNumberingScene;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionBoundaryStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreSideDock;
import com.marcosmoreiradev.docupodcaststudio.presentation.narrative.NarrativeVideoSideDock;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionCatalogo;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Workspace that previews a normalized imported document with reusable SideDock modules. */
public final class DocumentWorkspaceView extends BorderPane {
    private static final String READING_SIZE_CLASS_PREFIX = "document-reader-size-";

    private final DocuPodcastShellViewModel viewModel;
    private static final double DOCUMENT_PAGE_MAX_WIDTH = 1280.0;
    private static final double ACTIVE_READING_TOP_OFFSET = 78.0;
    // Legacy guard label kept for source tests: large docs no longer render the whole document.
    private static final int LARGE_DOCUMENT_INITIAL_RENDER_LIMIT = 650;
    private static final int LARGE_DOCUMENT_WINDOW_SIZE = 260;

    private final VBox content = new VBox(10);
    private final StackPane pageHost = new StackPane(content);
    private final ScrollPane documentScroll = new ScrollPane(pageHost);
    private final PdfVisualDocumentView pdfVisualView;
    private final PdfVisibleTextPreparationCoordinator pdfVisibleTextPreparation;
    private final StringProperty selectedBlockId = new SimpleStringProperty("");
    private final Map<String, Node> blockNodes = new LinkedHashMap<>();
    private final Map<String, Node> sentenceNodes = new LinkedHashMap<>();
    private final Map<String, DocumentSentenceSpan> sentenceSpanIndex = new LinkedHashMap<>();
    private final Map<String, Integer> blockIndexById = new LinkedHashMap<>();
    private final Map<String, String> intervencionByBlockId = new LinkedHashMap<>();
    private final ObservableSet<String> technicalProblemSelection = FXCollections.observableSet(new LinkedHashSet<>());
    private final ObservableList<PdfRegionCaptureDraft> pdfRegionCaptureSelection = FXCollections.observableArrayList();
    private final IntervencionBoundaryStore intervencionBoundaryStore;
    private Set<String> focusedTheatreSceneBlockIds = Set.of();
    private final ActiveReadingAnchor readingAnchor = ActiveReadingAnchor.defaultAnchor();
    private String activePlaybackBlockId = "";
    private String activePlaybackUnitId = "";
    private ReadableDocument renderedDocument;
    private DocumentRenderWindow renderedWindow = DocumentRenderWindow.all(0);
    private boolean autoWindowSwitchInProgress;
    private int nextPdfRegionCaptureIndex = 1;
    private Path pdfRegionSelectionSourcePath;
    private final BooleanSupplier saveProjectRequest;
    private final BooleanSupplier saveProjectBeforeAudioRequest;
    private final BooleanSupplier audioEngineBeforeGenerationRequest;
    private final DocumentWorkspaceMode mode;
    private FloatingReadingControlBar floatingReadingControls;
    private RailReadingControlBar railReadingControls;

    public DocumentWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this(viewModel, () -> false);
    }

    public DocumentWorkspaceView(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest) {
        this(viewModel, saveProjectRequest, saveProjectRequest, () -> true);
    }

    public DocumentWorkspaceView(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest,
                                 BooleanSupplier saveProjectBeforeAudioRequest) {
        this(viewModel, saveProjectRequest, saveProjectBeforeAudioRequest, () -> true);
    }

    public DocumentWorkspaceView(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest,
                                 BooleanSupplier saveProjectBeforeAudioRequest,
                                 BooleanSupplier audioEngineBeforeGenerationRequest) {
        this(viewModel, saveProjectRequest, saveProjectBeforeAudioRequest,
                audioEngineBeforeGenerationRequest, DocumentWorkspaceMode.READING);
    }

    public DocumentWorkspaceView(DocuPodcastShellViewModel viewModel, BooleanSupplier saveProjectRequest,
                                 BooleanSupplier saveProjectBeforeAudioRequest,
                                 BooleanSupplier audioEngineBeforeGenerationRequest,
                                 DocumentWorkspaceMode mode) {
        this.viewModel = viewModel;
        this.intervencionBoundaryStore = viewModel == null ? new IntervencionBoundaryStore() : viewModel.intervencionBoundaryStore();
        this.saveProjectRequest = saveProjectRequest == null ? () -> false : saveProjectRequest;
        this.saveProjectBeforeAudioRequest = saveProjectBeforeAudioRequest == null ? this.saveProjectRequest : saveProjectBeforeAudioRequest;
        this.audioEngineBeforeGenerationRequest = audioEngineBeforeGenerationRequest == null ? () -> true : audioEngineBeforeGenerationRequest;
        this.mode = mode == null ? DocumentWorkspaceMode.READING : mode;
        this.pdfVisibleTextPreparation = new PdfVisibleTextPreparationCoordinator(viewModel, this::pdfOcrCacheDirectory);
        getStyleClass().add("document-workspace");
        getStyleClass().add(this.mode == DocumentWorkspaceMode.THEATRE_SCRIPT
                ? "document-workspace-theatre-script"
                : "document-workspace-reading");
        addEventFilter(MouseEvent.MOUSE_PRESSED, this::clearSelectionFromWorkspacePointerPress);
        applyReadingFontSizeClass(viewModel.readingFontSizeProperty().get());
        setPadding(new Insets(0));

        documentScroll.setFitToWidth(true);
        documentScroll.getStyleClass().add("document-scroll");
        pageHost.getStyleClass().add("document-page-host");
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        content.getStyleClass().add("document-page");
        content.setMaxWidth(DOCUMENT_PAGE_MAX_WIDTH);
        content.setMinWidth(0);
        content.setMinHeight(1120);
        pageHost.setMinWidth(0);
        documentScroll.setMinWidth(0);
        pageHost.setOnMouseClicked(event -> {
            if (event.getTarget() == pageHost) {
                clearSelectionFromBlankDocumentClick();
                event.consume();
            }
        });
        content.setOnMouseClicked(event -> {
            if (event.getTarget() == content) {
                clearSelectionFromBlankDocumentClick();
                event.consume();
            }
        });
        this.pdfVisualView = new PdfVisualDocumentView(
                viewModel.applicationServices().document().buildPdfVisualDocument(),
                viewModel.applicationServices().document().renderPdfVisualPage(),
                viewModel::readingZoomPercent,
                this::capturePdfRegionSelection);
        pdfVisualView.setTextTargetSelectionHandler(this::selectPdfTextTarget);
        pdfVisualView.setTextPreparationRequestHandler(this::preparePdfTextPageNow);
        pdfVisualView.setVisible(false);
        pdfVisualView.setManaged(false);
        pdfVisualView.documentProgressProperty().addListener((obs, oldValue, newValue) ->
                viewModel.updatePdfVisualDocumentProgress(newValue == null ? 0.0 : newValue.doubleValue()));
        pdfVisualView.visiblePageNumberProperty().addListener((obs, oldValue, newValue) ->
                preparePdfVisibleTextPage(newValue == null ? 0 : newValue.intValue()));
        viewModel.readingFontSizeProperty().addListener((obs, oldValue, newValue) -> {
            applyReadingFontSizeClass(newValue.intValue());
            pdfVisualView.refreshZoom();
        });

        FloatingReadingControlBar readingControls = floatingReadingControl();
        floatingReadingControls = readingControls;
        // StackPane readingStage = new StackPane(documentScroll, readingControls)
        // Compatibilidad de guardarrail: el visor PDF se intercala debajo de la misma playbar flotante.
        StackPane readingStage = new StackPane(documentScroll, pdfVisualView, readingControls);
        readingStage.getStyleClass().add("document-reading-stage");
        readingStage.setMinWidth(0);
        StackPane.setAlignment(documentScroll, Pos.CENTER);
        StackPane.setAlignment(pdfVisualView, Pos.CENTER);
        StackPane.setAlignment(readingControls, Pos.TOP_CENTER);
        StackPane.setMargin(readingControls, new Insets(6, 24, 0, 24));

        BorderPane documentSurface = new BorderPane(readingStage);
        documentSurface.getStyleClass().add("document-surface");
        documentSurface.setMinWidth(240);

        Node leftDock = buildSideDock();
        TheatreSideDock theatreSideDock = null;
        DocumentStudySideDock documentStudySideDock = null;
        NarrativeVideoSideDock narrativeVideoSideDock = null;
        ObservableBooleanValue activeRightDockExpanded;
        SplitPane splitPane;
        if (mode == DocumentWorkspaceMode.THEATRE_SCRIPT) {
            theatreSideDock = buildTheatreSideDock();
            SplitPane.setResizableWithParent(leftDock, Boolean.FALSE);
            SplitPane.setResizableWithParent(documentSurface, Boolean.TRUE);
            SplitPane.setResizableWithParent(theatreSideDock, Boolean.FALSE);
            splitPane = new SplitPane(leftDock, documentSurface, theatreSideDock);
            activeRightDockExpanded = theatreSideDock.expandedProperty();
        } else {
            theatreSideDock = buildTheatreSideDock();
            documentStudySideDock = buildDocumentStudySideDock();
            narrativeVideoSideDock = buildNarrativeVideoSideDock();
            StackPane rightDockHost = new StackPane(documentStudySideDock, narrativeVideoSideDock, theatreSideDock);
            TheatreSideDock finalTheatreSideDock = theatreSideDock;
            DocumentStudySideDock finalDocumentStudySideDock = documentStudySideDock;
            NarrativeVideoSideDock finalNarrativeVideoSideDock = narrativeVideoSideDock;
            StackPane.setAlignment(finalTheatreSideDock, Pos.CENTER_RIGHT);
            StackPane.setAlignment(finalDocumentStudySideDock, Pos.CENTER_RIGHT);
            StackPane.setAlignment(finalNarrativeVideoSideDock, Pos.CENTER_RIGHT);
            Runnable syncRightDockVisibility = () -> {
                boolean theatreProject = currentProjectUsesTheatreDock();
                boolean narrativeProject = currentProjectUsesNarrativeDock();
                finalTheatreSideDock.setVisible(theatreProject);
                finalTheatreSideDock.setManaged(theatreProject);
                finalNarrativeVideoSideDock.setVisible(narrativeProject);
                finalNarrativeVideoSideDock.setManaged(narrativeProject);
                finalDocumentStudySideDock.setVisible(!theatreProject && !narrativeProject);
                finalDocumentStudySideDock.setManaged(!theatreProject && !narrativeProject);
            };
            rightDockHost.minWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> currentProjectUsesTheatreDock()
                            ? finalTheatreSideDock.getMinWidth()
                            : currentProjectUsesNarrativeDock()
                                    ? finalNarrativeVideoSideDock.getMinWidth()
                                    : finalDocumentStudySideDock.getMinWidth(),
                    viewModel.currentProjectModeProperty(),
                    finalTheatreSideDock.minWidthProperty(),
                    finalNarrativeVideoSideDock.minWidthProperty(),
                    finalDocumentStudySideDock.minWidthProperty()));
            rightDockHost.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> currentProjectUsesTheatreDock()
                            ? finalTheatreSideDock.getPrefWidth()
                            : currentProjectUsesNarrativeDock()
                                    ? finalNarrativeVideoSideDock.getPrefWidth()
                                    : finalDocumentStudySideDock.getPrefWidth(),
                    viewModel.currentProjectModeProperty(),
                    finalTheatreSideDock.prefWidthProperty(),
                    finalNarrativeVideoSideDock.prefWidthProperty(),
                    finalDocumentStudySideDock.prefWidthProperty()));
            rightDockHost.maxWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> currentProjectUsesTheatreDock()
                            ? finalTheatreSideDock.getMaxWidth()
                            : currentProjectUsesNarrativeDock()
                                    ? finalNarrativeVideoSideDock.getMaxWidth()
                                    : finalDocumentStudySideDock.getMaxWidth(),
                    viewModel.currentProjectModeProperty(),
                    finalTheatreSideDock.maxWidthProperty(),
                    finalNarrativeVideoSideDock.maxWidthProperty(),
                    finalDocumentStudySideDock.maxWidthProperty()));
            syncRightDockVisibility.run();
            viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> syncRightDockVisibility.run());
            SplitPane.setResizableWithParent(leftDock, Boolean.FALSE);
            SplitPane.setResizableWithParent(documentSurface, Boolean.TRUE);
            SplitPane.setResizableWithParent(rightDockHost, Boolean.FALSE);
            splitPane = new SplitPane(leftDock, documentSurface, rightDockHost);
            activeRightDockExpanded = Bindings.createBooleanBinding(
                    () -> currentProjectUsesTheatreDock()
                            ? finalTheatreSideDock.expandedProperty().get()
                            : currentProjectUsesNarrativeDock()
                                    ? finalNarrativeVideoSideDock.expandedProperty().get()
                                    : finalDocumentStudySideDock.expandedProperty().get(),
                    viewModel.currentProjectModeProperty(),
                    finalTheatreSideDock.expandedProperty(),
                    finalNarrativeVideoSideDock.expandedProperty(),
                    finalDocumentStudySideDock.expandedProperty());
        }
        splitPane.getStyleClass().add("document-split");
        if (mode == DocumentWorkspaceMode.THEATRE_SCRIPT) {
            splitPane.setDividerPositions(0.19, 0.68); // splitPane.setDividerPositions(0.19, 0.55)
        } else {
            splitPane.setDividerPositions(0.20, 0.78);
        }
        installFloatingControlResponsiveness(readingControls, readingStage, activeRightDockExpanded);
        setCenter(splitPane);

        ChangeListener<ReadableDocument> documentListener = (obs, oldValue, newValue) -> render(newValue);
        viewModel.currentDocumentProperty().addListener(documentListener);
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> {
            render(viewModel.currentDocumentProperty().get());
            syncActivePlaybackCue(viewModel.playbackCursorProperty().get());
        });
        viewModel.focusedTheatreSceneIdProperty().addListener((obs, oldValue, newValue) ->
                render(viewModel.currentDocumentProperty().get()));
        intervencionBoundaryStore.revisionProperty().addListener((obs, oldValue, newValue) ->
                render(viewModel.currentDocumentProperty().get()));
        viewModel.playbackCursorProperty().addListener((obs, oldValue, newValue) -> syncActivePlaybackCue(newValue));
        selectedBlockId.addListener((obs, oldValue, newValue) -> updateSelectionStyles());
        viewModel.selectedDocumentTextRangeProperty().addListener((obs, oldValue, newValue) -> {
            updateSelectionStyles();
            updateSentenceSelectionStyles();
            updatePdfPinnedSelection();
        });
        viewModel.technicalProblemPreparationActiveProperty().addListener((obs, oldValue, newValue) -> {
            ReadableDocument document = viewModel.currentDocumentProperty().get();
            if (shouldUsePdfVisualDocument(document)) {
                pdfVisualView.setRegionSelectionActive(Boolean.TRUE.equals(newValue));
            } else {
                render(document);
            }
        });
        viewModel.selectedDocumentBlockIdProperty().addListener((obs, oldValue, newValue) -> {
            String normalized = newValue == null ? "" : newValue;
            if (!normalized.equals(selectedBlockId.get())) {
                selectedBlockId.set(normalized);
            }
            updatePdfPinnedSelection();
            scrollToDocumentBlock(normalized);
        });
        documentScroll.vvalueProperty().addListener((obs, oldValue, newValue) -> maybeAdvanceRenderWindowForScroll(newValue == null ? 0.0 : newValue.doubleValue()));
        render(viewModel.currentDocumentProperty().get());
    }

    private void installFloatingControlResponsiveness(FloatingReadingControlBar controls, StackPane readingStage,
                                                      ObservableBooleanValue rightSidebarExpanded) {
        InvalidationListener refresh = ignored -> updateFloatingControlLayout(
                controls, readingStage, rightSidebarExpanded != null && rightSidebarExpanded.get());
        readingStage.widthProperty().addListener(refresh);
        if (rightSidebarExpanded != null) {
            rightSidebarExpanded.addListener(refresh);
        }
        viewModel.documentPlaybarDockedProperty().addListener(refresh);
        updateFloatingControlLayout(controls, readingStage, rightSidebarExpanded != null && rightSidebarExpanded.get());
        Platform.runLater(() -> updateFloatingControlLayout(
                controls, readingStage, rightSidebarExpanded != null && rightSidebarExpanded.get()));
    }

    private void updateFloatingControlLayout(FloatingReadingControlBar controls, StackPane readingStage,
                                             boolean rightSidebarExpanded) {
        boolean dockedInLeftRail = rightSidebarExpanded || viewModel.documentPlaybarDockedProperty().get();
        controls.setVisible(!dockedInLeftRail);
        controls.setManaged(!dockedInLeftRail);
        if (dockedInLeftRail) {
            return;
        }
        boolean narrowReader = readingStage.getWidth() > 0 && readingStage.getWidth() < 760.0;
        boolean vertical = narrowReader;
        controls.setVerticalLayout(vertical);
        if (vertical) {
            StackPane.setAlignment(controls, Pos.TOP_LEFT);
            StackPane.setMargin(controls, new Insets(10, 0, 0, 12));
        } else {
            StackPane.setAlignment(controls, Pos.TOP_CENTER);
            StackPane.setMargin(controls, new Insets(6, 24, 0, 24));
        }
    }

    private Node buildSideDock() {
        SideDockModuleRegistry registry = new SideDockModuleRegistry()
                .register(StaticSideDockModule.of(
                        SideDockModuleId.DOCUMENT_CONTEXT_DETAILS,
                        "Fragmento",
                        "Seleccion seleccionado y acciones basicas.",
                        "Texto",
                        () -> new DocumentContextDetailsPanel(viewModel)))
                .register(StaticSideDockModule.of(
                        SideDockModuleId.DOCUMENT_INDEX,
                        "Índice",
                        "Árbol del documento: raíz, secciones y hojas para saltar sin modificar la fuente.",
                        "Árbol",
                    () -> new DocumentIndexPanel(
                            viewModel.currentDocumentProperty(),
                            selectedBlockId,
                            this::selectBlock,
                            viewModel.applicationServices().document().buildDocumentOutline(),
                            viewModel.applicationServices().document().buildPdfEnhancedOutline(),
                            viewModel.applicationServices().document().searchPdfText(),
                            this::showPdfSearchHighlight,
                            this::scrollPdfVisualToPage,
                            this::pdfOcrCacheDirectory,
                            viewModel.applicationServices().document().resolvePdfNarratableDocument(),
                            document -> viewModel.replaceCurrentDocumentFromApplication(document, "Texto OCR del PDF guardado como fragmentos narrables."))))
                .register(StaticSideDockModule.of(
                        SideDockModuleId.DOCUMENT_AUDIO_NARRATION,
                        "Audio",
                        "Origen de audio usable para la selección: voz local/probada o archivo del computador.",
                        "Audio",
                        () -> new DocumentAudioNarrationPanel(viewModel)));
        return new WorkspaceSideDock(
                new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Documento"),
                registry,
                true,
                WorkspaceSideDock.RailPlacement.LEFT,
                this::railReadingControl);
    }

    private DocumentStudySideDock buildDocumentStudySideDock() {
        return new DocumentStudySideDock(
                viewModel,
                technicalProblemSelection,
                pdfRegionCaptureSelection,
                this::selectedTechnicalProblemBlocks,
                this::selectedPdfRegionCaptures,
                this::clearTechnicalProblemSelection,
                this::openTechnicalProblemDialog);
    }

    private TheatreSideDock buildTheatreSideDock() {
        return new TheatreSideDock(viewModel, this.saveProjectRequest, intervencionBoundaryStore);
    }

    private NarrativeVideoSideDock buildNarrativeVideoSideDock() {
        return new NarrativeVideoSideDock(viewModel);
    }

    private boolean currentProjectUsesTheatreDock() {
        return viewModel.currentProjectModeProperty().get() == ProjectMode.THEATRE_PRODUCTION;
    }

    private boolean currentProjectUsesNarrativeDock() {
        return viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO;
    }

    private Node railReadingControl() {
        if (railReadingControls == null) {
            railReadingControls = new RailReadingControlBar(
                    viewModel.documentPrimaryActionLabelProperty(),
                    this::runPrimaryActionFromPlaybar,
                    viewModel::pausePlayback,
                    viewModel::resumePlayback,
                    viewModel::stopPlayback,
                    viewModel::playPreviousFragment,
                    viewModel::playNextFragment,
                    viewModel.previousFragmentAvailableProperty(),
                    viewModel.nextFragmentAvailableProperty(),
                    Bindings.createBooleanBinding(() -> {
                        PlaybackCursor cursor = viewModel.playbackCursorProperty().get();
                        return cursor != null && !cursor.stoppedState();
                    }, viewModel.playbackCursorProperty()),
                    viewModel.playbackRateProperty(),
                    () -> viewModel.setPlaybackRate(1.0),
                    () -> viewModel.setPlaybackRate(1.5),
                    () -> viewModel.setPlaybackRate(1.75),
                    viewModel::refreshSourceDocument);
            railReadingControls.visibleProperty().bind(Bindings.or(
                    viewModel.documentRightRailVisibleProperty(),
                    viewModel.documentPlaybarDockedProperty()));
            railReadingControls.managedProperty().bind(railReadingControls.visibleProperty());
        }
        return railReadingControls;
    }

    private void applyReadingFontSizeClass(int fontSize) {
        getStyleClass().removeIf(styleClass -> styleClass.startsWith(READING_SIZE_CLASS_PREFIX));
        getStyleClass().add(READING_SIZE_CLASS_PREFIX + fontSize);
    }

    private void render(ReadableDocument document) {
        ReadableDocument previousDocument = renderedDocument;
        renderedDocument = document;
        activePlaybackBlockId = "";
        activePlaybackUnitId = "";
        resetPdfRegionSelectionIfSourceChanged(document);
        if (document == null) {
            showBlockDocumentReader();
            pdfVisualView.clear();
            viewModel.updatePdfVisualDocumentProgress(0.0);
            blockIndexById.clear();
            renderedWindow = DocumentRenderWindow.all(0);
            renderWindow(document, renderedWindow);
            return;
        }
        rebuildBlockIndex(document);
        if (shouldUsePdfVisualDocument(document)) {
            showPdfVisualDocument(document, samePdfVisualSource(previousDocument, document) && pdfVisualView.isVisible());
            renderedWindow = DocumentRenderWindow.all(document.blocks().size());
            return;
        }
        showBlockDocumentReader();
        pdfVisualView.clear();
        viewModel.updatePdfVisualDocumentProgress(0.0);
        int pivot = Math.max(0, blockIndex(document, selectedBlockId.get()).orElse(0));
        renderedWindow = windowFor(document, pivot);
        renderWindow(document, renderedWindow);
    }

    private boolean shouldUsePdfVisualDocument(ReadableDocument document) {
        return document != null && document.format() == SourceDocumentFormat.PDF;
    }

    private void showPdfVisualDocument(ReadableDocument document, boolean reuseVisualDocument) {
        documentScroll.setVisible(false);
        documentScroll.setManaged(false);
        pdfVisualView.setVisible(true);
        pdfVisualView.setManaged(true);
        content.getChildren().clear();
        blockNodes.clear();
        sentenceNodes.clear();
        sentenceSpanIndex.clear();
        pdfVisualView.setReadingProjection(pdfProjection(document));
        if (!reuseVisualDocument) {
            pdfVisualView.showDocument(document);
            preparePdfVisibleTextPage(1);
        }
        viewModel.updatePdfVisualDocumentProgress(pdfVisualView.documentProgressProperty().get());
        pdfVisualView.setRegionSelectionActive(viewModel.technicalProblemPreparationActiveProperty().get());
        updatePdfPinnedSelection();
        syncActivePlaybackCue(viewModel.playbackCursorProperty().get());
    }

    private boolean samePdfVisualSource(ReadableDocument previous, ReadableDocument current) {
        return shouldUsePdfVisualDocument(previous) && shouldUsePdfVisualDocument(current)
                && Objects.equals(previous.sourcePath(), current.sourcePath());
    }

    private void preparePdfVisibleTextPage(int page) {
        viewModel.updatePdfVisiblePageNumber(page);
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (shouldUsePdfVisualDocument(document)) {
            viewModel.updateStatusMessage("Pagina PDF " + page + " lista. Haz clic en la hoja para analizar OCR.");
        }
    }

    private void preparePdfTextPageNow(int page) {
        viewModel.updatePdfVisiblePageNumber(page);
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (shouldUsePdfVisualDocument(document)) {
            viewModel.updateStatusMessage("Analizando pagina PDF " + page + " por clic...");
            pdfVisibleTextPreparation.preparePageNow(document, page);
        }
    }

    public void retryVisiblePdfTextPreparation() {
        int page = Math.max(viewModel.pdfVisiblePageNumber(), pdfVisualView.visiblePageNumberProperty().get());
        if (page > 0) {
            preparePdfTextPageNow(page);
        }
    }

    private void scrollPdfVisualToPage(int pageNumber) {
        pdfVisualView.scrollToPage(pageNumber);
        viewModel.updateStatusMessage("Pagina PDF " + pageNumber + ".");
    }

    private double pdfTextViewportTopOffset() {
        Node playbar = floatingReadingControls;
        if (playbar == null || !playbar.isVisible() || playbar.getScene() == null || pdfVisualView.getScene() == null) {
            return 50.0;
        }
        Bounds barBounds = playbar.localToScene(playbar.getBoundsInLocal());
        Bounds pdfBounds = pdfVisualView.localToScene(pdfVisualView.getBoundsInLocal());
        if (barBounds == null || pdfBounds == null) {
            return 50.0;
        }
        return Math.max(50.0, barBounds.getMaxY() - pdfBounds.getMinY() + 50.0);
    }

    private void showBlockDocumentReader() {
        pdfVisualView.setRegionSelectionActive(false);
        pdfVisualView.clearTextHighlight();
        pdfVisualView.clearPinnedTextTarget();
        viewModel.updatePdfVisualDocumentProgress(0.0);
        pdfVisualView.setVisible(false);
        pdfVisualView.setManaged(false);
        documentScroll.setVisible(true);
        documentScroll.setManaged(true);
    }

    private void resetPdfRegionSelectionIfSourceChanged(ReadableDocument document) {
        Path next = shouldUsePdfVisualDocument(document) ? document.sourcePath() : null;
        if (Objects.equals(pdfRegionSelectionSourcePath, next)) {
            return;
        }
        pdfRegionSelectionSourcePath = next;
        pdfRegionCaptureSelection.clear();
        nextPdfRegionCaptureIndex = 1;
    }

    private void renderWindow(ReadableDocument document, DocumentRenderWindow window) {
        content.getChildren().clear();
        blockNodes.clear();
        sentenceNodes.clear();
        sentenceSpanIndex.clear();
        if (document == null) {
            content.getChildren().add(emptyState());
            return;
        }
        Label title = new Label(documentTitleWithExtension(document));
        title.getStyleClass().add("document-title");
        Label modeLabel = new Label(documentModeLabel());
        modeLabel.getStyleClass().add("document-mode-label");
        VBox header = new VBox(8, title, modeLabel);
        header.getStyleClass().add("document-reading-header");
        content.getChildren().add(header);
        if (largeDocument(document)) {
            content.getChildren().add(largeDocumentNotice(window));
        }
        java.util.List<DocumentBlock> blocksToRender = visibleBlocksForInitialRender(document);
        for (DocumentBlock block : blocksToRender) {
            Node card = blockCard(block);
            blockNodes.put(block.id(), card);
            content.getChildren().add(card);
        }
        if (largeDocument(document)) {
            content.getChildren().add(largeDocumentNotice(window));
        }
        updateSelectionStyles();
        updateSentenceSelectionStyles();
        syncActivePlaybackCue(viewModel.playbackCursorProperty().get());
    }

    private java.util.List<DocumentBlock> visibleBlocksForInitialRender(ReadableDocument document) {
        if (document == null) {
            return java.util.List.of();
        }
        if (!largeDocument(document)) {
            return document.blocks();
        }
        int start = Math.max(0, Math.min(renderedWindow.startInclusive(), document.blocks().size()));
        int end = Math.max(start, Math.min(renderedWindow.endExclusive(), document.blocks().size()));
        return document.blocks().subList(start, end);
    }

    private boolean largeDocument(ReadableDocument document) {
        return document != null && document.blocks().size() > LARGE_DOCUMENT_INITIAL_RENDER_LIMIT;
    }

    private DocumentRenderWindow windowFor(ReadableDocument document, int pivotIndex) {
        if (!largeDocument(document)) {
            return DocumentRenderWindow.all(document == null ? 0 : document.blocks().size());
        }
        return DocumentRenderWindow.around(document.blocks().size(), pivotIndex, LARGE_DOCUMENT_WINDOW_SIZE);
    }

    private Node largeDocumentNotice(DocumentRenderWindow window) {
        VBox notice = new VBox(8);
        notice.getStyleClass().add("document-large-preview-notice");
        Label title = new Label("Documento grande abierto");
        title.getStyleClass().add("document-large-preview-title");
        Label detail = new Label(window.humanRangeLabel()
                + ". DocuPodcast carga solo una ventana cercana al punto actual para mantener fluido el lector. La fuente completa permanece disponible para lectura, búsqueda interna, audio y capas del proyecto.");
        detail.setWrapText(true);
        detail.getStyleClass().add("document-large-preview-detail");
        HBox actions = new HBox(8);
        actions.getStyleClass().add("document-large-preview-actions");
        Button previous = new Button("Bloques anteriores");
        previous.getStyleClass().add("document-large-preview-button");
        previous.setDisable(!window.hasPrevious());
        previous.setOnAction(event -> renderWindowAroundIndex(Math.max(0, window.startInclusive() - 1), true));
        Button next = new Button("Siguientes bloques");
        next.getStyleClass().add("document-large-preview-button");
        next.setDisable(!window.hasNext());
        next.setOnAction(event -> renderWindowAroundIndex(Math.min(window.totalBlocks() - 1, window.endExclusive()), true));
        Button beginning = new Button("Inicio del documento");
        beginning.getStyleClass().add("document-large-preview-button-secondary");
        beginning.setDisable(!window.hasPrevious());
        beginning.setOnAction(event -> renderWindowAroundIndex(0, true));
        Button end = new Button("Final del documento");
        end.getStyleClass().add("document-large-preview-button-secondary");
        end.setDisable(!window.hasNext());
        end.setOnAction(event -> renderWindowAroundIndex(Math.max(0, window.totalBlocks() - 1), true));
        actions.getChildren().addAll(previous, next, beginning, end);
        notice.getChildren().addAll(title, detail, actions);
        return notice;
    }

    private void renderWindowAroundIndex(int pivotIndex, boolean scrollTop) {
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (document == null) {
            return;
        }
        renderedWindow = windowFor(document, pivotIndex);
        renderWindow(document, renderedWindow);
        if (scrollTop) {
            Platform.runLater(() -> documentScroll.setVvalue(0));
        }
    }

    private void maybeAdvanceRenderWindowForScroll(double value) {
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (document == null || !largeDocument(document) || autoWindowSwitchInProgress) {
            return;
        }
        if (value >= 0.985 && renderedWindow.hasNext()) {
            autoSwitchToWindow(Math.min(renderedWindow.totalBlocks() - 1, renderedWindow.endExclusive()), 0.08);
        } else if (value <= 0.015 && renderedWindow.hasPrevious()) {
            autoSwitchToWindow(Math.max(0, renderedWindow.startInclusive() - 1), 0.92);
        }
    }

    private void autoSwitchToWindow(int pivotIndex, double nextScrollValue) {
        autoWindowSwitchInProgress = true;
        renderWindowAroundIndex(pivotIndex, false);
        Platform.runLater(() -> {
            documentScroll.setVvalue(Math.max(0.0, Math.min(1.0, nextScrollValue)));
            Platform.runLater(() -> autoWindowSwitchInProgress = false);
        });
    }

    private Label followModeNote() {
        Label note = new Label(readingAnchor.userLabel());
        note.setWrapText(true);
        note.getStyleClass().add("document-follow-note");
        return note;
    }

    private FloatingReadingControlBar floatingReadingControl() {
        FloatingReadingControlBar control = new FloatingReadingControlBar(
                viewModel.documentPrimaryActionLabelProperty(),
                viewModel.documentPrimaryActionHintProperty(),
                this::runPrimaryActionFromPlaybar,
                viewModel::pausePlayback,
                viewModel::resumePlayback,
                viewModel::stopPlayback,
                viewModel::playDocumentFromBeginning,
                viewModel::playPreviousFragment,
                viewModel::playNextFragment,
                viewModel.previousFragmentAvailableProperty(),
                viewModel.nextFragmentAvailableProperty(),
                Bindings.createBooleanBinding(() -> {
                    PlaybackCursor cursor = viewModel.playbackCursorProperty().get();
                    return cursor != null && !cursor.stoppedState();
                }, viewModel.playbackCursorProperty()),
                viewModel.playbackRateProperty(),
                () -> viewModel.setPlaybackRate(1.0),
                () -> viewModel.setPlaybackRate(1.5),
                () -> viewModel.setPlaybackRate(1.75),
                "Refrescar contenido",
                viewModel::refreshSourceDocument);
        Tooltip.install(control, new Tooltip("Control global de lectura: iniciar, pausar, reanudar, detener o saltar entre fragmentos."));
        return control;
    }

    private String documentModeLabel() {
        return mode == DocumentWorkspaceMode.THEATRE_SCRIPT ? "Mapa textual" : "Vista documento";
    }

    private void runPrimaryActionFromPlaybar() {
        boolean needsProjectContainer = viewModel.currentDocumentProperty().get() != null
                && viewModel.projectOpenProperty().get()
                && viewModel.currentProjectFile().isEmpty();
        if (needsProjectContainer && !saveProjectBeforeAudioRequest.getAsBoolean()) {
            return;
        }
        if (viewModel.audioEngineUnavailableForDocumentPrimaryAction() && !audioEngineBeforeGenerationRequest.getAsBoolean()) {
            return;
        }
        viewModel.runDocumentPrimaryAction();
    }

    private VBox emptyState() {
        EmptyStateView box = new EmptyStateView(
                "Documento narrado",
                "Abre una fuente documental para leerla y escucharla desde aquí",
                "DocuPodcast conserva la fuente como solo lectura; las capas de voz, audio, emoción e imagen viven en el proyecto, no dentro del Word original.");
        box.getStyleClass().add("document-empty-state");
        return box;
    }

    private VBox blockCard(DocumentBlock block) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("document-block", "document-block-" + block.type().name().toLowerCase(Locale.ROOT));
        card.setOnMouseClicked(event -> {
            selectBlock(block.id());
            event.consume();
        });
        card.setOnContextMenuRequested(event -> {
            ContextMenu menu = paragraphContextMenu(block);
            menu.show(card, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        Label marker = new Label(blockMarker(block));
        marker.getStyleClass().add("document-block-marker");
        Tooltip.install(marker, new Tooltip(blockMarkerTooltip(block)));

        VBox textColumn = new VBox(3);
        textColumn.getStyleClass().add("document-block-body");
        theatreReplicaBadge(block).ifPresent(textColumn.getChildren()::add);
        Node text = sourceVisualHeadlineOrSentenceFlow(block);
        textColumn.getChildren().add(text);
        sourceVisualPreview(block).ifPresent(textColumn.getChildren()::add);
        Tooltip.install(card, new Tooltip(readableKind(block) + " · " + blockMarkerTooltip(block)));

        intervencionBadge(block).ifPresent(badge -> {
            HBox aliasRow = new HBox(badge);
            aliasRow.getStyleClass().add("document-block-alias-row");
            card.getChildren().add(aliasRow);
        });
        HBox row = new HBox(8);
        if (viewModel.technicalProblemPreparationActiveProperty().get()) {
            row.getChildren().add(problemCheckBox(block));
        }
        row.getChildren().addAll(marker, textColumn);
        row.getStyleClass().add("document-block-row");
        card.getChildren().add(row);
        return card;
    }

    private CheckBox problemCheckBox(DocumentBlock block) {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("document-problem-checkbox");
        checkBox.setSelected(technicalProblemSelection.contains(block.id()));
        checkBox.setTooltip(new Tooltip("Incluir este bloque en el problema tecnico"));
        checkBox.setOnAction(event -> {
            if (checkBox.isSelected()) {
                technicalProblemSelection.add(block.id());
            } else {
                technicalProblemSelection.remove(block.id());
            }
            event.consume();
        });
        checkBox.setOnMouseClicked(event -> event.consume());
        return checkBox;
    }

    private List<DocumentBlock> selectedTechnicalProblemBlocks() {
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (document == null || technicalProblemSelection.isEmpty()) {
            return List.of();
        }
        return document.blocks().stream()
                .filter(block -> technicalProblemSelection.contains(block.id()))
                .toList();
    }

    private List<PdfRegionCaptureDraft> selectedPdfRegionCaptures() {
        return List.copyOf(pdfRegionCaptureSelection);
    }

    private void clearTechnicalProblemSelection() {
        technicalProblemSelection.clear();
        pdfRegionCaptureSelection.clear();
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (!shouldUsePdfVisualDocument(document)) {
            renderWindow(document, renderedWindow);
        }
        viewModel.updateStatusMessage("Seleccion de problema tecnico limpia.");
    }

    private void openTechnicalProblemDialog() {
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (shouldUsePdfVisualDocument(document)) {
            openPdfTechnicalProblemDialog();
            return;
        }
        List<DocumentBlock> selectedBlocks = selectedTechnicalProblemBlocks();
        if (selectedBlocks.isEmpty()) {
            viewModel.updateStatusMessage("Marca bloques del documento antes de generar un problema tecnico.");
            return;
        }
        Map<String, Path> sourceCropPreviews = viewModel.applicationServices().document().prepareStudySourceCrops().prepare(document, selectedBlocks);
        TechnicalProblemDialog.show(getScene() == null ? null : getScene().getWindow(), selectedBlocks, sourceCropPreviews)
                .ifPresent(result -> {
                    try {
                        List<StudyProblemSourceDraft> sources = new java.util.ArrayList<>(selectedBlocks.stream()
                                .map(block -> StudyProblemSourceDraft.fromBlock(block, result.sourceCropPaths().get(block.id())))
                                .toList());
                        sources.addAll(result.additionalSources());
                        viewModel.saveTechnicalProblemFromSources(sources, result.title(), result.solutionText(),
                                result.canvasSnapshot(), result.canvasStateJson());
                        if (result.externalPngTarget() != null) {
                            exportExternalTechnicalProblemImage(result);
                        }
                        clearTechnicalProblemSelection();
                    } catch (java.io.IOException | RuntimeException ex) {
                        viewModel.updateStatusMessage("No se pudo guardar el problema tecnico: " + ex.getMessage());
                    }
                });
    }

    private void openPdfTechnicalProblemDialog() {
        List<PdfRegionCaptureDraft> regions = selectedPdfRegionCaptures();
        if (regions.isEmpty()) {
            viewModel.updateStatusMessage("Arrastra una o mas regiones sobre el PDF antes de generar un problema tecnico.");
            return;
        }
        if (viewModel.currentProjectDirectory().isEmpty()) {
            viewModel.updateStatusMessage("Guarda el proyecto antes de crear problemas PDF por capturas.");
            return;
        }
        List<StudyProblemSourceDraft> sourceDrafts = regions.stream()
                .map(PdfRegionCaptureDraft::toStudySourceDraft)
                .toList();
        TechnicalProblemDialog.showForDrafts(getScene() == null ? null : getScene().getWindow(), sourceDrafts)
                .ifPresent(result -> {
                    try {
                        List<StudyProblemSourceDraft> sources = new java.util.ArrayList<>(sourceDrafts);
                        sources.addAll(result.additionalSources());
                        viewModel.saveTechnicalProblemFromSources(sources, result.title(), result.solutionText(),
                                result.canvasSnapshot(), result.canvasStateJson());
                        if (result.externalPngTarget() != null) {
                            exportExternalTechnicalProblemImage(result);
                        }
                        clearTechnicalProblemSelection();
                    } catch (java.io.IOException | RuntimeException ex) {
                        viewModel.updateStatusMessage("No se pudo guardar el problema tecnico PDF: " + ex.getMessage());
                    }
                });
    }

    private void exportExternalTechnicalProblemImage(TechnicalProblemDialog.TechnicalProblemResult result) throws java.io.IOException {
        if (result == null || result.externalPngTarget() == null) {
            return;
        }
        if (result.externalCanvasSnapshot() == null) {
            viewModel.updateStatusMessage("No se pudo exportar PNG externo: el lienzo no produjo imagen premium.");
            return;
        }
        Path output = viewModel.exportTechnicalProblemImage(result.externalCanvasSnapshot(), result.externalPngTarget());
        if (result.exportWarnings().isEmpty()) {
            return;
        }
        viewModel.updateStatusMessage("Solucion PNG exportada: " + output.getFileName() + ". "
                + String.join(" ", result.exportWarnings()));
    }

    private void capturePdfRegionSelection(PdfViewportSelection selection) {
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (!shouldUsePdfVisualDocument(document) || selection == null) {
            return;
        }
        Path sourcePath = document.sourcePath();
        String regionId = nextPdfRegionId();
        viewModel.updateStatusMessage("Capturando region PDF " + regionId + "...");
        Task<PdfRegionCaptureResult> task = new Task<>() {
            @Override
            protected PdfRegionCaptureResult call() throws Exception {
                return viewModel.applicationServices().document().capturePdfVisualRegion().capture(new PdfRegionCaptureRequest(
                        sourcePath,
                        selection,
                        null,
                        4.0,
                        216,
                        48_000_000L,
                        java.awt.Color.WHITE,
                        true));
            }
        };
        task.setOnSucceeded(event -> {
            if (!Objects.equals(pdfRegionSelectionSourcePath, sourcePath)) {
                return;
            }
            PdfRegionCaptureResult result = task.getValue();
            pdfRegionCaptureSelection.add(new PdfRegionCaptureDraft(
                    regionId,
                    result.sourcePage(),
                    result.bbox(),
                    result.pngPath(),
                    result.widthPixels(),
                    result.heightPixels(),
                    result.dpi(),
                    result.warnings()));
            viewModel.updateStatusMessage("Region PDF agregada al problema: " + regionId + " (p. " + result.sourcePage() + ").");
        });
        task.setOnFailed(event -> viewModel.updateStatusMessage(
                "No se pudo capturar la region PDF: " + diagnostic(task.getException())));
        Thread worker = new Thread(task, "pdf-region-capture-" + regionId);
        worker.setDaemon(true);
        worker.start();
    }

    private String nextPdfRegionId() {
        return "PDFREG-" + String.format(Locale.ROOT, "%04d", nextPdfRegionCaptureIndex++);
    }

    private static String diagnostic(Throwable ex) {
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "error sin detalle." : message;
    }



    private Node sourceVisualHeadlineOrSentenceFlow(DocumentBlock block) {
        if (block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.TABLE_NOTICE) {
            String rows = block.metadata().getOrDefault("rows", "?");
            String columns = block.metadata().getOrDefault("columns", "?");
            Label text = new Label("Tabla del documento fuente (" + rows + " filas, " + columns + " columnas)");
            text.setWrapText(true);
            text.getStyleClass().add("document-block-text");
            return text;
        }
        return sentenceFlow(block);
    }

    private Optional<Node> sourceVisualPreview(DocumentBlock block) {
        if (block == null) {
            return Optional.empty();
        }
        if (block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.IMAGE_NOTICE) {
            String base64 = block.metadata().getOrDefault("embeddedImageBase64", "");
            Optional<SourceVisualBlockView> image = SourceVisualBlockView.embeddedImage(
                    "Imagen detectada en la fuente",
                    "Se muestra como parte del documento original. No entra a la secuencia visual hasta que el usuario la asocie.",
                    base64,
                    viewModel::playbackActiveForFullscreenPause,
                    viewModel::pausePlayback,
                    viewModel::resumePlayback);
            if (image.isPresent()) {
                return image.map(node -> (Node) node);
            }
            return Optional.of(SourceVisualBlockView.placeholder(
                    "Imagen detectada en la fuente",
                    "El documento informa una imagen, pero no se pudo previsualizar sus bytes. Sigue siendo un bloque visual fuente asignable por el usuario.",
                    "Imagen"));
        }
        if (block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.TABLE_NOTICE) {
            String rows = block.metadata().getOrDefault("rows", "?");
            String columns = block.metadata().getOrDefault("columns", "?");
            String preview = block.metadata().getOrDefault("tableMarkdown", "");
            String detail = "Tabla detectada en el documento fuente (" + rows + " filas, " + columns + " columnas). Bloque visual no narrable por defecto.";
            return Optional.of(SourceVisualBlockView.tableGrid(
                    "Tabla detectada en la fuente",
                    detail,
                    preview,
                    rows,
                    columns));
        }
        if (block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.MATH_NOTICE) {
            return Optional.of(SourceVisualBlockView.placeholder(
                    "LaTeX/fórmula detectado",
                    "DocuPodcast lo identifica como bloque visual no narrable. No se renderiza en esta versión para evitar prometer edición matemática completa.",
                    "Fórmula"));
        }
        return Optional.empty();
    }

    private Node sentenceFlow(DocumentBlock block) {
        java.util.List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split(block);
        if (spans.isEmpty()) {
            Label text = new Label(block.text());
            text.setWrapText(true);
            text.getStyleClass().add("document-block-text");
            return text;
        }
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("document-block-text-flow");
        for (int i = 0; i < spans.size(); i++) {
            DocumentSentenceSpan span = spans.get(i);
            Text sentence = new Text(span.text());
            sentence.getStyleClass().add("document-sentence");
            sentence.setOnMouseClicked(event -> {
                selectSentence(span);
                event.consume();
            });
            Tooltip.install(sentence, new Tooltip("Seleccionar oración para voz, audio, emoción o imagen"));
            sentenceNodes.put(span.id(), sentence);
            sentenceSpanIndex.put(span.id(), span);
            flow.getChildren().add(sentence);
            if (i < spans.size() - 1) {
                flow.getChildren().add(new Text(" "));
            }
        }
        return flow;
    }

    private void selectSentence(DocumentSentenceSpan span) {
        if (span == null || span.blank()) {
            return;
        }
        selectedBlockId.set(span.blockId());
        viewModel.selectDocumentTextRange(span.range(), span.text());
        updateSelectionStyles();
        updateSentenceSelectionStyles();
        scrollToDocumentBlock(span.blockId());
    }

    private void selectPdfTextTarget(PdfVisualTextTarget target) {
        if (target == null || !target.available()) {
            return;
        }
        selectedBlockId.set(target.blockId());
        if (target.kind() == com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTargetKind.SENTENCE
                && target.range() != null) {
            viewModel.selectDocumentTextRange(target.range(), target.text());
        } else {
            viewModel.selectDocumentBlock(target.blockId());
        }
        pdfVisualView.showPinnedTextTarget(target);
        pdfVisualView.scrollToTextTarget(target, pdfTextViewportTopOffset());
    }

    private void selectBlock(String blockId) {
        String normalized = blockId == null ? "" : blockId;
        selectedBlockId.set(normalized);
        viewModel.selectDocumentBlock(normalized);
        scrollToDocumentBlock(normalized);
    }

    private void scrollToDocumentBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return;
        }
        Node node = blockNodes.get(blockId);
        if (node != null) {
            scrollNodeNearReadingTop(node);
            return;
        }
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (shouldUsePdfVisualDocument(document)) {
            sourcePageForBlock(document, blockId).ifPresent(pdfVisualView::scrollToPage);
            return;
        }
        if (document == null || !largeDocument(document)) {
            return;
        }
        blockIndex(document, blockId).ifPresent(index -> {
            renderWindowAroundIndex(index, false);
            Node rendered = blockNodes.get(blockId);
            if (rendered != null) {
                scrollNodeNearReadingTop(rendered);
            }
        });
    }

    private void rebuildBlockIndex(ReadableDocument document) {
        blockIndexById.clear();
        intervencionByBlockId.clear();
        focusedTheatreSceneBlockIds = Set.of();
        if (document == null) { return; }
        for (int i = 0; i < document.blocks().size(); i++) { blockIndexById.put(document.blocks().get(i).id(), i); }
        List<IntervencionCatalogo.IntervencionInfo> aliases = IntervencionCatalogo.intervenciones(document, viewModel.currentScriptProperty().get());
        aliases.forEach(alias -> intervencionByBlockId.put(alias.blockId(), alias.alias()));
        String focusedSceneId = viewModel.focusedTheatreSceneIdProperty().get();
        Optional<com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer.Scene> focusedScene = viewModel.theatreScenes().stream()
                .filter(scene -> scene.id().equals(focusedSceneId))
                .findFirst();
        boolean completeSceneBoundary = focusedScene
                .map(scene -> intervencionBoundaryStore.limite(scene.id()))
                .map(boundary -> !boundary.startId().isBlank() && !boundary.endId().isBlank())
                .orElse(false);
        focusedTheatreSceneBlockIds = completeSceneBoundary
                ? Set.copyOf(IntervencionNumberingScene.intervencionesParaEscena(
                        aliases, viewModel.theatreScenes(), intervencionBoundaryStore, focusedScene.orElseThrow())
                        .stream().map(IntervencionCatalogo.IntervencionInfo::blockId).toList())
                : Set.of();
    }

    private Optional<Integer> blockIndex(ReadableDocument document, String blockId) {
        if (document == null || blockId == null || blockId.isBlank()) { return Optional.empty(); }
        Integer cached = blockIndexById.get(blockId);
        if (cached != null) { return Optional.of(cached); }
        rebuildBlockIndex(document);
        return Optional.ofNullable(blockIndexById.get(blockId));
    }

    private void scrollNodeNearReadingTop(Node node) {
        if (node == null) {
            return;
        }
        Platform.runLater(() -> {
            Bounds viewport = documentScroll.getViewportBounds();
            double scrollableHeight = Math.max(1.0, pageHost.getBoundsInLocal().getHeight() - viewport.getHeight());
            Bounds nodeSceneBounds = node.localToScene(node.getBoundsInLocal());
            Bounds hostSceneBounds = pageHost.localToScene(pageHost.getBoundsInLocal());
            double nodeY = nodeSceneBounds.getMinY() - hostSceneBounds.getMinY();
            double target = (nodeY - ACTIVE_READING_TOP_OFFSET) / scrollableHeight;
            documentScroll.setVvalue(Math.max(0.0, Math.min(1.0, target)));
        });
    }

    private void updateSelectionStyles() {
        String selected = selectedBlockId.get();
        boolean sentenceRangeActive = viewModel.selectedDocumentTextRangeProperty().get() != null;
        for (Map.Entry<String, Node> entry : blockNodes.entrySet()) {
            boolean active = entry.getKey().equals(selected) && !sentenceRangeActive;
            if (active && !entry.getValue().getStyleClass().contains("document-block-selected")) {
                entry.getValue().getStyleClass().add("document-block-selected");
            } else if (!active) {
                entry.getValue().getStyleClass().remove("document-block-selected");
            }
            boolean sceneFocused = focusedTheatreSceneBlockIds.contains(entry.getKey());
            if (sceneFocused && !entry.getValue().getStyleClass().contains("document-block-theatre-scene-focus")) {
                entry.getValue().getStyleClass().add("document-block-theatre-scene-focus");
            } else if (!sceneFocused) {
                entry.getValue().getStyleClass().remove("document-block-theatre-scene-focus");
            }
        }
    }

    private void updateSentenceSelectionStyles() {
        DocumentTextRange selectedRange = viewModel.selectedDocumentTextRangeProperty().get();
        for (Map.Entry<String, Node> entry : sentenceNodes.entrySet()) {
            DocumentSentenceSpan span = spanForSentenceId(entry.getKey());
            boolean active = selectedRange != null && span != null && span.range().equals(selectedRange);
            if (active && !entry.getValue().getStyleClass().contains("document-sentence-selected")) {
                entry.getValue().getStyleClass().add("document-sentence-selected");
            } else if (!active) {
                entry.getValue().getStyleClass().remove("document-sentence-selected");
            }
        }
    }

    private DocumentSentenceSpan spanForSentenceId(String sentenceId) {
        if (sentenceId == null || sentenceId.isBlank()) { return null; }
        return sentenceSpanIndex.get(sentenceId);
    }

    // syncActivePlaybackBlock: compatibility label for T37 source guard; current implementation follows cue/unit when available.
    private void syncActivePlaybackCue(PlaybackCursor cursor) {
        String segmentId = segmentId(cursor);
        String nextBlockId = blockIdForPlaybackSegment(segmentId);
        PlaybackCue cue = cueForCursor(cursor).orElse(null);
        String nextUnitId = cue == null ? "" : cue.unitId();
        boolean unitChanged = !nextUnitId.equals(activePlaybackUnitId);
        if (!nextBlockId.equals(activePlaybackBlockId)) {
            activePlaybackBlockId = nextBlockId;
            ensureBlockRendered(nextBlockId);
            updateActiveReadingStyles();
            scrollToActiveReadingBlock(nextBlockId);
            updatePdfVisualReadingHighlight(nextBlockId, nextUnitId);
        }
        if (unitChanged) {
            activePlaybackUnitId = nextUnitId;
            updateActiveReadingStyles();
            sentenceForCueUnit(nextUnitId, nextBlockId).ifPresent(this::selectSentence);
            updatePdfVisualReadingHighlight(nextBlockId, nextUnitId);
        }
    }

    private Optional<PlaybackCue> cueForCursor(PlaybackCursor cursor) {
        PlaybackManifest manifest = viewModel.currentPlaybackManifestProperty().get();
        if (manifest == null || manifest.emptyManifest() || cursor == null) {
            return Optional.empty();
        }
        Optional<PlaybackCue> byPosition = manifest.cueAt(cursor.positionSeconds())
                .filter(cue -> cue.segmentId().equals(cursor.segmentId()));
        return byPosition.isPresent() ? byPosition : manifest.cueForSegment(cursor.segmentId());
    }

    private Optional<DocumentSentenceSpan> sentenceForCueUnit(String unitId, String blockId) {
        int unitIndex = unitIndex(unitId);
        if (unitIndex < 0 || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        return document == null ? Optional.empty() : document.blockById(blockId)
                .flatMap(block -> {
                    java.util.List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split(block);
                    return unitIndex < spans.size() ? Optional.of(spans.get(unitIndex)) : Optional.empty();
                });
    }

    private static int unitIndex(String unitId) {
        if (unitId == null) {
            return -1;
        }
        int marker = unitId.lastIndexOf("-U");
        if (marker < 0 || marker + 2 >= unitId.length()) {
            return -1;
        }
        try {
            return Math.max(-1, Integer.parseInt(unitId.substring(marker + 2)) - 1);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String segmentId(PlaybackCursor cursor) {
        return cursor == null ? "" : normalize(cursor.segmentId());
    }

    private String blockIdForPlaybackSegment(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return "";
        }
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        if (script == null || script.empty()) {
            return "";
        }
        return script.segments().stream()
                .filter(segment -> segment.id().equals(segmentId))
                .findFirst()
                .flatMap(this::firstSourceBlockId)
                .orElse("");
    }

    private Optional<String> firstSourceBlockId(NarrationSegment segment) {
        return segment.sourceBlockIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .findFirst();
    }

    private void ensureBlockRendered(String blockId) {
        if (blockId == null || blockId.isBlank() || blockNodes.containsKey(blockId)) {
            return;
        }
        ReadableDocument document = renderedDocument == null ? viewModel.currentDocumentProperty().get() : renderedDocument;
        if (shouldUsePdfVisualDocument(document)) {
            return;
        }
        if (document == null || !largeDocument(document)) {
            return;
        }
        blockIndex(document, blockId).ifPresent(index -> {
            renderedWindow = windowFor(document, index);
            renderWindow(document, renderedWindow);
        });
    }


    private void updateActiveReadingStyles() {
        boolean sentenceCueActive = activePlaybackUnitId != null && !activePlaybackUnitId.isBlank();
        for (Map.Entry<String, Node> entry : blockNodes.entrySet()) {
            boolean active = entry.getKey().equals(activePlaybackBlockId) && !sentenceCueActive;
            if (active && !entry.getValue().getStyleClass().contains("document-block-active-reading")) {
                entry.getValue().getStyleClass().add("document-block-active-reading");
            } else if (!active) {
                entry.getValue().getStyleClass().remove("document-block-active-reading");
            }
        }
    }

    private void scrollToActiveReadingBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return;
        }
        Node node = blockNodes.get(blockId);
        if (node != null) {
            scrollNodeNearReadingTop(node);
            return;
        }
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        if (shouldUsePdfVisualDocument(document)) {
            sourcePageForBlock(document, blockId).ifPresent(pdfVisualView::scrollToPage);
            return;
        }
        if (document == null || document.blocks().isEmpty()) {
            return;
        }
        int index = -1;
        for (int i = 0; i < document.blocks().size(); i++) {
            if (document.blocks().get(i).id().equals(blockId)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            double target = readingAnchor.scrollValueFor(index, document.blocks().size());
            Platform.runLater(() -> documentScroll.setVvalue(target));
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static Optional<Integer> sourcePageForBlock(ReadableDocument document, String blockId) {
        if (document == null || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        return document.blockById(blockId)
                .flatMap(block -> parsePositiveInt(block.metadata().getOrDefault("sourcePage", "")));
    }

    private void updatePdfVisualReadingHighlight(String blockId, String unitId) {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        if (!shouldUsePdfVisualDocument(document)) {
            pdfVisualView.clearTextHighlight();
            return;
        }
        if (blockId == null || blockId.isBlank()) {
            pdfVisualView.clearTextHighlight();
            return;
        }
        PdfVisualReadingProjection projection = pdfProjection(document);
        Optional<DocumentSentenceSpan> span = sentenceForCueUnit(unitId, blockId);
        if (span.isPresent()) {
            projection.targetForRange(span.get().range())
                    .map(PdfVisualTextTarget::highlight)
                    .ifPresentOrElse(this::showPdfPlaybackHighlight,
                            () -> projection.highlightForBlock(blockId)
                                    .ifPresentOrElse(this::showPdfPlaybackHighlight, pdfVisualView::clearTextHighlight));
            return;
        }
        projection.highlightForBlock(blockId)
                .ifPresentOrElse(this::showPdfPlaybackHighlight, pdfVisualView::clearTextHighlight);
    }

    private void showPdfPlaybackHighlight(PdfVisualTextHighlight highlight) {
        pdfVisualView.showTextHighlight(highlight);
        pdfVisualView.scrollToTextHighlight(highlight, pdfTextViewportTopOffset());
    }

    private PdfVisualReadingProjection pdfProjection(ReadableDocument document) {
        return viewModel.applicationServices().document().buildPdfVisualReadingProjection().build(document);
    }

    private void updatePdfPinnedSelection() {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        if (!shouldUsePdfVisualDocument(document)) {
            pdfVisualView.clearPinnedTextTarget();
            return;
        }
        PdfVisualReadingProjection projection = pdfProjection(document);
        DocumentTextRange range = viewModel.selectedDocumentTextRangeProperty().get();
        if (range != null) {
            projection.targetForRange(range).ifPresentOrElse(
                    pdfVisualView::showPinnedTextTarget,
                    () -> projection.targetForBlock(range.blockId()).ifPresentOrElse(
                            pdfVisualView::showPinnedTextTarget,
                            pdfVisualView::clearPinnedTextTarget));
            return;
        }
        String blockId = selectedBlockId.get();
        if (blockId == null || blockId.isBlank()) {
            pdfVisualView.clearPinnedTextTarget();
            return;
        }
        projection.targetForBlock(blockId).ifPresentOrElse(
                pdfVisualView::showPinnedTextTarget,
                pdfVisualView::clearPinnedTextTarget);
    }

    private void showPdfSearchHighlight(PdfVisualTextHighlight highlight) {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        if (!shouldUsePdfVisualDocument(document) || highlight == null || !highlight.available()) {
            return;
        }
        pdfVisualView.showTextHighlight(highlight);
        pdfVisualView.scrollToTextHighlight(highlight, pdfTextViewportTopOffset());
    }

    private Path pdfOcrCacheDirectory() {
        return viewModel.currentProjectDirectory()
                .map(directory -> directory.resolve(".docupodcast-cache").resolve("pdf-ocr"))
                .orElseGet(() -> Path.of(System.getProperty("java.io.tmpdir"), "docupodcast-studio", "pdf-ocr"));
    }

    private static Optional<Integer> parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private void clearSelectionFromBlankDocumentClick() {
        if (hasDocumentSelection() && canClearSelectionFromBlankDocumentClick()) {
            viewModel.clearSelectedDocumentBlock();
        }
    }

    private void clearSelectionFromWorkspacePointerPress(MouseEvent event) {
        if (event.getClickCount() != 1 || !hasDocumentSelection()
                || targetInsideDocumentBlock(event.getTarget())
                || targetInsideWorkspaceSideDock(event.getTarget())) {
            return;
        }
        if (isInteractiveControl(event.getTarget())) {
            return;
        }
        Platform.runLater(() -> {
            if (hasDocumentSelection()) {
                viewModel.clearSelectedDocumentBlock();
            }
        });
    }

    private static boolean isInteractiveControl(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof ButtonBase
                    || current instanceof ComboBoxBase
                    || current instanceof Spinner
                    || current instanceof Slider
                    || current instanceof ScrollBar) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean targetInsideWorkspaceSideDock(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof WorkspaceSideDock) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean targetInsideDocumentBlock(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("document-block")
                    || current.getStyleClass().contains("document-sentence")
                    || current.getStyleClass().contains("document-theatre-text-alias")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean hasDocumentSelection() {
        return !selectedBlockId.get().isBlank() || viewModel.selectedDocumentTextRangeProperty().get() != null;
    }

    private boolean canClearSelectionFromBlankDocumentClick() {
        PlaybackCursor cursor = viewModel.playbackCursorProperty().get();
        return cursor == null || cursor.stoppedState();
    }

    private String documentTitleWithExtension(ReadableDocument document) {
        if (document == null || document.sourcePath() == null || document.sourcePath().getFileName() == null) {
            return document == null ? "" : document.title();
        }
        String fileName = document.sourcePath().getFileName().toString();
        return fileName.isBlank() ? document.title() : fileName;
    }

    private Optional<Label> intervencionBadge(DocumentBlock block) {
        if (mode != DocumentWorkspaceMode.THEATRE_SCRIPT || block == null) {
            return Optional.empty();
        }
        String alias = intervencionByBlockId.getOrDefault(block.id(), "");
        if (alias.isBlank()) {
            return Optional.empty();
        }
        Label badge = new Label(com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreZigzagLayout.displayLabel(alias));
        badge.getStyleClass().add("document-theatre-text-alias");
        Tooltip.install(badge, new Tooltip(com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreZigzagLayout.displayLabel(alias) + " - inicio de fragmento textual"));
        return Optional.of(badge);
    }

    private Optional<Label> theatreReplicaBadge(DocumentBlock block) {
        if (mode != DocumentWorkspaceMode.THEATRE_SCRIPT || block == null) {
            return Optional.empty();
        }
        int index = blockIndexById.getOrDefault(block.id(), -1);
        if (index < 0) {
            return Optional.empty();
        }
        Label badge = new Label("Texto " + (index + 1));
        badge.getStyleClass().add("document-theatre-replica-id");
        Tooltip.install(badge, new Tooltip("Segmento de texto " + (index + 1) + " - parrafo del documento usado como referencia"));
        return Optional.of(badge);
    }

    private ContextMenu paragraphContextMenu(DocumentBlock block) {
        MenuItem copyText = new MenuItem("Copiar texto");
        copyText.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(block == null ? "" : block.text());
            Clipboard.getSystemClipboard().setContent(content);
            viewModel.updateStatusMessage("Texto del parrafo copiado.");
        });
        return new ContextMenu(copyText);
    }

    private String readableKind(DocumentBlock block) {
        return block.type().displayName();
    }

    private String blockMarker(DocumentBlock block) {
        if (block.metadata().containsKey("warning") || block.metadata().containsKey("diagnostic")) {
            return "⚠";
        }
        return switch (block.type()) {
            case TITLE, HEADING, SUBHEADING -> "◆";
            case IMAGE_NOTICE -> "▣";
            case TABLE_NOTICE -> "▦";
            case MATH_NOTICE -> "∑";
            case IGNORED, EMPTY -> "○";
            default -> block.narratable() ? "●" : "○";
        };
    }

    private String blockMarkerTooltip(DocumentBlock block) {
        if (block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.IMAGE_NOTICE
                || block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.TABLE_NOTICE
                || block.type() == com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType.MATH_NOTICE) {
            return "Bloque visual de la fuente documental. Se muestra en la hoja y solo entra a la secuencia visual si el usuario lo asocia.";
        }
        if (block.narratable()) {
            return "Bloque narrable. La voz, audio, emoción o imagen se guardarán como capa del proyecto, no dentro del Word.";
        }
        return "Bloque no narrable por defecto. Puedes reclasificarlo desde Acciones si debe leerse.";
    }

}
