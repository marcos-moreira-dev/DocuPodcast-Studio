package com.marcosmoreiradev.docupodcaststudio.presentation.shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.LocalResourceScheduler;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioInputDevice;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleVisualBindingDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.recording.RecordingActionPlan;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneReferenceResolution;
import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineAvailability;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ReusableAudioCoverage;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioCoverageSnapshotAssembler;
import com.marcosmoreiradev.docupodcaststudio.application.audio.PriorityAudioPlaybackPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.media.PreparedAudioAsset;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContext;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContextCompiler;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobSnapshotMapper;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentListenPhase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationProgress;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolveDocumentProcessingSelectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvedDocumentProcessingSelection;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentProcessingIntervalPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.document.FilterDocumentContentProjectionBySelectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentInteractionProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.WordDocumentInteractionProjectionAdapter;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDocumentInteractionProjectionAdapter;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.ResolveDocumentStudyVideoNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.SourceDocumentRefreshDecisionFactory;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentListenPlan;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.ReconcileDocumentStudyVideoConfigurationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.ReadingProfilePreview;
import com.marcosmoreiradev.docupodcaststudio.application.reading.AdaptNarrationLanguageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.NarrationTranslationException;
import com.marcosmoreiradev.docupodcaststudio.application.reading.NarrationTranslationCache;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPackageExportResult;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreStageGeometry;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectWorkspaceHydration;
import com.marcosmoreiradev.docupodcaststudio.application.project.ReconcileGeneratedAudioJobAssetsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.DocumentAudioPreparationExtent;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingIntervalUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.StreamingPlaybackWindow;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextReference;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextRole;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectVisualProcessingSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.PdfListeningMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
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
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreExperienceController;
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
import com.marcosmoreiradev.docupodcaststudio.presentation.playback.PlaybackController;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentFragmentRailPresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentExperienceController;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentRailImagePresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentRailProjectionFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.documentary.DocumentaryExperienceController;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentVisualFragmentKey;
import com.marcosmoreiradev.docupodcaststudio.application.document.ListeningSessionState;
import com.marcosmoreiradev.docupodcaststudio.presentation.storyboard.StoryboardScenePresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportController;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.PreparedExportIntent;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.narrative.NarrativeExperienceController;
import com.marcosmoreiradev.docupodcaststudio.presentation.project.ProjectLifecycleController;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProviderRegistry;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import com.marcosmoreiradev.docupodcaststudio.application.image.*;
import com.marcosmoreiradev.docupodcaststudio.application.artifacts.ProjectArtifactStore;
import com.marcosmoreiradev.docupodcaststudio.application.media.GenerationJobRepositoryProvider;
public final class DocuPodcastShellViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DocuPodcastShellViewModel.class);
    public static final int MIN_READING_FONT_SIZE = 14;
    public static final int DEFAULT_READING_FONT_SIZE = 18;
    public static final int MAX_READING_FONT_SIZE = 28;
    private final WorkspaceApplicationServices workspaceServices;
    private final MediaEnginePlatform mediaEngines;
    private final InkInputProviderRegistry inkInputProviders;
    private final DrawingFeatureCatalog drawingFeatures;
    private final MediaCapabilityService mediaCapabilities;
    private final AdaptNarrationLanguageUseCase adaptNarrationLanguage;
    private final ProjectSessionCoordinator sessions = new ProjectSessionCoordinator();
    private final ProjectLifecycleController projectController;
    private final ReconcileGeneratedAudioJobAssetsUseCase generatedAudioAssetReconciliation =
            new ReconcileGeneratedAudioJobAssetsUseCase();
    private final DocumentIntakeCoordinator documentIntake;
    private final SourceDocumentRefreshCoordinator sourceDocumentRefresh;
    private final WorkspaceNavigationCoordinator workspaceNavigation;
    private final DocumentExperienceController documentController;
    private final PlaybackController playbackController;
    private final PlaybackFragmentNavigator playbackNavigator = new PlaybackFragmentNavigator();
    private final AudioWorkflowCoordinator audioWorkflow;
    private final ExportController exportController;
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
    private final TheatreExperienceController theatreController;
    private final DocumentaryExperienceController documentaryController = new DocumentaryExperienceController();
    private final NarrativeVideoAssetWorkflow narrativeVideoAssetWorkflow = new NarrativeVideoAssetWorkflow();
    private final NarrativeDocumentContextCompiler narrativeDocumentContextCompiler =
            new NarrativeDocumentContextCompiler();
    private final NarrativeExperienceController narrativeController;
    private final NarrativeImageGenerationWorkflow narrativeImageGenerationWorkflow;
    private final TheatreFrameGenerationWorkflow theatreFrameGenerationWorkflow;
    private final TheatreImageAssetWorkflow theatreImageAssetWorkflow;
    private final ProjectImageManagementWorkflow projectImageWorkflow;
    private final DocumentPlaybackSelectionResolver playbackSelectionResolver = new DocumentPlaybackSelectionResolver();
    private final PlayableAudioJobSelector playableAudioJobSelector = new PlayableAudioJobSelector();
    private final ReusableAudioCoverage reusableAudioCoverage = new ReusableAudioCoverage();
    private final AudioCoverageSnapshotAssembler audioCoverageSnapshotAssembler =
            new AudioCoverageSnapshotAssembler();
    private final ResolveDocumentProcessingSelectionUseCase resolveDocumentProcessingSelection =
            new ResolveDocumentProcessingSelectionUseCase();
    private final DocumentProcessingIntervalPolicy documentProcessingIntervalPolicy =
            new DocumentProcessingIntervalPolicy();
    private final FilterDocumentContentProjectionBySelectionUseCase
            filterDocumentContentProjectionBySelection =
            new FilterDocumentContentProjectionBySelectionUseCase();
    private final ResolveDocumentStudyVideoNarrationUseCase
            resolveDocumentStudyVideoNarration =
            new ResolveDocumentStudyVideoNarrationUseCase();
    private final WordDocumentInteractionProjectionAdapter wordInteractionAdapter =
            new WordDocumentInteractionProjectionAdapter();
    private final PdfDocumentInteractionProjectionAdapter pdfInteractionAdapter =
            new PdfDocumentInteractionProjectionAdapter();
    private final PriorityAudioPlaybackPolicy priorityAudioPlaybackPolicy =
            new PriorityAudioPlaybackPolicy();
    private final StudioSessionStore sessionStore = new StudioSessionStore();
    private final ReadOnlyStringWrapper windowTitle = sessionStore.windowTitleState();
    private final ObjectProperty<WorkspaceKind> activeWorkspace = sessionStore.activeWorkspaceState();
    private final ObjectProperty<ReadableDocument> currentDocument = new SimpleObjectProperty<>();
    private final ObjectProperty<PreparedPdfSource> currentPreparedPdfSource = new SimpleObjectProperty<>();
    private final ObjectProperty<NarrationScriptDocument> currentScript = new SimpleObjectProperty<>();
    private final ObjectProperty<StoryboardDocument> currentStoryboard = new SimpleObjectProperty<>();
    private final StringProperty selectedScriptSegmentId = sessionStore.selectedScriptSegmentIdState();
    private final StringProperty selectedDocumentBlockId = sessionStore.selectedDocumentBlockIdState();
    private final DoubleProperty pdfVisualDocumentProgress = new SimpleDoubleProperty(0.0);
    private final IntegerProperty pdfVisiblePageNumber = new SimpleIntegerProperty(0);
    private final ObjectProperty<PdfPreparationProgress> pdfPreparationProgress =
            new SimpleObjectProperty<>(new PdfPreparationProgress(
                    PdfPreparationProgress.State.IDLE, 0, 0, 0, 0, ""));
    private final BooleanProperty localDocumentAnalysisRunning =
            new SimpleBooleanProperty(false);
    private final StringProperty localDocumentAnalysisTitle =
            new SimpleStringProperty("");
    private final StringProperty localDocumentAnalysisDetail =
            new SimpleStringProperty("");
    private final DoubleProperty localDocumentAnalysisProgress =
            new SimpleDoubleProperty(-1.0);
    private final StringProperty localDocumentAnalysisFooter =
            new SimpleStringProperty("");
    private final ObjectProperty<DocumentTextRange> selectedDocumentTextRange = sessionStore.selectedDocumentTextRangeState();
    private final ObjectProperty<com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef>
            selectedPdfRegion = sessionStore.selectedPdfRegionState();
    private final ObjectProperty<com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget>
            selectedPdfVisualTarget = new SimpleObjectProperty<>();
    private final StringProperty requestedPdfRegionReviewId = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper selectedDocumentRangeLabel = new ReadOnlyStringWrapper("Sin texto seleccionado");
    private final ReadOnlyStringWrapper selectedDocumentSourceLocation = new ReadOnlyStringWrapper("Ubicación fuente: sin selección.");
    private String selectedDocumentTextPreview = "";
    private final StringProperty selectedVisualFragmentSegmentId = new SimpleStringProperty("");
    private final StringProperty selectedVisualFragmentImageUri = new SimpleStringProperty("");
    private final ObjectProperty<DocumentVisualFragmentKey> selectedVisualFragmentKey = new SimpleObjectProperty<>(DocumentVisualFragmentKey.empty());
    private final StringProperty lastStoryboardImageAssetId = new SimpleStringProperty("");
    private final IntegerProperty documentMediaRevision = new SimpleIntegerProperty(0);
    /** Row-scoped source-visual changes. Kept separate from the cross-feature media revision. */
    private final ObjectProperty<java.util.Set<String>> documentarySourceVisualChanges =
            new SimpleObjectProperty<>(java.util.Set.of());
    private final ObjectProperty<PlaybackCursor> playbackCursor = new SimpleObjectProperty<>(PlaybackCursor.stopped());
    /** Exact acoustic unit currently handed to the player; authoritative for visual follow-along. */
    private final ObjectProperty<PlaybackCue> activePlaybackCue = new SimpleObjectProperty<>();
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
    private DocumentExportContinuation pendingDocumentExport = null;
    private Consumer<Throwable> pendingDocumentExportFailure;
    private volatile Thread pendingNarrationTranslationWorker;
    private String activeDocumentExportCorrelationId = "";
    private final AtomicBoolean documentExportCancellationRequested = new AtomicBoolean(false);
    private boolean documentExportRenderRunning;
    private long documentExportStartedNanos;
    private final java.util.Set<String> documentarySourceVisualsInFlight =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final int DOCUMENTARY_SOURCE_VISUAL_BATCH_SIZE = 24;
    private final ObjectProperty<AudioJobStatusDto> activeAudioJobStatus = new SimpleObjectProperty<>(AudioJobStatusDto.idle());
    private volatile CompletableFuture<Void> activeAudioMaintenance =
            CompletableFuture.completedFuture(null);
    private volatile CompletableFuture<String> activeAudioSubmission =
            CompletableFuture.completedFuture("");
    private volatile String activeSubmittedAudioJobId = "";
    private static final int MAX_INCREMENTAL_PDF_AUDIO_BACKLOG = 3;
    private final NavigableSet<Integer> incrementalPdfAudioBacklog = new TreeSet<>();
    private final Set<Integer> incrementalPdfObservedPages = new HashSet<>();
    private final Set<Integer> incrementalPdfFailedAudioPages = new HashSet<>();
    private boolean incrementalPdfAudioActive;
    private boolean incrementalPdfTranslationPreparing;
    private boolean incrementalPdfPreparationComplete;
    private boolean incrementalPdfAutoPlay;
    private int incrementalPdfStartPage;
    private int incrementalPdfEndPage;
    private int incrementalPdfActiveAudioPage;
    private String incrementalPdfAudioJobId = "";
    private long incrementalPdfRequestedNanos;
    private long incrementalPdfFirstPageNanos;
    private long incrementalPdfFirstSegmentsNanos;
    private long incrementalPdfFirstAudioNanos;
    private long incrementalPdfFirstPlaybackNanos;
    private final ObjectProperty<ReadingProfile> activeReadingProfile = new SimpleObjectProperty<>(ReadingProfile.academicDefaults());
    private final ObjectProperty<VoiceLibrary> activeVoiceLibrary = new SimpleObjectProperty<>(VoiceLibrary.defaults());
    private final BooleanProperty projectOpen = sessionStore.projectOpenState(), dirty = sessionStore.dirtyState(),
            saveableProjectOpen = new SimpleBooleanProperty(false), audioJobRunning = new SimpleBooleanProperty(false),
            voiceRecordingRunning = new SimpleBooleanProperty(false), manualAudioRecordingRunning = new SimpleBooleanProperty(false),
            documentRightRailVisible = new SimpleBooleanProperty(false), documentPlaybarDocked = new SimpleBooleanProperty(false), technicalProblemPreparationActive = new SimpleBooleanProperty(false),
            theatreRefreshRunning = new SimpleBooleanProperty(false);
    private final BooleanProperty narrativeVisualGenerationRunning = new SimpleBooleanProperty(false);
    private final AtomicBoolean narrativeVisualCancellationRequested = new AtomicBoolean(false);
    private final AtomicBoolean narrationTranslationCancellationRequested =
            new AtomicBoolean(false);
    private final AtomicLong narrationTranslationGeneration = new AtomicLong();
    private final AtomicReference<AtomicBoolean> activeNarrationTranslationCancellation =
            new AtomicReference<>();
    private final AtomicReference<Thread> activeNarrationTranslationThread =
            new AtomicReference<>();
    private final StringProperty focusedTheatreSceneId = new SimpleStringProperty("");
    private final BooleanProperty readAfterColonForNarration = new SimpleBooleanProperty(false);
    private final BooleanProperty documentAudioPortionEnabled =
            new SimpleBooleanProperty(false);
    private final ObjectProperty<DocumentAudioPreparationExtent>
            documentAudioPreparationExtent = new SimpleObjectProperty<>(
                    DocumentAudioPreparationExtent.SHORT_READING);
    private final ObjectProperty<DocumentProcessingScope> documentProcessingScope =
            new SimpleObjectProperty<>(DocumentProcessingScope.FULL_DOCUMENT);
    private final IntegerProperty documentProcessingIntervalStart =
            new SimpleIntegerProperty(1);
    private final IntegerProperty documentProcessingIntervalEnd =
            new SimpleIntegerProperty(1);
    private final ReadOnlyIntegerWrapper documentProcessingPageCount =
            new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyBooleanWrapper documentProcessingIntervalSupported =
            new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper documentProcessingIntervalValid =
            new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper managedAudioChunksAvailable =
            new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<DocumentInteractionProjection>
            documentInteractionProjection = new ReadOnlyObjectWrapper<>(
            DocumentInteractionProjection.none());
    private final ReadOnlyBooleanWrapper documentSelectionValid =
            new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyObjectWrapper<DocumentReadingReadinessSnapshot>
            fullDocumentReadingReadiness = new ReadOnlyObjectWrapper<>(
                    DocumentReadingReadinessSnapshot.unavailable(""));
    private boolean synchronizingDocumentProcessingScope;
    private final IntegerProperty readingFontSize = new SimpleIntegerProperty(DEFAULT_READING_FONT_SIZE);
    private Path activeVoiceRecordingFile;
    private String activeManualAudioSegmentId = "", activeVoiceRecordingVoiceId = "";
    private VoiceReferenceTone activeVoiceRecordingTone = VoiceReferenceTone.NEUTRAL;
    private final ObjectProperty<Path> lastGeneratedVoiceTestPath = new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper generatedVoiceTestStatus = new ReadOnlyStringWrapper("Sin voz de prueba generada.");
    private final ReadOnlyStringWrapper documentVoiceToneStatus = new ReadOnlyStringWrapper("Selecciona una voz y un tono para este fragmento.");
    private final ReadOnlyStringWrapper statusMessage = sessionStore.statusMessageState();
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
    public DocuPodcastShellViewModel(WorkspaceApplicationServices workspaceServices,
                                      InkInputProviderRegistry inkInputProviders,
                                      MediaEnginePlatform mediaEngines,
                                      DrawingFeatureCatalog drawingFeatures,
                                      MediaCapabilityService mediaCapabilities,
                                      GenerationJobRepositoryProvider jobRepositories,
                                      ProjectArtifactStore projectArtifacts,
                                      NarrationTranslationCache narrationTranslationCache) {
        this.workspaceServices = Objects.requireNonNull(workspaceServices, "workspaceServices");
        this.mediaEngines = Objects.requireNonNullElseGet(mediaEngines, MediaEnginePlatform::empty);
        this.inkInputProviders = Objects.requireNonNull(inkInputProviders, "inkInputProviders");
        this.drawingFeatures = Objects.requireNonNull(drawingFeatures, "drawingFeatures");
        this.mediaCapabilities = Objects.requireNonNull(mediaCapabilities, "mediaCapabilities");
        this.adaptNarrationLanguage = new AdaptNarrationLanguageUseCase(
                this.mediaCapabilities, narrationTranslationCache);
        this.projectController = new ProjectLifecycleController(this.workspaceServices, sessions, jobRepositories);
        this.documentIntake = new DocumentIntakeCoordinator(this.workspaceServices);
        this.sourceDocumentRefresh = new SourceDocumentRefreshCoordinator(this.workspaceServices);
        this.workspaceNavigation = new WorkspaceNavigationCoordinator(sessions);
        this.documentController = new DocumentExperienceController(this.workspaceServices);
        this.playbackController = new PlaybackController();
        this.audioWorkflow = new AudioWorkflowCoordinator(this.workspaceServices);
        this.exportController = new ExportController(this.workspaceServices, this.audioWorkflow);
        this.narrativeLayerWorkflow = new NarrativeLayerCoordinator();
        this.voiceSampleWorkflow = new VoiceSampleWorkflowCoordinator(this.workspaceServices);
        this.readingComfortWorkflow = new ReadingComfortCoordinator(this.workspaceServices);
        this.projectImageWorkflow = new ProjectImageManagementWorkflow(this.workspaceServices);
        this.narrativeImageGenerationWorkflow = new NarrativeImageGenerationWorkflow(
                this.workspaceServices.generation().storyboard(), this.mediaCapabilities);
        this.theatreDemoManifestWorkflow = new TheatreDemoManifestWorkflow(this.workspaceServices);
        this.studyProblemWorkflow = new StudyProblemWorkflow(this.workspaceServices.project().documentStudy().studyProblemPdfExporter());
        this.theatreController = new TheatreExperienceController(
                this.workspaceServices, mediaCapabilities);
        this.theatreFrameGenerationWorkflow = new TheatreFrameGenerationWorkflow(
                this.workspaceServices, mediaCapabilities);
        this.theatreImageAssetWorkflow = new TheatreImageAssetWorkflow(
                this.workspaceServices, theatreCharacterImageWorkflow, theatreObjectImageWorkflow);
        this.narrativeController = new NarrativeExperienceController(
                this.workspaceServices.administration().settings().loadOperationalSettings(),
                this.mediaCapabilities,
                this.narrativeVideoAssetWorkflow,
                this.narrativeDocumentContextCompiler,
                projectArtifacts);
        this.playbackTransport = new PlaybackTransportCoordinator(this.workspaceServices.playback().playback().segmentAudioPlayer()); this.theatreAudioPlayback = new TheatreAudioTrackPlaybackCoordinator(this.workspaceServices.playback().playback().backgroundAudioPlayer());
        activeReadingProfile.set(workspaceServices.project().readingProfile().createDefaultProfile().create());
        activeVoiceLibrary.set(workspaceServices.administration().voice().createDefaultVoiceLibrary().create());
        loadReadingComfortSettings();
        playbackTimer = new Timeline(new KeyFrame(Duration.millis(250), event -> tickPlayback()));
        playbackTimer.setCycleCount(Timeline.INDEFINITE);
        playbackTransport.setOnPlaybackFinished(this::handlePlaybackFinishedOnFxThread);
        selectedDocumentBlockId.addListener((obs, oldValue, newValue) -> {
            actualizarFrameActivo();
            refreshDocumentInteractionProjection();
        });
        selectedScriptSegmentId.addListener((obs, oldValue, newValue) ->
                refreshDocumentInteractionProjection());
        selectedPdfRegion.addListener((obs, oldValue, newValue) ->
                refreshDocumentInteractionProjection());
        var pdfScheduler = this.workspaceServices.project().document()
                .pdfPagePreparationScheduler();
        pdfPreparationProgress.set(pdfScheduler.snapshot());
        pdfScheduler.addProgressListener(this::acceptPdfPreparationProgress);
        currentPreparedPdfSource.addListener((obs, previous, current) ->
        {
            resetDocumentProcessingInterval(current);
            refreshFullDocumentReadingReadiness();
            refreshDocumentInteractionProjection();
        });
        currentDocument.addListener((obs, previous, current) -> {
            if (currentPreparedPdfSource.get() == null) {
                resetDocumentProcessingInterval(null);
            }
            refreshFullDocumentReadingReadiness();
            refreshDocumentInteractionProjection();
        });
        currentScript.addListener((obs, previous, current) -> {
            if (currentPreparedPdfSource.get() == null && currentDocument.get() != null) {
                resetDocumentProcessingInterval(null);
            }
            refreshFullDocumentReadingReadiness();
            refreshDocumentInteractionProjection();
        });
        managedAudioChunksAvailable.addListener((obs, previous, current) -> {
            refreshFullDocumentReadingReadiness();
            refreshDocumentInteractionProjection();
        });
        documentProcessingIntervalStart.addListener((obs, oldValue, newValue) ->
                refreshDocumentProcessingIntervalValidity());
        documentProcessingIntervalEnd.addListener((obs, oldValue, newValue) ->
                refreshDocumentProcessingIntervalValidity());
        documentAudioPortionEnabled.addListener((obs, oldValue, newValue) ->
                syncScopeFromLegacyAudioPreparation());
        documentAudioPreparationExtent.addListener((obs, oldValue, newValue) ->
                syncScopeFromLegacyAudioPreparation());
        refreshDocumentInteractionProjection();
    }

    public WorkspaceApplicationServices.ProjectWorkspace projectWorkspace() { return workspaceServices.project(); }
    public WorkspaceApplicationServices.PlaybackWorkspace playbackWorkspace() { return workspaceServices.playback(); }
    public WorkspaceApplicationServices.GenerationWorkspace generationWorkspace() { return workspaceServices.generation(); }
    public WorkspaceApplicationServices.ExportWorkspace exportWorkspace() { return workspaceServices.exports(); }
    public WorkspaceApplicationServices.AdministrationWorkspace administrationWorkspace() {
        return workspaceServices.administration();
    }

    public MediaEnginePlatform mediaEnginePlatform() {
        return mediaEngines;
    }

    public void preferVoiceEngineForCurrentOperation(String engineId) {
        audioWorkflow.preferVoiceEngine(engineId);
    }

    public WorkspaceApplicationServices.RuntimeWorkspace runtimeWorkspace() {
        return workspaceServices.runtime();
    }
    public InkInputProviderRegistry inkInputProviders() { return inkInputProviders; }
    public DrawingFeatureCatalog drawingFeatures() { return drawingFeatures; }
    public StudioSessionStore sessionStore() { return sessionStore; }

    public ReadOnlyStringProperty windowTitleProperty() { return windowTitle.getReadOnlyProperty(); }

    public ObjectProperty<WorkspaceKind> activeWorkspaceProperty() { return activeWorkspace; }

    public ReadOnlyObjectProperty<ReadableDocument> currentDocumentProperty() { return currentDocument; }

    public ReadOnlyObjectProperty<PreparedPdfSource> currentPreparedPdfSourceProperty() {
        return currentPreparedPdfSource;
    }

    public ReadOnlyObjectProperty<NarrationScriptDocument> currentScriptProperty() { return currentScript; }

    public ReadOnlyObjectProperty<StoryboardDocument> currentStoryboardProperty() { return currentStoryboard; }

    public ReadOnlyStringProperty selectedScriptSegmentIdProperty() { return selectedScriptSegmentId; }

    public ReadOnlyStringProperty selectedDocumentBlockIdProperty() { return selectedDocumentBlockId; }

    public ReadOnlyDoubleProperty pdfVisualDocumentProgressProperty() { return pdfVisualDocumentProgress; }

    public ReadOnlyObjectProperty<PdfPreparationProgress> pdfPreparationProgressProperty() {
        return pdfPreparationProgress;
    }

    public ReadOnlyBooleanProperty managedAudioChunksAvailableProperty() {
        return managedAudioChunksAvailable.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<DocumentReadingReadinessSnapshot>
    fullDocumentReadingReadinessProperty() {
        return fullDocumentReadingReadiness.getReadOnlyProperty();
    }

    public void pauseDocumentPreparation() {
        projectWorkspace().document().pdfPagePreparationScheduler().pause();
    }

    public void resumeDocumentPreparation() {
        projectWorkspace().document().pdfPagePreparationScheduler().resume();
    }

    public void cancelPendingDocumentPreparation() {
        projectWorkspace().document().pdfPagePreparationScheduler().cancelAllPending();
    }

    public ReadOnlyBooleanProperty localDocumentAnalysisRunningProperty() {
        return localDocumentAnalysisRunning;
    }

    public ReadOnlyStringProperty localDocumentAnalysisTitleProperty() {
        return localDocumentAnalysisTitle;
    }

    public ReadOnlyStringProperty localDocumentAnalysisDetailProperty() {
        return localDocumentAnalysisDetail;
    }

    public ReadOnlyDoubleProperty localDocumentAnalysisProgressProperty() {
        return localDocumentAnalysisProgress;
    }

    public ReadOnlyStringProperty localDocumentAnalysisFooterProperty() {
        return localDocumentAnalysisFooter;
    }

    public void beginLocalDocumentAnalysis(String title, String detail) {
        Runnable update = () -> {
            localDocumentAnalysisTitle.set(title == null ? "" : title.strip());
            localDocumentAnalysisDetail.set(detail == null ? "" : detail.strip());
            localDocumentAnalysisProgress.set(-1.0);
            localDocumentAnalysisFooter.set("Preparando la operación. Puedes ocultar este panel sin detenerla.");
            localDocumentAnalysisRunning.set(true);
        };
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }

    public void updateLocalDocumentAnalysisProgress(
            int completed, int total, String detail) {
        Runnable update = () -> {
            int safeTotal = Math.max(0, total);
            int safeCompleted = Math.max(0, Math.min(completed, safeTotal));
            localDocumentAnalysisProgress.set(safeTotal == 0
                    ? -1.0 : safeCompleted / (double) safeTotal);
            if (detail != null && !detail.isBlank()) {
                localDocumentAnalysisDetail.set(detail.strip());
            }
        };
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }

    public void updateLocalDocumentAnalysisFooter(String footer) {
        Runnable update = () -> localDocumentAnalysisFooter.set(
                footer == null ? "" : footer.strip());
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }

    public void endLocalDocumentAnalysis() {
        Runnable update = () -> {
            localDocumentAnalysisRunning.set(false);
            localDocumentAnalysisTitle.set("");
            localDocumentAnalysisDetail.set("");
            localDocumentAnalysisProgress.set(-1.0);
            localDocumentAnalysisFooter.set("");
        };
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }

    /** Cancels the active translation request, not merely the PDF preparation UI. */
    public boolean cancelNarrationTranslationAnalysis() {
        AtomicBoolean operationCancellation =
                activeNarrationTranslationCancellation.get();
        Thread worker = activeNarrationTranslationThread.get();
        if (operationCancellation == null || worker == null || !worker.isAlive()) {
            return false;
        }
        narrationTranslationCancellationRequested.set(true);
        operationCancellation.set(true);
        narrationTranslationGeneration.incrementAndGet();
        Runnable update = () -> {
            localDocumentAnalysisTitle.set("Deteniendo analisis");
            localDocumentAnalysisDetail.set(
                    "Cancelando la inferencia activa y liberando sus recursos...");
            localDocumentAnalysisProgress.set(-1.0);
            statusMessage.set("Cancelacion de traduccion solicitada.");
        };
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
        worker.interrupt();
        return true;
    }

    public void beginDocumentExportRender() {
        documentExportCancellationRequested.set(false);
        documentExportRenderRunning = true;
        documentExportStartedNanos = System.nanoTime();
        beginLocalDocumentAnalysis("Preparando video", "Construyendo el plan de exportación.");
    }

    public void acceptDocumentExportProgress(VideoRenderProgress progress) {
        VideoRenderProgress safe = progress == null ? VideoRenderProgress.idle() : progress;
        Runnable update = () -> {
            localDocumentAnalysisTitle.set(safe.stage().label());
            localDocumentAnalysisDetail.set(safe.currentStep());
            localDocumentAnalysisProgress.set(safe.determinateProgress()
                    ? safe.ratio() : -1.0);
            localDocumentAnalysisFooter.set(videoProgressFooter(safe,
                    documentExportStartedNanos));
        };
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }

    private static String videoProgressFooter(VideoRenderProgress progress,
                                              long startedNanos) {
        String phase = switch (progress.stage()) {
            case PREPARING -> "Organizando audio, imágenes y duración del video.";
            case PLANNING_ILLUSTRATION -> "La IA general prepara una descripción visual para esta intervención.";
            case GENERATING_ILLUSTRATION -> "El motor de imágenes puede cargar modelos y generar usando GPU y RAM. El tiempo mostrado corresponde a toda la exportación.";
            case REVIEWING_ILLUSTRATION -> "La IA general comprueba la ilustración antes de incorporarla al video.";
            case BUILDING_FRAMES -> "Creando recursos visuales temporales para las unidades del video.";
            case RENDERING_WITH_FFMPEG -> "Codificando y comprimiendo el MP4. El trabajo puede cancelarse manualmente.";
            case ASSEMBLING_FINAL -> "Uniendo la línea de tiempo completa; en documentos largos esta fase puede tardar.";
            case MIXING_TRACKS -> "Mezclando audio y video sin regenerar las voces ya vigentes.";
            case VERIFYING_OUTPUT -> "Comprobando que el archivo final exista y sea reproducible.";
            case COMPLETED -> "Exportación terminada.";
            case CANCELLED -> "Exportación cancelada; los derivados válidos se conservan.";
            case FAILED -> "La exportación falló; consulta el detalle técnico.";
            case IDLE -> "Sin exportación activa.";
        };
        if (startedNanos <= 0L) return phase;
        long elapsed = Math.max(0L,
                (System.nanoTime() - startedNanos) / 1_000_000_000L);
        String timing = " Tiempo transcurrido: " + compactDuration(elapsed) + ".";
        if (!progress.determinateProgress() || progress.completedFrames() <= 0) {
            return phase + timing;
        }
        long remaining = Math.max(0L, Math.round(elapsed
                * (progress.totalFrames() - progress.completedFrames())
                / (double) progress.completedFrames()));
        return phase + timing + " Estimación restante: "
                + compactDuration(remaining) + ".";
    }

    private static String compactDuration(long seconds) {
        long safe = Math.max(0L, seconds);
        long hours = safe / 3600;
        long minutes = (safe % 3600) / 60;
        long remainder = safe % 60;
        if (hours > 0) return hours + " h " + minutes + " min";
        if (minutes > 0) return minutes + " min " + remainder + " s";
        return remainder + " s";
    }

    public boolean documentExportCancellationRequested() {
        return documentExportCancellationRequested.get();
    }

    public boolean documentExportWorkflowActive() {
        return pendingDocumentExport != null || documentExportRenderRunning;
    }

    public void endDocumentExportRender() {
        documentExportRenderRunning = false;
        documentExportStartedNanos = 0L;
        activeDocumentExportCorrelationId = "";
        endLocalDocumentAnalysis();
    }

    public int pdfVisiblePageNumber() { return pdfVisiblePageNumber.get(); }

    public void updatePdfVisiblePageNumber(int page) { pdfVisiblePageNumber.set(Math.max(0, page)); }

    public ReadOnlyObjectProperty<DocumentTextRange> selectedDocumentTextRangeProperty() { return selectedDocumentTextRange; }
    public ReadOnlyObjectProperty<com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef>
    selectedPdfRegionProperty() { return selectedPdfRegion; }

    public ReadOnlyObjectProperty<com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget>
    selectedPdfVisualTargetProperty() { return selectedPdfVisualTarget; }

    public ReadOnlyStringProperty requestedPdfRegionReviewIdProperty() {
        return requestedPdfRegionReviewId;
    }

    public ReadOnlyStringProperty selectedDocumentRangeLabelProperty() { return selectedDocumentRangeLabel.getReadOnlyProperty(); }

    public ReadOnlyStringProperty selectedDocumentSourceLocationProperty() { return selectedDocumentSourceLocation.getReadOnlyProperty(); }

    public ReadOnlyStringProperty selectedVisualFragmentSegmentIdProperty() { return selectedVisualFragmentSegmentId; }

    public ReadOnlyStringProperty selectedVisualFragmentImageUriProperty() { return selectedVisualFragmentImageUri; }

    public ReadOnlyObjectProperty<DocumentVisualFragmentKey> selectedVisualFragmentKeyProperty() { return selectedVisualFragmentKey; }

    public ReadOnlyIntegerProperty documentMediaRevisionProperty() { return documentMediaRevision; }
    public ReadOnlyObjectProperty<java.util.Set<String>> documentarySourceVisualChangesProperty() {
        return documentarySourceVisualChanges;
    }

    public ReadOnlyStringProperty documentPrimaryActionLabelProperty() { return documentPrimaryActionLabel.getReadOnlyProperty(); }

    public ReadOnlyStringProperty documentPrimaryActionHintProperty() { return documentPrimaryActionHint.getReadOnlyProperty(); }

    public ReadOnlyStringProperty documentListenFlowTitleProperty() { return documentListenFlowTitle.getReadOnlyProperty(); }

    public ReadOnlyStringProperty documentListenFlowDetailProperty() { return documentListenFlowDetail.getReadOnlyProperty(); }

    public ReadOnlyStringProperty streamingBufferStatusProperty() { return streamingBufferStatus.getReadOnlyProperty(); }

    public BooleanProperty documentAudioPortionEnabledProperty() {
        return documentAudioPortionEnabled;
    }

    public ObjectProperty<DocumentProcessingScope> documentProcessingScopeProperty() {
        return documentProcessingScope;
    }

    public ReadOnlyObjectProperty<DocumentInteractionProjection>
    documentInteractionProjectionProperty() {
        return documentInteractionProjection.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty documentSelectionValidProperty() {
        return documentSelectionValid.getReadOnlyProperty();
    }

    public DocumentProcessingScope documentProcessingScope() {
        return Objects.requireNonNullElse(documentProcessingScope.get(),
                DocumentProcessingScope.FULL_DOCUMENT);
    }

    public void setDocumentProcessingScope(DocumentProcessingScope scope) {
        DocumentProcessingScope safe = Objects.requireNonNullElse(scope,
                DocumentProcessingScope.FULL_DOCUMENT);
        boolean rejectedPartialScope = false;
        boolean rejectedUnsupportedScope = false;
        DocumentInteractionProjection currentInteraction =
                documentInteractionProjection.get();
        if (currentInteraction != null && !currentInteraction.supports(safe)) {
            safe = DocumentProcessingScope.FULL_DOCUMENT;
            rejectedUnsupportedScope = true;
        }
        if (DocumentInteractionProjection.partial(safe)
                && (currentInteraction == null || !currentInteraction.selectionValid())) {
            safe = DocumentProcessingScope.FULL_DOCUMENT;
            rejectedPartialScope = true;
        }
        synchronizingDocumentProcessingScope = true;
        try {
            documentProcessingScope.set(safe);
            documentAudioPortionEnabled.set(safe.legacyPortionEnabled());
            documentAudioPreparationExtent.set(safe.legacyExtent());
        } finally {
            synchronizingDocumentProcessingScope = false;
        }
        if (rejectedUnsupportedScope) {
            statusMessage.set("Ese alcance no está disponible para la fuente actual. Se mantiene lectura completa.");
        } else if (rejectedPartialScope) {
            statusMessage.set("Selecciona una oración, bloque o región antes de usar un alcance parcial. Se mantiene lectura completa.");
        } else {
            statusMessage.set("Alcance documental: " + safe + ".");
        }
        refreshDocumentInteractionProjection();
    }

    private void refreshDocumentInteractionProjection() {
        DocumentProcessingScope requested = documentProcessingScope();
        boolean narrationReady = currentScript.get() != null && !currentScript.get().empty();
        boolean audioReady = managedAudioChunksAvailable.get();
        DocumentInteractionProjection projection;
        if (currentPreparedPdfSource.get() != null) {
            projection = pdfInteractionAdapter.project(
                    currentPreparedPdfSource.get(), selectedPdfRegion.get(),
                    selectedScriptSegmentId.get(), requested, narrationReady, audioReady);
        } else if (currentDocument.get() != null) {
            projection = wordInteractionAdapter.project(
                    currentDocument.get(), selectedDocumentBlockId.get(),
                    selectedScriptSegmentId.get(), requested, narrationReady, audioReady);
        } else {
            projection = DocumentInteractionProjection.none();
        }
        documentInteractionProjection.set(projection);
        documentSelectionValid.set(projection.selectionValid());
        if (projection.effectiveScope() != requested) {
            synchronizingDocumentProcessingScope = true;
            try {
                documentProcessingScope.set(projection.effectiveScope());
                documentAudioPortionEnabled.set(
                        projection.effectiveScope().legacyPortionEnabled());
                documentAudioPreparationExtent.set(
                        projection.effectiveScope().legacyExtent());
            } finally {
                synchronizingDocumentProcessingScope = false;
            }
        }
    }

    public IntegerProperty documentProcessingIntervalStartProperty() {
        return documentProcessingIntervalStart;
    }

    public IntegerProperty documentProcessingIntervalEndProperty() {
        return documentProcessingIntervalEnd;
    }

    public ReadOnlyIntegerProperty documentProcessingPageCountProperty() {
        return documentProcessingPageCount.getReadOnlyProperty();
    }

    public int documentProcessingPageCount() {
        return documentProcessingPageCount.get();
    }

    public ReadOnlyBooleanProperty documentProcessingIntervalSupportedProperty() {
        return documentProcessingIntervalSupported.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty documentProcessingIntervalValidProperty() {
        return documentProcessingIntervalValid.getReadOnlyProperty();
    }

    public Optional<DocumentProcessingInterval> validatedDocumentProcessingInterval() {
        if (!documentProcessingIntervalSupported.get()) return Optional.empty();
        try {
            int start = documentProcessingIntervalStart.get();
            int end = documentProcessingIntervalEnd.get();
            int count = documentProcessingPageCount.get();
            return Optional.of(currentPreparedPdfSource.get() != null
                    ? DocumentProcessingInterval.pages(start, end, count)
                    : DocumentProcessingInterval.blocks(start, end, count));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    /** One-based visible page for PDF or selected semantic block for Word. */
    public int documentProcessingIntervalAnchor() {
        if (currentPreparedPdfSource.get() != null) {
            return Math.max(1, pdfVisiblePageNumber());
        }
        NarrationScriptDocument script = currentScript.get();
        List<String> blocks = script == null ? List.of()
                : ResolveDocumentProcessingSelectionUseCase.sourceBlocks(
                        script.segments());
        String selected = Objects.toString(selectedDocumentBlockId.get(), "").strip();
        int index = blocks.indexOf(selected);
        return index < 0 ? 1 : index + 1;
    }

    public void setDocumentProcessingInterval(int start, int end) {
        documentProcessingIntervalStart.set(start);
        documentProcessingIntervalEnd.set(end);
        refreshDocumentProcessingIntervalValidity();
    }

    public void setDocumentProcessingIntervalStart(int start) {
        int pageCount = Math.max(1, documentProcessingPageCount());
        DocumentProcessingInterval adjusted = documentProcessingIntervalPolicy
                .afterStartChange(start, documentProcessingIntervalEnd.get(), pageCount);
        documentProcessingIntervalStart.set(adjusted.start());
        documentProcessingIntervalEnd.set(adjusted.end());
        refreshDocumentProcessingIntervalValidity();
    }

    public void setDocumentProcessingIntervalEnd(int end) {
        documentProcessingIntervalEnd.set(end);
        refreshDocumentProcessingIntervalValidity();
    }

    public ObjectProperty<DocumentAudioPreparationExtent>
    documentAudioPreparationExtentProperty() {
        return documentAudioPreparationExtent;
    }

    public boolean documentAudioPortionEnabled() {
        return documentAudioPortionEnabled.get();
    }

    public void setDocumentAudioPortionEnabled(boolean enabled) {
        documentAudioPortionEnabled.set(enabled);
        statusMessage.set(enabled
                ? "La próxima preparación de audio comenzará en la selección y respetará el tamaño elegido."
                : "La próxima preparación de audio abarcará el documento completo.");
    }

    public DocumentAudioPreparationExtent documentAudioPreparationExtent() {
        return Objects.requireNonNullElse(
                documentAudioPreparationExtent.get(),
                DocumentAudioPreparationExtent.SHORT_READING);
    }

    public void setDocumentAudioPreparationExtent(
            DocumentAudioPreparationExtent extent) {
        DocumentAudioPreparationExtent safe = Objects.requireNonNullElse(
                extent, DocumentAudioPreparationExtent.SHORT_READING);
        documentAudioPreparationExtent.set(safe);
        statusMessage.set("Tamaño de la porción: " + safe.displayName()
                + ". " + safe.detail() + ".");
    }

    public String documentAudioPreparationSummary() {
        if (documentProcessingScope() == DocumentProcessingScope.INTERVAL) {
            if (!documentProcessingIntervalSupported.get()) {
                return "No hay unidades narrables disponibles para definir el intervalo.";
            }
            return validatedDocumentProcessingInterval()
                    .map(interval -> interval.unit() == DocumentProcessingIntervalUnit.PAGE
                            ? "Páginas " + interval.start() + "-" + interval.end()
                            + " · " + interval.size() + " página(s)."
                            : "Bloques " + interval.start() + "-" + interval.end()
                            + " · " + interval.size() + " bloque(s) narrable(s).")
                    .orElse("El intervalo no es válido para la fuente actual.");
        }
        if (!documentAudioPortionEnabled()) {
            return "Documento completo · todos los fragmentos narrables en un solo trabajo.";
        }
        DocumentAudioPreparationExtent extent = documentAudioPreparationExtent();
        boolean explicitSelection = selectedPdfRegion.get() != null
                || (selectedDocumentBlockId.get() != null
                && !selectedDocumentBlockId.get().isBlank());
        String origin = explicitSelection
                ? "Desde el fragmento seleccionado"
                : "Desde la página o el inicio visible";
        return origin + " · " + extent.detail() + ".";
    }

    private void syncScopeFromLegacyAudioPreparation() {
        if (synchronizingDocumentProcessingScope) return;
        DocumentProcessingScope mapped = !documentAudioPortionEnabled()
                ? DocumentProcessingScope.FULL_DOCUMENT
                : documentAudioPreparationExtent() == DocumentAudioPreparationExtent.SINGLE_FRAGMENT
                ? DocumentProcessingScope.SINGLE_FRAGMENT
                : DocumentProcessingScope.FROM_SELECTION;
        setDocumentProcessingScope(mapped);
    }

    private void resetDocumentProcessingInterval(PreparedPdfSource source) {
        int unitCount;
        DocumentProcessingIntervalUnit unit;
        if (source != null) {
            unitCount = projectWorkspace().document().openPreparedPdfWorkspace()
                    .pageCount(source.workspace());
            unit = DocumentProcessingIntervalUnit.PAGE;
        } else {
            NarrationScriptDocument script = currentScript.get();
            unitCount = script == null ? 0
                    : ResolveDocumentProcessingSelectionUseCase
                    .sourceBlocks(script.segments()).size();
            if (unitCount == 0 && currentDocument.get() != null) {
                unitCount = currentDocument.get().blocks().size();
            }
            unit = DocumentProcessingIntervalUnit.BLOCK;
        }
        documentProcessingPageCount.set(Math.max(0, unitCount));
        documentProcessingIntervalSupported.set(unitCount > 0);
        int anchor = unitCount <= 0 ? 1
                : Math.max(1, Math.min(unitCount,
                source == null ? documentProcessingIntervalAnchor()
                        : pdfVisiblePageNumber()));
        DocumentProcessingInterval initial = unitCount <= 0
                ? new DocumentProcessingInterval(
                unit, 1, 1)
                : documentProcessingIntervalPolicy.initial(anchor, unitCount);
        documentProcessingIntervalStart.set(initial.start());
        documentProcessingIntervalEnd.set(initial.end());
        refreshDocumentProcessingIntervalValidity();
    }

    private void refreshDocumentProcessingIntervalValidity() {
        documentProcessingIntervalValid.set(
                validatedDocumentProcessingInterval().isPresent());
    }

    public DocumentListeningPreferences documentListeningPreferences() {
        return sessions.activeSession()
                .map(session -> session.project()
                        .documentListeningPreferences())
                .orElseGet(DocumentListeningPreferences::defaults);
    }

    public void setDocumentListeningPreferences(
            DocumentListeningPreferences preferences) {
        DocumentListeningPreferences previous = documentListeningPreferences();
        DocumentListeningPreferences safe = Objects.requireNonNullElseGet(
                preferences, DocumentListeningPreferences::defaults);
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withDocumentListeningPreferences(safe), true));
        statusMessage.set(safe.reviewTechnicalElements()
                ? "La escucha ofrecerá revisar localmente los elementos técnicos pendientes."
                : "La escucha omitirá los elementos técnicos pendientes.");
        if (previous.secondarySemanticPolicy()
                != safe.secondarySemanticPolicy()) {
            currentScript.set(null);
            bumpDocumentMediaRevision();
            statusMessage.set(safe.secondarySemanticPolicy().description());
        }
        refreshProjectState();
    }

    public DocumentTranslationPreferences documentTranslationPreferences() {
        return sessions.activeSession()
                .map(session -> session.project().documentTranslationPreferences())
                .orElseGet(DocumentTranslationPreferences::defaults);
    }

    public void setDocumentTranslationPreferences(
            DocumentTranslationPreferences preferences) {
        DocumentTranslationPreferences previous = documentTranslationPreferences();
        DocumentTranslationPreferences safe = Objects.requireNonNullElseGet(
                preferences, DocumentTranslationPreferences::defaults);
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withDocumentTranslationPreferences(safe), true));
        if (!previous.equals(safe)) {
            narrationTranslationCancellationRequested.set(true);
            narrationTranslationGeneration.incrementAndGet();
            resetPlaybackState();
            activeAudioJobStatus.set(AudioJobStatusDto.idle());
            audioJobRunning.set(false);
        }
        statusMessage.set(!safe.enabled()
                ? "Traducción desactivada: se escuchará el idioma original."
                : "Idioma de escucha: " + safe.listeningLanguage()
                + ". El documento fuente no se modificará.");
        refreshFullDocumentReadingReadiness();
        refreshProjectState();
    }

    public ReadOnlyObjectProperty<PlaybackCursor> playbackCursorProperty() { return playbackCursor; }

    public ReadOnlyObjectProperty<PlaybackCue> activePlaybackCueProperty() { return activePlaybackCue; }

    public ReadOnlyObjectProperty<PlaybackManifest> currentPlaybackManifestProperty() { return currentPlaybackManifest; }

    public ReadOnlyObjectProperty<ProjectMode> currentProjectModeProperty() { return currentProjectMode; }

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

    public ReadOnlyObjectProperty<AudioJobStatusDto> activeAudioJobStatusProperty() { return activeAudioJobStatus; }

    public ReadOnlyObjectProperty<ReadingProfile> activeReadingProfileProperty() { return activeReadingProfile; }

    public ReadOnlyObjectProperty<VoiceLibrary> activeVoiceLibraryProperty() { return activeVoiceLibrary; }

    public ReadOnlyStringProperty statusMessageProperty() { return statusMessage.getReadOnlyProperty(); }

    public void updateStatusMessage(String message) { sessionStore.updateStatus(message); }

    public void replaceCurrentDocumentFromApplication(ReadableDocument document, String message) { if (document == null) { return; } sessions.activeSession().ifPresent(session -> session.setImportedDocument(document)); currentDocument.set(document); currentScript.set(null); statusMessage.set(message == null || message.isBlank() ? "Documento actualizado." : message); refreshProjectState(); }

    public ReadOnlyBooleanProperty projectOpenProperty() { return projectOpen; }

    public ReadOnlyBooleanProperty dirtyProperty() { return dirty; }

    public ReadOnlyBooleanProperty saveableProjectOpenProperty() { return saveableProjectOpen; }

    public ReadOnlyBooleanProperty audioJobRunningProperty() { return audioJobRunning; }

    public ReadOnlyBooleanProperty theatreRefreshRunningProperty() { return theatreRefreshRunning; }

    public ReadOnlyBooleanProperty narrativeVisualGenerationRunningProperty() {
        return narrativeVisualGenerationRunning;
    }

    public ReadOnlyBooleanProperty choralVoiceRenderingProperty() { return theatreChoralVoiceRenderWorkflow.runningProperty(); }

    public ReadOnlyDoubleProperty choralVoiceRenderProgressProperty() { return theatreChoralVoiceRenderWorkflow.progressProperty(); }

    public ReadOnlyStringProperty choralVoiceRenderStatusProperty() { return theatreChoralVoiceRenderWorkflow.statusProperty(); }

    public ReadOnlyBooleanProperty voiceRecordingRunningProperty() { return voiceRecordingRunning; }

    public ReadOnlyBooleanProperty manualAudioRecordingRunningProperty() { return manualAudioRecordingRunning; }

    public ReadOnlyStringProperty focusedTheatreSceneIdProperty() { return focusedTheatreSceneId; }

    public void focusTheatreScene(String sceneId) { focusedTheatreSceneId.set(sceneId == null ? "" : sceneId.strip()); }

    public ReadOnlyObjectProperty<Path> lastGeneratedVoiceTestPathProperty() { return lastGeneratedVoiceTestPath; }

    public ReadOnlyStringProperty generatedVoiceTestStatusProperty() { return generatedVoiceTestStatus.getReadOnlyProperty(); }

    public ReadOnlyStringProperty documentVoiceToneStatusProperty() { return documentVoiceToneStatus.getReadOnlyProperty(); }

    public ReadOnlyStringProperty spatialFrameModeProperty() { return spatialFrameMode; }

    public void setSpatialFrameMode(String mode) {
        String normalized = TheatreStageGeometry.normalizeFrameMode(mode);
        sessions.activeSession().ifPresent(session -> session.replaceProject(session.project()
                .withViewState("theatre.presentationMode", normalized), true));
        spatialFrameMode.set(normalized);
        refreshProjectState();
    }

    public ReadOnlyStringProperty activePlacementAliasProperty() { return activePlacementAlias; }

    public ReadOnlyObjectProperty<TheatreProjectLayer.TextActionPlacement> activeTextActionPlacementProperty() { return activeTextActionPlacement; }

    public BooleanProperty documentRightRailVisibleProperty() { return documentRightRailVisible; }

    public BooleanProperty documentPlaybarDockedProperty() { return documentPlaybarDocked; }

    public BooleanProperty technicalProblemPreparationActiveProperty() { return technicalProblemPreparationActive; }

    public BooleanProperty readAfterColonForNarrationProperty() { return readAfterColonForNarration; }

    public IntegerProperty readingFontSizeProperty() { return readingFontSize; }

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

    public int readingZoomPercent() { return readingComfortWorkflow.zoomPercent(readingFontSize.get()); }

    public void updatePdfVisualDocumentProgress(double progress) { pdfVisualDocumentProgress.set(Math.max(0.0, Math.min(1.0, progress))); }

    private void acceptPdfPreparationProgress(PdfPreparationProgress snapshot) {
        PdfPreparationProgress safe = snapshot == null
                ? new PdfPreparationProgress(PdfPreparationProgress.State.IDLE,
                0, 0, 0, 0, "")
                : snapshot;
        Runnable update = () -> pdfPreparationProgress.set(safe);
        if (Platform.isFxApplicationThread()) update.run(); else Platform.runLater(update);
    }

    public void toggleDocumentRightRail() { documentRightRailVisible.set(!documentRightRailVisible.get()); statusMessage.set(documentRightRailVisible.get() ? "Rail derecho visible." : "Rail derecho oculto."); }

    public void openTechnicalProblemPanel() { if (!hasDocumentSource()) { technicalProblemPreparationActive.set(false); statusMessage.set("Abre una fuente documental antes de preparar problemas tecnicos."); refreshProjectState(); return; } activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); technicalProblemPreparationActive.set(true); documentRightRailVisible.set(true); statusMessage.set("Panel de problema tecnico abierto. Usa el panel derecho para seleccionar fragmentos."); refreshProjectState(); }

    public void toggleDocumentPlaybarDocked() { documentPlaybarDocked.set(!documentPlaybarDocked.get()); statusMessage.set(documentPlaybarDocked.get() ? "Playbar desplazada al rail izquierdo." : "Playbar flotante sobre el documento."); }

    public void toggleTechnicalProblemPreparation() { if (!hasDocumentSource()) { technicalProblemPreparationActive.set(false); statusMessage.set("Abre una fuente documental antes de preparar problemas tecnicos."); refreshProjectState(); return; } technicalProblemPreparationActive.set(!technicalProblemPreparationActive.get()); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); documentRightRailVisible.set(true); statusMessage.set(technicalProblemPreparationActive.get() ? "Seleccion de fragmentos para problema tecnico activa." : "Seleccion de fragmentos para problema tecnico desactivada."); refreshProjectState(); }
    private void loadReadingComfortSettings() { readingFontSize.set(readingComfortWorkflow.loadInitialFontSize()); }

    public String voiceRecordingStatusLabel() {
        if (voiceRecordingRunning.get()) {
            return "Grabando muestra de voz humana" + (activeVoiceRecordingFile == null ? "" : ": " + activeVoiceRecordingFile.getFileName());
        }
        return "Sin grabación activa";
    }

    public boolean hasUnsavedChanges() { return sessions.dirty(); }

    public boolean hasOpenProject() { return sessions.hasActiveSession(); }

    public Optional<DocuPodcastProject> currentProject() { return sessions.activeSession().map(ProjectSession::project); }

    public boolean beginTheatreRefresh() {
        if (theatreRefreshRunning.get()) return false;
        theatreRefreshRunning.set(true);
        statusMessage.set("Preparando la actualización de la obra…");
        return true;
    }

    public void finishTheatreRefresh(String message) {
        theatreRefreshRunning.set(false);
        if (message != null && !message.isBlank()) statusMessage.set(message.strip());
        refreshProjectState();
    }

    public void applyPersistedTheatreRefresh(DocuPodcastProject project, String message) {
        ProjectSession session = requireSession();
        session.replaceProject(Objects.requireNonNull(project, "project"), false);
        activeVoiceLibrary.set(project.voiceLibrary());
        bumpDocumentMediaRevision();
        theatreRefreshRunning.set(false);
        statusMessage.set(message == null || message.isBlank() ? "Obra actualizada." : message.strip());
        refreshProjectState();
    }

    /** Applies a grammar import as an unsaved user-visible project mutation. */
    public void applyTheatrePackageImport(com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.ImportOfficialTheatrePackageUseCase.Result result) {
        ProjectSession session = requireSession();
        session.replaceProject(result.project(), true);
        session.setImportedDocument(result.document());
        currentPreparedPdfSource.set(null);
        currentDocument.set(result.document());
        selectedDocumentBlockId.set("");
        clearVisualFragmentSelection();
        technicalProblemPreparationActive.set(false);
        applyImportedOrGeneratedScript(session, result.script());
        applyTheatreGrammarImport(session.project(), "Carpeta teatral configurada: " + result.script().segments().size() + " parlamentos.");
    }

    public void applyTheatreGrammarImport(DocuPodcastProject project, String message) {
        ProjectSession session = requireSession();
        session.replaceProject(Objects.requireNonNull(project, "project"), true);
        theatreBoundaryWorkflow.hydrate(project, intervencionBoundaryStore);
        activeVoiceLibrary.set(project.voiceLibrary());
        bumpDocumentMediaRevision();
        statusMessage.set(message == null || message.isBlank() ? "Gramática teatral aplicada." : message.strip());
        refreshProjectState();
    }

    public void updateProjectVisualProcessing(ProjectVisualProcessingSettings settings) {
        sessions.activeSession().ifPresent(session -> {
            session.replaceProject(session.project().withVisualProcessing(settings), true);
            refreshProjectState();
        });
    }

    public Optional<Path> currentProjectFile() { return sessions.activeSession().flatMap(ProjectSession::projectFile); }

    public DocumentStudyVideoConfiguration documentaryVideoConfiguration() { return currentProject().map(project -> project.study().documentaryVideoConfiguration()).orElseGet(DocumentStudyVideoConfiguration::empty); }

    public Optional<DocumentContentProjection> currentDocumentContentProjection() {
        return sessions.activeSession().flatMap(session -> session.documentSource().map(source -> {
            DocumentContentProjection projection = projectWorkspace().document()
                    .buildDocumentContentProjection().build(source, currentScript.get());
            DocumentStudyVideoConfiguration current = session.project().study()
                    .documentaryVideoConfiguration();
            DocumentStudyVideoConfiguration reconciled =
                    new ReconcileDocumentStudyVideoConfigurationUseCase()
                            .execute(projection, current);
            if (!reconciled.equals(current)) {
                session.replaceProject(session.project().withStudy(session.project().study()
                        .withDocumentaryVideoConfiguration(reconciled)), true);
            }
            return projection;
        }));
    }

    public boolean documentaryVideoConfigurationAvailable() {
        return currentProjectMode.get() == ProjectMode.DOCUMENTARY_STUDIO
                && sessions.activeSession().flatMap(ProjectSession::documentSource).isPresent();
    }

    public void updateDocumentaryVideoConfiguration(DocumentStudyVideoConfiguration value) { documentaryController.update(requireSession(), value); documentaryVideoChanged("Configuracion de video documental actualizada."); }

    public ProjectAssetReference importDocumentaryVideoImage(Path source) throws IOException { ProjectAssetReference asset = documentaryController.importImage(workspaceServices, requireSession(), source); documentaryVideoChanged("Imagen copiada dentro del proyecto: " + asset.displayName()); return asset; }

    public DocumentaryExperienceController.DrawingAsset saveDocumentaryDrawing(String blockId, Path png, String state) throws IOException { var asset = documentaryController.saveDrawing(requireSession(), blockId, png, state); documentaryVideoChanged("Dibujo guardado dentro del proyecto."); return asset; }
    public DocumentaryExperienceController.DrawingAsset saveDocumentaryDrawing(String blockId, Path png, String state, Map<String, Path> stagedSources) throws IOException { var asset = documentaryController.saveDrawing(requireSession(), blockId, png, state, stagedSources); documentaryVideoChanged("Ilustracion guardada dentro del proyecto."); return asset; }

    public DocumentStudyMusicTrack importDocumentaryMusic(Path source) throws IOException { var imported = documentaryController.importMusic(workspaceServices, requireSession(), source); documentaryVideoChanged("Musica copiada dentro del proyecto: " + imported.displayName()); return imported.track(); }

    public Optional<Path> resolveCurrentProjectAsset(String assetId) { return documentaryController.resolveAsset(currentProject(), currentProjectFile(), assetId); }

    public Optional<Path> resolveCurrentProjectRelativePath(String path) { return documentaryController.resolveRelative(currentProjectFile(), path); }

    public Optional<Path> materializeDocumentaryPdfSourceVisual(String contentId) throws IOException {
        return materializeDocumentarySourceVisual(contentId);
    }

    public Optional<Path> materializeDocumentarySourceVisual(String contentId) throws IOException {
        DocumentContentProjection projection = currentDocumentContentProjection().orElse(null);
        if (projection == null) return Optional.empty();
        var content = projection.itemById(contentId).orElse(null);
        if (content == null) return Optional.empty();
        Path projectDirectory = currentProjectDirectory().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de materializar la imagen original."));
        Path path;
        ProjectAssetReference sourceVisualAsset;
        String visualFingerprint;
        if (content.pdfAnchor().isPresent()) {
            var result = projectWorkspace().document().materializePdfDocumentContentAsset()
                    .materialize(content, projection.items(), projection.sourcePath(), projectDirectory);
            path = result.path();
            sourceVisualAsset = result.asset();
            visualFingerprint = result.visualFingerprint();
        } else if (projectWorkspace().document().materializeWordDocumentContentAsset()
                .canMaterialize(content)) {
            var result = projectWorkspace().document().materializeWordDocumentContentAsset()
                    .materialize(content, projectDirectory);
            path = result.path();
            sourceVisualAsset = result.asset();
            visualFingerprint = result.visualFingerprint();
        } else {
            return Optional.empty();
        }
        applyMaterializedSourceVisuals(java.util.List.of(
                new MaterializedDocumentarySourceVisual(content, sourceVisualAsset, visualFingerprint)));
        return Optional.of(path);
    }

    /** Materializes documentary source thumbnails without coupling the operation to card selection. */
    public void materializeDocumentarySourceVisualsAsync(java.util.Collection<String> contentIds) {
        DocumentContentProjection projection = currentDocumentContentProjection().orElse(null);
        Path projectDirectory = currentProjectDirectory().orElse(null);
        if (projection == null || projectDirectory == null || contentIds == null || contentIds.isEmpty()) return;
        java.util.List<String> pending = contentIds.stream().filter(java.util.Objects::nonNull)
                .map(String::strip).filter(id -> !id.isBlank()).distinct().filter(id -> {
                    var content = projection.itemById(id).orElse(null);
                    if (content == null) return false;
                    var configured = ReconcileDocumentStudyVideoConfigurationUseCase
                            .configuredSlide(content, documentaryVideoConfiguration()).orElse(null);
                    return configured == null || configured.sourceVisualAssetId().isBlank()
                            || resolveCurrentProjectAsset(configured.sourceVisualAssetId()).isEmpty();
                }).filter(documentarySourceVisualsInFlight::add).toList();
        if (pending.isEmpty()) return;
        long materializationStartedNanos = System.nanoTime();
        LOGGER.info("documentary-source-visual.materialization state=STARTED pending={} batchSize={}",
                pending.size(), DOCUMENTARY_SOURCE_VISUAL_BATCH_SIZE);
        Thread worker = new Thread(() -> {
            java.util.ArrayList<MaterializedDocumentarySourceVisual> completed = new java.util.ArrayList<>();
            for (String id : pending) {
                try {
                    var content = projection.itemById(id).orElse(null);
                    if (content == null) {
                        documentarySourceVisualsInFlight.remove(id);
                        continue;
                    }
                    ProjectAssetReference asset;
                    String fingerprint;
                    if (content.pdfAnchor().isPresent()) {
                        var result = projectWorkspace().document().materializePdfDocumentContentAsset()
                                .materialize(content, projection.items(), projection.sourcePath(), projectDirectory);
                        asset = result.asset();
                        fingerprint = result.visualFingerprint();
                    } else if (projectWorkspace().document().materializeWordDocumentContentAsset()
                            .canMaterialize(content)) {
                        var result = projectWorkspace().document().materializeWordDocumentContentAsset()
                                .materialize(content, projectDirectory);
                        asset = result.asset();
                        fingerprint = result.visualFingerprint();
                    } else {
                        documentarySourceVisualsInFlight.remove(id);
                        continue;
                    }
                    completed.add(new MaterializedDocumentarySourceVisual(content, asset, fingerprint));
                    if (completed.size() >= DOCUMENTARY_SOURCE_VISUAL_BATCH_SIZE) {
                        publishMaterializedSourceVisualBatch(completed, projectDirectory);
                        completed = new java.util.ArrayList<>();
                    }
                } catch (IOException | RuntimeException ex) {
                    Platform.runLater(() -> {
                        documentarySourceVisualsInFlight.remove(id);
                        statusMessage.set("No se pudo preparar una miniatura original: " + rootCauseMessage(ex));
                    });
                }
            }
            publishMaterializedSourceVisualBatch(completed, projectDirectory);
            LOGGER.info("documentary-source-visual.materialization state=QUEUED pending={} elapsedMs={}",
                    pending.size(), java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - materializationStartedNanos));
        }, "docupodcast-source-thumbnail-materializer");
        worker.setDaemon(true);
        worker.start();
    }

    private void publishMaterializedSourceVisualBatch(
            java.util.List<MaterializedDocumentarySourceVisual> materialized,
            Path expectedProjectDirectory) {
        if (materialized == null || materialized.isEmpty()) return;
        java.util.List<MaterializedDocumentarySourceVisual> batch = java.util.List.copyOf(materialized);
        Platform.runLater(() -> {
            try {
                boolean sameProject = currentProjectDirectory()
                        .map(path -> path.toAbsolutePath().normalize().equals(
                                expectedProjectDirectory.toAbsolutePath().normalize()))
                        .orElse(false);
                if (sameProject) {
                    applyMaterializedSourceVisuals(batch);
                } else {
                    LOGGER.info("documentary-source-visual.materialization state=DISCARDED reason=PROJECT_CHANGED count={}",
                            batch.size());
                }
            } finally {
                batch.forEach(item -> documentarySourceVisualsInFlight.remove(item.content().contentId()));
            }
        });
    }

    /** Applies one immutable project mutation and one row-scoped notification per batch. */
    private void applyMaterializedSourceVisuals(
            java.util.List<MaterializedDocumentarySourceVisual> materialized) {
        if (materialized == null || materialized.isEmpty()) return;
        long startedNanos = System.nanoTime();
        ProjectSession active = requireSession();
        var updatedProject = active.project();
        var updatedConfiguration = updatedProject.study().documentaryVideoConfiguration();
        java.util.LinkedHashSet<String> changedContentIds = new java.util.LinkedHashSet<>();
        boolean projectChanged = false;
        for (MaterializedDocumentarySourceVisual result : materialized) {
            var content = result.content();
            var sourceVisualAsset = result.asset();
            String visualFingerprint = result.visualFingerprint();
            var previous = ReconcileDocumentStudyVideoConfigurationUseCase
                    .configuredSlide(content, updatedConfiguration)
                    .orElseGet(() -> com.marcosmoreiradev.docupodcaststudio.domain.study
                            .DocumentVideoSlideConfiguration.empty(content.contentId()));
            if (!previous.contentId().equals(content.contentId())) {
                previous = new com.marcosmoreiradev.docupodcaststudio.domain.study
                        .DocumentVideoSlideConfiguration(content.contentId(),
                        previous.sourceFingerprint(), previous.visual(),
                        previous.durationSeconds(), previous.enabled(),
                        previous.sourceVisualAssetId(), previous.sourceVisualFingerprint());
            }
            if (updatedProject.assets().byId(sourceVisualAsset.id()).isEmpty()) {
                updatedProject = updatedProject.withAsset(sourceVisualAsset);
                projectChanged = true;
            }
            if (!sourceVisualAsset.id().equals(previous.sourceVisualAssetId())
                    || !visualFingerprint.equals(previous.sourceVisualFingerprint())
                    || !content.fingerprint().equals(previous.sourceFingerprint())) {
                updatedConfiguration = updatedConfiguration.withContent(previous.withSourceVisualAsset(
                        sourceVisualAsset.id(), visualFingerprint, content.fingerprint()));
                changedContentIds.add(content.contentId());
                projectChanged = true;
            }
        }
        if (!projectChanged) return;
        updatedProject = updatedProject.withStudy(updatedProject.study()
                .withDocumentaryVideoConfiguration(updatedConfiguration));
        active.replaceProject(updatedProject, true);
        if (!changedContentIds.isEmpty()) {
            documentarySourceVisualChanges.set(java.util.Set.copyOf(changedContentIds));
        }
        LOGGER.info("documentary-source-visual.materialization state=BATCH_APPLIED received={} changed={} elapsedMs={}",
                materialized.size(), changedContentIds.size(),
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }

    private record MaterializedDocumentarySourceVisual(
            com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem content,
            ProjectAssetReference asset,
            String visualFingerprint) { }
    private void documentaryVideoChanged(String message) { bumpDocumentMediaRevision(); statusMessage.set(message); refreshProjectState(); }

    public NarrativeProjectLayer narrativeProjectLayer() {
        return currentProject().map(DocuPodcastProject::narrative).orElseGet(NarrativeProjectLayer::empty);
    }

    public NarrativeVideoConfiguration narrativeVideoConfiguration() {
        return narrativeProjectLayer().videoConfiguration();
    }

    public boolean narrativeVideoConfigurationAvailable() {
        return currentProjectMode.get() == ProjectMode.NARRATIVE_VIDEO
                && currentDocument.get() != null
                && currentDocument.get().format()
                == com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat.DOCX;
    }

    public List<DocumentBlock> narrativeVideoParagraphs() {
        return narrativeDocumentContextCompiler.narrativeParagraphs(currentDocument.get());
    }

    public Optional<NarrativeDocumentContext> narrativeDocumentContext(String blockId) {
        if (currentDocument.get() == null || blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(narrativeDocumentContextCompiler.compile(currentDocument.get(), blockId));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public void synchronizeNarrativeDocumentSnapshot() {
        if (!narrativeVideoConfigurationAvailable()) {
            return;
        }
        ReadableDocument document = currentDocument.get();
        String fingerprint = narrativeDocumentContextCompiler.fingerprint(document);
        NarrativeProjectLayer current = narrativeProjectLayer();
        if (fingerprint.equals(current.documentFingerprint())) {
            return;
        }
        narrativeVideoAssetWorkflow.update(requireSession(), current.withDocumentSnapshot(
                fingerprint, narrativeDocumentContextCompiler.normalizedDocumentText(document)));
        narrativeVideoChanged("El contexto del Word cambio; las tomas narrativas se marcaron como desactualizadas.");
    }

    public void updateNarrativeVideoConfiguration(NarrativeVideoConfiguration configuration) {
        narrativeVideoAssetWorkflow.updateConfiguration(requireSession(), configuration);
        narrativeVideoChanged("Ajustes de video narrativo actualizados.");
    }

    public void setNarrativeParagraphEnabled(String blockId, boolean enabled) {
        NarrativeProjectLayer layer = narrativeProjectLayer();
        NarrativeParagraphTake take = layer.takeOrDefault(blockId).withEnabled(enabled);
        narrativeVideoAssetWorkflow.update(requireSession(), layer.withTake(take));
        narrativeVideoChanged(enabled
                ? "Parrafo habilitado para el video narrativo."
                : "Parrafo excluido de narracion y video.");
    }

    public ProjectAssetReference importNarrativeKeyframe(String blockId, Path source) throws IOException {
        ProjectAssetReference asset = narrativeVideoAssetWorkflow.importKeyframe(requireSession(), blockId, source);
        narrativeVideoChanged("Imagen clave copiada dentro del proyecto: " + asset.displayName());
        return asset;
    }

    public NarrativeContextReference importNarrativeContextReference(Path source,
                                                                     NarrativeContextRole role) throws IOException {
        NarrativeContextReference reference = narrativeVideoAssetWorkflow.importContextReference(
                requireSession(), source, role);
        narrativeVideoChanged("Referencia global copiada dentro del proyecto: " + reference.displayName());
        return reference;
    }

    public void updateNarrativeContextReference(NarrativeContextReference reference) {
        NarrativeProjectLayer updated = narrativeProjectLayer().withContextReference(reference);
        narrativeVideoAssetWorkflow.update(requireSession(), updated);
        narrativeVideoChanged("Banco global de contexto narrativo actualizado.");
    }

    public void removeNarrativeContextReference(String referenceId) {
        NarrativeProjectLayer updated = narrativeProjectLayer().withoutContextReference(referenceId);
        narrativeVideoAssetWorkflow.update(requireSession(), updated);
        narrativeVideoChanged("Referencia retirada del banco narrativo.");
    }

    public void generateNarrativeKeyframe(String blockId) {
        String target = normalizeNarrativeBlockId(blockId);
        startNarrativeVisualGeneration(
                "Preparando imagen clave para " + target + "...",
                (session, document, script, jobs, progress, cancelled) ->
                        narrativeController.generateKeyframe(
                                session, document, target, progress, cancelled).message());
    }

    public void generateNarrativeClips(String blockId) {
        String target = normalizeNarrativeBlockId(blockId);
        startNarrativeVisualGeneration(
                "Preparando clips para " + target + "...",
                (session, document, script, jobs, progress, cancelled) ->
                        narrativeController.generateClips(
                                session, document, script, jobs, target, progress, cancelled).message());
    }

    public void generatePendingNarrativeTakes() {
        startNarrativeVisualGeneration(
                "Analizando tomas narrativas pendientes...",
                (session, document, script, jobs, progress, cancelled) -> {
                    int completed = 0;
                    int skipped = 0;
                    for (DocumentBlock block :
                            narrativeDocumentContextCompiler.narrativeParagraphs(document)) {
                        if (cancelled.getAsBoolean()) {
                            throw new IOException("Generacion narrativa cancelada por el usuario.");
                        }
                        NarrativeParagraphTake take =
                                session.project().narrative().takeOrDefault(block.id());
                        if (!take.enabled()) {
                            skipped++;
                            continue;
                        }
                        if (take.clipsReady()) {
                            skipped++;
                            continue;
                        }
                        if (!take.keyframeReady() || take.stale()) {
                            narrativeController.generateKeyframe(
                                    session, document, block.id(), progress, cancelled);
                        }
                        narrativeController.generateClips(
                                session, document, script, jobs, block.id(), progress, cancelled);
                        completed++;
                    }
                    return "Lote narrativo completado: " + completed
                            + " toma(s) generadas y " + skipped + " omitida(s).";
                });
    }

    public void cancelNarrativeVisualGeneration() {
        if (!narrativeVisualGenerationRunning.get()) {
            return;
        }
        narrativeVisualCancellationRequested.set(true);
        statusMessage.set(
                "Deteniendo generacion narrativa al terminar la operacion local en curso...");
    }

    public void openNarrativeVideoProduction() {
        if (currentProjectMode.get() != ProjectMode.NARRATIVE_VIDEO) {
            statusMessage.set("Abre un proyecto de Video narrativo para configurar sus tomas.");
            return;
        }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        documentRightRailVisible.set(true);
        statusMessage.set("Contenido del video narrativo activo.");
        refreshProjectState();
    }

    private String normalizeNarrativeBlockId(String blockId) {
        String target = blockId == null ? "" : blockId.strip();
        if (target.isBlank()) {
            throw new IllegalArgumentException("Selecciona un parrafo narrativo.");
        }
        return target;
    }

    private void startNarrativeVisualGeneration(String initialMessage,
                                                NarrativeVisualWork work) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> startNarrativeVisualGeneration(initialMessage, work));
            return;
        }
        if (narrativeVisualGenerationRunning.get()) {
            statusMessage.set("Ya hay una generacion visual narrativa en curso.");
            return;
        }
        try {
            if (!narrativeVideoConfigurationAvailable()) {
                throw new IOException(
                        "Abre un proyecto de Video narrativo con una fuente Word/DOCX.");
            }
            synchronizeNarrativeDocumentSnapshot();
            if (currentScript.get() == null || currentScript.get().empty()) {
                buildNarrationScriptFromDocument();
            }
            ProjectSession session = requireSession();
            Path projectFile = session.projectFile().orElseThrow(() ->
                    new IOException(
                            "Guarda el proyecto antes de generar contenido narrativo."));
            ReadableDocument document = currentDocument.get();
            NarrationScriptDocument script = currentScript.get();
            List<AudioJobSnapshot> jobs = listPersistedJobsSafely();
            ReadingProfile readingProfile = activeReadingProfile.get();
            VoiceLibrary voiceLibrary = activeVoiceLibrary.get();
            narrativeVisualCancellationRequested.set(false);
            narrativeVisualGenerationRunning.set(true);
            statusMessage.set(initialMessage);
            Thread worker = new Thread(() -> {
                String completionMessage = "";
                Throwable failure = null;
                try {
                    completionMessage = work.run(
                            session,
                            document,
                            script,
                            jobs,
                            message -> Platform.runLater(() -> {
                                if (sessions.activeSession().orElse(null) == session
                                        && message != null && !message.isBlank()) {
                                    statusMessage.set(message);
                                }
                            }),
                            narrativeVisualCancellationRequested::get);
                } catch (Throwable ex) {
                    failure = ex;
                }
                try {
                    if (sessions.activeSession().orElse(null) == session) {
                        projectController.saveProject(
                                session,
                                projectFile,
                                readingProfile,
                                voiceLibrary);
                    }
                } catch (Throwable saveFailure) {
                    if (failure == null) {
                        failure = saveFailure;
                    } else {
                        failure.addSuppressed(saveFailure);
                    }
                }
                String finalMessage = completionMessage;
                Throwable finalFailure = failure;
                Platform.runLater(() -> {
                    narrativeVisualGenerationRunning.set(false);
                    narrativeVisualCancellationRequested.set(false);
                    if (sessions.activeSession().orElse(null) != session) {
                        return;
                    }
                    if (finalFailure == null) {
                        narrativeVideoChanged(finalMessage);
                    } else if (rootCauseMessage(finalFailure).toLowerCase(Locale.ROOT)
                            .contains("cancel")) {
                        narrativeVideoChanged(
                                "Generacion narrativa detenida. Se conservaron los resultados completos.");
                    } else {
                        statusMessage.set(
                                "No se pudo generar el contenido narrativo: "
                                        + rootCauseMessage(finalFailure));
                        refreshProjectState();
                    }
                });
            }, "docupodcast-narrative-visual-generation");
            worker.setDaemon(true);
            worker.start();
        } catch (Exception ex) {
            narrativeVisualGenerationRunning.set(false);
            narrativeVisualCancellationRequested.set(false);
            statusMessage.set(
                    "No se pudo iniciar la generacion narrativa: " + rootCauseMessage(ex));
            refreshProjectState();
        }
    }

    private void narrativeVideoChanged(String message) {
        bumpDocumentMediaRevision();
        statusMessage.set(message);
        refreshProjectState();
    }

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

    public boolean selectedTheatreApplyCameraPlane() { return new TheatreCameraApplicationPolicy().appliesToSegment(currentStoryboard.get(), selectedTheatreVisualSegment().orElse(null)); }

    public void setSelectedTheatreApplyCameraPlane(boolean applyCamera) { StoryboardDocument storyboard = currentStoryboard.get(); String segmentId = selectedTheatreVisualSegment().map(NarrationSegment::id).orElse(""); if (storyboard == null || segmentId.isBlank()) { statusMessage.set("Asocia una imagen al fragmento antes de cambiar Aplicar plano."); refreshProjectState(); return; } StoryboardBinding binding = storyboard.bindingForSegment(segmentId).orElse(null); if (binding == null) { statusMessage.set("Asocia una imagen al fragmento antes de cambiar Aplicar plano."); refreshProjectState(); return; } Map<String, String> metadata = new TheatreCameraApplicationPolicy().metadataWithApplyCamera(binding.metadata(), applyCamera); StoryboardDocument updated = storyboard.withBinding(binding.withMetadata(metadata)); currentStoryboard.set(updated); sessions.activeSession().ifPresent(session -> session.setStoryboard(updated)); selectedScriptSegmentId.set(segmentId); statusMessage.set(applyCamera ? "Plano aplicado al fragmento." : "Plano omitido para este fragmento."); bumpDocumentMediaRevision(); refreshProjectState(); }

    public void setTheatreCameraCueForSelectedSegment(String cameraId) throws IOException { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); statusMessage.set(theatreVisualSetupWorkflow.setCameraCue(session, id, cameraId).message()); bumpDocumentMediaRevision(); refreshProjectState(); }

    public TheatreVisualSetupCoordinator.StageBackdropPreview selectedTheatreStageBackdropPreview() { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); String sceneId = new TheatreFragmentLinkPolicy().sceneIdForIntervention(session.project().theatre(), id).orElse(""); return theatreVisualSetupWorkflow.stageBackdropPreview(sessions.activeSession(), currentProjectDirectory(), id, sceneId); }

    public void assignStageBackdropForSelectedSegment(Path imageFile, boolean fragmentOverride) throws IOException { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); String sceneId = new TheatreFragmentLinkPolicy().sceneIdForIntervention(session.project().theatre(), id).orElse(""); statusMessage.set(theatreVisualSetupWorkflow.assignStageBackdrop(workspaceServices, session, id, sceneId, imageFile, fragmentOverride).message()); bumpDocumentMediaRevision(); refreshProjectState(); }

    public void clearStageBackdropFromSelectedSegment() { ProjectSession session = requireSession(); String id = selectedTheatreInterventionId(session).orElse(""); statusMessage.set(theatreVisualSetupWorkflow.clearStageBackdropFromIntervention(session, id).message()); bumpDocumentMediaRevision(); refreshProjectState(); }

    public String theatreContextTextOverride(String segmentId) { return new TheatreContextTextPolicy().text(currentProject().orElse(null), segmentId).orElse(""); }

    public void saveTheatreContextText(String segmentId, String text) { ProjectSession session = requireSession(); session.replaceProject(new TheatreContextTextPolicy().save(session.project(), segmentId, text), true); statusMessage.set("Contexto textual guardado para " + segmentId + "."); refreshProjectState(); }

    public void addTheatreCharacterSceneImage(String characterId, String sceneId, Path imageFile) throws IOException { statusMessage.set(theatreImageAssetWorkflow.addCharacterImage(requireSession(), characterId, sceneId, imageFile).message()); refreshProjectState(); }

    public void replaceTheatreCharacterSceneImage(String imageId, Path imageFile) throws IOException { statusMessage.set(theatreImageAssetWorkflow.replaceCharacterImage(requireSession(), currentStoryboard.get(), imageId, imageFile).message()); refreshProjectState(); }

    public void updateTheatreCharacterSceneImageNote(String imageId, String note) { statusMessage.set(theatreImageAssetWorkflow.updateCharacterImageNote(requireSession(), imageId, note).message()); refreshProjectState(); }

    public void deleteTheatreCharacterSceneImage(String imageId) { statusMessage.set(theatreImageAssetWorkflow.deleteCharacterImage(requireSession(), currentStoryboard.get(), imageId).message()); refreshProjectState(); }

    public List<TheatreProjectLayer.TextActionPlacement> theatreTextActionPlacements() { return sessions.activeSession().map(session -> session.project().theatre().textActionPlacements()).orElseGet(List::of); }

    public void saveTheatreTextActionPlacement(TheatreProjectLayer.TextActionPlacement placement) { Objects.requireNonNull(placement, "placement"); ProjectSession session = requireSession(); theatreTextActionPlacementSaveWorkflow.save(session, placement); activePlacementAlias.set(placement.intervencionId()); activeTextActionPlacement.set(placement); statusMessage.set(placement.intervencionId() + " actualizado en mapa espacial."); refreshProjectState(); }

    public TechnicalProblem saveTechnicalProblem(List<DocumentBlock> sourceBlocks, String requestedTitle, String solutionText, WritableImage solutionImage, java.util.Map<String, Path> sourceCropPaths) throws IOException { return saveTechnicalProblem(sourceBlocks, requestedTitle, solutionText, solutionImage, sourceCropPaths, null); }

    public TechnicalProblem saveTechnicalProblem(List<DocumentBlock> sourceBlocks, String requestedTitle, String solutionText, WritableImage solutionImage, java.util.Map<String, Path> sourceCropPaths, String canvasStateJson) throws IOException { TechnicalProblem problem = studyProblemWorkflow.save(requireSession(), currentProjectDirectory(), sourceBlocks, requestedTitle, solutionText, solutionImage, sourceCropPaths, canvasStateJson); statusMessage.set("Problema tecnico guardado: " + problem.id() + (problem.solutionImageAssetId().isBlank() ? "." : ". Lienzo exportado como PNG.")); bumpDocumentMediaRevision(); refreshProjectState(); return problem; }

    public TechnicalProblem saveTechnicalProblemFromSources(java.util.List<com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft> sourceDrafts, String requestedTitle, String solutionText, WritableImage solutionImage) throws IOException { return saveTechnicalProblemFromSources(sourceDrafts, requestedTitle, solutionText, solutionImage, null); }

    public TechnicalProblem saveTechnicalProblemFromSources(java.util.List<com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft> sourceDrafts, String requestedTitle, String solutionText, WritableImage solutionImage, String canvasStateJson) throws IOException { TechnicalProblem problem = studyProblemWorkflow.saveFromSources(requireSession(), currentProjectDirectory(), sourceDrafts, requestedTitle, solutionText, solutionImage, canvasStateJson); statusMessage.set("Problema tecnico guardado: " + problem.id() + (problem.solutionImageAssetId().isBlank() ? "." : ". Lienzo exportado como PNG.")); bumpDocumentMediaRevision(); refreshProjectState(); return problem; }

    public com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemsProjection studyProblemsProjection() { return workspaceServices.project().documentStudy().buildStudyProblemsProjection().build(currentProject().orElse(null), currentProjectDirectory().orElse(null)); }

    public TechnicalProblem updateTechnicalProblemSolution(String problemId, String solutionText, WritableImage solutionImage, String notes) throws IOException { return updateTechnicalProblemSolution(problemId, solutionText, solutionImage, notes, null); }

    public TechnicalProblem updateTechnicalProblemSolution(String problemId, String solutionText, WritableImage solutionImage, String notes, String canvasStateJson) throws IOException { TechnicalProblem problem = studyProblemWorkflow.updateSolution(requireSession(), currentProjectDirectory(), problemId, solutionText, solutionImage, notes, canvasStateJson); statusMessage.set("Problema tecnico actualizado: " + problem.id() + "."); bumpDocumentMediaRevision(); refreshProjectState(); return problem; }

    public void deleteTechnicalProblem(String problemId) throws IOException { studyProblemWorkflow.delete(requireSession(), currentProjectDirectory(), problemId); statusMessage.set("Problema tecnico eliminado: " + problemId + "."); bumpDocumentMediaRevision(); refreshProjectState(); }

    public void exportTechnicalProblemImage(String problemId, Path target) throws IOException { Path output = studyProblemWorkflow.exportSolutionImage(requireSession(), currentProjectDirectory(), problemId, target); statusMessage.set("Solucion PNG exportada: " + output.getFileName() + "."); }

    public Path exportTechnicalProblemImage(WritableImage image, Path target) throws IOException { Path output = studyProblemWorkflow.exportSolutionImage(image, target); statusMessage.set("Solucion PNG exportada: " + output.getFileName() + "."); return output; }

    public void exportTechnicalProblemText(String problemId, Path target) throws IOException { Path output = studyProblemWorkflow.exportSolutionText(requireSession(), problemId, target); statusMessage.set("Solucion textual exportada: " + output.getFileName() + "."); }

    public void exportAllTechnicalProblemImages(Path targetDirectory) throws IOException { var report = studyProblemWorkflow.exportAllSolutionImages(requireSession(), currentProjectDirectory(), targetDirectory); statusMessage.set("Problemas exportados: " + report.exported() + " PNG, omitidos: " + report.skipped() + ", errores: " + report.failed() + ". Manifest: " + report.manifestPath().getFileName() + "."); }

    public void exportAllTechnicalProblemImagesPdf(Path targetPdf) throws IOException { var report = studyProblemWorkflow.exportAllSolutionImagesAsPdf(requireSession(), currentProjectDirectory(), targetPdf); statusMessage.set("PDF de ejercicios exportado: " + report.pdfPath().getFileName() + ". Paginas: " + report.exported() + ", omitidos: " + report.skipped() + ", errores: " + report.failed() + "."); }

    public Path exportTheatreInterventionContext(String sceneId, String intervencionId, Path targetDirectory) throws IOException {
        TheatreInterventionContextExportWorkflow.Result result = theatreInterventionContextExportWorkflow.export(requireSession(), currentScript.get(), sceneId, intervencionId, targetDirectory);
        statusMessage.set("Paquete IA creado para " + intervencionId + ": " + result.copiedImages() + " imagenes copiadas en " + result.folder() + ".");
        return result.folder();
    }

    public TheatreContextExportEstimate estimateTheatreContextPackages(TheatreContextExportScope scope) { return theatreBulkContextExportWorkflow.estimate(requireSession(), currentScript.get(), scope); }

    public TheatreBulkInterventionContextExportWorkflow.Result exportTheatreContextPackages(TheatreContextExportScope scope, Path targetDirectory) throws IOException { var result = theatreBulkContextExportWorkflow.export(requireSession(), currentScript.get(), scope, targetDirectory); statusMessage.set("Paquetes IA teatrales exportados: " + result.packages() + " paquetes en " + result.root() + "."); return result; }

    public List<TheatreImageGenerationUnit> theatreImageGenerationQueue(TheatreContextExportScope scope) { return theatreController.queue(requireSession(), currentScript.get(), scope); }

    public List<TheatreImageContextAsset> theatreImageGenerationContextAssets(TheatreImageGenerationUnit unit) { return theatreController.contextAssets(requireSession(), unit); }

    public EngineReadiness imageEngineReadiness() { return theatreController.readiness(); }

    public TheatreGeneratedImageCandidate generateTheatreImageCandidate(TheatreImageGenerationUnit unit, ImageGenerationWorkspaceSettings settings, TheatreImageGenerationPreset preset, boolean assign) throws IOException { return generateTheatreImageCandidate(unit, settings, preset, ImageEnhancementOutputProfile.FHD_1080, assign); }

    public TheatreGeneratedImageCandidate generateTheatreImageCandidate(TheatreImageGenerationUnit unit, ImageGenerationWorkspaceSettings settings, TheatreImageGenerationPreset preset, ImageEnhancementOutputProfile outputProfile, boolean assign) throws IOException {
        return generateTheatreImageCandidate(unit, settings, preset, outputProfile, TheatreImageAspectRatio.WIDE_16_9, assign, null);
    }

    public TheatreGeneratedImageCandidate generateTheatreImageCandidate(TheatreImageGenerationUnit unit, ImageGenerationWorkspaceSettings settings, TheatreImageGenerationPreset preset, ImageEnhancementOutputProfile outputProfile, TheatreImageAspectRatio aspectRatio, boolean assign, Consumer<String> progress) throws IOException {
        var candidate = theatreController.generate(requireSession(), unit, settings, preset, outputProfile, aspectRatio, assign, progress);
        updateProjectStatusOnFxThread((candidate.approved() ? "Imagen generada y asignada a " : "Candidato IA generado para ") + candidate.interventionId() + ".", true);
        return candidate;
    }

    public TheatreGeneratedImageCandidate generateTheatreTransitionImageCandidate(TheatreImageGenerationUnit unit, String previousAssetId, Path previousFrame, String nextAssetId, Path nextFrame, ImageGenerationWorkspaceSettings settings, TheatreImageGenerationPreset preset, ImageEnhancementOutputProfile outputProfile, TheatreImageAspectRatio aspectRatio, Consumer<String> progress) throws IOException { var candidate = theatreController.generateTransition(requireSession(), unit, previousAssetId, previousFrame, nextAssetId, nextFrame, settings, preset, outputProfile, aspectRatio, progress); updateProjectStatusOnFxThread("Candidato IA intermedio generado para " + unit.interventionId() + ".", true); return candidate; }

    public TheatreGeneratedFrameCandidate generateTheatreIntermediateFrameCandidate(TheatreIntermediateFrameBatchPlanner.Item item, ImageGenerationWorkspaceSettings settings, Consumer<String> progress) throws IOException { var candidate = theatreController.generateIntermediateTransition(requireSession(), item.current(), item.next(), item.previousReference(), item.nextReference(), settings, progress); updateProjectStatusOnFxThread("Frame intermedio generado para " + candidate.interventionId() + " -> " + candidate.nextInterventionId() + ".", true); return candidate; }

    public TheatreGeneratedImageCandidate approveTheatreGeneratedImageCandidate(TheatreGeneratedImageCandidate candidate) {
        var approved = theatreController.approve(requireSession(), candidate);
        updateProjectStatusOnFxThread("Candidato IA aprobado para " + approved.interventionId() + ".", true);
        return approved;
    }

    public TheatreGeneratedImageCandidate enhanceTheatreImageCandidate(TheatreGeneratedImageCandidate c, ImageEnhancementOutputProfile p, ImageAspectStrategy s) throws IOException {
        var e = new ImageEnhancementWorkflow(workspaceServices, mediaCapabilities).enhanceCandidate(requireSession(), c, p, s);
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
        TheatreGeneratedImageCandidate approved = theatreController.approve(requireSession(), new TheatreGeneratedImageCandidate(candidate.unitId(), candidate.sceneId(), candidate.interventionId(), candidate.segmentId(), candidate.assetId(), candidate.outputPath(), candidate.approved()));
        TheatreGeneratedFrameCandidate frame = new TheatreGeneratedFrameCandidate(candidate.unitId(), candidate.sceneId(), candidate.interventionId(), candidate.segmentId(), candidate.frameIndex(), candidate.transitionFrame(), candidate.nextInterventionId(), approved.assetId(), approved.outputPath(), true);
        updateProjectStatusOnFxThread("Frame aprobado para " + frame.interventionId() + ".", true);
        return frame;
    }

    public List<TheatreProjectLayer.IntervencionVisual> theatreIntervencionesVisuales() { return sessions.activeSession().map(session -> session.project().theatre().intervencionesVisuales()).orElseGet(List::of); }

    public TheatreAudioTrackTimeline theatreAudioTrackTimeline() { return theatreAudioTrackWorkflow.timeline(workspaceServices, sessions.activeSession(), currentScript.get(), currentPlaybackManifest.get()); }

    public Optional<String> selectedTheatreAudioInterventionId() { return theatreAudioTrackWorkflow.interventionForSelection(sessions.activeSession(), currentScript.get(), selectedScriptSegmentId.get(), selectedDocumentBlockId.get()); }

    public void selectTheatreAudioIntervention(String id) { theatreAudioTrackWorkflow.blockForIntervention(sessions.activeSession(), id).ifPresent(this::selectDocumentBlock); }

    public void selectTheatreAudioSegment(String id) { theatreAudioTrackWorkflow.blockForSegment(currentScript.get(), id).ifPresent(this::selectDocumentBlock); }

    public List<TheatreChoralVoiceWorkflow.Option> selectedTheatreChoralVoiceOptions() { return new TheatreChoralVoiceWorkflow().options(sessions.activeSession(), selectedTheatreAudioInterventionId(), audioEngineDescriptor()); }

    public TheatreChoralVoiceWorkflow.State selectedTheatreChoralVoiceState() { Optional<String> interventionId = selectedTheatreAudioInterventionId(); return new TheatreChoralVoiceWorkflow().state(sessions.activeSession(), interventionId, interventionId.flatMap(this::narrationSegmentForIntervention)); }

    public boolean canRenderSelectedTheatreChoralVoices(List<String> ids) { return theatreChoralVoiceRenderWorkflow.canRender(selectedTheatreChoralVoiceOptions(), ids, currentProjectMode.get() == ProjectMode.THEATRE_PRODUCTION, selectedTheatreAudioInterventionId().isPresent(), audioJobRunning.get() || choralVoiceRenderingProperty().get()); }

    public void renderSelectedTheatreChoralVoices(List<String> characterIds) { try { if (currentProjectMode.get() != ProjectMode.THEATRE_PRODUCTION) throw new IOException("Las voces simultaneas solo estan disponibles en modo Teatro."); if (audioJobRunning.get() || choralVoiceRenderingProperty().get()) throw new IOException("Espera a que termine el trabajo de audio actual."); if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument(); ProjectSession session = requireSession(); Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de renderizar voces simultaneas.")); String interventionId = selectedTheatreAudioInterventionId().orElseThrow(() -> new IOException("Selecciona una intervencion teatral narrable.")); List<String> participants = TheatreChoralVoiceRenderCoordinator.normalize(characterIds); if (!canRenderSelectedTheatreChoralVoices(participants)) throw new IOException("Selecciona al menos dos personajes con voz local disponible."); RenderTheatreChoralVoiceUseCase useCase = workspaceServices.project().theatre().renderChoralVoice(); if (useCase == null) throw new IOException("El render multipersona no esta disponible en esta configuracion de la aplicacion."); saveCurrentProjectAs(projectFile); var request = new TheatreChoralVoiceRenderRequest(session.project(), projectFile, currentScript.get(), interventionId, participants, audioEngineDescriptor()); theatreChoralVoiceRenderWorkflow.start(useCase, request, result -> applyTheatreChoralVoiceResult(session, projectFile, result), statusMessage::set); } catch (Exception ex) { theatreChoralVoiceRenderWorkflow.fail("No se pudo iniciar la voz multipersona: " + rootCauseMessage(ex), statusMessage::set); refreshProjectState(); } }
    private void applyTheatreChoralVoiceResult(ProjectSession session, Path projectFile, TheatreChoralVoiceRenderResult result) { try { session.replaceProject(result.project(), true); activeVoiceLibrary.set(result.project().voiceLibrary()); saveCurrentProjectAs(projectFile); rebuildPlaybackManifestFromLatestJob(); bumpDocumentMediaRevision(); theatreChoralVoiceRenderWorkflow.complete(result.message(), statusMessage::set); } catch (Exception ex) { theatreChoralVoiceRenderWorkflow.fail("La mezcla se genero, pero no se pudo guardar el proyecto: " + rootCauseMessage(ex), statusMessage::set); } finally { refreshProjectState(); } }

    public void clearSelectedTheatreChoralVoices() { try { var result = new TheatreChoralVoiceWorkflow().clear(requireSession(), selectedTheatreAudioInterventionId().orElse("")); if (result.saved() && currentProjectFile().isPresent()) saveCurrentProjectAs(currentProjectFile().get()); theatreChoralVoiceRenderWorkflow.complete(result.message(), statusMessage::set); rebuildPlaybackManifestFromLatestJob(); bumpDocumentMediaRevision(); refreshProjectState(); } catch (IOException ex) { theatreChoralVoiceRenderWorkflow.fail("No se pudo restaurar la voz simple: " + rootCauseMessage(ex), statusMessage::set); refreshProjectState(); } }

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
        PreparedPdfSource pdf = currentPreparedPdfSource.get();
        if (pdf != null) return Optional.of(pdf.sourcePath());
        ReadableDocument document = currentDocument.get();
        return document == null ? Optional.empty() : Optional.of(document.sourcePath());
    }

    private boolean hasDocumentSource() {
        return currentDocument.get() != null || currentPreparedPdfSource.get() != null;
    }

    public Optional<Path> currentProjectDirectory() { return currentProjectFile().map(Path::toAbsolutePath).map(Path::normalize).map(Path::getParent); }

    public Optional<Path> currentExportsDirectory() { return currentProjectDirectory().map(directory -> directory.resolve("exports")); }

    public void createNewProject(String title) { createNewProject(title, ProjectMode.defaultMode()); }

    public void createNewProject(String title, ProjectMode mode) {
        ProjectSession session = projectController.createNewProject(title, mode);
        DocuPodcastProject project = session.project();
        activeReadingProfile.set(project.readingProfile());
        activeVoiceLibrary.set(project.voiceLibrary());
        currentDocument.set(null); currentPreparedPdfSource.set(null); currentScript.set(null); currentStoryboard.set(null); lastStoryboardImageAssetId.set("");
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
        OpenedProjectContext opened = prepareProjectOpen(sourceFile);
        applyOpenedProject(opened, sourceFile,
                prepareOpenedProjectPlaybackManifest(opened, sourceFile));
    }

    public OpenedProjectContext prepareProjectOpen(Path sourceFile) throws IOException {
        OpenedProjectContext opened = projectController.prepareProject(sourceFile);
        boolean repairedAviadores = theatreDemoManifestWorkflow.repairAviadoresLegacyAssets(opened.session());
        DocuPodcastProject project = opened.session().project();
        if (repairedAviadores) {
            projectController.saveProject(opened.session(), sourceFile, project.readingProfile(), project.voiceLibrary());
        }
        return opened;
    }

    public void applyOpenedProject(OpenedProjectContext opened, Path sourceFile) throws IOException {
        applyOpenedProject(opened, sourceFile,
                prepareOpenedProjectPlaybackManifest(opened, sourceFile));
    }

    public PlaybackManifest prepareOpenedProjectPlaybackManifest(
            OpenedProjectContext opened, Path sourceFile) {
        NarrationScriptDocument script = opened == null
                ? null : opened.narrationScript().orElse(null);
        if (script == null || script.empty() || sourceFile == null) {
            return PlaybackManifest.empty();
        }
        Path projectDirectory = sourceFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) return PlaybackManifest.empty();
        try {
            ProjectSession session = opened.session();
            java.util.List<AudioJobSnapshot> snapshots = audioWorkflow.persistedJobs(projectDirectory);
            AudioGenerationRequest request = audioGenerationRequestFor(
                    session, script, projectDirectory, session.title());
            ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                    request.generationUnits(), snapshots, projectDirectory);
            if (coverage.readyAudio().isEmpty()) return PlaybackManifest.empty();
            java.time.Instant now = java.time.Instant.now();
            AudioJobSnapshot combined = new AudioJobSnapshot(
                    "COVERAGE-MANIFEST", script.title(), AudioJobState.COMPLETED,
                    AudioGenerationStage.EXPORT_READY, coverage.readyAudio().size(),
                    request.generationUnitCount(), 0,
                    coverage.readyAudio().size() / (double) request.generationUnitCount(),
                    "", "", 0L, "Cobertura compatible reunida desde jobs persistidos.",
                    "jobs", "", "", coverage.readyAudio(), now, now);
            var plan = workspaceServices.generation().render().buildNarrationRenderPlan()
                    .build(script, session.project());
            return workspaceServices.playback().playback().buildPlaybackManifest()
                    .build(script, combined, opened.storyboard().orElse(null), plan,
                            session.project());
        } catch (IOException | RuntimeException unavailable) {
            return PlaybackManifest.empty();
        }
    }

    public void applyOpenedProject(OpenedProjectContext opened, Path sourceFile,
                                   PlaybackManifest preparedPlayback) throws IOException {
        projectController.activateProject(opened);
        DocuPodcastProject project = opened.session().project();
        activeReadingProfile.set(project.readingProfile());
        activeVoiceLibrary.set(project.voiceLibrary());
        currentDocument.set(opened.importedDocument().orElse(null));
        currentPreparedPdfSource.set(opened.preparedPdfSource().orElse(null));
        currentScript.set(opened.narrationScript().orElse(null));
        currentStoryboard.set(opened.storyboard().orElse(null));
        lastStoryboardImageAssetId.set(opened.lastStoryboardImageAssetId());
        selectedScriptSegmentId.set(opened.selectedScriptSegmentId());
        selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        theatreBoundaryWorkflow.hydrate(project, intervencionBoundaryStore);
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        loadLatestPersistedAudioStatus(sourceFile);
        currentPlaybackManifest.set(preparedPlayback == null
                ? PlaybackManifest.empty() : preparedPlayback);
        statusMessage.set("Proyecto abierto: " + project.metadata().title()
                + ". Perfil de lectura: " + project.readingProfile().name()
                + ". Rehidratación: " + opened.hydration().statusLabel() + ".");
        if (!opened.recoveryWarnings().isEmpty()) {
            statusMessage.set(statusMessage.get() + " Estado degradado: "
                    + opened.recoveryWarnings().size()
                    + " derivados ausentes; se regenerarán solo al solicitarlos.");
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
        PreparedPdfSource previousPdf = session.preparedPdfSource().orElse(null);
        theatreBoundaryWorkflow.persist(session, intervencionBoundaryStore);
        projectController.saveProject(session, targetFile, activeReadingProfile.get(), activeVoiceLibrary.get());
        sessions.activeSession().flatMap(ProjectSession::importedDocument).ifPresent(doc -> Platform.runLater(() -> currentDocument.set(doc)));
        PreparedPdfSource savedPdf = session.preparedPdfSource().orElse(null);
        currentPreparedPdfSource.set(savedPdf);
        if (previousPdf != null
                && (savedPdf == null || !previousPdf.workspace().projectRoot()
                .equals(savedPdf.workspace().projectRoot()))) {
            workspaceServices.project().document().createPreparedPdfSessionWorkspace()
                    .closeIfSessionWorkspace(previousPdf);
        }
        String saveMsg = "Proyecto guardado en: " + targetFile + projectSourceCopyStatusSuffix() + ".";
        Platform.runLater(() -> statusMessage.set(saveMsg));
        refreshProjectState();
    }

    public ProjectIntegrityInspectionOutcome inspectPreparedProject(OpenedProjectContext opened, Path sourceFile) throws IOException {
        List<AudioJobSnapshot> jobs = workspaceServices.playback().audio().listPersistedAudioJobs()
                .list(sourceFile.toAbsolutePath().normalize().getParent());
        return projectController.inspectIntegrity(opened.session(), sourceFile, opened.hydration(), jobs);
    }
    private String projectSourceCopyStatusSuffix() {
        return currentSourceDocumentPath()
                .map(path -> ". Fuente canónica: " + path.getFileName() + " dentro de la carpeta source del proyecto")
                .orElse("");
    }

    public void closeCurrentProject() {
        PreparedPdfSource transientPdf = currentPreparedPdfSource.get();
        projectController.closeProject();
        try {
            workspaceServices.project().document().createPreparedPdfSessionWorkspace()
                    .closeIfSessionWorkspace(transientPdf);
        } catch (IOException ignored) {
            // Best-effort cleanup; closing the project must remain available.
        }
        currentDocument.set(null); currentPreparedPdfSource.set(null); currentScript.set(null); currentStoryboard.set(null); lastStoryboardImageAssetId.set("");
        selectedScriptSegmentId.set(""); selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        intervencionBoundaryStore.clear();
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        activeReadingProfile.set(workspaceServices.project().readingProfile().createDefaultProfile().create());
        activeVoiceLibrary.set(workspaceServices.administration().voice().createDefaultVoiceLibrary().create());
        loadReadingComfortSettings();
        activeWorkspace.set(WorkspaceKind.WELCOME_HOME);
        statusMessage.set("Proyecto cerrado.");
        refreshProjectState();
    }

    public com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource
    importAndClassifySourceDocument(Path sourceFile) throws IOException {
        return documentIntake.importAndClassify(sourceFile, activeReadingProfile.get());
    }

    public ReadableDocument importAndClassifyBlockSourceDocument(Path sourceFile) throws IOException {
        return documentIntake.importAndClassifyBlock(sourceFile, activeReadingProfile.get());
    }

    public void attachImportedDocument(
            com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource source)
            throws IOException {
        ReadingProfile profile = activeReadingProfile.get();
        ProjectSession session = sessions.activeSession().orElseThrow(() ->
                new IOException("Primero crea un proyecto y después selecciona su fuente documental."));
        PreparedPdfSource previousPdf = currentPreparedPdfSource.get();
        if (source instanceof PreparedPdfSource pdf) {
            documentIntake.attachDocumentSource(session, pdf, profile);
            currentPreparedPdfSource.set(pdf);
            currentDocument.set(null);
        } else {
            ReadableDocument classified =
                    ((com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource) source)
                            .document();
            documentIntake.attachImportedDocument(session, classified, profile);
            currentPreparedPdfSource.set(null);
            currentDocument.set(classified);
        }
        workspaceServices.project().document().createPreparedPdfSessionWorkspace()
                .closeIfSessionWorkspace(previousPdf);
        currentScript.set(null); currentStoryboard.set(null);
        lastStoryboardImageAssetId.set(""); selectedScriptSegmentId.set(""); selectedDocumentBlockId.set(""); clearVisualFragmentSelection(); technicalProblemPreparationActive.set(false); resetDocumentSideDocksForProjectStart();
        resetPlaybackState(); activeAudioJobStatus.set(AudioJobStatusDto.idle()); audioJobRunning.set(false); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        if (source instanceof PreparedPdfSource) {
            statusMessage.set("PDF importado en un workspace V2 temporal. Sus páginas se prepararán progresivamente.");
        } else {
            ReadableDocument classified =
                    ((com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource) source)
                            .document();
            statusMessage.set("Documento fuente importado en modo solo lectura y perfil aplicado: %d bloques, %d narrables, %d imágenes, %d tablas, %d ignorados.".formatted(
                    classified.blocks().size(), classified.narratableBlockCount(),
                    classified.imageNoticeCount(), classified.tableNoticeCount(), classified.ignoredCount()));
        }
        refreshProjectState();
    }

    public Path createPdfSourceFromImageFolder(Path sourceFolder) throws IOException { Path projectDirectory = currentProjectDirectory().orElseThrow(() -> new IOException("Guarda el proyecto antes de crear una fuente PDF desde carpeta.")); String folderName = sourceFolder == null || sourceFolder.getFileName() == null ? "imagenes" : sourceFolder.getFileName().toString(); Path outputDirectory = projectDirectory.resolve("source").resolve("generated-pdf-from-images"); Files.createDirectories(outputDirectory); Path createdPdf = workspaceServices.project().document().createPdfFromImageFolder().create(sourceFolder, uniqueGeneratedPdfPath(outputDirectory, safeGeneratedPdfStem(folderName))); attachImportedDocument(importAndClassifySourceDocument(createdPdf)); statusMessage.set("Fuente PDF creada desde carpeta de imagenes: " + createdPdf.getFileName() + "."); refreshProjectState(); return createdPdf; }
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
        ReadableDocument updated = workspaceServices.project().document().updateDocumentBlockType().update(document, blockId, type);
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
        ReadableDocument updated = workspaceServices.project().readingProfile().applyReadingProfile().apply(document, profile);
        currentDocument.set(updated);
        sessions.activeSession().ifPresent(session -> session.setImportedDocument(updated));
        currentScript.set(null); resetPlaybackState(); activeAudioJobStatus.set(AudioJobStatusDto.idle()); audioJobRunning.set(false);
        statusMessage.set("Perfil aplicado: " + profile.name() + ". Estructura: " + updated.structuralBlockCount() + " bloques; ignorados: " + updated.ignoredCount() + ".");
        refreshProjectState();
    }

    public void applyDefaultReadingProfile() { applyReadingProfile(activeReadingProfile.get()); }

    public boolean readTablesAndTextBoxesForNarration() { ReadingProfile profile = activeReadingProfile.get(); return profile != null && profile.tablePolicy().readsAll(); }

    public TableNarrationPolicy tableNarrationPolicy() {
        ReadingProfile profile = activeReadingProfile.get();
        return profile == null
                ? ReadingProfile.academicDefaults().tablePolicy()
                : profile.tablePolicy();
    }

    public void setTableNarrationPolicy(TableNarrationPolicy policy) {
        ReadingProfile base = activeReadingProfile.get() == null
                ? ReadingProfile.academicDefaults() : activeReadingProfile.get();
        TableNarrationPolicy next = policy == null
                ? TableNarrationPolicy.SUMMARIZE : policy;
        if (base.tablePolicy() == next) return;
        ReadingProfile updated = new ReadingProfile(
                base.id(), base.name(), base.description(), base.headingRules(),
                base.imagePolicy(), next);
        activeReadingProfile.set(updated);
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withReadingProfile(updated), true));
        if (audioJobRunning.get()) cancelActiveAudioJobSilently();
        invalidatePersistedAudioForNarrationChange();
        if (currentDocument.get() != null) buildNarrationScriptFromDocument();
        statusMessage.set("Tratamiento de cuadros y tablas: "
                + next.displayName() + ". Reconstruye los fragmentos de audio "
                + "para aplicar el cambio.");
        refreshProjectState();
    }

    public void setImageNarrationPolicy(ImageNarrationPolicy policy) {
        ReadingProfile base = activeReadingProfile.get() == null
                ? ReadingProfile.academicDefaults() : activeReadingProfile.get();
        ImageNarrationPolicy next = policy == null
                ? ImageNarrationPolicy.IGNORE_IMAGES : policy;
        if (base.imagePolicy() == next) return;
        ReadingProfile updated = new ReadingProfile(
                base.id(), base.name(), base.description(), base.headingRules(),
                next, base.tablePolicy());
        activeReadingProfile.set(updated);
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withReadingProfile(updated), true));
        invalidatePersistedAudioForNarrationChange();
        refreshProjectState();
    }

    public void applyPdfListeningMode(PdfListeningMode mode) {
        PdfListeningMode selected = mode == null
                ? PdfListeningMode.ESSENTIAL_READING : mode;
        PreparedPdfSource pdf = currentPreparedPdfSource.get();
        if (pdf != null && selected != PdfListeningMode.ADVANCED_REVIEW) {
            try {
                projectWorkspace().document().openPreparedPdfWorkspace().readingPreferences().update(
                        pdf.workspace(), selected == PdfListeningMode.ESSENTIAL_READING
                                ? com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy.NATIVE_TEXT
                                : com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy.SEMANTIC);
            } catch (IOException failure) {
                statusMessage.set("No se pudo guardar el modo de lectura: " + failure.getMessage());
                return;
            }
        }
        setImageNarrationPolicy(selected.imagePolicy());
        setTableNarrationPolicy(selected.tablePolicy());
        setDocumentListeningPreferences(documentListeningPreferences()
                .withReviewTechnicalElements(selected.automaticDrafts()));
        statusMessage.set("Modo PDF aplicado: " + selected.displayName()
                + ". Las políticas por objeto siguen siendo editables.");
    }

    public void setReadTablesAndTextBoxesForNarration(boolean enabled) {
        setTableNarrationPolicy(enabled
                ? TableNarrationPolicy.READ_ALL
                : TableNarrationPolicy.SKIP);
    }

    public void setReadAfterColonForNarration(boolean enabled) {
        boolean projectionChanged = readAfterColonForNarration.get() != enabled;
        boolean persisted = sessions.activeSession()
                .map(session -> session.project().documentReadAfterColon() == enabled)
                .orElse(true);
        if (!projectionChanged && persisted) { return; }
        readAfterColonForNarration.set(enabled);
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withDocumentReadAfterColon(enabled), true));
        if (!projectionChanged) { refreshProjectState(); return; }
        if (currentDocument.get() == null) { statusMessage.set("Opción de lectura después de dos puntos guardada. Abre un documento para aplicarla."); refreshProjectState(); return; }
        if (audioJobRunning.get()) { cancelActiveAudioJobSilently(); }
        invalidatePersistedAudioForNarrationChange();
        buildNarrationScriptFromDocument();
        statusMessage.set((enabled ? "Se leerá desde después de ':' cuando parezca diálogo." : "Se leerá el texto completo antes y después de ':'.") + " Procesar lectura completa aplicará el cambio.");
        refreshProjectState();
    }

    public void setReadAfterColonForNarration(boolean enabled, boolean renderFromSelection) {
        setReadAfterColonForNarration(enabled);
    }

    public ReadingProfilePreview previewReadingProfile(ReadingProfile profile) {
        ReadableDocument document = currentDocument.get();
        if (document == null) {
            return new ReadingProfilePreview(java.util.List.of());
        }
        return workspaceServices.project().readingProfile().previewReadingProfile().preview(document, profile);
    }

    public void buildNarrationScriptFromDocument() {
        PreparedPdfSource pdf = currentPreparedPdfSource.get();
        if (pdf != null) {
            ProjectSession session = requireSession();
            NarrationScriptDocument script = workspaceServices.project().script()
                    .buildPreparedPdfNarration()
                    .build(pdf.workspace(), pdf.title(), "es",
                            readAfterColonForNarration.get(), activeReadingProfile.get(),
                            documentListeningPreferences()
                                    .secondarySemanticPolicy());
            applyImportedOrGeneratedScript(session, script);
            var issues = documentController.validate(script);
            statusMessage.set(documentController.projectionReadyMessage(script, issues));
            refreshProjectState();
            return;
        }
        ReadableDocument document = currentDocument.get();
        Optional<String> readinessProblem = documentController.readinessProblem(document, activeReadingProfile.get());
        if (readinessProblem.isPresent()) {
            statusMessage.set(readinessProblem.get());
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        ProjectSession session = requireSession();
        ReadingProfile profile = effectiveWordReadingProfile(
                session.project().readingProfile(), documentListeningPreferences());
        NarrationScriptDocument script = documentController.buildNarrationProjection(
                session, document, readAfterColonForNarration.get(), profile);
        applyImportedOrGeneratedScript(session, script);
        var issues = documentController.validate(script);
        statusMessage.set(documentController.projectionReadyMessage(script, issues));
        refreshProjectState();
    }

    public boolean requiresWordSemanticImagePreparation() {
        ReadableDocument document = currentDocument.get();
        if (document == null || document.format()
                != com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat.DOCX
                || !documentListeningPreferences().secondarySemanticPolicy()
                .includes(SecondarySemanticComponentKind.IMAGE)) return false;
        return projectWorkspace().document().prepareWordSemanticImages()
                .requiresPreparation(document,
                        documentListeningPreferences().secondarySemanticPolicy());
    }

    public com.marcosmoreiradev.docupodcaststudio.application.document
            .PrepareWordSemanticImagesUseCase.Result prepareWordSemanticImages()
            throws InterruptedException, IOException {
        return prepareWordSemanticImages(ignored -> { });
    }

    public com.marcosmoreiradev.docupodcaststudio.application.document
            .PrepareWordSemanticImagesUseCase.Result prepareWordSemanticImages(
            com.marcosmoreiradev.docupodcaststudio.application.document
                    .PrepareWordSemanticImagesUseCase.ProgressListener progress)
            throws InterruptedException, IOException {
        return prepareWordSemanticImages("", progress);
    }

    public com.marcosmoreiradev.docupodcaststudio.application.document
            .PrepareWordSemanticImagesUseCase.Result prepareWordSemanticImages(
            String engineId,
            com.marcosmoreiradev.docupodcaststudio.application.document
                    .PrepareWordSemanticImagesUseCase.ProgressListener progress)
            throws InterruptedException, IOException {
        ReadableDocument document = currentDocument.get();
        if (document == null) {
            throw new IOException("No hay un documento Word abierto.");
        }
        Path projectDirectory = currentProjectDirectory().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de describir imagenes Word."));
        var result = projectWorkspace().document().prepareWordSemanticImages().execute(
                document, projectDirectory,
                documentListeningPreferences().secondarySemanticPolicy(),
                currentProject().map(project -> project.metadata().language()).orElse("es"),
                engineId == null || engineId.isBlank() ? null : new com.marcosmoreiradev.docupodcaststudio.media.api.EngineId(engineId),
                progress);
        return result;
    }

    public void applyPreparedWordSemanticImages(
            com.marcosmoreiradev.docupodcaststudio.application.document
                    .PrepareWordSemanticImagesUseCase.Result result) throws IOException {
        if (result == null || !result.changed()) return;
        currentDocument.set(result.document());
        ProjectSession session = requireSession();
        session.setImportedDocument(result.document());
        currentScript.set(null);
        Optional<Path> projectFile = session.projectFile();
        if (projectFile.isPresent()) {
            projectController.saveProject(session, projectFile.get(),
                    activeReadingProfile.get(), activeVoiceLibrary.get());
        }
        refreshProjectState();
    }

    private static ReadingProfile effectiveWordReadingProfile(
            ReadingProfile base, DocumentListeningPreferences preferences) {
        ReadingProfile safe = base == null ? ReadingProfile.academicDefaults() : base;
        var policy = preferences.secondarySemanticPolicy();
        TableNarrationPolicy table = policy.includes(SecondarySemanticComponentKind.TABLE)
                ? (safe.tablePolicy().skips() ? TableNarrationPolicy.SUMMARIZE
                : safe.tablePolicy()) : TableNarrationPolicy.SKIP;
        ImageNarrationPolicy image = policy.includes(SecondarySemanticComponentKind.IMAGE)
                ? ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT
                : ImageNarrationPolicy.IGNORE_IMAGES;
        return new ReadingProfile(safe.id(), safe.name(), safe.description(),
                safe.headingRules(), image, table);
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
        selectedPdfRegion.set(null);
        selectedPdfVisualTarget.set(null);
        selectedDocumentBlockId.set(normalized);
        clearDocumentTextRange();
        if (normalized.isBlank()) { selectedScriptSegmentId.set(""); actualizarFrameActivo();
            setDocumentProcessingScope(DocumentProcessingScope.FULL_DOCUMENT);
            statusMessage.set("Selección de fragmento limpia. Alcance: lectura completa.");
            refreshProjectState(); return; }
        Optional<DocumentFragmentRailPresentation> visual = visualFragmentForSelection(normalized, null); visual.ifPresent(this::pinVisualFragment); Optional<NarrationSegment> linked = visual.flatMap(fragment -> findSegment(fragment.segmentId())).or(() -> firstSegmentForDocumentBlock(normalized));
        if (linked.isPresent()) {
            selectedScriptSegmentId.set(linked.get().id());
            PlaybackManifest manifest = currentPlaybackManifest.get();
            if (manifest != null && !manifest.emptyManifest() && manifest.cueForSegment(linked.get().id()).isPresent()) {
                playbackCursor.set(workspaceServices.playback().playback().seekPlayback().seek(playbackCursor.get(), manifest, linked.get().id()));
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
        selectedPdfRegion.set(null);
        selectedPdfVisualTarget.set(null);
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

    /**
     * Mirrors the currently narrated Word sentence into the shared selection
     * authority without seeking or otherwise mutating the playback transport.
     */
    public void focusDocumentTextRangeDuringPlayback(
            DocumentTextRange range, String selectedText,
            String narrationSegmentId) {
        if (range == null || range.collapsed()) return;
        selectedPdfRegion.set(null);
        selectedPdfVisualTarget.set(null);
        selectedDocumentBlockId.set(range.blockId());
        selectedDocumentTextRange.set(range);
        applySelectionLabels(documentSelectionWorkflow.sentenceSelection(
                range, selectedText, currentDocument.get()));
        String segmentId = Objects.toString(narrationSegmentId, "").strip();
        if (!segmentId.isBlank()) selectedScriptSegmentId.set(segmentId);
        visualFragmentForSelection(range.blockId(), range)
                .ifPresent(this::pinVisualFragment);
        statusMessage.set("Reproduciendo oración en " + range.displayLabel()
                + (segmentId.isBlank() ? "." : " → " + segmentId + "."));
        refreshDocumentInteractionProjection();
    }

    public void selectPdfRegion(com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget target) {
        if (target == null || !target.available()) return;
        selectedDocumentBlockId.set("");
        selectedDocumentTextRange.set(null);
        com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef selection =
                com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef.from(target);
        selectedPdfRegion.set(selection);
        selectedPdfVisualTarget.set(target);
        resolveCurrentDocumentSelection().ifPresent(snapshot -> {
            selectedDocumentRangeLabel.set(snapshot.text().isBlank()
                    ? "Región PDF " + selection.regionId() : snapshot.text());
            selectedDocumentSourceLocation.set("Página " + snapshot.pageNumber()
                    + " · " + snapshot.type() + " · " + snapshot.narratability().name());
        });
        alignPlaybackCursorToPdfSelection(selection);
        statusMessage.set("Región PDF seleccionada en la página " + selection.pageNumber()
                + ". Los paneles usan directamente la preparación PDF V2.");
        refreshProjectState();
    }

    /** Follows playback without seeking it again or announcing a manual selection. */
    public void followPdfPlaybackTarget(
            com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget target) {
        if (target == null || !target.available()) return;
        var selection = com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfRegionSelectionRef.from(target);
        var current = selectedPdfRegion.get();
        var currentTarget = selectedPdfVisualTarget.get();
        if (selection.equals(current) && currentTarget != null
                && currentTarget.id().equals(target.id())) return;
        selectedDocumentBlockId.set("");
        selectedDocumentTextRange.set(null);
        selectedPdfRegion.set(selection);
        selectedPdfVisualTarget.set(target);
        String label = target.text().isBlank()
                ? "Región PDF " + target.regionId() : target.text();
        selectedDocumentRangeLabel.set(label);
        selectedDocumentSourceLocation.set("Página " + target.pageNumber()
                + " · elemento que se está narrando");
    }

    public void requestPdfRegionReview(List<String> regionIds) {
        List<String> pendingRegionIds = regionIds == null ? List.of() : regionIds.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        String firstRegionId = pendingRegionIds.stream().findFirst().orElse("");
        if (firstRegionId.isBlank()) {
            statusMessage.set("No hay contenido dudoso pendiente de revisión.");
            return;
        }
        // Clear first so a repeated request for the same region still notifies the workspace.
        requestedPdfRegionReviewId.set("");
        requestedPdfRegionReviewId.set(firstRegionId);
        statusMessage.set("Revisión de contenido dudoso: se muestra la primera de "
                + pendingRegionIds.size() + " región(es).");
    }

    public void setSelectedPdfRegionNarratability(
            com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability narratability)
            throws IOException {
        PreparedPdfSource source = currentPreparedPdfSource.get();
        com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef
                selection = selectedPdfRegion.get();
        if (source == null || selection == null || narratability == null) {
            throw new IOException("Selecciona primero una región PDF preparada.");
        }
        var keepText = com.marcosmoreiradev.docupodcaststudio.application.document
                .UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.<String>keep();
        var keepType = com.marcosmoreiradev.docupodcaststudio.application.document
                .UpdatePreparedPdfRegionOverrideUseCase.OverrideValue
                .<com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType>keep();
        var setNarratability = com.marcosmoreiradev.docupodcaststudio.application.document
                .UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.set(narratability);
        var keepOrder = com.marcosmoreiradev.docupodcaststudio.application.document
                .UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.<Integer>keep();
        workspaceServices.project().document().updatePreparedPdfRegionOverride()
                .execute(source.workspace(), selection.pageNumber(),
                        selection.regionId(),
                        new com.marcosmoreiradev.docupodcaststudio.application.document
                                .UpdatePreparedPdfRegionOverrideUseCase.OverridePatch(
                                keepText, keepType, setNarratability, keepOrder));
        statusMessage.set(narratability
                == com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability.NARRATABLE
                ? "La región se conservará como texto narrable."
                : "La región se omitirá de la lectura; seguirá visible y buscable.");
        bumpDocumentMediaRevision();
        // Publish a real selection change so all direct PDF V2 projections and
        // the contextual inspector reload the persisted override immediately.
        selectedPdfRegion.set(null);
        selectedPdfRegion.set(selection);
        refreshProjectState();
    }

    public Optional<com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSelectionSnapshot>
    resolveCurrentDocumentSelection() {
        ProjectSession session = sessions.activeSession().orElse(null);
        if (session == null) return Optional.empty();
        var source = session.documentSource();
        if (source.isEmpty()) return Optional.empty();
        if (source.get() instanceof PreparedPdfSource) {
            return workspaceServices.project().document().resolveDocumentSelection()
                    .resolve(source.get(), selectedPdfRegion.get());
        }
        String blockId = selectedDocumentBlockId.get();
        if (blockId == null || blockId.isBlank()) return Optional.empty();
        DocumentTextRange range = selectedDocumentTextRange.get();
        int start = range == null ? 0 : range.startOffset();
        int end = range == null ? Integer.MAX_VALUE : range.endOffset();
        return workspaceServices.project().document().resolveDocumentSelection()
                .resolve(source.get(),
                        new com.marcosmoreiradev.docupodcaststudio.application.document.BlockSelectionRef(
                                blockId, start, end));
    }

    public Optional<com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource>
    currentDocumentSource() {
        return sessions.activeSession().flatMap(ProjectSession::documentSource);
    }

    private void alignPlaybackCursorToPdfSelection(
            com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef selection) {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || selection == null) return;
        script.segments().stream()
                .filter(segment -> segment.metadata().getOrDefault("sourceRegionIds", "")
                        .contains(selection.regionId()))
                .findFirst()
                .ifPresent(segment -> {
                    selectedScriptSegmentId.set(segment.id());
                    PlaybackManifest manifest = currentPlaybackManifest.get();
                    if (manifest != null && !manifest.emptyManifest()) {
                        manifest.cueForSegment(segment.id()).ifPresent(cue ->
                                playbackCursor.set(new PlaybackCursor(cue.segmentId(),
                                        cue.startSeconds(), true)));
                    }
                });
    }
    private void alignPlaybackCursorToSelectedSentence(NarrationSegment segment, DocumentTextRange range, String selectedText) {
        if (segment == null || range == null || playbackTransport.playerPlaying() || playbackTransport.continuationActive()) { return; }
        PlaybackManifest manifest = currentPlaybackManifest.get();
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
        VoiceToneReferenceResolution resolution = workspaceServices.administration().voice().resolveVoiceToneReference().resolve(activeVoiceLibrary.get(), voiceId, requested);
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
        var result = workspaceServices.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, imageFile);
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

    public void importImageForSegment(String segmentId, Path imageFile, NarrativeLayerKind imageKind) throws IOException { selectNarrativeVisualFragment(segmentId); importImageForSelectedDocumentRange(imageFile, imageKind); openNarrativeVideoProduction(); }

    public void generateNarrativeImageForSegment(String segmentId) throws IOException { ProjectSession session = requireSession(); NarrationSegment segment = findSegment(segmentId).orElseThrow(() -> new IOException("No se encontro el fragmento narrativo solicitado.")); var generated = narrativeImageGenerationWorkflow.generate(session, segment, workspaceServices.administration().settings().loadOperationalSettings().load(), message -> statusMessage.set(message == null || message.isBlank() ? "Generando imagen narrativa." : message)); selectNarrativeVisualFragment(segment.id()); lastStoryboardImageAssetId.set(generated.importResult().imageAsset().id()); removeExistingLayerOfKindSilently(NarrativeLayerKind.IMAGE); Optional<NarrativeLayerCoordinator.AssignmentOutcome> assignment = assignDocumentLayerTarget(NarrativeLayerKind.IMAGE, generated.importResult().imageAsset().id()); assignment.ifPresent(outcome -> { refreshStoryboardFromImageLayers(session); statusMessage.set(generated.message() + " " + outcome.message()); }); openNarrativeVideoProduction(); bumpDocumentMediaRevision(); }

    public void removeImageAssignmentForSegment(String segmentId, NarrativeLayerKind imageKind) { selectNarrativeVisualFragment(segmentId); if (imageKind == NarrativeLayerKind.BRIDGE_IMAGE) removeAssignmentOfKindForSelectedDocumentRange(NarrativeLayerKind.BRIDGE_IMAGE); else removeImageAssignmentForSelectedDocumentRange(); openNarrativeVideoProduction(); }

    public void selectNarrativeVisualFragment(String segmentId) { selectDocumentBlockForStoryboardSegment(segmentId); openNarrativeVideoProduction(); }

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
        var importResult = workspaceServices.generation().media().importUserMediaAsset().importMedia(session.project(), projectFile, mediaFile);
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
        // FAST_LISTEN always audits acoustic coverage for the complete
        // narration. An explicit selection only chooses the preferred first
        // cue; it must never bypass generation of the remaining gaps.
        listenToDocument();
    }

    /** Starts a freshly materialized PDF reading at the requested page. */
    public void runPreparedPdfPrimaryActionFromPage(int page) {
        if (currentPreparedPdfSource.get() == null) {
            runDocumentPrimaryAction();
            return;
        }
        firstPreparedPdfSegmentAtOrAfterPage(currentScript.get(),
                Math.max(1, page)).ifPresent(segment ->
                selectedScriptSegmentId.set(segment.id()));
        var selectedRegion = selectedPdfRegion.get();
        if (selectedRegion != null && selectedRegion.pageNumber() >= page) {
            alignPlaybackCursorToPdfSelection(selectedRegion);
        }
        runDocumentPrimaryAction();
    }

    public void listenToDocument() {
        Optional<NarrationSegment> preferredStart =
                explicitDocumentAudioSelection();
        ReadableDocument document = currentDocument.get();
        boolean preparedPdf = currentPreparedPdfSource.get() != null;
        if (preparedPdf && (currentScript.get() == null || currentScript.get().empty())) {
            buildNarrationScriptFromDocument();
            if (currentScript.get() == null || currentScript.get().empty()) {
                statusMessage.set("El PDF todavía no tiene regiones NARRATABLE preparadas. "
                        + "Haz clic en una página o prepara el alcance que deseas escuchar.");
                activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
                return;
            }
        }
        DocumentListenPlan plan = preparedPdf ? DocumentListenPlan.playExistingAudio()
                : documentController.listeningPlan(
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
        if (preferredStart.isEmpty()) {
            preferredStart = explicitDocumentAudioSelection();
        }
        PlaybackManifest manifest = ensurePlaybackManifestLoaded();
        boolean completeDocumentAudio = hasCompleteAudioCoverageForDocument();
        if (manifest != null && !manifest.emptyManifest() && completeDocumentAudio) {
            Optional<NarrationSegment> segment = preferredStart.or(() ->
                    currentScript.get() == null ? Optional.empty()
                            : currentScript.get().segments().stream()
                            .filter(NarrationSegment::narratable).findFirst());
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
            statusMessage.set(preparedPdf
                    ? "El audio del PDF sigue generándose; se reproducirá cuando haya suficiente búfer."
                    : documentController.listeningPlan(document, currentScript.get(), buffered, true,
                    currentProjectFile().isPresent(), activeAudioJobStatus.get(),
                    playbackBufferPolicy).userMessage());
            refreshProjectState();
            return;
        }
        if (currentProjectFile().isEmpty()) {
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            statusMessage.set(DocumentListenPlan.saveProjectRequired().userMessage());
            refreshProjectState();
            return;
        }
        waitingForBufferedSegmentAfter = "";
        DocumentPlaybackIntent playbackIntent = preferredStart
                .map(segment -> DocumentPlaybackIntent.forAction(
                        DocumentAudioAction.FAST_LISTEN,
                        DocumentProcessingScope.FULL_DOCUMENT,
                        segment.id(), true, false))
                .orElseGet(DocumentPlaybackIntent::fromBeginning);
        submitAudioGeneration(playbackIntent);
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
        PlaybackManifest manifest = currentPlaybackManifest.get();
        if (manifest != null && !manifest.emptyManifest()) {
            playbackCursor.set(workspaceServices.playback().playback().seekPlayback().seek(
                    playbackCursor.get(), manifest, segment.get().id()));
        }
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
            submitAudioGenerationFromSegment(segment.get(), true, 1);
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
        RecordingActionPlan plan = workspaceServices.playback().recording().prepareRecordingAction().prepare(RecordingPurpose.HUMAN_VOICE_FOR_TEXT, range.get());
        statusMessage.set(plan.userMessage() + " Archivo sugerido: " + plan.suggestedFileName() + "."); refreshProjectState();
    }

    public java.util.List<AudioInputDevice> audioInputDevices() { return manualInterventionAudioWorkflow.inputDevices(workspaceServices); }

    public void startManualInterventionRecording(String blockId, String displayName, String preview, String inputDeviceId) throws IOException { if (voiceRecordingRunning.get() || manualAudioRecordingRunning.get() || workspaceServices.playback().recording().stopAudioRecording().recording()) { statusMessage.set("Ya hay una grabacion activa. Detenla o cancelala antes de iniciar otra."); return; } NarrationSegment segment = manualRecordingSegment(blockId).orElseThrow(() -> new IOException("No se encontro un segmento narrable para esa intervencion.")); Path output = manualInterventionAudioWorkflow.startRecording(workspaceServices, requireSession(), segment, inputDeviceId); activeManualAudioSegmentId = segment.id(); manualAudioRecordingRunning.set(true); selectedScriptSegmentId.set(segment.id()); if (blockId != null && !blockId.isBlank()) selectedDocumentBlockId.set(blockId.strip()); statusMessage.set("Grabando audio manual para " + ManualInterventionAudioWorkflow.displayName(displayName, segment) + ": " + output.getFileName() + "."); refreshProjectState(); }

    public Path stopManualInterventionRecordingDraft(String blockId, String displayName) throws IOException { if (!manualAudioRecordingRunning.get() && !workspaceServices.playback().recording().stopAudioRecording().recording()) { statusMessage.set("No hay una grabacion manual activa."); throw new IOException("No hay una grabacion manual activa."); } NarrationSegment segment = manualRecordingSegment(blockId).or(() -> findSegment(activeManualAudioSegmentId)).orElseThrow(() -> new IOException("No se encontro el segmento de la grabacion manual.")); Path recorded; try { recorded = workspaceServices.playback().recording().stopAudioRecording().stop(); } finally { manualAudioRecordingRunning.set(false); activeManualAudioSegmentId = ""; } statusMessage.set("Borrador grabado para " + ManualInterventionAudioWorkflow.displayName(displayName, segment) + ": " + recorded.getFileName() + "."); refreshProjectState(); return recorded; }

    public void assignManualInterventionRecording(String blockId, String displayName, Path recorded) throws IOException { if (recorded == null || !java.nio.file.Files.isRegularFile(recorded)) { throw new IOException("No hay borrador WAV valido para asignar."); } NarrationSegment segment = manualRecordingSegment(blockId).or(() -> findSegment(activeManualAudioSegmentId)).orElseThrow(() -> new IOException("No se encontro el segmento para asignar audio manual.")); applyManualAudioSnapshot(manualInterventionAudioWorkflow.applyRecording(workspaceServices, audioWorkflow, playableAudioJobSelector, requireSession(), currentScript.get(), activeAudioJobStatus.get().jobId(), segment, displayName, recorded), "Audio manual aplicado a " + segment.id() + ". Playback y exportacion usaran " + segment.id() + "-manual.wav."); }

    public void stopManualInterventionRecording(String blockId, String displayName, String preview) throws IOException { Path recorded = stopManualInterventionRecordingDraft(blockId, displayName); assignManualInterventionRecording(blockId, displayName, recorded); }

    public void cancelManualInterventionRecording() throws IOException { if (!manualAudioRecordingRunning.get() && !workspaceServices.playback().recording().stopAudioRecording().recording()) { statusMessage.set("No hay grabacion manual para cancelar."); return; } workspaceServices.playback().recording().cancelAudioRecording().cancel(); manualAudioRecordingRunning.set(false); activeManualAudioSegmentId = ""; statusMessage.set("Grabacion manual cancelada. No se reemplazo ningun fragmento de audio."); refreshProjectState(); }

    public void playManualInterventionAudio(String blockId) throws IOException { NarrationSegment segment = manualRecordingSegment(blockId).orElseThrow(() -> new IOException("No se encontro un segmento narrable para esa intervencion.")); Path projectDirectory = currentProjectDirectory().orElseThrow(() -> new IOException("Guarda el proyecto antes de escuchar audio manual.")); Path audio = manualInterventionAudioWorkflow.playableAudioForSegment(audioWorkflow, playableAudioJobSelector, projectDirectory, currentScript.get(), activeAudioJobStatus.get().jobId(), segment.id()); playbackTransport.playStandalone(audio, 0.0); statusMessage.set("Reproduciendo audio de " + segment.id() + ": " + audio.getFileName() + "."); }

    public void playStandaloneAudio(Path audio) throws IOException { if (audio == null || !java.nio.file.Files.isRegularFile(audio)) { throw new IOException("No existe el audio para reproducir."); } playbackTransport.playStandalone(audio, 0.0); statusMessage.set("Reproduciendo " + audio.getFileName() + "."); }

    public void deleteManualInterventionAudio(String blockId, String displayName) throws IOException { NarrationSegment segment = manualRecordingSegment(blockId).orElseThrow(() -> new IOException("No se encontro un segmento narrable para esa intervencion.")); applyManualAudioSnapshot(manualInterventionAudioWorkflow.deleteManualAudio(workspaceServices, audioWorkflow, playableAudioJobSelector, requireSession(), currentScript.get(), activeAudioJobStatus.get().jobId(), segment, displayName), "Audio manual eliminado de " + segment.id() + ". Se restauro el WAV generado si existia; si no, quedo pendiente."); }

    public boolean canRegenerateSelectedTheatreInterventionAudio() { return currentProjectMode.get() == ProjectMode.THEATRE_PRODUCTION && !audioJobRunning.get() && !choralVoiceRenderingProperty().get() && !selectedDocumentBlockId.get().isBlank() && currentDocument.get() != null; }

    public void regenerateSelectedTheatreInterventionAudio() {
        try { if (currentScript.get() == null || currentScript.get().empty()) buildNarrationScriptFromDocument(); NarrationSegment segment = manualRecordingSegment(selectedDocumentBlockId.get()).orElseThrow(() -> new IOException("Selecciona una intervencion narrable antes de regenerar su audio.")); ProjectSession session = requireSession(); Path file = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de regenerar audio.")); if (audioEngineUnavailableForGeneration()) throw new IOException(audioEngineUnavailableMessage()); saveCurrentProjectAs(file); audioJobRunning.set(true); statusMessage.set("Renderizando de nuevo el audio de " + segment.id() + "...");
            theatreInterventionAudioRegenerationWorkflow.start(workspaceServices, audioWorkflow, playableAudioJobSelector, session, currentScript.get(), activeAudioJobStatus.get().jobId(), segment,
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
            Optional<PlaybackCue> cue = playbackController.preferredStartCue(manifest, pending);
            if (cue.isPresent()) {
                return cue;
            }
        }
        return playbackController.preferredStartCue(manifest, selectedDocumentSegmentOrSelected());
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
                : workspaceServices.administration().voice().importVoiceSample().storesSamplesOutsideProject() ? workspaceServices.administration().voice().importVoiceSample().effectiveProjectFile(null) : null;
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
        if (voiceRecordingRunning.get() || manualAudioRecordingRunning.get() || workspaceServices.playback().recording().stopAudioRecording().recording()) { statusMessage.set("Ya hay una grabacion activa. Detenla antes de iniciar otra."); return; }
        VoiceReferenceTone targetTone = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        VoiceProfile targetVoice = voice == null ? activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null) : voice;
        VoiceToneRecordingPlan tonePlan = voiceToneRecordingPlan(targetVoice, targetTone);
        Path output = voiceSampleWorkflow.startRecording(voiceLibraryProjectFileOrNull(), tonePlan, inputDeviceId);
        activeVoiceRecordingFile = output; activeVoiceRecordingVoiceId = targetVoice == null ? "" : targetVoice.id(); activeVoiceRecordingTone = targetTone; voiceRecordingRunning.set(true); activeWorkspace.set(WorkspaceKind.VOICE_LIBRARY);
        statusMessage.set("Grabando muestra para " + (targetVoice == null ? "la voz seleccionada" : targetVoice.displayName()) + " · emoción " + targetTone.displayName() + ": " + output.getFileName() + ". Lee la frase guía y pulsa Detener y guardar."); refreshProjectState();
    }

    public void stopOwnVoiceRecording() throws IOException {
        if (!voiceRecordingRunning.get() && !workspaceServices.playback().recording().stopAudioRecording().recording()) { statusMessage.set("No hay una grabación de voz activa."); return; }
        VoiceReferenceTone completedTone = activeVoiceRecordingTone == null ? VoiceReferenceTone.NEUTRAL : activeVoiceRecordingTone;
        Path recorded;
        try { recorded = workspaceServices.playback().recording().stopAudioRecording().stop(); } finally { voiceRecordingRunning.set(false); activeVoiceRecordingFile = null; activeVoiceRecordingTone = VoiceReferenceTone.NEUTRAL; }
        VoiceProfile recordedVoice = activeVoiceLibrary.get().voiceById(activeVoiceRecordingVoiceId).orElseGet(() -> activeVoiceLibrary.get().voiceById("VOC-OWN-PLACEHOLDER").orElse(null)); activeVoiceRecordingVoiceId = "";
        importVoiceSampleInternal(recorded, (recordedVoice == null ? "Voz avanzada" : recordedVoice.displayName()) + " — muestra " + completedTone.displayName().toLowerCase(java.util.Locale.ROOT), voiceToneRecordingPlan(recordedVoice, completedTone), true);
        statusMessage.set("Grabación detenida y registrada como muestra de voz · tono " + completedTone.displayName() + ": " + recorded.getFileName() + "."); refreshProjectState();
    }

    public void cancelOwnVoiceRecording() throws IOException {
        if (!voiceRecordingRunning.get() && !workspaceServices.playback().recording().stopAudioRecording().recording()) { statusMessage.set("No hay una grabación de voz activa para cancelar."); return; }
        workspaceServices.playback().recording().cancelAudioRecording().cancel();
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
            NarrationScriptDocument updated = workspaceServices.administration().voice().assignVoiceToSegment()
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
        return workspaceServices.administration().voice().voiceCapabilityPolicy()
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
        java.util.List<String> issues = workspaceServices.administration().voice().validateVoiceLibrary().validate(activeVoiceLibrary.get());
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
        StoryboardDocument storyboard = workspaceServices.generation().storyboard().buildStoryboardFromScript().build(script);
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
        var result = workspaceServices.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, imageFile);
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
        ExampleVisualBindingWorkflow.Result result = exampleVisualBindingWorkflow.bind(workspaceServices, session, projectFile, currentDocument.get(), currentScript.get(), visualAssets, bindings);
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
        if (storyboard == null) storyboard = workspaceServices.generation().storyboard().buildStoryboardFromScript().build(script);
        try {
            StoryboardDocument updatedStoryboard = workspaceServices.generation().storyboard().bindImageToSegment().bind(
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
                ? workspaceServices.generation().storyboard().validateStoryboard().validate(storyboard, script, assets)
                : java.util.List.of();
        final StoryboardDocument storyboardForImageLayers = storyboard;
        StoryboardDocument effectiveStoryboard = active
                .map(session -> workspaceServices.generation().storyboard().buildStoryboardFromImageLayers().build(
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

    public void saveTheatreStoryboardFrame(String segmentId, Path framePng, String inkStateJson, boolean activateDrawn,
                                           java.util.List<com.marcosmoreiradev.docupodcaststudio.application.storyboard.TheatreDrawingVaultItem> drawingVault) throws IOException {
        applyTheatreFrameResult(theatreStoryboardFrameWorkflow.save(workspaceServices, requireSession(), currentStoryboard.get(),
                currentScript.get(), segmentId, framePng, inkStateJson, activateDrawn, drawingVault, currentProjectDirectory()));
    }

    public void toggleTheatreStoryboardFrameVariant(String segmentId) { try { applyTheatreFrameResult(theatreStoryboardFrameWorkflow.toggle(workspaceServices, requireSession(), currentStoryboard.get(), currentScript.get(), segmentId, currentProjectDirectory())); } catch (IOException | RuntimeException ex) { statusMessage.set("No se pudo alternar variante visual: " + ex.getMessage()); } }

    public TheatreAudioTrackWorkflow.Result saveTheatreAudioTrack(String trackId, String segmentId, String interventionId, PreparedAudioAsset source, double from, TheatreProjectLayer.AudioTrackEndMode endMode, double to, double volume, boolean fade, boolean replace) throws IOException { var result = theatreAudioTrackWorkflow.save(workspaceServices, requireSession(), currentScript.get(), currentPlaybackManifest.get(), trackId, segmentId, interventionId, source, from, endMode, to, volume, fade, replace); if (result.saved()) saveCurrentProjectAs(currentProjectFile().orElseThrow()); statusMessage.set(result.message()); bumpDocumentMediaRevision(); refreshProjectState(); return result; }

    public TheatreAudioTrackWorkflow.Result confirmTheatreAudioTrackReplacement(TheatreProjectLayer.TheatreAudioTrack track) { var result = theatreAudioTrackWorkflow.saveCandidate(workspaceServices, requireSession(), currentScript.get(), currentPlaybackManifest.get(), track, true); statusMessage.set(result.message()); bumpDocumentMediaRevision(); refreshProjectState(); return result; }

    public void removeTheatreAudioTrack(String trackId) { try { statusMessage.set(theatreAudioTrackWorkflow.remove(workspaceServices, requireSession(), trackId).message()); saveCurrentProjectAs(currentProjectFile().orElseThrow()); } catch (IOException ex) { throw new IllegalStateException(ex.getMessage(), ex); } bumpDocumentMediaRevision(); refreshProjectState(); }

    public void activateTheatreStoryboardVisualVariant(String segmentId, String variant) { try { applyTheatreFrameResult(theatreStoryboardFrameWorkflow.activate(workspaceServices, requireSession(), currentStoryboard.get(), currentScript.get(), segmentId, variant, currentProjectDirectory())); } catch (IOException | RuntimeException ex) { statusMessage.set("No se pudo activar la variante visual: " + ex.getMessage()); } }
    public void materializeTheatreSceneryVisualVariant(String segmentId) { try { applyTheatreFrameResult(theatreStoryboardFrameWorkflow.materializeScenery(workspaceServices, requireSession(), currentStoryboard.get(), currentScript.get(), segmentId, currentProjectDirectory())); } catch (IOException | RuntimeException ex) { statusMessage.set("No se pudo crear la variante de personajes y escenografía: " + ex.getMessage()); } }
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
        java.util.List<StoryboardValidationIssue> issues = workspaceServices.generation().storyboard().validateStoryboard()
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

    public void selectDocumentAudioSource(String engineId) {
        try {
            if (engineId == null || engineId.equalsIgnoreCase(audioEngineDescriptor().engineId())) return;
            if (audioJobRunning.get()) {
                reportUserVisibleError("Espera a que termine el audio actual o cancélalo antes de cambiar el motor.");
                return;
            }
            refreshChunksAfterDocumentVoiceChange(audioWorkflow.selectDocumentAudioSource(engineId));
        }
        catch (IOException | RuntimeException ex) { reportUserVisibleError("No se pudo cambiar el origen de voz: " + ex.getMessage()); }
    }

    public void useVoiceForDocument(VoiceProfile voice) {
        useVoiceForDocument(voice, configuredDocumentVoiceTone());
    }

    public void useVoiceForDocument(VoiceProfile voice, VoiceReferenceTone tone) {
        if (!configureVoiceForDocument(voice, tone)) return;
        statusMessage.set("Regenerando fragmentos de audio del documento; "
                + "las voces específicas se respetan.");
        generateAudioChunksWithoutPlayback();
    }

    /**
     * Applies the acoustic default immediately without starting a render.
     * Generation entry points consume this persisted selection later.
     */
    public boolean configureVoiceForDocument(
            VoiceProfile voice, VoiceReferenceTone tone) {
        if (voice == null) return false;
        try {
            VoiceReferenceTone requested = tone == null
                    ? VoiceReferenceTone.NEUTRAL : tone;
            String previousVoiceId = configuredVoiceProfileId();
            VoiceReferenceTone previousTone = configuredDocumentVoiceTone();
            VoiceReferenceTone resolvedTone;
            String toneMessage;
            if (!voice.hasSample()) {
                resolvedTone = VoiceReferenceTone.NEUTRAL;
                toneMessage = "El narrador predeterminado usará la voz nativa del motor; "
                        + "no requiere una muestra de referencia.";
            } else {
                VoiceToneReferenceResolution resolution = workspaceServices.administration()
                        .voice().resolveVoiceToneReference()
                        .resolve(activeVoiceLibrary.get(), voice.id(), requested);
                if (!resolution.available()) {
                    documentVoiceToneStatus.set(resolution.userMessage());
                    reportUserVisibleError(resolution.userMessage());
                    return false;
                }
                resolvedTone = resolution.resolvedTone();
                toneMessage = toneStatus(resolution);
            }
            String result = audioWorkflow.useVoiceForDocument(voice);
            rememberDocumentDefaultVoiceAndTone(voice, resolvedTone);
            documentVoiceToneStatus.set(toneMessage);
            boolean changed = !voice.id().equals(previousVoiceId)
                    || resolvedTone != previousTone;
            if (changed) invalidatePersistedAudioAfterVoiceSelection();
            statusMessage.set(result + " Tono global: " + resolvedTone.displayName()
                    + ". Configuración activa; el audio se actualizará al generar."
                    + (changed ? " La cobertura anterior ya no se reutilizará." : ""));
            refreshProjectState();
            return true;
        }
        catch (IOException | RuntimeException ex) {
            reportUserVisibleError("No se pudo usar la voz seleccionada: " + ex.getMessage());
            return false;
        }
    }

    public void useVoiceForDocumentFrom(VoiceProfile voice, String blockId) {
        useVoiceForDocumentFrom(voice, configuredDocumentVoiceTone(), blockId);
    }

    public void useVoiceForDocumentFrom(VoiceProfile voice, VoiceReferenceTone tone,
                                        String blockId) {
        try {
            VoiceReferenceTone requested = tone == null
                    ? VoiceReferenceTone.NEUTRAL : tone;
            if (!voice.hasSample()) {
                audioWorkflow.useVoiceForDocument(voice);
                rememberDocumentDefaultVoiceAndTone(voice, VoiceReferenceTone.NEUTRAL);
                documentVoiceToneStatus.set("El narrador predeterminado usará la voz nativa del motor; no requiere una muestra de referencia.");
                invalidatePersistedAudioAfterVoiceSelection();
                if (currentDocument.get() != null && blockId != null && !blockId.isBlank()) {
                    Optional<NarrationSegment> segment = firstSegmentForDocumentBlock(blockId);
                    if (segment.isPresent()) {
                        submitAudioGenerationFromSegment(segment.get(), false);
                        return;
                    }
                }
                generateAudioChunksWithoutPlayback();
                return;
            }
            VoiceToneReferenceResolution resolution = workspaceServices.administration()
                    .voice().resolveVoiceToneReference()
                    .resolve(activeVoiceLibrary.get(), voice.id(), requested);
            if (!resolution.available()) {
                documentVoiceToneStatus.set(resolution.userMessage());
                reportUserVisibleError(resolution.userMessage());
                return;
            }
            audioWorkflow.useVoiceForDocument(voice);
            rememberDocumentDefaultVoiceAndTone(voice, resolution.resolvedTone());
            documentVoiceToneStatus.set(toneStatus(resolution));
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

    public String configuredVoiceProfileId() {
        return sessions.activeSession()
                .map(ProjectSession::project)
                .map(DocuPodcastProject::documentDefaultVoiceProfileId)
                .filter(voiceId -> !voiceId.isBlank())
                .orElseGet(audioWorkflow::configuredVoiceProfileId);
    }

    public VoiceReferenceTone configuredDocumentVoiceTone() {
        return sessions.activeSession().map(ProjectSession::project)
                .map(DocuPodcastProject::documentDefaultVoiceToneId)
                .flatMap(VoiceReferenceTone::fromLayerTargetId)
                .orElse(VoiceReferenceTone.NEUTRAL);
    }

    public long specificDocumentVoiceOverrideCount() {
        return sessions.activeSession().map(ProjectSession::project)
                .map(project -> project.narrativeLayerAssignments().stream()
                        .filter(assignment -> assignment.kind() == NarrativeLayerKind.VOICE
                                || assignment.kind() == NarrativeLayerKind.EMOTION)
                        .map(assignment -> assignment.textRange().segmentId())
                        .distinct().count())
                .orElse(0L);
    }

    private void rememberDocumentDefaultVoice(VoiceProfile voice) {
        if (voice == null || voice.id() == null || voice.id().isBlank()) return;
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withDocumentDefaultVoiceProfileId(voice.id()), true));
    }

    private void rememberDocumentDefaultVoiceAndTone(
            VoiceProfile voice, VoiceReferenceTone tone) {
        if (voice == null || voice.id() == null || voice.id().isBlank()) return;
        VoiceReferenceTone safeTone = tone == null
                ? VoiceReferenceTone.NEUTRAL : tone;
        sessions.activeSession().ifPresent(session -> session.replaceProject(
                session.project().withDocumentDefaultVoiceProfileId(voice.id())
                        .withDocumentDefaultVoiceToneId(safeTone.layerTargetId()), true));
    }
    private void refreshChunksAfterDocumentVoiceChange(String result) {
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        managedAudioChunksAvailable.set(false);
        refreshFullDocumentReadingReadiness();
        statusMessage.set(result + " Cambio pendiente de procesar. Usa Generar o Reprocesar lectura completa."
                + " Los audios guardados y las asignaciones de voz se conservan."
                + (administrationWorkspace().voice().voiceCapabilityPolicy().activeEngineProfile(audioEngineDescriptor()).simpleLocalMode()
                ? " El motor usará automáticamente su voz predeterminada; las voces y tonos personalizados quedan inactivos." : ""));
        refreshProjectState();
    }
    private boolean invalidatePersistedAudioAfterVoiceSelection() {
        Optional<Path> file = currentProjectFile();
        if (file.isEmpty()) { resetPlaybackState(); return false; }
        if (currentDocument.get() == null) deletePersistedAudioAsync(file.get().toAbsolutePath().normalize().getParent(), "Audio anterior eliminado tras cambiar la voz.");
        else {
            resetPlaybackState();
            activeAudioJobStatus.set(AudioJobStatusDto.idle());
            managedAudioChunksAvailable.set(false);
            refreshFullDocumentReadingReadiness();
        }
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

    /** Starts a bounded producer/consumer window for canonical PDF pages and page-sized TTS jobs. */
    public void beginIncrementalPdfAudio(int startPage, int endPage, boolean autoPlay) {
        incrementalPdfAudioActive = true;
        incrementalPdfTranslationPreparing = false;
        incrementalPdfPreparationComplete = false;
        incrementalPdfAutoPlay = autoPlay;
        incrementalPdfStartPage = Math.max(1, startPage);
        incrementalPdfEndPage = Math.max(incrementalPdfStartPage, endPage);
        incrementalPdfActiveAudioPage = 0;
        incrementalPdfAudioJobId = "";
        incrementalPdfAudioBacklog.clear();
        incrementalPdfObservedPages.clear();
        incrementalPdfFailedAudioPages.clear();
        incrementalPdfRequestedNanos = System.nanoTime();
        incrementalPdfFirstPageNanos = 0L;
        incrementalPdfFirstSegmentsNanos = 0L;
        incrementalPdfFirstAudioNanos = 0L;
        incrementalPdfFirstPlaybackNanos = 0L;
        if (autoPlay) {
            documentPlaybackRequested = true;
        }
        statusMessage.set("Preparando página PDF " + incrementalPdfStartPage
                + "; el audio comenzará por página sin esperar el documento completo.");
    }

    /** Receives only pages accepted and atomically published by the PDF preparation use case. */
    public void acceptPreparedPdfPageForIncrementalAudio(int pageNumber) {
        if (!incrementalPdfAudioActive || pageNumber < incrementalPdfStartPage
                || pageNumber > incrementalPdfEndPage
                || !incrementalPdfObservedPages.add(pageNumber)) return;
        PreparedPdfSource source = currentPreparedPdfSource.get();
        if (source == null) return;
        if (incrementalPdfFirstPageNanos == 0L) {
            incrementalPdfFirstPageNanos = System.nanoTime();
        }
        try {
            var builder = workspaceServices.project().script().buildPreparedPdfNarration();
            NarrationScriptDocument pageScript = builder.buildPage(
                    source.workspace(), pageNumber, source.title(), "es",
                    readAfterColonForNarration.get(), activeReadingProfile.get(),
                    documentListeningPreferences().secondarySemanticPolicy());
            NarrationScriptDocument merged = builder.mergePage(
                    currentScript.get(), pageScript, pageNumber);
            applyIncrementalPdfScript(merged);
            if (!pageScript.empty() && incrementalPdfFirstSegmentsNanos == 0L) {
                incrementalPdfFirstSegmentsNanos = System.nanoTime();
            }
            if (incrementalPdfAudioBacklog.size() < MAX_INCREMENTAL_PDF_AUDIO_BACKLOG) {
                incrementalPdfAudioBacklog.add(pageNumber);
            }
            prepareIncrementalPdfPlaybackIntent();
            statusMessage.set("Página PDF " + pageNumber
                    + " aceptada; preparando sus fragmentos de voz.");
            drainIncrementalPdfAudio();
        } catch (RuntimeException failure) {
            incrementalPdfFailedAudioPages.add(pageNumber);
            statusMessage.set("La página " + pageNumber
                    + " quedó preparada, pero no pudo proyectarse a audio: "
                    + rootCauseMessage(failure));
        }
    }

    /** Reconciles the final canonical script; later pages keep using page-sized audio jobs. */
    public void completeIncrementalPdfAudioPreparation(int preferredPage, boolean autoPlay) {
        if (!incrementalPdfAudioActive) {
            beginIncrementalPdfAudio(Math.max(1, preferredPage),
                    Math.max(1, preferredPage), autoPlay);
        }
        incrementalPdfPreparationComplete = true;
        incrementalPdfAutoPlay |= autoPlay;
        PreparedPdfSource source = currentPreparedPdfSource.get();
        if (source != null) {
            try {
                NarrationScriptDocument canonical = workspaceServices.project().script()
                        .buildPreparedPdfNarration().build(
                                source.workspace(), source.title(), "es",
                                readAfterColonForNarration.get(), activeReadingProfile.get(),
                                documentListeningPreferences().secondarySemanticPolicy());
                NarrationScriptDocument current = currentScript.get();
                if (current != null && !current.empty()) {
                    canonical = new NarrationScriptDocument(
                            current.id(), canonical.title(), canonical.language(),
                            canonical.sourceDocumentTitle(), canonical.segments(),
                            current.createdAt(), java.time.Instant.now(), current.notes());
                }
                applyIncrementalPdfScript(canonical);
            } catch (RuntimeException failure) {
                statusMessage.set("La preparación PDF terminó, pero no se pudo reconciliar "
                        + "la narración final: " + rootCauseMessage(failure));
            }
        }
        prepareIncrementalPdfPlaybackIntent();
        drainIncrementalPdfAudio();
    }

    /** Enables buffered playback without declaring the background look-ahead complete. */
    public void startIncrementalPdfPlayback(int preferredPage) {
        if (!incrementalPdfAudioActive) return;
        incrementalPdfAutoPlay = true;
        incrementalPdfStartPage = Math.max(1, preferredPage);
        prepareIncrementalPdfPlaybackIntent();
        drainIncrementalPdfAudio();
    }

    /** Marks only the producer side complete; queued/running page TTS may still finish. */
    public void markIncrementalPdfPreparationComplete() {
        if (!incrementalPdfAudioActive) return;
        incrementalPdfPreparationComplete = true;
        drainIncrementalPdfAudio();
    }

    public String incrementalPdfAudioTimingLabel() {
        return "T0 solicitud; T1 página=" + elapsedMillis(incrementalPdfFirstPageNanos)
                + " ms; T2 segmentos=" + elapsedMillis(incrementalPdfFirstSegmentsNanos)
                + " ms; T3 audio=" + elapsedMillis(incrementalPdfFirstAudioNanos)
                + " ms; T4 playback=" + elapsedMillis(incrementalPdfFirstPlaybackNanos) + " ms";
    }

    private long elapsedMillis(long eventNanos) {
        return eventNanos <= 0L || incrementalPdfRequestedNanos <= 0L
                ? -1L : Math.max(0L, (eventNanos - incrementalPdfRequestedNanos) / 1_000_000L);
    }

    private void applyIncrementalPdfScript(NarrationScriptDocument script) {
        if (script == null) return;
        sessions.activeSession().ifPresent(session -> {
            session.setNarrationScript(script);
            currentScript.set(script);
        });
    }

    private void prepareIncrementalPdfPlaybackIntent() {
        if (!incrementalPdfAutoPlay || currentScript.get() == null
                || currentScript.get().empty()) return;
        Optional<NarrationSegment> preferred = Optional.empty();
        var selected = selectedPdfRegion.get();
        if (selected != null && selected.pageNumber() >= incrementalPdfStartPage
                && selected.pageNumber() <= incrementalPdfEndPage) {
            alignPlaybackCursorToPdfSelection(selected);
            preferred = currentScript.get().segmentById(selectedScriptSegmentId.get());
        }
        preferred = preferred.or(() -> firstPreparedPdfSegmentAtOrAfterPage(
                currentScript.get(), incrementalPdfStartPage));
        preferred.ifPresent(segment -> applyPlaybackIntent(
                DocumentPlaybackIntent.fromSegment(segment.id(), false)));
    }

    private void drainIncrementalPdfAudio() {
        if (!incrementalPdfAudioActive || !incrementalPdfAudioJobId.isBlank()
                || audioJobRunning.get() || incrementalPdfTranslationPreparing) return;
        NarrationScriptDocument effectiveScript = currentScript.get();
        if (effectiveScript != null) {
            Optional<NarrationScriptDocument> cached = adaptNarrationLanguage.fromCache(
                    effectiveScript, documentTranslationPreferences(),
                    currentProjectDirectory().orElse(null));
            if (cached.isEmpty()) {
                incrementalPdfTranslationPreparing = true;
                prepareEffectiveNarrationAsync(effectiveScript, translated -> {
                    incrementalPdfTranslationPreparing = false;
                    drainIncrementalPdfAudio(translated);
                });
                return;
            }
            effectiveScript = cached.get();
        }
        drainIncrementalPdfAudio(effectiveScript);
    }

    private void drainIncrementalPdfAudio(NarrationScriptDocument effectiveScript) {
        if (!incrementalPdfAudioActive || !incrementalPdfAudioJobId.isBlank()
                || audioJobRunning.get()) return;
        Optional<IncrementalPdfAudioBatch> next;
        try {
            next = nextIncrementalPdfAudioBatch(effectiveScript);
        } catch (IOException | RuntimeException failure) {
            statusMessage.set("No se pudo comprobar la cobertura incremental de audio: "
                    + rootCauseMessage(failure));
            return;
        }
        if (next.isEmpty()) {
            PlaybackManifest ready = rebuildPlaybackManifestFromLatestJob();
            if (incrementalPdfAutoPlay && ready != null && !ready.emptyManifest()
                    && !playbackTransport.playerPlaying()
                    && !playbackTransport.sequentialActive()) {
                Optional<NarrationSegment> start = selectedDocumentSegmentOrSelected()
                        .or(() -> firstPreparedPdfSegmentAtOrAfterPage(
                                currentScript.get(), incrementalPdfStartPage));
                start.ifPresent(segment -> startPlaybackFromSegment(segment.id(), false));
                if (playbackTransport.playerPlaying() || playbackTransport.sequentialActive()) {
                    incrementalPdfFirstPlaybackNanos = incrementalPdfFirstPlaybackNanos == 0L
                            ? System.nanoTime() : incrementalPdfFirstPlaybackNanos;
                }
            }
            if (incrementalPdfPreparationComplete) {
                incrementalPdfAudioActive = false;
                statusMessage.set("Audio PDF incremental listo. "
                        + incrementalPdfAudioTimingLabel());
            }
            return;
        }
        IncrementalPdfAudioBatch batch = next.orElseThrow();
        incrementalPdfAudioBacklog.remove(batch.pageNumber());
        incrementalPdfActiveAudioPage = batch.pageNumber();
        audioJobRunning.set(true);
        statusMessage.set("Sintetizando audio de la página " + batch.pageNumber()
                + "; el Modelo de IA puede continuar con la siguiente página.");
        try {
            String jobId = audioWorkflow.submit(batch.request(), status ->
                    Platform.runLater(() -> acceptIncrementalPdfAudioStatus(status)));
            incrementalPdfAudioJobId = jobId;
            activeSubmittedAudioJobId = jobId;
        } catch (RuntimeException failure) {
            audioJobRunning.set(false);
            incrementalPdfFailedAudioPages.add(batch.pageNumber());
            incrementalPdfActiveAudioPage = 0;
            statusMessage.set("No se pudo iniciar TTS para la página "
                    + batch.pageNumber() + ": " + rootCauseMessage(failure));
            drainIncrementalPdfAudio();
        }
    }

    private Optional<IncrementalPdfAudioBatch> nextIncrementalPdfAudioBatch(
            NarrationScriptDocument effectiveScript)
            throws IOException {
        NarrationScriptDocument script = effectiveScript;
        Optional<Path> root = currentProjectDirectory();
        if (script == null || script.empty() || root.isEmpty()) return Optional.empty();
        ProjectSession session = requireSession();
        AudioGenerationRequest full = audioGenerationRequestFor(
                session, script, root.get(), session.title());
        ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                full.generationUnits(), audioWorkflow.persistedJobs(root.get()), root.get());
        Set<String> missing = coverage.missingOrStale().stream()
                .map(entry -> entry.unit().id()).collect(java.util.stream.Collectors.toSet());
        if (missing.isEmpty()) return Optional.empty();
        TreeSet<Integer> candidates = new TreeSet<>(incrementalPdfAudioBacklog);
        script.segments().stream().map(DocuPodcastShellViewModel::pdfSegmentPage)
                .filter(page -> page >= incrementalPdfStartPage && page <= incrementalPdfEndPage)
                .forEach(candidates::add);
        for (int page : candidates) {
            if (incrementalPdfFailedAudioPages.contains(page)) continue;
            Set<String> segmentIds = script.segments().stream()
                    .filter(segment -> pdfSegmentPage(segment) == page)
                    .map(NarrationSegment::id).collect(java.util.stream.Collectors.toSet());
            Set<String> pageUnits = full.generationUnits().stream()
                    .filter(unit -> segmentIds.contains(unit.sourceSegmentId()))
                    .map(com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit::id)
                    .filter(missing::contains)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!pageUnits.isEmpty()) {
                return Optional.of(new IncrementalPdfAudioBatch(page,
                        audioWorkflow.retainGenerationUnits(full, pageUnits)));
            }
        }
        return Optional.empty();
    }

    private void acceptIncrementalPdfAudioStatus(AudioJobStatusDto status) {
        if (status.completedSegments() > 0 && incrementalPdfFirstAudioNanos == 0L) {
            incrementalPdfFirstAudioNanos = System.nanoTime();
        }
        applyAudioStatusOnFxThread(status);
        if ((playbackTransport.playerPlaying() || playbackTransport.sequentialActive())
                && incrementalPdfFirstPlaybackNanos == 0L) {
            incrementalPdfFirstPlaybackNanos = System.nanoTime();
        }
        if (!status.state().terminal() || !status.jobId().equals(incrementalPdfAudioJobId)) {
            return;
        }
        if (!status.completed()) {
            incrementalPdfFailedAudioPages.add(incrementalPdfActiveAudioPage);
        }
        incrementalPdfAudioJobId = "";
        incrementalPdfActiveAudioPage = 0;
        drainIncrementalPdfAudio();
    }

    private static int pdfSegmentPage(NarrationSegment segment) {
        try {
            return Integer.parseInt(segment.metadata().getOrDefault("pdfSourcePage", "0"));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    public ResolvedDocumentProcessingSelection resolveActiveDocumentProcessingSelection() {
        NarrationScriptDocument canonical = currentScript.get();
        if (canonical == null || canonical.empty()) {
            throw new IllegalStateException("No hay narracion canonica para resolver el alcance.");
        }
        return resolveActiveDocumentProcessingSelection(canonical);
    }

    private ResolvedDocumentProcessingSelection resolveActiveDocumentProcessingSelection(
            NarrationScriptDocument canonical) {
        refreshDocumentInteractionProjection();
        DocumentInteractionProjection interaction = documentInteractionProjection.get();
        DocumentProcessingScope scope = interaction.effectiveScope();
        DocumentProcessingInterval interval = scope == DocumentProcessingScope.INTERVAL
                ? validatedDocumentProcessingInterval().orElseThrow(() ->
                new IllegalStateException("El intervalo documental activo no es valido.")) : null;
        String anchor = interaction.preferredNarrationSegmentId();
        if (DocumentInteractionProjection.partial(scope)
                && (!interaction.selectionValid() || anchor.isBlank())) {
            throw new IllegalStateException(
                    "Selecciona un fragmento narrable antes de procesar desde aquí.");
        }
        PreparedPdfSource pdf = currentPreparedPdfSource.get();
        String sourceSha = pdf == null ? "" : pdf.workspace().sourceSha256();
        String sourceDocumentId = pdf == null ? canonical.id()
                : pdf.workspace().sourcePath().toAbsolutePath().normalize().toString();
        ResolvedDocumentProcessingSelection selection =
                resolveDocumentProcessingSelection.execute(canonical, scope, interval,
                        anchor, sourceDocumentId, sourceSha);
        LOGGER.info("document-scope.boundary stage=PROCESSING_SELECTION "
                        + "correlationId={} scope={} interval={}-{} resolvedPages={} "
                        + "canonicalSegmentIds={} translationTarget={}",
                selection.selectionRevision(), selection.scope(),
                selection.requestedStart(), selection.requestedEnd(),
                selection.resolvedPageNumbers(), selection.resolvedSegmentIds(),
                documentTranslationPreferences().enabled()
                        ? documentTranslationPreferences().listeningLanguage().tag()
                        : "source");
        return selection;
    }

    private NarrationScriptDocument activeDocumentScopeNarration(
            NarrationScriptDocument canonical) throws IOException {
        if (currentProjectMode.get() != ProjectMode.DOCUMENTARY_STUDIO) {
            return canonical;
        }
        try {
            return resolveActiveDocumentProcessingSelection(canonical).narration();
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            throw new IOException("No se pudo resolver el alcance documental activo: "
                    + invalid.getMessage(), invalid);
        }
    }

    private record IncrementalPdfAudioBatch(int pageNumber,
                                            AudioGenerationRequest request) { }

    public void generateAudioChunksWithoutPlayback() {
        if (rejectDuplicateCompleteAudioProcessing()) return;
        ReadableDocument document = currentDocument.get();
        boolean preparedPdf = currentPreparedPdfSource.get() != null;
        if (document == null && !preparedPdf) { statusMessage.set("Abre una fuente documental antes de generar fragmentos de audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        documentPlaybackRequested = false; waitingForBufferedSegmentAfter = ""; playSingleCueOnly = false;
        if (preparedPdf) {
            rebuildPreparedPdfNarrationKeepingSelection();
        }
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            buildNarrationScriptFromDocument(); script = currentScript.get();
            if (script == null || script.empty()) { statusMessage.set("No se pudo preparar la lectura para generar fragmentos de audio."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        }
        try {
            ResolvedDocumentProcessingSelection selection =
                    resolveActiveDocumentProcessingSelection(script);
            if (selection.narration().empty()) {
                statusMessage.set("El alcance documental activo no contiene fragmentos narrables.");
                refreshProjectState();
                return;
            }
            submitAudioGeneration(selection.narration(), DocumentPlaybackIntent.none());
        } catch (IllegalArgumentException | IllegalStateException invalidScope) {
            statusMessage.set("No se puede generar audio: " + invalidScope.getMessage());
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
            return;
        }
        if (audioJobRunning.get()) { statusMessage.set("Generando fragmentos de audio sin iniciar reproducción. Puedes ocultar el panel y continuar trabajando."); }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState();
    }

    public void processCompleteReadingWithoutPlayback() {
        if (rejectDuplicateCompleteAudioProcessing()) return;
        ReadableDocument document = currentDocument.get();
        boolean preparedPdf = currentPreparedPdfSource.get() != null;
        if (document == null && !preparedPdf) {
            statusMessage.set("Abre una fuente documental antes de procesar la lectura.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
            return;
        }
        documentPlaybackRequested = false;
        waitingForBufferedSegmentAfter = "";
        playSingleCueOnly = false;
        if (preparedPdf) rebuildPreparedPdfNarrationKeepingSelection();
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            buildNarrationScriptFromDocument();
            script = currentScript.get();
        }
        if (script == null || script.empty()) {
            statusMessage.set("No se pudo preparar la lectura completa.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
            return;
        }
        ResolvedDocumentProcessingSelection selection = fullDocumentSelection(script);
        DocumentTranslationPreferences preferences = documentTranslationPreferences();
        beginLocalDocumentAnalysis("Preparando lectura completa",
                preferences.enabled()
                        ? "Completando la narración en " + preferences.listeningLanguage() + "."
                        : "Verificando la narración canónica completa.");
        prepareEffectiveNarrationAsync(selection.narration(), effective -> {
            endLocalDocumentAnalysis();
            refreshFullDocumentReadingReadiness();
            statusMessage.set("Narración completa preparada. Verificando audio vigente y generando únicamente lo faltante.");
            submitEffectiveAudioGeneration(effective,
                    DocumentPlaybackIntent.forAction(
                            DocumentAudioAction.PROCESS_COMPLETE,
                            DocumentProcessingScope.FULL_DOCUMENT,
                            "", false, false));
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
        }, (stage, progress, message) -> {
            localDocumentAnalysisTitle.set("Preparando lectura completa");
            localDocumentAnalysisDetail.set(message);
            localDocumentAnalysisProgress.set(progress);
        }, failure -> {
            endLocalDocumentAnalysis();
            statusMessage.set("No se pudo completar la lectura: "
                    + rootCauseMessage(failure));
            refreshFullDocumentReadingReadiness();
            refreshProjectState();
        });
    }

    /**
     * Complete-reading generation is non-preemptive. Repeating the same command while
     * its semantic preparation or TTS job is active keeps the current work instead of
     * cancelling it and enqueuing an equivalent replacement.
     */
    private boolean rejectDuplicateCompleteAudioProcessing() {
        if (!audioJobRunning.get() && !localDocumentAnalysisRunning.get()) {
            return false;
        }
        statusMessage.set("La lectura completa ya se está procesando. "
                + "Se mantiene el trabajo actual; no se creó otro lote.");
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        refreshProjectState();
        return true;
    }

    private ResolvedDocumentProcessingSelection fullDocumentSelection(
            NarrationScriptDocument canonical) {
        PreparedPdfSource pdf = currentPreparedPdfSource.get();
        String sourceSha = pdf == null ? "" : pdf.workspace().sourceSha256();
        String sourceDocumentId = pdf == null ? canonical.id()
                : pdf.workspace().sourcePath().toAbsolutePath().normalize().toString();
        return resolveDocumentProcessingSelection.execute(canonical,
                DocumentProcessingScope.FULL_DOCUMENT, null, "",
                sourceDocumentId, sourceSha);
    }

    public void processPdfIntervalWithoutPlayback(DocumentProcessingInterval interval) {
        Objects.requireNonNull(interval, "interval");
        if (currentPreparedPdfSource.get() == null) {
            statusMessage.set("El intervalo por páginas solo está disponible para PDF.");
            refreshProjectState();
            return;
        }
        DocumentProcessingInterval validated = interval.validatedAgainst(
                documentProcessingPageCount());
        documentPlaybackRequested = false;
        waitingForBufferedSegmentAfter = "";
        playSingleCueOnly = false;
        rebuildPreparedPdfNarrationKeepingSelection();
        NarrationScriptDocument canonical = currentScript.get();
        if (canonical == null || canonical.empty()) {
            statusMessage.set("No se pudo preparar narración para el intervalo solicitado.");
            refreshProjectState();
            return;
        }
        ResolvedDocumentProcessingSelection selection = resolveDocumentProcessingSelection.execute(
                canonical, DocumentProcessingScope.INTERVAL, validated, "",
                currentPreparedPdfSource.get().workspace().sourcePath().toString(),
                currentPreparedPdfSource.get().workspace().sourceSha256());
        NarrationScriptDocument intervalScript = selection.narration();
        if (intervalScript.empty()) {
            statusMessage.set("Las páginas " + validated.start() + "-" + validated.end()
                    + " no contienen fragmentos narrables preparados.");
            refreshProjectState();
            return;
        }
        prepareIntervalListeningNarration(selection, validated);
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        refreshProjectState();
    }

    public void processWordIntervalWithoutPlayback(DocumentProcessingInterval interval) {
        Objects.requireNonNull(interval, "interval");
        if (currentDocument.get() == null || currentPreparedPdfSource.get() != null) {
            statusMessage.set("El intervalo por bloques requiere un documento Word activo.");
            refreshProjectState();
            return;
        }
        if (interval.unit() != DocumentProcessingIntervalUnit.BLOCK) {
            statusMessage.set("El intervalo de Word debe expresarse por bloques narrables.");
            refreshProjectState();
            return;
        }
        buildNarrationScriptFromDocument();
        NarrationScriptDocument canonical = currentScript.get();
        if (canonical == null || canonical.empty()) {
            statusMessage.set("No se pudo preparar narración para el intervalo solicitado.");
            refreshProjectState();
            return;
        }
        int blockCount = ResolveDocumentProcessingSelectionUseCase
                .sourceBlocks(canonical.segments()).size();
        DocumentProcessingInterval validated = interval.validatedAgainst(blockCount);
        documentPlaybackRequested = false;
        waitingForBufferedSegmentAfter = "";
        playSingleCueOnly = false;
        ResolvedDocumentProcessingSelection selection =
                resolveDocumentProcessingSelection.execute(
                        canonical, DocumentProcessingScope.INTERVAL, validated, "",
                        canonical.id(), "");
        if (selection.narration().empty()) {
            statusMessage.set("Los bloques " + validated.start() + "-"
                    + validated.end() + " no contienen fragmentos narrables.");
            refreshProjectState();
            return;
        }
        prepareIntervalListeningNarration(selection, validated);
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        refreshProjectState();
    }

    private void prepareIntervalListeningNarration(
            ResolvedDocumentProcessingSelection selection,
            DocumentProcessingInterval interval) {
        String correlationId = "DOCPROC-LISTEN-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16)
                .toUpperCase(java.util.Locale.ROOT);
        DocumentTranslationPreferences preferences = documentTranslationPreferences();
        DocumentListeningPreparationSnapshot before = listeningPreparationSnapshot(
                correlationId, selection, true, preferences);
        LOGGER.info("document-processing.listening correlationId={} stage=READINESS "
                        + "scope={} pages={} segmentIds={} semanticPrepared={} "
                        + "listeningLanguage={} listeningPrepared={} missing={} invalid={}",
                correlationId, selection.scope(), selection.resolvedPageNumbers(),
                selection.resolvedSegmentIds(), before.semanticallyPrepared(),
                before.listeningLanguage(), before.listeningPrepared(),
                before.translation().missingSegmentIds(),
                before.translation().invalidSegmentIds());
        beginLocalDocumentAnalysis("Preparando narración en "
                        + preferences.listeningLanguage(),
                before.listeningPrepared()
                        ? "Verificando narración efectiva del intervalo."
                        : "Traduciendo los fragmentos pendientes del intervalo.");
        prepareEffectiveNarrationAsync(selection.narration(), effective -> {
            DocumentListeningPreparationSnapshot after = listeningPreparationSnapshot(
                    correlationId, selection, true, preferences);
            if (!after.listeningPrepared()) {
                finishIntervalListeningPreparationFailure(correlationId, selection,
                        preferences, new IOException(
                                "La narración para escucha quedó incompleta. missing="
                                        + after.translation().missingSegmentIds() + " invalid="
                                        + after.translation().invalidSegmentIds()));
                return;
            }
            LOGGER.info("document-processing.listening correlationId={} "
                            + "stage=LISTENING_PREPARED terminal=COMPLETED scope={} pages={} "
                            + "segmentIds={} semanticPrepared=true listeningLanguage={} "
                            + "listeningPrepared=true cacheHits={}",
                    correlationId, selection.scope(), selection.resolvedPageNumbers(),
                    selection.resolvedSegmentIds(), after.listeningLanguage(),
                    after.translation().cacheHits());
            endLocalDocumentAnalysis();
            statusMessage.set("Narración para escucha preparada en "
                    + preferences.listeningLanguage() + " para las páginas "
                    + interval.start() + "-" + interval.end() + ".");
            submitEffectiveAudioGeneration(effective, DocumentPlaybackIntent.none());
        }, (stage, progress, message) -> {
            localDocumentAnalysisTitle.set("Traduciendo narración");
            localDocumentAnalysisDetail.set(message);
            localDocumentAnalysisProgress.set(progress);
            LOGGER.info("document-processing.listening correlationId={} "
                            + "stage=TRANSLATION progress={} message={}",
                    correlationId, progress, message);
        }, failure -> finishIntervalListeningPreparationFailure(
                correlationId, selection, preferences, failure));
    }

    private DocumentListeningPreparationSnapshot listeningPreparationSnapshot(
            String correlationId, ResolvedDocumentProcessingSelection selection,
            boolean semanticallyPrepared, DocumentTranslationPreferences preferences) {
        return new DocumentListeningPreparationSnapshot(correlationId, selection,
                semanticallyPrepared, preferences.listeningLanguage().tag(),
                adaptNarrationLanguage.inspectCache(selection.narration(), preferences,
                        currentProjectDirectory().orElse(null)));
    }

    private void finishIntervalListeningPreparationFailure(
            String correlationId, ResolvedDocumentProcessingSelection selection,
            DocumentTranslationPreferences preferences, Throwable failure) {
        DocumentListeningPreparationSnapshot after = listeningPreparationSnapshot(
                correlationId, selection, true, preferences);
        LOGGER.warn("document-processing.listening correlationId={} "
                        + "stage=LISTENING_PREPARATION terminal=FAILED scope={} pages={} "
                        + "segmentIds={} semanticPrepared=true listeningLanguage={} "
                        + "listeningPrepared=false missing={} invalid={} reason={}",
                correlationId, selection.scope(), selection.resolvedPageNumbers(),
                selection.resolvedSegmentIds(), after.listeningLanguage(),
                after.translation().missingSegmentIds(),
                after.translation().invalidSegmentIds(), rootCauseMessage(failure), failure);
        endLocalDocumentAnalysis();
        statusMessage.set("Las páginas están preparadas semánticamente, pero la narración en "
                + preferences.listeningLanguage()
                + " no quedó completa: " + rootCauseMessage(failure));
        documentPlaybackRequested = false;
        refreshProjectState();
    }

    public void submitAudioGenerationWithPendingExport(Runnable exportTask) {
        submitAudioGenerationWithPendingExport(null, null, exportTask);
    }

    public void submitAudioGenerationWithPendingExport(
            DocumentExportReadinessSnapshot readiness, Runnable exportTask) {
        submitAudioGenerationWithPendingExport(readiness, null, exportTask);
    }

    public void submitAudioGenerationWithPendingExport(
            DocumentExportReadinessSnapshot readiness,
            PreparedExportIntent intent,
            Runnable exportTask) {
        submitAudioGenerationWithPendingExport(readiness, intent, exportTask, null);
    }

    public void submitAudioGenerationWithPendingExport(
            DocumentExportReadinessSnapshot readiness, PreparedExportIntent intent,
            Runnable exportTask, Consumer<Throwable> failureHandler) {
        if (pendingDocumentExport != null) {
            statusMessage.set("Ya existe una preparación de exportación en curso.");
            if (failureHandler != null) failureHandler.accept(new IOException("Ya existe una preparación de exportación en curso."));
            return;
        }
        pendingDocumentExportFailure = failureHandler;
        String correlationId = "DOCEXP-REPAIR-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT);
        try {
            NarrationScriptDocument selected = narrationForPreparedExport(intent);
            List<String> selectedScopeIds = selected.segments().stream()
                    .map(NarrationSegment::id).toList();
            if (readiness != null
                    && !selectedScopeIds.equals(readiness.selection().resolvedSegmentIds())) {
                throw new IOException("El alcance documental cambió después de comprobar "
                        + "la exportación. Vuelve a pulsar Exportar.");
            }
            List<String> selectedIds = selected.segments().stream()
                    .filter(NarrationSegment::narratable)
                    .map(NarrationSegment::id).toList();
            pendingDocumentExport = new DocumentExportContinuation(correlationId,
                    selectedIds, intent, exportTask);
            activeDocumentExportCorrelationId = correlationId;
            documentExportCancellationRequested.set(false);
            beginLocalDocumentAnalysis("Preparando exportación",
                    "Resolviendo traducción y audio del alcance seleccionado.");
            LOGGER.info("document-export.workflow correlationId={} readinessCorrelationId={} "
                            + "stage=REPAIR_REQUEST count={} segmentIds={} command={} target={}", correlationId,
                    readiness == null ? "" : readiness.correlationId(),
                    pendingDocumentExport.segmentIds().size(),
                    pendingDocumentExport.segmentIds(),
                    intent == null ? "" : intent.commandId(),
                    intent == null ? "" : intent.targetFile());
            submitAudioGeneration(selected, DocumentPlaybackIntent.none());
        } catch (IOException invalidScope) {
            failPendingDocumentExport("PROCESSING_SELECTION", invalidScope);
            statusMessage.set(invalidScope.getMessage());
        }
    }

    public void generateAudioChunksFromSelectedFragment() {
        if (currentPreparedPdfSource.get() != null) {
            rebuildPreparedPdfNarrationKeepingSelection();
        } else if (currentScript.get() == null || currentScript.get().empty()) {
            buildNarrationScriptFromDocument();
        }
        Optional<NarrationSegment> segment = explicitDocumentAudioSelection();
        if (segment.isEmpty()) { statusMessage.set("Selecciona una oración o bloque antes de renderizar audio desde ese fragmento."); activeWorkspace.set(WorkspaceKind.DOCUMENT_READER); refreshProjectState(); return; }
        documentPlaybackRequested = false; waitingForBufferedSegmentAfter = ""; playSingleCueOnly = false; submitAudioGenerationFromSegment(segment.get(), false);
    }

    public void generateAudioChunkForSelectedFragment() {
        if (currentPreparedPdfSource.get() != null) {
            rebuildPreparedPdfNarrationKeepingSelection();
        } else if (currentScript.get() == null || currentScript.get().empty()) {
            buildNarrationScriptFromDocument();
        }
        Optional<NarrationSegment> segment = explicitDocumentAudioSelection();
        if (segment.isEmpty()) {
            statusMessage.set("Selecciona una oración o bloque antes de procesar este fragmento.");
            refreshProjectState();
            return;
        }
        documentPlaybackRequested = false;
        waitingForBufferedSegmentAfter = "";
        playSingleCueOnly = false;
        submitAudioGenerationFromSegment(segment.get(), false, 1);
    }

    private void prepareEffectiveNarrationAsync(
            NarrationScriptDocument source,
            Consumer<NarrationScriptDocument> continuation) {
        prepareEffectiveNarrationAsync(source, continuation, failure -> {
            failPendingDocumentExport("TRANSLATION", failure);
            documentPlaybackRequested = false;
            refreshProjectState();
        });
    }

    private void prepareEffectiveNarrationAsync(
            NarrationScriptDocument source,
            Consumer<NarrationScriptDocument> continuation,
            Consumer<Throwable> failureContinuation) {
        prepareEffectiveNarrationAsync(source, continuation, ProgressSink.NONE,
                failureContinuation);
    }

    private void prepareEffectiveNarrationAsync(
            NarrationScriptDocument source,
            Consumer<NarrationScriptDocument> continuation,
            ProgressSink translationProgress,
            Consumer<Throwable> failureContinuation) {
        if (source == null || source.empty()) {
            continuation.accept(source);
            return;
        }
        DocumentTranslationPreferences preferences = documentTranslationPreferences();
        if (!preferences.enabled()) {
            try {
                continuation.accept(adaptNarrationLanguage.execute(source, preferences,
                        currentProjectDirectory().orElse(null),
                        com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken.NONE,
                        com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE).script());
            } catch (Exception impossibleLocalFailure) {
                continuation.accept(source);
            }
            return;
        }
        long generation = narrationTranslationGeneration.incrementAndGet();
        narrationTranslationCancellationRequested.set(false);
        AtomicBoolean operationCancellation = new AtomicBoolean(false);
        statusMessage.set("Adaptando la narración al idioma de escucha "
                + preferences.listeningLanguage() + "...");
        Thread worker = Thread.ofVirtual().name("narration-translation-" + generation)
                .unstarted(() -> {
            try {
                AdaptNarrationLanguageUseCase.Result result = adaptNarrationLanguage.execute(
                        source, preferences, currentProjectDirectory().orElse(null),
                        () -> operationCancellation.get()
                                || narrationTranslationCancellationRequested.get(),
                        (stage, progress, message) -> Platform.runLater(() -> {
                            if (generation == narrationTranslationGeneration.get()
                                    && !operationCancellation.get()) {
                                statusMessage.set(message);
                                translationProgress.report(stage, progress, message);
                                if (pendingDocumentExport != null) {
                                    localDocumentAnalysisTitle.set("Traduciendo narración");
                                    localDocumentAnalysisDetail.set(message);
                                    localDocumentAnalysisProgress.set(progress);
                                    LOGGER.info("document-export.workflow correlationId={} "
                                                    + "stage=TRANSLATION progress={} message={}",
                                            pendingDocumentExport.correlationId(), progress, message);
                                }
                            }
                        }), pendingDocumentExport == null ? ""
                                : pendingDocumentExport.correlationId());
                Platform.runLater(() -> {
                    if (generation != narrationTranslationGeneration.get()
                            || narrationTranslationCancellationRequested.get()
                            || operationCancellation.get()) return;
                    statusMessage.set("Narración adaptada: " + result.translationCalls()
                            + " traducción(es), " + result.cacheHits()
                            + " desde caché y " + result.passthroughSegments()
                            + " sin traducción.");
                    refreshFullDocumentReadingReadiness();
                    continuation.accept(result.script());
                });
            } catch (InterruptedException cancelled) {
                Thread.currentThread().interrupt();
                completeCancelledNarrationTranslation(
                        operationCancellation, generation, cancelled);
            } catch (Exception failure) {
                if (operationCancellation.get()
                        || narrationTranslationCancellationRequested.get()) {
                    completeCancelledNarrationTranslation(
                            operationCancellation, generation, failure);
                } else {
                    Platform.runLater(() -> failureContinuation.accept(failure));
                }
            } finally {
                activeNarrationTranslationThread.compareAndSet(
                        Thread.currentThread(), null);
                activeNarrationTranslationCancellation.compareAndSet(
                        operationCancellation, null);
            }
        });
        activeNarrationTranslationCancellation.set(operationCancellation);
        activeNarrationTranslationThread.set(worker);
        pendingNarrationTranslationWorker = worker;
        worker.start();
    }

    private void completeCancelledNarrationTranslation(
            AtomicBoolean operationCancellation, long generation, Throwable cause) {
        Platform.runLater(() -> {
            if (!operationCancellation.get()) return;
            LOGGER.info("document-translation.lifecycle generation={} terminal=CANCELLED "
                    + "requestReleased=true reason={}", generation,
                    rootCauseMessage(cause));
            if (pendingDocumentExport != null) {
                failPendingDocumentExport("CANCELLED",
                        new IOException("Traducción cancelada por el usuario.", cause));
            } else {
                endLocalDocumentAnalysis();
                statusMessage.set("Análisis de traducción cancelado. "
                        + "Los resultados válidos anteriores se conservaron.");
                documentPlaybackRequested = false;
                refreshProjectState();
            }
        });
    }

    private static NarrationScriptDocument replaceSegments(
            NarrationScriptDocument source, List<NarrationSegment> replacements) {
        Map<String, NarrationSegment> byId = replacements.stream()
                .collect(java.util.stream.Collectors.toMap(NarrationSegment::id,
                        java.util.function.Function.identity()));
        List<NarrationSegment> merged = source.segments().stream()
                .map(segment -> byId.getOrDefault(segment.id(), segment)).toList();
        String language = replacements.isEmpty() ? source.language()
                : replacements.getFirst().metadata().getOrDefault(
                "effectiveLanguage", source.language());
        return new NarrationScriptDocument(source.id(), source.title(), language,
                source.sourceDocumentTitle(), merged, source.createdAt(),
                java.time.Instant.now(), source.notes());
    }

    private static NarrationScriptDocument firstNarratableSegmentScript(
            NarrationScriptDocument source) {
        List<NarrationSegment> segments = source.segments().stream()
                .filter(NarrationSegment::narratable).limit(1).toList();
        return new NarrationScriptDocument(source.id(), source.title(), source.language(),
                source.sourceDocumentTitle(), segments, source.createdAt(),
                java.time.Instant.now(), source.notes());
    }

    public void submitAudioGeneration() {
        submitAudioGeneration(DocumentPlaybackIntent.none());
    }

    private void submitAudioGeneration(DocumentPlaybackIntent playbackIntent) {
        submitAudioGeneration(currentScript.get(), playbackIntent);
    }

    private void submitAudioGeneration(NarrationScriptDocument script,
                                       DocumentPlaybackIntent playbackIntent) {
        prepareEffectiveNarrationAsync(script, effective ->
                submitEffectiveAudioGeneration(effective, playbackIntent));
    }

    private void submitEffectiveAudioGeneration(NarrationScriptDocument script,
                                                DocumentPlaybackIntent playbackIntent) {
        if (script == null || script.empty()) {
            IOException failure = new IOException(
                    "Prepara la lectura del documento antes de generar audio.");
            statusMessage.set(failure.getMessage());
            failPendingDocumentExportIfActive("AUDIO_PRECONDITION", failure);
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        if (audioJobRunning.get() && isNonPreemptiveCompleteAudioIntent(playbackIntent)) {
            statusMessage.set(pendingDocumentExport == null
                    ? "La lectura completa ya se está procesando. Se mantiene el trabajo actual; no se creó otro lote."
                    : "La exportación esperará al trabajo de voz activo y reutilizará sus resultados.");
            refreshProjectState();
            return;
        }
        ProjectSession session;
        try {
            session = requireSession();
        } catch (RuntimeException missingSession) {
            failPendingDocumentExportIfActive("AUDIO_PRECONDITION", missingSession);
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        Optional<Path> file = session.projectFile();
        if (file.isEmpty()) {
            IOException failure = new IOException(
                    "Guarda el proyecto antes de generar audio para crear la carpeta jobs/.");
            statusMessage.set(failure.getMessage());
            failPendingDocumentExportIfActive("AUDIO_PRECONDITION", failure);
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        if (audioEngineUnavailableForGeneration()) {
            IOException failure = new IOException(audioEngineUnavailableMessage());
            statusMessage.set(failure.getMessage());
            failPendingDocumentExportIfActive("AUDIO_PRECONDITION", failure);
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
            return;
        }
        try { saveCurrentProjectAs(file.get()); }
        catch (IOException ex) {
            IOException failure = new IOException(
                    "No se pudo guardar el proyecto antes de generar audio: "
                            + ex.getMessage(), ex);
            statusMessage.set(failure.getMessage());
            failPendingDocumentExportIfActive("PROJECT_SAVE", failure);
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            return;
        }
        Path projectDirectory = file.get().toAbsolutePath().normalize().getParent();
        AudioGenerationRequest request = audioGenerationRequestFor(session, script, projectDirectory, session.title());
        boolean compatiblePlaybackStarted = false;
        try {
            ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                    request.generationUnits(), audioWorkflow.persistedJobs(projectDirectory),
                    projectDirectory);
            if (playbackIntent != null && playbackIntent.autoPlay()
                    && !coverage.readyAudio().isEmpty()) {
                compatiblePlaybackStarted = startCompatiblePlaybackBeforeGeneration(
                        playbackIntent);
            }
            Set<String> required = coverage.missingOrStale().stream()
                    .map(entry -> entry.unit().id())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (pendingDocumentExport != null) {
                int totalUnits = request.generationUnits().size();
                pendingDocumentExport.recordAudioPlan(totalUnits,
                        Math.max(0, totalUnits - required.size()));
            }
            if (required.isEmpty()) {
                statusMessage.set(compatiblePlaybackStarted
                        ? "Audio vigente verificado. Reproduciendo sin crear un job nuevo."
                        : "Audio vigente verificado; no se creó un job de voz.");
                continuePendingDocumentExportAfterCoverageRecheck();
                refreshProjectState();
                return;
            }
            request = audioWorkflow.retainGenerationUnits(request, required);
            statusMessage.set("Preparando " + required.size()
                    + " fragmento(s) de audio faltante(s) o desactualizado(s).");
            if (pendingDocumentExport != null) {
                int total = pendingDocumentExport.plannedTtsUnits();
                int ready = pendingDocumentExport.initiallyReusableTtsUnits();
                localDocumentAnalysisTitle.set("Generando audio");
                localDocumentAnalysisDetail.set("Unidades de voz listas " + ready + "/" + total);
                localDocumentAnalysisProgress.set(total == 0 ? 0.0
                        : (double) ready / total);
                LOGGER.info("document-export.workflow correlationId={} "
                                + "stage=AUDIO_GENERATION readyUnits={} totalUnits={} "
                                + "missingUnits={} narrationSegments={}",
                        pendingDocumentExport.correlationId(), ready, total, required.size(),
                        pendingDocumentExport.segmentIds().size());
            }
        } catch (IOException coverageFailure) {
            statusMessage.set("No se pudo verificar la cobertura de audio; no se iniciará "
                    + "una regeneración completa implícita: " + coverageFailure.getMessage());
            failPendingDocumentExport("AUDIO_COVERAGE", coverageFailure);
            refreshProjectState();
            return;
        }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        AudioEngineDescriptor engine = audioEngineDescriptor();
        try {
            submitPriorityAudioRequestAsync(request, "Generación de huecos enviada",
                    engine.statusLabel(), restorePlaybackIntent(playbackIntent),
                    compatiblePlaybackStarted);
        } catch (RuntimeException submissionFailure) {
            failPendingDocumentExportIfActive("AUDIO_SUBMISSION", submissionFailure);
        }
        refreshProjectState();
    }

    private static boolean isNonPreemptiveCompleteAudioIntent(
            DocumentPlaybackIntent intent) {
        if (intent == null || intent.autoPlay()) return false;
        return intent.action() == DocumentAudioAction.PROCESS_COMPLETE
                || intent.action() == DocumentAudioAction.GENERATE_ALL;
    }

    private boolean startCompatiblePlaybackBeforeGeneration(
            DocumentPlaybackIntent playbackIntent) {
        applyPlaybackIntent(playbackIntent);
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        if (manifest == null || manifest.emptyManifest()) return false;
        Optional<PlaybackCue> start = playbackIntent.preferredSegmentId().isBlank()
                ? manifest.firstCue()
                : manifest.cueForSegment(playbackIntent.preferredSegmentId())
                .or(manifest::firstCue);
        return start.map(cue -> startPlaybackFromCue(cue, true)).orElse(false);
    }

    public void submitMockAudioGeneration() { submitAudioGeneration(); }
    private AudioGenerationRequest audioGenerationRequestFor(ProjectSession session, NarrationScriptDocument script, Path projectDirectory, String jobName) {
        AudioGenerationRequest request = audioWorkflow.buildGenerationRequest(
                session, script, projectDirectory, jobName);
        if (LOGGER.isInfoEnabled()) {
            Map<String, Long> acousticProfiles = new LinkedHashMap<>();
            request.generationUnits().forEach(unit -> {
                String profile = unit.effectiveVoiceProfileId(
                        request.voiceProfileId()) + "/"
                        + request.requestedReferenceToneFor(unit).name() + "/"
                        + unit.sourceFingerprint().voiceConfigurationSha256();
                acousticProfiles.merge(profile, 1L, Long::sum);
            });
            LOGGER.info("audio-generation.acoustic-snapshot engineId={} runtime={} "
                            + "defaultVoiceId={} units={} profiles={}",
                    audioEngineDescriptor().engineId(), request.acousticRuntimeId(),
                    session.project().documentDefaultVoiceProfileId(),
                    request.generationUnits().size(), acousticProfiles);
        }
        return request;
    }

    private static Optional<NarrationSegment> firstPreparedPdfSegmentAtOrAfterPage(
            NarrationScriptDocument script, int visiblePage) {
        if (script == null) return Optional.empty();
        return script.segments().stream()
                .filter(segment -> {
                    try {
                        return Integer.parseInt(segment.metadata()
                                .getOrDefault("pdfSourcePage", "0")) >= visiblePage;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .findFirst();
    }

    private Optional<NarrationSegment> explicitDocumentAudioSelection() {
        NarrationScriptDocument script = currentScript.get();
        if (script == null || script.empty()) {
            return Optional.empty();
        }
        if (selectedPdfRegion.get() != null) {
            return script.segmentById(selectedScriptSegmentId.get());
        }
        String blockId = selectedDocumentBlockId.get();
        if (blockId != null && !blockId.isBlank()) {
            return firstSegmentForDocumentBlock(blockId);
        }
        return Optional.empty();
    }

    private void rebuildPreparedPdfNarrationKeepingSelection() {
        if (currentPreparedPdfSource.get() == null) {
            return;
        }
        String preferredSegmentId = Objects.toString(
                selectedScriptSegmentId.get(), "").strip();
        var preferredRegion = selectedPdfRegion.get();
        buildNarrationScriptFromDocument();
        NarrationScriptDocument rebuilt = currentScript.get();
        if (rebuilt == null || rebuilt.empty()) {
            return;
        }
        if (!preferredSegmentId.isBlank()
                && rebuilt.segmentById(preferredSegmentId).isPresent()) {
            selectedScriptSegmentId.set(preferredSegmentId);
        } else if (preferredRegion != null) {
            alignPlaybackCursorToPdfSelection(preferredRegion);
        }
    }

    private boolean submitAudioGenerationFromSegment(NarrationSegment startSegment, boolean requestPlayback) {
        return submitAudioGenerationFromSegment(startSegment, requestPlayback, null);
    }

    private boolean submitAudioGenerationFromSegment(
            NarrationSegment startSegment,
            boolean requestPlayback,
            Integer maximumVoiceFragmentsOverride) {
        return submitAudioGenerationFromSegment(startSegment, requestPlayback,
                maximumVoiceFragmentsOverride, false);
    }

    private boolean submitAudioGenerationFromSegment(
            NarrationSegment startSegment,
            boolean requestPlayback,
            Integer maximumVoiceFragmentsOverride,
            boolean preserveInterruptedJob) {
        return submitAudioGenerationFromSegment(startSegment, requestPlayback,
                maximumVoiceFragmentsOverride, preserveInterruptedJob, null);
    }

    private boolean submitAudioGenerationFromSegment(
            NarrationSegment startSegment,
            boolean requestPlayback,
            Integer maximumVoiceFragmentsOverride,
            boolean preserveInterruptedJob,
            NarrationScriptDocument effectiveScriptOverride) {
        if (startSegment == null) {
            statusMessage.set("Selecciona una oración o bloque antes de preparar audio desde ahí.");
            activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
            refreshProjectState();
            return false;
        }
        NarrationScriptDocument script = effectiveScriptOverride == null
                ? currentScript.get() : effectiveScriptOverride;
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
        if (effectiveScriptOverride == null) {
            NarrationScriptDocument sourceScript = script;
            NarrationScriptDocument translationScope =
                    maximumVoiceFragmentsOverride != null
                    && maximumVoiceFragmentsOverride == 1
                    ? firstNarratableSegmentScript(suffixScript) : suffixScript;
            prepareEffectiveNarrationAsync(translationScope, adaptedSuffix -> {
                NarrationScriptDocument merged = replaceSegments(
                        sourceScript, adaptedSuffix.segments());
                NarrationSegment effectiveStart = merged.segmentById(startSegment.id())
                        .orElse(startSegment);
                submitAudioGenerationFromSegment(effectiveStart, requestPlayback,
                        maximumVoiceFragmentsOverride, preserveInterruptedJob, merged);
            });
            return true;
        }
        int maximumVoiceFragments = maximumVoiceFragmentsOverride != null
                ? Math.max(0, maximumVoiceFragmentsOverride)
                : documentAudioPortionEnabled()
                    ? documentAudioPreparationExtent().maximumVoiceFragments()
                    : 0;
        AudioGenerationRequest request = audioGenerationRequestForSelection(
                session, script, suffixScript, projectDirectory, session.title(),
                startSegment.id(), maximumVoiceFragments);
        try {
            ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                    request.generationUnits(), audioWorkflow.persistedJobs(projectDirectory),
                    projectDirectory);
            java.util.Set<String> uncovered = coverage.missingOrStale().stream()
                    .map(entry -> entry.unit().id())
                    .collect(java.util.stream.Collectors.toSet());
            if (uncovered.isEmpty()) {
                statusMessage.set("El audio solicitado ya esta vigente; no se creo un job nuevo.");
                if (requestPlayback) startPlaybackFromSegment(startSegment.id(), false);
                refreshProjectState();
                return true;
            }
            request = audioWorkflow.retainGenerationUnits(request, uncovered);
        } catch (IOException ex) {
            statusMessage.set("No se pudo comprobar toda la cobertura previa; se preparara el alcance solicitado: "
                    + ex.getMessage());
        }
        activeWorkspace.set(WorkspaceKind.DOCUMENT_READER);
        boolean singleFragment = maximumVoiceFragmentsOverride != null
                && maximumVoiceFragments == 1;
        String scopeLabel = singleFragment
                ? "solo este fragmento"
                : maximumVoiceFragments > 0
                ? "hasta " + maximumVoiceFragments + " fragmentos de voz"
                : "todo lo narrable desde la selección";
        Runnable playbackIntent = restorePlaybackIntent(requestPlayback
                ? DocumentPlaybackIntent.fromSegment(startSegment.id(), singleFragment)
                : DocumentPlaybackIntent.none());
        if (preserveInterruptedJob) {
            boolean keepCompatiblePlayback = priorityAudioPlaybackPolicy.keepPlayback(
                    true, playbackCursor.get());
            submitPriorityAudioRequestAsync(request,
                    "Narración prioritaria desde la selección",
                    audioEngineDescriptor().statusLabel(), playbackIntent,
                    keepCompatiblePlayback);
        } else {
            submitFreshAudioRequestAsync(projectDirectory, request,
                    "Preparando audio desde la selección: " + scopeLabel,
                    audioEngineDescriptor().statusLabel(), playbackIntent);
        }
        refreshProjectState(); return true;
    }

    /**
     * Plays an already compatible PDF chunk without OCR, Qwen, TTS or a progress overlay.
     * The projection is rebuilt only from persisted prepared-page and job artifacts.
     */
    public enum PdfNarrateCacheResult { COMPLETE, PARTIAL, MISS }

    public PdfNarrateCacheResult playSelectedPdfTargetFromCompatibleCache() {
        if (currentPreparedPdfSource.get() == null || selectedPdfRegion.get() == null) {
            return PdfNarrateCacheResult.MISS;
        }
        rebuildPreparedPdfNarrationKeepingSelection();
        NarrationScriptDocument script = currentScript.get();
        if (script != null) {
            script = adaptNarrationLanguage.fromCache(
                    script, documentTranslationPreferences(),
                    currentProjectDirectory().orElse(null)).orElse(null);
            if (script == null) return PdfNarrateCacheResult.MISS;
        }
        NarrationScriptDocument effectiveScript = script;
        int page = selectedPdfRegion.get().pageNumber();
        Optional<NarrationSegment> start = explicitDocumentAudioSelection()
                .or(() -> firstPreparedPdfSegmentAtOrAfterPage(effectiveScript, page));
        if (start.isEmpty()) return PdfNarrateCacheResult.MISS;
        PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();
        Optional<PlaybackCue> cue = playbackSelectionResolver.cueForSelection(
                manifest, start.get(), selectedDocumentTextRange.get(), selectedDocumentTextPreview);
        if (cue.isEmpty()) return PdfNarrateCacheResult.MISS;
        boolean completeSuffix = false;
        try {
            ProjectSession session = requireSession();
            Path root = currentProjectDirectory().orElseThrow();
            AudioGenerationRequest request = audioGenerationRequestFor(
                    session, effectiveScript, root, session.title());
            ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                    request.generationUnits(), audioWorkflow.persistedJobs(root), root);
            Optional<String> firstUnit = request.generationUnits().stream()
                    .filter(unit -> start.get().id().equals(unit.sourceSegmentId()))
                    .map(unit -> unit.id()).findFirst();
            completeSuffix = firstUnit.isPresent() && coverage.completeFrom(firstUnit.get());
        } catch (IOException | RuntimeException ignored) {
            completeSuffix = false;
        }
        stopPlayback();
        pendingPlaybackStartSegmentId = "";
        selectedScriptSegmentId.set(start.get().id());
        currentPlaybackManifest.set(manifest);
        playbackTransport.startRuntimeQueue(manifest, cue.get());
        startSequentialPlayback(manifest, cue.get(), false);
        statusMessage.set("Reproduciendo desde " + start.get().id()
                + (completeSuffix
                ? " con cobertura compatible completa."
                : " con audio compatible; se completaran solo los huecos posteriores."));
        refreshProjectState();
        return completeSuffix ? PdfNarrateCacheResult.COMPLETE
                : PdfNarrateCacheResult.PARTIAL;
    }

    /** Keeps cached playback active and submits only missing/stale suffix units. */
    public void fillMissingAudioFromSelectedPdfTarget() {
        NarrationScriptDocument script = currentScript.get();
        int page = selectedPdfRegion.get() == null ? 1 : selectedPdfRegion.get().pageNumber();
        Optional<NarrationSegment> start = explicitDocumentAudioSelection()
                .or(() -> firstPreparedPdfSegmentAtOrAfterPage(script, page));
        if (start.isEmpty()) {
            statusMessage.set("No hay fragmentos pendientes desde el elemento seleccionado.");
            return;
        }
        submitAudioGenerationFromSegment(start.get(), true, 0, true);
    }

    /** Starts buffered narration at the selected PDF element and preserves the interrupted job. */
    public void narrateFromSelectedPdfTarget() {
        if (currentPreparedPdfSource.get() == null
                || selectedPdfRegion.get() == null) {
            statusMessage.set("Selecciona un elemento PDF antes de narrar desde aquí.");
            return;
        }
        rebuildPreparedPdfNarrationKeepingSelection();
        NarrationScriptDocument script = currentScript.get();
        int page = selectedPdfRegion.get().pageNumber();
        Optional<NarrationSegment> start = explicitDocumentAudioSelection()
                .or(() -> firstPreparedPdfSegmentAtOrAfterPage(script, page));
        if (start.isEmpty()) {
            statusMessage.set("No hay texto narrable desde el elemento seleccionado. "
                    + "La interpretación pudo quedar omitida por política, rechazo "
                    + "o evidencia insuficiente.");
            refreshProjectState();
            return;
        }
        stopPlayback();
        submitAudioGenerationFromSegment(start.get(), true, 0, true);
    }
    private AudioGenerationRequest audioGenerationRequestForSelection(ProjectSession session, NarrationScriptDocument fullScript,
                                                                      NarrationScriptDocument suffixScript, Path projectDirectory,
                                                                      String jobName, String startSegmentId,
                                                                      int maximumVoiceFragments) {
        return audioWorkflow.buildGenerationRequestForSelection(
                session, fullScript, suffixScript, projectDirectory, jobName,
                startSegmentId, maximumVoiceFragments);
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
        cancelIncrementalPdfAudioPipeline();
        statusMessage.set(audioWorkflow.cancelActiveAudioJob(activeAudioJobStatus.get(), updated -> {
            waitingForBufferedSegmentAfter = "";
            audioJobRunning.set(false);
            activeAudioJobStatus.set(updated);
        }));
    }

    public void cancelCurrentAudioOperation() {
        documentExportCancellationRequested.set(true);
        if (documentExportRenderRunning) {
            statusMessage.set("Cancelación de exportación solicitada. Se conservarán los derivados válidos.");
            return;
        }
        if (pendingDocumentExport != null && !audioJobRunning.get()) {
            failPendingDocumentExport("CANCELLED",
                    new IOException("Operación cancelada por el usuario."));
        }
        narrationTranslationCancellationRequested.set(true);
        narrationTranslationGeneration.incrementAndGet();
        cancelIncrementalPdfAudioPipeline();
        CompletableFuture<Void> maintenance = activeAudioMaintenance;
        if (maintenance != null && !maintenance.isDone()) {
            AudioJobStatusDto status = activeAudioJobStatus.get();
            if (status != null && !status.jobId().isBlank()) {
                audioWorkflow.cancelActiveAudioJobSilently(status, updated -> Platform.runLater(() ->
                        activeAudioJobStatus.set(updated)));
            }
            statusMessage.set("Cancelación solicitada. Se completará el paso seguro actual "
                    + "antes de liberar los archivos de audio.");
            return;
        }
        String jobId = currentAudioJobId();
        if (jobId.isBlank()) {
            cancelActiveAudioJob();
            return;
        }
        cancelKnownAudioJobAndThen(jobId, null);
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
        if (status.state().terminal()) {
            refreshFullDocumentReadingReadiness();
        }
        if (pendingDocumentExport != null) {
            int total = pendingDocumentExport.plannedTtsUnits();
            int initiallyReady = pendingDocumentExport.initiallyReusableTtsUnits();
            if (total <= 0) {
                total = initiallyReady + status.totalSegments();
            }
            int ready = Math.min(total, initiallyReady + status.completedSegments());
            localDocumentAnalysisTitle.set(
                    com.marcosmoreiradev.docupodcaststudio.presentation.process.AudioProgressText.title(status));
            localDocumentAnalysisDetail.set("Unidades de voz listas " + ready + "/" + total
                    + (status.currentSegmentId().isBlank()
                    ? "" : " · " + status.currentSegmentId())
                    + (status.message().isBlank() ? "" : " · " + status.message()));
            localDocumentAnalysisProgress.set(total == 0 ? 0.0
                    : (double) ready / total);
            LOGGER.info("document-export.workflow correlationId={} "
                            + "stage=AUDIO_GENERATION state={} readyUnits={} totalUnits={} "
                            + "narrationSegments={} jobId={} unitId={}",
                    pendingDocumentExport.correlationId(), status.state(), ready, total,
                    pendingDocumentExport.segmentIds().size(), status.jobId(),
                    status.currentSegmentId());
        }
        if (status.state().terminal() && incrementalPdfAudioActive
                && incrementalPdfAudioJobId.isBlank()) {
            Platform.runLater(this::drainIncrementalPdfAudio);
        }
        if (status.state().terminal()
                && status.jobId().equals(activeSubmittedAudioJobId)) {
            activeSubmittedAudioJobId = "";
        }
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
            if (pendingDocumentExport != null) {
                continuePendingDocumentExportAfterCoverageRecheck();
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
        if (status.state().terminal() && pendingDocumentExport != null) {
            failPendingDocumentExport("AUDIO_GENERATION",
                    new IOException(status.statusLine()));
        }
        if (!playbackNeedsManifest && status.running()) { refreshStreamingBufferStatus(); return; }
        refreshProjectState();
    }

    private void cancelIncrementalPdfAudioPipeline() {
        incrementalPdfAudioActive = false;
        incrementalPdfPreparationComplete = false;
        incrementalPdfAudioBacklog.clear();
        incrementalPdfObservedPages.clear();
        incrementalPdfAudioJobId = "";
        incrementalPdfActiveAudioPage = 0;
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
        if (playbackController.canStartBufferedPlayback(documentPlaybackRequested, playbackCursor.get(), status, manifest, playbackBufferPolicy)) { return true; }
        PlaybackCursor cursor = playbackCursor.get();
        return (cursor == null || cursor.stoppedState())
                && status.completedSegments() > 0
                && pendingPlaybackStartSegmentId.isBlank();
    }
    private boolean tryContinueAfterBufferGap(PlaybackManifest manifest) {
        if (!playbackController.canContinueAfterGap(waitingForBufferedSegmentAfter, manifest)) {
            return false;
        }
        Optional<PlaybackCue> nextCue = playbackController.nextCueAfterGap(waitingForBufferedSegmentAfter, manifest);
        if (nextCue.isEmpty() && manifest != null && manifest.cueForUnit(waitingForBufferedSegmentAfter).isEmpty()) {
            // Some resumed/suffix jobs build a fresh manifest that starts after the cue that caused
            // the buffer gap. In that case the next available cue is the first cue of the new
            // manifest; do not wait for another manual Play click.
            nextCue = manifest.firstCue();
        }
        if (nextCue.isEmpty()) {
            statusMessage.set(playbackController.waitingForBufferMessage(playbackBufferPolicy, playbackBufferStatusLabel()));
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
        managedAudioChunksAvailable.set(hasManagedAudioChunksOnDisk());
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
        managedAudioChunksAvailable.set(hasManagedAudioChunksOnDisk());
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
            ProjectSession session = requireSession();
            AudioGenerationRequest currentRequest = audioGenerationRequestFor(
                    session, script, projectDirectory, session.title());
            ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                    currentRequest.generationUnits(), snapshots, projectDirectory);
            if (coverage.readyAudio().isEmpty()) {
                currentPlaybackManifest.set(PlaybackManifest.empty());
                return currentPlaybackManifest.get();
            }
            java.time.Instant now = java.time.Instant.now();
            AudioJobSnapshot combined = new AudioJobSnapshot(
                    "COVERAGE-MANIFEST", script.title(), AudioJobState.COMPLETED,
                    AudioGenerationStage.EXPORT_READY, coverage.readyAudio().size(),
                    currentRequest.generationUnitCount(), 0,
                    coverage.readyAudio().size() / (double) currentRequest.generationUnitCount(),
                    "", "", 0L, "Cobertura compatible reunida desde jobs persistidos.",
                    "jobs", "", "", coverage.readyAudio(), now, now);
            var plan = workspaceServices.generation().render().buildNarrationRenderPlan()
                    .build(script, session.project());
            PlaybackManifest manifest = workspaceServices.playback().playback().buildPlaybackManifest()
                    .build(script, combined, currentStoryboard.get(), plan, session.project());
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
        statusMessage.set(playbackController.waitingForBufferMessage(playbackBufferPolicy, "")
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
        if (result.started()) {
            // PlaybackCursor is a transport clock and may compare equal when the same cue is
            // replayed. Publish the concrete cue separately so Word/PDF visual focus always
            // receives one event for every physical audio start.
            activePlaybackCue.set(null);
            activePlaybackCue.set(cue);
            syncTheatreAudioPlayback(cue.startSeconds() + Math.max(0.0, localOffset));
            return true;
        }
        if (result.playerFailure()) { playbackTransport.stopPlayerAndContinuation(); }
        statusMessage.set(result.message());
        return false;
    }

    public void deleteAllPersistedAudioChunks() {
        Optional<Path> projectDirectory = currentProjectDirectory();
        if (projectDirectory.isEmpty()) { statusMessage.set("Guarda el proyecto antes de eliminar chunks de audio."); refreshProjectState(); return; }
        deletePersistedAudioAsync(projectDirectory.get(), "Chunks de audio eliminados. Puedes reconstruirlos desde la barra de estado.");
    }
    private void invalidatePersistedAudioForNarrationChange() {
        // Preserve completed WAVs. The rebuilt request and ReusableAudioCoverage
        // decide, unit by unit, which acoustic fingerprints are still current.
        resetPlaybackState();
        activeAudioJobStatus.set(AudioJobStatusDto.idle());
        audioJobRunning.set(false);
        statusMessage.set("Cambió la narración; se conservará el audio compatible y "
                + "se regenerarán únicamente los fragmentos afectados.");
    }
    private void submitFreshAudioRequestAsync(
            Path directory,
            AudioGenerationRequest request,
            String successPrefix,
            String engine) {
        submitFreshAudioRequestAsync(directory, request, successPrefix, engine, null);
    }
    private void submitFreshAudioRequestAsync(
            Path directory,
            AudioGenerationRequest request,
            String successPrefix,
            String engine,
            Runnable restorePlaybackIntent) {
        resetPlaybackState();
        if (restorePlaybackIntent != null) {
            restorePlaybackIntent.run();
        }
        audioJobRunning.set(true);
        statusMessage.set("Esperando la terminación segura del trabajo anterior...");
        CompletableFuture<String> submission = audioWorkflow.interruptAndSubmitAsync(
                activeAudioJobStatus.get(),
                updated -> Platform.runLater(() -> activeAudioJobStatus.set(updated)),
                request,
                this::acceptAudioStatus
        );
        activeAudioSubmission = submission;
        submission.whenComplete((jobId, failure) -> Platform.runLater(() -> {
            if (failure == null) {
                managedAudioChunksAvailable.set(hasManagedAudioChunksOnDisk());
                activeSubmittedAudioJobId = jobId == null ? "" : jobId.strip();
                statusMessage.set(successPrefix + ": " + jobId + ". Motor: " + engine + ".");
            } else {
                audioJobRunning.set(false);
                documentPlaybackRequested = false;
                singleCuePlaybackRequested = false;
                pendingPlaybackStartSegmentId = "";
                statusMessage.set("No se pudo preparar el nuevo render de audio: " + rootCauseMessage(failure));
            }
            refreshProjectState();
        }));
    }

    private Runnable restorePlaybackIntent(DocumentPlaybackIntent intent) {
        DocumentPlaybackIntent safe = intent == null ? DocumentPlaybackIntent.none() : intent;
        return () -> applyPlaybackIntent(safe);
    }

    private void applyPlaybackIntent(DocumentPlaybackIntent intent) {
        DocumentPlaybackIntent safe = intent == null ? DocumentPlaybackIntent.none() : intent;
        documentPlaybackRequested = safe.autoPlay();
        singleCuePlaybackRequested = safe.singleCue();
        playSingleCueOnly = false;
        waitingForBufferedSegmentAfter = "";
        pendingPlaybackStartSegmentId = safe.autoPlay() ? safe.preferredSegmentId() : "";
        if (!safe.preferredSegmentId().isBlank()) {
            selectedScriptSegmentId.set(safe.preferredSegmentId());
        }
    }
    private void submitPriorityAudioRequestAsync(
            AudioGenerationRequest request,
            String successPrefix,
            String engine,
            Runnable restorePlaybackIntent,
            boolean keepCompatiblePlayback) {
        if (!keepCompatiblePlayback) {
            resetPlaybackState();
            if (restorePlaybackIntent != null) restorePlaybackIntent.run();
        }
        audioJobRunning.set(true);
        statusMessage.set(keepCompatiblePlayback
                ? "Completando huecos de audio sin interrumpir la reproducción compatible..."
                : "Interrumpiendo el trabajo anterior y conservando sus avances...");
        CompletableFuture<String> submission = audioWorkflow.interruptAndSubmitAsync(
                activeAudioJobStatus.get(),
                updated -> Platform.runLater(() -> activeAudioJobStatus.set(updated)),
                request, this::acceptAudioStatus
        );
        activeAudioSubmission = submission;
        submission.whenComplete((jobId, failure) -> Platform.runLater(() -> {
            if (failure == null) {
                activeSubmittedAudioJobId = jobId == null ? "" : jobId.strip();
                statusMessage.set(successPrefix + ": " + jobId
                        + ". Motor: " + engine + ".");
            } else {
                audioJobRunning.set(false);
                if (!keepCompatiblePlayback) {
                    documentPlaybackRequested = false;
                    pendingPlaybackStartSegmentId = "";
                }
                statusMessage.set("No se pudo aplicar la nueva prioridad de audio: "
                        + rootCauseMessage(failure));
            }
            refreshProjectState();
        }));
    }
    private void deletePersistedAudioAsync(Path directory, String successMessage) {
        ProjectSession targetSession = sessions.activeSession().orElse(null);
        Path targetProjectFile = targetSession == null
                ? null : targetSession.projectFile().orElse(null);
        resetPlaybackState();
        audioJobRunning.set(true);
        statusMessage.set("Cancelando y esperando el trabajo de audio activo...");
        CompletableFuture<Void> maintenance = audioWorkflow.prepareFreshWorkspaceAsync(
                directory,
                currentAudioJobId(),
                activeAudioJobStatus.get(),
                updated -> Platform.runLater(() -> activeAudioJobStatus.set(updated)));
        activeAudioMaintenance = maintenance;
        maintenance.whenComplete((ignored, failure) -> Platform.runLater(() -> {
            if (activeAudioMaintenance == maintenance) {
                activeAudioMaintenance = CompletableFuture.completedFuture(null);
            }
            audioJobRunning.set(false);
            activeAudioJobStatus.set(AudioJobStatusDto.idle());
            if (failure == null) {
                String catalogRepairFailure = reconcileDeletedAudioAssets(
                        directory, targetSession, targetProjectFile);
                managedAudioChunksAvailable.set(false);
                bumpDocumentMediaRevision();
                refreshStreamingBufferStatus();
                refreshFullDocumentReadingReadiness();
                statusMessage.set(catalogRepairFailure.isBlank()
                        ? successMessage
                        : successMessage + " " + catalogRepairFailure);
            } else {
                statusMessage.set("No se pudieron eliminar los chunks de audio: "
                        + rootCauseMessage(failure));
            }
            refreshProjectState();
        }));
    }

    private String reconcileDeletedAudioAssets(
            Path deletedProjectDirectory,
            ProjectSession targetSession,
            Path targetProjectFile) {
        if (targetSession == null || targetProjectFile == null
                || sessions.activeSession().orElse(null) != targetSession) {
            return "El proyecto activo cambió; no se modificó su catálogo.";
        }
        Path expectedDirectory = targetProjectFile.toAbsolutePath().normalize().getParent();
        Path deletedDirectory = deletedProjectDirectory == null
                ? null : deletedProjectDirectory.toAbsolutePath().normalize();
        if (expectedDirectory == null || !expectedDirectory.equals(deletedDirectory)) {
            return "La limpieza pertenecía a otro proyecto; no se modificó el catálogo activo.";
        }

        boolean wasDirty = targetSession.dirty();
        ReconcileGeneratedAudioJobAssetsUseCase.Result result =
                generatedAudioAssetReconciliation.removeAll(targetSession.project());
        if (!result.changed()) {
            return "";
        }
        targetSession.replaceProject(result.project(), wasDirty);
        try {
            workspaceServices.project().project().saveProject()
                    .save(result.project(), targetProjectFile);
            return "";
        } catch (IOException ex) {
            targetSession.replaceProject(result.project(), true);
            return "Los archivos se eliminaron, pero el catálogo quedó pendiente de guardar: "
                    + rootCauseMessage(ex);
        }
    }

    public boolean documentProcessingActive() {
        PdfPreparationProgress progress = pdfPreparationProgress.get();
        boolean pdfRunning = progress != null
                && (progress.state() == PdfPreparationProgress.State.RUNNING
                || progress.state() == PdfPreparationProgress.State.PAUSED
                || progress.queued() > 0);
        return audioJobRunning.get() || localDocumentAnalysisRunning.get() || pdfRunning;
    }

    public void cancelAudioAndThen(Runnable continuation) {
        Objects.requireNonNull(continuation, "continuation");
        CompletableFuture<Void> maintenance = activeAudioMaintenance;
        if (maintenance != null && !maintenance.isDone()) {
            statusMessage.set("Esperando que termine la limpieza segura de audio antes de reiniciar...");
            maintenance.whenComplete((ignored, failure) -> Platform.runLater(() ->
                    cancelAudioAndThen(continuation)));
            return;
        }
        CompletableFuture<String> submission = activeAudioSubmission;
        if (submission != null && !submission.isDone()) {
            statusMessage.set("Esperando el identificador del trabajo activo para cancelarlo...");
            submission.whenComplete((jobId, failure) -> Platform.runLater(() -> {
                if (failure != null) {
                    continuation.run();
                } else {
                    cancelKnownAudioJobAndThen(jobId, continuation);
                }
            }));
            return;
        }
        String jobId = currentAudioJobId();
        if (jobId.isBlank()) {
            continuation.run();
            return;
        }
        cancelKnownAudioJobAndThen(jobId, continuation);
    }

    private String currentAudioJobId() {
        String submitted = activeSubmittedAudioJobId == null
                ? "" : activeSubmittedAudioJobId.strip();
        if (!submitted.isBlank()) return submitted;
        CompletableFuture<String> submission = activeAudioSubmission;
        if (submission != null && submission.isDone()
                && !submission.isCompletedExceptionally()
                && !submission.isCancelled()) {
            String completed = submission.getNow("");
            if (completed != null && !completed.isBlank()) return completed.strip();
        }
        AudioJobStatusDto status = activeAudioJobStatus.get();
        String fromStatus = status == null || status.jobId() == null
                ? "" : status.jobId().strip();
        return fromStatus;
    }

    private void cancelKnownAudioJobAndThen(String jobId, Runnable continuation) {
        String target = jobId == null ? "" : jobId.strip();
        if (target.isBlank()) {
            if (continuation != null) continuation.run();
            return;
        }
        audioJobRunning.set(true);
        statusMessage.set("Cancelando " + target
                + " y esperando que el motor de voz libere memoria y archivos...");
        audioWorkflow.cancelAndAwaitAsync(target).whenComplete((stopped, failure) ->
                Platform.runLater(() -> {
                    if (failure != null || !Boolean.TRUE.equals(stopped)) {
                        audioJobRunning.set(false);
                        statusMessage.set("No se pudo detener por completo " + target
                                + ". No se inició otro render para evitar dos procesos simultáneos."
                                + (failure == null ? "" : " " + rootCauseMessage(failure)));
                        refreshProjectState();
                        return;
                    }
                    audioJobRunning.set(false);
                    if (target.equals(activeSubmittedAudioJobId)) {
                        activeSubmittedAudioJobId = "";
                    }
                    if (continuation == null) {
                        statusMessage.set("Generación cancelada. Los fragmentos completados se conservaron; "
                                + "puedes seguir generando desde la barra de estado.");
                    } else {
                        statusMessage.set("Trabajo anterior detenido. Preparando el nuevo render...");
                        continuation.run();
                    }
                }));
    }
    private void continuePendingDocumentExportAfterCoverageRecheck() {
        DocumentExportContinuation pending = pendingDocumentExport;
        if (pending == null) return;
        localDocumentAnalysisTitle.set("Verificando los fragmentos para exportar");
        localDocumentAnalysisDetail.set("Comprobando que el alcance seleccionado tenga todos sus audios vigentes.");
        localDocumentAnalysisProgress.set(-1.0);
        localDocumentAnalysisFooter.set("Verificando cobertura y compatibilidad de los audios guardados; no se están regenerando voces.");
        try {
            NarrationScriptDocument scoped = narrationForPreparedExport(
                    pending.intent().orElse(null));
            List<String> currentIds = scoped.segments().stream().map(NarrationSegment::id).toList();
            if (!currentIds.equals(pending.segmentIds())) {
                throw new IOException("El alcance documental cambió mientras se preparaba la exportación.");
            }
            NarrationScriptDocument effective = effectiveNarrationForExport(
                    scoped, documentExportCancellationRequested::get, ignored -> { });
            ReusableAudioCoverage.Report freshCoverage = currentReusableAudioCoverage(effective)
                    .orElseThrow(() -> new IOException(
                            "No se pudo reconstruir la cobertura de audio desde disco."));
            if (!freshCoverage.complete()) {
                List<String> missing = freshCoverage.missingOrStale().stream()
                        .map(entry -> entry.unit().sourceSegmentId().isBlank()
                                ? entry.unit().id() : entry.unit().sourceSegmentId())
                        .distinct().toList();
                pending.claimAfterCoverage(false);
                throw new IOException("Persisten " + missing.size()
                        + " fragmento(s) sin cobertura: " + missing);
            }
            AudioCoverageSnapshotAssembler.Result rebuilt = reconciledAudioForExport(effective,
                    pending.correlationId());
            LOGGER.info("document-export.workflow correlationId={} stage=COVERAGE_RECHECK "
                            + "ready={} missing=0", pending.correlationId(),
                    rebuilt.readyForPreflight());
            Runnable export = pending.claimAfterCoverage(rebuilt.readyForPreflight())
                    .orElseThrow(() -> new IOException(
                            "La continuación de exportación ya alcanzó un estado terminal."));
            pendingDocumentExport = null;
            pendingDocumentExportFailure = null;
            export.run();
        } catch (Exception failure) {
            failPendingDocumentExport("COVERAGE_RECHECK", failure);
        }
    }

    private NarrationScriptDocument narrationForPreparedExport(
            PreparedExportIntent intent) throws IOException {
        if (intent != null
                && intent.commandId() == AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO) {
            return resolveDocumentStudyExportSelection().selection().narration();
        }
        return activeDocumentScopeNarration(currentScript.get());
    }

    private void failPendingDocumentExport(String stage, Throwable failure) {
        Consumer<Throwable> failureHandler = pendingDocumentExportFailure;
        pendingDocumentExportFailure = null;
        DocumentExportContinuation pending = pendingDocumentExport;
        pendingDocumentExport = null;
        if (pending != null) {
            if ("CANCELLED".equals(stage)) pending.cancel(); else pending.fail();
        }
        activeDocumentExportCorrelationId = "";
        endLocalDocumentAnalysis();
        String segmentId = failure instanceof NarrationTranslationException translation
                ? translation.segmentId() : "";
        String reason = failure instanceof NarrationTranslationException translation
                ? translation.reason() : rootCauseMessage(failure);
        if (pending != null) {
            LOGGER.warn("document-export.workflow correlationId={} stage={} terminal=FAILED "
                            + "segmentId={} reason={} diagnostics={}", pending.correlationId(), stage,
                    segmentId, reason,
                    failure instanceof NarrationTranslationException translation
                            ? translation.diagnostics() : java.util.Map.of(), failure);
        }
        if (pending == null) {
            statusMessage.set("No se pudo adaptar la narración al idioma de escucha: " + reason);
        } else {
            statusMessage.set(segmentId.isBlank()
                    ? "No se pudo completar la preparación de exportación: " + reason
                    : "No se pudo preparar 1 fragmento. segmentId=" + segmentId
                    + " · stage=TRANSLATION · reason=" + reason);
        }
        if (failureHandler != null) failureHandler.accept(failure);
    }

    /** Background-only join: cancellation must not release a child project still used by translation. */
    public void awaitNarrationTranslationStopped() throws InterruptedException {
        Thread worker = pendingNarrationTranslationWorker;
        if (worker != null && worker != Thread.currentThread()) worker.join();
    }

    private void failPendingDocumentExportIfActive(String stage, Throwable failure) {
        if (pendingDocumentExport != null) {
            failPendingDocumentExport(stage, failure);
        }
    }

    private static String rootCauseMessage(Throwable failure) { Throwable current = failure; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }

    private void resetPlaybackState() { documentPlaybackRequested = false; playSingleCueOnly = false; singleCuePlaybackRequested = false; waitingForBufferedSegmentAfter = ""; pendingPlaybackStartSegmentId = ""; lastSequentialCueUnitId = ""; resetPlaybackTransportOnly(); playbackTransport.resetRuntimeQueue(); currentPlaybackManifest.set(PlaybackManifest.empty()); }

    public boolean playbackActiveForFullscreenPause() { PlaybackCursor current = playbackCursor.get(); return current != null && current.playing() || playbackTransport.playerPlaying() || playbackTransport.continuationActive() || playbackTransport.sequentialActive(); }
    private boolean playbackPausedOrInactive() { PlaybackCursor current = playbackCursor.get(); return current == null || current.paused(); }
    private void resetPlaybackTransportOnly() { playSingleCueOnly = false; playbackTransport.stopTransport(); theatreAudioPlayback.stop(); playbackTimer.stop(); activePlaybackCue.set(null); playbackCursor.set(PlaybackCursor.stopped()); } private void syncTheatreAudioPlayback(double position) { theatreAudioPlayback.sync(currentProjectDirectory().orElse(null), theatreAudioTrackTimeline(), position, playbackRate.get()); }

    public void exportPodcastWav(Path targetFile) throws IOException {
        statusMessage.set(exportPodcastWavResult(targetFile));
    }

    /** Performs filesystem/audio work without mutating JavaFX state from a worker thread. */
    public String exportPodcastWavResult(Path targetFile) throws IOException {
        Path projectDirectory = requireProjectDirectory();
        NarrationScriptDocument scoped = activeDocumentScopeNarration(currentScript.get());
        NarrationScriptDocument effective = effectiveNarrationForExport(scoped,
                () -> false, ignored -> { });
        AudioCoverageSnapshotAssembler.Result audio = reconciledAudioForExport(effective);
        ProjectSession session = requireSession();
        var plan = workspaceServices.generation().render().buildNarrationRenderPlan()
                .build(effective, session.project());
        PlaybackManifest manifest = workspaceServices.playback().playback()
                .buildPlaybackManifest().build(effective, audio.exportJobs().getFirst(),
                        currentStoryboard.get(), plan, session.project());
        return exportController.exportPodcastWav(
                projectDirectory, manifest, targetFile, playbackRate.get());
    }

    public void exportDiagnosticReport(Path targetFile) throws IOException {
        statusMessage.set(exportController.exportDiagnosticReport(requireSession(), currentScript.get(), currentStoryboard.get(), listPersistedJobsSafely(), targetFile));
    }

    public void exportProjectBundle(Path targetDirectory) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar un paquete portable."));
        statusMessage.set(exportController.exportProjectBundle(session, projectFile, currentScript.get(), currentStoryboard.get(), listPersistedJobsSafely(), targetDirectory, this::saveCurrentProjectAs));
    }

    public void exportSimpleVideoPackage(Path targetDirectory) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video simple."));
        NarrationScriptDocument scoped = activeDocumentScopeNarration(currentScript.get());
        NarrationScriptDocument effective = effectiveNarrationForExport(
                scoped, () -> false, ignored -> { });
        statusMessage.set(exportController.exportSimpleVideoPackage(session, projectFile,
                effective, currentStoryboard.get(), audioJobsForExport(effective),
                targetDirectory, loadOperationalSettingsSafely().video()
                        .silentVisualBlockSeconds(), this::saveCurrentProjectAs));
    }

    public void exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution) throws IOException { exportFinalVideo(targetFile, resolution, 30, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.AUTO, ignored -> { }, () -> false); }

    public void exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException {
        exportFinalVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), progress, cancellationRequested);
    }

    private volatile ExportController.DocumentaryVideoOutcome lastDocumentaryVideoOutcome;
    public ExportController.DocumentaryVideoOutcome lastDocumentaryVideoOutcome() { return lastDocumentaryVideoOutcome; }

    public void exportDocumentStudyTextAudioVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportDocumentStudyTextAudioVideo(targetFile, resolution, framesPerSecond, encoderPolicy, DocumentTextVideoOptions.defaults().withResolution(resolution), progress, cancellationRequested); }

    public void exportDocumentStudyTextAudioVideo(
            Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond,
            com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy,
            DocumentTextVideoOptions textOptions, Consumer<VideoRenderProgress> progress,
            BooleanSupplier cancellationRequested) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de exportar video documental texto+audio."));
        DocumentStudyExportSelection exportSelection = resolveDocumentStudyExportSelection();
        DocumentContentProjection projection = exportSelection.projection();
        ResolvedDocumentProcessingSelection selection = exportSelection.selection();
        NarrationScriptDocument script = effectiveNarrationForExport(selection.narration(),
                cancellationRequested, progress);
        LOGGER.info("document-scope.boundary stage=EFFECTIVE_NARRATION correlationId={} "
                        + "scope={} resolvedPages={} canonicalSegmentIds={} "
                        + "effectiveSegmentIds={} translationTarget={}",
                activeDocumentExportCorrelationId.isBlank()
                        ? selection.selectionRevision() : activeDocumentExportCorrelationId,
                selection.scope(),
                selection.resolvedPageNumbers(), selection.resolvedSegmentIds(),
                script.segments().stream().map(NarrationSegment::id).toList(),
                script.language());
        var result = exportController.exportDocumentStudyTextAudioVideo(
                session, projectFile, projection, script, reconciledAudioForExport(script,
                        activeDocumentExportCorrelationId.isBlank()
                                ? selection.selectionRevision()
                                : activeDocumentExportCorrelationId),
                targetFile, resolution, framesPerSecond, encoderPolicy, textOptions,
                this::saveCurrentProjectAs, progress, cancellationRequested);
        lastDocumentaryVideoOutcome = result;
        Platform.runLater(() -> statusMessage.set(result.summary()));
    }

    private NarrationScriptDocument effectiveNarrationForExport(
            NarrationScriptDocument source, BooleanSupplier cancellationRequested,
            Consumer<VideoRenderProgress> progress) throws IOException {
        if (source == null || source.empty()) return source;
        Optional<NarrationScriptDocument> cached = adaptNarrationLanguage.fromCache(
                source, documentTranslationPreferences(),
                currentProjectDirectory().orElse(null));
        if (cached.isPresent()) return cached.get();
        throw new IOException("La traducción requerida no está completa. "
                + "Usa Renderizar y exportar para preparar únicamente los fragmentos faltantes.");
    }

    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreWorkVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), false, progress, cancellationRequested); }

    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreWorkVideo(targetFile, resolution, framesPerSecond, encoderPolicy, scope, false, progress, cancellationRequested); }

    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, boolean renderUnassignedVisuals, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreWorkVideo(targetFile, resolution, framesPerSecond, encoderPolicy, scope, renderUnassignedVisuals, false, progress, cancellationRequested); }

    public void exportTheatreWorkVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, boolean renderUnassignedVisuals, boolean includeInferredFrames, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { ProjectSession session = requireSession(); Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video teatral limpio.")); NarrationScriptDocument scopedScript = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope); String result = exportController.exportTheatreWork(session, projectFile, scopedScript, audioJobsForExport(scopedScript), targetFile, resolution, framesPerSecond, encoderPolicy, loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), currentStoryboard.get(), renderUnassignedVisuals, includeInferredFrames, ignored -> { }, progress, cancellationRequested); Platform.runLater(() -> statusMessage.set(result)); }

    public List<String> missingTheatreCleanVisualSegmentIds(TheatreExportScope scope) { ProjectSession session = requireSession(); NarrationScriptDocument script = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope); return new com.marcosmoreiradev.docupodcaststudio.application.video.BuildTheatreCleanVideoPlanUseCase().missingVisualSegmentIds(session.project(), script, currentStoryboard.get(), session.projectFile().map(Path::getParent).orElse(null)); }

    public void exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video."));
        NarrationScriptDocument scopedScript;
        if (session.project().metadata().mode() == ProjectMode.THEATRE_PRODUCTION) {
            scopedScript = theatreExportScopeScriptFilter.filter(
                    session, currentScript.get(), scope);
        } else if (session.project().metadata().mode() == ProjectMode.DOCUMENTARY_STUDIO) {
            scopedScript = effectiveNarrationForExport(
                    activeDocumentScopeNarration(currentScript.get()),
                    cancellationRequested, progress);
        } else {
            scopedScript = currentScript.get();
        }
        String exportResult = exportController.exportFinalVideo(session, projectFile, scopedScript, currentStoryboard.get(), audioJobsForExport(scopedScript), targetFile, resolution, framesPerSecond, encoderPolicy, loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), ignored -> { }, progress, cancellationRequested);
        Platform.runLater(() -> statusMessage.set(exportResult));
    }

    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreSpatialVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), companionMode, progress, cancellationRequested); }

    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreSpatialVideo(targetFile, resolution, framesPerSecond, encoderPolicy, scope, companionMode, false, progress, cancellationRequested); }

    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, boolean includeInferredFrames, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException { exportTheatreSpatialVideo(targetFile, resolution, framesPerSecond, encoderPolicy, TheatreExportScope.all(), companionMode, includeInferredFrames, progress, cancellationRequested); }

    public void exportTheatreSpatialVideo(Path targetFile, SimpleVideoResolutionPreset resolution, int framesPerSecond, com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy encoderPolicy, TheatreExportScope scope, com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode companionMode, boolean includeInferredFrames, Consumer<VideoRenderProgress> progress, BooleanSupplier cancellationRequested) throws IOException {
        ProjectSession session = requireSession();
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar video mapa."));
        NarrationScriptDocument scopedScript = theatreExportScopeScriptFilter.filter(session, currentScript.get(), scope);
        String exportResult = exportController.exportTheatreSpatialVideo(session, projectFile, scopedScript,
                audioJobsForExport(scopedScript), targetFile, resolution, framesPerSecond, encoderPolicy,
                loadOperationalSettingsSafely().video().silentVisualBlockSeconds(), currentStoryboard.get(),
                companionMode.frameMode(), scope, includeInferredFrames, ignored -> { }, progress,
                cancellationRequested);
        Platform.runLater(() -> statusMessage.set(exportResult));
    }
    private OperationalSettings loadOperationalSettingsSafely() {
        try {
            return workspaceServices.administration().settings().loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }
    private java.util.List<AudioJobSnapshot> listPersistedJobsSafely() {
        Optional<Path> file = currentProjectFile();
        try { return file.isEmpty() ? java.util.List.of() : workspaceServices.playback().audio().listPersistedAudioJobs().list(file.get().toAbsolutePath().normalize().getParent()); }
        catch (IOException ex) { return java.util.List.of(); }
    }

    public boolean hasAllChunksRendered() {
        try {
            NarrationScriptDocument scoped = activeDocumentScopeNarration(currentScript.get());
            NarrationScriptDocument effective = adaptNarrationLanguage.fromCache(
                    scoped, documentTranslationPreferences(),
                    currentProjectDirectory().orElse(null)).orElse(null);
            return currentReusableAudioCoverage(effective).map(
                    ReusableAudioCoverage.Report::complete).orElse(false);
        } catch (IOException | RuntimeException invalidScope) {
            return false;
        }
    }

    /**
     * Full-script coverage used by the full-document listen command.
     * A non-empty playback manifest is only a cache hit, not proof that every
     * current narration unit has a valid WAV.
     */
    private boolean hasCompleteAudioCoverageForDocument() {
        try {
            NarrationScriptDocument effective = adaptNarrationLanguage.fromCache(
                    currentScript.get(), documentTranslationPreferences(),
                    currentProjectDirectory().orElse(null)).orElse(null);
            return currentReusableAudioCoverage(effective)
                    .map(ReusableAudioCoverage.Report::complete)
                    .orElse(false);
        } catch (RuntimeException invalidCoverage) {
            return false;
        }
    }

    /**
     * Cache/file-only FULL_DOCUMENT readiness. This method never invokes OCR,
     * semantic inference, translation, TTS, composition or rendering.
     */
    public DocumentReadingReadinessSnapshot inspectFullDocumentReadingReadiness() {
        String language = documentTranslationPreferences().listeningLanguage().tag();
        NarrationScriptDocument canonical = currentScript.get();
        boolean semanticReady = canonical != null && !canonical.empty()
                && (currentDocument.get() != null || currentPreparedPdfSource.get() != null);
        PreparedPdfSource pdf = currentPreparedPdfSource.get();
        if (semanticReady && pdf != null) {
            var preparedWorkspace = workspaceServices.project().document()
                    .openPreparedPdfWorkspace();
            int pageCount = preparedWorkspace.pageCount(pdf.workspace());
            Set<Integer> preparedPages = preparedWorkspace.loadPreparedPages(pdf.workspace())
                    .stream()
                    .filter(page -> page.status()
                            != com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                            .PdfPagePreparationStatus.FAILED)
                    .map(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                            .PreparedPdfPage::pageNumber)
                    .collect(java.util.stream.Collectors.toSet());
            semanticReady = pageCount > 0 && preparedPages.size() == pageCount
                    && java.util.stream.IntStream.rangeClosed(1, pageCount)
                    .allMatch(preparedPages::contains);
        }
        if (!semanticReady) {
            return DocumentReadingReadinessSnapshot.unavailable(language);
        }
        try {
            ResolvedDocumentProcessingSelection selection =
                    fullDocumentSelection(canonical);
            var translation = adaptNarrationLanguage.inspectCache(
                    selection.narration(), documentTranslationPreferences(),
                    currentProjectDirectory().orElse(null));
            int reusable = 0;
            int missing = 0;
            int stale = 0;
            int invalid = 0;
            if (translation.effectiveScript().isPresent()) {
                Path root = currentProjectDirectory().orElse(null);
                if (root == null) {
                    missing = selection.resolvedSegmentIds().size();
                } else {
                    ProjectSession session = requireSession();
                    AudioGenerationRequest request = audioGenerationRequestFor(
                            session, translation.effectiveScript().orElseThrow(),
                            root, session.title());
                    ReusableAudioCoverage.Report coverage = reusableAudioCoverage.resolve(
                            request.generationUnits(), audioWorkflow.persistedJobs(root), root);
                    for (ReusableAudioCoverage.Entry entry : coverage.entries()) {
                        switch (entry.state()) {
                            case READY -> reusable++;
                            case MISSING, MISSING_FILE -> missing++;
                            case STALE -> stale++;
                            case INVALID -> invalid++;
                        }
                    }
                }
            } else {
                missing = selection.resolvedSegmentIds().size();
            }
            return new DocumentReadingReadinessSnapshot(
                    DocumentProcessingScope.FULL_DOCUMENT, true,
                    translation.complete(), language,
                    translation.missingSegmentIds(), translation.invalidSegmentIds(),
                    reusable, missing, stale, invalid);
        } catch (IOException | RuntimeException unavailable) {
            return DocumentReadingReadinessSnapshot.unavailable(language);
        }
    }

    public void refreshFullDocumentReadingReadiness() {
        fullDocumentReadingReadiness.set(inspectFullDocumentReadingReadiness());
    }

    /**
     * Single export authority: processing scope first, configured video region
     * second. Audio coverage must never escape this intersection.
     */
    private DocumentStudyExportSelection resolveDocumentStudyExportSelection()
            throws IOException {
        ResolvedDocumentProcessingSelection processingSelection;
        try {
            processingSelection = resolveActiveDocumentProcessingSelection();
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new IOException("No se pudo resolver el alcance documental activo: "
                    + failure.getMessage(), failure);
        }
        DocumentContentProjection source = currentDocumentContentProjection().orElseThrow(() ->
                new IOException("Prepara la lectura de la fuente Word o PDF antes de exportar."));
        DocumentContentProjection scopedProjection =
                filterDocumentContentProjectionBySelection.execute(source, processingSelection);
        ProjectSession session = requireSession();
        NarrationScriptDocument videoNarration = resolveDocumentStudyVideoNarration.execute(
                scopedProjection,
                session.project().study().documentaryVideoConfiguration(),
                session.project().documentListeningPreferences().secondarySemanticPolicy(),
                processingSelection.narration());
        List<String> videoSegmentIds = videoNarration.segments().stream()
                .filter(NarrationSegment::narratable)
                .map(NarrationSegment::id)
                .toList();
        ResolvedDocumentProcessingSelection exportSelection =
                new ResolvedDocumentProcessingSelection(
                        processingSelection.scope(), processingSelection.unit(),
                        processingSelection.requestedStart(), processingSelection.requestedEnd(),
                        processingSelection.resolvedPageNumbers(), videoSegmentIds,
                        processingSelection.sourceDocumentId(), processingSelection.sourceSha(),
                        processingSelection.selectionRevision(), videoNarration);
        LOGGER.info("document-export.region scope={} processingSegments={} "
                        + "configuredSpokenSegments={} excludedAudioSegments={}",
                processingSelection.scope(), processingSelection.resolvedSegmentIds().size(),
                videoSegmentIds.size(),
                Math.max(0, processingSelection.resolvedSegmentIds().size()
                        - videoSegmentIds.size()));
        return new DocumentStudyExportSelection(scopedProjection, exportSelection);
    }

    private record DocumentStudyExportSelection(
            DocumentContentProjection projection,
            ResolvedDocumentProcessingSelection selection) { }

    /** Performs a passive, cache-only documentary export check. */
    public DocumentExportReadinessSnapshot inspectDocumentExportReadiness()
            throws IOException {
        String correlationId = "DOCEXP-CHECK-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16)
                .toUpperCase(java.util.Locale.ROOT);
        ResolvedDocumentProcessingSelection selection =
                resolveDocumentStudyExportSelection().selection();
        var translation = adaptNarrationLanguage.inspectCache(
                selection.narration(), documentTranslationPreferences(),
                currentProjectDirectory().orElse(null));
        List<String> missingAudio = new ArrayList<>();
        List<String> staleAudio = new ArrayList<>();
        List<String> invalidAudio = new ArrayList<>();
        List<String> missingCompositions = new ArrayList<>();
        boolean compositionsReady = false;
        if (translation.effectiveScript().isPresent()) {
            try {
                ProjectSession session = requireSession();
                Path root = currentProjectDirectory().orElseThrow();
                AudioGenerationRequest request = audioGenerationRequestFor(session,
                        translation.effectiveScript().orElseThrow(), root, session.title());
                AudioCoverageSnapshotAssembler.PassiveInspection audio =
                        audioCoverageSnapshotAssembler.inspect(request.generationUnits(),
                                audioWorkflow.persistedJobs(root), root);
                for (ReusableAudioCoverage.Entry entry : audio.coverage().entries()) {
                    String segmentId = entry.unit().sourceSegmentId().isBlank()
                            ? entry.unit().id() : entry.unit().sourceSegmentId();
                    if (entry.state() == ReusableAudioCoverage.State.STALE) {
                        staleAudio.add(segmentId);
                    } else if (entry.state() == ReusableAudioCoverage.State.INVALID) {
                        invalidAudio.add(segmentId);
                    } else if (!entry.ready()) {
                        missingAudio.add(segmentId);
                    }
                }
                missingCompositions.addAll(audio.missingCompositionSegmentIds());
                compositionsReady = audio.compositionsReady();
            } catch (IOException | RuntimeException unavailable) {
                missingAudio.addAll(selection.resolvedSegmentIds());
                missingCompositions.addAll(selection.resolvedSegmentIds());
            }
        } else {
            missingAudio.addAll(selection.resolvedSegmentIds());
            missingCompositions.addAll(selection.resolvedSegmentIds());
        }
        DocumentExportReadinessSnapshot snapshot = new DocumentExportReadinessSnapshot(
                correlationId, selection, translation.effectiveScript(),
                translation.missingSegmentIds(), translation.invalidSegmentIds(),
                missingAudio, staleAudio, invalidAudio, missingCompositions,
                compositionsReady);
        LOGGER.info("document-export.readiness correlationId={} stage=READINESS "
                        + "scope={} pages={} segmentIds={} translationMissing={} "
                        + "translationInvalid={} audioMissing={} audioStale={} audioInvalid={} "
                        + "compositionMissing={} compositionsReady={} readyToRender={}",
                correlationId, selection.scope(), selection.resolvedPageNumbers(),
                selection.resolvedSegmentIds(), snapshot.missingTranslationSegmentIds(),
                snapshot.invalidTranslationSegmentIds(), snapshot.missingAudioSegmentIds(),
                snapshot.staleAudioSegmentIds(), snapshot.invalidAudioSegmentIds(),
                snapshot.missingCompositionSegmentIds(),
                snapshot.compositionsReady(),
                snapshot.readyToRender());
        return snapshot;
    }

    private java.util.List<AudioJobSnapshot> audioJobsForExport(
            NarrationScriptDocument script) throws IOException {
        return reconciledAudioForExport(script).exportJobs();
    }

    private AudioCoverageSnapshotAssembler.Result reconciledAudioForExport(
            NarrationScriptDocument script) throws IOException {
        return reconciledAudioForExport(script, "DOCEXP-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16)
                .toUpperCase(java.util.Locale.ROOT));
    }

    private AudioCoverageSnapshotAssembler.Result reconciledAudioForExport(
            NarrationScriptDocument script, String correlationId) throws IOException {
        ProjectSession session = requireSession();
        Path root = currentProjectDirectory().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de verificar audio para exportar."));
        AudioGenerationRequest request = audioGenerationRequestFor(
                session, script, root, session.title());
        String narrationRevision = AudioCoverageSnapshotAssembler.acousticRevision(
                request.generationUnits());
        java.util.List<String> requestedSegmentIds = request.generationUnits().stream()
                .map(unit -> unit.sourceSegmentId().isBlank()
                        ? unit.id() : unit.sourceSegmentId())
                .distinct().toList();
        LOGGER.info("document-export.audio-boundary correlationId={} narrationRevision={} "
                        + "stage=VIEW_MODEL_REQUEST count={} ids={}",
                correlationId, narrationRevision, requestedSegmentIds.size(),
                requestedSegmentIds);
        for (NarrationSegment segment : script.segments()) {
            LOGGER.info("document-export.current-script correlationId={} narrationRevision={} "
                            + "segmentId={} sourceBlockIds={} textLength={} voiceId={} "
                            + "language={} textHash={}", correlationId, narrationRevision,
                    segment.id(), segment.sourceBlockIds(), segment.narrationText().length(),
                    segment.voiceProfileId(), segment.metadata().getOrDefault("language", ""),
                    com.marcosmoreiradev.docupodcaststudio.domain.audio
                            .AudioSourceFingerprint.generic(segment.narrationText(), "", "")
                            .textSha256());
        }
        java.util.List<AudioJobSnapshot> persisted = audioWorkflow.persistedJobs(root);
        AudioCoverageSnapshotAssembler.Result result = audioCoverageSnapshotAssembler.assemble(
                request.generationUnits(), persisted, root, correlationId);
        for (ReusableAudioCoverage.Entry entry : result.coverage().entries()) {
            String jobId = persisted.stream().filter(job -> job.segments().stream()
                            .anyMatch(audio -> audio.segmentId().equals(entry.unit().id())))
                    .map(AudioJobSnapshot::jobId).findFirst().orElse("");
            String path = entry.audio() == null ? "" : entry.audio().audioRelativePath();
            boolean exists = !path.isBlank() && java.nio.file.Files.isRegularFile(
                    root.resolve(path).normalize());
            LOGGER.info("document-export.audio-unit correlationId={} narrationRevision={} "
                            + "unitId={} sourceSegmentId={} textHash={} fingerprint={} "
                            + "jobId={} status={} wavPath={} wavExists={} mismatchReason={}", correlationId,
                    narrationRevision, entry.unit().id(), entry.unit().sourceSegmentId(),
                    entry.unit().sourceFingerprint().textSha256(),
                    entry.unit().sourceFingerprint(), jobId, entry.state(), path, exists,
                    entry.mismatchReason());
        }
        java.util.List<String> coveredSegmentIds = result.coverage().entries().stream()
                .filter(ReusableAudioCoverage.Entry::ready)
                .map(entry -> entry.unit().sourceSegmentId().isBlank()
                        ? entry.unit().id() : entry.unit().sourceSegmentId())
                .distinct().toList();
        java.util.List<String> composedSegmentIds = result.exportJobs().stream()
                .flatMap(job -> job.segments().stream())
                .map(com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot::segmentId)
                .distinct().toList();
        LOGGER.info("document-export.audio-boundary correlationId={} narrationRevision={} "
                        + "stage=COVERAGE count={} ids={} missing={}",
                correlationId, narrationRevision, coveredSegmentIds.size(), coveredSegmentIds,
                result.coverage().missingOrStale().stream()
                        .map(entry -> entry.unit().id()).toList());
        LOGGER.info("document-export.audio-boundary correlationId={} narrationRevision={} "
                        + "stage=COMPOSITION count={} ids={} composed={} reused={} "
                        + "manifest={} failures={}", correlationId, narrationRevision,
                composedSegmentIds.size(), composedSegmentIds, result.composedSegments(),
                result.reusedSegments(), result.manifestRelativePath(),
                result.compositionFailures());
        if (!result.readyForPreflight()) {
            throw new IOException("La cobertura final de audio no esta completa: "
                    + result.coverage().missingOrStale().size()
                    + " fragmento(s) requieren generacion o reparacion.");
        }
        return result;
    }

    public boolean hasChunksRenderedForTheatreScope(TheatreExportScope scope) {
        ProjectSession session = sessions.activeSession().orElse(null);
        return currentReusableAudioCoverage(theatreExportScopeScriptFilter.filter(
                session, currentScript.get(), scope)).map(
                ReusableAudioCoverage.Report::complete).orElse(false);
    }

    private Optional<ReusableAudioCoverage.Report> currentReusableAudioCoverage(
            NarrationScriptDocument script) {
        if (script == null || script.empty()) return Optional.empty();
        try {
            ProjectSession session = requireSession();
            Path root = currentProjectDirectory().orElseThrow();
            AudioGenerationRequest request = audioGenerationRequestFor(
                    session, script, root, session.title());
            return Optional.of(reusableAudioCoverage.resolve(request.generationUnits(),
                    audioWorkflow.persistedJobs(root), root));
        } catch (IOException | RuntimeException unavailable) {
            return Optional.empty();
        }
    }

    public void clearSelectedDocumentBlock() {
        selectedDocumentBlockId.set(""); selectedScriptSegmentId.set(""); clearVisualFragmentSelection(); clearDocumentTextRange();
        selectedPdfRegion.set(null);
        selectedPdfVisualTarget.set(null);
        setDocumentProcessingScope(DocumentProcessingScope.FULL_DOCUMENT);
        statusMessage.set("Selección de fragmento limpia. Alcance: lectura completa.");
        refreshProjectState();
    }

    public Optional<UserVisibleDecision> inspectProjectIntegrityDecision() {
        try {
            Optional<ProjectSession> session = sessions.activeSession(); Optional<Path> file = currentProjectFile();
            if (session.isEmpty() || file.isEmpty()) { throw new IOException("Guarda el proyecto antes de validar su integridad."); }
            ProjectWorkspaceHydration hydration = new ProjectWorkspaceHydration(
                    session.get().documentSource(), session.get().narrationScript(), session.get().storyboard());
            ProjectIntegrityInspectionOutcome outcome = projectController.inspectIntegrity(session.get(), file.get(), hydration, listPersistedJobsSafely());
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
            NarrationScriptDocument scoped = activeDocumentScopeNarration(currentScript.get());
            NarrationScriptDocument effective = adaptNarrationLanguage.fromCache(
                    scoped, documentTranslationPreferences(),
                    currentProjectDirectory().orElse(null)).orElse(scoped);
            var report = workspaceServices.exports().export().inspectExportReadiness().inspect(session.get().project(), currentProjectFile().orElse(null), effective, currentStoryboard.get(), listPersistedJobsSafely());
            statusMessage.set("Estado de exportación: " + report.status().displayName() + " · exportables " + report.exportableCount() + " · bloqueadas " + report.blockedCount() + ".");
            if (!report.hasBlockedOutput()) return Optional.empty();
            StringBuilder detail = new StringBuilder();
            for (var item : report.items()) { if (item.blocked()) detail.append(item.kind().displayName()).append(": ").append(String.join(", ", item.missingRequirements())).append(System.lineSeparator()); }
            return Optional.of(UserVisibleDecision.warning("Exportación con bloqueos", "Hay salidas bloqueadas. Revisa los faltantes antes de exportar. " + detail.toString().strip()));
        } catch (IOException | RuntimeException ex) { UserVisibleDecision decision = UserVisibleDecision.error("No se pudo revisar el estado de exportación", ex.getMessage(), ex.toString()); statusMessage.set(decision.headline() + ": " + decision.message()); return Optional.of(decision); }
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
        ListeningSessionState state = documentController.listeningSession(
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
            managedAudioChunksAvailable.set(false);
            sessionStore.clearProject();
            currentProjectMode.set(ProjectMode.defaultMode());
            windowTitle.set("DocuPodcast Studio — Inicio");
            return;
        }
        ProjectSession session = requireSession();
        if (!managedAudioChunksAvailable.get()) {
            managedAudioChunksAvailable.set(hasManagedAudioChunksOnDisk());
        }
        spatialFrameMode.set(TheatreStageGeometry.normalizeFrameMode(session.project().viewState().getOrDefault("theatre.presentationMode", "fragments")));
        readAfterColonForNarration.set(session.project().documentReadAfterColon());
        sessionStore.updateProject(session.project(), true, session.dirty());
        currentProjectMode.set(new com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy().resolve(session.project()));
        String suffix = session.dirty() ? " *" : "";
        String fileLabel = session.projectFile()
                .map(path -> " — " + path.getFileName())
                .orElse(" — sin guardar");
        windowTitle.set("DocuPodcast Studio — " + session.title() + fileLabel + suffix);
    }

    private boolean hasManagedAudioChunksOnDisk() {
        Path root = currentProjectDirectory().orElse(null);
        if (root == null) return false;
        Path jobs = root.resolve("jobs").normalize();
        if (!jobs.startsWith(root) || !java.nio.file.Files.isDirectory(jobs)) return false;
        try (java.util.stream.Stream<Path> files = java.nio.file.Files.walk(jobs)) {
            // Any persisted job artifact is user-manageable, including a broken
            // WAV or a cancelled job whose status file never recorded a path.
            return files.anyMatch(java.nio.file.Files::isRegularFile);
        } catch (IOException unreadableJobs) {
            return false;
        }
    }

    @FunctionalInterface
    private interface NarrativeVisualWork {
        String run(ProjectSession session,
                   ReadableDocument document,
                   NarrationScriptDocument script,
                   List<AudioJobSnapshot> jobs,
                   Consumer<String> progress,
                   BooleanSupplier cancellationRequested) throws Exception;
    }

}
