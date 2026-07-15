package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Probes bundled FFmpeg/FFprobe without relying on PATH or global installs. */
public final class FfmpegRuntimeProbeUseCase {
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private final ExternalProcessRunner runner;

    public FfmpegRuntimeProbeUseCase() {
        this(ExternalProcessRunner.unavailable("FfmpegRuntimeProbeUseCase"));
    }

    public FfmpegRuntimeProbeUseCase(ExternalProcessRunner runner) {
        this.runner = runner == null ? ExternalProcessRunner.unavailable("FfmpegRuntimeProbeUseCase") : runner;
    }

    public FfmpegRuntimeReport inspect(FfmpegToolDiscovery discovery) {
        Path ffmpeg = discovery == null ? null : discovery.ffmpegExecutable();
        Path ffprobe = discovery == null ? null : discovery.ffprobeExecutable();
        boolean ffmpegReady = ffmpeg != null && Files.isRegularFile(ffmpeg);
        boolean ffprobeReady = ffprobe != null && Files.isRegularFile(ffprobe);
        ArrayList<String> warnings = new ArrayList<>();
        if (!ffmpegReady) {
            warnings.add("Falta tools/ffmpeg/bin/ffmpeg.exe; la RC final no debe depender de PATH global.");
        }
        if (!ffprobeReady) {
            warnings.add("Falta tools/ffmpeg/bin/ffprobe.exe; se requiere para validar video final.");
        }
        String ffmpegVersionOutput = ffmpegReady ? run(ffmpeg, List.of("-version"), warnings) : "";
        String ffmpegVersion = firstLine(ffmpegVersionOutput);
        String ffprobeVersion = ffprobeReady ? firstLine(run(ffprobe, List.of("-version"), warnings)) : "";
        List<String> encoders = ffmpegReady
                ? mergeEncoders(
                parseEncoderOutput(run(ffmpeg, List.of("-hide_banner", "-encoders"), warnings)),
                parseConfigurationOutput(ffmpegVersionOutput))
                : List.of();
        if (ffmpegReady && encoders.stream().noneMatch("libx264"::equalsIgnoreCase)) {
            warnings.add("El binario FFmpeg no reporta libx264; el fallback CPU de video no está confirmado.");
        }
        return new FfmpegRuntimeReport(ffmpeg, ffprobe, ffmpegReady, ffprobeReady, ffmpegVersion, ffprobeVersion, encoders, warnings);
    }

    public static List<String> parseEncoderOutput(String output) {
        String text = output == null ? "" : output;
        LinkedHashSet<String> encoders = new LinkedHashSet<>();
        for (String expected : List.of("libx264", "h264_nvenc", "h264_qsv", "h264_amf")) {
            if (text.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT))) {
                encoders.add(expected);
            }
        }
        return List.copyOf(encoders);
    }

    static List<String> parseConfigurationOutput(String output) {
        String text = output == null ? "" : output.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> encoders = new LinkedHashSet<>();
        if (text.contains("--enable-libx264")) {
            encoders.add("libx264");
        }
        if (text.contains("--enable-nvenc") || text.contains("--enable-ffnvcodec")) {
            encoders.add("h264_nvenc");
        }
        if (text.contains("--enable-libvpl") || text.contains("--enable-libmfx")) {
            encoders.add("h264_qsv");
        }
        if (text.contains("--enable-amf")) {
            encoders.add("h264_amf");
        }
        return List.copyOf(encoders);
    }

    private static List<String> mergeEncoders(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return List.copyOf(merged);
    }

    private String run(Path executable, List<String> args, List<String> warnings) {
        try {
            ArrayList<String> command = new ArrayList<>();
            command.add(executable.toString());
            command.addAll(args);
            ExternalProcessRequest request = ExternalProcessRequest.of(command,
                            executable.getFileName().toString() + " probe",
                            TIMEOUT)
                    .redirectingErrorStream();
            ExternalProcessResult result = runner.run(request);
            if (result.timedOut()) {
                warnings.add("Timeout al consultar " + executable.getFileName() + ".");
            } else if (result.exitCode() != 0) {
                warnings.add(executable.getFileName() + " devolvió código " + result.exitCode() + ".");
            }
            return result.combinedOutputTail();
        } catch (Exception ex) {
            warnings.add("No se pudo ejecutar " + executable.getFileName() + ": " + ex.getMessage());
            return "";
        }
    }

    private static String firstLine(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        return output.lines().findFirst().orElse("").strip();
    }
}
