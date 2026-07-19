package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.errors.ExternalProcessCancelledException;
import com.marcosmoreiradev.docupodcaststudio.application.errors.ExternalProcessFailedException;
import com.marcosmoreiradev.docupodcaststudio.application.errors.ExternalProcessTimeoutException;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Mode-independent MP4 renderer over an already prepared video plan. */
public final class RenderFinalVideoPlanUseCase {
    private static final Duration FRAME_TIMEOUT = Duration.ofMinutes(20);
    private static final Duration CONCAT_TIMEOUT = Duration.ofMinutes(20);
    private static final int OUTPUT_AUDIO_SAMPLE_RATE = 48_000;
    private static final String OUTPUT_AUDIO_LAYOUT = "stereo";

    private final FfmpegRuntimeProbeUseCase probe;
    private final EmbeddedFfmpegLocator locator;
    private final ExternalProcessRunner runner;
    private final VideoAudioOverlayFfmpegCommandFactory audioOverlayCommandFactory = new VideoAudioOverlayFfmpegCommandFactory();

    public RenderFinalVideoPlanUseCase() {
        this(new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("RenderFinalVideoPlanUseCase")),
                new EmbeddedFfmpegLocator(),
                ExternalProcessRunner.unavailable("RenderFinalVideoPlanUseCase"));
    }

    public RenderFinalVideoPlanUseCase(FfmpegRuntimeProbeUseCase probe,
                                       EmbeddedFfmpegLocator locator) {
        this(probe, locator, ExternalProcessRunner.unavailable("RenderFinalVideoPlanUseCase"));
    }

    public RenderFinalVideoPlanUseCase(FfmpegRuntimeProbeUseCase probe,
                                       EmbeddedFfmpegLocator locator,
                                       ExternalProcessRunner runner) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.locator = Objects.requireNonNull(locator, "locator");
        this.runner = runner == null ? ExternalProcessRunner.unavailable("RenderFinalVideoPlanUseCase") : runner;
    }

    public FinalVideoExportResult render(FinalVideoRenderRequest request) throws IOException {
        return render(request, ignored -> { }, () -> false);
    }

    public FinalVideoExportResult render(FinalVideoRenderRequest request,
                                         Consumer<VideoRenderProgress> progress) throws IOException {
        return render(request, progress, () -> false);
    }

    public FinalVideoExportResult render(FinalVideoRenderRequest request,
                                         Consumer<VideoRenderProgress> progress,
                                         BooleanSupplier cancellationRequested) throws IOException {
        Objects.requireNonNull(request, "request");
        Consumer<VideoRenderProgress> safeProgress = progress == null ? ignored -> { } : progress;
        BooleanSupplier cancel = cancellationRequested == null ? () -> false : cancellationRequested;
        return renderPlan(request, request.plan(), safeProgress, cancel);
    }

    private FinalVideoExportResult renderPlan(FinalVideoRenderRequest request,
                                              SimpleVideoPlan plan,
                                              Consumer<VideoRenderProgress> safeProgress,
                                              BooleanSupplier cancel) throws IOException {
        safeProgress.accept(VideoRenderProgress.preparing(plan.frameCount()));
        if (!plan.exportableAsRenderedVideo()) {
            throw new IOException("No se puede renderizar MP4 final: faltan imágenes o audio en la secuencia visual. "
                    + "Frames sin imagen: " + plan.framesMissingImage()
                    + "; frames hablados sin audio: " + plan.framesMissingAudio() + ".");
        }
        FfmpegToolDiscovery discovery = locator.locate(request.applicationRoot(), request.configuredFfmpeg());
        FfmpegRuntimeReport runtime = probe.inspect(discovery);
        if (!runtime.readyForFinalVideo()) {
            throw new IOException("Video local no está listo para exportar MP4 final: "
                    + String.join(" · ", runtime.warnings())
                    + ". Prepara Video local en Configuración.");
        }
        if (!runtime.supportsPolicy(request.settings().encoderPolicy())) {
            throw new IOException("El encoder seleccionado no está disponible en Video local. "
                    + "Usa Automático o CPU - libx264 en Configuración.");
        }
        Path target = ensureMp4Target(request.targetMp4());
        Files.createDirectories(target.getParent());
        Path workRoot = request.projectDirectory().resolve("exports/video-render-work").toAbsolutePath().normalize();
        Files.createDirectories(workRoot);
        Path work = Files.createTempDirectory(workRoot, "export-");
        Set<Path> generatedMapDirectories = generatedPlanDirectories(plan, request.projectDirectory());
        try {
        Path clips = work.resolve("clips");
        Files.createDirectories(clips);
        ArrayList<Path> clipFiles = new ArrayList<>();
        int index = 1;
        for (SimpleVideoFrame frame : plan.frames()) {
            throwIfCancelled(cancel, index - 1, plan.frameCount(), safeProgress);
            int currentIndex = index;
            safeProgress.accept(VideoRenderProgress.rendering(currentIndex - 1, plan.frameCount(), "Codificando " + frame.id()));
            Path outputClip = clips.resolve("unit-" + String.format(Locale.ROOT, "%03d", index++) + ".mp4");
            renderFrame(runner, runtime.ffmpegExecutable(), request.projectDirectory(), request.settings(), frame, outputClip, cancel,
                    line -> safeProgress.accept(VideoRenderProgress.rendering(currentIndex - 1, plan.frameCount(), ffmpegDetail(frame.id(), line))));
            clipFiles.add(outputClip);
            safeProgress.accept(VideoRenderProgress.rendering(currentIndex, plan.frameCount(), "Unidad codificada: " + frame.id()));
        }
        throwIfCancelled(cancel, plan.frameCount(), plan.frameCount(), safeProgress);
        safeProgress.accept(VideoRenderProgress.verifying(plan.frameCount(), "Finalizando secuencia"));
        Path concat = work.resolve("unit-clips.txt");
        Files.writeString(concat, concatFile(clipFiles), StandardCharsets.UTF_8);
        VideoAudioOverlayPlan audioOverlayPlan = request.audioOverlayPlan();
        Path narrationBase = audioOverlayPlan.empty() ? target : work.resolve("narration-base.mp4");
        run(runner, CONCAT_TIMEOUT, concatCommand(runtime.ffmpegExecutable(), concat, narrationBase), cancel,
                line -> safeProgress.accept(VideoRenderProgress.rendering(plan.frameCount(), plan.frameCount(), ffmpegDetail("Unión final", line))));
        if (!audioOverlayPlan.empty()) {
            safeProgress.accept(VideoRenderProgress.mixing(plan.frameCount(), "Mezclando pistas de audio"));
            run(runner, CONCAT_TIMEOUT,
                    audioOverlayCommandFactory.build(runtime.ffmpegExecutable(), narrationBase, target,
                            audioOverlayPlan, plan.totalDurationSeconds()), cancel,
                    line -> safeProgress.accept(VideoRenderProgress.mixing(plan.frameCount(), ffmpegDetail("Mezcla multimedia", line))));
        }
        safeProgress.accept(VideoRenderProgress.verifying(plan.frameCount(), target.toString()));
        if (!Files.isRegularFile(target) || Files.size(target) <= 0L) {
            throw new IOException("Video local terminó sin crear un MP4 final válido: " + target);
        }
        safeProgress.accept(VideoRenderProgress.completed(plan.frameCount(), target.toString()));
        FinalVideoExportResult result = new FinalVideoExportResult(target, workRoot, request.settings().resolution(), plan.frameCount(),
                plan.totalDurationSeconds(), Files.size(target), runtime.warnings());
        return result;
        } finally {
            deleteRecursively(work);
            for (Path directory : generatedMapDirectories) deleteRecursively(directory);
        }
    }

    private static Set<Path> generatedPlanDirectories(SimpleVideoPlan plan, Path projectDirectory) {
        Path projectRoot = projectDirectory.toAbsolutePath().normalize();
        LinkedHashSet<Path> directories = new LinkedHashSet<>();
        for (SimpleVideoFrame frame : plan.frames()) {
            if (frame.imageRelativePath() == null || frame.imageRelativePath().isBlank()) continue;
            Path parent = projectRoot.resolve(frame.imageRelativePath()).normalize().getParent();
            if (parent != null && parent.startsWith(projectRoot.resolve("exports/video-render-work"))) {
                String name = parent.getFileName().toString();
                if (name.startsWith("theatre-map-") || name.startsWith("theatre-clean-placeholder-")) {
                    directories.add(parent);
                }
            }
        }
        return directories;
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private static Path ensureMp4Target(Path target) {
        String name = target.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
            return target;
        }
        return target.resolveSibling(name + ".mp4");
    }

    private static void renderFrame(ExternalProcessRunner runner,
                                    Path ffmpeg,
                                    Path projectDirectory,
                                    SimpleVideoExportSettings settings,
                                    SimpleVideoFrame frame,
                                    Path outputClip,
                                    BooleanSupplier cancellationRequested,
                                    Consumer<String> outputLine) throws IOException {
        Files.createDirectories(outputClip.getParent());
        if (frame.visualParts().size() > 1
                || frame.visualParts().stream().anyMatch(SimpleVideoFrame.VisualPart::videoClip)) {
            renderFrameWithVisualParts(runner, ffmpeg, projectDirectory, settings, frame, outputClip,
                    cancellationRequested, outputLine);
            return;
        }
        Path image = requireProjectFile(projectDirectory, frame.imageRelativePath(), "imagen", frame.id());
        ArrayList<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-loop");
        command.add("1");
        command.add("-t");
        command.add(format(frame.frameDurationSeconds()));
        command.add("-i");
        command.add(image.toString());
        if (frame.silentVisual()) {
            command.add("-f");
            command.add("lavfi");
            command.add("-t");
            command.add(format(frame.frameDurationSeconds()));
            command.add("-i");
            command.add(silentAudioSource());
        } else {
            Path audio = requireProjectFile(projectDirectory, frame.audioRelativePath(), "audio", frame.id());
            command.add("-i");
            command.add(audio.toString());
        }
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("1:a:0");
        command.add("-vf");
        command.add(normalizedVideoFilter(settings));
        command.add("-r");
        command.add(Integer.toString(settings.framesPerSecond()));
        command.add("-fps_mode");
        command.add("cfr");
        String codec = settings.encoderPolicy().ffmpegEncoder();
        if (!codec.isBlank()) {
            command.add("-c:v");
            command.add(codec);
        }
        appendExactAudioDuration(command, frame);
        appendAudioEncoding(command);
        command.add("-t");
        command.add(format(frame.frameDurationSeconds()));
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        command.add(outputClip.toString());
        run(runner, FRAME_TIMEOUT, command, cancellationRequested, outputLine);
        if (!Files.isRegularFile(outputClip) || Files.size(outputClip) <= 0L) {
            throw new IOException("No se creó el clip de video para " + frame.id() + ".");
        }
    }

    private static void renderFrameWithVisualParts(ExternalProcessRunner runner,
                                                   Path ffmpeg,
                                                   Path projectDirectory,
                                                   SimpleVideoExportSettings settings,
                                                   SimpleVideoFrame frame,
                                                   Path outputClip,
                                                   BooleanSupplier cancellationRequested,
                                                   Consumer<String> outputLine) throws IOException {
        Path partRoot = Files.createTempDirectory(outputClip.getParent(), outputClip.getFileName() + "-parts-");
        try {
            ArrayList<Path> visualClips = new ArrayList<>();
            int index = 1;
            for (SimpleVideoFrame.VisualPart part : frame.visualParts()) {
                Path partClip = partRoot.resolve("visual-" + String.format(Locale.ROOT, "%03d", index++) + ".mp4");
                renderVisualPart(runner, ffmpeg, projectDirectory, settings, frame, part, partClip,
                        cancellationRequested, outputLine);
                visualClips.add(partClip);
            }
            Path concat = partRoot.resolve("visual-parts.txt");
            Files.writeString(concat, concatFile(visualClips), StandardCharsets.UTF_8);
            Path visualSequence = partRoot.resolve("visual-sequence.mp4");
            run(runner, CONCAT_TIMEOUT, concatCommand(ffmpeg, concat, visualSequence), cancellationRequested, outputLine);
            muxVisualSequence(runner, ffmpeg, projectDirectory, frame, visualSequence, outputClip,
                    cancellationRequested, outputLine);
            if (!Files.isRegularFile(outputClip) || Files.size(outputClip) <= 0L) {
                throw new IOException("No se creÃ³ el clip de video para " + frame.id() + ".");
            }
        } finally {
            deleteRecursively(partRoot);
        }
    }

    private static void renderVisualPart(ExternalProcessRunner runner,
                                         Path ffmpeg,
                                         Path projectDirectory,
                                         SimpleVideoExportSettings settings,
                                         SimpleVideoFrame frame,
                                         SimpleVideoFrame.VisualPart part,
                                         Path outputClip,
                                         BooleanSupplier cancellationRequested,
                                         Consumer<String> outputLine) throws IOException {
        Path visual = requireProjectFile(projectDirectory, part.imageRelativePath(),
                part.videoClip() ? "clip de video" : "imagen", frame.id());
        ArrayList<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        if (part.videoClip()) {
            command.add("-ss");
            command.add(format(part.sourceStartSeconds()));
            command.add("-i");
            command.add(visual.toString());
            command.add("-t");
            command.add(format(part.durationSeconds()));
        } else {
            command.add("-loop");
            command.add("1");
            command.add("-t");
            command.add(format(part.durationSeconds()));
            command.add("-i");
            command.add(visual.toString());
        }
        command.add("-vf");
        command.add(settings.resolution().ffmpegScaleExpression()
                + ",fps=" + settings.framesPerSecond() + ",setpts=PTS-STARTPTS,format=yuv420p");
        command.add("-r");
        command.add(Integer.toString(settings.framesPerSecond()));
        command.add("-fps_mode");
        command.add("cfr");
        String codec = settings.encoderPolicy().ffmpegEncoder();
        if (!codec.isBlank()) {
            command.add("-c:v");
            command.add(codec);
        }
        command.add("-an");
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        command.add(outputClip.toString());
        run(runner, FRAME_TIMEOUT, command, cancellationRequested, outputLine);
    }

    private static void muxVisualSequence(ExternalProcessRunner runner,
                                          Path ffmpeg,
                                          Path projectDirectory,
                                          SimpleVideoFrame frame,
                                          Path visualSequence,
                                          Path outputClip,
                                          BooleanSupplier cancellationRequested,
                                          Consumer<String> outputLine) throws IOException {
        ArrayList<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-i");
        command.add(visualSequence.toString());
        if (frame.silentVisual()) {
            command.add("-f");
            command.add("lavfi");
            command.add("-t");
            command.add(format(frame.frameDurationSeconds()));
            command.add("-i");
            command.add(silentAudioSource());
        } else {
            Path audio = requireProjectFile(projectDirectory, frame.audioRelativePath(), "audio", frame.id());
            command.add("-i");
            command.add(audio.toString());
        }
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("1:a:0");
        command.add("-c:v");
        command.add("copy");
        appendExactAudioDuration(command, frame);
        appendAudioEncoding(command);
        command.add("-t");
        command.add(format(frame.frameDurationSeconds()));
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        command.add(outputClip.toString());
        run(runner, FRAME_TIMEOUT, command, cancellationRequested, outputLine);
    }

    private static void appendExactAudioDuration(List<String> command, SimpleVideoFrame frame) {
        String duration = format(frame.frameDurationSeconds());
        command.add("-af");
        command.add("apad=pad_dur=" + duration
                + ",atrim=duration=" + duration
                + ",asetpts=PTS-STARTPTS"
                + ",aresample=" + OUTPUT_AUDIO_SAMPLE_RATE
                + ",aformat=sample_fmts=fltp:sample_rates=" + OUTPUT_AUDIO_SAMPLE_RATE
                + ":channel_layouts=" + OUTPUT_AUDIO_LAYOUT);
    }

    private static void appendAudioEncoding(List<String> command) {
        command.add("-c:a");
        command.add("aac");
        command.add("-ar");
        command.add(Integer.toString(OUTPUT_AUDIO_SAMPLE_RATE));
        command.add("-ac");
        command.add("2");
    }

    private static String silentAudioSource() {
        return "anullsrc=channel_layout=" + OUTPUT_AUDIO_LAYOUT + ":sample_rate=" + OUTPUT_AUDIO_SAMPLE_RATE;
    }

    private static String normalizedVideoFilter(SimpleVideoExportSettings settings) {
        return settings.resolution().ffmpegScaleExpression()
                + ",fps=" + settings.framesPerSecond()
                + ",setpts=PTS-STARTPTS,format=yuv420p";
    }

    private static Path requireProjectFile(Path projectDirectory, String relativePath, String kind, String frameId) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("El frame " + frameId + " no tiene " + kind + " asignado.");
        }
        Path file = projectDirectory.resolve(relativePath).toAbsolutePath().normalize();
        if (!file.startsWith(projectDirectory.toAbsolutePath().normalize()) || !Files.isRegularFile(file)) {
            throw new IOException("No se encontró " + kind + " para " + frameId + ": " + relativePath);
        }
        return file;
    }

    private static List<String> concatCommand(Path ffmpeg, Path concat, Path target) {
        return List.of(ffmpeg.toString(), "-y", "-fflags", "+genpts",
                "-f", "concat", "-safe", "0", "-i", concat.toString(),
                "-c", "copy", "-avoid_negative_ts", "make_zero",
                "-movflags", "+faststart", target.toString());
    }

    private static String concatFile(List<Path> clips) {
        StringBuilder text = new StringBuilder();
        for (Path clip : clips) {
            text.append("file '").append(clip.toAbsolutePath().normalize().toString().replace("'", "'\\''").replace('\\', '/')).append("'\n");
        }
        return text.toString();
    }

    private static void run(ExternalProcessRunner runner,
                            Duration timeout,
                            List<String> command,
                            BooleanSupplier cancellationRequested,
                            Consumer<String> outputLine) throws IOException {
        try {
            ExternalProcessRequest request = ExternalProcessRequest.of(command, "ffmpeg-final-video", timeout)
                    .redirectingErrorStream();
            ExternalProcessResult result = runner.run(request, new ExternalProcessObserver() {
                @Override
                public void onOutputLine(String line) {
                    if (outputLine != null && usefulProgressLine(line)) {
                        outputLine.accept(line.strip());
                    }
                }

                @Override
                public boolean cancellationRequested() {
                    return cancellationRequested != null && cancellationRequested.getAsBoolean();
                }
            });
            if (result.cancelled()) {
                throw new ExternalProcessCancelledException("Exportación de video cancelada",
                        "La exportación de video fue cancelada por el usuario.",
                        result.commandAudit(), result.combinedOutputTail(), "");
            }
            if (result.timedOut()) {
                throw new ExternalProcessTimeoutException("Video local tardó demasiado",
                        "Video local tardó demasiado y fue detenido.",
                        result.commandAudit(), result.combinedOutputTail(), "");
            }
            if (result.exitCode() != 0) {
                throw new ExternalProcessFailedException("Video local falló",
                        "Video local devolvió código " + result.exitCode() + " al renderizar el MP4 final.",
                        result.exitCode(), result.commandAudit(), result.combinedOutputTail(), "", null);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Exportación de video interrumpida.", ex);
        }
    }

    private static boolean usefulProgressLine(String line) {
        if (line == null) {
            return false;
        }
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("frame=") || normalized.contains("time=") || normalized.contains("speed=")
                || normalized.contains("error") || normalized.contains("failed");
    }

    private static void throwIfCancelled(BooleanSupplier cancellationRequested,
                                         int completedFrames,
                                         int totalFrames,
                                         Consumer<VideoRenderProgress> progress) throws IOException {
        if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
            progress.accept(VideoRenderProgress.cancelled(completedFrames, totalFrames));
            throw new IOException("Exportación de video cancelada por el usuario.");
        }
    }

    private static String ffmpegDetail(String step, String line) {
        String cleanStep = step == null || step.isBlank() ? "Video local" : step;
        String cleanLine = line == null ? "" : line.strip();
        if (cleanLine.length() > 180) {
            cleanLine = cleanLine.substring(0, 180) + "...";
        }
        return cleanStep + " · " + cleanLine;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
