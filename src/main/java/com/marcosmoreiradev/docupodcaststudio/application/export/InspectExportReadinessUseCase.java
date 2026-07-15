package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.application.fragment.BuildFragmentWorkspaceProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.BuildNarrativeVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.BuildNarrativeVideoWorkspaceProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVideoWorkspaceProjection;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.render.BuildNarrationRenderPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.render.BuildRenderUnitPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreProductionProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreProductionProjection;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreProductionReadiness;
import com.marcosmoreiradev.docupodcaststudio.application.video.BuildSimpleVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inspects which export outputs are really available from the loaded brain state.
 *
 * <p>T78 centralizes export honesty: the UI may ask this use case later, but the decision lives
 * in application code and can be used by package export, diagnostics and smoke automation.</p>
 */
public final class InspectExportReadinessUseCase {
    private final ProjectExportFormatPolicy exportFormatPolicy;
    private final ExportPodcastWavUseCase podcastWav;
    private final BuildNarrationRenderPlanUseCase buildNarrationRenderPlan;
    private final BuildRenderUnitPlanUseCase buildRenderUnitPlan;
    private final BuildSimpleVideoPlanUseCase buildSimpleVideoPlan;
    private final ProjectModePolicy projectModePolicy;
    private final BuildFragmentWorkspaceProjectionUseCase buildFragmentWorkspaceProjection;
    private final BuildNarrativeVideoWorkspaceProjectionUseCase buildNarrativeVideoWorkspaceProjection;
    private final BuildNarrativeVideoPlanUseCase buildNarrativeVideoPlan;
    private final BuildTheatreProductionProjectionUseCase buildTheatreProductionProjection;

    public InspectExportReadinessUseCase() {
        this(new ProjectExportFormatPolicy(), new ExportPodcastWavUseCase(),
                new BuildNarrationRenderPlanUseCase(), new BuildRenderUnitPlanUseCase(), new BuildSimpleVideoPlanUseCase(),
                new ProjectModePolicy(), new BuildFragmentWorkspaceProjectionUseCase(),
                new BuildNarrativeVideoWorkspaceProjectionUseCase(), new BuildNarrativeVideoPlanUseCase(),
                new BuildTheatreProductionProjectionUseCase());
    }

    public InspectExportReadinessUseCase(ProjectExportFormatPolicy exportFormatPolicy, ExportPodcastWavUseCase podcastWav) {
        this(exportFormatPolicy, podcastWav,
                new BuildNarrationRenderPlanUseCase(), new BuildRenderUnitPlanUseCase(), new BuildSimpleVideoPlanUseCase(),
                new ProjectModePolicy(), new BuildFragmentWorkspaceProjectionUseCase(),
                new BuildNarrativeVideoWorkspaceProjectionUseCase(), new BuildNarrativeVideoPlanUseCase(),
                new BuildTheatreProductionProjectionUseCase());
    }

    public InspectExportReadinessUseCase(ProjectExportFormatPolicy exportFormatPolicy,
                                         ExportPodcastWavUseCase podcastWav,
                                         BuildNarrationRenderPlanUseCase buildNarrationRenderPlan,
                                         BuildRenderUnitPlanUseCase buildRenderUnitPlan,
                                         BuildSimpleVideoPlanUseCase buildSimpleVideoPlan,
                                         ProjectModePolicy projectModePolicy,
                                         BuildFragmentWorkspaceProjectionUseCase buildFragmentWorkspaceProjection,
                                         BuildNarrativeVideoWorkspaceProjectionUseCase buildNarrativeVideoWorkspaceProjection,
                                         BuildNarrativeVideoPlanUseCase buildNarrativeVideoPlan,
                                         BuildTheatreProductionProjectionUseCase buildTheatreProductionProjection) {
        this.exportFormatPolicy = Objects.requireNonNull(exportFormatPolicy, "exportFormatPolicy");
        this.podcastWav = Objects.requireNonNull(podcastWav, "podcastWav");
        this.buildNarrationRenderPlan = Objects.requireNonNull(buildNarrationRenderPlan, "buildNarrationRenderPlan");
        this.buildRenderUnitPlan = Objects.requireNonNull(buildRenderUnitPlan, "buildRenderUnitPlan");
        this.buildSimpleVideoPlan = Objects.requireNonNull(buildSimpleVideoPlan, "buildSimpleVideoPlan");
        this.projectModePolicy = Objects.requireNonNull(projectModePolicy, "projectModePolicy");
        this.buildFragmentWorkspaceProjection = Objects.requireNonNull(buildFragmentWorkspaceProjection, "buildFragmentWorkspaceProjection");
        this.buildNarrativeVideoWorkspaceProjection = Objects.requireNonNull(buildNarrativeVideoWorkspaceProjection, "buildNarrativeVideoWorkspaceProjection");
        this.buildNarrativeVideoPlan = Objects.requireNonNull(buildNarrativeVideoPlan, "buildNarrativeVideoPlan");
        this.buildTheatreProductionProjection = Objects.requireNonNull(buildTheatreProductionProjection, "buildTheatreProductionProjection");
    }

