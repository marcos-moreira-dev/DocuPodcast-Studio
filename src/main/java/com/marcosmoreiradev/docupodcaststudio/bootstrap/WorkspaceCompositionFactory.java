package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GetGuideTopicUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.guide.SearchGuideTopicsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.resources.ExportAiResourcesUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.assets.RegisterProjectAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.fragment.BuildFragmentWorkspaceProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.fragment.FragmentWorkspaceProjectionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.BuildGrammarTemplateUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ImportProjectGrammarMarkdownUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.CancelAudioGenerationJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.DeletePersistedAudioJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListAudioGenerationJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.InspectAudioJobMaintenanceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListPersistedAudioJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListAudioProcessDiagnosticsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.GetAudioEngineDescriptorUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListAudioEngineAvailabilityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.InspectAudioEngineReadinessUiUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.LoadPersistedAudioJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ManualAudioSegmentJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ResumePersistedAudioJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.SubmitAudioGenerationJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.playback.AdvancePlaybackCursorUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.process.InspectProcessJobContractUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.process.ListProcessJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.process.BuildProcessJobDashboardProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.process.AudioJobProcessMapper;
import com.marcosmoreiradev.docupodcaststudio.application.process.VideoRenderJobProcessMapper;
import com.marcosmoreiradev.docupodcaststudio.application.video.SubmitVideoRenderJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.CancelVideoRenderJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.playback.BuildPlaybackManifestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.playback.SeekPlaybackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.render.BuildNarrationRenderPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.render.BuildRenderUnitPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.assets.RemoveProjectAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ImportDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildDocumentOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildDocumentContentProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfEnhancedOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfVisualDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfVisualReadingProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvePdfPageMapUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.CapturePdfVisualRegionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializePdfDocumentContentAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.CapturePdfAnalysisVisualEvidenceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.CreatePdfFromImageFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSourceImportService;
import com.marcosmoreiradev.docupodcaststudio.application.document.UpdateDocumentBlockTypeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializeImportedDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.RefreshSourceDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ReconcileTextAnchorsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.RenderPdfVisualPageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.SearchPdfTextUseCase;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.BlockPdfSemanticPageResponseParser;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxNativeTextEvidenceExtractor;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfPageMapRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfOperationAttemptRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PopplerPdfNativePageExtractor;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeLocator;
import com.marcosmoreiradev.docupodcaststudio.application.document.PrepareDocumentListeningUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PrepareListeningSessionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfPageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.AnalyzePdfPageSemanticallyUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ReconcilePdfOperationAttemptsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPagePreparationScheduler;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfScopeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvePdfPreparationScopeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.AnalyzePreparedPdfLayoutUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DiagnosePreparedPdfWorkspaceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDerivedTreatmentRegistry;
import com.marcosmoreiradev.docupodcaststudio.application.document.UpdatePdfDerivedTreatmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.GeneratePdfDerivedTreatmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.OpenPreparedPdfWorkspaceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.CreatePreparedPdfSessionWorkspaceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolveDocumentSelectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPreparedPdfRegionContextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.UpdatePreparedPdfRegionOverrideUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.LocatePdfAudioSourceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ListPdfDerivedTreatmentsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.RunDocumentLocalAnalysisBatchUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolveDocumentTechnicalElementsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfReviewDashboardUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfProcessingCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.document.ReviewPdfTreatmentBatchUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDerivedTreatmentEngineRegistry;
import com.marcosmoreiradev.docupodcaststudio.application.document.RuleBasedSmallTableNarrationEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.AcademicTableNarrationEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalVisualPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalContextCorrectionPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalNarratabilityReviewPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalMathPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalTableExplanationPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalPpTableStructurePdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyTextVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildStudyProblemsProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.ExportDocumentStudyVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxStudyProblemPdfExporter;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreProductionProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreAudioTrackTimelineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ExportTheatreVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.RemoveTheatreAudioTrackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.RenderTheatreChoralVoiceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFragmentLinkPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGlobalVisualAssetProjectionProvider;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.UpsertTheatreAudioTrackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.CreateProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.PrepareRecordingActionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.StartAudioRecordingUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.StopAudioRecordingUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.CancelAudioRecordingUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.BuildPreparedReadingProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.MaterializeNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.UpdateNarrationSegmentTextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.ValidateNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.AssignVoiceToSegmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.BuildVoiceAssignmentOptionsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.BuildVoiceRegistrationWizardPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.BuildVoiceToneRecordingPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.GenerateVoiceTestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.DownloadVoiceReferenceSampleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.DeleteVoiceReferenceSampleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ResolveVoiceToneReferenceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.CreateDefaultVoiceLibraryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.MaterializeVoiceLibraryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ImportVoiceSampleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ValidateVoiceLibraryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceCapabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.project.OpenProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.LoadProjectWorkspaceArtifactsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.SaveProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ValidateProjectPayloadUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ValidateProjectWorkspaceIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRoundTripUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.InspectProjectIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.ApplyReadingProfileUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.CreateDefaultReadingProfileUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.PreviewReadingProfileUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.services.AssetApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.AudioApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.DocumentApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.DocumentStudyApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportDiagnosticReportUseCase;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.observability.FileSupportBundleExporter;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportPodcastAudioUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportPodcastWavUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportProjectBundleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.InspectExportReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.InspectFinalAudioExportReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectExportFormatPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.ExportSimpleVideoPackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.ExportNarrativeVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.InspectFinalVideoSmokeReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.RenderFinalVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.services.ExportApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ExampleApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.FragmentApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.GrammarApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.GuideApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.AiResourceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ProjectApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.PlaybackApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ProcessApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ReadingProfileApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.RecordingApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.RenderApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.ScriptApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BindImageToSegmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BuildStoryboardFromScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BuildStoryboardFromImageLayersUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.visual.BuildVisualProductionProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.visual.BuildVisualPromptContextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualAssetTracePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImportImageAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.MaterializeStoryboardUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreGeneratedFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ValidateStoryboardUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.services.StoryboardApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.TheatreApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.VoiceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.VisualProductionApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.services.MediaApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.media.ImportUserMediaAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.media.UserMediaFormatPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.AssessComputeAccelerationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectComputeEnvironmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.PrepareXttsPytorchCudaUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.RunXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ProcessXttsCudaRuntimeProbeGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.compute.WindowsComputeDeviceDiscoveryGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxRenderEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxImageFolderPdfBuilder;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBookmarkOutlineHintProvider;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.TesseractPdfOcrEngine;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.WavAudioDurationProbe;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.playback.JavaSoundSegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.engines.AppStartupEnginePreflightUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.engines.BuildHumanEnginePreflightSummaryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.engines.InspectAiEnginesPreflightUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSmokeTestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectLocalTheatreImageSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.RunXttsReadinessSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ConfirmXttsSmokePlaybackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadXttsOfficialModelUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadPiperPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadLocalTheatreImagePackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportXttsModelFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportPiperVoiceFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportLocalTheatreImagePackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxComponentImportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportLocalTheatreImageRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PrepareLocalTheatreImageRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PrepareXttsPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.SelectPiperAsEngineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.SelectXttsAsEngineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.ImportFfmpegRuntimeFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.DownloadFfmpegPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.BuildSimpleVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.InspectFinalVideoRuntimeStatusUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SaveOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ValidateOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.AuditEngineArtifactsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.BuildGuiSmokeChecklistUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.ApplicationRuntimeRoots;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ClasspathExampleProjectCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.examples.CreateExampleProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.examples.InspectExampleProjectReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.grammar.FileSystemProjectSemanticsRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Wires the dependency bundles of each workspace without a DI framework. */
public final class WorkspaceCompositionFactory {
    public WorkspaceApplicationServices create(InfrastructureServices infrastructure,
                                               MediaCapabilityService mediaCapabilities,
                                               Path applicationRoot) {
        return create(infrastructure, mediaCapabilities, ApplicationRuntimeRoots.unified(applicationRoot));
    }

