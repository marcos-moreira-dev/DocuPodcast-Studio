package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.application.media.VideoAudioExtractionGateway;
import com.marcosmoreiradev.docupodcaststudio.application.media.VideoAudioExtractionResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Extracts a WAV track from a video using the configured or embedded FFmpeg binary. */
public final class FfmpegVideoAudioExtractionGateway implements VideoAudioExtractionGateway {
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private final FfmpegToolDiscovery discovery;
    private final ExternalProcessRunner runner;

    public FfmpegVideoAudioExtractionGateway(FfmpegToolDiscovery discovery) {
        this(discovery, new DefaultExternalProcessRunner());
    }

    public FfmpegVideoAudioExtractionGateway(FfmpegToolDiscovery discovery, ExternalProcessRunner runner) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.runner = runner == null ? new DefaultExternalProcessRunner() : runner;
    }

    @Override
    public boolean ready() {
        return discovery.ready() && discovery.ffmpegExecutable() != null && Files.isRegularFile(discovery.ffmpegExecutable());
    }

    @Override
    public String readinessMessage() {
        return discovery.message();
    }

    @Override
    public VideoAudioExtractionResult extractAudio(Path sourceVideoFile, Path targetAudioFile) throws IOException {
        Objects.requireNonNull(sourceVideoFile, "sourceVideoFile");
        Objects.requireNonNull(targetAudioFile, "targetAudioFile");
        if (!ready()) {
            throw new IOException("FFmpeg no está disponible para extraer audio de video. " + readinessMessage());
        }
        if (!Files.isRegularFile(sourceVideoFile)) {
            throw new IOException("Video file not found: " + sourceVideoFile);
        }
        if (targetAudioFile.getParent() != null) {
            Files.createDirectories(targetAudioFile.getParent());
        }
        Path stdoutLog = sidecar(targetAudioFile, "-ffmpeg-stdout.log");
        Path stderrLog = sidecar(targetAudioFile, "-ffmpeg-stderr.log");
        Path manifest = sidecar(targetAudioFile, "-ffmpeg-extraction.txt");
        List<String> command = ffmpegCommand(
                discovery.ffmpegExecutable(),
                "-y",
                "-i", sourceVideoFile.toAbsolutePath().normalize().toString(),
                "-vn",
                "-acodec", "pcm_s16le",
                "-ar", "44100",
                "-ac", "2",
                targetAudioFile.toAbsolutePath().normalize().toString()
        );
        ExternalProcessResult result;
        try {
            result = runner.run(ExternalProcessRequest.of(command, "ffmpeg-video-audio-extraction", TIMEOUT));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Video audio extraction interrupted", ex);
        }
        writeOutput(stdoutLog, result.stdout());
        writeOutput(stderrLog, result.stderr());
        if (result.timedOut()) {
            writeManifest(manifest, sourceVideoFile, targetAudioFile, command, false,
                    "FFmpeg excedió el tiempo máximo de extracción.", stdoutLog, stderrLog);
            return new VideoAudioExtractionResult(sourceVideoFile, targetAudioFile, false,
                    "FFmpeg excedió el tiempo máximo de extracción. Logs: " + stdoutLog.getFileName() + " / " + stderrLog.getFileName());
        }
        String stderr = result.stderr();
        if (result.exitCode() != 0) {
            String message = stderr.isBlank() ? "FFmpeg terminó con código " + result.exitCode() : stderr;
            writeManifest(manifest, sourceVideoFile, targetAudioFile, command, false, message, stdoutLog, stderrLog);
            return new VideoAudioExtractionResult(sourceVideoFile, targetAudioFile, false, message);
        }
        String message = "Audio extraído con FFmpeg desde video (WAV PCM 44.1 kHz estéreo).";
        writeManifest(manifest, sourceVideoFile, targetAudioFile, command, true, message, stdoutLog, stderrLog);
        return new VideoAudioExtractionResult(sourceVideoFile, targetAudioFile, true, message);
    }

    private static List<String> ffmpegCommand(Path executable, String... args) {
        List<String> command = new ArrayList<>();
        String executableText = executable == null ? "" : executable.toString();
        if (requiresWindowsCommandShell(executableText)) {
            command.add(windowsCommandShell());
            command.add("/c");
        }
        command.add(executableText);
        command.addAll(List.of(args));
        return command;
    }

    private static boolean requiresWindowsCommandShell(String executable) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String lower = executable == null ? "" : executable.toLowerCase(Locale.ROOT);
        return os.contains("win") && (lower.endsWith(".cmd") || lower.endsWith(".bat"));
    }

    private static String windowsCommandShell() {
        String comspec = System.getenv("COMSPEC");
        return comspec == null || comspec.isBlank() ? "cmd.exe" : comspec;
    }

    private static Path sidecar(Path targetAudioFile, String suffix) {
        String name = targetAudioFile.getFileName() == null ? "audio.wav" : targetAudioFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot < 0 ? name : name.substring(0, dot);
        Path parent = targetAudioFile.getParent();
        return (parent == null ? Path.of(stem + suffix) : parent.resolve(stem + suffix)).normalize();
    }

    private static void writeOutput(Path file, String output) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, output == null ? "" : output, StandardCharsets.UTF_8);
    }

    private static void writeManifest(Path manifest,
                                      Path sourceVideoFile,
                                      Path targetAudioFile,
                                      List<String> command,
                                      boolean success,
                                      String message,
                                      Path stdoutLog,
                                      Path stderrLog) throws IOException {
        String content = "# FFmpeg video audio extraction" + System.lineSeparator()
                + "createdAt=" + Instant.now() + System.lineSeparator()
                + "source=" + sourceVideoFile.toAbsolutePath().normalize() + System.lineSeparator()
                + "target=" + targetAudioFile.toAbsolutePath().normalize() + System.lineSeparator()
                + "profile=ASSIGNABLE_AUDIO" + System.lineSeparator()
                + "sampleRate=44100" + System.lineSeparator()
                + "channels=2" + System.lineSeparator()
                + "success=" + success + System.lineSeparator()
                + "message=" + (message == null ? "" : message.replace(System.lineSeparator(), " ")) + System.lineSeparator()
                + "stdoutLog=" + stdoutLog.getFileName() + System.lineSeparator()
                + "stderrLog=" + stderrLog.getFileName() + System.lineSeparator()
                + "command=" + String.join(" ", command) + System.lineSeparator();
        Files.writeString(manifest, content, StandardCharsets.UTF_8);
    }
}
