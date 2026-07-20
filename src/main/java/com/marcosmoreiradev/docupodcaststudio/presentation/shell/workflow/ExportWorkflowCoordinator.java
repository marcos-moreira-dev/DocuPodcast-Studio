package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.errors.ApplicationPreconditionException;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessItem;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
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
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.Objects;

/**
 * Coordinates user-facing export flows for the shell.
 *
 * <p>RF1 starts extracting large workflow blocks from {@code DocuPodcastShellViewModel}
 * without changing the visible behavior. The view-model keeps UI state and delegates
 * export orchestration here.</p>
 */
public final class ExportWorkflowCoordinator {
    private final WorkspaceApplicationServices applicationServices;
    private final AudioWorkflowCoordinator audioWorkflow;

    public ExportWorkflowCoordinator(WorkspaceApplicationServices applicationServices, AudioWorkflowCoordinator audioWorkflow) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
        this.audioWorkflow = Objects.requireNonNull(audioWorkflow, "audioWorkflow");
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
                        manifest, projectDirectory, targetFile, Path.of("."), configuredFfmpeg(operational), playbackRate)
                : applicationServices.exports().export().exportPodcastAudio().exportLatest(
                        jobs, projectDirectory, targetFile, Path.of("."), configuredFfmpeg(operational));
        return result.humanSummary() + " · reporte " + result.reportFile().getFileName() + ".";
    }

    public String exportDiagnosticReport(ProjectSession session,
                                         NarrationScriptDocument script,
                                         StoryboardDocument storyboard,
                                         List<AudioJobSnapshot> jobs,
                                         Path targetFile) throws IOException {
        Path exported = applicationServices.exports().export().exportDiagnosticReport()
                .export(session.project(), script, storyboard, jobs, targetFile);
        return "Reporte diagnóstico exportado: " + exported + ".";
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
                operational.video().preferEmbeddedFfmpeg(),
                true,
                operational.compute().policy(),
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
                configuredFfmpeg(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportNarrativeVideo()
                .export(request, progress, cancellationRequested);
        return result.humanSummary() + ".";
    }

    private static SimpleVideoResolutionPreset narrativeResolution(ProjectSession session) {
        var configuration = session.project().narrative().videoConfiguration();
        return configuration.width() >= 1080 && configuration.height() >= 1920
                ? SimpleVideoResolutionPreset.FULL_HD_VERTICAL_1080X1920
                : SimpleVideoResolutionPreset.HD_VERTICAL_720X1280;
    }

    public String exportDocumentStudyTextAudioVideo(ProjectSession session,
                                                    Path projectFile,
                                                    com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument document,
                                                    NarrationScriptDocument script,
                                                    List<AudioJobSnapshot> jobs,
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
        saveProject.save(projectFile);
        ensureExportable(session, projectFile, script, null, jobs,
                ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO, "video documental texto+audio");
        OperationalSettings operational = loadOperationalSettings();
        SimpleVideoResolutionPreset preset = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                preset,
                framesPerSecond,
                0.35,
                operational.video().preferEmbeddedFfmpeg(),
                true,
                operational.compute().policy(),
                effectiveVideoEncoderPolicy(operational, requestedEncoder));
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        DocumentTextVideoOptions effectiveTextOptions = textVideoOptions == null
                ? DocumentTextVideoOptions.defaults().withResolution(preset)
                : textVideoOptions.withResolution(preset);
        if (document == null || document.format()
                != com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat.DOCX) {
            throw new IOException("Video documental por parrafos y tablas requiere un Word/DOCX abierto.");
        }
        SimpleVideoPlan plan = applicationServices.project().documentStudy().buildDocumentStudyVideoPlan()
                .build(session.project(), document, script, jobs, projectDirectory, effectiveTextOptions);
        com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan overlays =
                applicationServices.project().documentStudy().buildDocumentStudyVideoAudioOverlayPlan()
                        .build(session.project(), projectDirectory, plan);
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
                configuredFfmpeg(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportDocumentStudyVideo()
                .export(request, plan, overlays, progress, cancellationRequested);
        return result.humanSummary() + ".";
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
                operational.video().preferEmbeddedFfmpeg(),
                true,
                operational.compute().policy(),
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
                configuredFfmpeg(operational));
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
                operational.video().preferEmbeddedFfmpeg(),
                true,
                operational.compute().policy(),
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
                configuredFfmpeg(operational));
        FinalVideoExportResult result = applicationServices.exports().export().exportTheatreVideo()
                .exportSpatialMap(request, spatialFrameMode, scope, progress, cancellationRequested);
        return result.humanSummary() + ".";
    }


    private void ensureFinalAudioReady(List<AudioJobSnapshot> jobs,
                                       PlaybackManifest manifest,
                                       Path targetFile,
                                       OperationalSettings operational) {
        ExportReadinessItem item = applicationServices.exports().export().inspectFinalAudioExportReadiness()
                .inspect(jobs, manifest, targetFile, Path.of("."), configuredFfmpeg(operational));
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

    private static Path configuredFfmpeg(OperationalSettings operational) {
        String raw = operational == null ? "" : operational.video().ffmpegExecutable();
        return raw == null || raw.isBlank() ? null : Path.of(raw);
    }

    @FunctionalInterface
    public interface ProjectSaveCallback {
        void save(Path projectFile) throws IOException;
    }
}
