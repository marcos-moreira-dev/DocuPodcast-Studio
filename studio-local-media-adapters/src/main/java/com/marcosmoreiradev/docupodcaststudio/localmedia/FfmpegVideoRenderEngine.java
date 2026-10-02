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
import java.util.function.Consumer;

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
                // Do not encode every narration unit as an independent AAC stream.
                // AAC adds encoder priming/padding at every file boundary. Concatenating
                // hundreds of those files makes the spoken timeline progressively lag
                // behind the corresponding visual frame even though every item is bound
                // to the correct WAV. Matroska + PCM keeps intermediate boundaries
                // sample-accurate; AAC is encoded once, for the continuous final stream.
                Path clip = work.resolve(intermediateClipFileName(++index));
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
            String joinedFilters = "[0:v]trim=duration=" + format(plan.durationSeconds())
                    + ",setpts=PTS-STARTPTS[vout];[0:a]asetpts=N/SR/TB,apad,atrim=duration="
                    + format(plan.durationSeconds()) + "[aout]";
            current.progress().report("ASSEMBLING", 0.0,
                    "Uniendo y comprimiendo " + clips.size() + " unidades en el MP4 final (duración "
                            + humanDuration(plan.durationSeconds()) + ").");
            runRequired(List.of(executable(), "-y", "-f", "concat", "-safe", "0", "-i",
                    concat.toString(), "-filter_complex", joinedFilters,
                    "-map", "[vout]", "-map", "[aout]", "-r",
                    Integer.toString(plan.framesPerSecond()), "-c:v",
                    codec(plan.encodingPreference()), "-c:a", "aac", "-b:a", "160k",
                    "-ac", "1", "-t",
                    format(plan.durationSeconds()), "-progress", "pipe:1", "-nostats",
                    joined.toString()),
                    work, current, "unir la línea de tiempo",
                    new FfmpegProgressReporter(current, plan.durationSeconds()));

            if (!plan.overlays().isEmpty()) {
                current.progress().report("MIXING", 1.0,
                        "Mezclando " + plan.overlays().size() + " pista(s) adicional(es).");
            }
            Path staged = plan.overlays().isEmpty() ? joined : mixOverlays(joined, plan, work, current);
            current.progress().report("VERIFYING", 1.0,
                    "Verificando duración, audio y contenedor del MP4 final.");
            MediaDurations durations = probeDurations(staged, work, current);
            validateDurations(plan, durations);
            current.cancellation().throwIfCancellationRequested();
            promote(staged, output);
            current.progress().report("COMPLETED", 1.0, "Video renderizado.");
            return new VideoRenderResult(output, durations.containerSeconds(), Map.of(
                    "engineId", ID.value(),
                    "encodingPreference", plan.encodingPreference().name(),
                    "plannedDurationSeconds", format(plan.durationSeconds()),
                    "videoStreamDurationSeconds", format(durations.videoSeconds()),
                    "audioStreamDurationSeconds", format(durations.audioSeconds()),
                    "containerDurationSeconds", format(durations.containerSeconds())));
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

        String filters = visualFilter(item, plan) + ";[" + audioIndex
                + ":a]" + normalizedTimelineAudioFilter(item.durationSeconds()) + "[aout]";
        command.addAll(List.of("-filter_complex", filters,
                "-map", "[vout]", "-map", "[aout]",
                "-r", Integer.toString(plan.framesPerSecond()), "-c:v", codec(plan.encodingPreference()),
                // Keep narration lossless and free of codec delay between units. The
                // assembly pass below is the sole AAC boundary in the normal export.
                "-c:a", intermediateAudioCodec(), "-ar", "44100", "-ac", "1",
                "-t", format(item.durationSeconds()), clip.toString()));
        runRequired(command, work, context, "renderizar " + item.id());
    }

    static String normalizedTimelineAudioFilter(double durationSeconds) {
        return "aresample=44100:async=0:first_pts=0,"
                + "aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=mono,"
                + "apad,atrim=duration=" + format(durationSeconds)
                + ",asetpts=PTS-STARTPTS";
    }

    static String intermediateAudioCodec() {
        return "pcm_s16le";
    }

    static String intermediateClipFileName(int index) {
        if (index < 1) throw new IllegalArgumentException("index must be >= 1");
        return String.format("clip-%04d.mkv", index);
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
        filter.append(";[").append(current).append("]trim=duration=")
                .append(format(item.durationSeconds())).append(",setpts=PTS-STARTPTS[vout]");
        return filter.toString();
    }

    private MediaDurations probeDurations(Path media, Path work, ExecutionContext context)
            throws IOException, InterruptedException {
        var result = executor.run(List.of(ffprobeExecutable(), "-v", "error",
                "-show_entries", "stream=codec_type,duration:format=duration",
                "-of", "default=noprint_wrappers=1", media.toString()), work, context);
        if (result.exitCode() != 0) {
            throw new IOException("FFprobe no pudo validar el video final: " + result.output());
        }
        double video = Double.NaN, audio = Double.NaN, container = Double.NaN;
        String currentType = "";
        for (String raw : result.output().split("\\R")) {
            String line = raw.strip();
            if (line.startsWith("codec_type=")) {
                currentType = line.substring("codec_type=".length());
            } else if (line.startsWith("duration=")) {
                double value = number(line.substring("duration=".length()));
                if ("video".equals(currentType) && !Double.isFinite(video)) video = value;
                else if ("audio".equals(currentType) && !Double.isFinite(audio)) audio = value;
                else container = value;
            }
        }
        if (!Double.isFinite(video) || !Double.isFinite(audio) || !Double.isFinite(container)) {
            throw new IOException("FFprobe no devolvio duraciones fisicas completas para el video final: "
                    + result.output());
        }
        return new MediaDurations(video, audio, container);
    }

    static void validateDurations(VideoTimelinePlan plan, MediaDurations durations)
            throws IOException {
        double tolerance = 1.0 / plan.framesPerSecond() + 0.010;
        double streamDifference = Math.abs(durations.videoSeconds() - durations.audioSeconds());
        if (streamDifference > tolerance) {
            throw new IOException("El video final no supera el gate temporal: video="
                    + format(durations.videoSeconds()) + " s, audio="
                    + format(durations.audioSeconds()) + " s, diferencia="
                    + format(streamDifference) + " s, tolerancia=" + format(tolerance) + " s.");
        }
        double planDifference = Math.max(Math.abs(durations.videoSeconds() - plan.durationSeconds()),
                Math.abs(durations.audioSeconds() - plan.durationSeconds()));
        if (planDifference > tolerance) {
            throw new IOException("El video final no coincide con el timeline planificado: plan="
                    + format(plan.durationSeconds()) + " s, video="
                    + format(durations.videoSeconds()) + " s, audio="
                    + format(durations.audioSeconds()) + " s.");
        }
    }

    private String ffprobeExecutable() {
        Path ffmpeg = Path.of(executable());
        Path fileName = ffmpeg.getFileName();
        if (fileName != null && fileName.toString().toLowerCase(java.util.Locale.ROOT)
                .startsWith("ffmpeg") && ffmpeg.getParent() != null) {
            String suffix = fileName.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".exe")
                    ? ".exe" : "";
            Path sibling = ffmpeg.resolveSibling("ffprobe" + suffix);
            if (Files.isRegularFile(sibling)) return sibling.toString();
        }
        return "ffprobe";
    }

    private static double number(String value) {
        try { return Double.parseDouble(value.strip()); }
        catch (RuntimeException ignored) { return Double.NaN; }
    }

    record MediaDurations(double videoSeconds, double audioSeconds,
                          double containerSeconds) { }

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
                "-c:v", "copy", "-c:a", "aac", "-b:a", "160k", "-ac", "1",
                mixed.toString()));
        runRequired(command, work, context, "mezclar audio superpuesto");
        return mixed;
    }

    private void runRequired(List<String> command, Path work, ExecutionContext context, String operation)
            throws IOException, InterruptedException {
        runRequired(command, work, context, operation, null);
    }

    private void runRequired(List<String> command, Path work, ExecutionContext context,
                             String operation, Consumer<String> outputLineConsumer)
            throws IOException, InterruptedException {
        var result = executor.run(command, work, context, outputLineConsumer);
        if (result.exitCode() != 0) throw new IOException("El renderizador falló al " + operation + ": " + result.output());
    }

    static final class FfmpegProgressReporter implements Consumer<String> {
        private final ExecutionContext context;
        private final double totalSeconds;
        private final long startedNanos = System.nanoTime();
        private double encodedSeconds;
        private double speed;
        private String frame = "";
        private String fps = "";

        FfmpegProgressReporter(ExecutionContext context, double totalSeconds) {
            this.context = context;
            this.totalSeconds = Math.max(0.001, totalSeconds);
        }

        @Override
        public void accept(String rawLine) {
            if (rawLine == null) return;
            int separator = rawLine.indexOf('=');
            if (separator <= 0) return;
            String key = rawLine.substring(0, separator).strip();
            String value = rawLine.substring(separator + 1).strip();
            switch (key) {
                case "out_time_us", "out_time_ms" -> {
                    double parsed = number(value);
                    if (Double.isFinite(parsed)) encodedSeconds = parsed / 1_000_000.0;
                }
                case "speed" -> {
                    double parsed = number(value.replace("x", ""));
                    if (Double.isFinite(parsed)) speed = parsed;
                }
                case "frame" -> frame = value;
                case "fps" -> fps = value;
                case "progress" -> report("end".equalsIgnoreCase(value));
                default -> { }
            }
        }

        private void report(boolean finished) {
            double encoded = finished ? totalSeconds
                    : Math.max(0.0, Math.min(totalSeconds, encodedSeconds));
            double ratio = Math.max(0.0, Math.min(1.0, encoded / totalSeconds));
            long elapsed = Math.max(0L,
                    (System.nanoTime() - startedNanos) / 1_000_000_000L);
            long remaining = speed > 0.0
                    ? Math.max(0L, Math.round((totalSeconds - encoded) / speed)) : -1L;
            StringBuilder message = new StringBuilder("Codificando MP4 final: ")
                    .append(humanDuration(encoded)).append(" de ")
                    .append(humanDuration(totalSeconds)).append(" · ")
                    .append(Math.round(ratio * 100.0)).append(" %")
                    .append(" · transcurrido ").append(humanDuration(elapsed));
            if (remaining >= 0) message.append(" · faltan aprox. ")
                    .append(humanDuration(remaining));
            if (speed > 0.0) message.append(" · velocidad ")
                    .append(String.format(java.util.Locale.ROOT, "%.2fx", speed));
            if (!frame.isBlank()) message.append(" · frame ").append(frame);
            if (!fps.isBlank()) message.append(" · ").append(fps).append(" fps");
            context.progress().report("ASSEMBLING", ratio, message.toString());
        }
    }

    private String executable() {
        String value = configuration.value("executable");
        return value.isBlank() ? "ffmpeg" : value;
    }

    static String codec(VideoEncodingPreference preference) {
        return preference.exactKind().ffmpegCodec();
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

    private static String humanDuration(double seconds) {
        long total = Math.max(0L, Math.round(seconds));
        long hours = total / 3600;
        long minutes = total % 3600 / 60;
        long remainingSeconds = total % 60;
        return hours > 0
                ? "%d h %02d min %02d s".formatted(hours, minutes, remainingSeconds)
                : "%d min %02d s".formatted(minutes, remainingSeconds);
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
