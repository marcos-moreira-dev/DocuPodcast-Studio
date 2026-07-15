package com.marcosmoreiradev.docupodcaststudio.presentation.shell;
import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioInputDevice;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleVisualBindingDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.recording.RecordingActionPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneReferenceResolution;
import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineAvailability;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.media.PreparedAudioAsset;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobSnapshotMapper;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentListenPhase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.document.SourceDocumentRefreshDecisionFactory;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentListenPlan;
import com.marcosmoreiradev.docupodcaststudio.application.reading.ReadingProfilePreview;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPackageExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreStageGeometry;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectWorkspaceHydration;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.StreamingPlaybackWindow;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionBoundaryStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionCatalogo;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreFrameSketchContext;
import com.marcosmoreiradev.docupodcaststudio.application.export.PodcastFinalWavExportResult;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardScene;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardValidationIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.*;
import com.marcosmoreiradev.docupodcaststudio.application.voice.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMetadata;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.audio.AudioQueueState;
import com.marcosmoreiradev.docupodcaststudio.presentation.playback.PlaybackSyncState;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentFragmentRailPresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentRailImagePresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentRailProjectionFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentVisualFragmentKey;
import com.marcosmoreiradev.docupodcaststudio.application.document.ListeningSessionState;
import com.marcosmoreiradev.docupodcaststudio.presentation.storyboard.StoryboardScenePresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.WritableImage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import com.marcosmoreiradev.docupodcaststudio.application.image.*;
public final class DocuPodcastShellViewModel {
    public static final int MIN_READING_FONT_SIZE = 14;
    public static final int DEFAULT_READING_FONT_SIZE = 18;
    public static final int MAX_READING_FONT_SIZE = 28;
    private final ApplicationServices applicationServices;
    private final ProjectSessionCoordinator sessions = new ProjectSessionCoordinator();
    private final ProjectWorkflowCoordinator projectWorkflow;
    private final DocumentIntakeCoordinator documentIntake;
    private final SourceDocumentRefreshCoordinator sourceDocumentRefresh;
    private final WorkspaceNavigationCoordinator workspaceNavigation;
    private final DocumentNarrationCoordinator documentNarration;
    private final PlaybackWorkflowCoordinator playbackWorkflow;
    private final PlaybackFragmentNavigator playbackNavigator = new PlaybackFragmentNavigator();
    private final AudioWorkflowCoordinator audioWorkflow;
    private final ExportWorkflowCoordinator exportWorkflow;
    private final NarrativeLayerCoordinator narrativeLayerWorkflow;
    private final VoiceSampleWorkflowCoordinator voiceSampleWorkflow;
    private final ExampleVisualBindingWorkflow exampleVisualBindingWorkflow = new ExampleVisualBindingWorkflow();
    private final VoiceProfileAdministrationCoordinator voiceProfileAdministration = new VoiceProfileAdministrationCoordinator();
    private final ReadingComfortCoordinator readingComfortWorkflow;
    private final DocumentSelectionCoordinator documentSelectionWorkflow = new DocumentSelectionCoordinator();
    private final TheatreCharacterProfileCoordinator theatreCharacterProfileWorkflow = new TheatreCharacterProfileCoordinator();
    private final TheatreCharacterImageCoordinator theatreCharacterImageWorkflow = new TheatreCharacterImageCoordinator();
    private final TheatreObjectProfileCoordinator theatreObjectProfileWorkflow = new TheatreObjectProfileCoordinator();
    private final TheatreObjectImageCoordinator theatreObjectImageWorkflow = new TheatreObjectImageCoordinator();
    private final TheatreSceneCoordinator theatreSceneWorkflow = new TheatreSceneCoordinator();
    private final TheatreBoundaryPersistenceCoordinator theatreBoundaryWorkflow = new TheatreBoundaryPersistenceCoordinator();
    private final TheatreTextActionPlacementSaveWorkflow theatreTextActionPlacementSaveWorkflow = new TheatreTextActionPlacementSaveWorkflow();
    private final TheatreDemoManifestWorkflow theatreDemoManifestWorkflow;
    private final TheatreInterventionContextExportWorkflow theatreInterventionContextExportWorkflow = new TheatreInterventionContextExportWorkflow();
    private final TheatreStoryboardFrameWorkflow theatreStoryboardFrameWorkflow = new TheatreStoryboardFrameWorkflow();
    private final TheatreAudioTrackWorkflow theatreAudioTrackWorkflow = new TheatreAudioTrackWorkflow();
    private final TheatreVisualSetupCoordinator theatreVisualSetupWorkflow = new TheatreVisualSetupCoordinator();
    private final TheatreBulkInterventionContextExportWorkflow theatreBulkContextExportWorkflow = new TheatreBulkInterventionContextExportWorkflow();
    private final TheatreExportScopeScriptFilter theatreExportScopeScriptFilter = new TheatreExportScopeScriptFilter();
    private final ManualInterventionAudioWorkflow manualInterventionAudioWorkflow = new ManualInterventionAudioWorkflow();
    private final TheatreInterventionAudioRegenerationWorkflow theatreInterventionAudioRegenerationWorkflow = new TheatreInterventionAudioRegenerationWorkflow();
    private final TheatreChoralVoiceRenderCoordinator theatreChoralVoiceRenderWorkflow = new TheatreChoralVoiceRenderCoordinator();
    private final StudyProblemWorkflow studyProblemWorkflow;
    private final TheatreImageGenerationWorkflow theatreImageGenerationWorkflow;
    private final DocumentStudyVideoAssetWorkflow documentaryVideoAssetWorkflow = new DocumentStudyVideoAssetWorkflow();
    private final TheatreFrameGenerationWorkflow theatreFrameGenerationWorkflow;
    private final TheatreImageAssetWorkflow theatreImageAssetWorkflow;
    private final ProjectImageManagementWorkflow projectImageWorkflow;
    private final DocumentPlaybackSelectionResolver playbackSelectionResolver = new DocumentPlaybackSelectionResolver();
    private final PlayableAudioJobSelector playableAudioJobSelector = new PlayableAudioJobSelector();
    private final ReadOnlyStringWrapper windowTitle = new ReadOnlyStringWrapper("DocuPodcast Studio — Inicio");
    private final ObjectProperty<WorkspaceKind> activeWorkspace = new SimpleObjectProperty<>(WorkspaceKind.WELCOME_HOME);
    private final ObjectProperty<ReadableDocument> currentDocument = new SimpleObjectProperty<>();
    private final ObjectProperty<NarrationScriptDocument> currentScript = new SimpleObjectProperty<>();
    private final ObjectProperty<StoryboardDocument> currentStoryboard = new SimpleObjectProperty<>();
    private final StringProperty selectedScriptSegmentId = new SimpleStringProperty("");
    private final StringProperty selectedDocumentBlockId = new SimpleStringProperty("");
    private final DoubleProperty pdfVisualDocumentProgress = new SimpleDoubleProperty(0.0);
    private final IntegerProperty pdfVisiblePageNumber = new SimpleIntegerProperty(0);
    private final ObjectProperty<DocumentTextRange> selectedDocumentTextRange = new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper selectedDocumentRangeLabel = new ReadOnlyStringWrapper("Sin texto seleccionado");
    private final ReadOnlyStringWrapper selectedDocumentSourceLocation = new ReadOnlyStringWrapper("Ubicación fuente: sin selección.");
    private String selectedDocumentTextPreview = "";
    private final StringProperty selectedVisualFragmentSegmentId = new SimpleStringProperty("");
    private final StringProperty selectedVisualFragmentImageUri = new SimpleStringProperty("");
    private final ObjectProperty<DocumentVisualFragmentKey> selectedVisualFragmentKey = new SimpleObjectProperty<>(DocumentVisualFragmentKey.empty());
    private final StringProperty lastStoryboardImageAssetId = new SimpleStringProperty("");
    private final IntegerProperty documentMediaRevision = new SimpleIntegerProperty(0);
    private final ObjectProperty<PlaybackCursor> playbackCursor = new SimpleObjectProperty<>(PlaybackCursor.stopped());
    private final ObjectProperty<PlaybackManifest> currentPlaybackManifest = new SimpleObjectProperty<>(PlaybackManifest.empty());
    private final ObjectProperty<ProjectMode> currentProjectMode = new SimpleObjectProperty<>(ProjectMode.defaultMode());
    private final Timeline playbackTimer;
    private final PlaybackTransportCoordinator playbackTransport;
    private final TheatreAudioTrackPlaybackCoordinator theatreAudioPlayback;
    private final PlaybackBufferPolicy playbackBufferPolicy = PlaybackBufferPolicy.defaultPolicy();
    private final AudioStatusUiThrottle audioStatusUiThrottle = new AudioStatusUiThrottle(this::applyAudioStatusOnFxThread);
    private final BooleanProperty previousFragmentAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty nextFragmentAvailable = new SimpleBooleanProperty(false);
    private final DoubleProperty playbackRate = new SimpleDoubleProperty(1.0);
    private boolean documentPlaybackRequested = false, playSingleCueOnly = false, singleCuePlaybackRequested = false;
    private String waitingForBufferedSegmentAfter = "", pendingPlaybackStartSegmentId = "", lastSequentialCueUnitId = "";
    private Runnable pendingExportTask = null;
    private final ObjectProperty<AudioJobStatusDto> activeAudioJobStatus = new SimpleObjectProperty<>(AudioJobStatusDto.idle());
    private final ObjectProperty<ReadingProfile> activeReadingProfile = new SimpleObjectProperty<>(ReadingProfile.academicDefaults());
    private final ObjectProperty<VoiceLibrary> activeVoiceLibrary = new SimpleObjectProperty<>(VoiceLibrary.defaults());
    private final BooleanProperty projectOpen = new SimpleBooleanProperty(false), dirty = new SimpleBooleanProperty(false),
            saveableProjectOpen = new SimpleBooleanProperty(false), audioJobRunning = new SimpleBooleanProperty(false),
            voiceRecordingRunning = new SimpleBooleanProperty(false), manualAudioRecordingRunning = new SimpleBooleanProperty(false),
            documentRightRailVisible = new SimpleBooleanProperty(false), documentPlaybarDocked = new SimpleBooleanProperty(false), technicalProblemPreparationActive = new SimpleBooleanProperty(false);
    private final StringProperty focusedTheatreSceneId = new SimpleStringProperty("");
    private final BooleanProperty readAfterColonForNarration = new SimpleBooleanProperty(false);
    private final IntegerProperty readingFontSize = new SimpleIntegerProperty(DEFAULT_READING_FONT_SIZE);
    private Path activeVoiceRecordingFile;
    private String activeManualAudioSegmentId = "", activeVoiceRecordingVoiceId = "";
    private VoiceReferenceTone activeVoiceRecordingTone = VoiceReferenceTone.NEUTRAL;
    private final ObjectProperty<Path> lastGeneratedVoiceTestPath = new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper generatedVoiceTestStatus = new ReadOnlyStringWrapper("Sin voz de prueba generada.");
    private final ReadOnlyStringWrapper documentVoiceToneStatus = new ReadOnlyStringWrapper("Selecciona una voz y un tono para este fragmento.");
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("Listo. Abre un Word/DOCX o crea un proyecto DocuPodcast.");
    private final ReadOnlyStringWrapper documentPrimaryActionLabel = new ReadOnlyStringWrapper("Reproducir documento");
    private final ReadOnlyStringWrapper documentPrimaryActionHint = new ReadOnlyStringWrapper("Abre un Word/DOCX para iniciar lectura narrada.");
    private final ReadOnlyStringWrapper documentListenFlowTitle = new ReadOnlyStringWrapper(ListeningSessionState.noDocument().title());
    private final ReadOnlyStringWrapper documentListenFlowDetail = new ReadOnlyStringWrapper(ListeningSessionState.noDocument().detail());
    private final ReadOnlyStringWrapper streamingBufferStatus = new ReadOnlyStringWrapper("Buffer: esperando documento.");
    private final IntervencionBoundaryStore intervencionBoundaryStore = new IntervencionBoundaryStore();
    private final StringProperty spatialFrameMode = new SimpleStringProperty("fragments");
    private final StringProperty activePlacementAlias = new SimpleStringProperty("");
    private final ObjectProperty<TheatreProjectLayer.TextActionPlacement> activeTextActionPlacement =
            new SimpleObjectProperty<>(TheatreProjectLayer.TextActionPlacement.empty());
    public DocuPodcastShellViewModel(ApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
        this.projectWorkflow = new ProjectWorkflowCoordinator(this.applicationServices, sessions);
        this.documentIntake = new DocumentIntakeCoordinator(this.applicationServices);
        this.sourceDocumentRefresh = new SourceDocumentRefreshCoordinator(this.applicationServices);
        this.workspaceNavigation = new WorkspaceNavigationCoordinator(sessions);
        this.documentNarration = new DocumentNarrationCoordinator(this.applicationServices);
        this.playbackWorkflow = new PlaybackWorkflowCoordinator();
        this.audioWorkflow = new AudioWorkflowCoordinator(this.applicationServices);
        this.exportWorkflow = new ExportWorkflowCoordinator(this.applicationServices, this.audioWorkflow);
        this.narrativeLayerWorkflow = new NarrativeLayerCoordinator();
        this.voiceSampleWorkflow = new VoiceSampleWorkflowCoordinator(this.applicationServices);
        this.readingComfortWorkflow = new ReadingComfortCoordinator(this.applicationServices);
        this.projectImageWorkflow = new ProjectImageManagementWorkflow(this.applicationServices);
        this.theatreDemoManifestWorkflow = new TheatreDemoManifestWorkflow(this.applicationServices);
        this.studyProblemWorkflow = new StudyProblemWorkflow(this.applicationServices.documentStudy().studyProblemPdfExporter());
        this.theatreImageGenerationWorkflow = new TheatreImageGenerationWorkflow(this.applicationServices);
        this.theatreFrameGenerationWorkflow = new TheatreFrameGenerationWorkflow(this.applicationServices);
        this.theatreImageAssetWorkflow = new TheatreImageAssetWorkflow(
                this.applicationServices, theatreCharacterImageWorkflow, theatreObjectImageWorkflow);
        this.playbackTransport = new PlaybackTransportCoordinator(this.applicationServices.playback().segmentAudioPlayer()); this.theatreAudioPlayback = new TheatreAudioTrackPlaybackCoordinator(this.applicationServices.playback().backgroundAudioPlayer());
        activeReadingProfile.set(applicationServices.readingProfile().createDefaultProfile().create());
        activeVoiceLibrary.set(applicationServices.voice().createDefaultVoiceLibrary().create());
        loadReadingComfortSettings();
        playbackTimer = new Timeline(new KeyFrame(Duration.millis(250), event -> tickPlayback()));
        playbackTimer.setCycleCount(Timeline.INDEFINITE);
        playbackTransport.setOnPlaybackFinished(this::handlePlaybackFinishedOnFxThread);
        selectedDocumentBlockId.addListener((obs, oldValue, newValue) -> actualizarFrameActivo());
    }
    public ApplicationServices applicationServices() { return applicationServices; }
    public ReadOnlyStringProperty windowTitleProperty() { return windowTitle.getReadOnlyProperty(); }
    public ObjectProperty<WorkspaceKind> activeWorkspaceProperty() { return activeWorkspace; }
    public ReadOnlyObjectProperty<ReadableDocument> currentDocumentProperty() { return currentDocument; }
    public ReadOnlyObjectProperty<NarrationScriptDocument> currentScriptProperty() { return currentScript; }
    public ReadOnlyObjectProperty<StoryboardDocument> currentStoryboardProperty() { return currentStoryboard; }
    public ReadOnlyStringProperty selectedScriptSegmentIdProperty() { return selectedScriptSegmentId; }
    public ReadOnlyStringProperty selectedDocumentBlockIdProperty() { return selectedDocumentBlockId; } public ReadOnlyDoubleProperty pdfVisualDocumentProgressProperty() { return pdfVisualDocumentProgress; } public int pdfVisiblePageNumber() { return pdfVisiblePageNumber.get(); } public void updatePdfVisiblePageNumber(int page) { pdfVisiblePageNumber.set(Math.max(0, page)); }
    public ReadOnlyObjectProperty<DocumentTextRange> selectedDocumentTextRangeProperty() { return selectedDocumentTextRange; }
    public ReadOnlyStringProperty selectedDocumentRangeLabelProperty() { return selectedDocumentRangeLabel.getReadOnlyProperty(); }
    public ReadOnlyStringProperty selectedDocumentSourceLocationProperty() { return selectedDocumentSourceLocation.getReadOnlyProperty(); }
    public ReadOnlyStringProperty selectedVisualFragmentSegmentIdProperty() { return selectedVisualFragmentSegmentId; } public ReadOnlyStringProperty selectedVisualFragmentImageUriProperty() { return selectedVisualFragmentImageUri; } public ReadOnlyObjectProperty<DocumentVisualFragmentKey> selectedVisualFragmentKeyProperty() { return selectedVisualFragmentKey; }
    public ReadOnlyIntegerProperty documentMediaRevisionProperty() { return documentMediaRevision; }
    public ReadOnlyStringProperty documentPrimaryActionLabelProperty() { return documentPrimaryActionLabel.getReadOnlyProperty(); }
    public ReadOnlyStringProperty documentPrimaryActionHintProperty() { return documentPrimaryActionHint.getReadOnlyProperty(); }
    public ReadOnlyStringProperty documentListenFlowTitleProperty() { return documentListenFlowTitle.getReadOnlyProperty(); }
    public ReadOnlyStringProperty documentListenFlowDetailProperty() { return documentListenFlowDetail.getReadOnlyProperty(); }
    public ReadOnlyStringProperty streamingBufferStatusProperty() { return streamingBufferStatus.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<PlaybackCursor> playbackCursorProperty() { return playbackCursor; }
    public ReadOnlyObjectProperty<PlaybackManifest> currentPlaybackManifestProperty() { return currentPlaybackManifest; } public ReadOnlyObjectProperty<ProjectMode> currentProjectModeProperty() { return currentProjectMode; }
    public PlaybackBufferPolicy playbackBufferPolicy() { return playbackBufferPolicy; }
    public ReadOnlyBooleanProperty previousFragmentAvailableProperty() { return previousFragmentAvailable; }
    public ReadOnlyBooleanProperty nextFragmentAvailableProperty() { return nextFragmentAvailable; }
    public ReadOnlyDoubleProperty playbackRateProperty() { return playbackRate; }
    public String playbackBufferStatusLabel() {
        AudioJobStatusDto status = activeAudioJobStatus.get();
        if (status.totalSegments() <= 0) {
            return playbackBufferPolicy.userLabel();
        }
        return streamingPlaybackWindow(status, currentPlaybackManifest.get()).readerStatusLabel();
    }
    public ReadOnlyObjectProperty<AudioJobStatusDto> activeAudioJobStatusProperty() { return activeAudioJobStatus; } public ReadOnlyObjectProperty<ReadingProfile> activeReadingProfileProperty() { return activeReadingProfile; }
    public ReadOnlyObjectProperty<VoiceLibrary> activeVoiceLibraryProperty() { return activeVoiceLibrary; } public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }
    public void updateStatusMessage(String message) { statusMessage.set(message == null || message.isBlank() ? "Listo." : message.strip()); } public void replaceCurrentDocumentFromApplication(ReadableDocument document, String message) { if (document == null) { return; } sessions.activeSession().ifPresent(session -> session.setImportedDocument(document)); currentDocument.set(document); currentScript.set(null); statusMessage.set(message == null || message.isBlank() ? "Documento actualizado." : message); refreshProjectState(); }
    public ReadOnlyBooleanProperty projectOpenProperty() { return projectOpen; } public ReadOnlyBooleanProperty dirtyProperty() { return dirty; }
    public ReadOnlyBooleanProperty saveableProjectOpenProperty() { return saveableProjectOpen; } public ReadOnlyBooleanProperty audioJobRunningProperty() { return audioJobRunning; }
    public ReadOnlyBooleanProperty choralVoiceRenderingProperty() { return theatreChoralVoiceRenderWorkflow.runningProperty(); } public ReadOnlyDoubleProperty choralVoiceRenderProgressProperty() { return theatreChoralVoiceRenderWorkflow.progressProperty(); } public ReadOnlyStringProperty choralVoiceRenderStatusProperty() { return theatreChoralVoiceRenderWorkflow.statusProperty(); }
    public ReadOnlyBooleanProperty voiceRecordingRunningProperty() { return voiceRecordingRunning; } public ReadOnlyBooleanProperty manualAudioRecordingRunningProperty() { return manualAudioRecordingRunning; }
    public ReadOnlyStringProperty focusedTheatreSceneIdProperty() { return focusedTheatreSceneId; } public void focusTheatreScene(String sceneId) { focusedTheatreSceneId.set(sceneId == null ? "" : sceneId.strip()); }
    public ReadOnlyObjectProperty<Path> lastGeneratedVoiceTestPathProperty() { return lastGeneratedVoiceTestPath; } public ReadOnlyStringProperty generatedVoiceTestStatusProperty() { return generatedVoiceTestStatus.getReadOnlyProperty(); }
    public ReadOnlyStringProperty documentVoiceToneStatusProperty() { return documentVoiceToneStatus.getReadOnlyProperty(); } public ReadOnlyStringProperty spatialFrameModeProperty() { return spatialFrameMode; }
    public void setSpatialFrameMode(String mode) { spatialFrameMode.set(TheatreStageGeometry.normalizeFrameMode(mode)); } public ReadOnlyStringProperty activePlacementAliasProperty() { return activePlacementAlias; }
    public ReadOnlyObjectProperty<TheatreProjectLayer.TextActionPlacement> activeTextActionPlacementProperty() { return activeTextActionPlacement; } public BooleanProperty documentRightRailVisibleProperty() { return documentRightRailVisible; } public BooleanProperty documentPlaybarDockedProperty() { return documentPlaybarDocked; } public BooleanProperty technicalProblemPreparationActiveProperty() { return technicalProblemPreparationActive; }
    public BooleanProperty readAfterColonForNarrationProperty() { return readAfterColonForNarration; } public IntegerProperty readingFontSizeProperty() { return readingFontSize; }
    public void decreaseReadingFontSize() { setReadingFontSize(readingComfortWorkflow.decrease(readingFontSize.get())); }
    public void increaseReadingFontSize() { setReadingFontSize(readingComfortWorkflow.increase(readingFontSize.get())); }
    public void resetReadingFontSize() { setReadingFontSize(DEFAULT_READING_FONT_SIZE); }
    public void setReadingFontSize(int requestedSize) {
        int next = readingComfortWorkflow.clampFontSize(requestedSize);
        if (readingFontSize.get() == next) { statusMessage.set("Tamaño de lectura: " + readingComfortWorkflow.zoomPercent(next) + "%."); return; }
        readingFontSize.set(next);
        ReadingComfortCoordinator.PersistedReadingFontSize persisted = readingComfortWorkflow.persistFontSize(next);
        statusMessage.set(persisted.saved() ? "Tamaño de lectura: " + readingComfortWorkflow.zoomPercent(next) + "% (" + next + " px)." : persisted.message());
    }
    private void bumpDocumentMediaRevision() { documentMediaRevision.set(documentMediaRevision.get() + 1); }
    private void updateProjectStatusOnFxThread(String message, boolean mediaChanged) {
        Runnable update = () -> { statusMessage.set(message == null || message.isBlank() ? "Listo." : message.strip()); if (mediaChanged) bumpDocumentMediaRevision(); refreshProjectState(); };
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }
    public int readingZoomPercent() { return readingComfortWorkflow.zoomPercent(readingFontSize.get()); } public void updatePdfVisualDocumentProgress(double progress) { pdfVisualDocumentProgress.set(Math.max(0.0, Math.min(1.0, progress))); }
    public void toggleDocumentRightRail() { documentRightRailVisible.set(!documentRightRailVisible.get()); statusMessage.set(documentRightRailVisible.get() ? "Rail derecho visible." : "Rail derecho oculto."); } public void openTechnicalProblemPanel() { if (currentDocument.get() == null) { technicalProblemPreparationActive.set(false); statusMessage.set("Abre una fuente documental antes de preparar problemas tecnicos."); refreshProjectState(); return; } activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); documentRightRailVisible.set(true); statusMessage.set("Panel de problema tecnico abierto. Usa el panel derecho para seleccionar fragmentos."); refreshProjectState(); } public void toggleDocumentPlaybarDocked() { documentPlaybarDocked.set(!documentPlaybarDocked.get()); statusMessage.set(documentPlaybarDocked.get() ? "Playbar desplazada al rail izquierdo." : "Playbar flotante sobre el documento."); }
    public void toggleTechnicalProblemPreparation() { if (currentDocument.get() == null) { technicalProblemPreparationActive.set(false); statusMessage.set("Abre una fuente documental antes de preparar problemas tecnicos."); refreshProjectState(); return; } technicalProblemPreparationActive.set(!technicalProblemPreparationActive.get()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); documentRightRailVisible.set(true); statusMessage.set(technicalProblemPreparationActive.get() ? "Seleccion de fragmentos para problema tecnico activa." : "Seleccion de fragmentos para problema tecnico desactivada."); refreshProjectState(); }
    private void loadReadingComfortSettings() { readingFontSize.set(readingComfortWorkflow.loadInitialFontSize()); }
    public String voiceRecordingStatusLabel() {
        if (voiceRecordingRunning.get()) {
            return "Grabando muestra de voz humana" + (activeVoiceRecordingFile == null ? "" : ": " + activeVoiceRecordingFile.getFileName());
        }
        return "Sin grabación activa";
    }
    public boolean hasUnsavedChanges() { return sessions.dirty(); } public boolean hasOpenProject() { return sessions.hasActiveSession(); }
    public Optional<DocuPodcastProject> currentProject() { return sessions.activeSession().map(ProjectSession::project); }
    public Optional<Path> currentProjectFile() { return sessions.activeSession().flatMap(ProjectSession::projectFile); }
    public DocumentStudyVideoConfiguration documentaryVideoConfiguration() { return currentProject().map(project -> project.study().documentaryVideoConfiguration()).orElseGet(DocumentStudyVideoConfiguration::empty); }
    public boolean documentaryVideoConfigurationAvailable() { return currentProjectMode.get() == ProjectMode.DOCUMENTARY_STUDIO && currentDocument.get() != null && currentDocument.get().format() == com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat.DOCX; }
    public void updateDocumentaryVideoConfiguration(DocumentStudyVideoConfiguration value) { documentaryVideoAssetWorkflow.update(requireSession(), value); documentaryVideoChanged("Configuracion de video documental actualizada."); }
    public ProjectAssetReference importDocumentaryVideoImage(Path source) throws IOException { ProjectAssetReference asset = documentaryVideoAssetWorkflow.importImage(applicationServices, requireSession(), source); documentaryVideoChanged("Imagen copiada dentro del proyecto: " + asset.displayName()); return asset; }
    public DocumentStudyVideoAssetWorkflow.DrawingAsset saveDocumentaryDrawing(String blockId, Path png, String state) throws IOException { var asset = documentaryVideoAssetWorkflow.saveDrawing(requireSession(), blockId, png, state); documentaryVideoChanged("Dibujo guardado dentro del proyecto."); return asset; }
    public DocumentStudyMusicTrack importDocumentaryMusic(Path source) throws IOException { var imported = documentaryVideoAssetWorkflow.importMusic(applicationServices, requireSession(), source); documentaryVideoChanged("Musica copiada dentro del proyecto: " + imported.displayName()); return imported.track(); }
    public Optional<Path> resolveCurrentProjectAsset(String assetId) { return documentaryVideoAssetWorkflow.resolveAsset(currentProject(), currentProjectFile(), assetId); }
    public Optional<Path> resolveCurrentProjectRelativePath(String path) { return documentaryVideoAssetWorkflow.resolveRelative(currentProjectFile(), path); }
    private void documentaryVideoChanged(String message) { bumpDocumentMediaRevision(); statusMessage.set(message); refreshProjectState(); }
    public List<TheatreProjectLayer.CharacterProfile> theatreCharacterProfiles() { return theatreCharacterProfileWorkflow.profiles(sessions.activeSession()); }
    public List<TheatreProjectLayer.VoiceRoleAlias> theatreVoiceRoleAliases() { return sessions.activeSession().map(session -> session.project().theatre().voiceRoleAliases()).orElseGet(List::of); }
    public Optional<TheatreProjectLayer.VoiceRoleAlias> theatreVoiceRoleAliasForCharacter(String characterId) { String target = characterId == null ? "" : characterId.strip(); return target.isBlank() ? Optional.empty() : theatreVoiceRoleAliases().stream().filter(alias -> alias.characterId().equals(target)).findFirst(); }
    public void assignTheatreCharacterVoice(String characterId, String displayName, String voiceProfileId) { var result = theatreCharacterProfileWorkflow.assignVoice(requireSession(), characterId, displayName, voiceProfileId, activeVoiceLibrary.get()); statusMessage.set(result.message()); refreshProjectState(); }
    public void saveTheatreCharacterDescription(String characterId, String displayName, String description) { statusMessage.set(theatreCharacterProfileWorkflow.save(sessions.activeSession(), characterId, displayName, description).message()); refreshProjectState(); }
    public List<TheatreProjectLayer.TheatreAct> theatreActs() { return theatreSceneWorkflow.acts(sessions.activeSession()); }
    public void addTheatreAct(String displayName, String description) { statusMessage.set(theatreSceneWorkflow.addAct(sessions.activeSession(), displayName, description).message()); refreshProjectState(); }
    public void updateTheatreAct(String actId, String displayName, String description) { statusMessage.set(theatreSceneWorkflow.updateAct(sessions.activeSession(), actId, displayName, description).message()); refreshProjectState(); }
    public List<TheatreProjectLayer.Scene> theatreScenes() { return theatreSceneWorkflow.scenes(sessions.activeSession()); }
    public void addTheatreScene(String actId, String displayName, String description) { statusMessage.set(theatreSceneWorkflow.addScene(sessions.activeSession(), actId, displayName, description).message()); refreshProjectState(); }
    public void updateTheatreScene(String sceneId, String displayName, String description) { statusMessage.set(theatreSceneWorkflow.updateScene(sessions.activeSession(), sceneId, displayName, description).message()); refreshProjectState(); }
    public void deleteTheatreScene(String sceneId) { statusMessage.set(theatreSceneWorkflow.deleteScene(sessions.activeSession(), sceneId).message()); refreshProjectState(); }
    public List<TheatreProjectLayer.CharacterImage> theatreCharacterSceneImages(String characterId, String sceneId) { return theatreCharacterImageWorkflow.images(sessions.activeSession(), characterId, sceneId); }
    public Optional<String> projectImageAssetUri(String assetId) { Optional<ProjectSession> session = sessions.activeSession(); if (session.isEmpty() || assetId == null || assetId.isBlank()) return Optional.empty(); return session.get().project().assets().byId(assetId).filter(ProjectAssetReference::isImage).flatMap(asset -> assetUri(currentProjectDirectory(), asset)); }
    public Optional<Path> projectImageAssetPath(String assetId) { Optional<ProjectSession> session = sessions.activeSession(); Optional<Path> root = currentProjectDirectory(); if (session.isEmpty() || root.isEmpty() || assetId == null || assetId.isBlank()) return Optional.empty(); Path projectRoot = root.get().toAbsolutePath().normalize(); return session.get().project().assets().byId(assetId).filter(ProjectAssetReference::isImage).map(asset -> projectRoot.resolve(asset.relativePath()).toAbsolutePath().normalize()).filter(path -> path.startsWith(projectRoot) && Files.isRegularFile(path)); }
    public Optional<TheatrePrimaryVisualReference> theatrePrimaryVisualReference(String interventionId) { Optional<ProjectSession> session = sessions.activeSession(); Optional<Path> root = currentProjectDirectory(); if (session.isEmpty() || root.isEmpty()) return Optional.empty(); return new TheatrePrimaryVisualResolver().resolve(session.get().project(), currentStoryboard.get(), currentScript.get(), interventionId, root.get()); }
    public List<TheatreProjectLayer.CameraReference> theatreCameraReferences() { return theatreVisualSetupWorkflow.cameraReferences(sessions.activeSession()); }
    public String selectedTheatreEffectiveCameraId() { Optional<ProjectSession> session = sessions.activeSession(); String id = session.flatMap(this::selectedTheatreInterventionId).orElse(""); return theatreVisualSetupWorkflow.effectiveCameraId(session, id); }
    public Optional<String> theatreCameraImageUri(String cameraId) { return theatreVisualSetupWorkflow.cameraImageUri(sessions.activeSession(), currentProjectDirectory(), cameraId); }
    public boolean selectedTheatreApplyCameraPlane() { return new TheatreCameraApplicationPolicy().applies(currentStoryboard.get(), selectedTheatreVisualSegment().map(NarrationSegment::id).orElse("")); }
    public void setSelectedTheatreApplyCameraPlane(boolean applyCamera) { StoryboardDocument storyboard = currentStoryboard.get(); String segmentId = selectedTheatreVisualSegment().map(NarrationSegment::id).orElse(""); if (storyboard == null || segmentId.isBlank()) { statusMessage.set("Asocia una imagen al fragmento antes de cambiar Aplicar plano."); refreshProjectState(); return; } StoryboardBinding binding = storyboard.bindingForSegment(segmentId).orElse(null); if (binding == null) { statusMessage.set("Asocia una imagen al fragmento antes de cambiar Aplicar plano."); refreshProjectState(); return; } Map<String, String> metadata = new TheatreCameraApplicationPolicy().metadataWithApplyCamera(binding.metadata(), applyCamera); StoryboardDocument updated = storyboard.withBinding(binding.withMetadata(metadata)); currentStoryboard.set(updated); sessions.activeSession().ifPresent(session -> session.setStoryboard(updated)); selectedScriptSegmentId.set(segmentId); statusMessage.set(applyCamera ? "Plano aplicado al fragmento." : "Plano omitido para este fragmento."); bumpDocumentMediaRevision(); refreshProjectState(); }
    public void setTheatreCameraCueForSelectedSegment(String cameraId) throws IOException { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); statusMessage.set(theatreVisualSetupWorkflow.setCameraCue(session, id, cameraId).message()); bumpDocumentMediaRevision(); refreshProjectState(); }
    public TheatreVisualSetupCoordinator.StageBackdropPreview selectedTheatreStageBackdropPreview() { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); String sceneId = new TheatreFragmentLinkPolicy().sceneIdForIntervention(session.project().theatre(), id).orElse(""); return theatreVisualSetupWorkflow.stageBackdropPreview(sessions.activeSession(), currentProjectDirectory(), id, sceneId); }
    public void assignStageBackdropForSelectedSegment(Path imageFile, boolean fragmentOverride) throws IOException { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); String sceneId = new TheatreFragmentLinkPolicy().sceneIdForIntervention(session.project().theatre(), id).orElse(""); statusMessage.set(theatreVisualSetupWorkflow.assignStageBackdrop(applicationServices, session, id, sceneId, imageFile, fragmentOverride).message()); bumpDocumentMediaRevision(); refreshProjectState(); }
    public void clearStageBackdropFromSelectedSegment() { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); statusMessage.set(theatreVisualSetupWorkflow.clearStageBackdropFromIntervention(session, id).message()); bumpDocumentMediaRevision(); refreshProjectState(); }
    public String theatreContextTextOverride(String segmentId) { return new TheatreContextTextPolicy().text(currentProject().orElse(null), segmentId).orElse(""); }
    public void saveTheatreContextText(String segmentId, String text) { ProjectSession session = requireSession(); session.replaceProject(new TheatreContextTextPolicy().save(session.project(), segmentId, text), true); statusMessage.set("Contexto textual guardado para " + segmentId + "."); refreshProjectState(); }
    public void addTheatreCharacterSceneImage(String characterId, String sceneId, Path imageFile) throws IOException { statusMessage.set(theatreImageAssetWorkflow.addCharacterImage(requireSession(), characterId, sceneId, imageFile).message()); refreshProjectState(); }
    public void replaceTheatreCharacterSceneImage(String imageId, Path imageFile) throws IOException { statusMessage.set(theatreImageAssetWorkflow.replaceCharacterImage(requireSession(), currentStoryboard.get(), imageId, imageFile).message()); refreshProjectState(); }
    public void updateTheatreCharacterSceneImageNote(String imageId, String note) { statusMessage.set(theatreImageAssetWorkflow.updateCharacterImageNote(requireSession(), imageId, note).message()); refreshProjectState(); }
    public void deleteTheatreCharacterSceneImage(String imageId) { statusMessage.set(theatreImageAssetWorkflow.deleteCharacterImage(requireSession(), currentStoryboard.get(), imageId).message()); refreshProjectState(); }
    public List<TheatreProjectLayer.TextActionPlacement> theatreTextActionPlacements() { return sessions.activeSession().map(session -> session.project().theatre().textActionPlacements()).orElseGet(List::of); }
    public void saveTheatreTextActionPlacement(TheatreProjectLayer.TextActionPlacement placement) { Objects.requireNonNull(placement, "placement"); ProjectSession session = requireSession(); theatreTextActionPlacementSaveWorkflow.save(session, placement); activePlacementAlias.set(placement.intervencionId()); activeTextActionPlacement.set(placement); statusMessage.set(placement.intervencionId() + " actualizado en mapa espacial."); refreshProjectState(); }
    public TechnicalProblem saveTechnicalProblem(List<DocumentBlock> sourceBlocks, String requestedTitle, String solutionText, WritableImage solutionImage, java.util.Map<String, Path> sourceCropPaths) throws IOException { return saveTechnicalProblem(sourceBlocks, requestedTitle, solutionText, solutionImage, sourceCropPaths, null); } public TechnicalProblem saveTechnicalProblem(List<DocumentBlock> sourceBlocks, String requestedTitle, String solutionText, WritableImage solutionImage, java.util.Map<String, Path> sourceCropPaths, String canvasStateJson) throws IOException { TechnicalProblem problem = studyProblemWorkflow.save(requireSession(), currentProjectDirectory(), sourceBlocks, requestedTitle, solutionText, solutionImage, sourceCropPaths, canvasStateJson); statusMessage.set("Problema tecnico guardado: " + problem.id() + (problem.solutionImageAssetId().isBlank() ? "." : ". Lienzo exportado como PNG.")); bumpDocumentMediaRevision(); refreshProjectState(); return problem; } public TechnicalProblem saveTechnicalProblemFromSources(java.util.List<com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft> sourceDrafts, String requestedTitle, String solutionText, WritableImage solutionImage) throws IOException { return saveTechnicalProblemFromSources(sourceDrafts, requestedTitle, solutionText, solutionImage, null); } public TechnicalProblem saveTechnicalProblemFromSources(java.util.List<com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft> sourceDrafts, String requestedTitle, String solutionText, WritableImage solutionImage, String canvasStateJson) throws IOException { TechnicalProblem problem = studyProblemWorkflow.saveFromSources(requireSession(), currentProjectDirectory(), sourceDrafts, requestedTitle, solutionText, solutionImage, canvasStateJson); statusMessage.set("Problema tecnico guardado: " + problem.id() + (problem.solutionImageAssetId().isBlank() ? "." : ". Lienzo exportado como PNG.")); bumpDocumentMediaRevision(); refreshProjectState(); return problem; } public com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemsProjection studyProblemsProjection() { return applicationServices.documentStudy().buildStudyProblemsProjection().build(currentProject().orElse(null), currentProjectDirectory().orElse(null)); } public TechnicalProblem updateTechnicalProblemSolution(String problemId, String solutionText, WritableImage solutionImage, String notes) throws IOException { return updateTechnicalProblemSolution(problemId, solutionText, solutionImage, notes, null); } public TechnicalProblem updateTechnicalProblemSolution(String problemId, String solutionText, WritableImage solutionImage, String notes, String canvasStateJson) throws IOException { TechnicalProblem problem = studyProblemWorkflow.updateSolution(requireSession(), currentProjectDirectory(), problemId, solutionText, solutionImage, notes, canvasStateJson); statusMessage.set("Problema tecnico actualizado: " + problem.id() + "."); bumpDocumentMediaRevision(); refreshProjectState(); return problem; }
    public void deleteTechnicalProblem(String problemId) throws IOException { studyProblemWorkflow.delete(requireSession(), currentProjectDirectory(), problemId); statusMessage.set("Problema tecnico eliminado: " + problemId + "."); bumpDocumentMediaRevision(); refreshProjectState(); } public void exportTechnicalProblemImage(String problemId, Path target) throws IOException { Path output = studyProblemWorkflow.exportSolutionImage(requireSession(), currentProjectDirectory(), problemId, target); statusMessage.set("Solucion PNG exportada: " + output.getFileName() + "."); } public Path exportTechnicalProblemImage(WritableImage image, Path target) throws IOException { Path output = studyProblemWorkflow.exportSolutionImage(image, target); statusMessage.set("Solucion PNG exportada: " + output.getFileName() + "."); return output; } public void exportTechnicalProblemText(String problemId, Path target) throws IOException { Path output = studyProblemWorkflow.exportSolutionText(requireSession(), problemId, target); statusMessage.set("Solucion textual exportada: " + output.getFileName() + "."); } public void exportAllTechnicalProblemImages(Path targetDirectory) throws IOException { var report = studyProblemWorkflow.exportAllSolutionImages(requireSession(), currentProjectDirectory(), targetDirectory); statusMessage.set("Problemas exportados: " + report.exported() + " PNG, omitidos: " + report.skipped() + ", errores: " + report.failed() + ". Manifest: " + report.manifestPath().getFileName() + "."); } public void exportAllTechnicalProblemImagesPdf(Path targetPdf) throws IOException { var report = studyProblemWorkflow.exportAllSolutionImagesAsPdf(requireSession(), currentProjectDirectory(), targetPdf); statusMessage.set("PDF de ejercicios exportado: " + report.pdfPath().getFileName() + ". Paginas: " + report.exported() + ", omitidos: " + report.skipped() + ", errores: " + report.failed() + "."); }
    public Path exportTheatreInterventionContext(String sceneId, String intervencionId, Path targetDirectory) throws IOException {
        TheatreInterventionContextExportWorkflow.Result result = theatreInterventionContextExportWorkflow.export(requireSession(), currentScript.get(), sceneId, intervencionId, targetDirectory);
        statusMessage.set("Paquete IA creado para " + intervencionId + ": " + result.copiedImages() + " imagenes copiadas en " + result.folder() + ".");
        return result.folder();
    }
    public TheatreContextExportEstimate estimateTheatreContextPackages(TheatreContextExportScope scope) { return theatreBulkContextExportWorkflow.estimate(requireSession(), currentScript.get(), scope); }
    public TheatreBulkInterventionContextExportWorkflow.Result exportTheatreContextPackages(TheatreContextExportScope scope, Path targetDirectory) throws IOException { var result = theatreBulkContextExportWorkflow.export(requireSession(), currentScript.get(), scope, targetDirectory); statusMessage.set("Paquetes IA teatrales exportados: " + result.packages() + " paquetes en " + result.root() + "."); return result; }
    public List<TheatreImageGenerationUnit> theatreImageGenerationQueue(TheatreContextExportScope scope) { return theatreImageGenerationWorkflow.queue(requireSession(), currentScript.get(), scope); }
    public List<TheatreImageContextAsset> theatreImageGenerationContextAssets(TheatreImageGenerationUnit unit) { return theatreImageGenerationWorkflow.contextAssets(requireSession(), unit); }
    public ComfyUiVisualEngineClient.ConnectionResult testComfyUi(ComfyUiConnectionSettings settings) { return theatreImageGenerationWorkflow.testConnection(settings); }
    public TheatreGeneratedImageCandidate generateTheatreImageCandidate(TheatreImageGenerationUnit unit, ComfyUiConnectionSettings settings, TheatreImageGenerationPreset preset, boolean assign) throws IOException { return generateTheatreImageCandidate(unit, settings, preset, ImageEnhancementOutputProfile.FHD_1080, assign); }
    public TheatreGeneratedImageCandidate generateTheatreImageCandidate(TheatreImageGenerationUnit unit, ComfyUiConnectionSettings settings, TheatreImageGenerationPreset preset, ImageEnhancementOutputProfile outputProfile, boolean assign) throws IOException {
        return generateTheatreImageCandidate(unit, settings, preset, outputProfile, TheatreImageAspectRatio.WIDE_16_9, assign, null);
    }
    public TheatreGeneratedImageCandidate generateTheatreImageCandidate(TheatreImageGenerationUnit unit, ComfyUiConnectionSettings settings, TheatreImageGenerationPreset preset, ImageEnhancementOutputProfile outputProfile, TheatreImageAspectRatio aspectRatio, boolean assign, Consumer<String> progress) throws IOException {
        var candidate = theatreImageGenerationWorkflow.generate(requireSession(), unit, settings, preset, outputProfile, aspectRatio, assign, progress);
        updateProjectStatusOnFxThread((candidate.approved() ? "Imagen generada y asignada a " : "Candidato IA generado para ") + candidate.interventionId() + ".", true);
        return candidate;
    }
    public TheatreGeneratedImageCandidate generateTheatreTransitionImageCandidate(TheatreImageGenerationUnit unit, String previousAssetId, Path previousFrame, String nextAssetId, Path nextFrame, ComfyUiConnectionSettings settings, TheatreImageGenerationPreset preset, ImageEnhancementOutputProfile outputProfile, TheatreImageAspectRatio aspectRatio, Consumer<String> progress) throws IOException { var candidate = theatreImageGenerationWorkflow.generateTransition(requireSession(), unit, previousAssetId, previousFrame, nextAssetId, nextFrame, settings, preset, outputProfile, aspectRatio, progress); updateProjectStatusOnFxThread("Candidato IA intermedio generado para " + unit.interventionId() + ".", true); return candidate; }
    public TheatreGeneratedFrameCandidate generateTheatreRifeIntermediateFrameCandidate(TheatreIntermediateFrameBatchPlanner.Item item, ComfyUiConnectionSettings settings, Consumer<String> progress) throws IOException { var candidate = theatreImageGenerationWorkflow.generateRifeTransition(requireSession(), item.current(), item.next(), item.previousReference(), item.nextReference(), settings, progress); updateProjectStatusOnFxThread("Frame RIFE intermedio generado para " + candidate.interventionId() + " -> " + candidate.nextInterventionId() + ".", true); return candidate; }
    public TheatreGeneratedImageCandidate approveTheatreGeneratedImageCandidate(TheatreGeneratedImageCandidate candidate) {
        var approved = theatreImageGenerationWorkflow.approve(requireSession(), candidate);
        updateProjectStatusOnFxThread("Candidato IA aprobado para " + approved.interventionId() + ".", true);
        return approved;
    }
    public TheatreGeneratedImageCandidate enhanceTheatreImageCandidate(TheatreGeneratedImageCandidate c, ImageEnhancementOutputProfile p, ImageAspectStrategy s) throws IOException {
        var e = new ImageEnhancementWorkflow(applicationServices).enhanceCandidate(requireSession(), c, p, s);
        updateProjectStatusOnFxThread("Imagen mejorada para " + e.interventionId() + " a " + p.displayName() + ".", true);
        return e;
    }
    public TheatreFrameGenerationWorkflow.Estimate estimateTheatreFrames(TheatreFrameGenerationRequest request) { return theatreFrameGenerationWorkflow.estimate(requireSession(), currentScript.get(), request); }
    public TheatreFrameGenerationResult generateTheatreFrames(TheatreFrameGenerationRequest request, Consumer<String> progress, BooleanSupplier cancelled) throws IOException {
        var result = theatreFrameGenerationWorkflow.generate(requireSession(), currentScript.get(), request, progress, cancelled);
        updateProjectStatusOnFxThread("Frames teatrales generados: " + result.generatedFrames() + " en " + result.root() + ".", true);
        return result;
    }
    public TheatreGeneratedFrameCandidate approveTheatreGeneratedFrameCandidate(TheatreGeneratedFrameCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.transitionFrame()) {
            ProjectSession session = requireSession();
            DocuPodcastProject updated = new UpsertTheatreIntermediateFrameUseCase().upsert(session.project(), candidate.interventionId(), candidate.nextInterventionId(), candidate.assetId(), "Frame inferido entre " + candidate.interventionId() + " y " + candidate.nextInterventionId());
            session.replaceProject(updated, true);
            TheatreGeneratedFrameCandidate frame = new TheatreGeneratedFrameCandidate(candidate.unitId(), candidate.sceneId(), candidate.interventionId(), candidate.segmentId(), candidate.frameIndex(), true, candidate.nextInterventionId(), candidate.assetId(), candidate.outputPath(), true);
            updateProjectStatusOnFxThread("Frame inferido guardado para " + frame.interventionId() + " -> " + frame.nextInterventionId() + ".", true);
            return frame;
        }
        TheatreGeneratedImageCandidate approved = theatreImageGenerationWorkflow.approve(requireSession(), new TheatreGeneratedImageCandidate(candidate.unitId(), candidate.sceneId(), candidate.interventionId(), candidate.segmentId(), candidate.assetId(), candidate.outputPath(), candidate.approved()));
        TheatreGeneratedFrameCandidate frame = new TheatreGeneratedFrameCandidate(candidate.unitId(), candidate.sceneId(), candidate.interventionId(), candidate.segmentId(), candidate.frameIndex(), candidate.transitionFrame(), candidate.nextInterventionId(), approved.assetId(), approved.outputPath(), true);
        updateProjectStatusOnFxThread("Frame aprobado para " + frame.interventionId() + ".", true);
        return frame;
    }
    public List<TheatreProjectLayer.IntervencionVisual> theatreIntervencionesVisuales() { return sessions.activeSession().map(session -> session.project().theatre().intervencionesVisuales()).orElseGet(List::of); } public TheatreAudioTrackTimeline theatreAudioTrackTimeline() { return theatreAudioTrackWorkflow.timeline(applicationServices, sessions.activeSession(), currentScript.get(), currentPlaybackManifest.get()); } public Optional<String> selectedTheatreAudioInterventionId() { return theatreAudioTrackWorkflow.interventionForSelection(sessions.activeSession(), currentScript.get(), selectedScriptSegmentId.get(), selectedDocumentBlockId.get()); } public void selectTheatreAudioIntervention(String id) { theatreAudioTrackWorkflow.blockForIntervention(sessions.activeSession(), id).ifPresent(this::selectDocumentBlock); } public void selectTheatreAudioSegment(String id) { theatreAudioTrackWorkflow.blockForSegment(currentScript.get(), id).ifPresent(this::selectDocumentBlock); }
    public List<TheatreChoralVoiceWorkflow.Option> selectedTheatreChoralVoiceOptions() { return new TheatreChoralVoiceWorkflow().options(sessions.activeSession(), selectedTheatreAudioInterventionId(), audioEngineDescriptor()); } public TheatreChoralVoiceWorkflow.State selectedTheatreChoralVoiceState() { Optional<String> interventionId = selectedTheatreAudioInterventionId(); return new TheatreChoralVoiceWorkflow().state(sessions.activeSession(), interventionId, interventionId.flatMap(this::narrationSegmentForIntervention)); }
    public boolean canRenderSelectedTheatreChoralVoices(List<String> ids) { return theatreChoralVoiceRenderWorkflow.canRender(selectedTheatreChoralVoiceOptions(), ids, currentProjectMode.get() == ProjectMode.THEATRE_PRODUCTION, selectedTheatreAudioInterventionId().isPresent(), audioJobRunning.get() || choralVoiceRenderingProperty().get()); }
    public void renderSelectedTheatreChoralVoices(List<String> characterIds) { try { if (currentProjectMode.get() != ProjectMode.THEATRE_PRODUCTION) throw new IOException("Las voces simultaneas solo estan disponibles en modo Teatro."); if (audioJobRunning.get() || choralVoiceRenderingProperty().get()) throw new IOException("Espera a que termine el trabajo de audio actual."); if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument(); ProjectSession session = requireSession(); Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de renderizar voces simultaneas.")); String interventionId = selectedTheatreAudioInterventionId().orElseThrow(() -> new IOException("Selecciona una intervencion teatral narrable.")); List<String> participants = TheatreChoralVoiceRenderCoordinator.normalize(characterIds); if (!canRenderSelectedTheatreChoralVoices(participants)) throw new IOException("Selecciona al menos dos personajes con voz local disponible."); RenderTheatreChoralVoiceUseCase useCase = applicationServices.theatre().renderChoralVoice(); if (useCase == null) throw new IOException("El render multipersona no esta disponible en esta configuracion de la aplicacion."); saveCurrentProjectAs(projectFile); var request = new TheatreChoralVoiceRenderRequest(session.project(), projectFile, currentScript.get(), interventionId, participants, audioEngineDescriptor()); theatreChoralVoiceRenderWorkflow.start(useCase, request, result -> applyTheatreChoralVoiceResult(session, projectFile, result), statusMessage::set); } catch (Exception ex) { theatreChoralVoiceRenderWorkflow.fail("No se pudo iniciar la voz multipersona: " + rootCauseMessage(ex), statusMessage::set); refreshProjectState(); } }
    private void applyTheatreChoralVoiceResult(ProjectSession session, Path projectFile, TheatreChoralVoiceRenderResult result) { try { session.replaceProject(result.project(), true); activeVoiceLibrary.set(result.project().voiceLibrary()); saveCurrentProjectAs(projectFile); rebuildPlaybackManifestFromLatestJob(); bumpDocumentMediaRevision(); theatreChoralVoiceRenderWorkflow.complete(result.message(), statusMessage::set); } catch (Exception ex) { theatreChoralVoiceRenderWorkflow.fail("La mezcla se genero, pero no se pudo guardar el proyecto: " + rootCauseMessage(ex), statusMessage::set); } finally { refreshProjectState(); } } public void clearSelectedTheatreChoralVoices() { try { var result = new TheatreChoralVoiceWorkflow().clear(requireSession(), selectedTheatreAudioInterventionId().orElse("")); if (result.saved() && currentProjectFile().isPresent()) saveCurrentProjectAs(currentProjectFile().get()); theatreChoralVoiceRenderWorkflow.complete(result.message(), statusMessage::set); rebuildPlaybackManifestFromLatestJob(); bumpDocumentMediaRevision(); refreshProjectState(); } catch (IOException ex) { theatreChoralVoiceRenderWorkflow.fail("No se pudo restaurar la voz simple: " + rootCauseMessage(ex), statusMessage::set); refreshProjectState(); } }
    public List<TheatreProjectLayer.TheatreObject> theatreObjects() { return theatreObjectProfileWorkflow.objects(sessions.activeSession()); }
    public void saveTheatreObjectDescription(String objectId, String displayName, String description) { statusMessage.set(theatreObjectProfileWorkflow.save(sessions.activeSession(), objectId, displayName, description).message()); refreshProjectState(); }
    public List<TheatreProjectLayer.ObjectImage> theatreObjectSceneImages(String objectId, String sceneId) { return theatreObjectImageWorkflow.images(sessions.activeSession(), objectId, sceneId); }
    public void addTheatreObjectSceneImage(String objectId, String sceneId, Path imageFile) throws IOException { statusMessage.set(theatreImageAssetWorkflow.addObjectImage(requireSession(), objectId, sceneId, imageFile).message()); refreshProjectState(); }
    public void replaceTheatreObjectSceneImage(String imageId, Path imageFile) throws IOException {
        statusMessage.set(theatreImageAssetWorkflow.replaceObjectImage(
                requireSession(), currentStoryboard.get(), imageId, imageFile).message());
        refreshProjectState();
    }
    public void updateTheatreObjectSceneImageNote(String imageId, String note) {
        statusMessage.set(theatreImageAssetWorkflow.updateObjectImageNote(
                requireSession(), imageId, note).message());
        refreshProjectState();
    }
    public void deleteTheatreObjectSceneImage(String imageId) {
        statusMessage.set(theatreImageAssetWorkflow.deleteObjectImage(
                requireSession(), currentStoryboard.get(), imageId).message());
        refreshProjectState();
    }
    public Optional<Path> currentSourceDocumentPath() {
        ReadableDocument document = currentDocument.get();
        return document == null ? Optional.empty() : Optional.of(document.sourcePath());
    }
    public Optional<Path> currentProjectDirectory() { return currentProjectFile().map(Path::toAbsolutePath).map(Path::normalize).map(Path::getParent); }
    public Optional<Path> currentExportsDirectory() { return currentProjectDirectory().map(directory -> directory.resolve("exports")); }
    public void createNewProject(String title) { createNewProject(title, ProjectMode.defaultMode()); }
    public void createNewProject(String title, ProjectMode mode) {
        ProjectSession session = projectWorkflow.createNewProject(title, mode);
        DocuPodcastProject project = session.project();
        activeReadingProfile.set(project.readingProfile());
        activeVoiceLibrary.set(project.voiceLibrary());
        currentDocument.set(null); currentScript.set(null); currentStoryboard.set(null); lastStoryboardImageAssetId.set("");
        selectedScriptSegmentId.set(""); selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        intervencionBoundaryStore.clear();
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        activeWorkspace.set(WorkspaceKind.WELCOME_HOME);
        statusMessage.set("Proyecto nuevo creado: " + project.metadata().title() + " (" + project.metadata().mode().displayName() + "). Guarda el proyecto para crear el archivo .docupodcast.json.");
        refreshProjectState();
    }
    public void openProject(Path sourceFile) throws IOException {
        OpenedProjectContext opened = projectWorkflow.openProject(sourceFile);
        boolean repairedAviadores = theatreDemoManifestWorkflow.repairAviadoresLegacyAssets(opened.session());
        DocuPodcastProject project = opened.session().project();
        if (repairedAviadores) {
            projectWorkflow.saveProject(opened.session(), sourceFile, project.readingProfile(), project.voiceLibrary());
        }
        activeReadingProfile.set(project.readingProfile());
        activeVoiceLibrary.set(project.voiceLibrary());
        currentDocument.set(opened.importedDocument().orElse(null));
        currentScript.set(opened.narrationScript().orElse(null));
        currentStoryboard.set(opened.storyboard().orElse(null));
        lastStoryboardImageAssetId.set(opened.lastStoryboardImageAssetId());
        selectedScriptSegmentId.set(opened.selectedScriptSegmentId());
        selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        theatreBoundaryWorkflow.hydrate(project, intervencionBoundaryStore);
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        activeWorkspace.set(opened.activeWorkspace());
        loadLatestPersistedAudioStatus(sourceFile);
        if (currentScript.get() != null) {
            rebuildPlaybackManifestFromLatestJob();
        }
        statusMessage.set("Proyecto abierto: " + project.metadata().title()
                + ". Perfil de lectura: " + project.readingProfile().name()
                + ". Rehidratación: " + opened.hydration().statusLabel() + ".");
        if (repairedAviadores) {
            statusMessage.set(statusMessage.get() + " Assets teatrales del demo reparados.");
        }
        refreshProjectState();
    }
    public void saveCurrentProject() throws IOException {
        ProjectSession session = requireSession();
        Path target = session.projectFile().orElseThrow(() -> new IOException("El proyecto todavía no tiene ruta. Usa Guardar como."));
        saveCurrentProjectAs(target);
    }
    public void saveCurrentProjectAs(Path targetFile) throws IOException {
        ProjectSession session = requireSession();
        theatreBoundaryWorkflow.persist(session, intervencionBoundaryStore);
        projectWorkflow.saveProject(session, targetFile, activeReadingProfile.get(), activeVoiceLibrary.get());
        sessions.activeSession().flatMap(ProjectSession::importedDocument).ifPresent(doc -> Platform.runLater(() -> currentDocument.set(doc)));
        String saveMsg = "Proyecto guardado en: " + targetFile + projectSourceCopyStatusSuffix() + ".";
        Platform.runLater(() -> statusMessage.set(saveMsg));
        refreshProjectState();
    }
    private String projectSourceCopyStatusSuffix() {
        return currentSourceDocumentPath()
                .map(path -> ". Fuente canónica: " + path.getFileName() + " dentro de la carpeta source del proyecto")
                .orElse("");
    }
    public void closeCurrentProject() {
        projectWorkflow.closeProject();
        currentDocument.set(null); currentScript.set(null); currentStoryboard.set(null); lastStoryboardImageAssetId.set("");
        selectedScriptSegmentId.set(""); selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        intervencionBoundaryStore.clear();
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        activeReadingProfile.set(applicationServices.readingProfile().createDefaultProfile().create());
        activeVoiceLibrary.set(applicationServices.voice().createDefaultVoiceLibrary().create());
        loadReadingComfortSettings();
        activeWorkspace.set(WorkspaceKind.WELCOME_HOME);
        statusMessage.set("Proyecto cerrado.");
        refreshProjectState();
    }
    public ReadableDocument importAndClassifySourceDocument(Path sourceFile) throws IOException {
        return documentIntake.importAndClassify(sourceFile, activeReadingProfile.get());
    }
    public void attachImportedDocument(ReadableDocument classified) throws IOException {
        ReadingProfile profile = activeReadingProfile.get();
        if (!sessions.hasActiveSession()) { createNewProject(classified.title()); }
        ProjectSession session = requireSession();
        documentIntake.attachImportedDocument(session, classified, profile);
        currentDocument.set(classified); currentScript.set(null); currentStoryboard.set(null);
        lastStoryboardImageAssetId.set(""); selectedScriptSegmentId.set(""); selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        resetPlaybackState(); activeAudioJobStatus.set(AudioJobStatusDto.idle()); audioJobRunning.set(false); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        statusMessage.set("Documento fuente importado en modo solo lectura y perfil aplicado: %d bloques, %d narrables, %d imágenes, %d tablas, %d ignorados.".formatted(
                classified.blocks().size(), classified.narratableBlockCount(), classified.imageNoticeCount(), classified.tableNoticeCount(), classified.ignoredCount()));
        refreshProjectState();
    }
    public Path createPdfSourceFromImageFolder(Path sourceFolder) throws IOException { Path projectDirectory = currentProjectDirectory().orElseThrow(() -> new IOException("Guarda el proyecto antes de crear una fuente PDF desde carpeta.")); String folderName = sourceFolder == null || sourceFolder.getFileName() == null ? "imagenes" : sourceFolder.getFileName().toString(); Path outputDirectory = projectDirectory.resolve("source").resolve("generated-pdf-from-images"); Files.createDirectories(outputDirectory); Path createdPdf = applicationServices.document().createPdfFromImageFolder().create(sourceFolder, uniqueGeneratedPdfPath(outputDirectory, safeGeneratedPdfStem(folderName))); attachImportedDocument(importAndClassifySourceDocument(createdPdf)); statusMessage.set("Fuente PDF creada desde carpeta de imagenes: " + createdPdf.getFileName() + "."); refreshProjectState(); return createdPdf; }
    private void resetDocumentSideDocksForProjectStart() { documentRightRailVisible.set(false); documentPlaybarDocked.set(false); }
    private static String safeGeneratedPdfStem(String value) { String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("-{2,}", "-").replaceAll("^[-.]+|[-.]+$", ""); return safe.isBlank() ? "imagenes" : safe; }
    private static Path uniqueGeneratedPdfPath(Path directory, String stem) { Path candidate = directory.resolve(stem + ".pdf"); for (int index = 2; Files.exists(candidate); index++) { candidate = directory.resolve(stem + "-" + index + ".pdf"); } return candidate; }
    public void importWordDocument(Path sourceFile) throws IOException { attachImportedDocument(importAndClassifySourceDocument(sourceFile)); }
    public Optional<UserVisibleDecision> refreshSourceDocumentDecision() {
        if (audioJobRunning.get()) {
            cancelActiveAudioJobSilently();
            waitingForBufferedSegmentAfter = "";
            documentPlaybackRequested = false;
        }
        ReadableDocument document = currentDocument.get();
        if (document == null) {
            statusMessage.set("No hay documento fuente para refrescar.");
            return Optional.empty();
        }
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) {
            UserVisibleDecision decision = UserVisibleDecision.warning("No se puede refrescar todavía", "Abre o crea un proyecto antes de refrescar el documento fuente.");
            statusMessage.set(decision.headline() + ": " + decision.message());
            return Optional.of(decision);
        }
        try {
            SourceDocumentRefreshOutcome outcome = sourceDocumentRefresh.refresh(session.get(), document, activeReadingProfile.get());
            if (outcome.refreshedDocumentAvailable()) {
                currentDocument.set(outcome.refreshedDocument());
            }
            if (outcome.report().hasContentChanges()) {
                resetPlaybackState();
                activeAudioJobStatus.set(AudioJobStatusDto.idle());
                audioJobRunning.set(false);
            }
            String refreshSummary = outcome.report().summary();
            if (outcome.projectContentChanged() && !outcome.report().hasContentChanges()) {
                refreshSummary = "Visuales internos del Word actualizados; guarda el proyecto para conservarlos.";
            }
            statusMessage.set("Refrescar contenido: " + refreshSummary);
            refreshProjectState();
            return SourceDocumentRefreshDecisionFactory.fromReport(outcome.report(), outcome.projectContentChanged());
        } catch (IOException ex) {
            UserVisibleDecision decision = UserVisibleDecision.error("No se pudo refrescar el documento fuente", ex.getMessage(), ex.toString());
            statusMessage.set(decision.headline() + ": " + decision.message());
            return Optional.of(decision);
        }
    }
    public void refreshSourceDocument() {
        refreshSourceDocumentDecision().ifPresent(value -> statusMessage.set(value.headline() + ": " + value.message()));
    }
    public void updateDocumentBlockType(String blockId, DocumentBlockType type) {
        ReadableDocument document = currentDocument.get();
        if (document == null) {
            statusMessage.set("No hay documento activo para reclasificar.");
            return;
        }
        ReadableDocument updated = applicationServices.document().updateDocumentBlockType().update(document, blockId, type);
        currentDocument.set(updated);
        sessions.activeSession().ifPresent(session -> session.setImportedDocument(updated));
        statusMessage.set("Bloque " + blockId + " marcado como " + type.displayName() + ".");
        refreshProjectState();
    }
    public void updateReadingProfile(ReadingProfile profile) {
        Objects.requireNonNull(profile, "profile");
        activeReadingProfile.set(profile);
        sessions.activeSession().ifPresent(session -> session.replaceProject(session.project().withReadingProfile(profile), true));
        statusMessage.set("Perfil de lectura actualizado: " + profile.name() + ". Aplica el perfil para reclasificar el documento activo.");
        refreshProjectState();
    }
    public void applyReadingProfile(ReadingProfile profile) {
        updateReadingProfile(profile);
        ReadableDocument document = currentDocument.get();
        if (document == null) {
            statusMessage.set("Perfil de lectura guardado. No hay documento activo para aplicar.");
            return;
        }
        ReadableDocument updated = applicationServices.readingProfile().applyReadingProfile().apply(document, profile);
        currentDocument.set(updated);
        sessions.activeSession().ifPresent(session -> session.setImportedDocument(updated));
        currentScript.set(null); resetPlaybackState(); activeAudioJobStatus.set(AudioJobStatusDto.idle()); audioJobRunning.set(false);
        statusMessage.set("Perfil aplicado: " + profile.name() + ". Estructura: " + updated.structuralBlockCount() + " bloques; ignorados: " + updated.ignoredCount() + ".");
        refreshProjectState();
    }
    public void applyDefaultReadingProfile() { applyReadingProfile(activeReadingProfile.get()); }
    public boolean readTablesAndTextBoxesForNarration() { ReadingProfile profile = activeReadingProfile.get(); return profile != null && profile.tablePolicy() == TableNarrationPolicy.READ_STRUCTURED; }
    public void setReadTablesAndTextBoxesForNarration(boolean enabled) { ReadingProfile base = activeReadingProfile.get() == null ? ReadingProfile.academicDefaults() : activeReadingProfile.get(); TableNarrationPolicy next = enabled ? TableNarrationPolicy.READ_STRUCTURED : TableNarrationPolicy.IGNORE_TABLES; if (base.tablePolicy() == next) { return; } ReadingProfile updated = new ReadingProfile(base.id(), base.name(), base.description(), base.headingRules(), base.imagePolicy(), next); activeReadingProfile.set(updated); sessions.activeSession().ifPresent(session -> session.replaceProject(session.project().withReadingProfile(updated), true)); if (audioJobRunning.get()) { cancelActiveAudioJobSilently(); } invalidatePersistedAudioForNarrationChange(); if (currentDocument.get() != null) { buildNarrationScriptFromDocument(); } statusMessage.set((enabled ? "Lectura estructurada de cuadros y tablas activada." : "Lectura de cuadros y tablas desactivada.") + " Reconstruye fragmentos de audio para aplicar el cambio."); refreshProjectState(); }
    public void setReadAfterColonForNarration(boolean enabled) {
        if (readAfterColonForNarration.get() == enabled) { return; }
        readAfterColonForNarration.set(enabled);
        if (currentDocument.get() == null) { statusMessage.set("Opción de lectura después de dos puntos guardada. Abre un documento para aplicarla."); refreshProjectState(); return; }
        if (audioJobRunning.get()) { cancelActiveAudioJobSilently(); }
        invalidatePersistedAudioForNarrationChange();
        buildNarrationScriptFromDocument();
        statusMessage.set((enabled ? "Se leerá desde después de ':' cuando parezca diálogo." : "Se leerá el texto completo antes y después de ':'.") + " Reconstruir fragmentos de audio aplicará el cambio.");
        refreshProjectState();
    }
    public void setReadAfterColonForNarration(boolean enabled, boolean renderFromSelection) {
        setReadAfterColonForNarration(enabled);
        if (currentDocument.get() != null && renderFromSelection) {
            Optional<NarrationSegment> segment = selectedDocumentSegmentOrSelected();
            if (segment.isPresent()) {
                submitAudioGenerationFromSegment(segment.get(), false);
                return;
            }
        }
        if (currentDocument.get() != null) {
            generateAudioChunksWithoutPlayback();
        }
    }
    public ReadingProfilePreview previewReadingProfile(ReadingProfile profile) {
        ReadableDocument document = currentDocument.get();
        if (document == null) {
            return new ReadingProfilePreview(java.util.List.of());
        }
        return applicationServices.readingProfile().previewReadingProfile().preview(document, profile);
    }
    public void buildNarrationScriptFromDocument() {
        ReadableDocument document = currentDocument.get();
        Optional<String> readinessProblem = documentNarration.readinessProblem(document, activeReadingProfile.get());
        if (readinessProblem.isPresent()) {
            statusMessage.set(readinessProblem.get());
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        ProjectSession session = requireSession();
        NarrationScriptDocument script = documentNarration.buildNarrationProjection(session, document, readAfterColonForNarration.get());
        applyImportedOrGeneratedScript(session, script);
        var issues = documentNarration.validate(script);
        statusMessage.set(documentNarration.projectionReadyMessage(script, issues));
        refreshProjectState();
    }
    private void actualizarFrameActivo() {
        String blockId = selectedDocumentBlockId.get();
        if (blockId == null || blockId.isBlank()) {
            activePlacementAlias.set("");
            activeTextActionPlacement.set(TheatreProjectLayer.TextActionPlacement.empty());
            return;
        }
        String alias = resolvePlacementAliasFromBlockId(blockId);
        activePlacementAlias.set(alias == null ? "" : alias);
        if (alias == null || alias.isBlank()) {
            activeTextActionPlacement.set(TheatreProjectLayer.TextActionPlacement.empty());
            return;
        }
        activeTextActionPlacement.set(
                theatreTextActionPlacements().stream()
                        .filter(p -> alias.equals(p.intervencionId()))
                        .findFirst()
                        .orElse(TheatreProjectLayer.TextActionPlacement.empty()));
    }
    private String resolvePlacementAliasFromBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) return "";
        var all = IntervencionCatalogo.intervenciones(currentDocument.get(), currentScript.get());
        for (var info : all) {
            if (blockId.equals(info.blockId())) return info.alias();
        }
        return "";
    }
    public void selectDocumentBlock(String blockId) {
        String normalized = documentSelectionWorkflow.normalizeBlockId(blockId);
        selectedDocumentBlockId.set(normalized);
        clearDocumentTextRange();
        if (normalized.isBlank()) { selectedScriptSegmentId.set(""); actualizarFrameActivo();
            statusMessage.set("Seleccion de fragmento limpia."); refreshProjectState(); return; }
        Optional<DocumentFragmentRailPresentation> visual = visualFragmentForSelection(normalized, null); visual.ifPresent(this::pinVisualFragment); Optional<NarrationSegment> linked = visual.flatMap(fragment -> findSegment(fragment.segmentId())).or(() -> firstSegmentForDocumentBlock(normalized));
        if (linked.isPresent()) {
            selectedScriptSegmentId.set(linked.get().id());
            PlaybackManifest manifest = ensurePlaybackManifestLoaded();
            if (manifest != null && !manifest.emptyManifest() && manifest.cueForSegment(linked.get().id()).isPresent()) {
                playbackCursor.set(applicationServices.playback().seekPlayback().seek(playbackCursor.get(), manifest, linked.get().id()));
            }
            statusMessage.set("Bloque seleccionado: " + normalized + ". Reproducir desde aquí usará " + linked.get().id() + ".");
        } else {
            statusMessage.set("Bloque seleccionado: " + normalized + ". Escuchar documento preparará la lectura y el audio sin modificar el Word original.");
        }
        actualizarFrameActivo();
        refreshProjectState();
    }
    public void selectDocumentTextRange(DocumentTextRange range, String selectedText) {
        if (range == null || range.collapsed()) { clearDocumentTextRange(); refreshProjectState(); return; }
        selectedDocumentBlockId.set(range.blockId());
        selectedDocumentTextRange.set(range);
        applySelectionLabels(documentSelectionWorkflow.sentenceSelection(range, selectedText, currentDocument.get()));
        Optional<DocumentFragmentRailPresentation> visual = visualFragmentForSelection(range.blockId(), range); visual.ifPresent(this::pinVisualFragment); Optional<NarrationSegment> linked = visual.flatMap(fragment -> findSegment(fragment.segmentId())).or(() -> firstSegmentForDocumentBlock(range.blockId()));
        linked.ifPresent(segment -> {
            selectedScriptSegmentId.set(segment.id());
            alignPlaybackCursorToSelectedSentence(segment, range, selectedText);
        });
        String target = linked.map(NarrationSegment::id).orElse("pendiente de preparación");
        statusMessage.set("Oración seleccionada en " + range.displayLabel()
                + " → " + target + ". Las capas se guardarán en el proyecto, no dentro del Word.");
        refreshProjectState();
    }
    public void selectDocumentFragmentRailItem(DocumentFragmentRailPresentation fragment) {
        if (fragment == null) {
            clearDocumentTextRange();
            refreshProjectState();
            return;
        }
        DocumentTextRange range = new DocumentTextRange(fragment.blockId(), fragment.startOffset(), fragment.endOffset());
        pinVisualFragment(fragment);
        selectedDocumentBlockId.set(range.blockId());
        selectedDocumentTextRange.set(range);
        applySelectionLabels(documentSelectionWorkflow.sentenceSelection(range, fragment.preview(), currentDocument.get()));
        Optional<NarrationSegment> linked = findSegment(fragment.segmentId()).or(() -> firstSegmentForDocumentBlock(range.blockId()));
        linked.ifPresent(segment -> {
            selectedScriptSegmentId.set(segment.id());
            alignPlaybackCursorToSelectedSentence(segment, range, fragment.preview());
        });
        String target = linked.map(NarrationSegment::id).orElse("pendiente de preparacion");
        statusMessage.set("Fragmento visual seleccionado en " + range.displayLabel()
                + " -> " + target + ". El panel Imagen se actualizo con su miniatura.");
        refreshProjectState();
    }
    private void alignPlaybackCursorToSelectedSentence(NarrationSegment segment, DocumentTextRange range, String selectedText) {
        if (segment == null || range == null || playbackTransport.playerPlaying() || playbackTransport.continuationActive()) { return; }
        PlaybackManifest manifest = currentPlaybackManifest.get();
        if (manifest == null || manifest.emptyManifest()) { manifest = ensurePlaybackManifestLoaded(); }
        if (manifest == null || manifest.emptyManifest()) { return; }
        playbackSelectionResolver.cueForSelection(manifest, segment, range, selectedText).ifPresent(cue -> playbackCursor.set(new PlaybackCursor(cue.segmentId(), cue.startSeconds(), true)));
    }
    public void prepareDocumentLayerAssignment(NarrativeLayerKind kind) {
        NarrativeLayerKind normalizedKind = kind == null ? NarrativeLayerKind.NOTE : kind;
        assignDocumentLayerTarget(normalizedKind, preferredLayerTargetId(normalizedKind))
                .ifPresent(outcome -> statusMessage.set(outcome.message()));
        refreshProjectState();
    }
    public void assignVoiceToSelectedDocumentRange(String voiceProfileId) {
        assignDocumentLayerTarget(NarrativeLayerKind.VOICE, voiceProfileId)
                .ifPresent(outcome -> statusMessage.set(outcome.message()));
        refreshProjectState();
    }
    public void assignVoiceToneToSelectedDocumentRange(String voiceProfileId, VoiceReferenceTone requestedTone) {
        String voiceId = voiceProfileId == null ? "" : voiceProfileId.strip();
        VoiceReferenceTone requested = requestedTone == null ? VoiceReferenceTone.NEUTRAL : requestedTone;
        if (voiceId.isBlank()) { documentVoiceToneStatus.set("Selecciona una voz antes de elegir tono."); statusMessage.set("Selecciona una voz antes de elegir tono."); refreshProjectState(); return; }
        VoiceToneReferenceResolution resolution = applicationServices.voice().resolveVoiceToneReference().resolve(activeVoiceLibrary.get(), voiceId, requested);
        documentVoiceToneStatus.set(toneStatus(resolution));
        if (!resolution.available()) { statusMessage.set(resolution.userMessage()); refreshProjectState(); return; }
        removeExistingLayerOfKindSilently(NarrativeLayerKind.VOICE);
        Optional<NarrativeLayerCoordinator.AssignmentOutcome> voiceOutcome = assignDocumentLayerTarget(NarrativeLayerKind.VOICE, voiceId);
        if (voiceOutcome.isEmpty() || !voiceOutcome.get().assigned()) { statusMessage.set(voiceOutcome.map(NarrativeLayerCoordinator.AssignmentOutcome::message).orElse("No se pudo asignar la voz al fragmento seleccionado.")); refreshProjectState(); return; }
        removeExistingLayerOfKindSilently(NarrativeLayerKind.EMOTION);
        assignDocumentLayerTarget(NarrativeLayerKind.EMOTION, resolution.resolvedTone().layerTargetId());
        statusMessage.set(voiceOutcome.get().message() + " " + toneStatus(resolution));
        refreshProjectState();
    }
    public void assignEmotionStyleToSelectedDocumentRange(String performanceStyleId) {
        assignDocumentLayerTarget(NarrativeLayerKind.EMOTION, performanceStyleId)
                .ifPresent(outcome -> statusMessage.set(outcome.message()));
        refreshProjectState();
    }
    public void assignEmotionStyleToDocumentBlock(String blockId, String performanceStyleId) {
        String normalizedBlockId = documentSelectionWorkflow.normalizeBlockId(blockId);
        String styleId = performanceStyleId == null ? "" : performanceStyleId.strip();
        if (normalizedBlockId.isBlank() || styleId.isBlank()) {
            statusMessage.set("Selecciona texto y emocion antes de asignar.");
            refreshProjectState();
            return;
        }
        selectDocumentBlock(normalizedBlockId);
        removeExistingLayerOfKindSilently(NarrativeLayerKind.EMOTION);
        assignDocumentLayerTarget(NarrativeLayerKind.EMOTION, styleId)
                .ifPresent(outcome -> statusMessage.set(outcome.message()));
        refreshProjectState();
    }
    public void importImageForSelectedDocumentRange(Path imageFile, NarrativeLayerKind imageKind) throws IOException {
        Objects.requireNonNull(imageFile, "imageFile"); NarrativeLayerKind normalizedKind = imageKind == NarrativeLayerKind.BRIDGE_IMAGE ? NarrativeLayerKind.BRIDGE_IMAGE : NarrativeLayerKind.IMAGE;
        Optional<ProjectSession> maybeSession = sessions.activeSession();
        if (maybeSession.isEmpty()) { statusMessage.set("Crea o abre un proyecto antes de importar imagenes."); refreshProjectState(); return; }
        ProjectSession session = maybeSession.get();
        Path projectFile = session.projectFile().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de importar imagenes para mantener rutas relativas."));
        if (selectedDocumentBlockId.get() == null || selectedDocumentBlockId.get().isBlank()) { statusMessage.set("Selecciona una oracion o bloque del documento antes de importar una imagen."); refreshProjectState(); return; }
        var result = applicationServices.storyboard().importImageAsset().importImage(session.project(), projectFile, imageFile);
        session.replaceProject(result.project(), true);
        if (normalizedKind == NarrativeLayerKind.IMAGE) lastStoryboardImageAssetId.set(result.imageAsset().id());
        removeExistingLayerOfKindSilently(normalizedKind); Optional<NarrativeLayerCoordinator.AssignmentOutcome> assignment = assignDocumentLayerTarget(normalizedKind, result.imageAsset().id());
        String imageLabel = normalizedKind == NarrativeLayerKind.BRIDGE_IMAGE ? "Imagen puente" : "Imagen";
        if (assignment.isPresent()) {
            if (normalizedKind == NarrativeLayerKind.IMAGE) refreshStoryboardFromImageLayers(session);
            statusMessage.set(imageLabel + " importada: " + result.imageAsset().displayName() + ". Copiada dentro del proyecto y asociada a la seleccion. " + assignment.get().message());
        } else {
            statusMessage.set(imageLabel + " importada: " + result.imageAsset().displayName() + ". Copiada dentro del proyecto. Selecciona una oracion o bloque para asociarla.");
        }
        if (normalizedKind == NarrativeLayerKind.IMAGE) {
            pinVisualFragment(selectedDocumentSegmentOrSelected().map(NarrationSegment::id).orElse(""),
                    assetUri(currentProjectDirectory(), result.imageAsset()).orElse(""));
        }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        bumpDocumentMediaRevision();
        refreshProjectState();
    }
    public void importImageForSegment(String segmentId, Path imageFile, NarrativeLayerKind imageKind) throws IOException { selectNarrativeVisualFragment(segmentId); importImageForSelectedDocumentRange(imageFile, imageKind); activeWorkspace.set(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION); refreshProjectState(); }
    public void generateNarrativeImageForSegment(String segmentId) throws IOException { ProjectSession session = requireSession(); NarrationSegment segment = findSegment(segmentId).orElseThrow(() -> new IOException("No se encontro el fragmento narrativo solicitado.")); var generated = new NarrativeImageGenerationWorkflow(applicationServices).generate(session, segment, applicationServices.settings().loadOperationalSettings().load(), message -> statusMessage.set(message == null || message.isBlank() ? "Generando imagen narrativa." : message)); selectNarrativeVisualFragment(segment.id()); lastStoryboardImageAssetId.set(generated.importResult().imageAsset().id()); removeExistingLayerOfKindSilently(NarrativeLayerKind.IMAGE); Optional<NarrativeLayerCoordinator.AssignmentOutcome> assignment = assignDocumentLayerTarget(NarrativeLayerKind.IMAGE, generated.importResult().imageAsset().id()); assignment.ifPresent(outcome -> { refreshStoryboardFromImageLayers(session); statusMessage.set(generated.message() + " " + outcome.message()); }); activeWorkspace.set(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION); bumpDocumentMediaRevision(); refreshProjectState(); }
    public void removeImageAssignmentForSegment(String segmentId, NarrativeLayerKind imageKind) { selectNarrativeVisualFragment(segmentId); if (imageKind == NarrativeLayerKind.BRIDGE_IMAGE) removeAssignmentOfKindForSelectedDocumentRange(NarrativeLayerKind.BRIDGE_IMAGE); else removeImageAssignmentForSelectedDocumentRange(); activeWorkspace.set(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION); refreshProjectState(); }
    public void selectNarrativeVisualFragment(String segmentId) { selectDocumentBlockForStoryboardSegment(segmentId); activeWorkspace.set(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION); }
    public void copyFragmentImageToAdjacentFragment(DocumentFragmentRailPresentation source, DocumentFragmentRailPresentation target) {
        if (source == null || target == null) { statusMessage.set("No hay un fragmento anterior o posterior disponible para copiar la imagen."); refreshProjectState(); return; }
        if (!source.imageReady()) { statusMessage.set("Ese fragmento no tiene imagen asociada para copiar."); refreshProjectState(); return; }
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("Abre un proyecto antes de copiar imágenes entre fragmentos."); refreshProjectState(); return; }
        DocumentTextRange targetRange = new DocumentTextRange(target.blockId(), target.startOffset(), target.endOffset());
        Optional<NarrationSegment> linked = findSegment(target.segmentId()).or(() -> firstSegmentForDocumentBlock(target.blockId()));
        Optional<ScriptTextRange> scriptRange = narrativeLayerWorkflow.scriptRangeForLayer(linked, targetRange, target.preview());
        if (scriptRange.isEmpty()) { statusMessage.set("No se pudo ubicar el fragmento destino para copiar la imagen."); refreshProjectState(); return; }
        findFirstAssignmentOfKind(session.get(), scriptRange.get(), NarrativeLayerKind.IMAGE).ifPresent(existing -> session.get().replaceProject(session.get().project().withoutNarrativeLayerAssignment(existing.id()), true));
        NarrativeLayerCoordinator.AssignmentOutcome outcome = narrativeLayerWorkflow.assign(session.get(), NarrativeLayerKind.IMAGE, scriptRange.get(), targetRange, currentDocument.get(), target.blockId(), target.preview(), source.imageAssetId());
        if (!outcome.assigned()) { statusMessage.set(outcome.message().isBlank() ? "No se pudo copiar la imagen al fragmento destino." : outcome.message()); refreshProjectState(); return; }
        refreshStoryboardFromImageLayers(session.get());
        bumpDocumentMediaRevision();
        selectDocumentTextRange(targetRange, target.preview());
        statusMessage.set("Imagen copiada al fragmento “" + target.title() + "”. El panel Imagen se actualizó con la misma miniatura.");
        refreshProjectState();
    }
    public void importUserAudioForSelectedDocumentRange(Path audioFile) throws IOException {
        importUserMediaAndAssignToSelectedDocumentRange(audioFile, "Audio del computador");
    }
    public void extractVideoAudioForSelectedDocumentRange(Path videoFile) throws IOException {
        importUserMediaAndAssignToSelectedDocumentRange(videoFile, "Audio extraído de video");
    }
    public void reportUserVisibleError(String message) {
        statusMessage.set(message == null || message.isBlank() ? "No se pudo completar la acción." : message.strip());
        refreshProjectState();
    }
    private void importUserMediaAndAssignToSelectedDocumentRange(Path mediaFile, String actionLabel) throws IOException {
        Objects.requireNonNull(mediaFile, "mediaFile");
        Optional<ProjectSession> maybeSession = sessions.activeSession();
        if (maybeSession.isEmpty()) { statusMessage.set("Crea o abre un proyecto antes de importar audio del computador."); refreshProjectState(); return; }
        ProjectSession session = maybeSession.get();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de importar audio o video para mantener rutas relativas."));
        if (selectedDocumentBlockId.get() == null || selectedDocumentBlockId.get().isBlank()) { statusMessage.set("Selecciona una oración o bloque del documento antes de importar audio."); refreshProjectState(); return; }
        var importResult = applicationServices.media().importUserMediaAsset().importMedia(session.project(), projectFile, mediaFile);
        session.replaceProject(importResult.project(), true);
        Optional<NarrativeLayerCoordinator.AssignmentOutcome> assignment = assignDocumentLayerTarget(NarrativeLayerKind.HUMAN_AUDIO, importResult.audioAsset().id());
        String prefix = (actionLabel == null || actionLabel.isBlank() ? "Audio" : actionLabel.strip()) + ": " + importResult.message();
        statusMessage.set(assignment.isPresent() ? prefix + " " + assignment.get().message() : prefix + " Selecciona una oración o bloque para asignarlo.");
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        refreshProjectState();
    }
    private void removeExistingLayerOfKindSilently(NarrativeLayerKind kind) {
        sessions.activeSession().ifPresent(session -> {
            Optional<ScriptTextRange> range = narrativeLayerWorkflow.scriptRangeForLayer(selectedDocumentSegmentOrSelected(), selectedDocumentTextRange.get(), selectedDocumentTextPreview);
            range.ifPresent(r -> narrativeLayerWorkflow.removeFirstAssignmentOfKind(session, r, kind));
        });
    }
    private static String toneStatus(VoiceToneReferenceResolution resolution) { return NarrativeLayerCoordinator.toneStatus(resolution); }
    private Optional<NarrativeLayerCoordinator.AssignmentOutcome> assignDocumentLayerTarget(NarrativeLayerKind kind, String preferredTargetId) {
        NarrativeLayerKind normalizedKind = kind == null ? NarrativeLayerKind.NOTE : kind;
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("Crea o abre un proyecto antes de asignar capas al documento."); return Optional.empty(); }
        String blockId = selectedDocumentBlockId.get();
        if (blockId == null || blockId.isBlank()) { statusMessage.set("Selecciona una oración o bloque del documento antes de asignar " + normalizedKind.displayName().toLowerCase(java.util.Locale.ROOT) + "."); return Optional.empty(); }
        if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument();
        Optional<NarrationSegment> linked = firstSegmentForDocumentBlock(blockId);
        if (linked.isEmpty()) { statusMessage.set("No se encontró un fragmento preparado para ese texto. Pulsa Escuchar documento para preparar lectura y audio primero."); return Optional.empty(); }
        linked.ifPresent(segment -> selectedScriptSegmentId.set(segment.id()));
        Optional<ScriptTextRange> scriptRange = narrativeLayerWorkflow.scriptRangeForLayer(linked, selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        if (scriptRange.isEmpty()) { statusMessage.set("No se pudo calcular el rango narrativo. Selecciona otra oración o bloque."); return Optional.empty(); }
        ProjectSession active = sessions.activeSession().orElse(session.get());
        return Optional.of(narrativeLayerWorkflow.assign(active, normalizedKind, scriptRange.get(), selectedDocumentTextRange.get(), currentDocument.get(), blockId, selectedDocumentTextPreview, preferredTargetId == null ? "" : preferredTargetId));
    }
    private String preferredLayerTargetId(NarrativeLayerKind kind) { return kind == NarrativeLayerKind.IMAGE ? lastStoryboardImageAssetId.get() : ""; }
    public void removePrimaryAssignmentForSelectedDocumentRange() {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("No hay proyecto activo para quitar capas."); return; }
        Optional<ScriptTextRange> range = narrativeLayerWorkflow.scriptRangeForLayer(selectedDocumentSegmentOrSelected(), selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        if (range.isEmpty()) { statusMessage.set("Selecciona una oración o bloque con capa principal antes de quitarla."); return; }
        statusMessage.set(narrativeLayerWorkflow.removePrimaryAssignment(session.get(), range.get()).message());
        refreshProjectState();
    }
    public void removeAllSpecificVoicesFromDocument() {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("No hay proyecto activo para eliminar voces específicas."); return; }
        int removed = narrativeLayerWorkflow.removeAllSpecificVoices(session.get());
        if (removed == 0) { statusMessage.set("El documento no tiene voces específicas por fragmento."); return; }
        statusMessage.set(invalidatePersistedAudioAfterVoiceSelection() ? "Voces específicas eliminadas: " + removed + ". Fragmentos de audio anteriores eliminados para usar la voz predeterminada." : "Voces específicas eliminadas: " + removed + ".");
        refreshProjectState();
    }
    public void removeImageAssignmentForSelectedDocumentRange() {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("No hay proyecto activo para quitar la imagen."); return; }
        Optional<NarrationSegment> linked = selectedDocumentSegmentOrSelected();
        Optional<ScriptTextRange> selectedRange = narrativeLayerWorkflow.scriptRangeForLayer(linked, selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        if (selectedRange.isEmpty()) { statusMessage.set("Selecciona una oración o bloque con imagen antes de quitarla."); return; }
        Optional<NarrativeLayerAssignment> removable = findFirstAssignmentOfKind(session.get(), selectedRange.get(), NarrativeLayerKind.IMAGE);
        if (removable.isEmpty()) { statusMessage.set("El fragmento seleccionado no tiene imagen asociada."); return; }
        var result = projectImageWorkflow.removeImageAssignment(session.get(), removable.get(), currentStoryboard.get(), currentProjectFile(), lastStoryboardImageAssetId.get());
        if (result.assetDeleted() && removable.get().targetId().equals(lastStoryboardImageAssetId.get())) lastStoryboardImageAssetId.set("");
        currentStoryboard.set(result.storyboard());
        selectedVisualFragmentImageUri.set("");
        bumpDocumentMediaRevision();
        statusMessage.set(result.message());
        refreshProjectState();
    }
    public void removeAllDocumentImages() {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("No hay proyecto activo para borrar imágenes."); return; }
        var result = projectImageWorkflow.removeAllDocumentImages(session.get(), currentStoryboard.get(), currentProjectFile());
        if (result.nothingToRemove()) { statusMessage.set("No hay imágenes asignadas para eliminar."); return; }
        lastStoryboardImageAssetId.set("");
        clearVisualFragmentSelection();
        currentStoryboard.set(result.storyboard());
        bumpDocumentMediaRevision();
        statusMessage.set("Imágenes eliminadas del proyecto: " + result.removedCount() + ". Las asignaciones visuales fueron limpiadas.");
        refreshProjectState();
    }
    private Optional<NarrativeLayerAssignment> findFirstAssignmentOfKind(ProjectSession session, ScriptTextRange selectedRange, NarrativeLayerKind kind) {
        return narrativeLayerWorkflow.findAssignmentOfKind(session, selectedRange, kind);
    }
    private void refreshStoryboardFromImageLayers(ProjectSession session) {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) return;
        StoryboardDocument refreshed = projectImageWorkflow.refreshStoryboardFromImageLayers(script, currentStoryboard.get(), session.project().assets(), session.project().narrativeLayerAssignments());
        session.setStoryboard(refreshed);
        currentStoryboard.set(refreshed);
    }
    public void removeEmotionAssignmentForSelectedDocumentRange() { removeAssignmentOfKindForSelectedDocumentRange(NarrativeLayerKind.EMOTION); }
    private void removeAssignmentOfKindForSelectedDocumentRange(NarrativeLayerKind kind) {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { statusMessage.set("No hay proyecto activo para quitar capas."); return; }
        Optional<ScriptTextRange> range = narrativeLayerWorkflow.scriptRangeForLayer(selectedDocumentSegmentOrSelected(), selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        if (range.isEmpty()) { statusMessage.set("Selecciona una oración o bloque con " + kind.displayName().toLowerCase(java.util.Locale.ROOT) + " antes de quitarla."); return; }
        statusMessage.set(narrativeLayerWorkflow.removeFirstAssignmentOfKind(session.get(), range.get(), kind).message());
        refreshProjectState();
    }
    public void selectNarrativeLayerAssignment(String assignmentId) {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty() || assignmentId == null || assignmentId.isBlank()) {
            return;
        }
        narrativeLayerWorkflow.findAssignment(session.get(), assignmentId)
                .ifPresent(assignment -> {
                    selectedScriptSegmentId.set(assignment.textRange().segmentId());
                    if (assignment.documentRange() != null) {
                        selectedDocumentBlockId.set(assignment.documentRange().blockId());
                        selectedDocumentTextRange.set(assignment.documentRange());
                        applySelectionLabels(documentSelectionWorkflow.layerSelection(assignment.displayName(), assignment.documentRange(), currentDocument.get()));
                    }
                    statusMessage.set("Capa seleccionada: " + assignment.kind().displayName() + " → " + assignment.displayName());
                    refreshProjectState();
                });
    }
    public java.util.List<com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentLayerAssignmentPresentation> documentLayerAssignmentPresentations() {
        return sessions.activeSession()
                .map(session -> narrativeLayerWorkflow.presentations(session))
                .orElseGet(java.util.List::of);
    }
    public void runDocumentPrimaryAction() {
        Optional<NarrationSegment> linked = selectedDocumentSegmentOrSelected();
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        if (linked.isPresent() && manifest != null && !manifest.emptyManifest()
                && manifest.cueForSegment(linked.get().id()).isEmpty()) {
            manifest = rebuildPlaybackManifestFromLatestJob();
        }
        if (linked.isPresent() && manifest != null && !manifest.emptyManifest()
                && manifest.cueForSegment(linked.get().id()).isPresent()) {
            playFromSegment(linked.get().id());
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set("Reproduciendo desde aquí: " + linked.get().id() + ".");
            refreshProjectState();
            return;
        }
        if (linked.isPresent()) {
            selectedScriptSegmentId.set(linked.get().id());
            playFromSelectedSegment();
            return;
        }
        listenToDocument();
    }
    public void listenToDocument() {
        ReadableDocument document = currentDocument.get();
        DocumentListenPlan plan = documentNarration.listeningPlan(
                document,
                currentScript.get(),
                currentPlaybackManifest.get(),
                audioJobRunning.get(),
                currentProjectFile().isPresent(),
                activeAudioJobStatus.get(),
                playbackBufferPolicy);
        if (plan.phase() == DocumentListenPhase.NO_DOCUMENT) {
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set(plan.userMessage());
            return;
        }
        if (plan.phase() == DocumentListenPhase.NO_NARRATABLE_TEXT) {
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set(plan.userMessage() + " Revisa el perfil de lectura o reclasifica bloques antes de escuchar.");
            return;
        }
        if (plan.requiresNarrationProjection()) {
            buildNarrationScriptFromDocument();
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            sessions.activeSession().ifPresent(session ->
                    session.replaceProject(session.project().withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name()), true));
            if (currentScript.get() == null || currentScript.get().empty()) {
                return;
            }
            if (!selectedDocumentBlockId.get().isBlank()) {
                firstSegmentForDocumentBlock(selectedDocumentBlockId.get())
                        .ifPresent(segment -> selectedScriptSegmentId.set(segment.id()));
            }
        }
        PlaybackManifest manifest = ensurePlaybackManifestLoaded();
        if (manifest != null && !manifest.emptyManifest()) {
            Optional<NarrationSegment> segment = selectedDocumentSegmentOrSelected();
            if (segment.isEmpty()) {
                statusMessage.set("No hay segmento narrable para reproducir.");
                return;
            }
            playFromSegment(segment.get().id());
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set("Reproduciendo documento desde " + segment.get().id() + ". La vista Documento sigue siendo la pantalla de lectura.");
            refreshProjectState();
            return;
        }
        if (audioJobRunning.get()) {
            documentPlaybackRequested = true;
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            PlaybackManifest buffered = rebuildPlaybackManifestFromLatestJob();
            if (tryStartBufferedPlayback(activeAudioJobStatus.get(), buffered)) {
                return;
            }
            statusMessage.set(documentNarration.listeningPlan(document, currentScript.get(), buffered, true, currentProjectFile().isPresent(), activeAudioJobStatus.get(), playbackBufferPolicy).userMessage());
            refreshProjectState();
            return;
        }
        if (currentProjectFile().isEmpty()) {
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set(DocumentListenPlan.saveProjectRequired().userMessage());
            refreshProjectState();
            return;
        }
        documentPlaybackRequested = true;
        waitingForBufferedSegmentAfter = "";
        Optional<NarrationSegment> selectedStart = selectedDocumentBlockId.get().isBlank() ? Optional.empty() : firstSegmentForDocumentBlock(selectedDocumentBlockId.get());
        if (selectedStart.isPresent()) { submitAudioGenerationFromSegment(selectedStart.get(), true); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        submitAudioGeneration();
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        if (audioJobRunning.get()) {
            statusMessage.set(DocumentListenPlan.generateAudio().userMessage() + " " + playbackBufferStatusLabel());
        }
        refreshProjectState();
    }
    private void applyImportedOrGeneratedScript(ProjectSession session, NarrationScriptDocument script) {
        ProjectMetadata metadata = session.project().metadata()
                .withTitle(script.title())
                .withKind(ProjectKind.NARRATION_SCRIPT)
                .withStatus(ProjectStatus.SCRIPT_READY);
        DocuPodcastProject updated = session.project()
                .withMetadata(metadata)
                .withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name());
        session.replaceProject(updated, true);
        session.setNarrationScript(script);
        session.clearStoryboard();
        currentScript.set(script);
        currentStoryboard.set(null);
        lastStoryboardImageAssetId.set("");
        selectedScriptSegmentId.set(script.segments().isEmpty() ? "" : script.segments().get(0).id());
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
    }
    public void showDocumentWorkspace(String message) {
        showWorkspace(WorkspaceKind.DOCUMENT_READER, message, "Documento activo.");
    }
    public void showTheatreScriptWorkspace(String message) {
        showWorkspace(WorkspaceKind.THEATRE_SCRIPT, message, "Guión teatral activo.");
    }
    public void showTheatreImageGenerationWorkspace() { showWorkspace(WorkspaceKind.THEATRE_IMAGE_GENERATION, "Gestion de frames teatrales activa.", "Gestion de frames teatrales activa."); }
    private void showWorkspace(WorkspaceKind workspace, String message, String fallback) {
        navigateToWorkspace(workspace);
        statusMessage.set(message == null || message.isBlank() ? fallback : message.strip());
        refreshProjectState();
    }
    public void selectScriptSegment(String segmentId) {
        Optional<NarrationSegment> segment = findSegment(segmentId);
        if (segment.isEmpty()) {
            selectedScriptSegmentId.set("");
            statusMessage.set("Selecciona un fragmento válido del documento para preparar voz, grabación o reproducción.");
            refreshProjectState();
            return;
        }
        selectedScriptSegmentId.set(segment.get().id());
        ensurePlaybackManifestLoaded();
        playbackCursor.set(applicationServices.playback().seekPlayback().seek(playbackCursor.get(), currentPlaybackManifest.get(), segment.get().id()));
        statusMessage.set("Segmento seleccionado: " + segment.get().id() + ". Playback preparado desde esta línea/segmento.");
        refreshProjectState();
    }
    public void playFromSelectedSegment() {
        Optional<NarrationSegment> segment = selectedDocumentSegmentOrSelected();
        if (segment.isEmpty() && currentDocument.get() != null) { buildNarrationScriptFromDocument(); segment = selectedDocumentSegmentOrSelected(); }
        if (segment.isEmpty()) { statusMessage.set("Prepara la lectura del documento antes de iniciar reproducción."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        playFromSegment(segment.get().id());
    }
    public void playFromSegment(String segmentId) { startPlaybackFromSegment(segmentId, false); }
    private boolean startPlaybackFromSegment(String segmentId, boolean bufferedStart) {
        Optional<NarrationSegment> segment = findSegment(segmentId);
        if (segment.isEmpty()) {
            statusMessage.set("Selecciona un fragmento válido antes de iniciar la reproducción.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return false;
        }
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        if (manifest.emptyManifest()) {
            if (audioJobRunning.get()) {
                statusMessage.set("Repriorizando audio desde " + segment.get().id() + ".");
            }
            return submitAudioGenerationFromSegment(segment.get(), true);
        }
        Optional<PlaybackCue> requestedCue = playbackSelectionResolver.cueForSelection(
                manifest, segment.get(), selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        if (requestedCue.isEmpty()) {
            manifest = rebuildPlaybackManifestFromLatestJob();
            requestedCue = playbackSelectionResolver.cueForSelection(
                    manifest, segment.get(), selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        }
        if (requestedCue.isEmpty()) {
            if (audioJobRunning.get()) {
                statusMessage.set("Repriorizando audio desde " + segment.get().id() + ".");
            }
            return submitAudioGenerationFromSegment(segment.get(), true);
        }
        pendingPlaybackStartSegmentId = "";
        selectedScriptSegmentId.set(segment.get().id());
        currentPlaybackManifest.set(manifest);
        playbackTransport.startRuntimeQueue(manifest, requestedCue.get());
        startSequentialPlayback(manifest, requestedCue.get(), bufferedStart);
        return true;
    }
    public void playSelectedFragmentOnly() {
        Optional<NarrationSegment> segment = selectedDocumentSegmentOrSelected();
        if (segment.isEmpty()) {
            statusMessage.set("Selecciona una oración o bloque antes de reproducir solo el fragmento.");
            return;
        }
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        PlaybackManifest fragmentManifest = manifest.onlySegment(segment.get().id());
        if (fragmentManifest.emptyManifest()) {
            manifest = rebuildPlaybackManifestFromLatestJob();
            fragmentManifest = manifest.onlySegment(segment.get().id());
        }
        if (fragmentManifest.emptyManifest()) {
            singleCuePlaybackRequested = true;
            submitAudioGenerationFromSegment(segment.get(), true);
            return;
        }
        PlaybackCue selectedCue = fragmentManifest.firstCue().orElseThrow();
        selectedScriptSegmentId.set(segment.get().id());
        currentPlaybackManifest.set(fragmentManifest);
        documentPlaybackRequested = false;
        waitingForBufferedSegmentAfter = "";
        playbackTransport.startRuntimeQueue(fragmentManifest, selectedCue);
        startSequentialPlayback(fragmentManifest, selectedCue, false);
        playSingleCueOnly = true;
        statusMessage.set("Reproduciendo solo este fragmento: " + segment.get().id() + ".");
        refreshProjectState();
    }
    public void pausePlayback() {
        recordPlaybackEvent("pause-requested", null, "user-pause");
        playbackTransport.pause(); theatreAudioPlayback.pause();
        playbackTimer.stop();
        playbackCursor.set(playbackCursor.get().pause());
        statusMessage.set("Playback pausado; pausa segura solicitada solo al reproductor. La generación de audio sigue separada en la barra de estado."); // cancelación segura solicitada
        refreshProjectState();
    }
    public void resumePlayback() {
        PlaybackCursor current = playbackCursor.get();
        PlaybackManifest manifest = ensurePlaybackManifestLoaded();
        if (manifest.emptyManifest()) {
            if (currentProjectFile().isPresent()) {
                resumeMostRecentRecoverableAudioJob();
            } else {
                statusMessage.set("No hay manifest de playback para reanudar. Guarda el proyecto y prepara audio primero.");
            }
            return;
        }
        if (current.segmentId().isBlank()) {
            playFromSelectedSegment();
            return;
        }
        PlaybackCursor resumed = current.resume();
        playbackCursor.set(resumed);
        Optional<PlaybackCue> activeCue = cueForCursor(manifest, resumed);
        double local = activeCue.map(cue -> cue.relativePosition(resumed.positionSeconds())).orElse(0.0);
        activeCue.ifPresent(cue -> recordPlaybackEvent("resume-requested", cue, "user-resume"));
        if (playbackTransport.sequentialActive()) playbackTransport.resume(local);
        else if (activeCue.isPresent() && playCueForCursor(resumed)) {
            playbackTransport.stopCueMonitoring();
            playbackTransport.startRuntimeQueue(manifest, activeCue.get());
            playbackTransport.attachSequentialPlayback(manifest, activeCue.get(), playbackRate.get(), local,
                    this::startCueFromSequentialQueue, this::completeSequentialPlayback);
            documentPlaybackRequested = true;
        } else { playbackTimer.stop(); refreshProjectState(); return; }
        syncTheatreAudioPlayback(resumed.positionSeconds()); theatreAudioPlayback.resume();
        playbackTimer.play();
        statusMessage.set("Playback reanudado desde " + current.segmentId() + ".");
        refreshProjectState();
    }
    public void setPlaybackRate(double rate) {
        double normalized = rate >= 1.74 ? 1.75 : rate >= 1.49 ? 1.5 : 1.0;
        PlaybackCursor current = playbackCursor.get();
        PlaybackManifest manifest = currentPlaybackManifest.get();
        Optional<PlaybackCue> activeCue = cueForCursor(manifest, current);
        playbackRate.set(normalized); theatreAudioPlayback.setPlaybackRate(normalized);
        if (current != null && current.playing() && activeCue.isPresent()) {
            restartActiveCueImmediatelyAtRate(activeCue.get(), normalized);
        } else {
            playbackTransport.setPlaybackRate(normalized);
            statusMessage.set("Velocidad de lectura preparada: " + rateLabel(normalized) + ".");
        }
        refreshProjectState();
    }
    private void restartActiveCueImmediatelyAtRate(PlaybackCue cue, double normalizedRate) {
        if (cue == null) { return; }
        playbackTransport.restartActiveCueAtRate(normalizedRate);
        playbackCursor.set(new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false));
        selectedScriptSegmentId.set(cue.segmentId()); playbackTransport.activateRuntimeCue(cue);
        boolean restarted = playExactCue(cue, 0.0);
        if (restarted) { playbackTransport.setSequentialPlaybackRate(normalizedRate, 0.0); playbackTimer.play();
            statusMessage.set("Velocidad " + rateLabel(normalizedRate) + ": reiniciando inmediatamente el fragmento " + cue.unitId() + "."); }
        else { statusMessage.set("No se pudo reiniciar el fragmento al cambiar a " + rateLabel(normalizedRate) + ". Revisa el WAV o regenera los fragmentos de audio."); }
    }
    private static String rateLabel(double rate) { return rate >= 1.74 ? "1.75x" : rate >= 1.49 ? "1.5x" : "1x"; }
    public void stopPlayback() {
        recordPlaybackEvent("stop-requested", null, "user-stop");
        documentPlaybackRequested = false;
        playSingleCueOnly = false;
        waitingForBufferedSegmentAfter = "";
        pendingPlaybackStartSegmentId = "";
        lastSequentialCueUnitId = "";
        resetPlaybackTransportOnly();
        playbackTransport.stopSequentialPlayback();
        statusMessage.set("Playback detenido. La generación de audio se controla desde la barra de estado.");
        refreshProjectState();
    }
    public void playDocumentFromBeginning() {
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        Optional<PlaybackCue> first = manifest == null || manifest.emptyManifest() ? Optional.empty() : manifest.firstCue();
        if (first.isEmpty()) { statusMessage.set("Primero genera audio para poder reproducir desde el inicio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        if (startPlaybackFromCue(first.get(), false)) { statusMessage.set("Reproduciendo desde el inicio del documento."); }
    }
    public void playNextFragment() { playRelativeFragment(1); }
    public void playPreviousFragment() { playRelativeFragment(-1); }
    private void playRelativeFragment(int direction) {
        var result = playbackNavigator.target(ensurePlaybackManifestLoaded(), playbackCursor.get(), selectedDocumentSegmentOrSelected(), direction);
        if (!result.ready()) { statusMessage.set(result.message()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        if (startPlaybackFromCue(result.cue(), false)) { statusMessage.set(result.message()); }
    }
    private boolean startPlaybackFromCue(PlaybackCue cue, boolean bufferedStart) {
        if (cue == null) { statusMessage.set("No hay fragmento de audio listo para reproducir."); refreshProjectState(); return false; }
        if (findSegment(cue.segmentId()).isEmpty()) { statusMessage.set("No se encontró el texto del fragmento " + cue.segmentId() + "."); refreshProjectState(); return false; }
        playbackTransport.stopTransport();
        selectedScriptSegmentId.set(cue.segmentId()); documentPlaybackRequested = true; playSingleCueOnly = false; waitingForBufferedSegmentAfter = ""; pendingPlaybackStartSegmentId = ""; lastSequentialCueUnitId = "";
        PlaybackManifest manifest = ensurePlaybackManifestLoaded();
        currentPlaybackManifest.set(manifest);
        playbackTransport.startRuntimeQueue(manifest, cue);
        startSequentialPlayback(manifest, cue, bufferedStart);
        if (singleCuePlaybackRequested) {
            playSingleCueOnly = true;
            singleCuePlaybackRequested = false;
        }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return true;
    }
    public void prepareAiVoiceForSelectedText() {
        Optional<ScriptTextRange> range = selectedRangeForWholeSegment();
        if (range.isEmpty()) { statusMessage.set("Selecciona un fragmento u oración del documento antes de asignar voz IA/TTS."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        statusMessage.set("Preparado para asignar voz IA/TTS al rango " + label(range.get()) + ". La elección real de voz llega con Voice Library."); refreshProjectState();
    }
    public void prepareHumanVoiceForSelectedText() {
        Optional<ScriptTextRange> range = selectedRangeForWholeSegment();
        if (range.isEmpty()) { statusMessage.set("Selecciona un fragmento u oración del documento antes de asociar voz humana."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        RecordingActionPlan plan = applicationServices.recording().prepareRecordingAction().prepare(RecordingPurpose.HUMAN_VOICE_FOR_TEXT, range.get());
        statusMessage.set(plan.userMessage() + " Archivo sugerido: " + plan.suggestedFileName() + "."); refreshProjectState();
    }
    public java.util.List<AudioInputDevice> audioInputDevices() { return manualInterventionAudioWorkflow.inputDevices(applicationServices); }
    public void startManualInterventionRecording(String blockId, String displayName, String preview, String inputDeviceId) throws IOException { if (voiceRecordingRunning.get() || manualAudioRecordingRunning.get() || applicationServices.recording().stopAudioRecording().recording()) { statusMessage.set("Ya hay una grabacion activa. Detenla o cancelala antes de iniciar otra."); return; } NarrationSegment segment = manualRecordingSegment(blockId).orElseThrow(() -> new IOException("No se encontro un segmento narrable para esa intervencion.")); Path output = manualInterventionAudioWorkflow.startRecording(applicationServices, requireSession(), segment, inputDeviceId); activeManualAudioSegmentId = segment.id(); manualAudioRecordingRunning.set(true); selectedScriptSegmentId.set(segment.id()); if (blockId != null && !blockId.isBlank()) selectedDocumentBlockId.set(blockId.strip()); statusMessage.set("Grabando audio manual para " + ManualInterventionAudioWorkflow.displayName(displayName, segment) + ": " + output.getFileName() + "."); refreshProjectState(); }
    public Path stopManualInterventionRecordingDraft(String blockId, String displayName) throws IOException { if (!manualAudioRecordingRunning.get() && !applicationServices.recording().stopAudioRecording().recording()) { statusMessage.set("No hay una grabacion manual activa."); throw new IOException("No hay una grabacion manual activa."); } NarrationSegment segment = manualRecordingSegment(blockId).or(() -> findSegment(activeManualAudioSegmentId)).orElseThrow(() -> new IOException("No se encontro el segmento de la grabacion manual.")); Path recorded; try { recorded = applicationServices.recording().stopAudioRecording().stop(); } finally { manualAudioRecordingRunning.set(false); activeManualAudioSegmentId = ""; } statusMessage.set("Borrador grabado para " + ManualInterventionAudioWorkflow.displayName(displayName, segment) + ": " + recorded.getFileName() + "."); refreshProjectState(); return recorded; }
    public void assignManualInterventionRecording(String blockId, String displayName, Path recorded) throws IOException { if (recorded == null || !java.nio.file.Files.isRegularFile(recorded)) { throw new IOException("No hay borrador WAV valido para asignar."); } NarrationSegment segment = manualRecordingSegment(blockId).or(() -> findSegment(activeManualAudioSegmentId)).orElseThrow(() -> new IOException("No se encontro el segmento para asignar audio manual.")); applyManualAudioSnapshot(manualInterventionAudioWorkflow.applyRecording(applicationServices, audioWorkflow, playableAudioJobSelector, requireSession(), currentScript.get(), activeAudioJobStatus.get().jobId(), segment, displayName, recorded), "Audio manual aplicado a " + segment.id() + ". Playback y exportacion usaran " + segment.id() + "-manual.wav."); }
    public void stopManualInterventionRecording(String blockId, String displayName, String preview) throws IOException { Path recorded = stopManualInterventionRecordingDraft(blockId, displayName); assignManualInterventionRecording(blockId, displayName, recorded); }
    public void cancelManualInterventionRecording() throws IOException { if (!manualAudioRecordingRunning.get() && !applicationServices.recording().stopAudioRecording().recording()) { statusMessage.set("No hay grabacion manual para cancelar."); return; } applicationServices.recording().cancelAudioRecording().cancel(); manualAudioRecordingRunning.set(false); activeManualAudioSegmentId = ""; statusMessage.set("Grabacion manual cancelada. No se reemplazo ningun fragmento de audio."); refreshProjectState(); }
    public void playManualInterventionAudio(String blockId) throws IOException { NarrationSegment segment = manualRecordingSegment(blockId).orElseThrow(() -> new IOException("No se encontro un segmento narrable para esa intervencion.")); Path projectDirectory = currentProjectDirectory().orElseThrow(() -> new IOException("Guarda el proyecto antes de escuchar audio manual.")); Path audio = manualInterventionAudioWorkflow.playableAudioForSegment(audioWorkflow, playableAudioJobSelector, projectDirectory, currentScript.get(), activeAudioJobStatus.get().jobId(), segment.id()); playbackTransport.playStandalone(audio, 0.0); statusMessage.set("Reproduciendo audio de " + segment.id() + ": " + audio.getFileName() + "."); }
    public void playStandaloneAudio(Path audio) throws IOException { if (audio == null || !java.nio.file.Files.isRegularFile(audio)) { throw new IOException("No existe el audio para reproducir."); } playbackTransport.playStandalone(audio, 0.0); statusMessage.set("Reproduciendo " + audio.getFileName() + "."); }
    public void deleteManualInterventionAudio(String blockId, String displayName) throws IOException { NarrationSegment segment = manualRecordingSegment(blockId).orElseThrow(() -> new IOException("No se encontro un segmento narrable para esa intervencion.")); applyManualAudioSnapshot(manualInterventionAudioWorkflow.deleteManualAudio(applicationServices, audioWorkflow, playableAudioJobSelector, requireSession(), currentScript.get(), activeAudioJobStatus.get().jobId(), segment, displayName), "Audio manual eliminado de " + segment.id() + ". Se restauro el WAV generado si existia; si no, quedo pendiente."); }
    public boolean canRegenerateSelectedTheatreInterventionAudio() { return currentProjectMode.get() == ProjectMode.THEATRE_PRODUCTION && !audioJobRunning.get() && !choralVoiceRenderingProperty().get() && !selectedDocumentBlockId.get().isBlank() && currentDocument.get() != null; }
    public void regenerateSelectedTheatreInterventionAudio() {
        try { if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument(); NarrationSegment segment = manualRecordingSegment(selectedDocumentBlockId.get()).orElseThrow(() -> new IOException("Selecciona una intervencion narrable antes de regenerar su audio.")); ProjectSession session = requireSession(); Path file = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de regenerar audio.")); if (audioEngineUnavailableForGeneration()) throw new IOException(audioEngineUnavailableMessage()); saveCurrentProjectAs(file); audioJobRunning.set(true); statusMessage.set("Renderizando de nuevo el audio de " + segment.id() + "...");
            theatreInterventionAudioRegenerationWorkflow.start(applicationServices, audioWorkflow, playableAudioJobSelector, session, currentScript.get(), activeAudioJobStatus.get().jobId(), segment,
                    status -> Platform.runLater(() -> { activeAudioJobStatus.set(status); audioJobRunning.set(status.running()); statusMessage.set(status.running() ? "Renderizando de nuevo " + segment.id() + ": " + status.statusLine() : "Validando audio nuevo de " + segment.id() + "..."); refreshProjectState(); }),
                    updated -> Platform.runLater(() -> applyManualAudioSnapshot(updated, "Audio actualizado para " + segment.id() + ".")),
                    failure -> Platform.runLater(() -> { audioJobRunning.set(false); statusMessage.set("No se pudo actualizar " + segment.id() + ": " + rootCauseMessage(failure)); refreshProjectState(); }));
        } catch (Exception ex) { audioJobRunning.set(false); statusMessage.set("No se pudo regenerar el fragmento: " + rootCauseMessage(ex)); refreshProjectState(); }
    }
    private Optional<NarrationSegment> selectedDocumentSegmentOrSelected() { Optional<NarrationSegment> linked = firstSegmentForDocumentBlock(selectedDocumentBlockId.get()); return linked.isPresent() ? linked : selectedSegmentOrFirst(); }
    private Optional<NarrationSegment> manualRecordingSegment(String blockId) { if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument(); return manualInterventionAudioWorkflow.segmentForBlock(currentScript.get(), blockId, selectedDocumentSegmentOrSelected()); }
    private Optional<NarrationSegment> narrationSegmentForIntervention(String interventionId) { if (interventionId == null || interventionId.isBlank() || currentScript.get() == null) return Optional.empty(); Optional<String> blockId = sessions.activeSession().flatMap(session -> session.project().theatre().intervenciones().stream().filter(item -> item.id().equals(interventionId)).map(TheatreProjectLayer.Intervencion::blockId).findFirst()); return blockId.flatMap(this::firstSegmentForDocumentBlock); }
    private void applyManualAudioSnapshot(AudioJobSnapshot updated, String message) { Path projectDirectory = currentProjectDirectory().orElse(Path.of("")); activeAudioJobStatus.set(AudioJobSnapshotMapper.toStatusDto(updated, projectDirectory)); audioJobRunning.set(false); rebuildPlaybackManifestFromLatestJob(); bumpDocumentMediaRevision(); statusMessage.set(message); refreshProjectState(); }
    private Optional<NarrationSegment> selectedTheatreVisualSegment() { return findSegment(selectedVisualFragmentSegmentId.get()).or(() -> findSegment(selectedScriptSegmentId.get())).or(() -> firstSegmentForDocumentBlock(selectedDocumentBlockId.get())); }
    private Optional<String> selectedTheatreInterventionId(ProjectSession session) { if (session == null) return Optional.empty(); TheatreProjectLayer theatre = session.project().theatre(); Optional<String> fromSegment = selectedTheatreVisualSegment().flatMap(segment -> interventionIdForSegment(theatre, segment)); if (fromSegment.isPresent()) return fromSegment; String blockId = selectedDocumentBlockId.get(); return blockId == null || blockId.isBlank() ? Optional.empty() : theatre.intervenciones().stream().filter(intervention -> intervention.blockId().equals(blockId)).map(TheatreProjectLayer.Intervencion::id).findFirst(); }
    private Optional<NarrationSegment> firstSegmentForDocumentBlock(String blockId) {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty() || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        return script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().stream().anyMatch(blockId::equals)).findFirst();
}
    private Optional<String> firstSourceBlockId(NarrationSegment segment) { if (segment == null) return Optional.empty(); return segment.sourceBlockIds().stream().filter(id -> id != null && !id.isBlank()).findFirst(); }
    private Optional<String> interventionIdForSegment(TheatreProjectLayer theatre, NarrationSegment segment) { if (theatre == null || segment == null) return Optional.empty(); return segment.sourceBlockIds().stream().flatMap(blockId -> theatre.intervenciones().stream().filter(intervention -> intervention.blockId().equals(blockId)).map(TheatreProjectLayer.Intervencion::id)).findFirst(); }
    private static Optional<String> assetUri(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty() || asset == null) {
            return Optional.empty();
        }
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.relativePath()).normalize();
        if (!resolved.startsWith(root)) {
            return Optional.empty();
        }
        return Optional.of(resolved.toUri().toString());
    }
    private Optional<PlaybackCue> preferredPlaybackStartCue(PlaybackManifest manifest) {
        if (manifest != null && !pendingPlaybackStartSegmentId.isBlank()) {
            Optional<NarrationSegment> pending = findSegment(pendingPlaybackStartSegmentId);
            Optional<PlaybackCue> cue = playbackWorkflow.preferredStartCue(manifest, pending);
            if (cue.isPresent()) {
                return cue;
            }
        }
        return playbackWorkflow.preferredStartCue(manifest, selectedDocumentSegmentOrSelected());
    }
    private Optional<NarrationSegment> selectedSegmentOrFirst() {
        Optional<NarrationSegment> selected = findSegment(selectedScriptSegmentId.get());
        if (selected.isPresent()) {
            return selected;
        }
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.segments().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(script.segments().get(0));
    }
    private Optional<ScriptTextRange> selectedRangeForWholeSegment() { return selectedSegmentOrFirst().map(segment -> new ScriptTextRange(segment.id(), 0, segment.narrationText().length())); }
    private Optional<NarrationSegment> findSegment(String segmentId) {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || segmentId == null || segmentId.isBlank()) {
            return Optional.empty();
        }
        return script.segmentById(segmentId);
    }
    private static String label(ScriptTextRange range) { return range.segmentId() + "[" + range.startOffset() + ".." + range.endOffset() + "]"; }
    public VoiceProfile saveAdvancedVoiceProfile(String voiceId, String displayName) throws IOException {
        ProjectSession session = requireSession();
        var result = voiceProfileAdministration.save(session, activeVoiceLibrary.get(), voiceId, displayName);
        session.replaceProject(result.project(), true); activeVoiceLibrary.set(result.library());
        activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set("Voz guardada: " + result.voice().displayName() + ". Neutral es necesaria para usarla en Documento.");
        refreshProjectState(); return result.voice();
    }
    public void deleteVoiceProfile(VoiceProfile voice) throws IOException {
        ProjectSession session = requireSession();
        var result = voiceProfileAdministration.delete(session, activeVoiceLibrary.get(), voice, voiceSampleWorkflow);
        session.replaceProject(result.project(), true); activeVoiceLibrary.set(result.library());
        activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set("Voz eliminada: " + voice.displayName() + ". También se retiraron " + result.removedSamples() + " muestra(s) y emociones asociadas del proyecto.");
        refreshProjectState();
    }
    public String deleteVoiceProfileImpactLabel(VoiceProfile voice) { return voiceProfileAdministration.impactLabel(activeVoiceLibrary.get(), voice); }
    public VoiceRegistrationWizardPlan voiceRegistrationWizardPlan(VoiceProfile voice) { return voiceSampleWorkflow.registrationPlan(activeVoiceLibrary.get(), voice); }
    public VoiceToneRecordingPlan voiceToneRecordingPlan(VoiceProfile voice, VoiceReferenceTone tone) { return voiceSampleWorkflow.toneRecordingPlan(activeVoiceLibrary.get(), voice, tone); }
    public void importOwnVoiceSample(Path sourceAudioFile) throws IOException { importOwnVoiceSample(sourceAudioFile, VoiceReferenceTone.NEUTRAL); }
    public void importOwnVoiceSample(Path sourceAudioFile, VoiceReferenceTone tone) throws IOException { importVoiceSample(activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null), sourceAudioFile, tone == null ? VoiceReferenceTone.NEUTRAL : tone); }
    public void importVoiceSample(VoiceProfile voice, Path sourceAudioFile, VoiceReferenceTone tone) throws IOException {
        VoiceReferenceTone targetTone = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        VoiceProfile targetVoice = voice == null ? activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null) : voice;
        importOwnVoiceSample(sourceAudioFile, (targetVoice == null ? "Voz avanzada" : targetVoice.displayName()) + " — muestra " + targetTone.displayName().toLowerCase(java.util.Locale.ROOT), voiceToneRecordingPlan(targetVoice, targetTone));
    }
    private void importOwnVoiceSample(Path sourceAudioFile, String displayName, VoiceToneRecordingPlan recordingPlan) throws IOException { importVoiceSampleInternal(sourceAudioFile, displayName, recordingPlan, false); }
    private Path voiceLibraryProjectFileOrNull() throws IOException {
        Optional<ProjectSession> session = sessions.activeSession();
        return session.isPresent() && session.get().projectFile().isPresent() ? session.get().projectFile().get()
                : applicationServices.voice().importVoiceSample().storesSamplesOutsideProject() ? applicationServices.voice().importVoiceSample().effectiveProjectFile(null) : null;
    }
    private void importVoiceSampleInternal(Path sourceAudioFile, String displayName, VoiceToneRecordingPlan recordingPlan, boolean recordedInJava) throws IOException {
        Optional<ProjectSession> maybeSession = sessions.activeSession();
        DocuPodcastProject baseProject = maybeSession.map(ProjectSession::project).orElseGet(() -> DocuPodcastProject.empty("Biblioteca de voces").withVoiceLibrary(activeVoiceLibrary.get()));
        VoiceReferenceTone targetTone = recordingPlan == null ? VoiceReferenceTone.NEUTRAL : recordingPlan.tone();
        VoiceSampleImportResult result = recordedInJava
                ? voiceSampleWorkflow.importRecordedToneSample(baseProject, activeVoiceLibrary.get(), voiceLibraryProjectFileOrNull(), sourceAudioFile, displayName, recordingPlan)
                : voiceSampleWorkflow.importToneSample(baseProject, activeVoiceLibrary.get(), voiceLibraryProjectFileOrNull(), sourceAudioFile, displayName, recordingPlan);
        maybeSession.ifPresent(session -> session.replaceProject(result.project(), true));
        activeVoiceLibrary.set(result.voiceLibrary()); activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set("Muestra de voz registrada para " + result.updatedVoiceProfile().displayName() + " · tono " + targetTone.displayName() + ": " + result.referenceSample().fileUri() + ". Documento ya puede usar los tonos registrados de esa voz."); refreshProjectState();
    }
    public void startOwnVoiceRecording() throws IOException { startOwnVoiceRecording(VoiceReferenceTone.NEUTRAL); }
    public void startOwnVoiceRecording(VoiceReferenceTone tone) throws IOException { startVoiceRecording(activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null), tone); }
    public void startVoiceRecording(VoiceProfile voice, VoiceReferenceTone tone) throws IOException { startVoiceRecording(voice, tone, ""); }
    public void startVoiceRecording(VoiceProfile voice, VoiceReferenceTone tone, String inputDeviceId) throws IOException {
        if (voiceRecordingRunning.get() || manualAudioRecordingRunning.get() || applicationServices.recording().stopAudioRecording().recording()) { statusMessage.set("Ya hay una grabacion activa. Detenla antes de iniciar otra."); return; }
        VoiceReferenceTone targetTone = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        VoiceProfile targetVoice = voice == null ? activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null) : voice;
        VoiceToneRecordingPlan tonePlan = voiceToneRecordingPlan(targetVoice, targetTone);
        Path output = voiceSampleWorkflow.startRecording(voiceLibraryProjectFileOrNull(), tonePlan, inputDeviceId);
        activeVoiceRecordingFile = output; activeVoiceRecordingVoiceId = targetVoice == null ? "" : targetVoice.id(); activeVoiceRecordingTone = targetTone; voiceRecordingRunning.set(true); activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set("Grabando muestra para " + (targetVoice == null ? "la voz seleccionada" : targetVoice.displayName()) + " · emoción " + targetTone.displayName() + ": " + output.getFileName() + ". Lee la frase guía y pulsa Detener y guardar."); refreshProjectState();
    }
    public void stopOwnVoiceRecording() throws IOException {
        if (!voiceRecordingRunning.get() && !applicationServices.recording().stopAudioRecording().recording()) { statusMessage.set("No hay una grabación de voz activa."); return; }
        VoiceReferenceTone completedTone = activeVoiceRecordingTone == null ? VoiceReferenceTone.NEUTRAL : activeVoiceRecordingTone;
        Path recorded;
        try { recorded = applicationServices.recording().stopAudioRecording().stop(); } finally { voiceRecordingRunning.set(false); activeVoiceRecordingFile = null; activeVoiceRecordingTone = VoiceReferenceTone.NEUTRAL; }
        VoiceProfile recordedVoice = activeVoiceLibrary.get().voiceById(activeVoiceRecordingVoiceId).orElseGet(() -> activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null)); activeVoiceRecordingVoiceId = "";
        importVoiceSampleInternal(recorded, (recordedVoice == null ? "Voz avanzada" : recordedVoice.displayName()) + " — muestra " + completedTone.displayName().toLowerCase(java.util.Locale.ROOT), voiceToneRecordingPlan(recordedVoice, completedTone), true);
        statusMessage.set("Grabación detenida y registrada como muestra de voz · tono " + completedTone.displayName() + ": " + recorded.getFileName() + "."); refreshProjectState();
    }
    public void cancelOwnVoiceRecording() throws IOException {
        if (!voiceRecordingRunning.get() && !applicationServices.recording().stopAudioRecording().recording()) { statusMessage.set("No hay una grabación de voz activa para cancelar."); return; }
        applicationServices.recording().cancelAudioRecording().cancel();
        voiceRecordingRunning.set(false); activeVoiceRecordingFile = null; activeVoiceRecordingVoiceId = ""; activeVoiceRecordingTone = VoiceReferenceTone.NEUTRAL; activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set("Grabación cancelada. La muestra anterior se conserva."); refreshProjectState();
    }
    public Callable<VoiceGeneratedTestResult> prepareVoiceTestGeneration(VoiceProfile voice, VoiceReferenceTone tone, String phrase) throws IOException {
        Path projectFile = requireSession().projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de generar una prueba de voz."));
        VoiceLibrary library = activeVoiceLibrary.get(); VoiceProfile target = voice == null ? library.voiceById("VOC-OWN-PLACEHOLDER").orElse(null) : voice; AudioEngineDescriptor engine = audioEngineDescriptor();
        return () -> voiceSampleWorkflow.generateTest(projectFile, library, target, tone, phrase, engine);
    }
    public void completeVoiceTestGeneration(VoiceGeneratedTestResult result) {
        lastGeneratedVoiceTestPath.set(result.audioFile().orElse(null));
        setGeneratedVoiceTestStatus(result.audioFile().map(path -> "Voz de prueba lista: " + path.getFileName() + ".").orElse("Prueba de voz terminada: " + result.userMessage()));
        activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        refreshProjectState();
    }
    public void markVoiceTestGenerationStarted() { setGeneratedVoiceTestStatus("Renderizando voz de prueba con el motor y dispositivo seleccionados."); }
    public void markVoiceTestGenerationFailed(String detail) { setGeneratedVoiceTestStatus("No se pudo generar la voz de prueba" + (detail == null || detail.isBlank() ? "" : ": " + detail) + "."); }
    public void resetGeneratedVoiceTestForSelection(String message) { lastGeneratedVoiceTestPath.set(null); setGeneratedVoiceTestStatus(message == null || message.isBlank() ? "Sin voz de prueba generada." : message); }
    private void setGeneratedVoiceTestStatus(String message) { generatedVoiceTestStatus.set(message); statusMessage.set(message); }
    public void playLastGeneratedVoiceTest() throws IOException {
        Path audio = lastGeneratedVoiceTestPath.get();
        if (audio == null || !Files.isRegularFile(audio)) {
            setGeneratedVoiceTestStatus("No hay una voz de prueba disponible para reproducir.");
            return;
        }
        playbackTransport.playStandalone(audio, 0.0);
        setGeneratedVoiceTestStatus("Reproduciendo voz de prueba: " + audio.getFileName() + ".");
    }
    public void playVoiceReferenceSample(VoiceProfile voice, VoiceReferenceTone tone) throws IOException {
        Path projectFile = voiceLibraryProjectFileOrNull();
        VoiceReferenceSample sample = voiceSampleWorkflow.requireExactSample(activeVoiceLibrary.get(), voice, tone);
        Path audio = voiceSampleWorkflow.resolveSamplePath(projectFile, sample);
        playbackTransport.playStandalone(audio, 0.0);
        statusMessage.set("Reproduciendo muestra " + sample.tone().displayName() + " de " + voiceSampleWorkflow.displayNameForVoice(voice) + ": " + audio.getFileName() + ".");
    }
    public void downloadVoiceReferenceSample(VoiceProfile voice, VoiceReferenceTone tone, Path targetDirectory) throws IOException {
        Optional<ProjectSession> maybeSession = sessions.activeSession();
        Path projectFile = voiceLibraryProjectFileOrNull();
        DocuPodcastProject baseProject = maybeSession.map(ProjectSession::project)
                .orElseGet(() -> DocuPodcastProject.empty("Biblioteca de voces").withVoiceLibrary(activeVoiceLibrary.get()));
        VoiceReferenceSample sample = voiceSampleWorkflow.requireExactSample(activeVoiceLibrary.get(), voice, tone);
        VoiceSampleDownloadResult result = voiceSampleWorkflow.downloadToneSample(baseProject, projectFile, sample, targetDirectory);
        statusMessage.set(result.message() + " Archivo: " + result.copiedFile().getFileName() + ".");
    }
    public void deleteVoiceReferenceSample(VoiceProfile voice, VoiceReferenceTone tone) throws IOException {
        Optional<ProjectSession> maybeSession = sessions.activeSession();
        Path projectFile = voiceLibraryProjectFileOrNull();
        DocuPodcastProject baseProject = maybeSession.map(ProjectSession::project)
                .orElseGet(() -> DocuPodcastProject.empty("Biblioteca de voces").withVoiceLibrary(activeVoiceLibrary.get()));
        VoiceReferenceSample sample = voiceSampleWorkflow.requireExactSample(activeVoiceLibrary.get(), voice, tone);
        VoiceSampleDeleteResult result = voiceSampleWorkflow.deleteToneSample(baseProject, projectFile, sample);
        VoiceLibrary updatedLibrary = voiceSampleWorkflow.removeReferenceSample(activeVoiceLibrary.get(), sample);
        maybeSession.ifPresent(session -> session.replaceProject(session.project()
                .withVoiceLibrary(updatedLibrary)
                .withoutAsset(sample.id()), true));
        activeVoiceLibrary.set(updatedLibrary);
        activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set(result.message() + " Referencia retirada de la biblioteca para tono " + sample.tone().displayName() + ".");
        refreshProjectState();
    }
    public void showVoiceLibraryWorkspace() {
        navigateToWorkspace(WorkspaceKind.VOICE_LIBRARY);
        VoiceLibrary library = activeVoiceLibrary.get();
        statusMessage.set("Biblioteca de voces activa: " + library.voices().size() + " voces, " + library.characters().size() + " personajes, " + library.styles().size() + " estilos.");
        refreshProjectState();
    }
    public void assignVoiceToSelectedSegment(String characterId, String voiceProfileId, String performanceStyleId) {
        NarrationScriptDocument script = currentScript.get();
        if (script == null) {
            statusMessage.set("Prepara la lectura del documento antes de asignar voces a fragmentos.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        Optional<NarrationSegment> selected = selectedSegmentOrFirst();
        if (selected.isEmpty()) {
            statusMessage.set("Selecciona un fragmento del documento antes de asignar una voz.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        try {
            NarrationScriptDocument updated = applicationServices.voice().assignVoiceToSegment()
                    .assign(script, activeVoiceLibrary.get(), selected.get().id(), characterId, voiceProfileId, performanceStyleId);
            currentScript.set(updated);
            selectedScriptSegmentId.set(selected.get().id());
            sessions.activeSession().ifPresent(session -> session.setNarrationScript(updated));
            statusMessage.set("Voz asignada a " + selected.get().id() + ": " + characterId + " / " + voiceProfileId + " / " + performanceStyleId + ".");
            refreshProjectState();
        } catch (RuntimeException ex) {
            statusMessage.set("No se pudo asignar voz: " + ex.getMessage());
        }
    }
    public VoiceLibraryCapabilityReport voiceCapabilityReport() {
        return applicationServices.voice().voiceCapabilityPolicy()
                .evaluate(activeVoiceLibrary.get(), audioEngineDescriptor());
    }
    public java.util.List<String> voiceCapabilityLabels() { return voiceCapabilityReport().summaryLines(); }
    public java.util.List<String> voiceLibraryLabels() {
        VoiceLibrary library = activeVoiceLibrary.get();
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("Voces: " + library.voices().size());
        for (VoiceProfile voice : library.voices()) {
            lines.add(voice.id() + " · " + voice.displayName() + " · " + voice.type().displayName() + " · " + voice.engineType().displayName() + " · " + voice.qualityPreset().displayName());
        }
        lines.add("Personajes: " + library.characters().size());
        for (CharacterProfile character : library.characters()) {
            lines.add(character.id() + " · " + character.displayName() + " · voz " + character.defaultVoiceProfileId() + " · estilo " + character.defaultPerformanceStyleId());
        }
        lines.add("Estilos: " + library.styles().size());
        for (PerformanceStyle style : library.styles()) {
            lines.add(style.id() + " · " + style.displayName() + (style.requiresEngineSupport() ? " · requiere soporte del motor" : " · básico"));
        }
        return java.util.List.copyOf(lines);
    }
    public java.util.List<String> voiceLibraryValidationLabels() {
        java.util.List<String> issues = applicationServices.voice().validateVoiceLibrary().validate(activeVoiceLibrary.get());
        if (issues.isEmpty()) {
            return java.util.List.of("Biblioteca válida para el MVP. Las voces humanas reales y clonación avanzada quedan sujetas a muestras/autorización.");
        }
        return issues;
    }
    public void buildStoryboardFromScript() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            statusMessage.set("Prepara la lectura del documento antes de crear la secuencia visual.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        ProjectSession session = requireSession();
        StoryboardDocument storyboard = applicationServices.storyboard().buildStoryboardFromScript().build(script);
        ProjectMetadata metadata = session.project().metadata()
                .withKind(ProjectKind.STORYBOARD)
                .withStatus(ProjectStatus.STORYBOARD_READY);
        DocuPodcastProject updated = session.project()
                .withMetadata(metadata)
                .withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name());
        session.replaceProject(updated, true);
        session.setStoryboard(storyboard);
        currentStoryboard.set(storyboard);
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        statusMessage.set("Secuencia visual creada: " + script.segmentCount() + " fragmentos listos para asociar imágenes.");
        refreshProjectState();
    }
    public void importStoryboardImage(Path imageFile) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de importar imágenes para mantener rutas relativas."));
        var result = applicationServices.storyboard().importImageAsset().importImage(session.project(), projectFile, imageFile);
        session.replaceProject(result.project(), true);
        lastStoryboardImageAssetId.set(result.imageAsset().id());
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        statusMessage.set("Imagen importada para el panel visual: " + result.imageAsset().relativePath()
                + ". Selecciona un fragmento y pulsa Asociar imagen.");
        refreshProjectState();
    }
    public ExampleVisualBindingWorkflow.Result importAndBindExampleVisuals(List<Path> visualAssets, List<ExampleVisualBindingDescriptor> bindings) throws IOException {
        if (bindings == null || bindings.isEmpty()) { int imported = 0; for (Path asset : visualAssets == null ? List.<Path>of() : visualAssets) { importStoryboardImage(asset); imported++; } return new ExampleVisualBindingWorkflow.Result(imported, 0, 0); }
        if (currentScript.get() == null || currentScript.get().empty()) { buildNarrationScriptFromDocument(); }
        ProjectSession session = requireSession(); Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de asociar visuales demo."));
        ExampleVisualBindingWorkflow.Result result = exampleVisualBindingWorkflow.bind(applicationServices, session, projectFile, currentDocument.get(), currentScript.get(), visualAssets, bindings);
        refreshStoryboardFromImageLayers(session); bumpDocumentMediaRevision(); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); statusMessage.set(result.message()); refreshProjectState(); return result;
    }
    public IntervencionBoundaryStore intervencionBoundaryStore() {
        return intervencionBoundaryStore;
    }
    public void configureAviadoresTheatreDemo(String exampleId, List<Path> visualAssets, Path theatreMarkdownFile) throws IOException {
        configureAviadoresTheatreDemo(exampleId, visualAssets, theatreMarkdownFile, null);
    }
    public void configureAviadoresTheatreDemo(String exampleId, List<Path> visualAssets, Path theatreMarkdownFile, Path targetProjectFile) throws IOException {
        if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument();
        ProjectSession session = requireSession();
        var result = theatreDemoManifestWorkflow.configureAviadores(exampleId, visualAssets, theatreMarkdownFile,
                targetProjectFile, session, currentDocument.get(), currentScript.get(), session.project().voiceLibrary());
        if (!result.configured()) return;
        activeVoiceLibrary.set(session.project().voiceLibrary());
        theatreBoundaryWorkflow.applySetupResult(result.setupResult(), currentDocument.get(), currentScript.get(), intervencionBoundaryStore);
        theatreBoundaryWorkflow.persist(session, intervencionBoundaryStore);
        refreshStoryboardFromImageLayers(session);
        bumpDocumentMediaRevision();
        refreshProjectState();
    }
    public void configureAviadoresTheatreDemo(String exampleId, List<Path> visualAssets) throws IOException {
        configureAviadoresTheatreDemo(exampleId, visualAssets, null, null);
    }
    public void bindLastStoryboardImageToSelectedSegment() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) { statusMessage.set("Prepara la lectura del documento antes de asociar imágenes."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        Optional<NarrationSegment> selected = selectedSegmentOrFirst();
        if (selected.isEmpty()) { statusMessage.set("Selecciona un fragmento del documento antes de asociar una imagen."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        ProjectSession session = requireSession();
        String imageAssetId = lastStoryboardImageAssetId.get();
        if (imageAssetId == null || imageAssetId.isBlank()) imageAssetId = session.project().assets().byKind(ProjectAssetKind.IMAGE).stream().findFirst().map(ProjectAssetReference::id).orElse("");
        if (imageAssetId.isBlank()) { statusMessage.set("Importa una imagen antes de asociarla al panel visual."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        StoryboardDocument storyboard = currentStoryboard.get();
        if (storyboard == null) storyboard = applicationServices.storyboard().buildStoryboardFromScript().build(script);
        try {
            StoryboardDocument updatedStoryboard = applicationServices.storyboard().bindImageToSegment().bind(
                    storyboard, script, session.project().assets(), selected.get().id(), imageAssetId, "Imagen asociada a " + selected.get().id(), StoryboardDisplayMode.FIT_CONTAIN);
            ProjectMetadata metadata = session.project().metadata()
                    .withKind(ProjectKind.STORYBOARD)
                    .withStatus(ProjectStatus.STORYBOARD_READY);
            DocuPodcastProject updatedProject = session.project()
                    .withMetadata(metadata)
                    .withViewState("activeWorkspace", WorkspaceKind.DOCUMENT_READER.name());
            session.replaceProject(updatedProject, true);
            session.setStoryboard(updatedStoryboard);
            currentStoryboard.set(updatedStoryboard);
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set("Imagen " + imageAssetId + " asociada al fragmento " + selected.get().id() + ".");
            refreshProjectState();
        } catch (RuntimeException ex) {
            statusMessage.set("No se pudo asociar imagen: " + ex.getMessage());
        }
    }
    public void selectDocumentBlockForStoryboardSegment(String segmentId) {
        Optional<NarrationSegment> segment = findSegment(segmentId);
        if (segment.isEmpty()) {
            clearVisualFragmentSelection();
            statusMessage.set("Imagen sin texto asignado. Selecciona una oración o bloque y asóciala desde el panel Visual.");
            refreshProjectState();
            return;
        }
        pinVisualFragment(segment.get().id(), null);
        selectedScriptSegmentId.set(segment.get().id());
        Optional<String> blockId = firstSourceBlockId(segment.get());
        if (blockId.isPresent()) {
            selectDocumentRangeForSegment(segment.get(), blockId.get());
            statusMessage.set("Fragmento asociado seleccionado: " + segment.get().id() + ". El inspector lateral se actualizó con sus capas.");
        } else {
            selectedDocumentBlockId.set("");
            clearDocumentTextRange();
            statusMessage.set("El segmento " + segment.get().id() + " no conserva bloque de origen para resaltar en el documento.");
        }
        bumpDocumentMediaRevision();
        refreshProjectState();
    }
    private void selectDocumentRangeForSegment(NarrationSegment segment, String blockId) {
        ReadableDocument document = currentDocument.get();
        Optional<DocumentTextRange> imageRange = sessions.activeSession()
                .flatMap(session -> session.project().narrativeLayerAssignments().stream()
                        .filter(assignment -> assignment.kind() == NarrativeLayerKind.IMAGE)
                        .filter(assignment -> assignment.textRange().segmentId().equals(segment.id()))
                        .map(NarrativeLayerAssignment::documentRange)
                        .filter(Objects::nonNull)
                        .findFirst());
        if (imageRange.isPresent()) {
            selectedDocumentBlockId.set(imageRange.get().blockId());
            selectedDocumentTextRange.set(imageRange.get());
            applySelectionLabels(documentSelectionWorkflow.documentRangeSelection(
                    document, imageRange.get(), segment.preview(90), "Oración seleccionada"));
            return;
        }
        Optional<DocumentBlock> block = document == null
                ? Optional.empty()
                : document.blockById(blockId);
        selectedDocumentBlockId.set(blockId);
        if (block.isPresent() && !block.get().text().isBlank()) {
            DocumentTextRange range = new DocumentTextRange(blockId, 0, block.get().text().length());
            selectedDocumentTextRange.set(range);
            applySelectionLabels(documentSelectionWorkflow.documentRangeSelection(
                    document, range, block.get().preview(90), "Fragmento seleccionado"));
        } else {
            clearDocumentTextRange();
        }
    }
    public void selectLooseStoryboardImage(String imageAssetId) {
        String normalized = imageAssetId == null ? "" : imageAssetId.strip();
        lastStoryboardImageAssetId.set(normalized);
        statusMessage.set(normalized.isBlank()
                ? "Imagen sin texto asignado. Selecciona una oración o bloque del documento antes de asociar."
                : "Imagen " + normalized + " sin texto asignado. Selecciona una oración o bloque y pulsa Asociar imagen / visual.");
        refreshProjectState();
    }
    public java.util.List<StoryboardScenePresentation> storyboardScenePresentations() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            return java.util.List.of();
        }
        StoryboardDocument storyboard = currentStoryboard.get();
        if (storyboard == null) {
            storyboard = StoryboardDocument.createForScript(script);
        }
        Optional<ProjectSession> active = sessions.activeSession();
        com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog assets = active
                .map(session -> session.project().assets())
                .orElseGet(com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog::empty);
        Optional<Path> projectDirectory = currentProjectFile()
                .map(file -> file.toAbsolutePath().normalize().getParent());
        java.util.List<StoryboardValidationIssue> issues = active.isPresent() && currentStoryboard.get() != null
                ? applicationServices.storyboard().validateStoryboard().validate(storyboard, script, assets)
                : java.util.List.of();
        final StoryboardDocument storyboardForImageLayers = storyboard;
        StoryboardDocument effectiveStoryboard = active
                .map(session -> applicationServices.storyboard().buildStoryboardFromImageLayers().build(
                        script, storyboardForImageLayers, session.project().assets(), session.project().narrativeLayerAssignments()))
                .orElse(storyboardForImageLayers);
        return effectiveStoryboard.scenesFor(script).stream()
                .map(scene -> StoryboardScenePresentation.from(
                        scene,
                        assets,
                        projectDirectory,
                        currentPlaybackManifest.get(),
                        playbackCursor.get(),
                        selectedScriptSegmentId.get(),
                        issues))
                .toList();
    }
    public java.util.List<String> storyboardSceneLabels() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) return java.util.List.of("Prepara la lectura del documento para ver la secuencia visual.");
        StoryboardDocument storyboard = currentStoryboard.get();
        if (storyboard == null) storyboard = StoryboardDocument.createForScript(script);
        return storyboard.scenesFor(script).stream().map(scene -> scene.segmentId() + " · " + scene.title() + " · " + scene.statusLabel()).toList();
    }
    public java.util.List<String> storyboardImageAssetLabels() {
        Optional<ProjectSession> active = sessions.activeSession();
        if (active.isEmpty()) return java.util.List.of("Guarda o abre un proyecto para importar imágenes.");
        java.util.List<ProjectAssetReference> images = active.get().project().assets().byKind(ProjectAssetKind.IMAGE);
        if (images.isEmpty()) return java.util.List.of("No hay imágenes importadas. Usa Importar imagen para visuales.");
        return images.stream().map(asset -> asset.id() + " · " + asset.displayName() + " · " + asset.relativePath()).toList();
    }
    public java.util.List<DocumentFragmentRailPresentation> documentFragmentRailPresentations() {
        return DocumentRailProjectionFactory.fragments(currentDocument.get(), currentScript.get(), sessions.activeSession().map(ProjectSession::project).orElse(null), currentStoryboard.get(), currentProjectFile().map(file -> file.toAbsolutePath().normalize().getParent()));
    }
    public Optional<String> theatreIntermediateFrameUri(String segmentId, String nextSegmentId) { Optional<ProjectSession> session = sessions.activeSession(); if (session.isEmpty() || segmentId == null || nextSegmentId == null || segmentId.isBlank() || nextSegmentId.isBlank()) return Optional.empty(); TheatreProjectLayer theatre = session.get().project().theatre(); Optional<String> from = interventionIdForSegment(theatre, findSegment(segmentId).orElse(null)); Optional<String> to = interventionIdForSegment(theatre, findSegment(nextSegmentId).orElse(null)); if (from.isEmpty() || to.isEmpty() || from.get().equals(to.get()) || !new TheatreVisualContinuityResolver().canInterpolate(theatre, from.get(), to.get())) return Optional.empty(); return theatre.intermediateFrames().stream().filter(frame -> frame.fromIntervencionId().equals(from.get()) && frame.toIntervencionId().equals(to.get())).findFirst().flatMap(frame -> projectImageAssetPath(frame.assetId())).map(path -> path.toUri().toString()); }
    public java.util.List<DocumentRailImagePresentation> documentRailImagePresentations() { return DocumentRailProjectionFactory.images(sessions.activeSession().map(ProjectSession::project).orElse(null), currentStoryboard.get(), currentProjectFile().map(file -> file.toAbsolutePath().normalize().getParent()), this::findSegment, this::firstSourceBlockId); }
    public String selectedDocumentImageUri() {
        String explicitSelectedImage = selectedVisualFragmentImageUri.get();
        if (explicitSelectedImage != null && !explicitSelectedImage.isBlank()) return explicitSelectedImage;
        String pinnedSegment = selectedVisualFragmentSegmentId.get(); if (pinnedSegment != null && !pinnedSegment.isBlank()) return visualFragmentImageUri(pinnedSegment).orElse("");
        DocumentTextRange selectedRange = selectedDocumentTextRange.get();
        if (selectedRange == null) return "";
        String selectedSegment = selectedScriptSegmentId.get() == null ? "" : selectedScriptSegmentId.get().strip();
        java.util.List<DocumentFragmentRailPresentation> fragments = documentFragmentRailPresentations();
        return fragments.stream()
                .filter(fragment -> sameDocumentRange(fragment, selectedRange))
                .filter(fragment -> selectedSegment.isBlank() || fragment.segmentId().equals(selectedSegment))
                .map(DocumentFragmentRailPresentation::imageFileUri).filter(uri -> uri != null && !uri.isBlank()).findFirst()
                .or(() -> fragments.stream().filter(fragment -> sameDocumentRange(fragment, selectedRange)).map(DocumentFragmentRailPresentation::imageFileUri).filter(uri -> uri != null && !uri.isBlank()).findFirst())
                .orElseGet(() -> selectedDocumentImageUriFromLayer(selectedRange));
    }
    public boolean canEditTheatreStoryboardFrame() { return theatreStoryboardFrameWorkflow.canEdit(currentProjectMode.get(), sessions.activeSession(), currentScript.get(), selectedScriptSegmentId.get()); }
    public Optional<TheatreFrameSketchContext> selectedTheatreStoryboardFrameContext() { return sessions.activeSession().flatMap(session -> theatreStoryboardFrameWorkflow.context(session, currentStoryboard.get(), currentScript.get(), selectedScriptSegmentId.get(), currentProjectDirectory())); }
    public void saveTheatreStoryboardFrame(String segmentId, Path framePng, String inkStateJson, boolean activateDrawn) throws IOException { applyTheatreFrameResult(theatreStoryboardFrameWorkflow.save(applicationServices, requireSession(), currentStoryboard.get(), currentScript.get(), segmentId, framePng, inkStateJson, activateDrawn, currentProjectDirectory())); }
    public void toggleTheatreStoryboardFrameVariant(String segmentId) { try { applyTheatreFrameResult(theatreStoryboardFrameWorkflow.toggle(applicationServices, requireSession(), currentStoryboard.get(), currentScript.get(), segmentId, currentProjectDirectory())); } catch (IOException | RuntimeException ex) { statusMessage.set("No se pudo alternar variante visual: " + ex.getMessage()); } } public TheatreAudioTrackWorkflow.Result saveTheatreAudioTrack(String trackId, String segmentId, String interventionId, PreparedAudioAsset source, double from, TheatreProjectLayer.AudioTrackEndMode endMode, double to, double volume, boolean fade, boolean replace) throws IOException { var result = theatreAudioTrackWorkflow.save(applicationServices, requireSession(), currentScript.get(), currentPlaybackManifest.get(), trackId, segmentId, interventionId, source, from, endMode, to, volume, fade, replace); if (result.saved()) saveCurrentProjectAs(currentProjectFile().orElseThrow()); statusMessage.set(result.message()); bumpDocumentMediaRevision(); refreshProjectState(); return result; } public TheatreAudioTrackWorkflow.Result confirmTheatreAudioTrackReplacement(TheatreProjectLayer.TheatreAudioTrack track) { var result = theatreAudioTrackWorkflow.saveCandidate(applicationServices, requireSession(), currentScript.get(), currentPlaybackManifest.get(), track, true); statusMessage.set(result.message()); bumpDocumentMediaRevision(); refreshProjectState(); return result; } public void removeTheatreAudioTrack(String trackId) { try { statusMessage.set(theatreAudioTrackWorkflow.remove(applicationServices, requireSession(), trackId).message()); saveCurrentProjectAs(currentProjectFile().orElseThrow()); } catch (IOException ex) { throw new IllegalStateException(ex.getMessage(), ex); } bumpDocumentMediaRevision(); refreshProjectState(); }
    public void activateTheatreStoryboardVisualVariant(String segmentId, String variant) { try { applyTheatreFrameResult(theatreStoryboardFrameWorkflow.activate(applicationServices, requireSession(), currentStoryboard.get(), currentScript.get(), segmentId, variant, currentProjectDirectory())); } catch (IOException | RuntimeException ex) { statusMessage.set("No se pudo activar la variante visual: " + ex.getMessage()); } }
    private void applyTheatreFrameResult(TheatreStoryboardFrameWorkflow.FrameResult result) { currentStoryboard.set(result.storyboard()); lastStoryboardImageAssetId.set(result.activeAsset().id()); pinVisualFragment(result.segmentId(), result.activeUri()); bumpDocumentMediaRevision(); statusMessage.set(result.message()); refreshProjectState(); }
    private void pinVisualFragment(DocumentFragmentRailPresentation fragment) { if (fragment == null) { clearVisualFragmentSelection(); return; } selectedVisualFragmentSegmentId.set(fragment.segmentId()); selectedVisualFragmentImageUri.set(fragment.imageFileUri()); selectedVisualFragmentKey.set(DocumentVisualFragmentKey.from(fragment)); }
    private void pinVisualFragment(String segmentId, String imageUri) { String normalizedSegment = segmentId == null ? "" : segmentId.strip(); DocumentVisualFragmentKey current = selectedVisualFragmentKey.get(); DocumentVisualFragmentKey key = current != null && normalizedSegment.equals(current.segmentId()) ? current : documentFragmentRailPresentations().stream().filter(fragment -> normalizedSegment.equals(fragment.segmentId())).findFirst().map(DocumentVisualFragmentKey::from).orElse(DocumentVisualFragmentKey.empty()); selectedVisualFragmentSegmentId.set(normalizedSegment); String normalizedUri = imageUri == null ? "" : imageUri.strip(); selectedVisualFragmentImageUri.set(!normalizedUri.isBlank() ? normalizedUri : visualFragmentImageUri(normalizedSegment).orElse("")); selectedVisualFragmentKey.set(key); }
    private void clearVisualFragmentSelection() { selectedVisualFragmentSegmentId.set(""); selectedVisualFragmentImageUri.set(""); selectedVisualFragmentKey.set(DocumentVisualFragmentKey.empty()); }
    private Optional<String> visualFragmentImageUri(String segmentId) { String normalized = segmentId == null ? "" : segmentId.strip(); if (normalized.isBlank()) return Optional.empty(); return documentFragmentRailPresentations().stream().filter(fragment -> normalized.equals(fragment.segmentId())).map(DocumentFragmentRailPresentation::imageFileUri).filter(uri -> uri != null && !uri.isBlank()).findFirst(); }
    private boolean sameDocumentRange(DocumentFragmentRailPresentation fragment, DocumentTextRange range) { return fragment != null && range != null && fragment.blockId().equals(range.blockId()) && fragment.startOffset() == range.startOffset() && fragment.endOffset() == range.endOffset(); } private Optional<DocumentFragmentRailPresentation> visualFragmentForSelection(String blockId, DocumentTextRange range) { String normalized = blockId == null ? "" : blockId.strip(); List<DocumentFragmentRailPresentation> fragments = documentFragmentRailPresentations(); return fragments.stream().filter(fragment -> sameDocumentRange(fragment, range)).findFirst().or(() -> fragments.stream().filter(fragment -> range != null && fragment.blockId().equals(range.blockId()) && fragment.startOffset() <= range.startOffset() && fragment.endOffset() >= range.endOffset()).findFirst()).or(() -> fragments.stream().filter(fragment -> fragment.blockId().equals(normalized)).findFirst()); }
    private String selectedDocumentImageUriFromLayer(DocumentTextRange selectedRange) {
        Optional<ProjectSession> active = sessions.activeSession();
        if (active.isEmpty()) {
            return "";
        }
        Optional<Path> projectDirectory = currentProjectFile()
                .map(file -> file.toAbsolutePath().normalize().getParent());
        Optional<ScriptTextRange> scriptRange = narrativeLayerWorkflow.scriptRangeForLayer(
                selectedDocumentSegmentOrSelected(), selectedRange, selectedDocumentTextPreview);
        return scriptRange.flatMap(range -> active.get().project().narrativeLayerAssignments().stream()
                        .filter(assignment -> assignment.kind() == NarrativeLayerKind.IMAGE)
                        .filter(assignment -> assignment.textRange().segmentId().equals(range.segmentId()))
                        .filter(assignment -> assignment.textRange().startOffset() < range.endOffset()
                                && range.startOffset() < assignment.textRange().endOffset())
                        .findFirst())
                .flatMap(imageAssignment -> active.get().project().assets()
                        .byId(imageAssignment.targetId())
                        .filter(ProjectAssetReference::isImage))
                .flatMap(image -> assetUri(projectDirectory, image))
                .orElse("");
    }
    public java.util.List<String> storyboardValidationLabels() {
        NarrationScriptDocument script = currentScript.get();
        StoryboardDocument storyboard = currentStoryboard.get();
        Optional<ProjectSession> active = sessions.activeSession();
        if (script == null || storyboard == null || active.isEmpty()) {
            return java.util.List.of("Crea la secuencia visual desde el documento preparado para ver validación.");
        }
        java.util.List<StoryboardValidationIssue> issues = applicationServices.storyboard().validateStoryboard()
                .validate(storyboard, script, active.get().project().assets());
        if (issues.isEmpty()) {
            return java.util.List.of("Secuencia visual válida: las imágenes asociadas apuntan a segmentos y assets existentes.");
        }
        return issues.stream()
                .map(issue -> issue.level() + " · " + issue.referenceId() + " · " + issue.message())
                .toList();
    }
    public java.util.List<AudioEngineAvailability> documentAudioSourceAvailability() {
        try {
            return audioWorkflow.engineAvailability();
        } catch (RuntimeException ex) {
            return java.util.List.of(AudioEngineAvailability.testMode(), AudioEngineAvailability.computerAudio());
        }
    }
    public java.util.List<String> audioEngineReadinessLines() { try { return audioWorkflow.engineReadinessLines(); } catch (RuntimeException ex) { return java.util.List.of("Motores y dependencias: no se pudo inspeccionar readiness. Usa Configuración para revisar motores."); } }
    public void selectDocumentAudioSource(String sourceLabel) {
        try {
            refreshChunksAfterDocumentVoiceChange(audioWorkflow.selectDocumentAudioSource(sourceLabel));
        }
        catch (IOException | RuntimeException ex) { reportUserVisibleError("No se pudo cambiar el origen de voz: " + ex.getMessage()); }
    }
    public void useVoiceForDocument(VoiceProfile voice) {
        try {
            refreshChunksAfterDocumentVoiceChange(audioWorkflow.useVoiceForDocument(voice));
        }
        catch (IOException | RuntimeException ex) { reportUserVisibleError("No se pudo usar la voz seleccionada: " + ex.getMessage()); }
    }
    public void useVoiceForDocumentFrom(VoiceProfile voice, String blockId) {
        try {
            audioWorkflow.useVoiceForDocument(voice);
            invalidatePersistedAudioAfterVoiceSelection();
            if (currentDocument.get() != null && blockId != null && !blockId.isBlank()) {
                Optional<NarrationSegment> segment = firstSegmentForDocumentBlock(blockId);
                if (segment.isPresent()) {
                    submitAudioGenerationFromSegment(segment.get(), false);
                    return;
                }
            }
            generateAudioChunksWithoutPlayback();
        }
        catch (IOException | RuntimeException ex) { reportUserVisibleError("No se pudo usar la voz seleccionada: " + ex.getMessage()); }
    }
    public String configuredVoiceProfileId() { return audioWorkflow.configuredVoiceProfileId(); }
    private void refreshChunksAfterDocumentVoiceChange(String result) {
        boolean invalidated = invalidatePersistedAudioAfterVoiceSelection();
        if (currentDocument.get() == null) {
            statusMessage.set(invalidated ? result + " Fragmentos de audio anteriores eliminados para evitar reproducir audio de otra voz." : result);
            return;
        }
        statusMessage.set(result + " Regenerando fragmentos de audio del documento; las voces específicas se respetan.");
        generateAudioChunksWithoutPlayback();
    }
    private boolean invalidatePersistedAudioAfterVoiceSelection() {
        Optional<Path> file = currentProjectFile();
        if (file.isEmpty()) { resetPlaybackState(); return false; }
        if (currentDocument.get() == null) deletePersistedAudioAsync(file.get().toAbsolutePath().normalize().getParent(), "Audio anterior eliminado tras cambiar la voz.");
        else resetPlaybackState();
        return true;
    }
    public AudioEngineDescriptor audioEngineDescriptor() { return audioWorkflow.engineDescriptor(); }
    public boolean audioEngineUnavailableForDocumentPrimaryAction() {
        AudioEngineDescriptor descriptor = audioEngineDescriptor(); if (descriptor.configured()) { return false; }
        PlaybackManifest manifest = currentPlaybackManifest.get(); if (manifest == null || manifest.emptyManifest()) { manifest = ensurePlaybackManifestLoaded(); }
        return manifest == null || manifest.emptyManifest();
    }
    public boolean audioEngineUnavailableForGeneration() { return !audioEngineDescriptor().configured(); }
    public String audioEngineUnavailableMessage() { return audioWorkflow.audioEngineUnavailableMessage(); }
    public AudioQueueState audioQueueState() {
        return audioWorkflow.buildAudioQueueState(activeAudioJobStatus.get(), currentProjectFile(), playbackCueLabels());
    }
    public void generateAudioChunksWithoutPlayback() {
        ReadableDocument document = currentDocument.get();
        if (document == null) { statusMessage.set("Abre una fuente documental antes de generar fragmentos de audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        documentPlaybackRequested = false; waitingForBufferedSegmentAfter = ""; playSingleCueOnly = false;
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            buildNarrationScriptFromDocument(); script = currentScript.get();
            if (script == null || script.empty()) { statusMessage.set("No se pudo preparar la lectura para generar fragmentos de audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        }
        int page = Math.max(1, pdfVisiblePageNumber()); Optional<NarrationSegment> pdfStart = PdfVisiblePageAudioStartSelector.firstSegmentAtOrAfterVisiblePage(document, script, page);
        if (PdfVisiblePageAudioStartSelector.isPdf(document) && pdfStart.isEmpty()) { statusMessage.set("Haz clic en la hoja para analizar OCR antes de generar audio desde la pagina PDF " + page + "."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        if (pdfStart.isPresent()) { submitAudioGenerationFromSegment(pdfStart.get(), false); } else { submitAudioGeneration(); }
        if (audioJobRunning.get()) { statusMessage.set("Generando fragmentos de audio sin iniciar reproducción. Puedes ocultar el panel y continuar trabajando."); }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState();
    }
    public void submitAudioGenerationWithPendingExport(Runnable exportTask) {
        pendingExportTask = exportTask;
        submitAudioGeneration();
    }
    public void generateAudioChunksFromSelectedFragment() {
        if (currentScript.get() == null || currentScript.get().empty()) { buildNarrationScriptFromDocument(); }
        Optional<NarrationSegment> segment = selectedDocumentSegmentOrSelected();
        if (segment.isEmpty()) { statusMessage.set("Selecciona una oración o bloque antes de renderizar audio desde ese fragmento."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        documentPlaybackRequested = false; waitingForBufferedSegmentAfter = ""; playSingleCueOnly = false; submitAudioGenerationFromSegment(segment.get(), false);
    }
    public void submitAudioGeneration() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) { statusMessage.set("Prepara la lectura del documento antes de generar audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        ProjectSession session = requireSession();
        Optional<Path> file = session.projectFile();
        if (file.isEmpty()) { statusMessage.set("Guarda el proyecto antes de generar audio para crear la carpeta jobs/."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        if (audioEngineUnavailableForGeneration()) { statusMessage.set(audioEngineUnavailableMessage()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        try { saveCurrentProjectAs(file.get()); }
        catch (IOException ex) { statusMessage.set("No se pudo guardar el proyecto antes de generar audio: " + ex.getMessage()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        Path projectDirectory = file.get().toAbsolutePath().normalize().getParent();
        AudioGenerationRequest request = audioGenerationRequestFor(session, script, projectDirectory, session.title());
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        AudioEngineDescriptor engine = audioEngineDescriptor();
        submitFreshAudioRequestAsync(projectDirectory, request, "Generación enviada", engine.statusLabel());
        refreshProjectState();
    }
    public void submitMockAudioGeneration() { submitAudioGeneration(); }
    private AudioGenerationRequest audioGenerationRequestFor(ProjectSession session, NarrationScriptDocument script, Path projectDirectory, String jobName) {
        return audioWorkflow.buildGenerationRequest(session, script, projectDirectory, jobName);
    }
    private boolean submitAudioGenerationFromSegment(NarrationSegment startSegment, boolean requestPlayback) {
        if (startSegment == null) {
            statusMessage.set("Selecciona una oración o bloque antes de preparar audio desde ahí.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
            return false;
        }
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) { statusMessage.set("Prepara la lectura del documento antes de generar audio desde la selección."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return false; }
        ProjectSession session = requireSession();
        Optional<Path> file = session.projectFile();
        if (file.isEmpty()) { statusMessage.set("Guarda el proyecto antes de generar audio desde la selección."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return false; }
        if (audioEngineUnavailableForGeneration()) { statusMessage.set(audioEngineUnavailableMessage()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return false; }
        try { saveCurrentProjectAs(file.get()); }
        catch (IOException ex) { statusMessage.set("No se pudo guardar el proyecto antes de generar audio desde la selección: " + ex.getMessage()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return false; }
        Path projectDirectory = file.get().toAbsolutePath().normalize().getParent();
        NarrationScriptDocument suffixScript = AudioWorkflowCoordinator.scriptStartingAt(script, startSegment.id());
        if (suffixScript.empty()) { statusMessage.set("No hay fragmentos narrables desde la selección " + startSegment.id() + "."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return false; }
        if (requestPlayback && audioJobRunning.get()) {
            documentPlaybackRequested = true;
            playSingleCueOnly = false;
            waitingForBufferedSegmentAfter = "";
            pendingPlaybackStartSegmentId = startSegment.id();
            selectedScriptSegmentId.set(startSegment.id());
            statusMessage.set("Audio en generación. La reproducción comenzará automáticamente cuando esté listo.");
            refreshProjectState();
            return true;
        }
        AudioGenerationRequest request = audioGenerationRequestForSelection(session, script, suffixScript, projectDirectory, session.title(), startSegment.id());
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        documentPlaybackRequested = requestPlayback; playSingleCueOnly = false; waitingForBufferedSegmentAfter = "";
        pendingPlaybackStartSegmentId = startSegment.id(); selectedScriptSegmentId.set(startSegment.id());
        submitFreshAudioRequestAsync(projectDirectory, request, "Generando audio desde la selección " + startSegment.id() + ": el primer fragmento preparado será el seleccionado", audioEngineDescriptor().statusLabel());
        refreshProjectState(); return true;
    }
    private AudioGenerationRequest audioGenerationRequestForSelection(ProjectSession session, NarrationScriptDocument fullScript,
                                                                      NarrationScriptDocument suffixScript, Path projectDirectory,
                                                                      String jobName, String startSegmentId) {
        return audioWorkflow.buildGenerationRequestForSelection(session, fullScript, suffixScript, projectDirectory, jobName, startSegmentId);
    }
    public void resumeMostRecentRecoverableAudioJob() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) { statusMessage.set("Prepara la lectura del documento antes de reanudar audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        Optional<Path> file = currentProjectFile();
        if (file.isEmpty()) { statusMessage.set("Guarda el proyecto antes de reanudar jobs de audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
        try {
            saveCurrentProjectAs(file.get());
            Path projectDirectory = file.get().toAbsolutePath().normalize().getParent();
            Optional<AudioJobSnapshot> recoverable = audioWorkflow.recoverableSnapshot(projectDirectory);
            if (recoverable.isEmpty()) { statusMessage.set("No hay jobs reanudables. Revisa el historial persistido de audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); return; }
            ProjectSession session = requireSession();
            AudioGenerationRequest request = audioGenerationRequestFor(session, script, projectDirectory, session.title());
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            String jobId = audioWorkflow.resume(request, recoverable.get(), this::acceptAudioStatus);
            statusMessage.set("Seguir generando enviado: " + jobId + ". Motor: " + audioEngineDescriptor().statusLabel() + ". Se conservan fragmentos completados y se completan los pendientes.");
            audioJobRunning.set(true);
            refreshProjectState();
        } catch (IOException ex) { statusMessage.set("No se pudo reanudar el job de audio: " + ex.getMessage()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); }
    }
    public void cancelActiveAudioJob() {
        statusMessage.set(audioWorkflow.cancelActiveAudioJob(activeAudioJobStatus.get(), updated -> {
            waitingForBufferedSegmentAfter = "";
            audioJobRunning.set(false);
            activeAudioJobStatus.set(updated);
        }));
    }
    private boolean cancelActiveAudioJobSilently() {
        return audioWorkflow.cancelActiveAudioJobSilently(activeAudioJobStatus.get(), updated -> {
            waitingForBufferedSegmentAfter = "";
            audioJobRunning.set(false);
            activeAudioJobStatus.set(updated);
        });
    }
    private void acceptAudioStatus(AudioJobStatusDto status) { audioStatusUiThrottle.submit(status); }
    private void applyAudioStatusOnFxThread(AudioJobStatusDto status) {
        activeAudioJobStatus.set(status); audioJobRunning.set(status.running()); statusMessage.set(status.statusLine());
        boolean playbackNeedsManifest = status.completedSegments() > 0 && (documentPlaybackRequested || !waitingForBufferedSegmentAfter.isBlank()
                || playbackTransport.continuationActive() || playbackTransport.playerPlaying());
        PlaybackManifest bufferedManifest = playbackNeedsManifest ? rebuildPlaybackManifestFromLatestJob() : currentPlaybackManifest.get();
        if (playbackNeedsManifest && bufferedManifest != null && !bufferedManifest.emptyManifest()
                && playbackTransport.sequentialActive()) {
            playbackTransport.refreshRuntimeQueue(bufferedManifest);
            playbackTransport.refreshSequentialQueue(bufferedManifest);
        }
        if (tryContinueAfterBufferGap(bufferedManifest) || tryStartBufferedPlayback(status, bufferedManifest)) { refreshProjectState(); return; }
        if (documentPlaybackRequested && status.running()) { statusMessage.set(status.statusLine() + " · " + playbackBufferStatusLabel()); }
        if (status.completed()) {
            registerCompletedAudioAssets(status);
            Runnable task = pendingExportTask;
            if (task != null) {
                pendingExportTask = null;
                try { task.run(); } catch (Exception ex) { statusMessage.set("Error al exportar tras renderizar: " + ex.getMessage()); }
                refreshProjectState();
                return;
            }
            PlaybackManifest completedManifest = rebuildPlaybackManifestFromLatestJob();
            if (completedManifest != null && !completedManifest.emptyManifest()
                    && playbackTransport.sequentialActive()) {
                playbackTransport.refreshRuntimeQueue(completedManifest);
                playbackTransport.refreshSequentialQueue(completedManifest);
            }
            if (tryContinueAfterBufferGap(completedManifest) || tryStartBufferedPlayback(status, completedManifest)) {
                refreshProjectState();
                return;
            }
            refreshProjectState();
            return;
        }
        if (!playbackNeedsManifest && status.running()) { refreshStreamingBufferStatus(); return; }
        refreshProjectState();
    }
    private boolean tryStartBufferedPlayback(AudioJobStatusDto status, PlaybackManifest manifest) {
        if (!canAttemptBufferedPlayback(status, manifest)) {
            return false;
        }
        Optional<PlaybackCue> firstCue = preferredPlaybackStartCue(manifest);
        if (firstCue.isEmpty()) {
            return false;
        }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        return startPlaybackFromCue(firstCue.get(), true);
    }
    private boolean canAttemptBufferedPlayback(AudioJobStatusDto status, PlaybackManifest manifest) {
        if (!documentPlaybackRequested || status == null || manifest == null || manifest.emptyManifest()) { return false; }
        if (playbackTransport.playerPlaying() || playbackTransport.continuationActive() || playbackTransport.sequentialActive()) { return false; }
        if (!pendingPlaybackStartSegmentId.isBlank() && manifest.cueForSegment(pendingPlaybackStartSegmentId).isPresent()) {
            return true;
        }
        if (playbackWorkflow.canStartBufferedPlayback(documentPlaybackRequested, playbackCursor.get(), status, manifest, playbackBufferPolicy)) { return true; }
        PlaybackCursor cursor = playbackCursor.get();
        return (cursor == null || cursor.stoppedState())
                && status.completedSegments() > 0
                && pendingPlaybackStartSegmentId.isBlank();
    }
    private boolean tryContinueAfterBufferGap(PlaybackManifest manifest) {
        if (!playbackWorkflow.canContinueAfterGap(waitingForBufferedSegmentAfter, manifest)) {
            return false;
        }
        Optional<PlaybackCue> nextCue = playbackWorkflow.nextCueAfterGap(waitingForBufferedSegmentAfter, manifest);
        if (nextCue.isEmpty() && manifest != null && manifest.cueForUnit(waitingForBufferedSegmentAfter).isEmpty()) {
            // Some resumed/suffix jobs build a fresh manifest that starts after the cue that caused
            // the buffer gap. In that case the next available cue is the first cue of the new
            // manifest; do not wait for another manual Play click.
            nextCue = manifest.firstCue();
        }
        if (nextCue.isEmpty()) {
            statusMessage.set(playbackWorkflow.waitingForBufferMessage(playbackBufferPolicy, playbackBufferStatusLabel()));
            refreshStreamingBufferStatus();
            return false;
        }
        String previous = waitingForBufferedSegmentAfter;
        waitingForBufferedSegmentAfter = "";
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        if (transitionToCue(nextCue.get())) {
            statusMessage.set("Buffer recuperado. Continuando después de " + previous + " → " + nextCue.get().unitId() + ".");
            return true;
        }
        statusMessage.set("Buffer recuperado, pero no se pudo abrir el siguiente WAV: " + nextCue.get().unitId() + ".");
        return false;
    }
    private void registerCompletedAudioAssets(AudioJobStatusDto status) {
        sessions.activeSession().ifPresent(session ->
                audioWorkflow.registerCompletedAudioAssets(session, status, this::bumpDocumentMediaRevision));
    }
    public java.util.List<String> persistedAudioJobLabels() {
        return audioWorkflow.persistedAudioJobLabels(currentProjectFile());
    }
    private Optional<AudioJobSnapshot> selectedAudioQueueSnapshot(java.util.List<AudioJobSnapshot> snapshots) { return audioWorkflow.selectedSnapshot(snapshots, activeAudioJobStatus.get().jobId()); }
    private java.util.List<String> audioJobDetailLines(AudioJobSnapshot selected) { return audioWorkflow.jobDetailLines(selected); }
    private java.util.List<String> audioProcessDiagnosticLabels(Path projectDirectory, AudioJobSnapshot selected) {
        try {
            return audioWorkflow.diagnosticLabels(projectDirectory, selected);
        } catch (IOException ex) {
            return java.util.List.of("No se pudieron leer diagnósticos de proceso: " + ex.getMessage());
        }
    }
    public java.util.List<String> persistedAudioJobDetailLabels() {
        return audioWorkflow.persistedAudioJobDetailLabels(currentProjectFile());
    }
    public java.util.List<String> audioProcessDiagnosticLabels() {
        return audioWorkflow.audioProcessDiagnosticLabels(currentProjectFile());
    }
    private void loadLatestPersistedAudioStatus(Path projectFile) {
        AudioJobStatusDto restored = audioWorkflow.loadLatestPersistedAudioStatus(
                projectFile == null ? Optional.empty() : Optional.of(projectFile));
        activeAudioJobStatus.set(restored);
        audioJobRunning.set(false);
    }
    public PlaybackManifest ensurePlaybackManifestLoaded() {
        PlaybackManifest current = currentPlaybackManifest.get();
        AudioJobStatusDto status = activeAudioJobStatus.get();
        int knownCues = current == null ? 0 : current.cueCount();
        if (audioJobRunning.get() || current == null || current.emptyManifest()
                || status == null || status.completedSegments() > knownCues) {
            return rebuildPlaybackManifestFromLatestJob();
        }
        return current;
    }
    public PlaybackManifest rebuildPlaybackManifestFromLatestJob() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            currentPlaybackManifest.set(PlaybackManifest.empty());
            return currentPlaybackManifest.get();
        }
        Optional<Path> file = currentProjectFile();
        if (file.isEmpty()) {
            currentPlaybackManifest.set(PlaybackManifest.empty());
            return currentPlaybackManifest.get();
        }
        try {
            Path projectDirectory = file.get().toAbsolutePath().normalize().getParent();
            java.util.List<AudioJobSnapshot> snapshots = audioWorkflow.persistedJobs(projectDirectory);
            String activeJobId = activeAudioJobStatus.get() == null ? "" : activeAudioJobStatus.get().jobId();
            Optional<AudioJobSnapshot> playable = playableAudioJobSelector.select(snapshots, activeJobId, script);
            if (playable.isEmpty()) {
                currentPlaybackManifest.set(PlaybackManifest.empty());
                return currentPlaybackManifest.get();
            }
            PlaybackManifest manifest = sessions.activeSession()
                    .map(session -> {
                        var plan = applicationServices.render().buildNarrationRenderPlan()
                                .build(script, session.project());
                        return applicationServices.playback().buildPlaybackManifest()
                                .build(script, playable.get(), currentStoryboard.get(), plan, session.project());
                    })
                    .orElseGet(() -> applicationServices.playback().buildPlaybackManifest()
                            .build(script, playable.get(), currentStoryboard.get()));
            currentPlaybackManifest.set(manifest);
            return manifest;
        } catch (IOException ex) {
            currentPlaybackManifest.set(PlaybackManifest.empty());
            statusMessage.set("No se pudo construir manifest de playback: " + ex.getMessage());
            return currentPlaybackManifest.get();
        }
    }
    public PlaybackSyncState playbackSyncState() { return PlaybackSyncState.from(currentScript.get(), currentStoryboard.get(), currentPlaybackManifest.get(), playbackCursor.get()); }
    public java.util.List<String> playbackCueLabels() { return playbackSyncState().cueLabels(); }
    public java.util.List<String> playbackSynchronizedSegmentLabels() { return playbackSyncState().segmentLabels(); }
    public String playbackManifestSummary() {
        PlaybackSyncState state = playbackSyncState();
        return state.summaryLabel() + " · " + state.readinessLabel()
                + " · " + playbackTransport.playerStatusLabel();
    }
    private void tickPlayback() {
        if (playbackTransport.sequentialActive()) { return; }
        PlaybackCursor cursor = playbackCursor.get();
        PlaybackManifest manifest = currentPlaybackManifest.get();
        if (cursor == null || cursor.paused() || manifest == null || manifest.emptyManifest()) {
            return;
        }
        Optional<PlaybackCue> cue = cueForCursor(manifest, cursor);
        if (cue.isEmpty()) {
            stopPlayback();
            return;
        }
        if (playbackTransport.shouldIgnoreTransientStop()) {
            return;
        }
        double absolutePosition = playbackTransport.absolutePosition(cue.get(), cursor);
        if (!playbackTransport.sequentialActive() && playbackTransport.shouldAdvance(cue.get(), absolutePosition)) {
            advanceAfterCompletedCue(cue.get());
            return;
        }
        PlaybackCursor updated = new PlaybackCursor(cue.get().segmentId(), absolutePosition, false);
        selectedScriptSegmentId.set(updated.segmentId());
        playbackCursor.set(updated); syncTheatreAudioPlayback(updated.positionSeconds());
        refreshProjectState();
    }
    private void handlePlaybackFinishedOnFxThread(Path audioFile) { Platform.runLater(() -> advanceAfterPlayerFinished(audioFile)); }
    private void advanceAfterPlayerFinished(Path audioFile) {
        PlaybackCursor cursor = playbackCursor.get();
        PlaybackManifest manifest = currentPlaybackManifest.get();
        if (cursor == null || cursor.paused() || manifest == null || manifest.emptyManifest()) {
            return;
        }
        Optional<PlaybackCue> cue = playbackTransport.sequentialActiveCue().or(() -> cueForCursor(manifest, cursor));
        if (cue.isEmpty() || !playbackTransport.matchesCompletedAudioFile(cue.get(), currentProjectFile().orElse(null), audioFile)) {
            return;
        }
        recordPlaybackEvent("player-finished-callback", cue.get(), "java-sound-finished");
        playbackTransport.markPlayerFinished();
        if (playbackTransport.sequentialActive()) {
            statusMessage.set("Fragmento completado por reproductor: " + cue.get().unitId() + ". "
                    + playbackRuntimeDiagnosticLabel(manifest, cue.get(), playbackTransport.nextRuntimeCueAfter(cue.get())));
            playbackTransport.advanceSequentialAfterCurrentCueFinished(cue.get().unitId());
            return;
        }
        if (playbackTransport.shouldAdvance(cue.get(), cue.get().endSeconds())) {
            advanceAfterCompletedCue(cue.get());
        }
    }
    private String playbackRuntimeDiagnosticLabel(PlaybackManifest manifest, PlaybackCue activeCue, Optional<PlaybackCue> nextCue) {
        return playbackTransport.runtimeDiagnosticLabel(currentProjectFile().orElse(null), manifest, activeCue, nextCue, playbackRate.get());
    }
    private Path recordPlaybackEvent(String event, PlaybackCue cue, String reason) {
        return playbackTransport.recordPlaybackEvent(currentProjectFile().orElse(null), event, cue, playbackRate.get(), reason,
                ignored -> statusMessage.set("Diagnóstico playback guardado por posible corte temprano: " + playbackTransport.diagnosticFile(currentProjectFile().orElse(null))));
    }
    private void advanceAfterCompletedCue(PlaybackCue completedCue) {
        recordPlaybackEvent("cue-advance", completedCue, playbackTransport.sequentialActive() ? "sequential-queue" : "deadline-or-callback");
        playbackTransport.stopCueMonitoring();
        playbackTransport.consumePlayerFinishedMarker();
        if (playSingleCueOnly) {
            playSingleCueOnly = false;
            resetPlaybackTransportOnly();
            statusMessage.set("Fragmento reproducido.");
            refreshProjectState();
            return;
        }
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        playbackTransport.refreshRuntimeQueue(manifest);
        Optional<PlaybackCue> next = playbackTransport.nextRuntimeCueAfter(completedCue);
        if (next.isEmpty()) {
            next = playbackTransport.nextCue(manifest, completedCue);
        }
        if (next.isEmpty()) {
            next = playbackTransport.nextCueAfterCompletedSegmentFallback(manifest, completedCue);
        }
        if (next.isEmpty()) {
            if (audioJobRunning.get() && playbackBufferPolicy.pauseWhenBufferMissing()) {
                waitForBufferedContinuation(completedCue);
                return;
            }
            resetPlaybackTransportOnly();
            documentPlaybackRequested = false;
            waitingForBufferedSegmentAfter = "";
            statusMessage.set("Playback completado. " + playbackTransport.manifestRuntimeLabel(manifest)
                    + " · " + playbackRuntimeDiagnosticLabel(manifest, completedCue, Optional.empty()));
            refreshProjectState();
            return;
        }
        if (transitionToCue(next.get())) {
            statusMessage.set("Continuando lectura: " + completedCue.unitId() + " → " + next.get().unitId()
                    + ". " + playbackRuntimeDiagnosticLabel(manifest, next.get(), playbackTransport.nextRuntimeCueAfter(next.get())));
            refreshProjectState();
            return;
        }
        if (audioJobRunning.get() && playbackBufferPolicy.pauseWhenBufferMissing()) {
            waitForBufferedContinuation(completedCue);
            return;
        }
        resetPlaybackTransportOnly();
        statusMessage.set("No se pudo continuar con el siguiente fragmento. " + playbackTransport.manifestRuntimeLabel(manifest)
                + " · " + playbackRuntimeDiagnosticLabel(manifest, completedCue, next)
                + " Revisa que el WAV exista o regenera los fragmentos de audio.");
        refreshProjectState();
    }
    private boolean transitionToCue(PlaybackCue cue) {
        if (cue == null) {
            return false;
        }
        playbackTransport.stopPlayerAndContinuation();
        playbackTransport.activateRuntimeCue(cue);
        PlaybackCursor nextCursor = new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false);
        selectedScriptSegmentId.set(nextCursor.segmentId());
        playbackCursor.set(nextCursor);
        if (!playExactCue(cue, 0.0)) { return false; }
        playbackTimer.play();
        return true;
    }
    private void startSequentialPlayback(PlaybackManifest manifest, PlaybackCue cue, boolean bufferedStart) {
        if (manifest == null || manifest.emptyManifest() || cue == null) { statusMessage.set("No hay cola de lectura reproducible para continuar."); refreshProjectState(); return; }
        documentPlaybackRequested = true; playSingleCueOnly = false; waitingForBufferedSegmentAfter = ""; activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        playbackTransport.startSequentialPlayback(manifest, cue, playbackRate.get(), this::startCueFromSequentialQueue, this::completeSequentialPlayback);
        statusMessage.set(bufferedStart ? "Audio inicial listo. La cola secuencial continuará mientras DocuPodcast prepara los siguientes fragmentos." : "Reproduciendo desde cola secuencial: " + cue.unitId() + ".");
        refreshProjectState();
    }
    private boolean startCueFromSequentialQueue(PlaybackCue cue) {
        if (cue == null) { return false; }
        lastSequentialCueUnitId = cue.unitId();
        recordPlaybackEvent("sequential-cue-start", cue, "queue-start");
        PlaybackCursor cursor = new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false); selectedScriptSegmentId.set(cue.segmentId()); playbackCursor.set(cursor);
        boolean started = playExactCue(cue, 0.0);
        if (started) { playbackTimer.play(); statusMessage.set("Continuando lectura secuencial exacta: " + cue.unitId() + ". " + playbackRuntimeDiagnosticLabel(currentPlaybackManifest.get(), cue, playbackTransport.nextRuntimeCueAfter(cue))); refreshProjectState(); }
        return started;
    }
    private void completeSequentialPlayback() {
        if (playSingleCueOnly) {
            playSingleCueOnly = false;
            resetPlaybackTransportOnly();
            statusMessage.set("Fragmento reproducido.");
            refreshProjectState();
            return;
        }
        String completedUnitId = lastSequentialCueUnitId;
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        if (!completedUnitId.isBlank() && manifest != null && !manifest.emptyManifest()) {
            playbackTransport.refreshRuntimeQueue(manifest);
            Optional<PlaybackCue> next = manifest.nextCueAfterUnit(completedUnitId);
            if (next.isPresent()) {
                playbackTransport.startRuntimeQueue(manifest, next.get());
                startSequentialPlayback(manifest, next.get(), true);
                statusMessage.set("Manifest actualizado. Continuando lectura secuencial: "
                        + completedUnitId + " -> " + next.get().unitId() + ".");
                refreshProjectState();
                return;
            }
        }
        if (audioJobRunning.get() && !completedUnitId.isBlank()) {
            PlaybackManifest current = currentPlaybackManifest.get();
            Optional<PlaybackCue> completedCue = manifest == null ? Optional.empty() : manifest.cueForUnit(completedUnitId);
            if (completedCue.isEmpty() && current != null) {
                completedCue = current.cueForUnit(completedUnitId);
            }
            completedCue.ifPresent(this::waitForBufferedContinuation);
            if (completedCue.isPresent()) {
                return;
            }
        }
        resetPlaybackTransportOnly();
        documentPlaybackRequested = false;
        waitingForBufferedSegmentAfter = "";
        lastSequentialCueUnitId = "";
        statusMessage.set("Playback completado por cola secuencial.");
        refreshProjectState();
    }
    private void waitForBufferedContinuation(PlaybackCue completedCue) {
        playbackTransport.stopCueMonitoring();
        playbackTimer.stop(); playbackTransport.stopPlayerAndContinuation();
        waitingForBufferedSegmentAfter = completedCue.unitId(); documentPlaybackRequested = true;
        selectedScriptSegmentId.set(completedCue.segmentId()); playbackCursor.set(new PlaybackCursor(completedCue.segmentId(), completedCue.endSeconds(), true));
        statusMessage.set(playbackWorkflow.waitingForBufferMessage(playbackBufferPolicy, "")
                + " · " + playbackRuntimeDiagnosticLabel(currentPlaybackManifest.get(), completedCue, Optional.empty()));
        refreshProjectState();
    }
    private Optional<PlaybackCue> cueForCursor(PlaybackManifest manifest, PlaybackCursor cursor) { return playbackTransport.cueForCursor(manifest, cursor); }
    private boolean playCueForCursor(PlaybackCursor cursor) {
        PlaybackManifest manifest = currentPlaybackManifest.get();
        if (manifest == null || manifest.emptyManifest() || cursor == null || cursor.segmentId().isBlank()) { return false; }
        Optional<PlaybackCue> cue = cueForCursor(manifest, cursor);
        if (cue.isEmpty()) { PlaybackManifest rebuiltManifest = rebuildPlaybackManifestFromLatestJob(); if (rebuiltManifest != null && !rebuiltManifest.emptyManifest()) { currentPlaybackManifest.set(rebuiltManifest); cue = cueForCursor(rebuiltManifest, cursor); } }
        if (cue.isEmpty()) { return false; }
        double localOffset = cue.get().relativePosition(cursor.positionSeconds());
        return playExactCue(cue.get(), localOffset);
    }
    private boolean playExactCue(PlaybackCue cue, double localOffset) {
        if (cue == null) { return false; }
        selectedScriptSegmentId.set(cue.segmentId());
        PlaybackTransportCoordinator.PlaybackTransportCommandResult result = playbackTransport.playProjectCue(
                currentProjectFile().orElse(null), currentPlaybackManifest.get(), cue, localOffset, playbackRate.get(), this::playbackPausedOrInactive, this::advanceAfterCompletedCue);
        recordPlaybackEvent(result.started() ? "cue-play-start" : "cue-play-failed", cue, result.diagnosticReason());
        if (result.started()) { syncTheatreAudioPlayback(cue.startSeconds() + Math.max(0.0, localOffset)); return true; }
        if (result.playerFailure()) { playbackTransport.stopPlayerAndContinuation(); }
        statusMessage.set(result.message());
        return false;
    }
    public void deleteAllPersistedAudioChunks() {
        Optional<Path> projectDirectory = currentProjectDirectory();
        if (projectDirectory.isEmpty()) { statusMessage.set("Guarda el proyecto antes de eliminar chunks de audio."); refreshProjectState(); return; }
        deletePersistedAudioAsync(projectDirectory.get(), "Chunks de audio eliminados. Puedes reconstruirlos desde la barra de estado.");
    }
    private void invalidatePersistedAudioForNarrationChange() { currentProjectDirectory().ifPresentOrElse(dir -> deletePersistedAudioAsync(dir, "Audio anterior invalidado."), () -> { resetPlaybackState(); activeAudioJobStatus.set(AudioJobStatusDto.idle()); audioJobRunning.set(false); }); }
    private void submitFreshAudioRequestAsync(Path directory, AudioGenerationRequest request, String successPrefix, String engine) { resetPlaybackState(); audioJobRunning.set(true); statusMessage.set("Esperando la terminación segura del trabajo anterior..."); audioWorkflow.replaceAndSubmitAsync(directory, activeAudioJobStatus.get(), updated -> Platform.runLater(() -> activeAudioJobStatus.set(updated)), request, this::acceptAudioStatus).whenComplete((jobId, failure) -> Platform.runLater(() -> { if (failure == null) statusMessage.set(successPrefix + ": " + jobId + ". Motor: " + engine + "."); else { audioJobRunning.set(false); statusMessage.set("No se pudo preparar el nuevo render de audio: " + rootCauseMessage(failure)); } refreshProjectState(); })); }
    private void deletePersistedAudioAsync(Path directory, String successMessage) { resetPlaybackState(); statusMessage.set("Cancelando y esperando el trabajo de audio activo..."); audioWorkflow.prepareFreshWorkspaceAsync(directory, activeAudioJobStatus.get(), updated -> Platform.runLater(() -> activeAudioJobStatus.set(updated))).whenComplete((ignored, failure) -> Platform.runLater(() -> { audioJobRunning.set(false); activeAudioJobStatus.set(AudioJobStatusDto.idle()); if (failure == null) { bumpDocumentMediaRevision(); refreshStreamingBufferStatus(); statusMessage.set(successMessage); } else statusMessage.set("No se pudieron eliminar los chunks de audio: " + rootCauseMessage(failure)); refreshProjectState(); })); }
    private static String rootCauseMessage(Throwable failure) { Throwable current = failure; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
    private void resetPlaybackState() { documentPlaybackRequested = false; playSingleCueOnly = false; waitingForBufferedSegmentAfter = ""; lastSequentialCueUnitId = ""; resetPlaybackTransportOnly(); playbackTransport.resetRuntimeQueue(); currentPlaybackManifest.set(PlaybackManifest.empty()); }
    public boolean playbackActiveForFullscreenPause() { PlaybackCursor current = playbackCursor.get(); return current != null && current.playing() || playbackTransport.playerPlaying() || playbackTransport.continuationActive() || playbackTransport.sequentialActive(); }
    private boolean playbackPausedOrInactive() { PlaybackCursor current = playbackCursor.get(); return current == null || current.paused(); }
    private void resetPlaybackTransportOnly() { playSingleCueOnly = false; playbackTransport.stopTransport(); theatreAudioPlayback.stop(); playbackTimer.stop(); playbackCursor.set(PlaybackCursor.stopped()); } private void syncTheatreAudioPlayback(double position) { theatreAudioPlayback.sync(currentProjectDirectory().orElse(null), theatreAudioTrackTimeline(), position, playbackRate.get()); }
    public void exportPodcastWav(Path targetFile) throws IOException {
        Path projectDirectory = requireProjectDirectory();
        PlaybackManifest manifest = ensurePlaybackManifestLoaded();
        statusMessage.set(exportWorkflow.exportPodcastWav(projectDirectory, manifest, targetFile, playbackRate.get()));
    }
    public void exportDiagnosticReport(Path targetFile) throws IOException {
        statusMessage.set(exportWorkflow.exportDiagnosticReport(requireSession(), currentScript.get(), currentStoryboard.get(), listPersistedJobsSafely(), targetFile));
    }
    public void exportProjectBundle(Path targetDirectory) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar un paquete portable."));
        statusMessage.set(exportWorkflow.exportProjectBundle(session, projectFile, currentScript.get(), currentStoryboard.get(), listPersistedJobsSafely(), targetDirectory, this::saveCurrentProjectAs));
    }
    public void exportSimpleVideoPackage(Path targetDirectory) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video simple."));
        statusMessage.set(exportWorkflow.exportSimpleVideoPackage(session, projectFile, currentScript.get(), currentStoryboard.get(), listPersistedJobsSafely(), targetDirectory, loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), this::saveCurrentProjectAs));
    }
    public void exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution) throws IOException { exportFinalVideo(targetFile, resolution, 30, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.AUTO, ignored -> { }, () -> false); }
    public void exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException {
        exportFinalVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), progress, cancellationRequested);
    }
    public void exportDocumentStudyTextAudioVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportDocumentStudyTextAudioVideo(targetFile, resolution, framesPerSecond, encoderPolicy, DocumentTextVideoOptions.defaults().withResolution(resolution), progress, cancellationRequested); }
    public void exportDocumentStudyTextAudioVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, DocumentTextVideoOptions textOptions, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { ProjectSession session = requireSession(); Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video documental texto+audio.")); String result = exportWorkflow.exportDocumentStudyTextAudioVideo(session, projectFile, currentDocument.get(), currentScript.get(), listPersistedJobsSafely(), targetFile, resolution, framesPerSecond, encoderPolicy, textOptions, this::saveCurrentProjectAs, progress, cancellationRequested); Platform.runLater(() -> statusMessage.set(result)); }
    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreWorkVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), false, progress, cancellationRequested); }
    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreWorkVideo(targetFile, resolution, framesPerSecond, encoderPolicy, scope, false, progress, cancellationRequested); }
    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, boolean renderUnassignedVisuals, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreWorkVideo(targetFile, resolution, framesPerSecond, encoderPolicy, scope, renderUnassignedVisuals, false, progress, cancellationRequested); }
    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, boolean renderUnassignedVisuals, boolean includeInferredFrames, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { ProjectSession session = requireSession(); Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video teatral limpio.")); NarrationScriptDocument scopedScript = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope); String result = exportWorkflow.exportTheatreWork(session, projectFile, scopedScript, listPersistedJobsSafely(), targetFile, resolution, framesPerSecond, encoderPolicy, loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), currentStoryboard.get(), renderUnassignedVisuals, includeInferredFrames, ignored -> { }, progress, cancellationRequested); Platform.runLater(() -> statusMessage.set(result)); }
    public List<String> missingTheatreCleanVisualSegmentIds(TheatreExportScope scope) { ProjectSession session = requireSession(); NarrationScriptDocument script = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope); return new com.marcosmoreiradev.docupodcaststudio.application.video.BuildTheatreCleanVideoPlanUseCase().missingVisualSegmentIds(session.project(), script, currentStoryboard.get(), session.projectFile().map(Path::getParent).orElse(null)); }
    public void exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video."));
        NarrationScriptDocument scopedScript = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope);
        String exportResult = exportWorkflow.exportFinalVideo(session, projectFile, scopedScript, currentStoryboard.get(), listPersistedJobsSafely(), targetFile, resolution, framesPerSecond, encoderPolicy, loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), ignored -> { }, progress, cancellationRequested);
        Platform.runLater(() -> statusMessage.set(exportResult));
    }
    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreSpatialVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), companionMode, progress, cancellationRequested); }
    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreSpatialVideo(targetFile, resolution, framesPerSecond, encoderPolicy, scope, companionMode, false, progress, cancellationRequested); }
    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, boolean includeInferredFrames, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreSpatialVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), companionMode, includeInferredFrames, progress, cancellationRequested); }
    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, boolean includeInferredFrames, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video mapa."));
        NarrationScriptDocument scopedScript = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope);
        String exportResult = exportWorkflow.exportTheatreSpatialVideo(session, projectFile, scopedScript,
                listPersistedJobsSafely(), targetFile, resolution, framesPerSecond, encoderPolicy,
                loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), currentStoryboard.get(),
                companionMode.frameMode(), scope, includeInferredFrames, ignored -> { }, progress,
                cancellationRequested);
        Platform.runLater(() -> statusMessage.set(exportResult));
    }
    private OperationalSettings loadOperationalSettingsSafely() {
        try {
            return applicationServices.settings().loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }
    private java.util.List<AudioJobSnapshot> listPersistedJobsSafely() {
        Optional<Path> file = currentProjectFile();
        try { return file.isEmpty() ? java.util.List.of() : applicationServices.audio().listPersistedAudioJobs().list(file.get().toAbsolutePath().normalize().getParent()); }
        catch (IOException ex) { return java.util.List.of(); }
    }
    public boolean hasAllChunksRendered() {
        Path projectDirectory = currentProjectFile().map(path -> path.toAbsolutePath().normalize().getParent()).orElse(null);
        return AudioRenderCoverage.hasAllChunksRendered(currentScript.get(), listPersistedJobsSafely(), projectDirectory);
    }
    public boolean hasChunksRenderedForTheatreScope(TheatreExportScope scope) {
        Path projectDirectory = currentProjectFile().map(path -> path.toAbsolutePath().normalize().getParent()).orElse(null);
        ProjectSession session = sessions.activeSession().orElse(null);
        return AudioRenderCoverage.hasAllChunksRendered(theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope), listPersistedJobsSafely(), projectDirectory);
    }
    public void clearSelectedDocumentBlock() {
        selectedDocumentBlockId.set(""); selectedScriptSegmentId.set(""); clearVisualFragmentSelection(); clearDocumentTextRange();
        statusMessage.set("Selección de fragmento limpia."); refreshProjectState();
    }
    public Optional<UserVisibleDecision> inspectProjectIntegrityDecision() {
        try {
            Optional<ProjectSession> session = sessions.activeSession(); Optional<Path> file = currentProjectFile();
            if (session.isEmpty() || file.isEmpty()) { throw new IOException("Guarda el proyecto antes de validar su integridad."); }
            ProjectWorkspaceHydration hydration = new ProjectWorkspaceHydration(session.get().importedDocument(), session.get().narrationScript(), session.get().storyboard());
            ProjectIntegrityInspectionOutcome outcome = projectWorkflow.inspectIntegrity(session.get(), file.get(), hydration, listPersistedJobsSafely());
            statusMessage.set(outcome.statusMessage());
            return outcome.decision();
        } catch (IOException | RuntimeException ex) {
            return Optional.of(UserVisibleDecision.error("No se pudo validar la integridad del proyecto", ex.getMessage(), ex.toString()));
        }
    }
    public void inspectProjectIntegrityForStatus() {
        inspectProjectIntegrityDecision().ifPresent(value -> statusMessage.set(value.headline() + ": " + value.message()));
    }
    public Optional<UserVisibleDecision> inspectExportReadinessDecision() {
        Optional<ProjectSession> session = sessions.activeSession();
        if (session.isEmpty()) { UserVisibleDecision decision = UserVisibleDecision.warning("No se puede revisar exportación", "Abre o crea un proyecto antes de revisar exportaciones."); statusMessage.set(decision.headline() + ": " + decision.message()); return Optional.of(decision); }
        try {
            var report = applicationServices.export().inspectExportReadiness().inspect(session.get().project(), currentProjectFile().orElse(null), currentScript.get(), currentStoryboard.get(), listPersistedJobsSafely());
            statusMessage.set("Estado de exportación: " + report.status().displayName() + " · exportables " + report.exportableCount() + " · bloqueadas " + report.blockedCount() + ".");
            if (!report.hasBlockedOutput()) return Optional.empty();
            StringBuilder detail = new StringBuilder();
            for (var item : report.items()) { if (item.blocked()) detail.append(item.kind().displayName()).append(": ").append(String.join(", ", item.missingRequirements())).append(System.lineSeparator()); }
            return Optional.of(UserVisibleDecision.warning("Exportación con bloqueos", "Hay salidas bloqueadas. Revisa los faltantes antes de exportar. " + detail.toString().strip()));
        } catch (RuntimeException ex) { UserVisibleDecision decision = UserVisibleDecision.error("No se pudo revisar el estado de exportación", ex.getMessage(), ex.toString()); statusMessage.set(decision.headline() + ": " + decision.message()); return Optional.of(decision); }
    }
    public void inspectExportReadinessForStatus() {
        inspectExportReadinessDecision().ifPresent(value -> statusMessage.set(value.headline() + ": " + value.message()));
    }
    private Path requireProjectDirectory() throws IOException {
        Path projectFile = currentProjectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar."));
        Path parent = projectFile.toAbsolutePath().normalize().getParent();
        if (parent == null) throw new IOException("No se pudo resolver la carpeta del proyecto.");
        return parent;
    }
    public void showWelcome() { navigateToWorkspace(WorkspaceKind.WELCOME_HOME); statusMessage.set("Pantalla de inicio activa."); refreshProjectState(); }
    public void showPlaceholder(WorkspaceKind kind) {
        WorkspaceKind workspace = kind == null ? WorkspaceKind.WELCOME_HOME : kind;
        WorkspaceKind resolved = navigateToWorkspace(workspace);
        statusMessage.set("Vista activa: " + resolved.displayName() + "."); refreshProjectState();
    }
    private WorkspaceKind navigateToWorkspace(WorkspaceKind workspace) {
        WorkspaceKind resolved = workspaceNavigation.rememberWorkspace(workspace); activeWorkspace.set(resolved);
        return resolved;
    }
    public void requestClose(WindowEvent event) {}
    private ProjectSession requireSession() { return sessions.activeSession().orElseThrow(() -> new IllegalStateException("No hay proyecto activo")); }
    private void clearDocumentTextRange() {
        selectedDocumentTextRange.set(null);
        applySelectionLabels(documentSelectionWorkflow.cleared(selectedDocumentBlockId.get(), currentDocument.get()));
    }
    private void applySelectionLabels(DocumentSelectionCoordinator.SelectionLabels labels) {
        selectedDocumentTextPreview = labels.preview();
        selectedDocumentRangeLabel.set(labels.rangeLabel());
        selectedDocumentSourceLocation.set(labels.sourceLocation());
    }
    private StreamingPlaybackWindow streamingPlaybackWindow(AudioJobStatusDto status, PlaybackManifest manifest) {
        return StreamingPlaybackWindow.of(playbackBufferPolicy, status == null ? 0 : status.completedSegments(), status == null ? 0 : status.totalSegments(), currentPlaybackIndex(manifest));
    }
    private int currentPlaybackIndex(PlaybackManifest manifest) {
        if (manifest == null || manifest.emptyManifest()) {
            return 0;
        }
        String currentSegment = playbackCursor.get() == null ? "" : playbackCursor.get().segmentId();
        if (currentSegment == null || currentSegment.isBlank()) {
            currentSegment = selectedScriptSegmentId.get();
        }
        if (currentSegment == null || currentSegment.isBlank()) {
            return 0;
        }
        for (int i = 0; i < manifest.cues().size(); i++) {
            if (manifest.cues().get(i).segmentId().equals(currentSegment)) {
                return i;
            }
        }
        return 0;
    }
    private void refreshStreamingBufferStatus() {
        AudioJobStatusDto status = activeAudioJobStatus.get();
        if (currentDocument.get() == null) {
            streamingBufferStatus.set("Buffer: esperando documento.");
            return;
        }
        if (status == null || status.totalSegments() <= 0) {
            streamingBufferStatus.set(playbackBufferPolicy.userLabel());
            return;
        }
        streamingBufferStatus.set(streamingPlaybackWindow(status, currentPlaybackManifest.get()).readerStatusLabel());
    }
    private void refreshDocumentListenFlow() {
        ListeningSessionState state = documentNarration.listeningSession(
                currentDocument.get(),
                currentScript.get(),
                currentPlaybackManifest.get(),
                audioJobRunning.get(),
                currentProjectFile().isPresent(),
                activeAudioJobStatus.get(),
                playbackBufferPolicy).state();
        documentListenFlowTitle.set(state.title());
        documentListenFlowDetail.set(state.detail());
    }
    private void refreshDocumentPrimaryAction() {
        DocumentSelectionCoordinator.PrimaryAction action = documentSelectionWorkflow.primaryAction(
                currentDocument.get(), selectedDocumentTextRange.get(), selectedDocumentBlockId.get());
        documentPrimaryActionLabel.set(action.label());
        documentPrimaryActionHint.set(action.hint());
    }
    private void refreshPlaybackNavigationAvailability() {
        PlaybackManifest manifest = currentPlaybackManifest.get(); Optional<NarrationSegment> selected = selectedDocumentSegmentOrSelected();
        previousFragmentAvailable.set(playbackNavigator.target(manifest, playbackCursor.get(), selected, -1).ready()); nextFragmentAvailable.set(playbackNavigator.target(manifest, playbackCursor.get(), selected, 1).ready());
    }
    private void refreshProjectState() {
        refreshDocumentPrimaryAction();
        refreshStreamingBufferStatus();
        refreshDocumentListenFlow();
        refreshPlaybackNavigationAvailability();
        boolean hasProject = sessions.hasActiveSession();
        projectOpen.set(hasProject);
        dirty.set(sessions.dirty());
        saveableProjectOpen.set(sessions.saveable());
        if (!hasProject) {
            currentProjectMode.set(ProjectMode.defaultMode());
            windowTitle.set("DocuPodcast Studio — Inicio");
            return;
        }
        ProjectSession session = requireSession(); currentProjectMode.set(new com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy().resolve(session.project()));
        String suffix = session.dirty() ? " *" : "";
        String fileLabel = session.projectFile()
                .map(path -> " — " + path.getFileName())
                .orElse(" — sin guardar");
        windowTitle.set("DocuPodcast Studio — " + session.title() + fileLabel + suffix);
    }

}
