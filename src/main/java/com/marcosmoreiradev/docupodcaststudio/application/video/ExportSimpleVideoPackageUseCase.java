package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Exports a simple-video package with an auditable render contract for FFmpeg. */
public final class ExportSimpleVideoPackageUseCase {
    private final BuildSimpleVideoPlanUseCase buildSimpleVideoPlan;
    private final BuildVideoRenderCommandPlanUseCase buildRenderCommandPlan;

    public ExportSimpleVideoPackageUseCase() {
        this(new BuildSimpleVideoPlanUseCase(), new BuildVideoRenderCommandPlanUseCase());
    }

    public ExportSimpleVideoPackageUseCase(BuildSimpleVideoPlanUseCase buildSimpleVideoPlan) {
        this(buildSimpleVideoPlan, new BuildVideoRenderCommandPlanUseCase());
    }

    public ExportSimpleVideoPackageUseCase(
            BuildSimpleVideoPlanUseCase buildSimpleVideoPlan,
            BuildVideoRenderCommandPlanUseCase buildRenderCommandPlan
    ) {
        this.buildSimpleVideoPlan = java.util.Objects.requireNonNull(buildSimpleVideoPlan, "buildSimpleVideoPlan");
        this.buildRenderCommandPlan = java.util.Objects.requireNonNull(buildRenderCommandPlan, "buildRenderCommandPlan");
    }

    public boolean canExport(NarrationScriptDocument script) {
        return script != null && !script.empty();
    }

    public SimpleVideoPackageExportResult export(
            DocuPodcastProject project,
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> jobs,
            Path targetDirectory
    ) throws IOException {
        return export(project, script, storyboard, jobs, targetDirectory, SimpleVideoExportSettings.defaults());
    }

    public SimpleVideoPackageExportResult export(
            DocuPodcastProject project,
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> jobs,
            Path targetDirectory,
            SimpleVideoExportSettings settings
    ) throws IOException {
        return export(project, script, storyboard, jobs, targetDirectory, settings, null);
    }

