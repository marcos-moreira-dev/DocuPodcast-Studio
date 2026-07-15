package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationResult;
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

/** Normalizes arbitrary user audio to WAV PCM through FFmpeg. */
public final class FfmpegAudioNormalizationGateway implements AudioNormalizationGateway {
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private final FfmpegToolDiscovery discovery;
    private final ExternalProcessRunner runner;

    public FfmpegAudioNormalizationGateway(FfmpegToolDiscovery discovery) {
        this(discovery, new DefaultExternalProcessRunner());
    }

    public FfmpegAudioNormalizationGateway(FfmpegToolDiscovery discovery, ExternalProcessRunner runner) {
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
    public AudioNormalizationResult normalize(Path sourceAudioFile,
                                              Path targetAudioFile,
                                              AudioNormalizationProfile profile) throws IOException {
        Objects.requireNonNull(sourceAudioFile, "sourceAudioFile");
        Objects.requireNonNull(targetAudioFile, "targetAudioFile");
        AudioNormalizationProfile normalizedProfile = profile == null
                ? AudioNormalizationProfile.ASSIGNABLE_AUDIO
                : profile;
        if (!ready()) {
            throw new IOException("FFmpeg no está disponible para preparar audio. " + readinessMessage());
        }
        if (!Files.isRegularFile(sourceAudioFile)) {
            throw new IOException("Audio file not found: " + sourceAudioFile);
        }
        if (targetAudioFile.getParent() != null) {
            Files.createDirectories(targetAudioFile.getParent());
        }
        Path stdoutLog = sidecar(targetAudioFile, "-ffmpeg-stdout.log");
        Path stderrLog = sidecar(targetAudioFile, "-ffmpeg-stderr.log");
        Path manifest = sidecar(targetAudioFile, "-ffmpeg-normalization.txt");
        List<String> command = ffmpegCommand(
                discovery.ffmpegExecutable(),
                "-y",
                "-i", sourceAudioFile.toAbsolutePath().normalize().toString(),
                "-vn",
                "-acodec", "pcm_s16le",
                "-ar", Integer.toString(normalizedProfile.sampleRate()),
                "-ac", Integer.toString(normalizedProfile.channels()),
                targetAudioFile.toAbsolutePath().normalize().toString()
        );
        ExternalProcessResult result;
        try {
            result = runner.run(ExternalProcessRequest.of(command, "ffmpeg-audio-normalization", TIMEOUT));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Audio normalization interrupted", ex);
        }
        writeOutput(stdoutLog, result.stdout());
        writeOutput(stderrLog, result.stderr());
        if (result.timedOut()) {
            writeManifest(manifest, sourceAudioFile, targetAudioFile, normalizedProfile, command,
                    false, "FFmpeg excedió el tiempo máximo de normalización.", stdoutLog, stderrLog);
            return new AudioNormalizationResult(sourceAudioFile, targetAudioFile, normalizedProfile, false,
                    "FFmpeg excedió el tiempo máximo de normalización. Logs: " + stdoutLog.getFileName() + " / " + stderrLog.getFileName());
        }
        String stderr = result.stderr();
        if (result.exitCode() != 0) {
            String message = stderr.isBlank() ? "FFmpeg terminó con código " + result.exitCode() : stderr;
            writeManifest(manifest, sourceAudioFile, targetAudioFile, normalizedProfile, command,
                    false, message, stdoutLog, stderrLog);
            return new AudioNormalizationResult(sourceAudioFile, targetAudioFile, normalizedProfile, false, message);
        }
        String message = "Audio normalizado con FFmpeg como " + normalizedProfile.displayName()
                + " (" + normalizedProfile.sampleRate() + " Hz, " + normalizedProfile.channels() + " canal(es)).";
        writeManifest(manifest, sourceAudioFile, targetAudioFile, normalizedProfile, command,
                true, message, stdoutLog, stderrLog);
        return new AudioNormalizationResult(sourceAudioFile, targetAudioFile, normalizedProfile, true, message);
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
                                      Path sourceAudioFile,
                                      Path targetAudioFile,
                                      AudioNormalizationProfile profile,
                                      List<String> command,
                                      boolean success,
                                      String message,
                                      Path stdoutLog,
                                      Path stderrLog) throws IOException {
        String content = "# FFmpeg audio normalization" + System.lineSeparator()
                + "createdAt=" + Instant.now() + System.lineSeparator()
                + "source=" + sourceAudioFile.toAbsolutePath().normalize() + System.lineSeparator()
                + "target=" + targetAudioFile.toAbsolutePath().normalize() + System.lineSeparator()
                + "profile=" + profile.name() + System.lineSeparator()
                + "sampleRate=" + profile.sampleRate() + System.lineSeparator()
                + "channels=" + profile.channels() + System.lineSeparator()
                + "success=" + success + System.lineSeparator()
                + "message=" + (message == null ? "" : message.replace(System.lineSeparator(), " ")) + System.lineSeparator()
                + "stdoutLog=" + stdoutLog.getFileName() + System.lineSeparator()
                + "stderrLog=" + stderrLog.getFileName() + System.lineSeparator()
                + "command=" + String.join(" ", command) + System.lineSeparator();
        Files.writeString(manifest, content, StandardCharsets.UTF_8);
    }
}
