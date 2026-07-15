package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the auditable FFmpeg render contract for a simple DocuPodcast video package without running FFmpeg. */
public final class BuildVideoRenderCommandPlanUseCase {
    public static final String OUTPUT_FILE_NAME = "video-simple.mp4";

    public VideoRenderCommandPlan build(
            SimpleVideoPlan plan,
            SimpleVideoExportSettings settings,
            FfmpegToolDiscovery ffmpeg
    ) {
        SimpleVideoExportSettings effectiveSettings = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        List<String> warnings = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        if (plan == null || plan.frameCount() == 0) {
            warnings.add("No hay frames de video para renderizar.");
        }
        if (plan != null && plan.framesMissingAudio() > 0) {
            warnings.add("Hay frames hablados sin audio; el render MP4 requiere audio listo para cada unidad hablada.");
        }
        if (plan != null && plan.framesMissingImage() > 0) {
            warnings.add("Hay frames sin imagen; el video renderizable solo debe incluir unidades visuales asignadas.");
        }
        if (plan != null && plan.framesSilentVisual() > 0) {
            commands.add("REM Hay frames visuales silenciosos; FFmpeg generará silencio sintético con la duración configurada.");
        }
        if (ffmpeg == null || !ffmpeg.ready()) {
            warnings.add("FFmpeg no está disponible; el paquete queda auditable y renderizable cuando se configure tools/ffmpeg o una ruta externa.");
        }
        if (effectiveSettings.encoderPolicy().hardwareAccelerated()) {
            warnings.add("Encoder por hardware solicitado (" + effectiveSettings.encoderPolicy().name()
                    + "); el binario FFmpeg debe soportarlo. Si no, usar CPU_X264 o AUTO.");
        }
        commands.add("REM Contrato de render: docupodcast-simple-video-render-v1");
        commands.add("REM La interfaz debe bloquear lectura normal y nuevas exportaciones durante el render.");
        commands.add("REM Cancelación segura: detener FFmpeg conserva el paquete y permite reintentar.");
        if (plan != null) {
            int index = 1;
            for (SimpleVideoFrame frame : plan.frames()) {
                String outputClip = "frames/frame-" + String.format(Locale.ROOT, "%03d", index++) + ".mp4";
                String codec = effectiveSettings.encoderPolicy().ffmpegCodecArgument();
                if (frame.imageAssigned() && frame.audioReady()) {
                    commands.add(ffmpegVariable() + " -y -loop 1 -t " + format(frame.frameDurationSeconds())
                            + " -i \"" + frame.imageRelativePath() + "\" -i \"" + frame.audioRelativePath() + "\" "
                            + "-vf \"" + effectiveSettings.resolution().ffmpegScaleExpression()
                            + charLabelFilters(frame) + ",format=yuv420p\" "
                            + "-r " + effectiveSettings.framesPerSecond()
                            + (codec.isBlank() ? "" : " " + codec)
                            + " -shortest \"" + outputClip + "\"");
                } else if (frame.imageAssigned() && frame.silentVisual()) {
                    commands.add(ffmpegVariable() + " -y -loop 1 -t " + format(frame.frameDurationSeconds())
                            + " -i \"" + frame.imageRelativePath() + "\" "
                            + "-f lavfi -t " + format(frame.frameDurationSeconds())
                            + " -i anullsrc=channel_layout=stereo:sample_rate=44100 "
                            + "-vf \"" + effectiveSettings.resolution().ffmpegScaleExpression()
                            + charLabelFilters(frame) + ",format=yuv420p\" "
                            + "-r " + effectiveSettings.framesPerSecond()
                            + (codec.isBlank() ? "" : " " + codec)
                            + " -shortest \"" + outputClip + "\"");
                } else {
                    commands.add("REM " + frame.id() + " requiere revisión antes de render: imagen="
                            + frame.imageLabel() + ", audio=" + frame.audioLabel());
                }
            }
            commands.add("REM Crear frame-clips.txt con los clips frame-###.mp4 en orden y unir:");
            commands.add(ffmpegVariable() + " -y -f concat -safe 0 -i frame-clips.txt -c copy \"" + OUTPUT_FILE_NAME + "\"");
        }
        return new VideoRenderCommandPlan(plan, effectiveSettings, ffmpeg, OUTPUT_FILE_NAME, commands, warnings, Instant.now());
    }

    private static String charLabelFilters(SimpleVideoFrame frame) {
        if (frame == null || frame.characterLabels() == null || frame.characterLabels().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SimpleVideoFrame.CharacterLabel label : frame.characterLabels()) {
            String escaped = label.characterName().replace("'", "'\\\\\\''").replace(":", "\\\\:");
            sb.append(",drawtext=text='").append(escaped)
                    .append(":x=").append((int) label.x())
                    .append(":y=").append((int) label.y())
                    .append(":fontsize=11:fontcolor=white:box=1:boxcolor=black@0.7");
        }
        return sb.toString();
    }

    private static String ffmpegVariable() {
        return "%FFMPEG%";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