    public WorkspaceApplicationServices create(InfrastructureServices infrastructure,
                                               MediaCapabilityService mediaCapabilities,
                                               ApplicationRuntimeRoots runtimeRoots) {
        DefaultExternalProcessRunner processRunner = new DefaultExternalProcessRunner();
        ApplicationRuntimeRoots resolvedRoots = java.util.Objects.requireNonNull(runtimeRoots, "runtime roots");
        Path resolvedApplicationRoot = resolvedRoots.runtimeRoot();
        Path installationRoot = resolvedRoots.installationRoot();
        TesseractRuntimeLocator tesseractLocator = new TesseractRuntimeLocator();
        Supplier<String> tesseractCommand = () -> tesseractLocator
                .locate(loadOperationalSettingsSafely(infrastructure), resolvedApplicationRoot)
                .command();
        Supplier<OperationalSettings.OcrSettings> ocrSettings = () -> loadOperationalSettingsSafely(infrastructure).ocr();
        EmbeddedFfmpegLocator ffmpegLocator = new EmbeddedFfmpegLocator();
        FfmpegRuntimeProbeUseCase ffmpegProbe = new FfmpegRuntimeProbeUseCase(processRunner);
        JsonPdfOperationAttemptRepository pdfOperationAttempts =
                new JsonPdfOperationAttemptRepository();
        ProjectApplicationServices project = new ProjectApplicationServices(
                new CreateProjectUseCase(),
                new SaveProjectUseCase(infrastructure.projectRepository()),
                new OpenProjectUseCase(infrastructure.projectRepository()),
                new ValidateProjectPayloadUseCase(),
                new LoadProjectWorkspaceArtifactsUseCase(
                        infrastructure.importedDocumentWorkspaceRepository(),
                        infrastructure.narrationScriptWorkspaceRepository(),
                        infrastructure.storyboardWorkspaceRepository()),
                new ValidateProjectWorkspaceIntegrityUseCase(),
                new ReconcilePdfOperationAttemptsUseCase(pdfOperationAttempts),
                new InspectProjectIntegrityUseCase(infrastructure.audioJobRepository()),
                new ProjectRoundTripUseCase(
                        infrastructure.projectRepository(),
                        infrastructure.importedDocumentWorkspaceRepository(),
                        infrastructure.narrationScriptWorkspaceRepository(),
                        infrastructure.storyboardWorkspaceRepository(),
                        infrastructure.audioJobRepository())
        );
        AssetApplicationServices assets = new AssetApplicationServices(
                new RegisterProjectAssetUseCase(),
                new RemoveProjectAssetUseCase()
        );
        ImportDocumentUseCase importDocument = new ImportDocumentUseCase(List.copyOf(infrastructure.documentImporters()));
        PdfBoxRenderEngine pdfRenderEngine = new PdfBoxRenderEngine();
        BuildDocumentOutlineUseCase buildDocumentOutline = new BuildDocumentOutlineUseCase();
        TesseractPdfOcrEngine tesseractPdfOcrEngine =
                new TesseractPdfOcrEngine(pdfRenderEngine, processRunner,
                        tesseractCommand);
        BuildPdfOcrTextLayerUseCase buildPdfOcrTextLayer =
                new BuildPdfOcrTextLayerUseCase(tesseractPdfOcrEngine);
        JsonPreparedPdfDocumentRepository preparedPdfRepository = new JsonPreparedPdfDocumentRepository();
        PdfProcessingCapabilityService pdfProcessingCapabilities =
                new PdfProcessingCapabilityService(mediaCapabilities,
                        () -> !tesseractCommand.get().isBlank());
        ResolvePdfPageMapUseCase resolvePdfPageMap = ResolvePdfPageMapUseCase.fromSystemProperty(
                preparedPdfRepository, new JsonPdfPageMapRepository());
        CreatePreparedPdfSessionWorkspaceUseCase createPreparedPdfSessionWorkspace =
                new CreatePreparedPdfSessionWorkspaceUseCase(
                        preparedPdfRepository, new BuildPdfVisualDocumentUseCase(pdfRenderEngine));
        DocumentSourceImportService documentSourceImport =
                new DocumentSourceImportService(importDocument, createPreparedPdfSessionWorkspace);
        OpenPreparedPdfWorkspaceUseCase openPreparedPdfWorkspace =
                new OpenPreparedPdfWorkspaceUseCase(
                        preparedPdfRepository, pdfOperationAttempts);
        AnalyzePdfPageSemanticallyUseCase semanticPageReader =
                new AnalyzePdfPageSemanticallyUseCase(
                        pdfRenderEngine, mediaCapabilities,
                        new BlockPdfSemanticPageResponseParser(),
                        pdfOperationAttempts,
                        new PdfBoxNativeTextEvidenceExtractor(),
                        java.time.Duration.ofMinutes(20),
                        com.marcosmoreiradev.docupodcaststudio.media.api
                                .PdfVlmRuntimeProfile.fromSystem(),
                        tesseractPdfOcrEngine);
        PreparePdfPageUseCase preparePdfPage = new PreparePdfPageUseCase(
                buildPdfOcrTextLayer, null, ocrSettings, preparedPdfRepository,
                new PdfBoxNativeTextEvidenceExtractor(), semanticPageReader)
                .withAlternateNativeExtractor(new PopplerPdfNativePageExtractor());
        PdfPagePreparationScheduler pdfPagePreparationScheduler =
                new PdfPagePreparationScheduler((request, priority) -> {
                    if (!preparePdfPage.requiresOcrAdmission(request)) {
                        return preparePdfPage.execute(request);
                    }
                    com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority
                            computePriority = priority
                            == com.marcosmoreiradev.docupodcaststudio.application.document
                            .PdfPreparationPriority.URGENT
                            ? com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority
                            .INTERACTIVE_ANALYSIS
                            : com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority
                            .BACKGROUND;
                    var admission = new com.marcosmoreiradev.docupodcaststudio.media.api
                            .ComputeAdmissionRequest(
                            "pdf-page-" + request.workspace().sourceSha256()
                                    + "-" + request.pageNumber(),
                            "prepare-pdf-page-" + request.pageNumber(),
                            computePriority,
                            com.marcosmoreiradev.docupodcaststudio.media.api
                                    .ComputeWorkloadKind.DOCUMENT_PREPARATION,
                            com.marcosmoreiradev.docupodcaststudio.media.api
                                    .ComputeResourceDemand.of(
                                    com.marcosmoreiradev.docupodcaststudio.media.api
                                            .ResourceId.CPU_HEAVY),
                            request.cancellationToken()::cancelled);
                    try (var ignored =
                                 mediaCapabilities.resourceScheduler().acquire(admission)) {
                        return preparePdfPage.execute(request);
                    } catch (InterruptedException cancelled) {
                        Thread.currentThread().interrupt();
                        return new com.marcosmoreiradev.docupodcaststudio.application
                                .document.PreparePdfPageResult(
                                request.pageNumber(), null, false, true,
                                java.util.List.of());
                    }
                });
        ResolvePdfPreparationScopeUseCase resolvePdfPreparationScope =
                new ResolvePdfPreparationScopeUseCase(preparedPdfRepository);
        PreparePdfScopeUseCase preparePdfScope = new PreparePdfScopeUseCase(
                resolvePdfPreparationScope, pdfPagePreparationScheduler);
        CapturePdfVisualRegionUseCase capturePdfVisualRegion =
                new CapturePdfVisualRegionUseCase(pdfRenderEngine);
        MaterializePdfDocumentContentAssetUseCase materializePdfDocumentContentAsset =
                new MaterializePdfDocumentContentAssetUseCase(capturePdfVisualRegion);
        var materializeWordDocumentContentAsset =
                new com.marcosmoreiradev.docupodcaststudio.application.document
                        .MaterializeWordDocumentContentAssetUseCase();
        DocumentApplicationServices document = new DocumentApplicationServices(
                importDocument,
                documentSourceImport,
                new MaterializeImportedDocumentUseCase(infrastructure.importedDocumentWorkspaceRepository()),
                new UpdateDocumentBlockTypeUseCase(),
                new RefreshSourceDocumentUseCase(documentSourceImport),
                new ReconcileTextAnchorsUseCase(),
                new PrepareDocumentListeningUseCase(),
                new PrepareListeningSessionUseCase(),
                buildDocumentOutline,
                new BuildDocumentContentProjectionUseCase(openPreparedPdfWorkspace),
                new BuildPdfEnhancedOutlineUseCase(preparedPdfRepository,
                        new PdfBookmarkOutlineHintProvider()),
                new BuildPdfVisualReadingProjectionUseCase(preparedPdfRepository, resolvePdfPageMap),
                buildPdfOcrTextLayer,
                new BuildPdfVisualDocumentUseCase(pdfRenderEngine),
                new RenderPdfVisualPageUseCase(pdfRenderEngine),
                new SearchPdfTextUseCase(preparedPdfRepository),
                openPreparedPdfWorkspace,
                createPreparedPdfSessionWorkspace,
                new ResolveDocumentSelectionUseCase(preparedPdfRepository),
                new BuildPreparedPdfRegionContextUseCase(preparedPdfRepository),
                new UpdatePreparedPdfRegionOverrideUseCase(preparedPdfRepository),
                new LocatePdfAudioSourceUseCase(),
                pdfPagePreparationScheduler,
                resolvePdfPreparationScope,
                preparePdfScope,
                new AnalyzePreparedPdfLayoutUseCase(preparedPdfRepository, null),
                PdfDerivedTreatmentRegistry.localDefaults(),
                new GeneratePdfDerivedTreatmentUseCase(preparedPdfRepository,
                        new PdfDerivedTreatmentEngineRegistry(
                               java.util.List.of(
                                       new RuleBasedSmallTableNarrationEngine(),
                                       new AcademicTableNarrationEngine(),
                                       new TransversalPpTableStructurePdfTreatmentEngine(
                                               mediaCapabilities),
                                       new TransversalTableExplanationPdfTreatmentEngine(
                                               mediaCapabilities),
                                        new TransversalMathPdfTreatmentEngine(mediaCapabilities),
                                        new TransversalVisualPdfTreatmentEngine(mediaCapabilities),
                                        new TransversalContextCorrectionPdfTreatmentEngine(
                                                mediaCapabilities),
                                         new TransversalNarratabilityReviewPdfTreatmentEngine(
                                                 mediaCapabilities))),
                        pdfOperationAttempts),
                new RunDocumentLocalAnalysisBatchUseCase(mediaCapabilities),
                new ResolveDocumentTechnicalElementsUseCase(preparedPdfRepository),
                new UpdatePdfDerivedTreatmentUseCase(preparedPdfRepository),
                new ListPdfDerivedTreatmentsUseCase(preparedPdfRepository),
                new BuildPdfReviewDashboardUseCase(
                        preparedPdfRepository, pdfOperationAttempts),
                pdfProcessingCapabilities,
                new ReviewPdfTreatmentBatchUseCase(preparedPdfRepository),
                new DiagnosePreparedPdfWorkspaceUseCase(preparedPdfRepository),
                new CapturePdfAnalysisVisualEvidenceUseCase(pdfRenderEngine),
                materializePdfDocumentContentAsset,
                materializeWordDocumentContentAsset,
                new com.marcosmoreiradev.docupodcaststudio.application.document
                        .PrepareWordSemanticImagesUseCase(
                        mediaCapabilities, materializeWordDocumentContentAsset),
                capturePdfVisualRegion,
                new CreatePdfFromImageFolderUseCase(new PdfBoxImageFolderPdfBuilder())
        );
        ReadingProfileApplicationServices readingProfile = new ReadingProfileApplicationServices(
                new CreateDefaultReadingProfileUseCase(),
                new ApplyReadingProfileUseCase(),
                new PreviewReadingProfileUseCase()
        );
        BuildNarrationScriptUseCase internalReadingPayloadBuilder =
                new BuildNarrationScriptUseCase();
        ScriptApplicationServices script = new ScriptApplicationServices(
                new BuildPreparedReadingProjectionUseCase(internalReadingPayloadBuilder),
                internalReadingPayloadBuilder,
                new BuildPreparedPdfNarrationUseCase(preparedPdfRepository, resolvePdfPageMap),
                new ValidateNarrationScriptUseCase(),
                new UpdateNarrationSegmentTextUseCase(),
                new MaterializeNarrationScriptUseCase(infrastructure.narrationScriptWorkspaceRepository())
        );
        ListAudioEngineAvailabilityUseCase listAudioEngineAvailability =
                new ListAudioEngineAvailabilityUseCase(mediaCapabilities.platform());
        SubmitAudioGenerationJobUseCase submitAudio = new SubmitAudioGenerationJobUseCase(infrastructure.audioGenerationGateway());
        LoadPersistedAudioJobUseCase loadAudio = new LoadPersistedAudioJobUseCase(infrastructure.audioJobRepository());
        ManualAudioSegmentJobUseCase manualAudio = new ManualAudioSegmentJobUseCase(infrastructure.audioJobRepository(), new WavAudioDurationProbe());
        AudioApplicationServices audio = new AudioApplicationServices(
                submitAudio,
                new ResumePersistedAudioJobUseCase(infrastructure.audioGenerationGateway()),
                new CancelAudioGenerationJobUseCase(infrastructure.audioGenerationGateway()),
                new ListAudioGenerationJobsUseCase(infrastructure.audioGenerationGateway()),
                new ListPersistedAudioJobsUseCase(infrastructure.audioJobRepository()),
                loadAudio,
                new DeletePersistedAudioJobsUseCase(infrastructure.audioJobRepository()),
                new GetAudioEngineDescriptorUseCase(infrastructure.audioGenerationGateway()),
                listAudioEngineAvailability,
                new InspectAudioEngineReadinessUiUseCase(listAudioEngineAvailability),
                new ListAudioProcessDiagnosticsUseCase(infrastructure.audioProcessDiagnosticsRepository()),
                new InspectAudioJobMaintenanceUseCase(),
                manualAudio
        );
        RecordingApplicationServices recording = new RecordingApplicationServices(
                new PrepareRecordingActionUseCase(),
                new StartAudioRecordingUseCase(infrastructure.audioRecordingGateway()),
                new StopAudioRecordingUseCase(infrastructure.audioRecordingGateway()),
                new CancelAudioRecordingUseCase(infrastructure.audioRecordingGateway())
        );
        PlaybackApplicationServices playback = new PlaybackApplicationServices(
                new SeekPlaybackUseCase(),
                new AdvancePlaybackCursorUseCase(),
                new BuildPlaybackManifestUseCase(),
                infrastructure.segmentAudioPlayer(),
                new JavaSoundSegmentAudioPlayer(),
                new JavaSoundSegmentAudioPlayer()
        );
        RenderApplicationServices render = new RenderApplicationServices(
                new BuildNarrationRenderPlanUseCase(),
                new BuildRenderUnitPlanUseCase()
        );

        UpsertTheatreStoryboardFrameVariantUseCase theatreFrameVariants = new UpsertTheatreStoryboardFrameVariantUseCase();
        StoryboardApplicationServices storyboard = new StoryboardApplicationServices(
                new BuildStoryboardFromScriptUseCase(),
                new BuildStoryboardFromImageLayersUseCase(),
                new BindImageToSegmentUseCase(),
                new ImportImageAssetUseCase(infrastructure.imageAssetRepository()),
                theatreFrameVariants,
                new UpsertTheatreGeneratedFrameVariantUseCase(theatreFrameVariants),
                new ValidateStoryboardUseCase(),
                new MaterializeStoryboardUseCase(infrastructure.storyboardWorkspaceRepository())
        );
        VoiceCapabilityPolicy voiceCapabilityPolicy = new VoiceCapabilityPolicy();
        VoiceApplicationServices voice = new VoiceApplicationServices(
                new CreateDefaultVoiceLibraryUseCase(),
                new ValidateVoiceLibraryUseCase(),
                new AssignVoiceToSegmentUseCase(),
                new MaterializeVoiceLibraryUseCase(infrastructure.voiceLibraryWorkspaceRepository()),
                new ImportVoiceSampleUseCase(infrastructure.voiceSampleRepository()),
                new DownloadVoiceReferenceSampleUseCase(infrastructure.voiceSampleRepository()),
                new DeleteVoiceReferenceSampleUseCase(infrastructure.voiceSampleRepository()),
                new BuildVoiceRegistrationWizardPlanUseCase(),
                new BuildVoiceToneRecordingPlanUseCase(),
                new BuildVoiceAssignmentOptionsUseCase(voiceCapabilityPolicy),
                new ResolveVoiceToneReferenceUseCase(),
                new GenerateVoiceTestUseCase(voiceCapabilityPolicy, new ResolveVoiceToneReferenceUseCase(),
                        infrastructure.voiceTestSynthesisGateway(),
                        new com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver(
                                installationRoot, resolvedApplicationRoot)),
                voiceCapabilityPolicy
        );

        RenderFinalVideoPlanUseCase finalVideoRenderer = new RenderFinalVideoPlanUseCase(mediaCapabilities);
        ExportApplicationServices export = new ExportApplicationServices(
                new ExportPodcastWavUseCase(),
                new ExportPodcastAudioUseCase(
                        new ExportPodcastWavUseCase(),
                        ffmpegLocator,
                        ffmpegProbe,
                        processRunner),
                new ExportDiagnosticReportUseCase(),
                new FileSupportBundleExporter(),
                new ExportProjectBundleUseCase(infrastructure.projectBundleExporter()),
                new InspectExportReadinessUseCase(),
                new InspectFinalAudioExportReadinessUseCase(new ExportPodcastWavUseCase(), ffmpegLocator, ffmpegProbe),
                new ExportSimpleVideoPackageUseCase(),
                finalVideoRenderer,
                new ExportDocumentStudyVideoUseCase(finalVideoRenderer),
                new ExportNarrativeVideoUseCase(
                        new BuildSimpleVideoPlanUseCase(),
                        finalVideoRenderer),
                new ExportTheatreVideoUseCase(finalVideoRenderer),
                new InspectFinalVideoSmokeReadinessUseCase(),
                new ProjectExportFormatPolicy()
        );
        ExampleApplicationServices examples = new ExampleApplicationServices(
                new ClasspathExampleProjectCatalog(),
                new CreateExampleProjectUseCase(),
                new InspectExampleProjectReadinessUseCase()
        );
        GuideApplicationServices guide = new GuideApplicationServices(
                infrastructure.guideCatalog(),
                new GetGuideTopicUseCase(infrastructure.guideCatalog()),
                new SearchGuideTopicsUseCase(infrastructure.guideCatalog())
        );
        AiResourceApplicationServices resources = new AiResourceApplicationServices(
                infrastructure.aiResourceCatalog(),
                new ExportAiResourcesUseCase(infrastructure.aiResourceExporter())
        );
        MediaApplicationServices media = new MediaApplicationServices(
                new ImportUserMediaAssetUseCase(infrastructure.userMediaAssetRepository(), infrastructure.videoAudioExtractionGateway(),
                        infrastructure.audioNormalizationGateway(), new UserMediaFormatPolicy(), new WavAudioDurationProbe()),
                new UserMediaFormatPolicy(),
                new WavAudioDurationProbe()
        );
        ProcessApplicationServices process = new ProcessApplicationServices(
                new ListProcessJobsUseCase(
                        infrastructure.audioJobRepository(),
                        new AudioJobProcessMapper(),
                        infrastructure.videoRenderJobRepository(),
                        new VideoRenderJobProcessMapper()),
                new InspectProcessJobContractUseCase(),
                new BuildProcessJobDashboardProjectionUseCase(),
                new SubmitVideoRenderJobUseCase(infrastructure.videoRenderJobRepository()),
                new CancelVideoRenderJobUseCase(infrastructure.videoRenderJobRepository())
        );
        ValidateOperationalSettingsUseCase validateSettings = new ValidateOperationalSettingsUseCase();
        SettingsApplicationServices settings = new SettingsApplicationServices(
                new LoadOperationalSettingsUseCase(infrastructure.operationalSettingsRepository()),
                new SaveOperationalSettingsUseCase(infrastructure.operationalSettingsRepository(), validateSettings),
                validateSettings,
                new InspectComputeEnvironmentUseCase(new WindowsComputeDeviceDiscoveryGateway(processRunner)),
                new AssessComputeAccelerationUseCase(),
                new InspectXttsCudaSmokeUseCase(),
                new PrepareXttsPytorchCudaUseCase(processRunner),
                new RunXttsCudaSmokeUseCase(new ProcessXttsCudaRuntimeProbeGateway(processRunner)),
                new InspectAiEnginesPreflightUseCase(),
                new AppStartupEnginePreflightUseCase(),
                new BuildHumanEnginePreflightSummaryUseCase(),
                new InspectXttsSetupReadinessUseCase(),
                new InspectXttsSmokeTestUseCase(),
                new RunXttsReadinessSmokeUseCase(new InspectXttsSetupReadinessUseCase(), infrastructure.voiceTestSynthesisGateway()),
                new ConfirmXttsSmokePlaybackUseCase(new InspectXttsSmokeTestUseCase(), new JavaSoundSegmentAudioPlayer()),
                new PrepareXttsPortableRuntimeUseCase(new InspectXttsSetupReadinessUseCase(), processRunner),
                new DownloadXttsOfficialModelUseCase(),
                new ImportXttsModelFolderUseCase(),
                new SelectXttsAsEngineUseCase(),
                new InspectPiperSetupReadinessUseCase(),
                new DownloadPiperPortableRuntimeUseCase(),
                new SelectPiperAsEngineUseCase(),
                new ImportPiperVoiceFolderUseCase(),
                new ImportFfmpegRuntimeFolderUseCase(),
                ffmpegProbe,
                new InspectFinalVideoRuntimeStatusUseCase(resolvedApplicationRoot, ffmpegProbe),
                new DownloadFfmpegPortableRuntimeUseCase(ffmpegProbe),
                new InspectLocalTheatreImageSetupReadinessUseCase(),
                new PrepareLocalTheatreImageRuntimeUseCase(),
                new ImportLocalTheatreImageRuntimeUseCase(),
                new DownloadLocalTheatreImagePackageUseCase(),
                new ImportLocalTheatreImagePackageUseCase(),
                new FluxComponentImportUseCase(processRunner),
                new AuditEngineArtifactsUseCase(),
                new BuildGuiSmokeChecklistUseCase(
                        new InspectXttsSetupReadinessUseCase(),
                        new InspectPiperSetupReadinessUseCase(),
                        ffmpegProbe),
                new com.marcosmoreiradev.docupodcaststudio.application.document
                        .DownloadTesseractPortableRuntimeUseCase(),
                new com.marcosmoreiradev.docupodcaststudio.application.document
                        .ImportTesseractRuntimeFolderUseCase()
        );
        GrammarApplicationServices grammar = new GrammarApplicationServices(
                new BuildGrammarTemplateUseCase(),
                new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository())
        );
        BuildFragmentWorkspaceProjectionUseCase buildFragmentProjection = new BuildFragmentWorkspaceProjectionUseCase();
        FragmentApplicationServices fragment = new FragmentApplicationServices(
                buildFragmentProjection,
                new FragmentWorkspaceProjectionCoordinator(buildFragmentProjection)
        );
        DocumentStudyApplicationServices documentStudy = new DocumentStudyApplicationServices(
                new BuildDocumentStudyProjectionUseCase(),
                new BuildStudyProblemsProjectionUseCase(),
                new BuildDocumentStudyTextVideoPlanUseCase(),
                new com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoPlanUseCase(
                        new com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudyVideoContentResolver(),
                        new com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudySlideCompositor(),
                        materializePdfDocumentContentAsset,
                        materializeWordDocumentContentAsset),
                new com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoAudioOverlayPlanUseCase(),
                new PdfBoxStudyProblemPdfExporter(),
                new com.marcosmoreiradev.docupodcaststudio.application.documentstudy.PrepareDocumentIllustrationsUseCase(mediaCapabilities)
        );
        TheatreFragmentLinkPolicy theatreLinkPolicy = new TheatreFragmentLinkPolicy();
        BuildTheatreAudioTrackTimelineUseCase theatreAudioTimeline = new BuildTheatreAudioTrackTimelineUseCase();
        TheatreApplicationServices theatre = new TheatreApplicationServices(
                new BuildTheatreProductionProjectionUseCase(theatreLinkPolicy),
                theatreLinkPolicy,
                theatreAudioTimeline,
                new UpsertTheatreAudioTrackUseCase(theatreAudioTimeline),
                new RemoveTheatreAudioTrackUseCase(),
                new RenderTheatreChoralVoiceUseCase(
                        infrastructure.voiceTestSynthesisGateway(),
                        voiceCapabilityPolicy,
                        new ResolveVoiceToneReferenceUseCase(),
                        ffmpegLocator,
                        ffmpegProbe,
                        processRunner,
                        () -> loadOperationalSettingsSafely(infrastructure),
                        resolvedApplicationRoot,
                        new com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver(
                                installationRoot, resolvedApplicationRoot))
        );
        VisualAssetTracePolicy visualTracePolicy = new VisualAssetTracePolicy();
        VisualProductionApplicationServices visual = new VisualProductionApplicationServices(
                new BuildVisualProductionProjectionUseCase(
                        visualTracePolicy,
                        new TheatreGlobalVisualAssetProjectionProvider(visualTracePolicy)),
                new BuildVisualPromptContextUseCase(),
                visualTracePolicy);
        var theatreImportState = new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage
                .JsonTheatreImportStateRepository();
        var theatrePackages = new com.marcosmoreiradev.docupodcaststudio.application.services
                .TheatrePackageApplicationServices(
                new com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.RefreshTheatrePackageUseCase(
                        new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage
                                .JsonTheatrePackageScanner(),
                        theatreImportState,
                        new com.marcosmoreiradev.docupodcaststudio.application.theatrepackage
                                .ComputeTheatreRefreshPlanUseCase(),
                        new com.marcosmoreiradev.docupodcaststudio.application.theatrepackage
                                .PreflightTheatreRefreshUseCase(),
                        new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage
                                .FileSystemTheatreAssetStager(),
                        new com.marcosmoreiradev.docupodcaststudio.application.theatrepackage
                                .ReconcileTheatreProjectUseCase(),
                        infrastructure.projectRepository(),
                        project.inspectProjectIntegrity(),
                        new com.marcosmoreiradev.docupodcaststudio.infrastructure.json.AtomicJsonFileWriter()),
                theatreImportState);
        return new WorkspaceApplicationServices(
                new WorkspaceApplicationServices.ProjectWorkspace(project, assets, document, readingProfile, script,
                        grammar, fragment, documentStudy, theatre, theatrePackages),
                new WorkspaceApplicationServices.PlaybackWorkspace(audio, recording, playback),
                new WorkspaceApplicationServices.GenerationWorkspace(render, storyboard, media, visual),
                new WorkspaceApplicationServices.ExportWorkspace(export, process),
                new WorkspaceApplicationServices.AdministrationWorkspace(voice, settings, examples, guide, resources,
                        mediaCapabilities.platform(),
                        new com.marcosmoreiradev.docupodcaststudio.application.media.administration
                                .CapabilityAdministrationService(
                                mediaCapabilities.platform(),
                                mediaCapabilities.resourceScheduler())),
                new WorkspaceApplicationServices.RuntimeWorkspace(resolvedRoots));
    }

    private static OperationalSettings loadOperationalSettingsSafely(InfrastructureServices infrastructure) {
        try {
            return infrastructure.operationalSettingsRepository().load();
        } catch (Exception ex) {
            return OperationalSettings.defaults();
        }
    }
}
