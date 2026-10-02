package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoBackgroundMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.batch.ManageDocumentVideoBatchQueueUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.batch.VerifyBatchVideoOutputUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.batch.WriteDocumentVideoBatchReportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ProjectGrammarKind;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideTopicId;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectContainerPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListVoiceEngineOperationalStatesUseCase;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ActiveAudioJobDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.AudioEngineUnavailableDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.DeleteAudioChunksDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.DocumentImportProgressDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ExportAiResourcesResultDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.IncompleteAudioExportDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ProjectInitialSourceDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ProjectNameDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ProjectSourceCopyNoticeDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ProjectSourceReplacementDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.RestartActiveProcessingDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.ReprocessCompleteReadingDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.UnsavedChangesDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandDispatchResult;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandDispatcher;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandRegistry;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.CommandAvailabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.CommandAuditInspector;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentWorkspaceView;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentWorkspaceMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.TechnicalProblemDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.examples.ExampleProjectDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportCenterCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportCenterContext;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportCenterDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportCenterSelection;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportCenterState;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportExecutionMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.PreparedExportIntent;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ProjectExportEligibilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DiagnosticUserDecisionFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentAudioAction;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentExportReadinessSnapshot;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentReadingReadinessSnapshot;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.ExampleProjectCreationWorkflow;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.GrammarWorkflowCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.PdfNarratablePreparationCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.TheatrePackageRefreshCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.WordSemanticPreparationCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.guide.GuideDialog;
import com.marcosmoreiradev.docupodcaststudio.application.document.SourceDocumentRequirementException;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotificationLevel;
import com.marcosmoreiradev.docupodcaststudio.application.media.administration.CapabilityAdministrationService;
import com.marcosmoreiradev.docupodcaststudio.application.media.administration.CapabilityRequirement;
import com.marcosmoreiradev.docupodcaststudio.presentation.settings.SettingsDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.settings.SettingsSupportActions;
import com.marcosmoreiradev.docupodcaststudio.presentation.status.StatusBarView;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreMapExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatrePortionExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatrePortionExportOptionsDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreImageGenerationWorkspaceView;
import com.marcosmoreiradev.docupodcaststudio.presentation.ribbon.RibbonStateCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.ribbon.RibbonView;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.LongProcessOverlayView;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptionsDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportProgressCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.batch.DocumentVideoBatchExecutionPort;
import com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceLibraryWorkspaceView;
import com.marcosmoreiradev.docupodcaststudio.presentation.welcome.RecentProjectEntry;
import com.marcosmoreiradev.docupodcaststudio.presentation.welcome.RecentProjectsStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.welcome.WelcomeWorkspaceView;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceDescriptorCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceRouteResolver;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceViewRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;
import com.marcosmoreiradev.docupodcaststudio.application.batch.DocumentVideoBatchWorkspaceRepository;

