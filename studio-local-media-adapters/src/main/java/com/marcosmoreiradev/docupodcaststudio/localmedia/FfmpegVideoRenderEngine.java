package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic timeline translator. FFmpeg syntax is intentionally confined to this adapter. */
public final class FfmpegVideoRenderEngine implements VideoRenderEngine {
    public static final EngineId ID = new EngineId("ffmpeg");
    private final EngineConfiguration configuration;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    public FfmpegVideoRenderEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.VIDEO_RENDERING, "Render de video local", "local", "external-process",
                Set.of(EngineFeature.HARDWARE_ACCELERATION), false);
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("executable", "Renderizador local", "Ruta o comando del ejecutable de render.",
                        ConfigurationFieldType.FILE, true, "ffmpeg")));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        try {
            var result = executor.run(List.of(executable(), "-version"), null,
                    ExecutionContext.defaults("video-render-readiness"));
            return result.exitCode() == 0
                    ? EngineReadiness.ready(ID, "Renderizador local disponible.")
                    : EngineReadiness.unavailable(ID, "El renderizador no respondió correctamente.", "Configura un ejecutable válido.");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "El renderizador local no está disponible.", "Prepara el runtime de video.");
        }
    }

    @Override public VideoRenderResult render(VideoRenderRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("video-render") : context;
        VideoTimelinePlan plan = request.effectivePlan();
        Path output = request.outputFile().toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent == null) throw new IOException("La salida de video necesita un directorio.");
        Files.createDirectories(parent);
        Path work = Files.createTempDirectory(parent, ".video-render-staging-").toAbsolutePath().normalize();
        ArrayList<Path> clips = new ArrayList<>();
        try {
            int index = 0;
            for (VideoTimelineItem item : plan.items()) {
                current.cancellation().throwIfCancellationRequested();
                Path clip = work.resolve(String.format("clip-%04d.mp4", ++index));
                renderItem(item, plan, clip, work, current);
                clips.add(clip);
                current.progress().report("RENDERING", index / (double) (plan.items().size() + 2),
                        "Renderizando unidad " + index + " de " + plan.items().size() + ".");
            }
            Path concat = work.resolve("clips.txt");
            StringBuilder lines = new StringBuilder();
            for (Path clip : clips) lines.append("file '").append(clip.toString().replace("'", "'\\''")).append("'\n");
            Files.writeString(concat, lines, StandardCharsets.UTF_8);
            Path joined = work.resolve("joined.mp4");
            runRequired(List.of(executable(), "-y", "-f", "concat", "-safe", "0", "-i",
                    concat.toString(), "-c", "copy", joined.toString()), work, current, "unir la línea de tiempo");

            Path staged = plan.overlays().isEmpty() ? joined : mixOverlays(joined, plan, work, current);
            current.cancellation().throwIfCancellationRequested();
            promote(staged, output);
            current.progress().report("COMPLETED", 1.0, "Video renderizado.");
            return new VideoRenderResult(output, plan.durationSeconds(), Map.of(
                    "engineId", ID.value(), "encodingPreference", plan.encodingPreference().name()));
        } finally {
            deleteStaging(work, parent);
        }
    }

    private void renderItem(VideoTimelineItem item, VideoTimelinePlan plan, Path clip, Path work,
                            ExecutionContext context) throws IOException, InterruptedException {
        ArrayList<String> command = new ArrayList<>(List.of(executable(), "-y"));
        for (TimelineVisualSource visual : item.visuals()) addVisualInput(command, visual, item.durationSeconds());
        int audioIndex = item.visuals().size();
        if (item.narrationAudio() != null) {
            command.addAll(List.of("-i", item.narrationAudio().toString()));
        } else {
            command.addAll(List.of("-f", "lavfi", "-t", format(item.durationSeconds()), "-i",
                    "anullsrc=channel_layout=stereo:sample_rate=44100"));
        }

        command.addAll(List.of("-filter_complex", visualFilter(item, plan),
                "-map", "[vout]", "-map", audioIndex + ":a",
                "-r", Integer.toString(plan.framesPerSecond()), "-c:v", codec(plan.encodingPreference()),
                "-c:a", "aac", "-t", format(item.durationSeconds()), "-shortest", clip.toString()));
        runRequired(command, work, context, "renderizar " + item.id());
    }

    private void addVisualInput(List<String> command, TimelineVisualSource visual, double itemDuration) {
        if (visual.kind() == TimelineVisualKind.STILL_IMAGE) {
            command.addAll(List.of("-loop", "1", "-t", format(itemDuration), "-i", visual.file().toString()));
        } else {
            command.addAll(List.of("-ss", format(visual.sourceStartSeconds()), "-t",
                    format(Math.min(itemDuration, visual.durationSeconds())), "-i", visual.file().toString()));
        }
    }

    private String visualFilter(VideoTimelineItem item, VideoTimelinePlan plan) {
        String size = "scale=" + plan.width() + ":" + plan.height()
                + ":force_original_aspect_ratio=decrease,pad=" + plan.width() + ":" + plan.height()
                + ":(ow-iw)/2:(oh-ih)/2,format=yuv420p";
        StringBuilder filter = new StringBuilder("[0:v]").append(size)
                .append(fades(item.visuals().getFirst(), item.durationSeconds())).append("[base0]");
        String current = "base0";
        for (int index = 1; index < item.visuals().size(); index++) {
            TimelineVisualSource overlay = item.visuals().get(index);
            String overlayName = "overlay" + index;
            String outputName = "base" + index;
            filter.append(";").append("[").append(index).append(":v]").append(size)
                    .append(fades(overlay, item.durationSeconds())).append("[").append(overlayName).append("]")
                    .append(";[").append(current).append("][").append(overlayName).append("]overlay=0:0[")
                    .append(outputName).append("]");
            current = outputName;
        }
        filter.append(";[").append(current).append("]null[vout]");
        return filter.toString();
    }

    private Path mixOverlays(Path joined, VideoTimelinePlan plan, Path work, ExecutionContext context)
            throws IOException, InterruptedException {
        ArrayList<String> command = new ArrayList<>(List.of(executable(), "-y", "-i", joined.toString()));
        for (TimelineAudioTrack overlay : plan.overlays()) command.addAll(List.of("-i", overlay.file().toString()));
        StringBuilder filters = new StringBuilder();
        ArrayList<String> mixInputs = new ArrayList<>(List.of("[0:a]"));
        int input = 1;
        for (TimelineAudioTrack overlay : plan.overlays()) {
            double length = Math.max(0.05, overlay.sourceEndSeconds() - overlay.sourceStartSeconds());
            String label = "audio" + input;
            if (!filters.isEmpty()) filters.append(';');
            filters.append('[').append(input).append(":a]atrim=start=").append(format(overlay.sourceStartSeconds()))
                    .append(":end=").append(format(overlay.sourceEndSeconds()))
                    .append(",asetpts=PTS-STARTPTS,volume=").append(format(overlay.volume()));
            if (overlay.fadeInSeconds() > 0) filters.append(",afade=t=in:st=0:d=").append(format(overlay.fadeInSeconds()));
            if (overlay.fadeOutSeconds() > 0) filters.append(",afade=t=out:st=")
                    .append(format(Math.max(0, length - overlay.fadeOutSeconds())))
                    .append(":d=").append(format(overlay.fadeOutSeconds()));
            long delay = Math.round(overlay.timelineStartSeconds() * 1000.0);
            filters.append(",adelay=").append(delay).append('|').append(delay).append('[').append(label).append(']');
            mixInputs.add("[" + label + "]");
            input++;
        }
        filters.append(';');
        mixInputs.forEach(filters::append);
        filters.append("amix=inputs=").append(mixInputs.size()).append(":duration=first:dropout_transition=0[aout]");
        Path mixed = work.resolve("mixed.mp4");
        command.addAll(List.of("-filter_complex", filters.toString(), "-map", "0:v", "-map", "[aout]",
                "-c:v", "copy", "-c:a", "aac", mixed.toString()));
        runRequired(command, work, context, "mezclar audio superpuesto");
        return mixed;
    }

    private void runRequired(List<String> command, Path work, ExecutionContext context, String operation)
            throws IOException, InterruptedException {
        var result = executor.run(command, work, context);
        if (result.exitCode() != 0) throw new IOException("El renderizador falló al " + operation + ": " + result.output());
    }

    private String executable() {
        String value = configuration.value("executable");
        return value.isBlank() ? "ffmpeg" : value;
    }

    private static String codec(VideoEncodingPreference preference) {
        return preference == VideoEncodingPreference.HARDWARE_PREFERRED ? "h264_nvenc" : "libx264";
    }

    private static String fades(TimelineVisualSource visual, double duration) {
        StringBuilder result = new StringBuilder();
        if (visual.fadeInSeconds() > 0) result.append(",fade=t=in:st=0:d=").append(format(visual.fadeInSeconds()));
        if (visual.fadeOutSeconds() > 0) result.append(",fade=t=out:st=")
                .append(format(Math.max(0, duration - visual.fadeOutSeconds())))
                .append(":d=").append(format(visual.fadeOutSeconds()));
        return result.toString();
    }

    private static void promote(Path staged, Path output) throws IOException {
        try {
            Files.move(staged, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(staged, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static void deleteStaging(Path root, Path expectedParent) {
        if (root == null || expectedParent == null || !root.startsWith(expectedParent)
                || !root.getFileName().toString().startsWith(".video-render-staging-") || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