    public ExportReadinessReport inspect(
            DocuPodcastProject project,
            Path projectFile,
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> audioJobs
    ) {
        Objects.requireNonNull(project, "project");
        List<AudioJobSnapshot> safeJobs = audioJobs == null ? List.of() : List.copyOf(audioJobs);
        ArrayList<ExportReadinessItem> items = new ArrayList<>();
        items.add(projectBundle(projectFile));
        items.add(diagnosticReport(project));
        items.add(podcastWav(projectFile, script, safeJobs));
        if (projectModePolicy.resolve(project) == ProjectMode.DOCUMENTARY_STUDIO) {
            items.add(documentTextAudioVideo(projectFile, script, safeJobs));
        }
        if (projectModePolicy.resolve(project) == ProjectMode.THEATRE_PRODUCTION) {
            TheatreProductionProjection theatre = theatreProjection(project, script, storyboard, safeJobs);
            items.add(theatreWork(projectFile, theatre));
            items.add(theatreSpatialMap(projectFile, theatre));
            items.add(theatrePortion(projectFile, theatre));
        }
        items.add(finalVideoMp4(project, projectFile, script, storyboard, safeJobs));
        items.add(storyboardSummary(script, storyboard));
        items.add(simpleVideoPackage(script, storyboard, safeJobs));
        return ExportReadinessReport.from(project.metadata().title(), items);
    }

    private ExportReadinessItem projectBundle(Path projectFile) {
        if (exportFormatPolicy.canExportProjectBundle(projectFile != null)) {
            return ExportReadinessItem.exportable(
                    ExportableArtifactKind.PROJECT_BUNDLE,
                    DocuPodcastExportFormat.DIRECTORY,
                    "carpeta-exportacion/",
                    List.of("El proyecto tiene archivo .docupodcast guardado."),
                    List.of("El paquete no instala motores ni modelos; solo empaqueta entrada, editable, salidas, assets, jobs y reportes."));
        }
        return ExportReadinessItem.blocked(
                ExportableArtifactKind.PROJECT_BUNDLE,
                DocuPodcastExportFormat.DIRECTORY,
                "carpeta-exportacion/",
                List.of("Guarda el proyecto antes de exportar un paquete portable."),
                List.of("Sin archivo de proyecto no se puede resolver carpeta raíz, assets ni jobs persistidos."));
    }