/** Main desktop shell for the onboarding build. */
public final class DocuPodcastShellView extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DocuPodcastShellView.class);
    private final DocuPodcastShellViewModel viewModel;
    private final StackPane workspaceHost = new StackPane();
    private final StackPane projectLoadingOverlay = new StackPane();
    private final javafx.beans.property.BooleanProperty openingProject = new javafx.beans.property.SimpleBooleanProperty(false);
    private long workspaceActivationSequence;
    private final WorkspaceDescriptorCatalog workspaceCatalog = WorkspaceDescriptorCatalog.official();
    private final WorkspaceRouteResolver workspaceRouteResolver = new WorkspaceRouteResolver(workspaceCatalog);
    private final ProjectContainerPathPolicy projectContainerPathPolicy = new ProjectContainerPathPolicy();
    private final ExampleProjectCreationWorkflow exampleProjectCreationWorkflow = new ExampleProjectCreationWorkflow();
    private final WorkspaceViewRegistry workspaceRegistry = new WorkspaceViewRegistry(workspaceCatalog);
    private final ExceptionAlertPresenter alertPresenter = new ExceptionAlertPresenter();
    private final ProjectNameDialog projectNameDialog = new ProjectNameDialog();
    private final RecentProjectsStore recentProjectsStore;
    private final ObservableList<RecentProjectEntry> recentProjects = FXCollections.observableArrayList();
    private final ProjectInitialSourceDialog projectInitialSourceDialog = new ProjectInitialSourceDialog();
    private final ProjectSourceCopyNoticeDialog projectSourceCopyNoticeDialog = new ProjectSourceCopyNoticeDialog();
    private final ProjectSourceReplacementDialog projectSourceReplacementDialog = new ProjectSourceReplacementDialog();
    private final ActiveAudioJobDialog activeAudioJobDialog = new ActiveAudioJobDialog();
    private static final String AUDIO_ENGINE_UNAVAILABLE_TITLE = "Motor de voz no disponible";
    private final AudioEngineUnavailableDialog audioEngineUnavailableDialog = new AudioEngineUnavailableDialog();
    private final IncompleteAudioExportDialog incompleteAudioExportDialog = new IncompleteAudioExportDialog();
    private final DeleteAudioChunksDialog deleteAudioChunksDialog = new DeleteAudioChunksDialog();
    private final RestartActiveProcessingDialog restartActiveProcessingDialog =
            new RestartActiveProcessingDialog();
    private final ReprocessCompleteReadingDialog reprocessCompleteReadingDialog =
            new ReprocessCompleteReadingDialog();
    private final UnsavedChangesDialog unsavedChangesDialog = new UnsavedChangesDialog();
    private final ExportAiResourcesResultDialog exportAiResourcesResultDialog = new ExportAiResourcesResultDialog();
    private final CapabilityAdministrationService capabilityAdministration;
    private final SettingsDialog settingsDialog;
    private final FxBackgroundTaskRunner backgroundTaskRunner = new FxBackgroundTaskRunner();
    private final VideoExportOptionsDialog videoExportOptionsDialog = new VideoExportOptionsDialog();
    private final TheatrePortionExportOptionsDialog theatrePortionExportOptionsDialog = new TheatrePortionExportOptionsDialog();
    private final VideoExportProgressCoordinator videoExportProgressCoordinator = new VideoExportProgressCoordinator();
    private final ExportCenterCoordinator exportCenterCoordinator = new ExportCenterCoordinator();
    private final ExportCenterDialog exportCenterDialog = new ExportCenterDialog();
    private final ProjectExportEligibilityPolicy exportEligibilityPolicy = new ProjectExportEligibilityPolicy();
    private final GrammarWorkflowCoordinator grammarWorkflow;
    private final TheatrePackageRefreshCoordinator theatrePackageRefresh;
    private final PdfNarratablePreparationCoordinator pdfNarratablePreparation;
    private final WordSemanticPreparationCoordinator wordSemanticPreparation;
    private static final String PREF_HIDE_SOURCE_COPY_NOTICE = "hideProjectSourceCopyNotice";
    private String lastAudioFailureKey = "";
    private final DocumentVideoBatchWorkspaceRepository batchRepository;
    private final ManageDocumentVideoBatchQueueUseCase batchQueue;
    private final VerifyBatchVideoOutputUseCase batchVideoVerifier = new VerifyBatchVideoOutputUseCase();
    private final WriteDocumentVideoBatchReportUseCase batchReportWriter =
            new WriteDocumentVideoBatchReportUseCase();
    private BatchExecutionSession activeBatchExecution;
    private Window expressNotificationOwner;
    private boolean expressOpenedChild;

    private final AppCommandRegistry commandRegistry = AppCommandRegistry.official();
    private final CommandAvailabilityPolicy commandAvailabilityPolicy = new CommandAvailabilityPolicy();
    private final AppCommandDispatcher commandDispatcher = new AppCommandDispatcher(commandRegistry);
    private final Preferences preferences = Preferences.userNodeForPackage(DocuPodcastShellView.class);
    private final RibbonStateCoordinator ribbonStateCoordinator = new RibbonStateCoordinator(preferences);
    private final BooleanProperty processOverlayExpanded = new SimpleBooleanProperty(true);
    private final BooleanProperty processOverlayMaximized = new SimpleBooleanProperty(false);
    private volatile Task<?> activeFinalAudioExportTask;

    public DocuPodcastShellView(DocuPodcastShellViewModel viewModel) {
        batchRepository = viewModel.projectWorkspace().project().batch().repository();
        batchQueue = viewModel.projectWorkspace().project().batch().queue();
        this.viewModel = viewModel;
        this.settingsDialog = new SettingsDialog(viewModel.administrationWorkspace().capabilities());
        this.capabilityAdministration = viewModel.administrationWorkspace().capabilities();
        this.recentProjectsStore = new RecentProjectsStore((projectFile, storedType) -> {
            try {
                return new ProjectModePolicy().resolve(viewModel.projectWorkspace().project().openProject().open(projectFile)).displayName();
            } catch (IOException | RuntimeException ex) {
                return storedType;
            }
        });
        this.grammarWorkflow = new GrammarWorkflowCoordinator(viewModel, backgroundTaskRunner, alertPresenter, this::owner);
        var theatrePackages = Objects.requireNonNull(viewModel.projectWorkspace().theatrePackage(),
                "theatre package application services");
        this.theatrePackageRefresh = new TheatrePackageRefreshCoordinator(
                viewModel,
                theatrePackages.refresh(),
                theatrePackages.importState(),
                backgroundTaskRunner,
                alertPresenter,
                this::owner);
        this.pdfNarratablePreparation = new PdfNarratablePreparationCoordinator(viewModel, backgroundTaskRunner, alertPresenter, this::owner);
        this.wordSemanticPreparation = new WordSemanticPreparationCoordinator(
                viewModel, backgroundTaskRunner);
        recentProjects.setAll(recentProjectsStore.load());
        getStyleClass().add("app-root");
        registerCommandHandlers();
        initialiseWorkspaces();
        workspaceHost.disableProperty().bind(viewModel.theatreRefreshRunningProperty().or(openingProject));
        setTop(buildTop());
        setCenter(buildCenter());
        setBottom(new StatusBarView(
                viewModel.statusMessageProperty(),
                viewModel.readingFontSizeProperty(),
                viewModel::decreaseReadingFontSize,
                viewModel::resetReadingFontSize,
                viewModel::increaseReadingFontSize,
                viewModel::setReadingFontSize,
                viewModel.activeAudioJobStatusProperty(),
                viewModel.pdfPreparationProgressProperty(),
                viewModel.localDocumentAnalysisRunningProperty(),
                viewModel.currentDocumentProperty(),
                viewModel.currentPreparedPdfSourceProperty(),
                Bindings.createStringBinding(
                        () -> viewModel.documentSelectionValidProperty().get()
                                ? (viewModel.selectedDocumentBlockIdProperty().get().isBlank()
                                ? "__PDF_SELECTION__"
                                : viewModel.selectedDocumentBlockIdProperty().get())
                                : "",
                        viewModel.documentSelectionValidProperty(),
                        viewModel.selectedDocumentBlockIdProperty()),
                viewModel.pdfVisualDocumentProgressProperty(),
                processOverlayExpanded,
                viewModel.audioJobRunningProperty(),
                viewModel.currentDocumentProperty().isNotNull()
                        .or(viewModel.currentPreparedPdfSourceProperty().isNotNull()),
                viewModel.documentProcessingScopeProperty(),
                viewModel.documentProcessingIntervalSupportedProperty(),
                viewModel.documentProcessingIntervalValidProperty(),
                viewModel.managedAudioChunksAvailableProperty(),
                viewModel.fullDocumentReadingReadinessProperty(),
                this::handleGenerateChunksFromStatusBar,
                this::handleGenerateSelectedChunkFromStatusBar,
                this::handleProcessIntervalFromStatusBar,
                viewModel::resumeMostRecentRecoverableAudioJob,
                this::cancelCurrentExportOrAudioOperation,
                this::handleDeleteAllAudioChunksFromStatusBar,
                this::handleOpenOcrSettings));
        viewModel.activeWorkspaceProperty().addListener((obs, oldValue, newValue) -> activate(newValue));
        viewModel.activeAudioJobStatusProperty().addListener((obs, oldValue, newValue) -> {
            showAudioGenerationFailureIfNeeded(oldValue, newValue);
            handleBatchAudioStatus(newValue);
        });
        activate(viewModel.activeWorkspaceProperty().get());
    }

    public void handleCloseRequest(WindowEvent event) {
        if (viewModel.theatreRefreshRunningProperty().get()) {
            event.consume();
            alertPresenter.show(UserNotification.warning("Actualización de obra en curso",
                    "Espera a que termine la transacción teatral antes de cerrar DocuPodcast."), owner());
            return;
        }
        if (!confirmDiscardOrSaveIfNeeded()) {
            event.consume();
        }
    }

    public void runStartupDependencyPreflight() {
        backgroundTaskRunner.start("engine-readiness-preflight", () -> {
            List<String> unavailable = capabilityAdministration.components().stream()
                    .filter(component -> {
                        try {
                            return !capabilityAdministration.inspect(new CapabilityRequirement(
                                    component.capability(), component.engineId(), null, null, Map.of())).ready();
                        }
                        catch (RuntimeException failure) { return true; }
                    })
                    .map(com.marcosmoreiradev.docupodcaststudio.application.media.administration
                            .ManagedComponentDescriptor::displayName)
                    .toList();
            if (!unavailable.isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    // Startup inspection can finish after the user has already opened a
                    // project or started a real operation. It must not overwrite the
                    // current workflow with a stale welcome/preflight diagnosis.
                    if (!viewModel.projectOpenProperty().get()) {
                        viewModel.updateStatusMessage(
                                "Capacidades no disponibles: " + String.join(", ", unavailable)
                                        + ". Revisa Configuración > Motores y dependencias.");
                    }
                });
            }
        });
    }

    private void initialiseWorkspaces() {
        workspaceRegistry
                .register(WorkspaceKind.WELCOME_HOME, () -> new WelcomeWorkspaceView(
                        () -> dispatchCommand(AppCommandId.OPEN_SOURCE_DOCUMENT),
                        () -> dispatchCommand(AppCommandId.OPEN_PROJECT),
                        () -> dispatchCommand(AppCommandId.NEW_PROJECT),
                        this::handleOpenTechnicalProblemExpress,
                        () -> dispatchCommand(AppCommandId.CREATE_DOCUMENT_VIDEO_BATCH),
                        () -> dispatchCommand(AppCommandId.OPEN_EXAMPLE_PROJECT),
                        this::handleOpenFirstUseSetup,
                        () -> dispatchCommand(AppCommandId.OPEN_GUIDE),
                        recentProjects,
                        this::handleOpenRecentProject))
                .register(WorkspaceKind.DOCUMENT_READER, () -> new DocumentWorkspaceView(viewModel, this::handleSaveProject,
                        () -> ensureProjectSavedForDocumentAudio("preparar o reproducir la lectura desde la barra flotante"),
                        () -> confirmAudioEngineReadyForDocumentAction(false),
                        DocumentWorkspaceMode.READING,
                        this::handleNarrateFromPdfTarget,
                        this::handleDocumentAudioAction))
                .registerAlias(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION, WorkspaceKind.DOCUMENT_READER)
                .register(WorkspaceKind.THEATRE_SCRIPT, () -> new DocumentWorkspaceView(viewModel, this::handleSaveProject,
                        () -> ensureProjectSavedForDocumentAudio("preparar o reproducir la lectura desde el guión teatral"),
                        () -> confirmAudioEngineReadyForDocumentAction(false),
                        DocumentWorkspaceMode.THEATRE_SCRIPT,
                        this::handleNarrateFromPdfTarget,
                        this::handleDocumentAudioAction))
                .register(WorkspaceKind.VOICE_LIBRARY, () -> new VoiceLibraryWorkspaceView(viewModel))
                .register(WorkspaceKind.THEATRE_IMAGE_GENERATION, () -> new TheatreImageGenerationWorkspaceView(viewModel));
        workspaceRegistry.validateRegistrations();
    }

    private Node buildTop() {
        VBox top = new VBox();
        RibbonView ribbon = new RibbonView(viewModel, this::dispatchCommand, ribbonStateCoordinator.collapsedProperty());
        ribbon.visibleProperty().bind(viewModel.projectOpenProperty());
        ribbon.managedProperty().bind(viewModel.projectOpenProperty());
        top.getChildren().add(buildMenuBar());
        top.getChildren().add(ribbon);
        return top;
    }

    private Node buildCenter() {
        LongProcessOverlayView processOverlay = new LongProcessOverlayView(
                viewModel, processOverlayExpanded,
                this::cancelLocalDocumentAnalysis,
                pdfNarratablePreparation::cancelActivePreparationSession,
                processOverlayMaximized);
        installWorkspaceHostClip();
        var loadingProgress = com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls.progressIndicator();
        loadingProgress.getStyleClass().add("project-loading-indicator");
        loadingProgress.setFocusTraversable(false);
        loadingProgress.setMinSize(72, 72);
        loadingProgress.setPrefSize(72, 72);
        loadingProgress.setMaxSize(72, 72);
        Label loadingLabel = new Label("Cargando proyecto…");
        loadingLabel.getStyleClass().add("project-loading-label");
        VBox loadingMessage = new VBox(18, loadingProgress, loadingLabel);
        loadingMessage.getStyleClass().add("project-loading-message");
        loadingMessage.setAlignment(Pos.CENTER);
        projectLoadingOverlay.getChildren().setAll(loadingMessage);
        projectLoadingOverlay.setStyle("-fx-background-color: rgba(255,255,255,0.88);");
        projectLoadingOverlay.setVisible(false);
        projectLoadingOverlay.setManaged(false);
        StackPane center = new StackPane(workspaceHost, processOverlay, projectLoadingOverlay);
        StackPane.setAlignment(processOverlay, Pos.CENTER_RIGHT);
        var overlayWidth = Bindings.when(processOverlayMaximized)
                .then(center.widthProperty())
                .otherwise(center.widthProperty().multiply(0.5));
        processOverlay.prefWidthProperty().bind(overlayWidth);
        processOverlay.maxWidthProperty().bind(overlayWidth);
        processOverlay.minHeightProperty().bind(center.heightProperty());
        processOverlay.prefHeightProperty().bind(center.heightProperty());
        return center;
    }

    private void installWorkspaceHostClip() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(workspaceHost.widthProperty());
        clip.heightProperty().bind(workspaceHost.heightProperty());
        workspaceHost.setClip(clip);
    }

    public AppCommandDispatchResult dispatchCommand(AppCommandId commandId) {
        return commandDispatcher.dispatch(commandId);
    }

    private void registerCommandHandlers() {
        commandDispatcher
                .register(AppCommandId.SHOW_WELCOME, viewModel::showWelcome)
                .register(AppCommandId.OPEN_DOCUMENT_READER, () -> viewModel.showDocumentWorkspace("Documento activo."))
                .register(AppCommandId.OPEN_THEATRE_SCRIPT, () -> viewModel.showTheatreScriptWorkspace("Guión teatral activo."))
                .register(AppCommandId.OPEN_THEATRE_IMAGE_GENERATION, viewModel::showTheatreImageGenerationWorkspace)
                .register(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION, viewModel::openNarrativeVideoProduction)
                .register(AppCommandId.EXIT_APPLICATION, this::requestWindowClose)
                .register(AppCommandId.CLEAR_SELECTION, viewModel::clearSelectedDocumentBlock)
                .register(AppCommandId.NEW_PROJECT, this::handleNewProject)
                .register(AppCommandId.CREATE_DOCUMENT_VIDEO_BATCH,
                        () -> { if (!confirmDiscardOrSaveIfNeeded()) return;
                            expressOpenedChild = false;
                            com.marcosmoreiradev.docupodcaststudio.presentation.batch.DocumentVideoBatchWindow.show(
                                owner(), this::handleOpenRecentProject, batchExecutionPort(),
                                this::returnFromDocumentVideoBatch, viewModel.projectWorkspace().project().batch()); })
                .register(AppCommandId.OPEN_PROJECT, this::handleOpenProject)
                .register(AppCommandId.SAVE_PROJECT, this::handleSaveProject)
                .register(AppCommandId.SAVE_PROJECT_AS, this::handleSaveProjectAs)
                .register(AppCommandId.CLOSE_PROJECT, this::handleCloseProject)
                .register(AppCommandId.OPEN_PROJECT_FOLDER, this::handleOpenProjectFolder)
                .register(AppCommandId.OPEN_SOURCE_DOCUMENT, this::handleImportWord)
                .register(AppCommandId.IMPORT_THEATRE_GRAMMAR, this::handleImportTheatreGrammar)
                .register(AppCommandId.REFRESH_THEATRE_PACKAGE, theatrePackageRefresh::start)
                .register(AppCommandId.EXPORT_THEATRE_GRAMMAR_TEMPLATE, this::handleExportTheatreGrammarTemplate)
                .register(AppCommandId.IMPORT_NARRATIVE_VIDEO_GRAMMAR, this::handleImportNarrativeVideoGrammar)
                .register(AppCommandId.EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE, this::handleExportNarrativeVideoGrammarTemplate)
                .register(AppCommandId.OPEN_EXAMPLE_PROJECT, this::handleOpenExampleProject)
                .register(AppCommandId.REFRESH_SOURCE_DOCUMENT, this::handleRefreshSourceDocument)
                .register(AppCommandId.OPEN_SOURCE_DOCUMENT_LOCATION, this::handleOpenSourceDocumentLocation)
                .register(AppCommandId.PREPARE_DOCUMENT_READING,
                        () -> wordSemanticPreparation.prepareThenRun(
                                () -> pdfNarratablePreparation.prepareThenRun(
                                        viewModel::buildNarrationScriptFromDocument)))
                .register(AppCommandId.PREPARE_TECHNICAL_PROBLEM, this::handlePrepareTechnicalProblem)
                .register(AppCommandId.LISTEN_DOCUMENT, this::handleListenDocument)
                .register(AppCommandId.PLAY_SELECTION, this::handlePlaySelection)
                .register(AppCommandId.PAUSE_PLAYBACK, viewModel::pausePlayback)
                .register(AppCommandId.RESUME_PLAYBACK, viewModel::resumePlayback)
                .register(AppCommandId.STOP_PLAYBACK, viewModel::stopPlayback)
                .register(AppCommandId.ASSIGN_AI_VOICE_TO_SELECTION, viewModel::prepareAiVoiceForSelectedText)
                .register(AppCommandId.ASSIGN_HUMAN_RECORDING_TO_SELECTION, viewModel::prepareHumanVoiceForSelectedText)
                .register(AppCommandId.IMPORT_VOICE_SAMPLE, this::handleImportVoiceSample)
                .register(AppCommandId.OPEN_VOICE_LIBRARY, viewModel::showVoiceLibraryWorkspace)
                .register(AppCommandId.IMPORT_AUDIO_FOR_SELECTION, this::handleImportAudioForSelection)
                .register(AppCommandId.EXTRACT_VIDEO_AUDIO_FOR_SELECTION, this::handleExtractVideoAudioForSelection)
                .register(AppCommandId.IMPORT_IMAGE_FOR_SELECTION, this::handleImportStoryboardImage)
                .register(AppCommandId.IMPORT_BRIDGE_IMAGE_FOR_SELECTION, this::handleImportBridgeImageForSelection)
                .register(AppCommandId.ASSOCIATE_IMAGE_TO_SELECTION, viewModel::bindLastStoryboardImageToSelectedSegment)
                .register(AppCommandId.CREATE_STORYBOARD, viewModel::buildStoryboardFromScript)
                .register(AppCommandId.GENERATE_AUDIO, this::handleGenerateAudio)
                .register(AppCommandId.CANCEL_AUDIO_JOB, viewModel::cancelActiveAudioJob)
                .register(AppCommandId.EXPORT_PROJECT_BUNDLE, this::handleExportProjectBundle)
                .register(AppCommandId.EXPORT_PODCAST_WAV, () -> handleOpenExportCenter(AppCommandId.EXPORT_PODCAST_WAV))
                .register(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, () -> handleOpenExportCenter(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO))
                .register(AppCommandId.EXPORT_DIAGNOSTIC_REPORT, this::handleExportDiagnosticReport)
                .register(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE, () -> handleOpenExportCenter(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE))
                .register(AppCommandId.EXPORT_THEATRE_WORK, () -> handleOpenExportCenter(AppCommandId.EXPORT_THEATRE_WORK))
                .register(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW, () -> handleOpenExportCenter(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW))
                .register(AppCommandId.EXPORT_THEATRE_PORTION, () -> handleOpenExportCenter(AppCommandId.EXPORT_THEATRE_PORTION))
                .register(AppCommandId.OPEN_EXPORT_CENTER, this::handleOpenExportCenter)
                .register(AppCommandId.OPEN_EXPORTS_FOLDER, this::handleOpenExportsFolder)
                .register(AppCommandId.TOGGLE_FULLSCREEN, this::handleToggleFullScreen)
                .register(AppCommandId.TOGGLE_RIBBON_COLLAPSED, ribbonStateCoordinator::toggleCollapsed)
                .register(AppCommandId.TOGGLE_RIGHT_RAIL, viewModel::toggleDocumentRightRail)
                .register(AppCommandId.TOGGLE_DOCUMENT_PLAYBAR_DOCK, viewModel::toggleDocumentPlaybarDocked)
                .register(AppCommandId.OPEN_SETTINGS, this::handleOpenSettings)
                .register(AppCommandId.OPEN_GUIDE, this::handleOpenGuide)
                .register(AppCommandId.OPEN_ABOUT, () -> handleOpenGuideTopic(GuideTopicId.GLOSSARY))
                .register(AppCommandId.OPEN_WORD_GUIDE, () -> handleOpenGuideTopic(GuideTopicId.IMPORT_WORD_NOTES))
                .register(AppCommandId.INSPECT_PROJECT_INTEGRITY, this::handleInspectProjectIntegrity)
                .register(AppCommandId.INSPECT_EXPORT_READINESS, this::handleInspectExportReadiness)
                .register(AppCommandId.EXPORT_AI_RESOURCES, this::handleExportAiResources);
        CommandAuditInspector.requireNoVisibleCommandGaps(commandRegistry, commandDispatcher);
    }


    private void handleInspectProjectIntegrity() {
        viewModel.inspectProjectIntegrityDecision().ifPresentOrElse(
                decision -> alertPresenter.showDecision(decision, owner()),
                () -> alertPresenter.show(UserNotification.success("Proyecto íntegro", "No se encontraron advertencias de integridad en el proyecto actual."), owner()));
    }

    private MenuItem commandItem(AppCommandId commandId) {
        MenuItem item = new MenuItem(commandRegistry.descriptor(commandId).label());
        item.disableProperty().bind(commandAvailabilityPolicy.disabledBinding(commandId, viewModel));
        item.visibleProperty().bind(commandAvailabilityPolicy.visibleBinding(commandId, viewModel));
        item.setOnAction(event -> dispatchCommand(commandId));
        return item;
    }

    private void requestWindowClose() {
        Window window = getScene() == null ? null : getScene().getWindow();
        if (window == null) {
            return;
        }
        WindowEvent closeEvent = new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST);
        window.fireEvent(closeEvent);
        if (!closeEvent.isConsumed()) {
            window.hide();
        }
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = StudioNavigationControls.menuBar();
        menuBar.getStyleClass().add("app-menu-bar");

        Menu archivo = new Menu("Archivo");
        MenuItem nuevo = commandItem(AppCommandId.NEW_PROJECT);
        MenuItem abrirProyecto = commandItem(AppCommandId.OPEN_PROJECT);
        Menu abrirRecientes = recentProjectsMenu();
        MenuItem salir = commandItem(AppCommandId.EXIT_APPLICATION);
        archivo.getItems().addAll(nuevo, abrirProyecto, abrirRecientes, new SeparatorMenuItem(), salir);

        Menu proyecto = new Menu("Proyecto");
        MenuItem guardar = commandItem(AppCommandId.SAVE_PROJECT);
        guardar.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        MenuItem guardarComo = commandItem(AppCommandId.SAVE_PROJECT_AS);
        MenuItem cerrarProyecto = commandItem(AppCommandId.CLOSE_PROJECT);
        MenuItem abrirCarpeta = commandItem(AppCommandId.OPEN_PROJECT_FOLDER);
        proyecto.getItems().addAll(guardar, guardarComo, cerrarProyecto, new SeparatorMenuItem(), abrirCarpeta);

        Menu fuenteDocumental = new Menu("Fuente documental");
        MenuItem abrirDocumento = commandItem(AppCommandId.OPEN_SOURCE_DOCUMENT);
        MenuItem refrescarDocumento = commandItem(AppCommandId.REFRESH_SOURCE_DOCUMENT);
        MenuItem abrirUbicacionFuente = commandItem(AppCommandId.OPEN_SOURCE_DOCUMENT_LOCATION);
        MenuItem prepararLectura = commandItem(AppCommandId.PREPARE_DOCUMENT_READING);
        MenuItem prepararProblema = commandItem(AppCommandId.PREPARE_TECHNICAL_PROBLEM);
        fuenteDocumental.getItems().addAll(abrirDocumento, refrescarDocumento, abrirUbicacionFuente, new SeparatorMenuItem(), prepararLectura, prepararProblema);

        Menu ejemplos = new Menu("Ejemplos");
        MenuItem abrirEjemplo = commandItem(AppCommandId.OPEN_EXAMPLE_PROJECT);
        ejemplos.getItems().addAll(abrirEjemplo);

        Menu ver = new Menu("Ver");
        MenuItem pantallaCompleta = commandItem(AppCommandId.TOGGLE_FULLSCREEN);
        MenuItem cinta = commandItem(AppCommandId.TOGGLE_RIBBON_COLLAPSED);
        MenuItem railDerecho = commandItem(AppCommandId.TOGGLE_RIGHT_RAIL);
        ver.getItems().addAll(pantallaCompleta, cinta, railDerecho);

        Menu lectura = new Menu("Lectura");
        MenuItem escucharDocumento = commandItem(AppCommandId.LISTEN_DOCUMENT);
        MenuItem reproducirSeleccion = commandItem(AppCommandId.PLAY_SELECTION);
        MenuItem pausar = commandItem(AppCommandId.PAUSE_PLAYBACK);
        MenuItem reanudar = commandItem(AppCommandId.RESUME_PLAYBACK);
        MenuItem detener = commandItem(AppCommandId.STOP_PLAYBACK);
        lectura.getItems().addAll(escucharDocumento, reproducirSeleccion, new SeparatorMenuItem(), pausar, reanudar, detener);

        Menu estudio = new Menu("Estudio");
        estudio.getItems().addAll(
                commandItem(AppCommandId.OPEN_DOCUMENT_READER),
                commandItem(AppCommandId.PREPARE_DOCUMENT_READING),
                commandItem(AppCommandId.PREPARE_TECHNICAL_PROBLEM),
                commandItem(AppCommandId.GENERATE_AUDIO),
                new SeparatorMenuItem(),
                commandItem(AppCommandId.OPEN_VOICE_LIBRARY));
        bindModeMenu(estudio, ProjectMode.DOCUMENTARY_STUDIO);

        Menu videoNarrativo = new Menu("Video narrativo");
        videoNarrativo.getItems().addAll(
                commandItem(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION),
                new SeparatorMenuItem(),
                commandItem(AppCommandId.IMPORT_NARRATIVE_VIDEO_GRAMMAR),
                commandItem(AppCommandId.EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE));
        bindModeMenu(videoNarrativo, ProjectMode.NARRATIVE_VIDEO);

        Menu teatro = new Menu("Teatro");
        MenuItem guionTeatral = commandItem(AppCommandId.OPEN_THEATRE_SCRIPT);
        MenuItem generacionIaTeatral = commandItem(AppCommandId.OPEN_THEATRE_IMAGE_GENERATION);
        MenuItem refrescarObra = commandItem(AppCommandId.REFRESH_THEATRE_PACKAGE);
        teatro.getItems().addAll(guionTeatral, generacionIaTeatral, new SeparatorMenuItem(), refrescarObra);
        bindModeMenu(teatro, ProjectMode.THEATRE_PRODUCTION);

        Menu exportar = new Menu("Exportar");
        MenuItem centroExportacion = commandItem(AppCommandId.OPEN_EXPORT_CENTER);
        MenuItem exportarPodcast = commandItem(AppCommandId.EXPORT_PODCAST_WAV);
        MenuItem exportarDocumental = commandItem(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO);
        MenuItem exportarStoryboard = commandItem(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE);
        MenuItem exportarObra = commandItem(AppCommandId.EXPORT_THEATRE_WORK);
        MenuItem exportarMapaTeatral = commandItem(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW);
        MenuItem exportarPorcionTeatral = commandItem(AppCommandId.EXPORT_THEATRE_PORTION);
        MenuItem estadoExportacion = commandItem(AppCommandId.INSPECT_EXPORT_READINESS);
        MenuItem abrirExportaciones = commandItem(AppCommandId.OPEN_EXPORTS_FOLDER);
        exportar.getItems().addAll(centroExportacion, new SeparatorMenuItem(),
                exportarPodcast, exportarDocumental, exportarStoryboard, exportarObra,
                exportarMapaTeatral, exportarPorcionTeatral,
                new SeparatorMenuItem(), estadoExportacion, abrirExportaciones);

        Menu configuracion = new Menu("Configuración");
        MenuItem abrirConfiguracion = commandItem(AppCommandId.OPEN_SETTINGS);
        configuracion.getItems().addAll(abrirConfiguracion);

        Menu ayuda = new Menu("Ayuda");
        MenuItem guia = commandItem(AppCommandId.OPEN_GUIDE);
        Menu soporteAvanzado = new Menu("Soporte avanzado");
        soporteAvanzado.getItems().addAll(
                commandItem(AppCommandId.EXPORT_PROJECT_BUNDLE),
                commandItem(AppCommandId.EXPORT_DIAGNOSTIC_REPORT),
                commandItem(AppCommandId.INSPECT_PROJECT_INTEGRITY));
        MenuItem acerca = commandItem(AppCommandId.OPEN_ABOUT);
        ayuda.getItems().addAll(guia, soporteAvanzado, acerca);

        menuBar.getMenus().addAll(archivo, proyecto, fuenteDocumental, ejemplos, ver, lectura,
                estudio, videoNarrativo, teatro, exportar, configuracion, ayuda);
        return menuBar;
    }

    private Menu recentProjectsMenu() {
        Menu menu = new Menu("Abrir recientes");
        menu.disableProperty().bind(Bindings.isEmpty(recentProjects));
        recentProjects.addListener((javafx.collections.ListChangeListener<RecentProjectEntry>) change -> rebuildRecentProjectsMenu(menu));
        rebuildRecentProjectsMenu(menu);
        return menu;
    }

    private void rebuildRecentProjectsMenu(Menu menu) {
        menu.getItems().clear();
        if (recentProjects.isEmpty()) {
            MenuItem empty = new MenuItem("Sin proyectos recientes");
            empty.setDisable(true);
            menu.getItems().add(empty);
            return;
        }
        for (RecentProjectEntry entry : recentProjects) {
            MenuItem item = new MenuItem(entry.displayName() + " (" + entry.typeLabel() + ")");
            item.setOnAction(event -> handleOpenRecentProject(entry.projectFile()));
            menu.getItems().add(item);
        }
    }

    private void bindModeMenu(Menu menu, ProjectMode mode) {
        menu.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> viewModel.projectOpenProperty().get()
                        && viewModel.currentProjectModeProperty().get() == mode,
                viewModel.projectOpenProperty(),
                viewModel.currentProjectModeProperty()));
    }



    public void handleOpenProjectFolder() {
        Path projectFile = viewModel.currentProjectFile().orElse(null);
        if (projectFile == null || projectFile.getParent() == null) {
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("El sistema no permite abrir carpetas desde la aplicación.");
            }
            Desktop.getDesktop().open(projectFile.toAbsolutePath().normalize().getParent().toFile());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo abrir la carpeta del proyecto", ex);
        }
    }


    private void handleRefreshSourceDocument() {
        viewModel.refreshSourceDocumentDecision().ifPresent(decision -> alertPresenter.showDecision(decision, owner()));
    }

    private void handleInspectExportReadiness() {
        handleInspectExportReadiness(owner());
    }

    private void handleInspectExportReadiness(Window dialogOwner) {
        viewModel.inspectExportReadinessDecision().ifPresentOrElse(
                decision -> alertPresenter.showDecision(decision, dialogOwner),
                () -> alertPresenter.showDecision(
                        UserVisibleDecision.informationDialog(
                                "Todo está bien",
                                "No se detectaron bloqueos críticos en las salidas disponibles."),
                        dialogOwner));
    }

    private void handleOpenExportCenter() {
        handleOpenExportCenter(null);
    }

    private void handleOpenExportCenter(AppCommandId preselectedCommand) {
        ProjectExportEligibilityPolicy.ExportEligibility eligibility =
                exportEligibilityPolicy.evaluate(
                        viewModel.currentDocumentSource().orElse(null),
                        viewModel.currentDocumentContentProjection()
                                .filter(projection -> !projection.items().isEmpty())
                                .isPresent());
        if (!eligibility.eligible()) {
            alertPresenter.showDecision(UserVisibleDecision.warning(eligibility.title(), eligibility.message()),
                    owner());
            viewModel.updateStatusMessage(eligibility.title());
            return;
        }
        ExportCenterState state = exportCenterCoordinator.stateFrom(viewModel);
        OperationalSettings settings = currentOperationalSettings();
        List<VideoEncoderPolicy> encoderPolicies = availableVideoEncoderPolicies(settings);
        ExportCenterContext context = new ExportCenterContext(
                viewModel.currentProjectModeProperty().get(),
                viewModel.theatreActs(),
                viewModel.theatreScenes(),
                encoderPolicies,
                defaultVideoEncoderPolicy(settings, encoderPolicies), viewModel.spatialFrameModeProperty().get());
        exportCenterDialog.show(
                        owner(),
                        exportCenterCoordinator.targets(state),
                        preselectedCommand,
                        context,
                        (window, selection) -> {
                            if (selection != null) {
                                readinessDecisionForSelection(state, selection)
                                        .ifPresent(decision -> alertPresenter.showDecision(decision, window));
                            }
                        })
                .ifPresent(this::executeExportCenterTarget);
    }

    private Optional<UserVisibleDecision> readinessDecisionForSelection(
            ExportCenterState state, ExportCenterSelection selection) {
        List<String> missing = missingCleanVisuals(selection);
        if (!missing.isEmpty() && !rendersUnassignedVisuals(selection)) {
            return Optional.of(UserVisibleDecision.warning(
                    "Faltan fragmentos visuales",
                    "Hay " + missing.size() + " fragmento(s) sin imagen ni lienzo. Activa "
                            + "'Renderizar fragmentos sin imagen ni lienzo' para usar fondo negro, o asigna sus visuales."));
        }
        if (!missing.isEmpty()) {
            return Optional.of(UserVisibleDecision.informationDialog(
                    "Todo está bien",
                    "La salida está lista. " + missing.size() + " fragmento(s) sin visual se renderizarán "
                            + "con fondo negro y el texto blanco 'Sin fragmento visual asignado'."));
        }
        return exportCenterCoordinator.readinessDecision(state, selection.commandId());
    }

    private boolean validateExportSelection(ExportCenterSelection selection) {
        List<String> missing = missingCleanVisuals(selection);
        if (missing.isEmpty() || rendersUnassignedVisuals(selection)) {
            return true;
        }
        alertPresenter.showDecision(UserVisibleDecision.warning(
                "No se puede exportar todavía",
                "Faltan visuales para " + String.join(", ", missing.stream().limit(8).toList())
                        + (missing.size() > 8 ? " y otros " + (missing.size() - 8) : "")
                        + ". Asigna una imagen/lienzo o activa el fallback negro."), owner());
        return false;
    }

    private List<String> missingCleanVisuals(ExportCenterSelection selection) {
        if (selection == null) {
            return List.of();
        }
        TheatreExportScope scope = TheatreExportScope.all();
        if (selection.commandId() == AppCommandId.EXPORT_THEATRE_PORTION) {
            TheatrePortionExportOptions portion = selection.theatrePortionOptions();
            if (portion == null || portion.output() != TheatrePortionExportOptions.Output.CLEAN_VIDEO) {
                return List.of();
            }
            scope = portion.scope();
        } else if (selection.commandId() != AppCommandId.EXPORT_THEATRE_WORK) {
            return List.of();
        }
        try {
            return viewModel.missingTheatreCleanVisualSegmentIds(scope);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private static boolean rendersUnassignedVisuals(ExportCenterSelection selection) {
        if (selection.commandId() == AppCommandId.EXPORT_THEATRE_PORTION) {
            TheatrePortionExportOptions portion = selection.theatrePortionOptions();
            return portion != null && portion.videoOptions().renderUnassignedVisuals();
        }
        return selection.videoOptions() != null && selection.videoOptions().renderUnassignedVisuals();
    }

    private void executeExportCenterTarget(ExportCenterSelection selection) {
        if (selection == null || selection.commandId() == null) {
            return;
        }
        if (!validateExportSelection(selection)) {
            return;
        }
        exportCenterCoordinator.routeFor(selection.commandId()).ifPresentOrElse(route -> {
            switch (route.operation()) {
                case PODCAST_AUDIO -> handleExportPodcastWav(
                        selection.audioFormat(), selection.executionMode());
                case DOCUMENT_STUDY_VIDEO ->
                        handleExportDocumentStudyTextAudioVideo(
                                selection.documentTextVideoOptions(), selection.executionMode());
                case NARRATIVE_VIDEO -> handleExportSimpleVideo(selection.videoOptions());
                case THEATRE_WORK -> handleExportTheatreWork(selection.videoOptions());
                case THEATRE_SPATIAL_MAP -> handleExportTheatreSpatialView(selection.theatreMapOptions());
                case THEATRE_PORTION -> handleExportTheatrePortion(selection.theatrePortionOptions());
            }
        }, this::handleInspectExportReadiness);
    }

    private void handlePrepareTechnicalProblem() {
        viewModel.openTechnicalProblemPanel();
    }

    private void handleListenDocument() {
        executeDocumentAudioAction(DocumentAudioAction.FAST_LISTEN,
                "escuchar o preparar audio del documento",
                () -> pdfNarratablePreparation.prepareFastListenThenRun(anchorPage -> {
                    if (viewModel.currentPreparedPdfSourceProperty().get() != null) {
                        viewModel.startIncrementalPdfPlayback(anchorPage);
                        return;
                    }
                    viewModel.buildNarrationScriptFromDocument();
                    if (anchorPage > 0) {
                        viewModel.runPreparedPdfPrimaryActionFromPage(anchorPage);
                    } else {
                        viewModel.runDocumentPrimaryAction();
                    }
                }));
    }

    private void handleNarrateFromPdfTarget(PdfVisualTextTarget target) {
        if (target == null || !target.available()) return;
        DocuPodcastShellViewModel.PdfNarrateCacheResult cache =
                viewModel.playSelectedPdfTargetFromCompatibleCache();
        if (cache == DocuPodcastShellViewModel.PdfNarrateCacheResult.COMPLETE) {
            processOverlayExpanded.set(false);
            return;
        }
        if (cache == DocuPodcastShellViewModel.PdfNarrateCacheResult.PARTIAL) {
            if (ensureProjectSavedForDocumentAudio("completar el audio faltante desde la selección")
                    && confirmAudioEngineReadyForDocumentAction(false)) {
                processOverlayExpanded.set(true);
                viewModel.fillMissingAudioFromSelectedPdfTarget();
            }
            return;
        }
        if (!ensureProjectSavedForDocumentAudio("narrar desde el elemento seleccionado")
                || !confirmAudioEngineReadyForDocumentAction(false)) {
            return;
        }
        viewModel.stopPlayback();
        processOverlayExpanded.set(true);
        pdfNarratablePreparation.reprioritizeFromSelectionThenRun(() -> {
            viewModel.buildNarrationScriptFromDocument();
            viewModel.narrateFromSelectedPdfTarget();
        });
    }

    private void handlePlaySelection() {
        executeDocumentAudioAction(DocumentAudioAction.PLAY_SELECTION,
                "reproducir la oración seleccionada",
                () -> pdfNarratablePreparation.prepareThenRun(() -> {
                    viewModel.buildNarrationScriptFromDocument();
                    viewModel.playFromSelectedSegment();
                }));
    }


    private void handleGenerateChunksFromStatusBar() {
        if (viewModel.documentProcessingActive()) {
            processOverlayExpanded.set(true);
            viewModel.updateStatusMessage(
                    "La lectura completa ya se está procesando. Se mantiene el trabajo actual; no se creó otro lote.");
            return;
        }
        DocumentReadingReadinessSnapshot readiness =
                viewModel.fullDocumentReadingReadinessProperty().get();
        if (readiness != null && readiness.reprocessing()
                && !reprocessCompleteReadingDialog.confirm(owner())) {
            return;
        }
        processOverlayExpanded.set(true);
        Runnable start = () -> {
            executeDocumentAudioAction(DocumentAudioAction.PROCESS_COMPLETE,
                    "procesar la lectura completa",
                    () -> pdfNarratablePreparation.prepareCompleteReadingThenRun(
                            () -> {
                                viewModel.buildNarrationScriptFromDocument();
                                viewModel.processCompleteReadingWithoutPlayback();
                            }));
        };
        start.run();
    }

    private void cancelDocumentPreparationForRestart() {
        pdfNarratablePreparation.cancelActivePreparationSession();
        cancelLocalDocumentAnalysis();
    }

    private void cancelLocalDocumentAnalysis() {
        if (viewModel.cancelNarrationTranslationAnalysis()) {
            return;
        }
        if (!wordSemanticPreparation.cancelLocalAnalysis()
                && viewModel.localDocumentAnalysisRunningProperty().get()) {
            pdfNarratablePreparation.cancelLocalAnalysis();
        }
    }

    private void handleGenerateSelectedChunkFromStatusBar() {
        processOverlayExpanded.set(true);
        executeDocumentAudioAction(DocumentAudioAction.GENERATE_SELECTION,
                "renderizar audio desde el fragmento seleccionado",
                () -> pdfNarratablePreparation.prepareFromSelectionThenRun(
                        () -> {
                            viewModel.buildNarrationScriptFromDocument();
                            viewModel.generateAudioChunksFromSelectedFragment();
                        }));
    }

    private void handleProcessSelectedFragment() {
        executeDocumentAudioAction(DocumentAudioAction.PROCESS_FRAGMENT,
                "procesar solamente el fragmento seleccionado",
                () -> pdfNarratablePreparation.prepareThenRun(() -> {
                    viewModel.buildNarrationScriptFromDocument();
                    viewModel.generateAudioChunkForSelectedFragment();
                }));
    }

    private void handleProcessIntervalFromStatusBar() {
        var requested = viewModel.validatedDocumentProcessingInterval();
        if (requested.isEmpty()) {
            viewModel.updateStatusMessage(
                    "El intervalo no es válido. Revisa la unidad inicial y final.");
            return;
        }
        processOverlayExpanded.set(true);
        boolean processingActive = viewModel.documentProcessingActive();
        if (processingActive && !restartActiveProcessingDialog.confirm(owner())) {
            return;
        }
        Runnable start = () -> executeDocumentAudioAction(
                DocumentAudioAction.PROCESS_INTERVAL,
                "procesar el intervalo documental",
                () -> {
                    if (viewModel.currentPreparedPdfSourceProperty().get() != null) {
                        pdfNarratablePreparation.prepareIntervalThenRun(
                                requested.get(),
                                () -> viewModel.processPdfIntervalWithoutPlayback(
                                        requested.get()));
                    } else {
                        viewModel.buildNarrationScriptFromDocument();
                        viewModel.processWordIntervalWithoutPlayback(
                                requested.get());
                    }
                });
        if (processingActive) {
            cancelDocumentPreparationForRestart();
            viewModel.cancelAudioAndThen(start);
        } else {
            start.run();
        }
    }

    private void handleDeleteAllAudioChunksFromStatusBar() {
        if (deleteAudioChunksDialog.confirm(owner())) {
            viewModel.deleteAllPersistedAudioChunks();
        }
    }

    private void handleGenerateAudio() {
        executeDocumentAudioAction(DocumentAudioAction.GENERATE_ALL,
                "generar audio por fragmentos",
                () -> pdfNarratablePreparation.prepareAudioThenRun(
                        () -> {
                            viewModel.buildNarrationScriptFromDocument();
                            viewModel.generateAudioChunksWithoutPlayback();
                        }));
    }

    private void executeDocumentAudioAction(
            DocumentAudioAction action,
            String projectAction,
            Runnable continuation) {
        if (!ensureProjectSavedForDocumentAudio(projectAction)
                || !confirmAudioEngineReadyForDocumentAction(action.forceGeneration())) {
            return;
        }
        if (action.requiresWordSemanticPreparation()) {
            wordSemanticPreparation.prepareThenRun(continuation);
        } else {
            continuation.run();
        }
    }

    private void handleDocumentAudioAction(DocumentAudioAction action) {
        switch (action) {
            case FAST_LISTEN -> handleListenDocument();
            case PLAY_SELECTION -> handlePlaySelection();
            case PROCESS_FRAGMENT -> handleProcessSelectedFragment();
            case PROCESS_INTERVAL -> handleProcessIntervalFromStatusBar();
            case PROCESS_COMPLETE -> handleGenerateChunksFromStatusBar();
            case GENERATE_ALL -> handleGenerateAudio();
            case GENERATE_SELECTION -> handleGenerateSelectedChunkFromStatusBar();
        }
    }

    private boolean confirmAudioEngineReadyForDocumentAction(boolean forceGeneration) {
        boolean unavailable = forceGeneration
                ? viewModel.audioEngineUnavailableForGeneration()
                : viewModel.audioEngineUnavailableForDocumentPrimaryAction();
        if (!unavailable) {
            return true;
        }
        if (audioEngineUnavailableDialog.show(owner(), viewModel.audioEngineUnavailableMessage())) {
            handleOpenVoiceEngineSettings();
        }
        return false;
    }

    private void showAudioGenerationFailureIfNeeded(AudioJobStatusDto oldStatus, AudioJobStatusDto newStatus) {
        if (activeBatchExecution != null) return; // The queue reports failures per document.
        if (newStatus == null || !newStatus.failed() || oldStatus == null || !oldStatus.running()) {
            return;
        }
        String key = newStatus.jobId() + "|" + newStatus.currentSegmentId() + "|" + newStatus.message();
        if (key.equals(lastAudioFailureKey)) {
            return;
        }
        lastAudioFailureKey = key;
        alertPresenter.show(new UserNotification(
                UserNotificationLevel.ERROR,
                "DocuPodcast Studio",
                "La generacion de voz fallo",
                audioGenerationFailureMessage(newStatus),
                newStatus.message()), owner());
    }

    private static String audioGenerationFailureMessage(AudioJobStatusDto status) {
        String detail = status == null || status.message() == null
                ? ""
                : status.message().toLowerCase(java.util.Locale.ROOT);
        if (detail.contains("pytorch cuda") || detail.contains("cuda no esta disponible")) {
            return "Seleccionaste una GPU manual, pero el Python local de DocuPodcast no tiene CUDA disponible. Cambia a CPU/AUTO o prepara el Python local con backend GPU y vuelve a generar.";
        }
        if (detail.contains("no puede representar la voz/personaje")) {
            return "La voz asignada no es compatible con el motor seleccionado. Elige Narrador predeterminado y revisa las voces específicas de los fragmentos; después pulsa Generar. Para conservar una voz personalizada, selecciona un motor compatible con voces de referencia.";
        }
        return "El motor de voz termino sin WAV valido para el fragmento actual. Abre los detalles tecnicos del dialogo o Diagnostico avanzado antes de reintentar.";
    }

    private boolean ensureProjectSavedForDocumentAudio(String action) {
        if (viewModel.currentProjectFile().isPresent()) {
            return true;
        }
        if (!viewModel.projectOpenProperty().get()) {
            showProjectRequiredMessage();
            return false;
        }
        alertPresenter.show(UserNotification.information(
                "Guardar proyecto antes de continuar",
                "Para " + action + ", DocuPodcast necesita crear una carpeta de proyecto. Elige dónde guardarla; después continuará la acción solicitada."), owner());
        return handleSaveProject();
    }

    private void handleOpenExampleProject() {
        if (!confirmDiscardOrSaveIfNeeded()) {
            return;
        }
        ExampleProjectDialog dialog = new ExampleProjectDialog(
                viewModel.administrationWorkspace().examples().catalog().listExamples(),
                viewModel.administrationWorkspace().examples().inspectReadiness());
        dialog.show(owner()).ifPresent(this::createExampleProjectFromDescriptor);
    }

    private void createExampleProjectFromDescriptor(ExampleProjectDescriptor example) {
        if (example.hasTheatreMarkdown()) {
            createTheatreExampleProjectFromDescriptor(example);
            return;
        }
        alertPresenter.show(UserNotification.information(
                "Crear proyecto demo",
                "DocuPodcast copiara el Word demo y sus assets a una carpeta de proyecto. Elige el nombre y ubicacion del archivo .docupodcast.json; despues se abrira el proyecto generado."), owner());
        FileChooser chooser = projectFileChooser("Crear proyecto demo DocuPodcast");
        chooser.setInitialFileName(example.defaultProjectName() + ".docupodcast.json");
        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return;
        }
        Path target = projectContainerPathPolicy.resolveSaveAsTarget(file.toPath());
        try {
            var result = exampleProjectCreationWorkflow.create(viewModel, example, target);
            rememberCurrentProject();
            result.decision().ifPresent(decision -> alertPresenter.showDecision(decision, owner()));
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo crear el proyecto demo", ex);
        }
    }

    private void createTheatreExampleProjectFromDescriptor(ExampleProjectDescriptor example) {
        alertPresenter.show(UserNotification.information(
                "Crear demo teatral",
                "Elige una carpeta contenedora. DocuPodcast creara una carpeta con el nombre del proyecto y dentro copiara teatro.md, source.docx y assets; despues generara el .docupodcast.json desde ese manifiesto."), owner());
        DirectoryChooser chooser = NativeSourceChooser.directoryChooser();
        chooser.setTitle("Elegir carpeta contenedora del demo teatral");
        File directory = chooser.showDialog(owner());
        if (directory == null) {
            return;
        }
        try {
            var result = exampleProjectCreationWorkflow.createInDirectory(viewModel, example, directory.toPath());
            rememberCurrentProject();
            result.decision().ifPresent(decision -> alertPresenter.showDecision(decision, owner()));
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo crear el proyecto demo", ex);
        }
    }

    public void handleOpenSourceDocumentLocation() {
        Path sourcePath = viewModel.currentSourceDocumentPath().orElse(null);
        if (sourcePath == null) {
            return;
        }
        Path normalized = sourcePath.toAbsolutePath().normalize();
        Path folder = Files.isDirectory(normalized) ? normalized : normalized.getParent();
        if (folder == null) {
            return;
        }
        try {
            openFolder(folder, "No se pudo abrir la ubicación de la fuente documental");
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo abrir la ubicación de la fuente documental", ex);
        }
    }

    public void handleOpenExportsFolder() {
        Path exportsDirectory = viewModel.currentExportsDirectory().orElse(null);
        if (exportsDirectory == null) {
            return;
        }
        try {
            Files.createDirectories(exportsDirectory);
            openFolder(exportsDirectory, "No se pudo abrir la carpeta de exportaciones");
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo abrir la carpeta de exportaciones", ex);
        }
    }

    private void openFolder(Path folder, String unsupportedMessage) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            throw new IOException(unsupportedMessage + ": el sistema no permite abrir carpetas desde la aplicación.");
        }
        Desktop.getDesktop().open(folder.toFile());
    }

    public void handleToggleFullScreen() {
        Window window = owner();
        if (window instanceof Stage stage) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    public void handleNewProject() {
        if (!confirmDiscardOrSaveIfNeeded()) {
            return;
        }
        projectNameDialog.showSetup(owner())
                .ifPresent(this::createProjectFromSetup);
    }

    private void handleOpenTechnicalProblemExpress() {
        TechnicalProblemDialog.showExpress(owner(),
                        viewModel.inkInputProviders().create(DrawingFeatureCatalog.DOCUMENT_PROBLEM),
                        viewModel.drawingFeatures().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM))
                .ifPresentOrElse(
                        result -> {
                            if (result.externalPngTarget() != null) {
                                viewModel.updateStatusMessage("Problema Técnico Express exportado: "
                                        + result.externalPngTarget().getFileName() + ".");
                            } else {
                                viewModel.updateStatusMessage("Problema Técnico Express cerrado.");
                            }
                        },
                        () -> viewModel.updateStatusMessage("Problema Técnico Express cancelado."));
    }

    private void createProjectFromSetup(ProjectNameDialog.ProjectSetup setup) {
        viewModel.createNewProject(setup.title(), setup.mode());
        ProjectInitialSourceDialog.Decision decision = projectInitialSourceDialog.show(owner());
        if (decision == ProjectInitialSourceDialog.Decision.CANCEL) {
            return;
        }
        if (decision == ProjectInitialSourceDialog.Decision.CREATE_WITHOUT_SOURCE) {
            return;
        }
        chooseSourceDocument("Elegir fuente inicial del proyecto")
                .ifPresent(this::runSourceDocumentImport);
    }

    public void handleOpenProject() {
        if (!confirmDiscardOrSaveIfNeeded()) {
            return;
        }
        FileChooser chooser = projectFileChooser("Abrir proyecto DocuPodcast");
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        try {
            openProjectFile(file.toPath());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo abrir el proyecto", ex);
        }
    }

    private void handleOpenRecentProject(Path projectFile) {
        if (projectFile == null) {
            return;
        }
        if (!Files.isRegularFile(projectFile)) {
            recentProjects.setAll(recentProjectsStore.forget(projectFile));
            alertPresenter.show(UserNotification.warning(
                    "Proyecto reciente no disponible",
                    "No se encontro el archivo: " + projectFile), owner());
            return;
        }
        if (!confirmDiscardOrSaveIfNeeded()) {
            return;
        }
        try {
            openProjectFile(projectFile);
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo abrir el proyecto reciente", ex);
        }
    }

    private void openProjectFile(Path projectFile) throws IOException {
        openProjectFile(projectFile, () -> {}, failure ->
                showError("No se pudo abrir el proyecto", failure));
    }

    private void openProjectFile(Path projectFile, Runnable afterOpen) {
        openProjectFile(projectFile, afterOpen, failure ->
                showError("No se pudo abrir el proyecto", failure));
    }

    private void openProjectFile(Path projectFile, Runnable afterOpen,
                                 java.util.function.Consumer<Throwable> failureHandler) {
        if (openingProject.get()) {
            failureHandler.accept(new IllegalStateException(
                    "Ya hay otro proyecto abriéndose."));
            return;
        }
        openingProject.set(true);
        projectLoadingOverlay.setManaged(true);
        projectLoadingOverlay.setVisible(true);
        getTop().setDisable(true);
        if (getBottom() != null) getBottom().setDisable(true);
        Task<PreparedProjectOpen> task = new Task<>() {
            @Override protected PreparedProjectOpen call() throws Exception {
                var opened = viewModel.prepareProjectOpen(projectFile);
                var integrity = viewModel.inspectPreparedProject(opened, projectFile);
                var playback = viewModel.prepareOpenedProjectPlaybackManifest(opened, projectFile);
                return new PreparedProjectOpen(opened, integrity, playback);
            }
        };
        task.setOnSucceeded(event -> {
            try {
                viewModel.applyOpenedProject(task.getValue().opened(), projectFile,
                        task.getValue().playback());
                rememberCurrentProject();
                finishProjectOpen();
                viewModel.updateStatusMessage(task.getValue().integrity().statusMessage());
                task.getValue().integrity().decision().ifPresent(decision -> alertPresenter.showDecision(decision, owner()));
                afterOpen.run();
            } catch (IOException | RuntimeException ex) {
                finishProjectOpen();
                failureHandler.accept(ex);
            }
        });
        task.setOnFailed(event -> {
            finishProjectOpen();
            failureHandler.accept(task.getException());
        });
        Thread worker = new Thread(task, "project-open");
        worker.setDaemon(true);
        worker.start();
    }

    private void finishProjectOpen() {
        openingProject.set(false);
        projectLoadingOverlay.setVisible(false);
        projectLoadingOverlay.setManaged(false);
        getTop().setDisable(false);
        if (getBottom() != null) getBottom().setDisable(false);
    }

    private record PreparedProjectOpen(
            com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.OpenedProjectContext opened,
            com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.ProjectIntegrityInspectionOutcome integrity,
            com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest playback) {}

    /**
     * Opens a project supplied by the production launcher and optionally starts the same
     * gap-aware audio generation action exposed by the reading workspace.
     */
    public void runStartupProjectAction(Path projectFile, boolean generateAudio) {
        openProjectFile(projectFile, () -> {
            if (generateAudio) {
                viewModel.submitAudioGeneration();
            }
        });
    }

    public boolean handleSaveProject() {
        try {
            if (viewModel.currentProjectFile().isPresent()) {
                viewModel.saveCurrentProject();
                rememberCurrentProject();
                showProjectSourceCopyNoticeIfNeeded();
                return true;
            }
            return handleSaveProjectAs();
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo guardar el proyecto", ex);
            return false;
        }
    }

    public boolean handleSaveProjectAs() {
        return handleSaveProjectAs(null);
    }

    private boolean handleSaveProjectAs(Path suggestedFile) {
        FileChooser chooser = projectFileChooser("Guardar proyecto DocuPodcast en carpeta contenedora");
        configureInitialProjectSaveLocation(chooser, suggestedFile);
        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return false;
        }
        Path target = projectContainerPathPolicy.resolveSaveAsTarget(file.toPath());
        try {
            viewModel.saveCurrentProjectAs(target);
            rememberCurrentProject();
            showProjectSourceCopyNoticeIfNeeded();
            return true;
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo guardar el proyecto", ex);
            return false;
        }
    }

    private void showProjectSourceCopyNoticeIfNeeded() {
        if (preferences.getBoolean(PREF_HIDE_SOURCE_COPY_NOTICE, false)) {
            return;
        }
        Path projectFile = viewModel.currentProjectFile().orElse(null);
        Path sourcePath = viewModel.currentSourceDocumentPath().orElse(null);
        if (projectFile == null || sourcePath == null) {
            return;
        }
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            return;
        }
        Path canonicalSource = sourcePath.toAbsolutePath().normalize();
        Path projectSourceFolder = projectDirectory.resolve("source").toAbsolutePath().normalize();
        if (!canonicalSource.startsWith(projectSourceFolder)) {
            return;
        }

        projectSourceCopyNoticeDialog.show(owner(), canonicalSource,
                dontShowAgain -> {
                    if (dontShowAgain) {
                        preferences.putBoolean(PREF_HIDE_SOURCE_COPY_NOTICE, true);
                    }
                });
    }

    private void rememberCurrentProject() {
        Path projectFile = viewModel.currentProjectFile().orElse(null);
        if (projectFile == null) {
            return;
        }
        String title = viewModel.currentProject()
                .map(project -> project.metadata().title())
                .orElse("");
        String projectType = viewModel.currentProject()
                .map(project -> project.metadata().mode().displayName())
                .orElse("");
        recentProjects.setAll(recentProjectsStore.remember(projectFile, title, projectType));
    }

    public void handleCloseProject() {
        if (!confirmDiscardOrSaveIfNeeded()) {
            return;
        }
        viewModel.closeCurrentProject();
    }

    private void returnFromDocumentVideoBatch() {
        if (viewModel.projectOpenProperty().get()) {
            if (expressOpenedChild) {
                try { viewModel.saveCurrentProject(); }
                catch (IOException failure) { throw new IllegalStateException("No se pudo guardar el proyecto del lote.", failure); }
            }
            viewModel.closeCurrentProject();
        }
        expressOpenedChild = false;
        viewModel.showWelcome();
        recentProjects.setAll(recentProjectsStore.load());
    }

    public void handleImportWord() {
        if (!prepareForSourceDocumentImport()) {
            return;
        }
        chooseSourceDocument("Abrir documento fuente").ifPresent(this::runSourceDocumentImport);
    }

    private Optional<Path> chooseSourceDocument(String title) {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos compatibles (*.docx, *.pdf, *.md, *.markdown, *.txt)", "*.docx", "*.pdf", "*.md", "*.markdown", "*.txt"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word/DOCX (*.docx)", "*.docx"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF texto u OCR local (*.pdf)", "*.pdf"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown (*.md, *.markdown)", "*.md", "*.markdown"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto plano (*.txt)", "*.txt"));
        File file = chooser.showOpenDialog(owner());
        return file == null ? Optional.empty() : Optional.of(file.toPath());
    }

    private boolean prepareForSourceDocumentImport() {
        if (!viewModel.projectOpenProperty().get()) {
            showProjectRequiredMessage();
            return false;
        }
        Optional<Path> currentSource = viewModel.currentSourceDocumentPath();
        if (currentSource.isEmpty()) {
            return true;
        }
        if (!projectSourceReplacementDialog.confirm(owner(), currentSource.get())) {
            return false;
        }
        return confirmSourceReplacementDirtyState();
    }

    private void showProjectRequiredMessage() {
        alertPresenter.show(UserNotification.information(
                "Primero crea un proyecto",
                "Crea o abre un proyecto y después selecciona su fuente documental. "
                        + "DocuPodcast no abre Word, PDF, Markdown ni TXT como archivos sueltos."), owner());
    }

    private boolean confirmSourceReplacementDirtyState() {
        if (viewModel.audioJobRunningProperty().get()) {
            activeAudioJobDialog.show(owner());
            return false;
        }
        if (!viewModel.hasUnsavedChanges()) {
            return true;
        }
        UnsavedChangesDialog.Decision decision = unsavedChangesDialog.show(owner());
        if (decision == UnsavedChangesDialog.Decision.CANCEL) {
            return false;
        }
        if (decision == UnsavedChangesDialog.Decision.SAVE) {
            return handleSaveProject();
        }
        Optional<Path> projectFile = viewModel.currentProjectFile();
        if (projectFile.isEmpty()) {
            return true;
        }
        try {
            viewModel.openProject(projectFile.get());
            return true;
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo recargar el proyecto antes de reemplazar la fuente", ex);
            return false;
        }
    }

    public void handleImportTheatreGrammar() {
        if (!prepareForSourceDocumentImport()) {
            return;
        }
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Importar obra a partir de gramatica teatral");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gramatica teatral Markdown (*.md, *.markdown)", "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        grammarWorkflow.importTheatreGrammar(file.toPath(), this::promptSaveProjectForImportedSourceIfNeeded);
    }

    public void handleExportTheatreGrammarTemplate() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Exportar plantilla de gramatica teatral");
        chooser.setInitialFileName(ProjectGrammarKind.THEATRE_PRODUCTION.defaultFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"));
        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return;
        }
        grammarWorkflow.exportTemplate(ProjectGrammarKind.THEATRE_PRODUCTION, file.toPath());
    }

    public void handleImportNarrativeVideoGrammar() {
        if (!prepareForSourceDocumentImport()) {
            return;
        }
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Importar guion narrativo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gramatica narrativa Markdown (*.md, *.markdown)", "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        grammarWorkflow.importNarrativeVideoGrammar(file.toPath(), this::promptSaveProjectForImportedSourceIfNeeded);
    }

    public void handleExportNarrativeVideoGrammarTemplate() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Exportar plantilla narrativa");
        chooser.setInitialFileName(ProjectGrammarKind.NARRATIVE_VIDEO.defaultFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"));
        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return;
        }
        grammarWorkflow.exportTemplate(ProjectGrammarKind.NARRATIVE_VIDEO, file.toPath());
    }

    private void runSourceDocumentImport(Path sourceFile) {
        DocumentImportProgressDialog progress = new DocumentImportProgressDialog(owner(), sourceFile.getFileName().toString());
        Task<com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource> task = new Task<>() {
            @Override
            protected com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource call() throws Exception {
                updateMessage("Leyendo documento para mostrarlo en la vista Documento...");
                return viewModel.importAndClassifySourceDocument(sourceFile);
            }
        };
        progress.bind(task);
        task.setOnSucceeded(event -> {
            progress.close();
            PauseTransition closePulse = new PauseTransition(Duration.millis(80));
            closePulse.setOnFinished(closeEvent -> {
                try {
                    viewModel.attachImportedDocument(task.getValue());
                    promptSaveProjectForImportedSourceIfNeeded(sourceFile);
                } catch (IOException | RuntimeException ex) {
                    showError("No se pudo importar el documento", ex);
                }
            });
            closePulse.play();
        });
        task.setOnFailed(event -> {
            progress.close();
            Throwable ex = task.getException();
            if (ex instanceof SourceDocumentRequirementException requirement) {
                alertPresenter.show(UserNotification.warning("PDF no compatible", requirement.getMessage()), owner());
            } else {
                showError("No se pudo importar el documento", ex instanceof Exception exception ? exception : new RuntimeException(ex));
            }
        });
        progress.show();
        backgroundTaskRunner.start("docupodcast-source-import", task);
    }


    public void handleImportStoryboardImage() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Importar imagen para visuales");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagen compatible (*.png, *.jpg, *.jpeg, *.webp, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG recomendado (*.png)", "*.png")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        try {
            viewModel.importStoryboardImage(file.toPath());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo importar la imagen", ex);
        }
    }

    public void handleImportBridgeImageForSelection() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Importar imagen puente");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagen compatible (*.png, *.jpg, *.jpeg, *.webp, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("PNG recomendado (*.png)", "*.png")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        try {
            viewModel.importImageForSelectedDocumentRange(file.toPath(), NarrativeLayerKind.BRIDGE_IMAGE);
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo importar la imagen puente", ex);
        }
    }

    public void handleImportVoiceSample() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Importar muestra de voz");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio compatible (*.wav, *.mp3, *.flac, *.ogg, *.m4a)", "*.wav", "*.mp3", "*.flac", "*.ogg", "*.m4a"),
                new FileChooser.ExtensionFilter("WAV recomendado (*.wav)", "*.wav")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        try {
            viewModel.importOwnVoiceSample(file.toPath());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo importar la muestra de voz", ex);
        }
    }


    public void handleImportAudioForSelection() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Elegir audio del computador");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio compatible (*.wav, *.mp3, *.m4a, *.flac, *.ogg)", "*.wav", "*.mp3", "*.m4a", "*.flac", "*.ogg"),
                new FileChooser.ExtensionFilter("WAV recomendado (*.wav)", "*.wav")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        try {
            viewModel.importUserAudioForSelectedDocumentRange(file.toPath());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo asignar el audio del computador", ex);
        }
    }

    public void handleExtractVideoAudioForSelection() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Extraer audio de video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video compatible (*.mp4, *.mov, *.mkv, *.webm)", "*.mp4", "*.mov", "*.mkv", "*.webm"),
                new FileChooser.ExtensionFilter("MP4 recomendado (*.mp4)", "*.mp4")
        );
        File file = chooser.showOpenDialog(owner());
        if (file == null) {
            return;
        }
        try {
            viewModel.extractVideoAudioForSelectedDocumentRange(file.toPath());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo extraer audio del video", ex);
        }
    }


    public void handleExportPodcastWav() { handleExportPodcastWav(AudioExportFormat.WAV); }
    public void handleExportPodcastWav(AudioExportFormat requestedFormat) {
        handleExportPodcastWav(requestedFormat,
                ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT);
    }

    private void handleExportPodcastWav(AudioExportFormat requestedFormat,
                                        ExportExecutionMode executionMode) {
        AudioExportFormat format = requestedFormat == null ? AudioExportFormat.WAV : requestedFormat;
        Path target = chooseExportPodcastWavTarget(format);
        if (target == null) return;
        ExportExecutionMode mode = executionMode == null
                ? ExportExecutionMode.READY_ONLY : executionMode;
        if (mode.preparesFullDocument()) {
            viewModel.setDocumentProcessingScope(DocumentProcessingScope.FULL_DOCUMENT);
            viewModel.beginLocalDocumentAnalysis("Preparando exportación de audio",
                    "Verificando el documento completo y reutilizando los derivados vigentes.");
            prepareDocumentForExportThenRun(() ->
                    handleExportPodcastWavPrepared(format, target, true));
            return;
        }
        handleExportPodcastWavPrepared(format, target, false);
    }

    private void handleExportPodcastWavPrepared(AudioExportFormat requestedFormat,
                                                Path target,
                                                boolean generateMissing) {
        AudioExportFormat format = requestedFormat == null ? AudioExportFormat.WAV : requestedFormat;
        if (!viewModel.hasAllChunksRendered()) {
            if (!generateMissing) {
                alertPresenter.showDecision(UserVisibleDecision.warning(
                        "Audio todavía incompleto",
                        "Activa 'Completar lo autorizado y exportar' para generar únicamente los fragmentos permitidos que estén pendientes y exportar automáticamente."), owner());
                return;
            }
            PreparedExportIntent intent = new PreparedExportIntent(
                    AppCommandId.EXPORT_PODCAST_WAV,
                    ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT,
                    target,
                    "Audio final");
            viewModel.submitAudioGenerationWithPendingExport(null, intent, () -> {
                exportPodcastWavInBackground(target);
            });
            return;
        }
        exportPodcastWavInBackground(target);
    }

    private void exportPodcastWavInBackground(Path target) {
        viewModel.beginLocalDocumentAnalysis("Finalizando audio",
                "Uniendo los fragmentos vigentes y escribiendo " + target.getFileName() + ".");
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return viewModel.exportPodcastWavResult(target);
            }
        };
        task.setOnSucceeded(event -> {
            activeFinalAudioExportTask = null;
            viewModel.endLocalDocumentAnalysis();
            viewModel.updateStatusMessage(task.getValue());
        });
        task.setOnFailed(event -> {
            activeFinalAudioExportTask = null;
            viewModel.endLocalDocumentAnalysis();
            showError("No se pudo exportar el audio final", task.getException());
        });
        task.setOnCancelled(event -> {
            activeFinalAudioExportTask = null;
            viewModel.endLocalDocumentAnalysis();
            viewModel.updateStatusMessage("Exportación de audio cancelada; los fragmentos válidos se conservaron.");
        });
        activeFinalAudioExportTask = task;
        backgroundTaskRunner.start("docupodcast-final-audio-export", task);
    }

    private void cancelCurrentExportOrAudioOperation() {
        Task<?> finalAudio = activeFinalAudioExportTask;
        if (finalAudio != null && finalAudio.isRunning()) {
            finalAudio.cancel(true);
            viewModel.updateStatusMessage(
                    "Cancelación de la exportación final de audio solicitada; los WAV preparados se conservarán.");
            return;
        }
        if (videoExportProgressCoordinator.cancelBackgroundExport()) {
            viewModel.updateStatusMessage(
                    "Cancelación del video solicitada; los derivados válidos se conservarán.");
            return;
        }
        viewModel.cancelCurrentAudioOperation();
    }
    private Path chooseExportPodcastWavTarget(AudioExportFormat requestedFormat) {
        AudioExportFormat format = requestedFormat == null ? AudioExportFormat.WAV : requestedFormat;
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Exportar audio final");
        chooser.setInitialFileName("audio-final" + format.extension());
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(format.displayName() + " (*" + format.extension() + ")", "*" + format.extension()),
                new FileChooser.ExtensionFilter("Audio final (*.wav, *.mp3, *.aac)", "*.wav", "*.mp3", "*.aac"),
                new FileChooser.ExtensionFilter("WAV sin compresión (*.wav)", "*.wav"),
                new FileChooser.ExtensionFilter("MP3 comprimido (*.mp3)", "*.mp3"),
                new FileChooser.ExtensionFilter("AAC comprimido (*.aac)", "*.aac")
        );
        File file = chooser.showSaveDialog(owner());
        return file == null ? null : format.normalizeTarget(file.toPath());
    }

    public void handleExportDiagnosticReport() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Exportar paquete de soporte sanitizado");
        chooser.setInitialFileName("docupodcast-soporte.zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Paquete ZIP (*.zip)", "*.zip"));
        File file = chooser.showSaveDialog(owner());
        if (file == null) {
            return;
        }
        try {
            viewModel.exportDiagnosticReport(file.toPath());
            alertPresenter.showDecision(DiagnosticUserDecisionFactory.diagnosticReportExported(file.toPath()), owner());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo exportar el reporte diagnóstico", ex);
        }
    }

    public void handleExportProjectBundle() {
        DirectoryChooser chooser = NativeSourceChooser.directoryChooser();
        chooser.setTitle("Elegir carpeta para paquete DocuPodcast");
        File folder = chooser.showDialog(owner());
        if (folder == null) {
            return;
        }
        try {
            viewModel.exportProjectBundle(folder.toPath());
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo exportar el paquete DocuPodcast", ex);
        }
    }


    public void handleExportSimpleVideo() {
        Optional<VideoExportOptions> options = chooseVideoResolution();
        if (options.isEmpty()) {
            return;
        }
        chooseTargetMp4AndExport("Exportar video final", "docupodcast-video", options.get(), "");
    }

    private void handleExportSimpleVideo(VideoExportOptions options) {
        chooseTargetMp4AndExport("Exportar video final", "docupodcast-video",
                options == null ? new VideoExportOptions(null, 30, null) : options, "");
    }

    public void handleExportDocumentStudyTextAudioVideo() {
        handleExportDocumentStudyTextAudioVideo(DocumentTextVideoOptions.defaults(),
                ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT);
    }

    public void handleExportDocumentStudyTextAudioVideo(DocumentTextVideoOptions requestedOptions) {
        handleExportDocumentStudyTextAudioVideo(requestedOptions,
                ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT);
    }

    private void handleExportDocumentStudyTextAudioVideo(
            DocumentTextVideoOptions requestedOptions,
            ExportExecutionMode executionMode) {
        DocumentTextVideoOptions textOptions = requestedOptions == null
                ? DocumentTextVideoOptions.defaults() : requestedOptions;
        VideoExportOptions options = documentStudyVideoOptions(textOptions);
        File target = fileForExport("Exportar video documental texto+audio",
                "estudio-documental-texto-audio", options);
        if (target == null || !ensureVideoRendererAvailable()) return;
        ExportExecutionMode mode = executionMode == null
                ? ExportExecutionMode.READY_ONLY : executionMode;
        if (mode.preparesFullDocument()) {
            viewModel.setDocumentProcessingScope(DocumentProcessingScope.FULL_DOCUMENT);
            viewModel.beginLocalDocumentAnalysis("Preparando exportación de video",
                    "Verificando semántica, narración y audio del documento completo.");
            prepareDocumentForExportThenRun(() ->
                    handleExportDocumentStudyTextAudioVideoPrepared(
                            textOptions, target, options, true));
            return;
        }
        handleExportDocumentStudyTextAudioVideoPrepared(textOptions, target, options, false);
    }

    private void handleExportDocumentStudyTextAudioVideoPrepared(
            DocumentTextVideoOptions requestedOptions,
            File target,
            VideoExportOptions options,
            boolean generateMissing) {
        var scriptAtClick = viewModel.currentScriptProperty().get();
        LOGGER.info("document-export.audio-boundary stage=JAVAFX_COMMAND count={} ids={}",
                scriptAtClick == null ? 0 : scriptAtClick.segments().size(),
                scriptAtClick == null ? java.util.List.of()
                        : scriptAtClick.segments().stream()
                        .map(com.marcosmoreiradev.docupodcaststudio.domain.script
                                .NarrationSegment::id).toList());
        DocumentTextVideoOptions textOptions = requestedOptions == null ? DocumentTextVideoOptions.defaults() : requestedOptions;
        String statusMessage = "Exportando video documental texto+audio.";
        DocumentExportReadinessSnapshot readiness;
        try {
            readiness = viewModel.inspectDocumentExportReadiness();
        } catch (IOException | RuntimeException failure) {
            if (generateMissing) viewModel.endLocalDocumentAnalysis();
            showError("No se pudo comprobar la preparación de la exportación", failure);
            return;
        }
        if (readiness.compositionOnlyPending()) {
            viewModel.updateStatusMessage(
                    "Componiendo el audio vigente de la región configurada y exportando video.");
            exportDocumentStudyTextAudioVideoInBackground(target, options, textOptions);
            return;
        }
        if (!readiness.readyToRender()) {
            if (!generateMissing) {
                alertPresenter.showDecision(UserVisibleDecision.warning(
                        "La exportación necesita preparación",
                        audioReadinessDetail(readiness)
                                + "\n\nActiva 'Completar lo autorizado y exportar' para completar la cadena según la configuración actual."), owner());
                return;
            }
            PreparedExportIntent intent = new PreparedExportIntent(
                    AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO,
                    ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT,
                    target.toPath(),
                    "Video de estudio documental");
            viewModel.submitAudioGenerationWithPendingExport(readiness, intent, () -> {
                viewModel.updateStatusMessage(statusMessage);
                exportDocumentStudyTextAudioVideoInBackground(target, options, textOptions);
            });
            return;
        }
        viewModel.updateStatusMessage(statusMessage);
        exportDocumentStudyTextAudioVideoInBackground(target, options, textOptions);
    }

    /** Resolves source-specific semantics before both audio and video export inspect narration. */
    private void prepareDocumentForExportThenRun(Runnable continuation) {
        Runnable afterWord = () -> {
            if (viewModel.currentDocumentProperty().get() != null) {
                viewModel.buildNarrationScriptFromDocument();
                continuation.run();
                return;
            }
            pdfNarratablePreparation.prepareCompleteReadingThenRun(() -> {
                viewModel.buildNarrationScriptFromDocument();
                continuation.run();
            });
        };
        wordSemanticPreparation.prepareThenRun(afterWord);
    }

    private static String audioReadinessDetail(
            DocumentExportReadinessSnapshot readiness) {
        int missing = readiness.missingAudioSegmentIds().size();
        int stale = readiness.staleAudioSegmentIds().size();
        int invalid = readiness.invalidAudioSegmentIds().size();
        int total = readiness.selection().resolvedSegmentIds().size();
        int reusable = Math.max(0, total - missing - stale - invalid);
        String condition;
        if (missing > 0 && (stale > 0 || invalid > 0)) {
            condition = "Faltan fragmentos y algunos deben actualizarse.";
        } else if (missing > 0) {
            condition = "Faltan fragmentos de audio.";
        } else if (stale > 0) {
            condition = "Hay fragmentos de audio que deben actualizarse.";
        } else if (invalid > 0) {
            condition = "Hay fragmentos de audio inválidos que deben reconstruirse.";
        } else {
            condition = "Faltan composiciones audiovisuales.";
        }
        return condition + "\n\nAlcance: "
                + readiness.selection().scope()
                + "\nAudio reutilizable: " + reusable
                + "\nAudio faltante: " + missing
                + "\nAudio por actualizar: " + stale
                + (invalid == 0 ? "" : "\nAudio inválido: " + invalid)
                + "\n\n¿Renderizar únicamente los derivados pendientes y luego exportar?";
    }

    public void handleExportTheatreWork() {
        Optional<VideoExportOptions> selected = chooseVideoResolution();
        if (selected.isEmpty()) {
            return;
        }
        handleExportTheatreWork(selected.get());
    }

    private void handleExportTheatreWork(VideoExportOptions requestedOptions) {
        VideoExportOptions options = requestedOptions;
        if (options == null) {
            return;
        }
        String statusMessage = "Exportando video teatral limpio.";
        if (!viewModel.hasAllChunksRendered()) {
            if (!incompleteAudioExportDialog.confirmRenderAndExport(owner())) { return; }
            viewModel.submitAudioGenerationWithPendingExport(() -> {
                viewModel.updateStatusMessage(statusMessage);
                exportTheatreWorkInBackground(
                        fileForExport("Exportar video teatral limpio", "docupodcast-teatro-limpio", options),
                        options);
            });
            return;
        }
        File file = fileForExport("Exportar video teatral limpio", "docupodcast-teatro-limpio", options);
        if (file == null) {
            return;
        }
        viewModel.updateStatusMessage(statusMessage);
        if (!ensureVideoRendererAvailable()) return;
        exportTheatreWorkInBackground(file, options);
    }

    public void handleExportTheatreSpatialView() {
        Optional<VideoExportOptions> options = chooseVideoResolution();
        if (options.isEmpty()) {
            return;
        }
        handleExportTheatreSpatialView(new TheatreMapExportOptions(
                options.get(), com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode.FRAGMENT_VISUALS));
    }

    private void handleExportTheatreSpatialView(TheatreMapExportOptions requestedOptions) {
        TheatreMapExportOptions options = requestedOptions;
        if (options == null || options.videoOptions() == null) {
            return;
        }
        String statusMessage = "Exportando video de mapa teatral con mapa espacial y fragmentos sincronizados.";
        if (!viewModel.hasAllChunksRendered()) {
            if (!incompleteAudioExportDialog.confirmRenderAndExport(owner())) { return; }
            viewModel.submitAudioGenerationWithPendingExport(() -> {
                viewModel.updateStatusMessage(statusMessage);
                exportTheatreSpatialVideoInBackground(
                        fileForExport("Exportar video de mapa teatral", "docupodcast-mapa-teatral", options.videoOptions()),
                        options);
            });
            return;
        }
        File file = fileForExport("Exportar video de mapa teatral", "docupodcast-mapa-teatral", options.videoOptions());
        if (file == null) {
            return;
        }
        viewModel.updateStatusMessage(statusMessage);
        if (!ensureVideoRendererAvailable()) return;
        exportTheatreSpatialVideoInBackground(file, options);
    }

    public void handleExportTheatrePortion() {
        OperationalSettings settings = currentOperationalSettings();
        List<VideoEncoderPolicy> encoderPolicies = availableVideoEncoderPolicies(settings);
        Optional<TheatrePortionExportOptions> selected = theatrePortionExportOptionsDialog.show(
                owner(),
                viewModel.theatreActs(),
                viewModel.theatreScenes(),
                encoderPolicies,
                defaultVideoEncoderPolicy(settings, encoderPolicies));
        if (selected.isEmpty()) {
            return;
        }
        handleExportTheatrePortion(selected.get());
    }

    private void handleExportTheatrePortion(TheatrePortionExportOptions requestedOptions) {
        TheatrePortionExportOptions options = requestedOptions;
        if (options == null) {
            return;
        }
        String status = options.output() == TheatrePortionExportOptions.Output.THEATRE_MAP
                ? "Exportando porcion de obra como video mapa teatral."
                : "Exportando porcion de obra como video de fragmentos.";
        Runnable exportAction = () -> {
            File file = fileForExport("Exportar porcion de obra", "docupodcast-porcion-obra", options.videoOptions());
            if (file == null) {
                return;
            }
            viewModel.updateStatusMessage(status);
            if (!ensureVideoRendererAvailable()) return;
            exportTheatrePortionInBackground(file, options);
        };
        if (!viewModel.hasChunksRenderedForTheatreScope(options.scope())) {
            if (!incompleteAudioExportDialog.confirmRenderAndExport(owner())) { return; }
            viewModel.submitAudioGenerationWithPendingExport(exportAction);
            return;
        }
        exportAction.run();
    }

    private void chooseTargetMp4AndExport(String title, String filePrefix, VideoExportOptions options, String statusMessage) {
        if (!viewModel.hasAllChunksRendered()) {
            if (!incompleteAudioExportDialog.confirmRenderAndExport(owner())) { return; }
            viewModel.submitAudioGenerationWithPendingExport(() -> {
                if (statusMessage != null && !statusMessage.isBlank()) {
                    viewModel.updateStatusMessage(statusMessage);
                }
                exportFinalVideoInBackground(fileForExport(title, filePrefix, options), options);
            });
            return;
        }
        File file = fileForExport(title, filePrefix, options);
        if (file == null) {
            return;
        }
        if (statusMessage != null && !statusMessage.isBlank()) {
            viewModel.updateStatusMessage(statusMessage);
        }
        if (!ensureVideoRendererAvailable()) return;
        exportFinalVideoInBackground(file, options);
    }

    private boolean ensureVideoRendererAvailable() {
        boolean ready = capabilityAdministration.components().stream()
                .filter(component -> CapabilityId.VIDEO_RENDERING.equals(component.capability()))
                .anyMatch(component -> {
                    try {
                        return capabilityAdministration.inspect(new CapabilityRequirement(
                                component.capability(), component.engineId(), null, null, Map.of())).ready();
                    }
                    catch (RuntimeException failure) { return false; }
                });
        if (ready) return true;
        alertPresenter.show(UserNotification.warning(
                "Render de video no disponible",
                "No hay un renderizador listo. Abre Configuración > Motores y dependencias "
                        + "para instalarlo, repararlo o probarlo."), owner());
        return false;
    }

    private File fileForExport(String title, String filePrefix, VideoExportOptions options) {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Video MP4 (*.mp4)", "*.mp4"));
        chooser.setInitialFileName(filePrefix + "-" + options.resolution().label().toLowerCase(java.util.Locale.ROOT)
                + "-" + options.framesPerSecond() + "fps.mp4");
        File selected = chooser.showSaveDialog(owner());
        if (selected == null || selected.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4")) {
            return selected;
        }
        File parent = selected.getAbsoluteFile().getParentFile();
        return new File(parent, selected.getName() + ".mp4");
    }

    private void exportFinalVideoInBackground(File file, VideoExportOptions options) {
        if (file == null) {
            return;
        }
        if (!saveProjectBeforeBackgroundExport("No se pudo guardar el proyecto antes de exportar el video final")) {
            return;
        }
        videoExportProgressCoordinator.export(owner(), file.toPath(), options,
                (targetFile, selectedOptions, progress, cancellationRequested) ->
                        viewModel.exportFinalVideo(targetFile,
                                selectedOptions.resolution(),
                                selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                progress,
                                cancellationRequested),
                ex -> showError("No se pudo exportar el video final", ex));
    }

    private void exportDocumentStudyTextAudioVideoInBackground(File file, VideoExportOptions options, DocumentTextVideoOptions textOptions) {
        if (file == null) {
            return;
        }
        if (!saveProjectBeforeBackgroundExport("No se pudo guardar el proyecto antes de exportar el video documental")) {
            return;
        }
        viewModel.beginDocumentExportRender();
        videoExportProgressCoordinator.exportInBackground(file.toPath(), options,
                    (targetFile, selectedOptions, progress, cancellationRequested) ->
                            viewModel.exportDocumentStudyTextAudioVideo(targetFile,
                                    selectedOptions.resolution(), selectedOptions.framesPerSecond(),
                                    selectedOptions.encoderPolicy(),
                                    textOptions == null ? DocumentTextVideoOptions.defaults()
                                            : textOptions.withResolution(selectedOptions.resolution()),
                                    progress,
                                    () -> cancellationRequested.getAsBoolean()
                                            || viewModel.documentExportCancellationRequested()),
                    viewModel::acceptDocumentExportProgress,
                    this::showDocumentVideoExportSuccess,
                    ex -> showError("No se pudo exportar el video documental texto+audio", ex),
                    viewModel::endDocumentExportRender);
    }

    /** Batch variant: same renderer and scheduler, with non-modal terminal callbacks. */
    private void exportDocumentStudyTextAudioVideoInBackground(
            File file,
            VideoExportOptions options,
            DocumentTextVideoOptions textOptions,
            Consumer<Path> success,
            Consumer<Throwable> failure,
            Consumer<com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress> progress) {
        if (file == null) {
            failure.accept(new IOException("No se definió el archivo MP4 de salida."));
            return;
        }
        try {
            viewModel.saveCurrentProject();
        } catch (IOException | RuntimeException ex) {
            failure.accept(ex);
            return;
        }
        viewModel.beginDocumentExportRender();
        videoExportProgressCoordinator.exportInBackground(file.toPath(), options,
                (targetFile, selectedOptions, renderProgress, cancellationRequested) ->
                        viewModel.exportDocumentStudyTextAudioVideo(targetFile,
                                selectedOptions.resolution(), selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                textOptions == null ? DocumentTextVideoOptions.defaults()
                                        : textOptions.withResolution(selectedOptions.resolution()),
                                renderProgress,
                                () -> cancellationRequested.getAsBoolean()
                                        || viewModel.documentExportCancellationRequested()),
                progress,
                success,
                failure,
                viewModel::endDocumentExportRender);
    }

    private void showDocumentVideoExportSuccess(Path targetFile) {
        Path normalized = targetFile == null ? null : targetFile.toAbsolutePath().normalize();
        if (normalized == null || !Files.isRegularFile(normalized)) {
            showError("La exportación terminó sin un archivo verificable",
                    new IOException("No se encontró el MP4 final en " + normalized));
            return;
        }

        ButtonType openFolderButton = com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse.button(
                "Abrir carpeta", ButtonBar.ButtonData.LEFT);
        var outcome = viewModel.lastDocumentaryVideoOutcome();
        boolean incomplete = outcome != null && outcome.missingIllustrations() > 0;
        String message = (incomplete
                ? "El video se exportó, pero faltan " + outcome.missingIllustrations()
                    + " ilustraciones de IA. Puedes reintentar la exportación: se reutilizan los audios y las imágenes válidas."
                : "El video se exportó correctamente.")
                + System.lineSeparator() + System.lineSeparator()
                + "Archivo: " + normalized.getFileName()
                + System.lineSeparator()
                + "Tamaño: " + formattedFileSize(normalized)
                + System.lineSeparator()
                + "Ubicación: " + normalized;
        Alert alert = StudioMessageDialog.create(
                owner(),
                incomplete ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION,
                "Exportación terminada",
                incomplete ? "Video con ilustraciones incompletas" : "Video exportado correctamente",
                message,
                "",
                openFolderButton,
                ButtonType.OK);
        Optional<ButtonType> response = alert.showAndWait();
        if (response.orElse(null) == openFolderButton) {
            Path parent = normalized.getParent();
            if (parent != null) {
                try {
                    openFolder(parent, "No se pudo abrir la carpeta del video exportado");
                } catch (IOException | RuntimeException ex) {
                    showError("No se pudo abrir la carpeta del video exportado", ex);
                }
            }
        }
    }

    private static String formattedFileSize(Path file) {
        try {
            long bytes = Files.size(file);
            if (bytes < 1024L) {
                return bytes + " B";
            }
            double kibibytes = bytes / 1024.0;
            if (kibibytes < 1024.0) {
                return String.format(java.util.Locale.ROOT, "%.1f KB", kibibytes);
            }
            double mebibytes = kibibytes / 1024.0;
            if (mebibytes < 1024.0) {
                return String.format(java.util.Locale.ROOT, "%.1f MB", mebibytes);
            }
            return String.format(java.util.Locale.ROOT, "%.2f GB", mebibytes / 1024.0);
        } catch (IOException ex) {
            return "no disponible";
        }
    }

    private void exportTheatreWorkInBackground(File file, VideoExportOptions options) {
        if (file == null || options == null) {
            return;
        }
        if (!saveProjectBeforeBackgroundExport("No se pudo guardar el proyecto antes de exportar la obra teatral")) {
            return;
        }
        videoExportProgressCoordinator.export(owner(), file.toPath(), options,
                (targetFile, selectedOptions, progress, cancellationRequested) ->
                        viewModel.exportTheatreWorkVideo(targetFile,
                                selectedOptions.resolution(),
                                selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                TheatreExportScope.all(),
                                selectedOptions.renderUnassignedVisuals(),
                                selectedOptions.includeInferredFrames(),
                                progress,
                                cancellationRequested),
                ex -> showError("No se pudo exportar el video teatral limpio", ex));
    }

    private void exportTheatreSpatialVideoInBackground(File file, TheatreMapExportOptions options) {
        if (file == null || options == null) {
            return;
        }
        if (!saveProjectBeforeBackgroundExport("No se pudo guardar el proyecto antes de exportar el video mapa")) {
            return;
        }
        videoExportProgressCoordinator.export(owner(), file.toPath(), options.videoOptions(),
                (targetFile, selectedOptions, progress, cancellationRequested) ->
                        viewModel.exportTheatreSpatialVideo(targetFile,
                                selectedOptions.resolution(),
                                selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                options.companionMode(),
                                selectedOptions.includeInferredFrames(),
                                progress,
                                cancellationRequested),
                ex -> showError("No se pudo exportar el video mapa", ex));
    }

    private void exportTheatrePortionInBackground(File file, TheatrePortionExportOptions options) {
        if (file == null || options == null) {
            return;
        }
        if (!saveProjectBeforeBackgroundExport("No se pudo guardar el proyecto antes de exportar la porcion de obra")) {
            return;
        }
        videoExportProgressCoordinator.export(owner(), file.toPath(), options.videoOptions(),
                (targetFile, selectedOptions, progress, cancellationRequested) -> {
                    if (options.output() == TheatrePortionExportOptions.Output.THEATRE_MAP) {
                        viewModel.exportTheatreSpatialVideo(targetFile,
                                selectedOptions.resolution(),
                                selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                options.scope(),
                                options.companionMode(),
                                selectedOptions.includeInferredFrames(),
                                progress,
                                cancellationRequested);
                    } else {
                        viewModel.exportTheatreWorkVideo(targetFile,
                                selectedOptions.resolution(),
                                selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                options.scope(),
                                selectedOptions.renderUnassignedVisuals(),
                                selectedOptions.includeInferredFrames(),
                                progress,
                                cancellationRequested);
                    }
                },
                ex -> showError("No se pudo exportar la porcion de obra", ex));
    }

    private boolean saveProjectBeforeBackgroundExport(String errorTitle) {
        try {
            viewModel.saveCurrentProject();
            return true;
        } catch (IOException | RuntimeException ex) {
            showError(errorTitle, ex);
            return false;
        }
    }

    private VideoExportOptions documentStudyVideoOptions(DocumentTextVideoOptions textOptions) {
        OperationalSettings settings = currentOperationalSettings();
        List<VideoEncoderPolicy> policies = availableVideoEncoderPolicies(settings);
        return new VideoExportOptions(
                (textOptions == null ? DocumentTextVideoOptions.defaults() : textOptions).resolution(),
                30,
                defaultVideoEncoderPolicy(settings, policies));
    }

    private Optional<VideoExportOptions> chooseVideoResolution() {
        OperationalSettings settings = currentOperationalSettings();
        List<VideoEncoderPolicy> encoderPolicies = availableVideoEncoderPolicies(settings);
        return videoExportOptionsDialog.show(owner(), encoderPolicies, defaultVideoEncoderPolicy(settings, encoderPolicies));
    }

    private static String theatreExportStatus(com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreWorkExportOptions options) {
        String map = options.showSpatialMap() ? "con mapa espacial si está preparado" : "sin mapa espacial lateral";
        String text = options.showText() ? "con texto" : "sin texto";
        String images = options.useFragmentImages() ? "usando imágenes de fragmentos cuando existan" : "con fondo teatral";
        return "Exportando obra teatral " + text + ", " + images + " y " + map + ".";
    }

    private OperationalSettings currentOperationalSettings() {
        try {
            return viewModel.administrationWorkspace().settings().loadOperationalSettings().load();
        } catch (IOException | RuntimeException ex) {
            return OperationalSettings.defaults();
        }
    }

    private List<VideoEncoderPolicy> availableVideoEncoderPolicies(OperationalSettings settings) {
        java.util.LinkedHashSet<VideoEncoderPolicy> policies = new java.util.LinkedHashSet<>();
        policies.add(VideoEncoderPolicy.CPU_X264);
        try {
            var report = viewModel.administrationWorkspace().settings().inspectComputeEnvironment().inspect(settings);
            for (var device : report.devices()) {
                addVideoEncoderForDevice(policies, device.id(), device.vendor(), device.displayName());
            }
        } catch (RuntimeException ex) {
            // If hardware discovery fails, keep export usable without advertising unavailable GPUs.
        }
        policies.add(VideoEncoderPolicy.AUTO);
        return List.copyOf(policies);
    }

    private static void addVideoEncoderForDevice(java.util.LinkedHashSet<VideoEncoderPolicy> policies, String... parts) {
        StringBuilder markerBuilder = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                markerBuilder.append(part == null ? "" : part).append(' ');
            }
        }
        String marker = markerBuilder.toString().toLowerCase(java.util.Locale.ROOT);
        if (marker.contains("nvidia") || marker.contains("geforce") || marker.contains("gtx") || marker.contains("rtx")) {
            policies.add(VideoEncoderPolicy.NVIDIA_NVENC);
        } else if (marker.contains("intel") || marker.contains("iris") || marker.contains("uhd") || marker.contains("arc")) {
            policies.add(VideoEncoderPolicy.INTEL_QSV);
        } else if (marker.contains("amd") || marker.contains("radeon")) {
            policies.add(VideoEncoderPolicy.AMD_AMF);
        }
    }

    private static VideoEncoderPolicy defaultVideoEncoderPolicy(OperationalSettings settings, List<VideoEncoderPolicy> availablePolicies) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        List<VideoEncoderPolicy> available = availablePolicies == null || availablePolicies.isEmpty()
                ? List.of(VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.AUTO)
                : availablePolicies;
        VideoEncoderPolicy configured = current.compute().videoEncoderPolicy();
        if (configured != VideoEncoderPolicy.AUTO && available.contains(configured)) {
            return configured;
        }
        if (!current.compute().allowGpuForVideo()) {
            return VideoEncoderPolicy.CPU_X264;
        }
        String selected = current.compute().selectedDeviceId().toLowerCase(java.util.Locale.ROOT);
        if (selected.startsWith("gpu-nvidia")) {
            return available.contains(VideoEncoderPolicy.NVIDIA_NVENC)
                    ? VideoEncoderPolicy.NVIDIA_NVENC
                    : VideoEncoderPolicy.CPU_X264;
        }
        if (selected.startsWith("gpu-intel")) {
            return available.contains(VideoEncoderPolicy.INTEL_QSV)
                    ? VideoEncoderPolicy.INTEL_QSV
                    : VideoEncoderPolicy.CPU_X264;
        }
        if (selected.startsWith("gpu-amd")) {
            return available.contains(VideoEncoderPolicy.AMD_AMF)
                    ? VideoEncoderPolicy.AMD_AMF
                    : VideoEncoderPolicy.CPU_X264;
        }
        return VideoEncoderPolicy.CPU_X264;
    }

    public void handleOpenSettings() {
        settingsDialog.show(owner(), viewModel.administrationWorkspace().settings(),
                SettingsSupportActions.of(commandDispatcher::canDispatch, this::dispatchCommand));
    }

    public void handleOpenVoiceEngineSettings() {
        settingsDialog.showVoiceEngines(owner(), viewModel.administrationWorkspace().settings());
    }

    public void handleOpenOcrSettings() {
        settingsDialog.showVoiceEngines(owner(), viewModel.administrationWorkspace().settings());
        retryVisiblePdfTextPreparation();
    }

    private void retryVisiblePdfTextPreparation() {
        Node view = workspaceRegistry.viewFor(viewModel.activeWorkspaceProperty().get());
        if (view instanceof DocumentWorkspaceView documentWorkspaceView) {
            documentWorkspaceView.retryVisiblePdfTextPreparation();
        }
    }

    public void handleOpenFirstUseSetup() {
        settingsDialog.showFirstUseSetup(owner(), viewModel.administrationWorkspace().settings());
    }

    public void handleOpenGuide() {
        new GuideDialog(viewModel.administrationWorkspace().guide()).show(owner());
    }

    public void handleOpenGuideTopic(GuideTopicId topicId) {
        new GuideDialog(viewModel.administrationWorkspace().guide()).showTopic(owner(), topicId);
    }

    public void handleExportAiResources() {
        DirectoryChooser chooser = NativeSourceChooser.directoryChooser();
        chooser.setTitle("Exportar recursos IA de DocuPodcast");
        File folder = chooser.showDialog(owner());
        if (folder == null) {
            return;
        }
        try {
            var result = viewModel.administrationWorkspace().resources().exportAiResources().export(folder.toPath());
            exportAiResourcesResultDialog.show(owner(), result);
        } catch (IOException | RuntimeException ex) {
            showError("No se pudieron exportar los recursos IA", ex);
        }
    }

    private boolean confirmDiscardOrSaveIfNeeded() {
        if (viewModel.audioJobRunningProperty().get()) {
            activeAudioJobDialog.show(owner());
            return false;
        }
        if (!viewModel.hasUnsavedChanges()) {
            return true;
        }
        UnsavedChangesDialog.Decision decision = unsavedChangesDialog.show(owner());
        if (decision == UnsavedChangesDialog.Decision.CANCEL) {
            return false;
        }
        if (decision == UnsavedChangesDialog.Decision.SAVE) {
            return handleSaveProject();
        }
        return true;
    }

    private void activate(WorkspaceKind workspaceKind) {
        WorkspaceKind resolved = workspaceRouteResolver.resolve(workspaceKind);
        long sequence = ++workspaceActivationSequence;
        if (!workspaceRegistry.isCached(resolved) && (resolved == WorkspaceKind.VOICE_LIBRARY
                || resolved == WorkspaceKind.SETTINGS || resolved == WorkspaceKind.THEATRE_IMAGE_GENERATION
                || resolved == WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION)) {
            Label loading = new Label("Cargando " + resolved.displayName().toLowerCase(java.util.Locale.ROOT) + "…");
            workspaceHost.getChildren().setAll(loading);
            javafx.animation.PauseTransition pending = new javafx.animation.PauseTransition(javafx.util.Duration.millis(40));
            pending.setOnFinished(event -> {
                if (sequence != workspaceActivationSequence) return;
                try {
                    workspaceHost.getChildren().setAll(workspaceRegistry.viewFor(resolved));
                } catch (RuntimeException ex) {
                    loading.setText("No se pudo cargar esta vista. Vuelve a abrirla para reintentar.");
                    showError("No se pudo cargar la vista", ex);
                }
            });
            pending.play();
            return;
        }
        Node view = workspaceRegistry.viewFor(resolved);
        workspaceHost.getChildren().setAll(view);
    }

    private FileChooser projectFileChooser(String title) {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Proyecto DocuPodcast (*.docupodcast.json)", "*.docupodcast.json"));
        return chooser;
    }

    private void promptSaveProjectForImportedSourceIfNeeded(Path sourceFile) {
        if (viewModel.currentProjectFile().isPresent()) {
            return;
        }
        if (!viewModel.projectOpenProperty().get()) {
            return;
        }
        handleSaveProjectAs(defaultProjectFileForSource(sourceFile));
    }

    private Path defaultProjectFileForSource(Path sourceFile) {
        Path normalized = sourceFile == null ? null : sourceFile.toAbsolutePath().normalize();
        Path folder = normalized == null ? null : normalized.getParent();
        if (folder == null) {
            folder = Path.of(System.getProperty("user.home", "."));
        }
        return folder.resolve(safeProjectBaseName(sourceFile) + ".docupodcast.json");
    }

    private static String safeProjectBaseName(Path sourceFile) {
        String fileName = sourceFile == null || sourceFile.getFileName() == null
                ? "proyecto-docupodcast"
                : sourceFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            fileName = fileName.substring(0, dot);
        }
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]+", " ").strip();
        return safeName.isBlank() ? "proyecto-docupodcast" : safeName;
    }

    private void configureInitialProjectSaveLocation(FileChooser chooser, Path suggestedFile) {
        if (chooser == null || suggestedFile == null) {
            return;
        }
        Path normalized = suggestedFile.toAbsolutePath().normalize();
        Path folder = normalized.getParent();
        if (folder != null && Files.isDirectory(folder)) {
            chooser.setInitialDirectory(folder.toFile());
        }
        Path fileName = normalized.getFileName();
        if (fileName != null) {
            chooser.setInitialFileName(fileName.toString());
        }
    }

    private DocumentVideoBatchExecutionPort batchExecutionPort() {
        return new DocumentVideoBatchExecutionPort() {
            @Override public void start(DocumentVideoBatchProject project, Path descriptor, Listener listener) {
                startBatchProduction(project, descriptor, listener);
            }
            @Override public void requestPause() {
                if (activeBatchExecution != null) {
                    activeBatchExecution.pauseRequested = true;
                    viewModel.updateStatusMessage("La cola Express se pausará al terminar el paso seguro actual.");
                }
            }
            @Override public void cancelCurrent() {
                if (activeBatchExecution != null) {
                    activeBatchExecution.cancelRequested = true;
                    if (activeBatchExecution.audioExportThread != null) activeBatchExecution.audioExportThread.interrupt();
                    videoExportProgressCoordinator.cancelBackgroundExport();
                    viewModel.cancelNarrationTranslationAnalysis();
                    viewModel.cancelCurrentAudioOperation();
                    wordSemanticPreparation.cancelLocalAnalysis();
                    pdfNarratablePreparation.cancelActivePreparationSession();
                    viewModel.updateStatusMessage("Cancelación del documento actual solicitada; la cola conservará los derivados válidos.");
                }
            }
            @Override public void cancelAll() {
                BatchExecutionSession session = activeBatchExecution;
                if (session == null) return;
                session.cancelRequested = true;
                session.cancelAllRequested = true;
                if (session.audioExportThread != null) session.audioExportThread.interrupt();
                videoExportProgressCoordinator.cancelBackgroundExport();
                viewModel.cancelNarrationTranslationAnalysis();
                viewModel.cancelCurrentAudioOperation();
                wordSemanticPreparation.cancelLocalAnalysis();
                pdfNarratablePreparation.cancelActivePreparationSession();
                try {
                    DocumentVideoBatchProject latest = batchRepository.open(session.descriptor);
                    session.project = batchQueue.cancelAll(latest, session.descriptor);
                    session.listener.projectChanged(session.project);
                } catch (IOException failure) {
                    LOGGER.error("No se pudo persistir la cancelación completa de la cola Express", failure);
                }
                // Wait for the active worker's callback before restoring the editor.
                if (session.currentItemId.isBlank()) advanceBatchProduction();
            }
            @Override public boolean running() { return activeBatchExecution != null; }
            @Override public EngineConfiguration engineConfiguration() {
                var platform = viewModel.mediaEnginePlatform();
                List<EngineChoice> voices = new ArrayList<>();
                voices.add(new EngineChoice("", "Usar la configuración general", "", true));
                new ListVoiceEngineOperationalStatesUseCase(platform).list().forEach(state ->
                        voices.add(new EngineChoice(state.engineId(), expressVoiceEngineName(state.engineId(), state.displayName()),
                                state.statusLabel(), state.ready() && state.realTts())));
                List<EngineChoice> ai = new ArrayList<>();
                ai.add(new EngineChoice("", "Automático (motor compatible)", "", true));
                platform.contentAnalysisEngines().supporting(ContentAnalysisOperation.IMAGE_DESCRIPTION)
                        .forEach(engine -> {
                            var readiness = engine.inspectReadiness(null);
                            ai.add(new EngineChoice(engine.descriptor().id().value(),
                                    expressAiEngineName(engine.descriptor().id().value(), engine.descriptor().displayName()),
                                    readiness.ready() ? "Listo" : "Requiere reparación",
                                    readiness.ready()));
                        });
                EngineId selectedVoice = SelectedMediaEngines.from(currentOperationalSettings()).voice();
                return new EngineConfiguration(voices, ai,
                        selectedVoice == null ? "" : selectedVoice.value(), "");
            }
            @Override public void selectGlobalVoiceEngine(String engineId) throws IOException {
                persistGlobalVoiceEngine(engineId);
            }
        };
    }

    private static String expressVoiceEngineName(String id, String fallback) {
        return fallback;
    }

    private static String expressAiEngineName(String id, String fallback) {
        return fallback;
    }

    private void startBatchProduction(DocumentVideoBatchProject project, Path descriptor,
                                      DocumentVideoBatchExecutionPort.Listener listener) {
        if (activeBatchExecution != null) {
            viewModel.updateStatusMessage("Ya hay una cola Express en ejecución.");
            return;
        }
        expressNotificationOwner = listener.notificationOwner();
        if (!project.profile().audioOnly() && !ensureVideoRendererAvailable()) return;
        try {
            Path normalizedDescriptor = descriptor.toAbsolutePath().normalize();
            DocumentVideoBatchProject recovered = batchQueue.recoverInterrupted(
                    batchRepository.open(normalizedDescriptor), normalizedDescriptor);
            // Express shares the current DocuPodcast voice selection. The ID stored in
            // older batch descriptors is only historical and must not override a newer
            // choice made later in the main settings surface.
            viewModel.preferVoiceEngineForCurrentOperation("");
            activeBatchExecution = new BatchExecutionSession(normalizedDescriptor, recovered, listener);
            listener.projectChanged(recovered);
            advanceBatchProduction();
        } catch (IOException | RuntimeException failure) {
            showError("No se pudo iniciar la producción por lotes", failure);
        }
    }

    private void persistGlobalVoiceEngine(String engineId) throws IOException {
        String requested = engineId == null ? "" : engineId.strip();
        if (requested.isBlank()) return;
        var engine = viewModel.mediaEnginePlatform().voiceEngines().find(new EngineId(requested))
                .orElseThrow(() -> new IOException("El motor de voz no está registrado: " + requested));
        var readiness = engine.inspectReadiness(null);
        if (!readiness.ready()) {
            throw new IOException("El motor de voz «" + engine.descriptor().displayName()
                    + "» no está listo: " + readiness.summary());
        }
        OperationalSettings current = currentOperationalSettings();
        OperationalSettings.MediaEngineSelectionSettings selected = current.mediaEngines();
        if (requested.equalsIgnoreCase(selected.voiceEngineId())) return;
        OperationalSettings updated = new OperationalSettings(
                current.readingDocument(), current.playbackBuffer(), current.tts(), current.video(),
                current.imageGeneration(), current.imageSuperResolution(),
                new OperationalSettings.MediaEngineSelectionSettings(requested, selected.imageEngineId(),
                        selected.videoGenerationEngineId(), selected.videoRenderEngineId()),
                current.frameGeneration(), current.compute(), current.ocr(), current.storage(), current.diagnostics());
        var report = viewModel.administrationWorkspace().settings().saveOperationalSettings().save(updated);
        if (!report.errors().isEmpty()) {
            throw new IOException("No se guardó el motor de voz: " + String.join(" ", report.errors()));
        }
        viewModel.updateStatusMessage("Motor de voz global: " + requested + ".");
    }

    private void advanceBatchProduction() {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null) return;
        try {
            session.project = batchRepository.open(session.descriptor);
            if (session.cancelAllRequested) {
                session.project = batchQueue.cancelAll(session.project, session.descriptor);
                finishBatchProduction(false, "Producción completa cancelada; los archivos terminados se conservaron.");
                return;
            }
            if (session.pauseRequested) {
                finishBatchProduction(true, "La cola quedó pausada y puede continuarse después.");
                return;
            }
            Optional<DocumentVideoBatchItem> next = session.project.items().stream()
                    .filter(item -> item.state() == BatchItemState.PENDING)
                    .sorted(Comparator.comparingInt(DocumentVideoBatchItem::order))
                    .findFirst();
            if (next.isEmpty()) {
                boolean hasPaused = session.project.items().stream().anyMatch(item ->
                        item.state() == BatchItemState.PAUSED
                                || item.state() == BatchItemState.PAUSE_REQUESTED);
                finishBatchProduction(hasPaused, hasPaused
                        ? "No quedan documentos pendientes; reanuda los elementos pausados para continuar."
                        : "Todos los documentos disponibles alcanzaron un estado terminal.");
                return;
            }
            session.currentItemId = next.get().id();
            session.cancelRequested = false;
            Path existingOutput = safeBatchResolve(session.descriptor.getParent(),
                    next.get().outputVideoRelativePath());
            if (!session.project.profile().audioOnly() && batchVideoVerifier.verify(existingOutput)) {
                transitionBatch(BatchItemState.COMPLETED, BatchItemStage.FINISHED,
                        1.0, "MP4 existente verificado y reutilizado: " + existingOutput.getFileName());
                session.currentItemId = "";
                javafx.application.Platform.runLater(this::advanceBatchProduction);
                return;
            }
            transitionBatch(BatchItemState.RUNNING, BatchItemStage.DOCUMENT_PREPARATION,
                    0.04, "Abriendo el proyecto documental");
            openAndPrepareBatchItem(next.get());
        } catch (IOException | RuntimeException failure) {
            failCurrentBatchItem(failure);
        }
    }

    private void openAndPrepareBatchItem(DocumentVideoBatchItem item) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null) return;
        try {
            Path root = session.descriptor.getParent();
            Path childDescriptor = safeBatchResolve(root, item.childProjectRelativePath());
            Path copiedSource = safeBatchResolve(root, item.copiedSourceRelativePath());
            openProjectFile(childDescriptor, () -> {
                expressOpenedChild = true;
                if (viewModel.currentSourceDocumentPath().isPresent()) {
                    applyBatchProfileAndPrepare(item);
                    return;
                }
                transitionBatch(BatchItemState.RUNNING, BatchItemStage.DOCUMENT_PREPARATION,
                        0.08, "Importando la copia protegida de la fuente");
                Task<ProjectDocumentSource> importTask = new Task<>() {
                    @Override protected ProjectDocumentSource call() throws Exception {
                        return viewModel.importAndClassifySourceDocument(copiedSource);
                    }
                };
                importTask.setOnSucceeded(event -> {
                    try {
                        if (batchCancellationCheckpoint()) return;
                        viewModel.attachImportedDocument(importTask.getValue());
                        viewModel.saveCurrentProject();
                        applyBatchProfileAndPrepare(item);
                    } catch (IOException | RuntimeException failure) {
                        failCurrentBatchItem(failure);
                    }
                });
                importTask.setOnFailed(event ->
                        failCurrentBatchItem(importTask.getException()));
                backgroundTaskRunner.start("docupodcast-batch-source-import", importTask);
            }, this::failCurrentBatchItem);
        } catch (IOException | RuntimeException failure) {
            failCurrentBatchItem(failure);
        }
    }

    private void applyBatchProfileAndPrepare(DocumentVideoBatchItem item) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || batchCancellationCheckpoint()) return;
        try {
            var semanticPolicy = session.project.profile().interpretImages()
                    ? com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF
                    : com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS;
            viewModel.setDocumentListeningPreferences(viewModel.documentListeningPreferences()
                    .withSecondarySemanticPolicy(semanticPolicy));
            if (!session.project.profile().audioOnly()) {
            var projection = viewModel.currentDocumentContentProjection().orElse(null);
            var configuration = viewModel.documentaryVideoConfiguration()
                    .withDefaultSecondarySemanticDuration(session.project.profile().imageSlideSeconds());
            if (projection != null) {
                for (var content : projection.items()) {
                    var slide = configuration.content(content.contentId())
                            .orElseGet(() -> com.marcosmoreiradev.docupodcaststudio.domain.study
                                    .DocumentVideoSlideConfiguration.empty(content.contentId()));
                    configuration = configuration.withContent(
                            slide.withDuration(session.project.profile().imageSlideSeconds()));
                }
            }
            var branding = session.project.profile().branding();
            if (branding.enabled() && projection != null) {
                Path logo = safeBatchResolve(session.descriptor.getParent(), branding.projectRelativePath());
                var asset = viewModel.importDocumentaryVideoImage(logo);
                var position = branding.placement() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BrandingPlacement.BOTTOM_LEFT
                        || branding.placement() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BrandingPlacement.TOP_LEFT
                        ? com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition.BOTTOM_LEFT
                        : com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition.BOTTOM_RIGHT;
                for (var content : projection.items()) {
                    var slide = configuration.content(content.contentId())
                            .orElseGet(() -> com.marcosmoreiradev.docupodcaststudio.domain.study
                                    .DocumentVideoSlideConfiguration.empty(content.contentId()));
                    configuration = configuration.withContent(slide.withVisual(
                            slide.visual().withMascot(asset.id(), position, branding.sizePercent())));
                }
            }
            viewModel.updateDocumentaryVideoConfiguration(configuration);
            }
            viewModel.saveCurrentProject();
            transitionBatch(BatchItemState.RUNNING, BatchItemStage.DOCUMENT_PREPARATION,
                    0.14, "Preparando semántica y lectura del documento completo");
            prepareDocumentForBatchExportThenRun(
                    () -> prepareBatchAudioAndVideo(item),
                    this::failCurrentBatchItem);
        } catch (IOException | RuntimeException failure) {
            failCurrentBatchItem(failure);
        }
    }

    /** Batch counterpart of the interactive preparation gate: no modal decisions. */
    private void prepareDocumentForBatchExportThenRun(
            Runnable continuation, java.util.function.Consumer<Throwable> failureHandler) {
        Runnable afterWord = () -> {
            if (viewModel.currentDocumentProperty().get() != null) {
                viewModel.buildNarrationScriptFromDocument();
                continuation.run();
                return;
            }
            BatchExecutionSession session = activeBatchExecution;
            String aiEngineId = session == null ? "" : session.project.profile().aiEngineId();
            pdfNarratablePreparation.prepareCompleteReadingForBatchThenRun(() -> {
                viewModel.buildNarrationScriptFromDocument();
                continuation.run();
            }, failureHandler, aiEngineId);
        };
        BatchExecutionSession session = activeBatchExecution;
        String aiEngineId = session == null ? "" : session.project.profile().aiEngineId();
        wordSemanticPreparation.prepareForBatchThenRun(afterWord, failureHandler, aiEngineId);
    }

    private void prepareBatchAudioAndVideo(DocumentVideoBatchItem item) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || batchCancellationCheckpoint()) return;
        try {
            if (pauseCurrentBatchItemIfRequested()) return;
            DocumentExportReadinessSnapshot readiness = viewModel.inspectDocumentExportReadiness();
            Runnable export = () -> {
                transitionBatch(BatchItemState.RUNNING, BatchItemStage.AUDIO_VERIFICATION,
                        0.50, "Audio vigente verificado; preparando la salida final");
                exportCurrentBatchItem(item);
            };
            if (readiness.readyToRender() || readiness.compositionOnlyPending()) {
                export.run();
                return;
            }
            transitionBatch(BatchItemState.RUNNING, BatchItemStage.AUDIO_GENERATION,
                    0.20, "Generando y verificando la voz faltante");
            session.awaitingAudio = true;
            PreparedExportIntent intent = new PreparedExportIntent(
                    session.project.profile().audioOnly() ? AppCommandId.EXPORT_PODCAST_WAV : AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO,
                    ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT,
                    safeBatchResolve(session.descriptor.getParent(), item.outputVideoRelativePath()),
                    session.project.profile().outputLabel() + " por lotes");
            viewModel.submitAudioGenerationWithPendingExport(readiness, intent, () -> {
                BatchExecutionSession current = activeBatchExecution;
                if (current == null) return;
                current.awaitingAudio = false;
                if (pauseCurrentBatchItemIfRequested() || batchCancellationCheckpoint()) return;
                export.run();
            }, failure -> {
                session.awaitingAudio = false;
                Task<Void> waitForWorker = new Task<>() {
                    @Override protected Void call() throws Exception {
                        viewModel.awaitNarrationTranslationStopped();
                        return null;
                    }
                };
                waitForWorker.setOnSucceeded(event -> {
                    if (activeBatchExecution == session && session.currentItemId.equals(item.id())) failCurrentBatchItem(failure);
                });
                waitForWorker.setOnFailed(event -> {
                    if (activeBatchExecution == session && session.currentItemId.equals(item.id())) failCurrentBatchItem(waitForWorker.getException());
                });
                backgroundTaskRunner.start("docupodcast-batch-audio-stop", waitForWorker);
            });
        } catch (IOException | RuntimeException failure) {
            failCurrentBatchItem(failure);
        }
    }

    private void exportCurrentBatchItem(DocumentVideoBatchItem item) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || batchCancellationCheckpoint()) return;
        try {
            Path target = safeBatchResolve(session.descriptor.getParent(), item.outputVideoRelativePath());
            Files.createDirectories(target.getParent());
            if (session.project.profile().audioOnly()) {
                exportBatchAudio(session, target);
                return;
            }
            transitionBatch(BatchItemState.RUNNING, BatchItemStage.VISUAL_PLAN,
                    0.52, "Construyendo el plan visual del documento");
            var effectiveVideo = session.project.profile().effectiveVideo(item.sourceRelativePath());
            if (effectiveVideo.backgroundMode() == com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoBackgroundMode.IMAGE
                    && !Files.isRegularFile(Path.of(effectiveVideo.backgroundImagePath()))) {
                throw new java.io.IOException("Imagen de fondo no disponible para " + item.title() + ": " + effectiveVideo.backgroundImagePath());
            }
            VideoExportOptions options = documentStudyVideoOptions(effectiveVideo);
            exportDocumentStudyTextAudioVideoInBackground(target.toFile(), options,
                    effectiveVideo, exported -> completeCurrentBatchItem(exported),
                    this::failCurrentBatchItem, progress -> {
                        BatchExecutionSession current = activeBatchExecution;
                        if (current == null) return;
                        viewModel.acceptDocumentExportProgress(progress);
                        double ratio = 0.55 + (0.40 * progress.ratio());
                        long now = System.nanoTime();
                        if (ratio - current.lastPersistedProgress >= 0.02
                                || now - current.lastProgressPersistNanos > 2_000_000_000L) {
                            current.lastPersistedProgress = ratio;
                            current.lastProgressPersistNanos = now;
                            transitionBatch(BatchItemState.RUNNING,
                                    progress.stage() == com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderStage.VERIFYING_OUTPUT
                                            ? BatchItemStage.VIDEO_VERIFICATION : BatchItemStage.VIDEO_RENDER,
                                    ratio, progress.currentStep());
                        }
                    });
        } catch (IOException | RuntimeException failure) {
            failCurrentBatchItem(failure);
        }
    }

    private void completeCurrentBatchItem(Path target) {
        try {
            if (activeBatchExecution == null || batchCancellationCheckpoint()) return;
            if (!batchVideoVerifier.verify(target)) {
                throw new IOException("La exportación no produjo un MP4 verificable: " + target);
            }
            transitionBatch(BatchItemState.COMPLETED, BatchItemStage.FINISHED,
                    1.0, "MP4 verificado: " + target.getFileName());
            BatchExecutionSession session = activeBatchExecution;
            if (session != null) session.currentItemId = "";
            advanceBatchProduction();
        } catch (IOException | RuntimeException failure) {
            failCurrentBatchItem(failure);
        }
    }

    private void exportBatchAudio(BatchExecutionSession session, Path target) throws IOException {
        viewModel.saveCurrentProject();
        transitionBatch(BatchItemState.RUNNING, BatchItemStage.AUDIO_EXPORT,
                0.55, "Exportando audio " + session.project.profile().audioFormat().displayName());
        Task<Void> export = new Task<>() {
            @Override protected Void call() throws Exception {
                session.audioExportThread = Thread.currentThread();
                try {
                    var verifier = new com.marcosmoreiradev.docupodcaststudio.application.batch.VerifyBatchAudioOutputUseCase();
                    if (!verifier.verify(target, session.project.profile().audioFormat())) {
                        if (session.cancelRequested) throw new IOException("Exportación cancelada");
                        viewModel.exportPodcastWavResult(target);
                        if (session.cancelRequested) throw new IOException("Exportación cancelada");
                        verifier.recordCompleted(target, session.project.profile().audioFormat());
                    }
                    return null;
                } finally { session.audioExportThread = null; }
            }
        };
        export.setOnSucceeded(event -> {
            if (activeBatchExecution != session || batchCancellationCheckpoint()) return;
            transitionBatch(BatchItemState.RUNNING, BatchItemStage.AUDIO_OUTPUT_VERIFICATION, 0.98,
                    "Audio exportado y verificado");
            transitionBatch(BatchItemState.COMPLETED, BatchItemStage.FINISHED, 1.0,
                    "Audio verificado: " + target.getFileName());
            session.currentItemId = "";
            advanceBatchProduction();
        });
        export.setOnFailed(event -> {
            if (activeBatchExecution == session) failCurrentBatchItem(export.getException());
        });
        backgroundTaskRunner.start("docupodcast-batch-audio-export", export);
    }

    private void handleBatchAudioStatus(AudioJobStatusDto status) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || !session.awaitingAudio || status == null) return;
        if (status.running()) {
            double progress = 0.20 + (0.30 * status.progress());
            long now = System.nanoTime();
            if (progress - session.lastPersistedProgress >= 0.02
                    || now - session.lastProgressPersistNanos > 2_000_000_000L) {
                session.lastPersistedProgress = progress;
                session.lastProgressPersistNanos = now;
                transitionBatch(BatchItemState.RUNNING, BatchItemStage.AUDIO_GENERATION,
                        progress, status.statusLine());
            }
        }
    }

    private boolean pauseCurrentBatchItemIfRequested() {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || session.currentItemId.isBlank()) return false;
        try {
            DocumentVideoBatchProject latest = batchRepository.open(session.descriptor);
            Optional<DocumentVideoBatchItem> current = latest.items().stream()
                    .filter(item -> item.id().equals(session.currentItemId)).findFirst();
            if (current.isPresent() && current.get().state() == BatchItemState.PAUSE_REQUESTED) {
                session.project = latest;
                transitionBatch(BatchItemState.PAUSED, current.get().stage(), current.get().progress(),
                        "Pausado en un punto seguro; los derivados válidos se conservaron");
                session.currentItemId = "";
                advanceBatchProduction();
                return true;
            }
        } catch (IOException failure) {
            failCurrentBatchItem(failure);
            return true;
        }
        return false;
    }

    private boolean batchCancellationCheckpoint() {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || !session.cancelRequested) return false;
        transitionBatch(BatchItemState.CANCELLED, BatchItemStage.FINISHED,
                session.lastPersistedProgress, "Cancelado por el usuario; derivados válidos conservados");
        session.currentItemId = "";
        advanceBatchProduction();
        return true;
    }

    private void failCurrentBatchItem(Throwable failure) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null) return;
        session.awaitingAudio = false;
        BatchItemState terminal = session.cancelRequested ? BatchItemState.CANCELLED : BatchItemState.FAILED;
        String message = failure == null ? "Error desconocido" : rootCauseMessage(failure);
        transitionBatch(terminal, BatchItemStage.FINISHED,
                session.lastPersistedProgress, message);
        session.currentItemId = "";
        javafx.application.Platform.runLater(this::advanceBatchProduction);
    }

    private void transitionBatch(BatchItemState state, BatchItemStage stage,
                                 double progress, String message) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null || session.currentItemId.isBlank()) return;
        if (session.cancelRequested && state == BatchItemState.RUNNING) return;
        try {
            DocumentVideoBatchProject latest = batchRepository.open(session.descriptor);
            session.project = batchQueue.transition(latest, session.descriptor,
                    session.currentItemId, state, stage, progress, message);
            session.listener.projectChanged(session.project);
        } catch (IOException failure) {
            LOGGER.error("No se pudo persistir el checkpoint de la cola Express", failure);
        }
    }

    private void finishBatchProduction(boolean paused, String message) {
        BatchExecutionSession session = activeBatchExecution;
        if (session == null) return;
        activeBatchExecution = null;
        viewModel.preferVoiceEngineForCurrentOperation("");
        DocumentVideoBatchProject project = session.project;
        int completed = (int) project.items().stream().filter(item -> item.state() == BatchItemState.COMPLETED).count();
        int failed = (int) project.items().stream().filter(item -> item.state() == BatchItemState.FAILED).count();
        int skipped = (int) project.items().stream().filter(item -> item.state() == BatchItemState.SKIPPED).count();
        int cancelled = (int) project.items().stream().filter(item -> item.state() == BatchItemState.CANCELLED).count();
        try {
            batchReportWriter.write(project, session.descriptor, paused, message);
        } catch (IOException reportFailure) {
            LOGGER.warn("No se pudo escribir el informe final de la cola Express", reportFailure);
        }
        session.listener.finished(project, new DocumentVideoBatchExecutionPort.BatchRunResult(
                completed, failed, skipped, cancelled, paused, message));
        viewModel.updateStatusMessage(paused ? "Cola Express pausada." : "Cola Express terminada.");
    }

    private static Path safeBatchResolve(Path root, String relative) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relative).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Ruta insegura fuera del proyecto por lotes: " + relative);
        }
        return resolved;
    }


    private static String rootCauseMessage(Throwable failure) {
        Throwable cursor = failure;
        while (cursor != null && cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String value = cursor == null ? "Error desconocido" : cursor.getMessage();
        return value == null || value.isBlank() ? cursor.getClass().getSimpleName() : value;
    }

    private static final class BatchExecutionSession {
        private final Path descriptor;
        private final DocumentVideoBatchExecutionPort.Listener listener;
        private DocumentVideoBatchProject project;
        private String currentItemId = "";
        private boolean pauseRequested;
        private volatile boolean cancelRequested;
        private boolean cancelAllRequested;
        private volatile Thread audioExportThread;
        private boolean awaitingAudio;
        private double lastPersistedProgress;
        private long lastProgressPersistNanos;
        private BatchExecutionSession(Path descriptor, DocumentVideoBatchProject project,
                                      DocumentVideoBatchExecutionPort.Listener listener) {
            this.descriptor = descriptor;
            this.project = project;
            this.listener = listener;
        }
    }

    private Window owner() {
        if (expressNotificationOwner != null && expressNotificationOwner.isShowing()) return expressNotificationOwner;
        return getScene() == null ? null : getScene().getWindow();
    }


    private void showError(String header, Throwable error) {
        alertPresenter.showFailure(header, error, owner());
    }
}