    public SimpleVideoPackageExportResult export(
            DocuPodcastProject project,
            NarrationScriptDocument script,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> jobs,
            Path targetDirectory,
            SimpleVideoExportSettings settings,
            Path configuredFfmpeg
    ) throws IOException {
        if (!canExport(script)) {
            throw new IOException("No hay narración interna para exportar video simple.");
        }
        SimpleVideoExportSettings exportSettings = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        Path root = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("frames"));
        ProjectAssetCatalog assets = project == null ? ProjectAssetCatalog.empty() : project.assets();
        SimpleVideoPlan plan = buildSimpleVideoPlan.build(script, storyboard, assets, jobs, exportSettings.silenceAfterFrameSeconds());
        plan = enrichWithCharacterLabels(plan, project == null ? null : project.theatre());
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, configuredFfmpeg);
        VideoRenderCommandPlan commandPlan = buildRenderCommandPlan.build(plan, exportSettings, discovery);

        Path planFile = root.resolve("VIDEO_SIMPLE_PLAN.md");
        Path framesFile = root.resolve("frames.csv");
        Path concatFile = root.resolve("ffmpeg-concat.txt");
        Path scriptFile = root.resolve("render-video-simple.bat");
        Path contractFile = root.resolve("FFMPEG_RENDER_CONTRACT.md");
        Path manifestFile = root.resolve("RENDER_MANIFEST.json");
        Path commandsFile = root.resolve("render-commands.txt");
        Path renderStateFile = root.resolve("RENDER_STATE.md");
        Files.writeString(planFile, planMarkdown(plan, exportSettings, commandPlan), StandardCharsets.UTF_8);
        Files.writeString(framesFile, framesCsv(plan), StandardCharsets.UTF_8);
        Files.writeString(concatFile, ffmpegConcat(plan), StandardCharsets.UTF_8);
        Files.writeString(scriptFile, renderScript(exportSettings, commandPlan), StandardCharsets.UTF_8);
        Files.writeString(contractFile, ffmpegRenderContract(exportSettings, commandPlan), StandardCharsets.UTF_8);
        Files.writeString(manifestFile, commandPlan.manifestJson(), StandardCharsets.UTF_8);
        Files.writeString(commandsFile, commandPlan.commandsText(), StandardCharsets.UTF_8);
        Files.writeString(renderStateFile, renderStateMarkdown(commandPlan), StandardCharsets.UTF_8);
        return new SimpleVideoPackageExportResult(root, planFile, framesFile, concatFile, scriptFile,
                manifestFile, commandsFile, renderStateFile,
                commandPlan.renderModeLabel(), commandPlan.renderableAsMp4(), commandPlan.outputFileName(),
                plan.frameCount(), plan.totalDurationSeconds(), plan.framesMissingImage(), plan.framesMissingAudio());
    }


    public SimpleVideoPackageExportResult export(
            DocuPodcastProject project,
            RenderUnitPlan renderUnitPlan,
            List<AudioJobSnapshot> jobs,
            Path targetDirectory
    ) throws IOException {
        return export(project, renderUnitPlan, jobs, targetDirectory, SimpleVideoExportSettings.defaults(), null);
    }

    public SimpleVideoPackageExportResult export(
            DocuPodcastProject project,
            RenderUnitPlan renderUnitPlan,
            List<AudioJobSnapshot> jobs,
            Path targetDirectory,
            SimpleVideoExportSettings settings,
            Path configuredFfmpeg
    ) throws IOException {
        return export(project, renderUnitPlan, jobs, targetDirectory, settings, configuredFfmpeg, null);
    }

    public SimpleVideoPackageExportResult export(
            DocuPodcastProject project,
            RenderUnitPlan renderUnitPlan,
            List<AudioJobSnapshot> jobs,
            Path targetDirectory,
            SimpleVideoExportSettings settings,
            Path configuredFfmpeg,
            TheatreProjectLayer theatre
    ) throws IOException {
        if (renderUnitPlan == null || renderUnitPlan.videoUnits().isEmpty()) {
            throw new IOException("No hay unidades visuales asignadas para exportar secuencia visual/video.");
        }
        SimpleVideoExportSettings exportSettings = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        Path root = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("frames"));
        ProjectAssetCatalog assets = project == null ? ProjectAssetCatalog.empty() : project.assets();
        SimpleVideoPlan plan = buildSimpleVideoPlan.build(renderUnitPlan, assets, jobs, exportSettings.silenceAfterFrameSeconds());
        plan = enrichWithCharacterLabels(plan, theatre);
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, configuredFfmpeg);
        VideoRenderCommandPlan commandPlan = buildRenderCommandPlan.build(plan, exportSettings, discovery);

        Path planFile = root.resolve("VIDEO_SIMPLE_PLAN.md");
        Path framesFile = root.resolve("frames.csv");
        Path concatFile = root.resolve("ffmpeg-concat.txt");
        Path scriptFile = root.resolve("render-video-simple.bat");
        Path contractFile = root.resolve("FFMPEG_RENDER_CONTRACT.md");
        Path manifestFile = root.resolve("RENDER_MANIFEST.json");
        Path commandsFile = root.resolve("render-commands.txt");
        Path renderStateFile = root.resolve("RENDER_STATE.md");
        Files.writeString(planFile, planMarkdown(plan, exportSettings, commandPlan), StandardCharsets.UTF_8);
        Files.writeString(framesFile, framesCsv(plan), StandardCharsets.UTF_8);
        Files.writeString(concatFile, ffmpegConcat(plan), StandardCharsets.UTF_8);
        Files.writeString(scriptFile, renderScript(exportSettings, commandPlan), StandardCharsets.UTF_8);
        Files.writeString(contractFile, ffmpegRenderContract(exportSettings, commandPlan), StandardCharsets.UTF_8);
        Files.writeString(manifestFile, commandPlan.manifestJson(), StandardCharsets.UTF_8);
        Files.writeString(commandsFile, commandPlan.commandsText(), StandardCharsets.UTF_8);
        Files.writeString(renderStateFile, renderStateMarkdown(commandPlan), StandardCharsets.UTF_8);
        return new SimpleVideoPackageExportResult(root, planFile, framesFile, concatFile, scriptFile,
                manifestFile, commandsFile, renderStateFile,
                commandPlan.renderModeLabel(), commandPlan.renderableAsMp4(), commandPlan.outputFileName(),
                plan.frameCount(), plan.totalDurationSeconds(), plan.framesMissingImage(), plan.framesMissingAudio());
    }

    private static String planMarkdown(SimpleVideoPlan plan, SimpleVideoExportSettings settings, VideoRenderCommandPlan commandPlan) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Plan de video simple DocuPodcast\n\n");
        markdown.append("Este paquete prepara frames simples para un video de lectura narrada. ")
                .append("Es un paquete renderizable y auditable con contrato de comandos FFmpeg. ")
                .append("La exportación no crea el MP4 final por sí sola; el render MP4 requiere ejecutar el contrato FFmpeg o un job de render explícito.\n\n");
        markdown.append("- Contrato de render: ").append(commandPlan.renderModeLabel()).append("\n");
        markdown.append("- Manifest de render: RENDER_MANIFEST.json\n");
        markdown.append("- Comandos auditables: render-commands.txt\n");
        markdown.append("- Frames: ").append(plan.frameCount()).append('\n');
        markdown.append("- Duración estimada: ").append(format(plan.totalDurationSeconds())).append(" s\n");
        markdown.append("- Silencio posterior por frame: ").append(format(plan.silenceAfterFrameSeconds())).append(" s\n");
        markdown.append("- Resolución objetivo: ").append(settings.resolutionLabel()).append("\n");
        markdown.append("- Política de dispositivo: ").append(settings.computePolicy().name()).append("\n");
        markdown.append("- Encoder solicitado: ").append(settings.encoderPolicy().name()).append("\n");
        markdown.append("- Encoder efectivo: ").append(commandPlan.effectiveEncoder()).append("\n");
        markdown.append("- FFmpeg embebido: ").append(settings.preferEmbeddedFfmpeg() ? "preferido" : "externo/configurado").append("\n");
        markdown.append("- Modo render: bloquea temporalmente lectura, edición y nuevas exportaciones de video cuando se ejecute FFmpeg\n");
        markdown.append("- Cancelación segura: detener FFmpeg conserva el paquete y permite reintentar\n");
        markdown.append("- Frames sin imagen: ").append(plan.framesMissingImage()).append('\n');
        markdown.append("- Frames sin audio hablado requerido: ").append(plan.framesMissingAudio()).append("\n");
        markdown.append("- Frames visuales silenciosos: ").append(plan.framesSilentVisual()).append("\n\n");
        markdown.append("## Frames\n\n");
        for (SimpleVideoFrame frame : plan.frames()) {
            markdown.append("### ").append(frame.id()).append(" — ").append(frame.title()).append("\n\n");
            markdown.append("- Segmento: ").append(frame.segmentId()).append('\n');
            markdown.append("- Imagen: ").append(frame.imageLabel()).append('\n');
            markdown.append("- Modo: ").append(frame.frameModeLabel()).append('\n');
            markdown.append("- Audio: ").append(frame.audioLabel()).append('\n');
            markdown.append("- Duración audio: ").append(format(frame.audioDurationSeconds())).append(" s\n");
            markdown.append("- Duración frame: ").append(format(frame.frameDurationSeconds())).append(" s\n");
            markdown.append("- Texto: ").append(frame.narrationPreview()).append("\n\n");
        }
        return markdown.toString();
    }

    private static String framesCsv(SimpleVideoPlan plan) {
        StringBuilder csv = new StringBuilder("frameId,segmentId,image,audio,audioSeconds,silenceAfterSeconds,frameSeconds,text\n");
        for (SimpleVideoFrame frame : plan.frames()) {
            csv.append(escape(frame.id())).append(',')
                    .append(escape(frame.segmentId())).append(',')
                    .append(escape(frame.frameModeLabel())).append(',')
                    .append(escape(frame.imageRelativePath())).append(',')
                    .append(escape(frame.audioRelativePath())).append(',')
                    .append(format(frame.audioDurationSeconds())).append(',')
                    .append(format(frame.silenceAfterSeconds())).append(',')
                    .append(format(frame.frameDurationSeconds())).append(',')
                    .append(escape(frame.narrationPreview())).append('\n');
        }
        return csv.toString();
    }

    private static String ffmpegConcat(SimpleVideoPlan plan) {
        StringBuilder concat = new StringBuilder();
        concat.append("# Lista de audio para FFmpeg concat. Las imágenes se resuelven desde frames.csv.\n");
        for (SimpleVideoFrame frame : plan.frames()) {
            if (!frame.audioRelativePath().isBlank()) {
                concat.append("file '").append(frame.audioRelativePath().replace("'", "'\\''")).append("'\n");
            } else {
                concat.append("# sin audio para ").append(frame.segmentId()).append('\n');
            }
        }
        return concat.toString();
    }

    private static String renderScript(SimpleVideoExportSettings settings, VideoRenderCommandPlan commandPlan) {
        String resolution = settings.resolution().width() + "x" + settings.resolution().height();
        String scale = settings.resolution().ffmpegScaleExpression().replace("%", "%%");
        return "@echo off\r\n"
                + "setlocal EnableExtensions EnableDelayedExpansion\r\n"
                + "echo DocuPodcast Studio - Render de video simple\r\n"
                + "echo Resolucion objetivo: " + resolution + " (" + settings.resolution().label() + ")\r\n"
                + "echo Modo render: la aplicacion debe bloquear lectura y nuevas exportaciones mientras FFmpeg trabaja.\r\n"
                + "set \"SCRIPT_DIR=%~dp0\"\r\n"
                + "set \"FFMPEG=%SCRIPT_DIR%tools\\ffmpeg\\bin\\ffmpeg.exe\"\r\n"
                + "if not exist \"%FFMPEG%\" set \"FFMPEG=%SCRIPT_DIR%..\\tools\\ffmpeg\\bin\\ffmpeg.exe\"\r\n"
                + "if not exist \"%FFMPEG%\" set \"FFMPEG=ffmpeg\"\r\n"
                + "echo Usando FFmpeg: %FFMPEG%\r\n"
                + "echo Manifest: RENDER_MANIFEST.json\r\n"
                + "echo Comandos: render-commands.txt\r\n"
                + "echo Estado: " + commandPlan.renderModeLabel() + "\r\n"
                + "echo Escala/pad objetivo: " + scale + "\r\n"
                + "echo Esta exportacion preparo un paquete; no creo el MP4 final por si sola.\r\n"
                + "echo Ejecuta o revisa render-commands.txt para producir video-simple.mp4 cuando el paquete este listo.\r\n"
                + "echo Si FFmpeg no existe, instala el paquete con tools/ffmpeg o configura la ruta desde Configuracion.\r\n";
    }

    private static String ffmpegRenderContract(SimpleVideoExportSettings settings, VideoRenderCommandPlan commandPlan) {
        return "# Contrato de render FFmpeg embebido\n\n"
                + "- Contrato: `docupodcast-simple-video-render-v1`.\n"
                + "- Ruta preferida: `tools/ffmpeg/bin/ffmpeg.exe`.\n"
                + "- Ruta complementaria: `tools/ffmpeg/bin/ffprobe.exe`.\n"
                + "- No se exige tocar PATH ni instalar FFmpeg globalmente.\n"
                + "- Resolución por defecto: " + settings.resolutionLabel() + ".\n"
                + "- Dispositivo de render solicitado: " + settings.computePolicy().name() + ".\n"
                + "- Encoder solicitado: " + settings.encoderPolicy().name() + " (`" + commandPlan.effectiveEncoder() + "`).\n"
                + "- Opciones de usuario: 720p, 1080p, 2K y 4K.\n"
                + "- Manifest generado: `RENDER_MANIFEST.json`.\n"
                + "- Comandos generados: `render-commands.txt`.\n"
                + "- Estado del paquete: " + commandPlan.renderModeLabel() + ".\n"
                + "- Este paquete es renderizable y auditable; la exportación no crea el MP4 final por sí sola.\n"
                + "- El render MP4 queda listo solo cuando audio, imágenes y FFmpeg están disponibles.\n"
                + "- Mientras se renderiza, la pantalla operativa entra en modo render bloqueante con barra de progreso y cancelación segura.\n";
    }

    private static String renderStateMarkdown(VideoRenderCommandPlan commandPlan) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Estado de render de video simple\n\n");
        markdown.append("- Contrato: `docupodcast-simple-video-render-v1`\n");
        markdown.append("- Estado: ").append(commandPlan.renderModeLabel()).append("\n");
        markdown.append("- Salida esperada: `").append(commandPlan.outputFileName()).append("`\n");
        markdown.append("- Política dispositivo: ").append(commandPlan.settings().computePolicy().name()).append("\n");
        markdown.append("- Encoder: ").append(commandPlan.settings().encoderPolicy().name()).append(" / ").append(commandPlan.effectiveEncoder()).append("\n");
        markdown.append("- Hardware acceleration ready: ").append(commandPlan.hardwareAccelerationReady()).append("\n");
        markdown.append("- Fallback: ").append(commandPlan.fallbackReason()).append("\n");
        markdown.append("- Bloqueo operativo: lectura normal y nuevas exportaciones quedan bloqueadas durante el render.\n");
        markdown.append("- Cancelación segura: detener FFmpeg conserva el paquete para reintentar.\n");
        markdown.append("\n## Advertencias\n\n");
        if (commandPlan.warnings().isEmpty()) {
            markdown.append("Sin advertencias: el paquete está listo para render MP4 con FFmpeg; esta exportación no ejecutó el MP4 final.\n");
        } else {
            for (String warning : commandPlan.warnings()) {
                markdown.append("- ").append(warning).append("\n");
            }
        }
        return markdown.toString();
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static SimpleVideoPlan enrichWithCharacterLabels(SimpleVideoPlan plan, TheatreProjectLayer theatre) {
        if (plan == null || theatre == null || theatre.textActionPlacements().isEmpty()) {
            return plan;
        }
        List<SimpleVideoFrame> enriched = new ArrayList<>();
        for (SimpleVideoFrame frame : plan.frames()) {
            String sceneId = resolveSceneId(frame, theatre);
            List<SimpleVideoFrame.CharacterLabel> labels = BuildSimpleVideoPlanUseCase.buildCharacterLabels(
                    theatre.textActionPlacements(), sceneId);
            enriched.add(new SimpleVideoFrame(
                    frame.id(), frame.segmentId(), frame.title(), frame.narrationPreview(),
                    frame.imageAssetId(), frame.imageRelativePath(), frame.audioRelativePath(),
                    frame.audioDurationSeconds(), frame.silenceAfterSeconds(),
                    frame.imageAssigned(), frame.audioReady(), frame.silentVisual(), labels));
        }
        return new SimpleVideoPlan(plan.title(), enriched, plan.silenceAfterFrameSeconds(), plan.createdAt());
    }

    private static String resolveSceneId(SimpleVideoFrame frame, TheatreProjectLayer theatre) {
        if (frame == null || theatre == null) {
            return "";
        }
        for (TheatreProjectLayer.TextActionPlacement p : theatre.textActionPlacements()) {
            if (p.intervencionId().equals(frame.segmentId())) {
                return p.sceneId();
            }
        }
        return "";
    }
}
