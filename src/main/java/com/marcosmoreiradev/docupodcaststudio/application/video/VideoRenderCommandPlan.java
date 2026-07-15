package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Deterministic render contract for a simple DocuPodcast image+audio video package. */
public record VideoRenderCommandPlan(
        SimpleVideoPlan videoPlan,
        SimpleVideoExportSettings settings,
        FfmpegToolDiscovery ffmpeg,
        String outputFileName,
        List<String> renderCommands,
        List<String> warnings,
        Instant createdAt
) {
    public VideoRenderCommandPlan {
        settings = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        outputFileName = outputFileName == null || outputFileName.isBlank() ? "video-simple.mp4" : outputFileName.strip();
        renderCommands = renderCommands == null ? List.of() : List.copyOf(renderCommands);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public boolean ffmpegReady() {
        return ffmpeg != null && ffmpeg.ready();
    }

    public boolean renderableAsMp4() {
        return videoPlan != null && videoPlan.exportableAsRenderedVideo() && ffmpegReady() && warnings.isEmpty();
    }

    public String renderModeLabel() {
        if (renderableAsMp4()) {
            return "MP4_RENDER_READY";
        }
        if (videoPlan != null && videoPlan.exportableAsRenderedVideo()) {
            return "PACKAGE_READY_FFMPEG_REQUIRED";
        }
        return "PACKAGE_NEEDS_REVIEW";
    }

    public String effectiveEncoder() {
        if (settings.encoderPolicy().ffmpegEncoder().isBlank()) {
            return "FFMPEG_DEFAULT";
        }
        return settings.encoderPolicy().ffmpegEncoder();
    }

    public boolean hardwareAccelerationReady() {
        return ffmpegReady() && settings.encoderPolicy().hardwareAccelerated();
    }

    public String fallbackReason() {
        if (!ffmpegReady()) {
            return "FFmpeg no disponible; paquete auditable hasta configurar herramienta.";
        }
        if (settings.encoderPolicy().hardwareAccelerated()) {
            return "Encoder por hardware solicitado; soporte real depende del binario FFmpeg instalado.";
        }
        if (settings.computePolicy() == com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.CPU_ONLY) {
            return "Política CPU_ONLY: render por CPU o encoder por defecto.";
        }
        return "Sin fallback requerido.";
    }

    public VideoRenderProgress initialProgress() {
        int frames = videoPlan == null ? 0 : videoPlan.frameCount();
        return VideoRenderProgress.preparing(frames);
    }

    public String manifestJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"contract\": \"docupodcast-simple-video-render-v1\",\n");
        json.append("  \"renderMode\": \"").append(renderModeLabel()).append("\",\n");
        json.append("  \"outputFile\": \"").append(escape(outputFileName)).append("\",\n");
        json.append("  \"resolution\": \"").append(escape(settings.resolutionLabel())).append("\",\n");
        json.append("  \"renderDevicePolicy\": \"").append(settings.computePolicy().name()).append("\",\n");
        json.append("  \"requestedEncoder\": \"").append(settings.encoderPolicy().name()).append("\",\n");
        json.append("  \"effectiveEncoder\": \"").append(escape(effectiveEncoder())).append("\",\n");
        json.append("  \"hardwareAccelerationRequested\": ").append(settings.encoderPolicy().hardwareAccelerated()).append(",\n");
        json.append("  \"hardwareAccelerationReady\": ").append(hardwareAccelerationReady()).append(",\n");
        json.append("  \"fallbackReason\": \"").append(escape(fallbackReason())).append("\",\n");
        json.append("  \"framesPerSecond\": ").append(settings.framesPerSecond()).append(",\n");
        json.append("  \"mainWorkspaceBlockedDuringRender\": ").append(settings.blockMainWorkspaceDuringRender()).append(",\n");
        json.append("  \"ffmpegReady\": ").append(ffmpegReady()).append(",\n");
        json.append("  \"frameCount\": ").append(videoPlan == null ? 0 : videoPlan.frameCount()).append(",\n");
        json.append("  \"totalDurationSeconds\": ").append(format(videoPlan == null ? 0.0 : videoPlan.totalDurationSeconds())).append(",\n");
        json.append("  \"framesMissingImage\": ").append(videoPlan == null ? 0 : videoPlan.framesMissingImage()).append(",\n");
        json.append("  \"framesMissingAudio\": ").append(videoPlan == null ? 0 : videoPlan.framesMissingAudio()).append(",\n");
        json.append("  \"warnings\": [");
        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append("\"").append(escape(warnings.get(i))).append("\"");
        }
        json.append("]\n");
        json.append("}\n");
        return json.toString();
    }

    public String commandsText() {
        StringBuilder text = new StringBuilder();
        text.append("# DocuPodcast Studio — comandos de render simple\n");
        text.append("# Contrato: docupodcast-simple-video-render-v1\n");
        text.append("# Modo: ").append(renderModeLabel()).append("\n");
        text.append("# Salida: ").append(outputFileName).append("\n");
        if (!warnings.isEmpty()) {
            text.append("# Advertencias que deben resolverse antes del render MP4:\n");
            for (String warning : warnings) {
                text.append("# - ").append(warning).append("\n");
            }
        }
        text.append("\n");
        for (String command : renderCommands) {
            text.append(command).append("\n");
        }
        return text.toString();
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
