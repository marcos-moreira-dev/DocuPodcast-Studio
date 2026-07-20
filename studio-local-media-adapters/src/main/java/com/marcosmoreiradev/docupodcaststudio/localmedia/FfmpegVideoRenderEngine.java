package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FfmpegVideoRenderEngine implements VideoRenderEngine {
    public static final EngineId ID = new EngineId("ffmpeg");
    private final EngineConfiguration configuration;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    public FfmpegVideoRenderEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.VIDEO_RENDERING, "Video local", "local", "external-process",
                Set.of(EngineFeature.HARDWARE_ACCELERATION), false);
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("executable", "FFmpeg", "Ruta o comando del ejecutable FFmpeg.",
                        ConfigurationFieldType.FILE, true, "ffmpeg")));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        try {
            var result = executor.run(List.of(executable(), "-version"), null,
                    ExecutionContext.defaults("ffmpeg-readiness"));
            return result.exitCode() == 0
                    ? EngineReadiness.ready(ID, "FFmpeg disponible.")
                    : EngineReadiness.unavailable(ID, "FFmpeg no respondió correctamente.", "Configura un ejecutable FFmpeg válido.");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "FFmpeg no está disponible.", "Prepara Video local o configura su ejecutable.");
        }
    }

    @Override public VideoRenderResult render(VideoRenderRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("video-render") : context;
        Path output = request.outputFile().toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        Path work = Files.createTempDirectory(output.getParent(), "video-render-");
        ArrayList<Path> clips = new ArrayList<>();
        try {
            int index = 0;
            for (VideoTimelineFrame frame : request.timeline()) {
                current.cancellation().throwIfCancellationRequested();
                Path clip = work.resolve(String.format("clip-%04d.mp4", ++index));
                ArrayList<String> command = new ArrayList<>(List.of(executable(), "-y", "-loop", "1",
                        "-t", format(frame.durationSeconds()), "-i", frame.image().toString()));
                if (frame.audio() != null) {
                    command.addAll(List.of("-i", frame.audio().toString()));
                } else {
                    command.addAll(List.of("-f", "lavfi", "-t", format(frame.durationSeconds()), "-i",
                            "anullsrc=channel_layout=stereo:sample_rate=44100"));
                }
                command.addAll(List.of("-vf", "scale=" + request.width() + ":" + request.height()
                                + ":force_original_aspect_ratio=decrease,pad=" + request.width() + ":" + request.height()
                                + ":(ow-iw)/2:(oh-ih)/2,format=yuv420p",
                        "-r", Integer.toString(request.framesPerSecond()), "-c:v", "libx264", "-shortest", clip.toString()));
                var result = executor.run(command, work, current);
                if (result.exitCode() != 0) throw new IOException("FFmpeg falló al crear clip " + index + ": " + result.output());
                clips.add(clip);
                current.progress().report("RENDERING", index / (double) (request.timeline().size() + 1),
                        "Renderizando clip " + index + " de " + request.timeline().size() + ".");
            }
            Path concat = work.resolve("clips.txt");
            StringBuilder lines = new StringBuilder();
            for (Path clip : clips) lines.append("file '").append(clip.toString().replace("'", "'\\''")).append("'\n");
            Files.writeString(concat, lines, StandardCharsets.UTF_8);
            var result = executor.run(List.of(executable(), "-y", "-f", "concat", "-safe", "0", "-i",
                    concat.toString(), "-c", "copy", output.toString()), work, current);
            if (result.exitCode() != 0) throw new IOException("FFmpeg falló al unir los clips: " + result.output());
            double duration = request.timeline().stream().mapToDouble(VideoTimelineFrame::durationSeconds).sum();
            current.progress().report("COMPLETED", 1.0, "Video renderizado.");
            return new VideoRenderResult(output, duration, Map.of("engineId", ID.value()));
        } finally {
            deleteTree(work);
        }
    }

    private String executable() {
        String value = configuration.value("executable");
        return value.isBlank() ? "ffmpeg" : value;
    }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.3f", value); }
    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
