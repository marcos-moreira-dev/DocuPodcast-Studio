package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.errors.ApplicationPreconditionException;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessItem;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.export.PodcastFinalAudioExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.export.PodcastFinalWavExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPackageExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoExportSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.AudioWorkflowCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.Set;
import java.util.EnumSet;

/**
 * Coordinates user-facing export flows for the shell.
 *
 * <p>RF1 starts extracting large workflow blocks from {@code DocuPodcastShellViewModel}
 * without changing the visible behavior. The view-model keeps UI state and delegates
 * export orchestration here.</p>
 */
public final class ExportController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportController.class);
    private final WorkspaceApplicationServices applicationServices;
    private final AudioWorkflowCoordinator audioWorkflow;

    public ExportController(WorkspaceApplicationServices applicationServices, AudioWorkflowCoordinator audioWorkflow) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
        this.audioWorkflow = Objects.requireNonNull(audioWorkflow, "audioWorkflow");
    }

    /** Controller operations that have a concrete application-service executor. */
    public static Set<ExportControllerOperation> supportedOperations() {
        return Set.copyOf(EnumSet.allOf(ExportControllerOperation.class));
    }

    public String exportPodcastWav(Path projectDirectory, PlaybackManifest manifest, Path targetFile) throws IOException {
        return exportPodcastWav(projectDirectory, manifest, targetFile, 1.0);
    }

    public String exportPodcastWav(Path projectDirectory, PlaybackManifest manifest, Path targetFile, double playbackRate) throws IOException {
        List<AudioJobSnapshot> jobs = audioWorkflow.persistedJobs(projectDirectory);
        boolean unitLevelAudio = manifest != null && !manifest.emptyManifest()
                && manifest.cues().stream().anyMatch(cue -> !cue.unitId().equals(cue.segmentId()));
        OperationalSettings operational = loadOperationalSettings();
        ensureFinalAudioReady(jobs, manifest, targetFile, operational);
        PodcastFinalAudioExportResult result = unitLevelAudio
                ? applicationServices.exports().export().exportPodcastAudio().exportPlaybackManifest(
                        manifest, projectDirectory, targetFile, Path.of("."), configuredRendererExecutable(operational), playbackRate)
                : applicationServices.exports().export().exportPodcastAudio().exportLatest(
                        jobs, projectDirectory, targetFile, Path.of("."), configuredRendererExecutable(operational));
        return result.humanSummary() + " · reporte " + result.reportFile().getFileName() + ".";
    }

    public String exportDiagnosticReport(ProjectSession session,
                                         NarrationScriptDocument script,
                                         StoryboardDocument storyboard,
                                         List<AudioJobSnapshot> jobs,
                                         Path targetFile) throws IOException {
        Path projectRoot = session.projectFile()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .map(Path::getParent)
                .orElse(null);
        LinkedHashMap<String, String> diagnostics = new LinkedHashMap<>();
        diagnostics.put("application", "DocuPodcast Studio");
        diagnostics.put("projectId", session.project().metadata().id());
        diagnostics.put("projectMode", session.project().metadata().mode().name());
        diagnostics.put("audioJobs", Integer.toString(jobs == null ? 0 : jobs.size()));
        diagnostics.put("scriptLoaded", Boolean.toString(script != null));
        diagnostics.put("storyboardLoaded", Boolean.toString(storyboard != null));
        Path defaultLogs = Path.of(System.getProperty("user.home", "."), ".docupodcast-studio", "logs");
        Path logDirectory = Path.of(System.getProperty("docupodcast.log.dir", defaultLogs.toString()));
        var result = applicationServices.exports().export().exportSupportBundle().export(
                new com.marcosmoreiradev.docupodcaststudio.application.observability.SupportBundleExporter.ExportRequest(
                        targetFile, logDirectory, projectRoot, List.of(), diagnostics));
        return "Paquete de soporte sanitizado exportado: " + result.archive() + ".";
    }

    public String exportProjectBundle(ProjectSession session,
                                      Path projectFile,
                                      NarrationScriptDocument script,
                                      StoryboardDocument storyboard,
                                      List<AudioJobSnapshot> jobs,
                                      Path targetDirectory,
                                      ProjectSaveCallback saveProject) throws IOException {
        saveProject.save(projectFile);
        ProjectBundleExportRequest request = new ProjectBundleExportRequest(
                session.project(), projectFile, script, storyboard, jobs, targetDirectory);
        ProjectBundleExportResult result = applicationServices.exports().export().exportProjectBundle().export(request);
        return "Paquete exportado: " + result.rootDirectory()
                + " · inputs " + result.copiedInputs()
                + " · outputs " + result.copiedOutputs()
                + " · jobs " + result.copiedJobs()
                + " · assets " + result.copiedAssets()
                + " · muestras de voz " + result.voiceReferenceSampleCount()
                + " · manifiesto " + result.manifestFile().getFileName() + ".";
    }

    public String exportSimpleVideoPackage(ProjectSession session,
                                           Path projectFile,
                                           NarrationScriptDocument script,
                                           StoryboardDocument storyboard,
                                           List<AudioJobSnapshot> jobs,
                                           Path targetDirectory,
                                           double silentVisualBlockSeconds,
                                           ProjectSaveCallback saveProject) throws IOException {
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar video simple.");
        }
        saveProject.save(projectFile);
        ensureExportable(session, projectFile, script, storyboard, jobs, ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE, "paquete de video simple");
        SimpleVideoPackageExportResult result;
        try {
            var narrationPlan = applicationServices.generation().render().buildNarrationRenderPlan()
                    .build(script, session.project());
            var renderUnitPlan = applicationServices.generation().render().buildRenderUnitPlan()
                    .build(narrationPlan, silentVisualBlockSeconds);
            result = applicationServices.exports().export().exportSimpleVideoPackage()
                    .export(session.project(), renderUnitPlan, jobs, targetDirectory);
        } catch (RuntimeException ex) {
            result = applicationServices.exports().export().exportSimpleVideoPackage()
                    .export(session.project(), script, storyboard, jobs, targetDirectory);
        }
        return "Paquete de video simple preparado: " + result.rootDirectory()
                + " · frames " + result.frameCount()
                + " · duración estimada " + String.format(Locale.ROOT, "%.1f", result.totalDurationSeconds()) + " s"
                + " · estado " + result.honestStatusLabel()
                + " · salida esperada " + result.outputFileName()
                + " · plan " + result.planMarkdownFile().getFileName() + ".";
    }

    public String exportFinalVideo(ProjectSession session,
                                   Path projectFile,
                                   NarrationScriptDocument script,
                                   StoryboardDocument storyboard,
                                   List<AudioJobSnapshot> jobs,
                                   Path targetFile,
                                   SimpleVideoResolutionPreset resolution,
                                   double silentVisualBlockSeconds,
                                   ProjectSaveCallback saveProject) throws IOException {
        return exportFinalVideo(session, projectFile, script, storyboard, jobs, targetFile, resolution,
                30, VideoEncoderPolicy.AUTO, silentVisualBlockSeconds, saveProject, ignored -> { }, () -> false);
    }

    public String exportFinalVideo(ProjectSession session,
                                   Path projectFile,
                                   NarrationScriptDocument script,
                                   StoryboardDocument storyboard,
                                   List<AudioJobSnapshot> jobs,
                                   Path targetFile,
                                   SimpleVideoResolutionPreset resolution,
                                   int framesPerSecond,
                                   VideoEncoderPolicy requestedEncoder,
                                   double silentVisualBlockSeconds,
                                   ProjectSaveCallback saveProject,
                                   Consumer<VideoRenderProgress> progress,
                                   BooleanSupplier cancellationRequested) throws IOException {
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar video.");
        }
        saveProject.save(projectFile);
        boolean narrativeVideo = session.project().metadata().mode() == ProjectMode.NARRATIVE_VIDEO;
        if (!narrativeVideo) {
            ensureFinalVideoReady(session, projectFile, script, storyboard, jobs);
        }
        OperationalSettings operational = loadOperationalSettings();
        SimpleVideoResolutionPreset preset = narrativeVideo
                ? narrativeResolution(session)
                : resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        int effectiveFramesPerSecond = narrativeVideo
                ? session.project().narrative().videoConfiguration().framesPerSecond()
                : framesPerSecond;
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                preset,
                effectiveFramesPerSecond,
                silentVisualBlockSeconds,
                operational.video().preferBundledRenderer(),
                true,
                operational.compute().policy(),
                operational.compute().selectedDeviceId(),
                effectiveVideoEncoderPolicy(operational, requestedEncoder));
        var renderUnitPlan = narrativeVideo ? null : applicationServices.generation().render().buildRenderUnitPlan().build(
                applicationServices.generation().render().buildNarrationRenderPlan().build(script, session.project()),
                silentVisualBlockSeconds);
        FinalVideoExportRequest request = new FinalVideoExportRequest(
                session.project(),
                renderUnitPlan,
                script,
                storyboard,
                jobs,
                projectFile.toAbsolutePath().normalize().getParent(),
                targetFile,
                settings,
                Path.of("."),
                configuredRendererExecutable(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportNarrativeVideo()
                .export(request, progress, cancellationRequested);
        return result.humanSummary() + ".";
    }

    private static SimpleVideoResolutionPreset narrativeResolution(ProjectSession session) {
        var configuration = session.project().narrative().videoConfiguration();
        return SimpleVideoResolutionPreset.fromDimensions(configuration.width(), configuration.height());
    }

    public record DocumentaryVideoOutcome(String summary, int missingIllustrations) { }

    public DocumentaryVideoOutcome exportDocumentStudyTextAudioVideo(ProjectSession session,
                                                    Path projectFile,
                                                    com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection document,
                                                    NarrationScriptDocument script,
                                                    com.marcosmoreiradev.docupodcaststudio.application.audio.AudioCoverageSnapshotAssembler.Result audioResolution,
                                                    Path targetFile,
                                                    SimpleVideoResolutionPreset resolution,
                                                    int framesPerSecond,
                                                    VideoEncoderPolicy requestedEncoder,
                                                    DocumentTextVideoOptions textVideoOptions,
                                                    ProjectSaveCallback saveProject,
                                                    Consumer<VideoRenderProgress> progress,
                                                    BooleanSupplier cancellationRequested) throws IOException {
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar video documental texto+audio.");
        }
        if (audioResolution == null || !audioResolution.readyForPreflight()) {
            throw new IOException("La exportacion documental requiere una resolucion de audio reconciliada.");
        }
        List<AudioJobSnapshot> jobs = audioResolution.exportJobs();
        saveProject.save(projectFile);
        ensureExportable(session, projectFile, script, null, jobs,
                ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO, "video documental texto+audio");
        OperationalSettings operational = loadOperationalSettings();
        SimpleVideoResolutionPreset preset = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                preset,
                framesPerSecond,
                0.35,
                operational.video().preferBundledRenderer(),
                true,
                operational.compute().policy(),
                operational.compute().selectedDeviceId(),
                effectiveVideoEncoderPolicy(operational, requestedEncoder));
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        DocumentTextVideoOptions effectiveTextOptions = textVideoOptions == null
                ? DocumentTextVideoOptions.defaults().withResolution(preset)
                : textVideoOptions.withResolution(preset);
        if (document == null) {
            throw new IOException("Video documental requiere una fuente Word o PDF preparada.");
        }
        for (var resolved : jobs.getFirst().segments()) {
            Path absolute = projectDirectory.resolve(resolved.audioRelativePath())
                    .toAbsolutePath().normalize();
            boolean exists = absolute.startsWith(projectDirectory)
                    && java.nio.file.Files.isRegularFile(absolute);
            long size = exists ? java.nio.file.Files.size(absolute) : 0L;
            LOGGER.debug("document-export.audio-boundary correlationId={} stage=PLAN_INPUT "
                            + "segmentId={} fingerprint={} expectedVoiceHash={} resolvedVoiceHash={} "
                            + "relativePath={} absolutePath={} exists={} fileSize={} fileSha256={} "
                            + "durationSeconds={} resolutionSource={} mismatchReason={}",
                    audioResolution.correlationId(), resolved.segmentId(), resolved.sourceFingerprint(),
                    resolved.sourceFingerprint().voiceConfigurationSha256(),
                    resolved.sourceFingerprint().voiceConfigurationSha256(),
                    resolved.audioRelativePath(), absolute, exists, size,
                    exists ? fileSha256(absolute) : "", resolved.durationSeconds(),
                    "RECONCILED_SEGMENT_COMPOSITION",
                    exists ? "" : "AUTHORIZED_AUDIO_FILE_MISSING");
        }
        var illustrationPreparation = applicationServices.project().documentStudy().prepareIllustrations();
        var illustrationResult = illustrationPreparation == null
                ? new com.marcosmoreiradev.docupodcaststudio.application.documentstudy.PrepareDocumentIllustrationsUseCase.Result(java.util.Map.of(), java.util.List.of())
                : illustrationPreparation.prepareWithProgress(session.project(), document, script, projectDirectory, operational,
                        () -> cancellationRequested != null && cancellationRequested.getAsBoolean(),
                        event -> { if (progress != null) progress.accept(new VideoRenderProgress(
                                switch (event.phase()) {
                                    case PROMPT -> com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderStage.PLANNING_ILLUSTRATION;
                                    case GENERATION -> com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderStage.GENERATING_ILLUSTRATION;
                                    case REVIEW -> com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderStage.REVIEWING_ILLUSTRATION;
                                    default -> com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderStage.PREPARING;
                                }, 0, 0, event.detail(), true, true)); });
        SimpleVideoPlan plan = applicationServices.project().documentStudy().buildDocumentStudyVideoPlan()
                .build(session.project(), document, script, audioResolution, projectDirectory, effectiveTextOptions,
                        illustrationResult.images());
        String correlationId = audioResolution.correlationId();
        LinkedHashSet<String> spokenVideoSegmentIds = plan.frames().stream()
                .filter(frame -> !frame.silentVisual()).map(SimpleVideoFrame::segmentId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> selectedEffectiveSegmentIds = script.segments().stream()
                .filter(NarrationSegment::narratable).map(NarrationSegment::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> spokenOutsideSelection = plan.frames().stream()
                .filter(frame -> !frame.silentVisual())
                .map(SimpleVideoFrame::segmentId)
                .filter(frameSegmentId -> !belongsToNarrationSelection(
                        frameSegmentId, selectedEffectiveSegmentIds))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!spokenOutsideSelection.isEmpty()) {
            throw new IOException("El plan de video contiene segmentos hablados fuera del "
                    + "alcance documental resuelto: " + spokenOutsideSelection);
        }
        LinkedHashSet<String> audioCoveredSegmentIds = jobs == null
                ? new LinkedHashSet<>() : jobs.stream()
                .flatMap(job -> job.segments().stream())
                .filter(audio -> audio.completed() && !audio.audioRelativePath().isBlank())
                .map(audio -> audio.segmentId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> intersection = new LinkedHashSet<>(spokenVideoSegmentIds);
        intersection.retainAll(audioCoveredSegmentIds);
        LinkedHashSet<String> missingFromAudio = new LinkedHashSet<>(spokenVideoSegmentIds);
        missingFromAudio.removeAll(audioCoveredSegmentIds);
        LinkedHashSet<String> extraAudioNotInVideo = new LinkedHashSet<>(audioCoveredSegmentIds);
        extraAudioNotInVideo.removeAll(spokenVideoSegmentIds);
        LOGGER.info("document-export.audio-matrix correlationId={} spokenVideoIds={} "
                        + "audioCoveredIds={} intersection={}/{} missingFromAudio={} "
                        + "extraAudioNotInVideo={}", correlationId, spokenVideoSegmentIds,
                audioCoveredSegmentIds, intersection.size(), spokenVideoSegmentIds.size(),
                missingFromAudio, extraAudioNotInVideo);
        for (SimpleVideoFrame frame : plan.frames()) {
            com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot resolved =
                    jobs.stream().flatMap(job -> job.segments().stream())
                            .filter(audio -> audio.segmentId().equals(frame.segmentId()))
                            .findFirst().orElse(null);
            String expected = resolved == null ? "" : resolved.sourceFingerprint().toString();
            boolean audioExists = !frame.audioRelativePath().isBlank()
                    && java.nio.file.Files.isRegularFile(projectDirectory
                    .resolve(frame.audioRelativePath()).normalize());
            LOGGER.info("document-export.video-plan correlationId={} stage=PLAN_RESOLUTION frameId={} "
                            + "segmentId={} spoken={} audioRequired={} expectedFingerprint={} "
                            + "resolvedFingerprint={} expectedVoiceHash={} resolvedVoiceHash={} "
                            + "resolvedAudioPath={} audioExists={} resolutionSource={} mismatchReason={}",
                    correlationId, frame.id(), frame.segmentId(), !frame.silentVisual(),
                    !frame.silentVisual(), expected, expected,
                    resolved == null ? "" : resolved.sourceFingerprint().voiceConfigurationSha256(),
                    resolved == null ? "" : resolved.sourceFingerprint().voiceConfigurationSha256(),
                    frame.audioRelativePath(), audioExists,
                    frame.audioRelativePath().contains("segment-audio-cache")
                            ? "RECONCILED_SEGMENT_COMPOSITION" : "VISUAL_OR_MISSING",
                    !frame.silentVisual() && !audioExists ? "AUTHORIZED_AUDIO_FILE_MISSING" : "");
            if (frame.visualBinding() != null) {
                var binding = frame.visualBinding();
                LOGGER.info("document-export.visual-binding correlationId={} frameId={} segmentId={} "
                                + "sourceBlockId={} regionId={} page={} readingOrder={} mode={} "
                                + "visualSource={} classification={} cropBBox={}",
                        correlationId, binding.frameId(), binding.segmentId(),
                        binding.sourceBlockId(), binding.regionId(), binding.pageNumber(),
                        binding.readingOrder(), binding.presentationMode(), binding.visualSource(),
                        binding.quality(), binding.cropBBox());
            }
        }
        LOGGER.info("document-scope.boundary correlationId={} stage=VIDEO_PLAN count={} ids={} "
                        + "selectedEffectiveIds={} outsideSelection={}",
                correlationId,
                plan.frames().stream().filter(frame -> !frame.silentVisual()).count(),
                plan.frames().stream().filter(frame -> !frame.silentVisual())
                        .map(SimpleVideoFrame::segmentId).distinct().toList(),
                selectedEffectiveSegmentIds, spokenOutsideSelection);
        var preflight = new com.marcosmoreiradev.docupodcaststudio.application.documentstudy
                .DocumentStudyVideoExportPreflight().inspect(plan, projectDirectory, script);
        LOGGER.info("document-scope.boundary correlationId={} stage=PREFLIGHT spoken={} missing={} ids={}",
                correlationId,
                plan.frames().stream().filter(frame -> !frame.silentVisual()).count(),
                preflight.spokenFramesMissingAudio(),
                preflight.missingFrames().stream().map(frame -> frame.narrationSegmentId())
                        .distinct().toList());
        if (!preflight.ready()) {
            String examples = preflight.missingFrames().stream().limit(5)
                    .map(frame -> frame.frameId() + "/" + frame.narrationSegmentId())
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new IOException("Preflight de video documental incompleto antes del render: frames sin imagen="
                    + preflight.framesMissingImage() + "; frames hablados sin audio="
                    + preflight.spokenFramesMissingAudio() + "; bindings visuales invalidos="
                    + preflight.invalidVisualBindings().size() + ". Faltantes: " + examples
                    + ". Genera o actualiza el audio de lectura y vuelve a exportar.");
        }
        com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan overlays =
                applicationServices.project().documentStudy().buildDocumentStudyVideoAudioOverlayPlan()
                        .build(session.project(), projectDirectory, plan);
        LOGGER.info("document-export.audio-boundary correlationId={} stage=RENDER_OVERLAYS count={} ids={}",
                correlationId,
                overlays.inputs().size(), overlays.inputs().stream()
                        .map(com.marcosmoreiradev.docupodcaststudio.application.video
                                .VideoAudioOverlayPlan.Input::overlayId).toList());
        FinalVideoExportRequest request = new FinalVideoExportRequest(
                session.project(),
                null,
                script,
                null,
                jobs,
                projectDirectory,
                targetFile,
                settings,
                Path.of("."),
                configuredRendererExecutable(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportDocumentStudyVideo()
                .export(request, plan, overlays, progress, cancellationRequested);
        return new DocumentaryVideoOutcome(result.humanSummary() + ". " + illustrationResult.summary()
                + (illustrationResult.warnings().isEmpty() ? ""
                : " Avisos de ilustraciones: " + String.join("; ", illustrationResult.warnings())
                + ". Detalles en media/images/document-illustrations/last-report.txt."), illustrationResult.missingCount());
    }

    public String exportTheatreWork(ProjectSession session,
                                    Path projectFile,
                                    NarrationScriptDocument script,
                                    List<AudioJobSnapshot> jobs,
                                    Path targetFile,
                                    SimpleVideoResolutionPreset resolution,
                                    int framesPerSecond,
                                    VideoEncoderPolicy requestedEncoder,
                                    double silentVisualBlockSeconds,
                                    StoryboardDocument storyboard,
                                    boolean renderUnassignedVisuals,
                                    ProjectSaveCallback saveProject,
                                    Consumer<VideoRenderProgress> progress,
                                    BooleanSupplier cancellationRequested) throws IOException {
        return exportTheatreWork(session, projectFile, script, jobs, targetFile, resolution, framesPerSecond,
                requestedEncoder, silentVisualBlockSeconds, storyboard, renderUnassignedVisuals, false,
                saveProject, progress, cancellationRequested);
    }

    public String exportTheatreWork(ProjectSession session,
                                    Path projectFile,
                                    NarrationScriptDocument script,
                                    List<AudioJobSnapshot> jobs,
                                    Path targetFile,
                                    SimpleVideoResolutionPreset resolution,
                                    int framesPerSecond,
                                    VideoEncoderPolicy requestedEncoder,
                                    double silentVisualBlockSeconds,
                                    StoryboardDocument storyboard,
                                    boolean renderUnassignedVisuals,
                                    boolean includeInferredFrames,
                                    ProjectSaveCallback saveProject,
                                    Consumer<VideoRenderProgress> progress,
                                    BooleanSupplier cancellationRequested) throws IOException {
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar obra teatral.");
        }
        saveProject.save(projectFile);
        ensureExportable(session, projectFile, script, null, jobs,
                ExportableArtifactKind.THEATRE_WORK_VIDEO, "obra teatral");
        OperationalSettings operational = loadOperationalSettings();
        SimpleVideoResolutionPreset preset = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                preset,
                framesPerSecond,
                silentVisualBlockSeconds,
                operational.video().preferBundledRenderer(),
                true,
                operational.compute().policy(),
                operational.compute().selectedDeviceId(),
                effectiveVideoEncoderPolicy(operational, requestedEncoder))
                .withRenderUnassignedVisuals(renderUnassignedVisuals)
                .withIncludeInferredFrames(includeInferredFrames);
        FinalVideoExportRequest request = new FinalVideoExportRequest(
                session.project(),
                null,
                script,
                storyboard,
                jobs,
                projectFile.toAbsolutePath().normalize().getParent(),
                targetFile,
                settings,
                Path.of("."),
                configuredRendererExecutable(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportTheatreVideo()
                .exportWork(request, progress, cancellationRequested);
        return result.humanSummary() + ".";
    }

    public String exportTheatreSpatialVideo(ProjectSession session,
                                            Path projectFile,
                                            NarrationScriptDocument script,
                                            List<AudioJobSnapshot> jobs,
                                            Path targetFile,
                                            SimpleVideoResolutionPreset resolution,
                                            int framesPerSecond,
                                            VideoEncoderPolicy requestedEncoder,
                                            double silentVisualBlockSeconds,
                                            String spatialFrameMode,
                                            ProjectSaveCallback saveProject,
                                            Consumer<VideoRenderProgress> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        return exportTheatreSpatialVideo(session, projectFile, script, jobs, targetFile, resolution, framesPerSecond,
                requestedEncoder, silentVisualBlockSeconds, spatialFrameMode, TheatreExportScope.all(),
                saveProject, progress, cancellationRequested);
    }

    public String exportTheatreSpatialVideo(ProjectSession session,
                                            Path projectFile,
                                            NarrationScriptDocument script,
                                            List<AudioJobSnapshot> jobs,
                                            Path targetFile,
                                            SimpleVideoResolutionPreset resolution,
                                            int framesPerSecond,
                                            VideoEncoderPolicy requestedEncoder,
                                            double silentVisualBlockSeconds,
                                            StoryboardDocument storyboard,
                                            String spatialFrameMode,
                                            ProjectSaveCallback saveProject,
                                            Consumer<VideoRenderProgress> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        return exportTheatreSpatialVideo(session, projectFile, script, jobs, targetFile, resolution, framesPerSecond,
                requestedEncoder, silentVisualBlockSeconds, storyboard, spatialFrameMode, TheatreExportScope.all(),
                saveProject, progress, cancellationRequested);
    }

    public String exportTheatreSpatialVideo(ProjectSession session,
                                            Path projectFile,
                                            NarrationScriptDocument script,
                                            List<AudioJobSnapshot> jobs,
                                            Path targetFile,
                                            SimpleVideoResolutionPreset resolution,
                                            int framesPerSecond,
                                            VideoEncoderPolicy requestedEncoder,
                                            double silentVisualBlockSeconds,
                                            String spatialFrameMode,
                                            TheatreExportScope scope,
                                            ProjectSaveCallback saveProject,
                                            Consumer<VideoRenderProgress> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        return exportTheatreSpatialVideo(session, projectFile, script, jobs, targetFile, resolution,
                framesPerSecond, requestedEncoder, silentVisualBlockSeconds, spatialFrameMode, scope, false,
                saveProject, progress, cancellationRequested);
    }

    public String exportTheatreSpatialVideo(ProjectSession session,
                                            Path projectFile,
                                            NarrationScriptDocument script,
                                            List<AudioJobSnapshot> jobs,
                                            Path targetFile,
                                            SimpleVideoResolutionPreset resolution,
                                            int framesPerSecond,
                                            VideoEncoderPolicy requestedEncoder,
                                            double silentVisualBlockSeconds,
                                            StoryboardDocument storyboard,
                                            String spatialFrameMode,
                                            TheatreExportScope scope,
                                            ProjectSaveCallback saveProject,
                                            Consumer<VideoRenderProgress> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        return exportTheatreSpatialVideo(session, projectFile, script, jobs, targetFile, resolution,
                framesPerSecond, requestedEncoder, silentVisualBlockSeconds, storyboard, spatialFrameMode, scope, false,
                saveProject, progress, cancellationRequested);
    }

    public String exportTheatreSpatialVideo(ProjectSession session,
                                            Path projectFile,
                                            NarrationScriptDocument script,
                                            List<AudioJobSnapshot> jobs,
                                            Path targetFile,
                                            SimpleVideoResolutionPreset resolution,
                                            int framesPerSecond,
                                            VideoEncoderPolicy requestedEncoder,
                                            double silentVisualBlockSeconds,
                                            String spatialFrameMode,
                                            TheatreExportScope scope,
                                            boolean includeInferredFrames,
                                            ProjectSaveCallback saveProject,
                                            Consumer<VideoRenderProgress> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        return exportTheatreSpatialVideo(session, projectFile, script, jobs, targetFile, resolution, framesPerSecond,
                requestedEncoder, silentVisualBlockSeconds, null, spatialFrameMode, scope, includeInferredFrames,
                saveProject, progress, cancellationRequested);
    }

    public String exportTheatreSpatialVideo(ProjectSession session,
                                            Path projectFile,
                                            NarrationScriptDocument script,
                                            List<AudioJobSnapshot> jobs,
                                            Path targetFile,
                                            SimpleVideoResolutionPreset resolution,
                                            int framesPerSecond,
                                            VideoEncoderPolicy requestedEncoder,
                                            double silentVisualBlockSeconds,
                                            StoryboardDocument storyboard,
                                            String spatialFrameMode,
                                            TheatreExportScope scope,
                                            boolean includeInferredFrames,
                                            ProjectSaveCallback saveProject,
                                            Consumer<VideoRenderProgress> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar video mapa.");
        }
        saveProject.save(projectFile);
        ExportableArtifactKind readinessKind = scope == null || scope.isAll()
                ? ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO
                : ExportableArtifactKind.THEATRE_PORTION_VIDEO;
        ensureExportable(session, projectFile, script, null, jobs, readinessKind,
                readinessKind == ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO
                        ? "video de mapa teatral"
                        : "porcion teatral");
        OperationalSettings operational = loadOperationalSettings();
        SimpleVideoResolutionPreset preset = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                preset,
                framesPerSecond,
                silentVisualBlockSeconds,
                operational.video().preferBundledRenderer(),
                true,
                operational.compute().policy(),
                operational.compute().selectedDeviceId(),
                effectiveVideoEncoderPolicy(operational, requestedEncoder))
                .withIncludeInferredFrames(includeInferredFrames);
        FinalVideoExportRequest request = new FinalVideoExportRequest(
                session.project(),
                null,
                script,
                storyboard,
                jobs,
                projectFile.toAbsolutePath().normalize().getParent(),
                targetFile,
                settings,
                Path.of("."),
                configuredRendererExecutable(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportTheatreVideo()
                .exportSpatialMap(request, spatialFrameMode, scope, progress, cancellationRequested);
        return result.humanSummary() + ".";
    }


    private void ensureFinalAudioReady(List<AudioJobSnapshot> jobs,
                                       PlaybackManifest manifest,
                                       Path targetFile,
                                       OperationalSettings operational) {
        ExportReadinessItem item = applicationServices.exports().export().inspectFinalAudioExportReadiness()
                .inspect(jobs, manifest, targetFile, Path.of("."), configuredRendererExecutable(operational));
        if (item.blocked()) {
            throw new ApplicationPreconditionException(
                    "No se puede exportar audio final",
                    String.join(" ", item.missingRequirements()),
                    "Formato: " + item.format().displayName() + System.lineSeparator()
                            + "Destino sugerido: " + item.targetHint());
        }
    }

    private void ensureExportable(ProjectSession session,
                                  Path projectFile,
                                  NarrationScriptDocument script,
                                  StoryboardDocument storyboard,
                                  List<AudioJobSnapshot> jobs,
                                  ExportableArtifactKind kind,
                                  String label) {
        var report = applicationServices.exports().export().inspectExportReadiness()
                .inspect(session.project(), projectFile, script, storyboard, jobs);
        ExportReadinessItem item = report.items().stream()
                .filter(candidate -> candidate.kind() == kind)
                .findFirst()
                .orElseThrow(() -> new ApplicationPreconditionException(
                        "No se pudo revisar exportación",
                        "No se encontró una regla de readiness para " + label + "."));
        if (item.blocked()) {
            throw new ApplicationPreconditionException(
                    "No se puede exportar " + label,
                    String.join(" ", item.missingRequirements()),
                    "Salida: " + kind.displayName());
        }
    }

    private void ensureFinalVideoReady(ProjectSession session,
                                       Path projectFile,
                                       NarrationScriptDocument script,
                                       StoryboardDocument storyboard,
                                       List<AudioJobSnapshot> jobs) throws IOException {
        var report = applicationServices.exports().export().inspectExportReadiness()
                .inspect(session.project(), projectFile, script, storyboard, jobs);
        ExportReadinessItem finalVideo = report.items().stream()
                .filter(item -> item.kind() == ExportableArtifactKind.FINAL_VIDEO_MP4)
                .findFirst()
                .orElseThrow(() -> new IOException("No se pudo revisar la preparación del MP4 final."));
        if (finalVideo.blocked()) {
            throw new IOException("No se puede exportar MP4 final. " + String.join(" ", finalVideo.missingRequirements()));
        }
    }

    private OperationalSettings loadOperationalSettings() {
        try {
            return applicationServices.administration().settings().loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }

    static boolean belongsToNarrationSelection(String frameSegmentId,
                                               java.util.Set<String> selectedNarrationSegmentIds) {
        if (frameSegmentId == null || selectedNarrationSegmentIds == null) return false;
        String candidate = frameSegmentId.strip();
        if (selectedNarrationSegmentIds.contains(candidate)) return true;
        int unitMarker = candidate.lastIndexOf("-U");
        if (unitMarker <= 0 || unitMarker + 2 >= candidate.length()) return false;
        for (int index = unitMarker + 2; index < candidate.length(); index++) {
            if (!Character.isDigit(candidate.charAt(index))) return false;
        }
        return selectedNarrationSegmentIds.contains(candidate.substring(0, unitMarker));
    }

    private static VideoEncoderPolicy effectiveVideoEncoderPolicy(OperationalSettings settings, VideoEncoderPolicy requested) {
        VideoEncoderPolicy selected = requested == null ? VideoEncoderPolicy.AUTO : requested;
        if (selected != VideoEncoderPolicy.AUTO) {
            return selected;
        }
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        VideoEncoderPolicy configured = current.compute().videoEncoderPolicy();
        if (configured != VideoEncoderPolicy.AUTO) {
            return configured;
        }
        if (!current.compute().allowGpuForVideo()) {
            return VideoEncoderPolicy.CPU_X264;
        }
        String device = current.compute().selectedDeviceId().toLowerCase(Locale.ROOT);
        if (device.startsWith("gpu-nvidia")) {
            return VideoEncoderPolicy.NVIDIA_NVENC;
        }
        if (device.startsWith("gpu-intel")) {
            return VideoEncoderPolicy.INTEL_QSV;
        }
        if (device.startsWith("gpu-amd")) {
            return VideoEncoderPolicy.AMD_AMF;
        }
        return VideoEncoderPolicy.CPU_X264;
    }

    private static Path configuredRendererExecutable(OperationalSettings operational) {
        String raw = operational == null ? "" : operational.video().configuredRendererExecutable();
        return raw == null || raw.isBlank() ? null : Path.of(raw);
    }

    private static String fileSha256(Path file) throws IOException {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            try (var input = java.nio.file.Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @FunctionalInterface
    public interface ProjectSaveCallback {
        void save(Path projectFile) throws IOException;
    }
}
