package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ProjectGrammarKind;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideTopicId;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectContainerPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
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
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ProjectExportEligibilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DiagnosticUserDecisionFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.ExampleProjectCreationWorkflow;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.GrammarWorkflowCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.PdfNarratablePreparationCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.guide.GuideDialog;
import com.marcosmoreiradev.docupodcaststudio.application.document.SourceDocumentRequirementException;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotificationLevel;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentAudioDefensiveDecisionGuard;
import com.marcosmoreiradev.docupodcaststudio.presentation.settings.EmbeddedDependencySetupAssistant;
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
import com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceLibraryWorkspaceView;
import com.marcosmoreiradev.docupodcaststudio.presentation.welcome.RecentProjectEntry;
import com.marcosmoreiradev.docupodcaststudio.presentation.welcome.RecentProjectsStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.welcome.WelcomeWorkspaceView;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceDescriptorCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceRouteResolver;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceViewRegistry;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
import java.util.prefs.Preferences;

/** Main desktop shell for the onboarding build. */
public final class DocuPodcastShellView extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final StackPane workspaceHost = new StackPane();
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
    private final UnsavedChangesDialog unsavedChangesDialog = new UnsavedChangesDialog();
    private final ExportAiResourcesResultDialog exportAiResourcesResultDialog = new ExportAiResourcesResultDialog();
    private final EmbeddedDependencySetupAssistant dependencySetupAssistant = new EmbeddedDependencySetupAssistant();
    private final SettingsDialog settingsDialog = new SettingsDialog();
    private final FxBackgroundTaskRunner backgroundTaskRunner = new FxBackgroundTaskRunner();
    private final VideoExportOptionsDialog videoExportOptionsDialog = new VideoExportOptionsDialog();
    private final TheatrePortionExportOptionsDialog theatrePortionExportOptionsDialog = new TheatrePortionExportOptionsDialog();
    private final VideoExportProgressCoordinator videoExportProgressCoordinator = new VideoExportProgressCoordinator();
    private final ExportCenterCoordinator exportCenterCoordinator = new ExportCenterCoordinator();
    private final ExportCenterDialog exportCenterDialog = new ExportCenterDialog();
    private final ProjectExportEligibilityPolicy exportEligibilityPolicy = new ProjectExportEligibilityPolicy();
    private final GrammarWorkflowCoordinator grammarWorkflow;
    private final PdfNarratablePreparationCoordinator pdfNarratablePreparation;
    private static final String PREF_HIDE_SOURCE_COPY_NOTICE = "hideProjectSourceCopyNotice";
    private String lastAudioDefensiveDecisionKey = "";
    private String lastAudioFailureKey = "";

    private final AppCommandRegistry commandRegistry = AppCommandRegistry.official();
    private final CommandAvailabilityPolicy commandAvailabilityPolicy = new CommandAvailabilityPolicy();
    private final AppCommandDispatcher commandDispatcher = new AppCommandDispatcher(commandRegistry);
    private final Preferences preferences = Preferences.userNodeForPackage(DocuPodcastShellView.class);
    private final RibbonStateCoordinator ribbonStateCoordinator = new RibbonStateCoordinator(preferences);
    private final BooleanProperty processOverlayExpanded = new SimpleBooleanProperty(true);

    public DocuPodcastShellView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        this.recentProjectsStore = new RecentProjectsStore((projectFile, storedType) -> {
            try {
                return new ProjectModePolicy().resolve(viewModel.applicationServices().project().openProject().open(projectFile)).displayName();
            } catch (IOException | RuntimeException ex) {
                return storedType;
            }
        });
        this.grammarWorkflow = new GrammarWorkflowCoordinator(viewModel, backgroundTaskRunner, alertPresenter, this::owner);
        this.pdfNarratablePreparation = new PdfNarratablePreparationCoordinator(viewModel, backgroundTaskRunner, alertPresenter, this::owner);
        recentProjects.setAll(recentProjectsStore.load());
        getStyleClass().add("app-root");
        registerCommandHandlers();
        initialiseWorkspaces();
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
                viewModel.currentDocumentProperty(),
                viewModel.selectedDocumentBlockIdProperty(),
                viewModel.pdfVisualDocumentProgressProperty(),
                processOverlayExpanded,
                viewModel.currentDocumentProperty().isNotNull().and(viewModel.audioJobRunningProperty().not()),
                this::handleGenerateChunksFromStatusBar,
                this::handleGenerateSelectedChunkFromStatusBar,
                viewModel::resumeMostRecentRecoverableAudioJob,
                viewModel::cancelActiveAudioJob,
                this::handleDeleteAllAudioChunksFromStatusBar,
                this::handleOpenOcrSettings));
        viewModel.activeWorkspaceProperty().addListener((obs, oldValue, newValue) -> activate(newValue));
        viewModel.activeAudioJobStatusProperty().addListener((obs, oldValue, newValue) -> showAudioGenerationFailureIfNeeded(oldValue, newValue));
        activate(viewModel.activeWorkspaceProperty().get());
    }

    public void handleCloseRequest(WindowEvent event) {
        if (!confirmDiscardOrSaveIfNeeded()) {
            event.consume();
        }
    }

    public void runStartupDependencyPreflight() {
        dependencySetupAssistant.runStartupPreflight(owner(), viewModel.applicationServices().settings());
    }

    private void initialiseWorkspaces() {
        workspaceRegistry
                .register(WorkspaceKind.WELCOME_HOME, () -> new WelcomeWorkspaceView(
                        () -> dispatchCommand(AppCommandId.OPEN_SOURCE_DOCUMENT),
                        () -> dispatchCommand(AppCommandId.OPEN_PROJECT),
                        () -> dispatchCommand(AppCommandId.NEW_PROJECT),
                        this::handleOpenTechnicalProblemExpress,
                        () -> dispatchCommand(AppCommandId.OPEN_EXAMPLE_PROJECT),
                        this::handleOpenFirstUseSetup,
                        () -> dispatchCommand(AppCommandId.OPEN_GUIDE),
                        recentProjects,
                        this::handleOpenRecentProject))
                .register(WorkspaceKind.DOCUMENT_READER, () -> new DocumentWorkspaceView(viewModel, this::handleSaveProject,
                        () -> ensureProjectSavedForDocumentAudio("preparar o reproducir la lectura desde la barra flotante"),
                        () -> confirmAudioEngineReadyForDocumentAction(false)))
                .register(WorkspaceKind.THEATRE_SCRIPT, () -> new DocumentWorkspaceView(viewModel, this::handleSaveProject,
                        () -> ensureProjectSavedForDocumentAudio("preparar o reproducir la lectura desde el guión teatral"),
                        () -> confirmAudioEngineReadyForDocumentAction(false),
                        DocumentWorkspaceMode.THEATRE_SCRIPT))
                .register(WorkspaceKind.VOICE_LIBRARY, () -> new VoiceLibraryWorkspaceView(viewModel))
                .register(WorkspaceKind.THEATRE_IMAGE_GENERATION, () -> new TheatreImageGenerationWorkspaceView(viewModel));
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
        LongProcessOverlayView processOverlay = new LongProcessOverlayView(viewModel, processOverlayExpanded);
        installWorkspaceHostClip();
        StackPane center = new StackPane(workspaceHost, processOverlay);
        StackPane.setAlignment(processOverlay, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(processOverlay, new Insets(0, 16, 16, 0));
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
                .register(AppCommandId.OPEN_PROJECT, this::handleOpenProject)
                .register(AppCommandId.SAVE_PROJECT, this::handleSaveProject)
                .register(AppCommandId.SAVE_PROJECT_AS, this::handleSaveProjectAs)
                .register(AppCommandId.CLOSE_PROJECT, this::handleCloseProject)
                .register(AppCommandId.OPEN_PROJECT_FOLDER, this::handleOpenProjectFolder)
                .register(AppCommandId.OPEN_SOURCE_DOCUMENT, this::handleImportWord)
                .register(AppCommandId.IMPORT_THEATRE_GRAMMAR, this::handleImportTheatreGrammar)
                .register(AppCommandId.EXPORT_THEATRE_GRAMMAR_TEMPLATE, this::handleExportTheatreGrammarTemplate)
                .register(AppCommandId.IMPORT_NARRATIVE_VIDEO_GRAMMAR, this::handleImportNarrativeVideoGrammar)
                .register(AppCommandId.EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE, this::handleExportNarrativeVideoGrammarTemplate)
                .register(AppCommandId.OPEN_EXAMPLE_PROJECT, this::handleOpenExampleProject)
                .register(AppCommandId.REFRESH_SOURCE_DOCUMENT, this::handleRefreshSourceDocument)
                .register(AppCommandId.OPEN_SOURCE_DOCUMENT_LOCATION, this::handleOpenSourceDocumentLocation)
                .register(AppCommandId.PREPARE_DOCUMENT_READING,
                        () -> pdfNarratablePreparation.prepareThenRun(viewModel::buildNarrationScriptFromDocument))
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
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("app-menu-bar");

        Menu archivo = new Menu("Archivo");
        MenuItem nuevo = commandItem(AppCommandId.NEW_PROJECT);
        MenuItem abrirProyecto = commandItem(AppCommandId.OPEN_PROJECT);
        Menu abrirRecientes = recentProjectsMenu();
        MenuItem salir = commandItem(AppCommandId.EXIT_APPLICATION);
        archivo.getItems().addAll(nuevo, abrirProyecto, abrirRecientes, new SeparatorMenuItem(), salir);

        Menu proyecto = new Menu("Proyecto");
        MenuItem guardar = commandItem(AppCommandId.SAVE_PROJECT);
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
        teatro.getItems().addAll(guionTeatral, generacionIaTeatral);
        bindModeMenu(teatro, ProjectMode.THEATRE_PRODUCTION);

        Menu exportar = new Menu("Exportar");
        MenuItem centroExportacion = commandItem(AppCommandId.OPEN_EXPORT_CENTER);
        MenuItem exportarPodcast = commandItem(AppCommandId.EXPORT_PODCAST_WAV);
        MenuItem exportarStoryboard = commandItem(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE);
        MenuItem exportarObra = commandItem(AppCommandId.EXPORT_THEATRE_WORK);
        MenuItem exportarMapaTeatral = commandItem(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW);
        MenuItem exportarPorcionTeatral = commandItem(AppCommandId.EXPORT_THEATRE_PORTION);
        MenuItem estadoExportacion = commandItem(AppCommandId.INSPECT_EXPORT_READINESS);
        MenuItem abrirExportaciones = commandItem(AppCommandId.OPEN_EXPORTS_FOLDER);
        exportar.getItems().addAll(centroExportacion, new SeparatorMenuItem(),
                exportarPodcast, exportarStoryboard, exportarObra, exportarMapaTeatral, exportarPorcionTeatral,
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
                exportEligibilityPolicy.evaluate(viewModel.currentDocumentProperty().get());
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
                defaultVideoEncoderPolicy(settings, encoderPolicies));
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
        switch (selection.commandId()) {
            case EXPORT_PODCAST_WAV -> handleExportPodcastWav(selection.audioFormat());
            case EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO -> handleExportDocumentStudyTextAudioVideo(selection.documentTextVideoOptions());
            case EXPORT_SIMPLE_VIDEO_PACKAGE -> handleExportSimpleVideo(selection.videoOptions());
            case EXPORT_THEATRE_WORK -> handleExportTheatreWork(selection.videoOptions());
            case EXPORT_THEATRE_SPATIAL_VIEW -> handleExportTheatreSpatialView(selection.theatreMapOptions());
            case EXPORT_THEATRE_PORTION -> handleExportTheatrePortion(selection.theatrePortionOptions());
            default -> handleInspectExportReadiness();
        }
    }

    private void handlePrepareTechnicalProblem() {
        viewModel.openTechnicalProblemPanel();
    }

    private void handleListenDocument() {
        if (ensureProjectSavedForDocumentAudio("escuchar o preparar audio del documento")
                && confirmAudioEngineReadyForDocumentAction(false)) {
            pdfNarratablePreparation.prepareThenRun(viewModel::runDocumentPrimaryAction);
        }
    }

    private void handlePlaySelection() {
        if (ensureProjectSavedForDocumentAudio("reproducir la oración seleccionada")
                && confirmAudioEngineReadyForDocumentAction(false)) {
            pdfNarratablePreparation.prepareThenRun(viewModel::playFromSelectedSegment);
        }
    }


    private void handleGenerateChunksFromStatusBar() {
        processOverlayExpanded.set(true);
        if (ensureProjectSavedForDocumentAudio("reconstruir fragmentos de audio")
                && confirmAudioEngineReadyForDocumentAction(true)) {
            pdfNarratablePreparation.prepareForwardThenRun(() -> viewModel.generateAudioChunksWithoutPlayback());
        }
    }

    private void handleGenerateSelectedChunkFromStatusBar() {
        processOverlayExpanded.set(true);
        if (ensureProjectSavedForDocumentAudio("renderizar audio desde el fragmento seleccionado")
                && confirmAudioEngineReadyForDocumentAction(true)) {
            pdfNarratablePreparation.prepareThenRun(viewModel::generateAudioChunksFromSelectedFragment);
        }
    }

    private void handleDeleteAllAudioChunksFromStatusBar() {
        if (deleteAudioChunksDialog.confirm(owner())) {
            viewModel.deleteAllPersistedAudioChunks();
        }
    }

    private void handleGenerateAudio() {
        if (ensureProjectSavedForDocumentAudio("generar audio por fragmentos")
                && confirmAudioEngineReadyForDocumentAction(true)) {
            pdfNarratablePreparation.prepareForwardThenRun(() -> viewModel.generateAudioChunksWithoutPlayback());
        }
    }

    private boolean confirmAudioEngineReadyForDocumentAction(boolean forceGeneration) {
        boolean unavailable = forceGeneration
                ? viewModel.audioEngineUnavailableForGeneration()
                : viewModel.audioEngineUnavailableForDocumentPrimaryAction();
        if (!unavailable) {
            showDocumentAudioDefensiveDecisions();
            return true;
        }
        if (audioEngineUnavailableDialog.show(owner(), viewModel.audioEngineUnavailableMessage())) {
            handleOpenVoiceEngineSettings();
        }
        return false;
    }

    private void showDocumentAudioDefensiveDecisions() {
        List<UserVisibleDecision> decisions = new DocumentAudioDefensiveDecisionGuard(viewModel.applicationServices())
                .decisionsBeforeDocumentGeneration();
        List<UserVisibleDecision> unseen = decisions.stream()
                .filter(UserVisibleDecision::requiresDialog)
                .filter(decision -> !decisionKey(decision).equals(lastAudioDefensiveDecisionKey))
                .toList();
        if (unseen.isEmpty()) {
            return;
        }
        lastAudioDefensiveDecisionKey = decisionKey(unseen.get(unseen.size() - 1));
        alertPresenter.showDialogDecisions(unseen, owner());
    }

    private void showAudioGenerationFailureIfNeeded(AudioJobStatusDto oldStatus, AudioJobStatusDto newStatus) {
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
        return "El motor de voz termino sin WAV valido para el fragmento actual. Abre los detalles tecnicos del dialogo o Diagnostico avanzado antes de reintentar.";
    }

    private static String decisionKey(UserVisibleDecision decision) {
        return decision == null ? "" : decision.headline() + "|" + decision.technicalDetail();
    }

    private boolean ensureProjectSavedForDocumentAudio(String action) {
        if (viewModel.currentProjectFile().isPresent()) {
            return true;
        }
        if (!viewModel.projectOpenProperty().get()) {
            return true;
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
                viewModel.applicationServices().examples().catalog().listExamples(),
                viewModel.applicationServices().examples().inspectReadiness());
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
        DirectoryChooser chooser = new DirectoryChooser();
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
        TechnicalProblemDialog.showExpress(owner())
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
        ProjectInitialSourceDialog.Decision decision = projectInitialSourceDialog.show(owner());
        if (decision == ProjectInitialSourceDialog.Decision.CANCEL) {
            return;
        }
        if (decision == ProjectInitialSourceDialog.Decision.CREATE_WITHOUT_SOURCE) {
            viewModel.createNewProject(setup.title(), setup.mode());
            return;
        }
        chooseSourceDocument("Elegir fuente inicial del proyecto")
                .ifPresent(sourceFile -> runSourceDocumentImport(sourceFile,
                        () -> viewModel.createNewProject(setup.title(), setup.mode())));
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
        viewModel.openProject(projectFile);
        rememberCurrentProject();
        viewModel.inspectProjectIntegrityDecision().ifPresent(decision -> alertPresenter.showDecision(decision, owner()));
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

        if (projectSourceCopyNoticeDialog.show(owner(), canonicalSource)) {
            preferences.putBoolean(PREF_HIDE_SOURCE_COPY_NOTICE, true);
        }
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

    public void handleImportWord() {
        if (!prepareForSourceDocumentImport()) {
            return;
        }
        chooseSourceDocument("Abrir documento fuente").ifPresent(this::runSourceDocumentImport);
    }

    private Optional<Path> chooseSourceDocument(String title) {
        FileChooser chooser = new FileChooser();
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
        Optional<Path> currentSource = viewModel.currentSourceDocumentPath();
        if (currentSource.isEmpty()) {
            return true;
        }
        if (!projectSourceReplacementDialog.confirm(owner(), currentSource.get())) {
            return false;
        }
        return confirmSourceReplacementDirtyState();
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        runSourceDocumentImport(sourceFile, null);
    }

    private void runSourceDocumentImport(Path sourceFile, Runnable beforeAttach) {
        DocumentImportProgressDialog progress = new DocumentImportProgressDialog(owner(), sourceFile.getFileName().toString());
        Task<ReadableDocument> task = new Task<>() {
            @Override
            protected ReadableDocument call() throws Exception {
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
                    if (beforeAttach != null) {
                        beforeAttach.run();
                    }
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
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
        AudioExportFormat format = requestedFormat == null ? AudioExportFormat.WAV : requestedFormat;
        if (!viewModel.hasAllChunksRendered()) {
            if (!incompleteAudioExportDialog.confirmRenderAndExport(owner())) { return; }
            Path target = chooseExportPodcastWavTarget(format);
            if (target == null) { return; }
            viewModel.submitAudioGenerationWithPendingExport(() -> {
                try { viewModel.exportPodcastWav(target); }
                catch (IOException | RuntimeException ex) { showError("No se pudo exportar el audio final", ex); }
            });
            return;
        }
        Path target = chooseExportPodcastWavTarget(format);
        if (target == null) { return; }
        try { viewModel.exportPodcastWav(target); }
        catch (IOException | RuntimeException ex) { showError("No se pudo exportar el audio final", ex); }
    }
    private Path chooseExportPodcastWavTarget(AudioExportFormat requestedFormat) {
        AudioExportFormat format = requestedFormat == null ? AudioExportFormat.WAV : requestedFormat;
        FileChooser chooser = new FileChooser();
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar reporte diagnóstico");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"));
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
        DirectoryChooser chooser = new DirectoryChooser();
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
        handleExportDocumentStudyTextAudioVideo(DocumentTextVideoOptions.defaults());
    }

    public void handleExportDocumentStudyTextAudioVideo(DocumentTextVideoOptions requestedOptions) {
        DocumentTextVideoOptions textOptions = requestedOptions == null ? DocumentTextVideoOptions.defaults() : requestedOptions;
        VideoExportOptions options = documentStudyVideoOptions(textOptions);
        String statusMessage = "Exportando video documental texto+audio.";
        if (!viewModel.hasAllChunksRendered()) {
            if (!incompleteAudioExportDialog.confirmRenderAndExport(owner())) { return; }
            viewModel.submitAudioGenerationWithPendingExport(() -> {
                viewModel.updateStatusMessage(statusMessage);
                exportDocumentStudyTextAudioVideoInBackground(
                        fileForExport("Exportar video documental texto+audio", "estudio-documental-texto-audio", options),
                        options,
                        textOptions);
            });
            return;
        }
        File file = fileForExport("Exportar video documental texto+audio", "estudio-documental-texto-audio", options);
        if (file == null) {
            return;
        }
        viewModel.updateStatusMessage(statusMessage);
        if (dependencySetupAssistant.offerVideoLocalSetupIfMissing(owner(), viewModel.applicationServices().settings(),
                () -> exportDocumentStudyTextAudioVideoInBackground(file, options, textOptions))) {
            return;
        }
        exportDocumentStudyTextAudioVideoInBackground(file, options, textOptions);
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
        if (dependencySetupAssistant.offerVideoLocalSetupIfMissing(owner(), viewModel.applicationServices().settings(),
                () -> exportTheatreWorkInBackground(file, options))) {
            return;
        }
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
        if (dependencySetupAssistant.offerVideoLocalSetupIfMissing(owner(), viewModel.applicationServices().settings(),
                () -> exportTheatreSpatialVideoInBackground(file, options))) {
            return;
        }
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
            if (dependencySetupAssistant.offerVideoLocalSetupIfMissing(owner(), viewModel.applicationServices().settings(),
                    () -> exportTheatrePortionInBackground(file, options))) {
                return;
            }
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
        if (dependencySetupAssistant.offerVideoLocalSetupIfMissing(owner(), viewModel.applicationServices().settings(),
                () -> exportFinalVideoInBackground(file, options))) {
            return;
        }
        exportFinalVideoInBackground(file, options);
    }
    private File fileForExport(String title, String filePrefix, VideoExportOptions options) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Video MP4 (*.mp4)", "*.mp4"));
        chooser.setInitialFileName(filePrefix + "-" + options.resolution().label().toLowerCase(java.util.Locale.ROOT)
                + "-" + options.framesPerSecond() + "fps.mp4");
        return chooser.showSaveDialog(owner());
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
        videoExportProgressCoordinator.export(owner(), file.toPath(), options,
                (targetFile, selectedOptions, progress, cancellationRequested) ->
                        viewModel.exportDocumentStudyTextAudioVideo(targetFile,
                                selectedOptions.resolution(),
                                selectedOptions.framesPerSecond(),
                                selectedOptions.encoderPolicy(),
                                textOptions == null ? DocumentTextVideoOptions.defaults() : textOptions.withResolution(selectedOptions.resolution()),
                                progress,
                                cancellationRequested),
                ex -> showError("No se pudo exportar el video documental texto+audio", ex));
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
            return viewModel.applicationServices().settings().loadOperationalSettings().load();
        } catch (IOException | RuntimeException ex) {
            return OperationalSettings.defaults();
        }
    }

    private List<VideoEncoderPolicy> availableVideoEncoderPolicies(OperationalSettings settings) {
        java.util.LinkedHashSet<VideoEncoderPolicy> policies = new java.util.LinkedHashSet<>();
        policies.add(VideoEncoderPolicy.CPU_X264);
        try {
            var report = viewModel.applicationServices().settings().inspectComputeEnvironment().inspect(settings);
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
        settingsDialog.show(owner(), viewModel.applicationServices().settings(),
                SettingsSupportActions.of(commandDispatcher::canDispatch, this::dispatchCommand));
    }

    public void handleOpenVoiceEngineSettings() {
        settingsDialog.showVoiceEngines(owner(), viewModel.applicationServices().settings());
    }

    public void handleOpenOcrSettings() {
        settingsDialog.showVoiceEngines(owner(), viewModel.applicationServices().settings());
        retryVisiblePdfTextPreparation();
    }

    private void retryVisiblePdfTextPreparation() {
        Node view = workspaceRegistry.viewFor(viewModel.activeWorkspaceProperty().get());
        if (view instanceof DocumentWorkspaceView documentWorkspaceView) {
            documentWorkspaceView.retryVisiblePdfTextPreparation();
        }
    }

    public void handleOpenFirstUseSetup() {
        settingsDialog.showFirstUseSetup(owner(), viewModel.applicationServices().settings());
    }

    public void handleOpenGuide() {
        new GuideDialog(viewModel.applicationServices().guide()).show(owner());
    }

    public void handleOpenGuideTopic(GuideTopicId topicId) {
        new GuideDialog(viewModel.applicationServices().guide()).showTopic(owner(), topicId);
    }

    public void handleExportAiResources() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Exportar recursos IA de DocuPodcast");
        File folder = chooser.showDialog(owner());
        if (folder == null) {
            return;
        }
        try {
            var result = viewModel.applicationServices().resources().exportAiResources().export(folder.toPath());
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
        Node view = workspaceRegistry.viewFor(resolved);
        workspaceHost.getChildren().setAll(view);
    }

    private FileChooser projectFileChooser(String title) {
        FileChooser chooser = new FileChooser();
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

    private Window owner() {
        return getScene() == null ? null : getScene().getWindow();
    }


    private void showError(String header, Throwable error) {
        alertPresenter.showFailure(header, error, owner());
    }
}