    private static ExportReadinessItem diagnosticReport(DocuPodcastProject project) {
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.DIAGNOSTIC_REPORT,
                DocuPodcastExportFormat.MARKDOWN,
                "reporte-diagnostico.md",
                List.of("Proyecto cargado: " + project.metadata().title() + "."),
                List.of("El reporte diagnóstico describe estado y advertencias; no repara archivos por sí solo."));
    }

    private ExportReadinessItem podcastWav(Path projectFile, NarrationScriptDocument script, List<AudioJobSnapshot> jobs) {
        List<String> missing = new ArrayList<>();
        if (projectFile == null) {
            missing.add("Guarda el proyecto antes de exportar audio final.");
        }
        if (podcastWav.canExport(jobs)) {
            if (!missing.isEmpty()) {
                return ExportReadinessItem.blocked(
                        ExportableArtifactKind.PODCAST_WAV,
                        DocuPodcastExportFormat.WAV,
                        "audio-final.wav / .mp3 / .aac",
                        missing,
                        List.of("Solo se exporta audio realmente existente; no se sintetizan fragmentos faltantes durante la exportaciÃ³n."));
            }
            return ExportReadinessItem.exportable(
                    ExportableArtifactKind.PODCAST_WAV,
                    DocuPodcastExportFormat.WAV,
                    "audio-final.wav / .mp3 / .aac",
                    List.of("Hay audio exportable para WAV final; MP3/AAC se comprimen desde WAV usando Video local/FFmpeg."),
                    List.of("Solo se exporta audio realmente existente; no se sintetizan fragmentos faltantes durante la exportación."));
        }
        if (script == null || script.empty()) {
            missing.add("Prepara la lectura antes de generar o exportar audio final.");
        } else if (jobs == null || jobs.isEmpty()) {
            missing.add("Genera audio antes de exportar podcast WAV.");
        } else if (jobs.stream().noneMatch(job -> job.state() == AudioJobState.COMPLETED)) {
            missing.add("Completa o reanuda un job de audio; los jobs actuales no están finalizados.");
        } else {
            missing.add("El último audio no tiene WAV final ni todos los segmentos WAV completos disponibles.");
        }
        return ExportReadinessItem.blocked(
                ExportableArtifactKind.PODCAST_WAV,
                DocuPodcastExportFormat.WAV,
                "audio-final.wav / .mp3 / .aac",
                missing,
                List.of("La exportación final no rellena silencio ni segmentos incompletos sin decisión explícita del usuario."));
    }

    private ExportReadinessItem documentTextAudioVideo(Path projectFile,
                                                       NarrationScriptDocument script,
                                                       List<AudioJobSnapshot> jobs) {
        ArrayList<String> missing = new ArrayList<>();
        if (projectFile == null) {
            missing.add("Guarda el proyecto antes de exportar video documental texto+audio.");
        }
        if (script == null || script.empty()) {
            missing.add("Prepara la lectura del documento antes de exportar video documental texto+audio.");
        }
        int missingAudio = missingAudioCount(script, jobs);
        if (script != null && !script.empty() && missingAudio > 0) {
            missing.add("Falta audio listo en " + missingAudio + " fragmento(s) narrable(s).");
        }
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO,
                    DocuPodcastExportFormat.MP4,
                    "estudio-documental-texto-audio.mp4",
                    missing,
                    List.of("Esta salida crea frames temporales de texto y usa audio existente; no crea imagenes ni Storyboard."));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO,
                DocuPodcastExportFormat.MP4,
                "estudio-documental-texto-audio.mp4",
                List.of("Lectura preparada y audio listo para " + primarySegmentCount(script) + " fragmento(s) narrable(s)."),
                List.of("Los frames de texto se generan temporalmente en exports/document-study-frames y no se guardan como assets."));
    }


    private ExportReadinessItem finalVideoMp4(DocuPodcastProject project,
                                              Path projectFile,
                                              NarrationScriptDocument script,
                                              StoryboardDocument storyboard,
                                              List<AudioJobSnapshot> jobs) {
        ArrayList<String> missing = new ArrayList<>();
        if (projectFile == null) {
            missing.add("Guarda el proyecto antes de exportar MP4 final.");
        }
        if (script == null || script.empty()) {
            missing.add("Prepara la lectura del documento antes de exportar MP4 final.");
        }
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.FINAL_VIDEO_MP4,
                    DocuPodcastExportFormat.MP4,
                    "video-final.mp4",
                    missing,
                    List.of("El MP4 final necesita proyecto guardado, lectura preparada, visuales asignados, audio listo y Video local disponible."));
        }
        if (projectModePolicy.resolve(project) == ProjectMode.NARRATIVE_VIDEO) {
            return narrativeFinalVideo(project, script, storyboard, jobs);
        }
        SimpleVideoPlan plan;
        try {
            var narrationPlan = buildNarrationRenderPlan.build(script, project);
            var renderUnitPlan = buildRenderUnitPlan.build(narrationPlan);
            plan = buildSimpleVideoPlan.build(renderUnitPlan, project.assets(), jobs);
        } catch (RuntimeException ex) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.FINAL_VIDEO_MP4,
                    DocuPodcastExportFormat.MP4,
                    "video-final.mp4",
                    List.of("No se pudo construir el plan de video final: " + ex.getMessage()),
                    List.of("La revisión de exportación no debe prometer MP4 si el plan visual/audio no se puede construir."));
        }
        if (plan.frameCount() == 0) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.FINAL_VIDEO_MP4,
                    DocuPodcastExportFormat.MP4,
                    "video-final.mp4",
                    List.of("No hay fragmentos con imagen asociada para renderizar video."),
                    List.of("Los fragmentos sin visual asignado se omiten del video; asocia al menos una imagen para crear MP4."));
        }
        if (plan.framesMissingImage() > 0 || plan.framesMissingAudio() > 0) {
            if (plan.framesMissingImage() > 0) {
                missing.add("Faltan imágenes en " + plan.framesMissingImage() + " frame(s) renderizables.");
            }
            if (plan.framesMissingAudio() > 0) {
                missing.add("Falta audio listo en " + plan.framesMissingAudio() + " frame(s) hablados.");
            }
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.FINAL_VIDEO_MP4,
                    DocuPodcastExportFormat.MP4,
                    "video-final.mp4",
                    missing,
                    List.of("La app no rellena audio o imágenes faltantes durante el render final sin decisión explícita."));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.FINAL_VIDEO_MP4,
                DocuPodcastExportFormat.MP4,
                "video-final.mp4",
                List.of("Hay " + plan.frameCount() + " frame(s) con visual y audio listo para MP4 final."),
                List.of("Video local/FFmpeg se valida al iniciar el render; si no está disponible, se mostrará un bloqueo operativo."));
    }

    private TheatreProductionProjection theatreProjection(DocuPodcastProject project,
                                                          NarrationScriptDocument script,
                                                          StoryboardDocument storyboard,
                                                          List<AudioJobSnapshot> jobs) {
        var fragments = buildFragmentWorkspaceProjection.build(null, script, project, storyboard, jobs, PlaybackManifest.empty());
        return buildTheatreProductionProjection.build(project, null, script, fragments, jobs, PlaybackManifest.empty());
    }

    private ExportReadinessItem theatreWork(Path projectFile, TheatreProductionProjection projection) {
        TheatreProductionReadiness readiness = projection.readiness();
        ArrayList<String> missing = withSavedProject(projectFile, readiness.workMissingRequirements());
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.THEATRE_WORK_VIDEO,
                    DocuPodcastExportFormat.MP4,
                    "docupodcast-teatro-limpio.mp4",
                    missing,
                    theatreLimitations(readiness));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.THEATRE_WORK_VIDEO,
                DocuPodcastExportFormat.MP4,
                "docupodcast-teatro-limpio.mp4",
                theatreEvidence("obra", readiness),
                theatreLimitations(readiness));
    }

    private ExportReadinessItem theatreSpatialMap(Path projectFile, TheatreProductionProjection projection) {
        TheatreProductionReadiness readiness = projection.readiness();
        ArrayList<String> missing = withSavedProject(projectFile, readiness.spatialMapMissingRequirements());
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO,
                    DocuPodcastExportFormat.MP4,
                    "docupodcast-mapa-teatral.mp4",
                    missing,
                    theatreLimitations(readiness));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO,
                DocuPodcastExportFormat.MP4,
                "docupodcast-mapa-teatral.mp4",
                theatreEvidence("mapa teatral", readiness),
                theatreLimitations(readiness));
    }

    private ExportReadinessItem theatrePortion(Path projectFile, TheatreProductionProjection projection) {
        TheatreProductionReadiness readiness = projection.readiness();
        ArrayList<String> missing = withSavedProject(projectFile, readiness.portionMissingRequirements());
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.THEATRE_PORTION_VIDEO,
                    DocuPodcastExportFormat.MP4,
                    "docupodcast-porcion-obra.mp4",
                    missing,
                    theatreLimitations(readiness));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.THEATRE_PORTION_VIDEO,
                DocuPodcastExportFormat.MP4,
                "docupodcast-porcion-obra.mp4",
                theatreEvidence("acto o escena", readiness),
                theatreLimitations(readiness));
    }

    private static ArrayList<String> withSavedProject(Path projectFile, List<String> requirements) {
        ArrayList<String> missing = new ArrayList<>();
        if (projectFile == null) {
            missing.add("Guarda el proyecto antes de exportar teatro.");
        }
        if (requirements != null) {
            missing.addAll(requirements);
        }
        return missing;
    }

    private static List<String> theatreEvidence(String target, TheatreProductionReadiness readiness) {
        return List.of(
                "Preparacion de " + target + ": " + readiness.interventionCount() + " intervencion(es), "
                        + readiness.sceneCount() + " escena(s), " + readiness.actCount() + " acto(s).",
                "Audio listo en " + readiness.audioReadyCount() + "/" + readiness.interventionCount()
                        + " intervencion(es); visuales teatrales en " + readiness.visualReadyCount() + "/"
                        + readiness.interventionCount() + ".",
                "Mapa teatral: " + readiness.textPlacementCount() + " placement(s), "
                        + readiness.spatialPositionCount() + " posicion(es), "
                        + readiness.actionCount() + " accion(es).");
    }

    private static List<String> theatreLimitations(TheatreProductionReadiness readiness) {
        ArrayList<String> limitations = new ArrayList<>();
        limitations.add("La exportacion teatral reutiliza audio, visuales, mapas y exportadores actuales; no genera faltantes en silencio.");
        limitations.add("No cambia el schema de TheatreProjectLayer ni crea una arquitectura paralela de jobs.");
        limitations.addAll(readiness.warnings());
        return List.copyOf(limitations);
    }

    private ExportReadinessItem narrativeFinalVideo(DocuPodcastProject project,
                                                    NarrationScriptDocument script,
                                                    StoryboardDocument storyboard,
                                                    List<AudioJobSnapshot> jobs) {
        var fragments = buildFragmentWorkspaceProjection.build(null, script, project, storyboard, jobs, PlaybackManifest.empty());
        NarrativeVideoWorkspaceProjection projection = buildNarrativeVideoWorkspaceProjection.build(fragments);
        SimpleVideoPlan plan = buildNarrativeVideoPlan.build("Video narrativo - " + script.title(), projection);
        ArrayList<String> missing = new ArrayList<>();
        if (projection.narratableCount() == 0) {
            missing.add("No hay fragmentos narrativos preparados para el video.");
        }
        if (projection.missingMainImageCount() > 0) {
            missing.add("Falta imagen principal en " + projection.missingMainImageCount() + " fragmento(s) narrativos.");
        }
        if (projection.missingAudioCount() > 0) {
            missing.add("Falta audio listo en " + projection.missingAudioCount() + " fragmento(s) narrativos.");
        }
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.FINAL_VIDEO_MP4,
                    DocuPodcastExportFormat.MP4,
                    "video-narrativo-final.mp4",
                    missing,
                    List.of("La imagen puente es opcional; el bloqueo solo exige fragmentos, audio e imagen principal."));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.FINAL_VIDEO_MP4,
                DocuPodcastExportFormat.MP4,
                "video-narrativo-final.mp4",
                List.of("Hay " + projection.readyForExportCount() + " fragmento(s) narrativos listos y "
                        + projection.bridgeImageCount() + " imagen(es) puente opcionales."),
                List.of("El MP4 narrativo usa el texto del fragmento como subtitulo base y reutiliza SimpleVideoPlan con "
                        + plan.frameCount() + " frame(s)."));
    }

    private static int missingAudioCount(NarrationScriptDocument script, List<AudioJobSnapshot> jobs) {
        if (script == null || script.empty()) {
            return 0;
        }
        java.util.Set<String> audioIds = completedAudioIds(jobs);
        int missing = 0;
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable() || secondaryReadUnit(segment)) {
                continue;
            }
            if (!hasAudioForSegment(audioIds, segment.id())) {
                missing++;
            }
        }
        return missing;
    }

    private static int primarySegmentCount(NarrationScriptDocument script) {
        if (script == null) {
            return 0;
        }
        return (int) script.segments().stream()
                .filter(NarrationSegment::narratable)
                .filter(segment -> !secondaryReadUnit(segment))
                .count();
    }

    private static java.util.Set<String> completedAudioIds(List<AudioJobSnapshot> jobs) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        if (jobs == null) {
            return result;
        }
        for (AudioJobSnapshot job : jobs) {
            if (job == null) {
                continue;
            }
            for (AudioSegmentSnapshot segment : job.segments()) {
                if (segment.completed() && !segment.audioRelativePath().isBlank()) {
                    result.add(segment.segmentId());
                }
            }
        }
        return result;
    }

    private static boolean hasAudioForSegment(java.util.Set<String> audioIds, String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return false;
        }
        if (audioIds.contains(segmentId)) {
            return true;
        }
        String unitPrefix = segmentId + "-";
        return audioIds.stream().anyMatch(id -> id.startsWith(unitPrefix));
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private ExportReadinessItem storyboardSummary(NarrationScriptDocument script, StoryboardDocument storyboard) {
        if (!exportFormatPolicy.canExportStoryboardPackage(script, storyboard)) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.STORYBOARD_SUMMARY,
                    DocuPodcastExportFormat.MARKDOWN,
                    "resumen_visual.md",
                    List.of("Necesitas lectura preparada y panel visual materializado para exportar el resumen visual."),
                    List.of("El panel visual es una capa del documento, no un editor de video completo."));
        }
        if (storyboard.bindingCount() == 0) {
            return ExportReadinessItem.warning(
                    ExportableArtifactKind.STORYBOARD_SUMMARY,
                    DocuPodcastExportFormat.MARKDOWN,
                    "resumen_visual.md",
                    List.of("Panel visual cargado sin imágenes asociadas."),
                    List.of("Asocia imágenes a fragmentos si esperas un resumen visual útil."),
                    List.of("El resumen no genera imágenes ni video; solo declara recursos existentes."));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.STORYBOARD_SUMMARY,
                DocuPodcastExportFormat.MARKDOWN,
                "resumen_visual.md",
                List.of("Panel visual cargado con " + storyboard.bindingCount() + " imágenes asociadas."),
                List.of("El resumen documenta asociaciones; el render de video se prepara por el paquete de video simple."));
    }

    private ExportReadinessItem simpleVideoPackage(NarrationScriptDocument script, StoryboardDocument storyboard, List<AudioJobSnapshot> jobs) {
        if (!exportFormatPolicy.canExportSimpleVideo(script)) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE,
                    DocuPodcastExportFormat.VIDEO_PACKAGE,
                    "video-simple/",
                    List.of("Prepara la lectura del documento antes de exportar el paquete de video simple."),
                    List.of("El video simple usa fragmentos preparados; no convierte el documento fuente directamente a MP4."));
        }
        ArrayList<String> warnings = new ArrayList<>();
        if (storyboard == null || storyboard.bindingCount() == 0) {
            warnings.add("No hay imágenes asociadas; el paquete queda para revisión visual o fallback neutro.");
        }
        if (!podcastWav.canExport(jobs)) {
            warnings.add("No hay audio final completo; el paquete será auditable pero puede quedar pendiente de WAV/FFmpeg.");
        }
        if (!warnings.isEmpty()) {
            return ExportReadinessItem.warning(
                    ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE,
                    DocuPodcastExportFormat.VIDEO_PACKAGE,
                    "video-simple/",
                    List.of("Lectura preparada cargada con " + script.segmentCount() + " fragmentos."),
                    warnings,
                    List.of("El paquete genera plan, CSV, manifest y comandos; la exportación no crea MP4 por sí sola y el render depende de audio, imágenes y FFmpeg disponible."));
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE,
                DocuPodcastExportFormat.VIDEO_PACKAGE,
                "video-simple/",
                List.of("Lectura preparada, panel visual y audio exportable están disponibles."),
                List.of("La exportación prepara contrato de render; la ejecución real de FFmpeg permanece auditable y cancelable."));
    }
}
