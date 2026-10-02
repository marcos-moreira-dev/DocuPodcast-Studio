package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;

import java.nio.file.Path;
import java.util.List;

/** Runtime probe result for bundled FFmpeg/FFprobe and encoder support. */
public record FfmpegRuntimeReport(
        Path ffmpegExecutable,
        Path ffprobeExecutable,
        boolean ffmpegReady,
        boolean ffprobeReady,
        String ffmpegVersion,
        String ffprobeVersion,
        List<String> encoders,
        List<String> warnings
) {
    public FfmpegRuntimeReport {
        ffmpegVersion = ffmpegVersion == null ? "" : ffmpegVersion.strip();
        ffprobeVersion = ffprobeVersion == null ? "" : ffprobeVersion.strip();
        encoders = encoders == null ? List.of() : List.copyOf(encoders);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean readyForFinalVideo() {
        return ffmpegReady && ffprobeReady && supportsLibx264();
    }

    public boolean supportsLibx264() {
        return supportsEncoder("libx264");
    }

    public boolean supportsNvenc() {
        return supportsEncoder("h264_nvenc");
    }

    public boolean supportsQsv() {
        return supportsEncoder("h264_qsv");
    }

    public boolean supportsAmf() {
        return supportsEncoder("h264_amf");
    }

    public boolean supportsEncoder(String encoder) {
        String expected = encoder == null ? "" : encoder.strip();
        return !expected.isBlank() && encoders.stream().anyMatch(value -> value.equalsIgnoreCase(expected));
    }

    public boolean supportsPolicy(VideoEncoderPolicy policy) {
        if (policy == null || policy == VideoEncoderPolicy.AUTO || policy == VideoEncoderPolicy.CPU_X264) {
            return supportsLibx264();
        }
        return switch (policy) {
            case NVIDIA_NVENC -> supportsNvenc();
            case INTEL_QSV -> supportsQsv();
            case AMD_AMF -> supportsAmf();
            default -> supportsLibx264();
        };
    }

    /**
     * Resolves the encoder that the final-video UI can actually promise for the current runtime.
     * AUTO remains GPU-first and falls back to libx264 only when no supported hardware encoder exists.
     */
    public String effectiveEncoder(VideoEncoderPolicy policy) {
        VideoEncoderPolicy requested = policy == null ? VideoEncoderPolicy.AUTO : policy;
        if (requested != VideoEncoderPolicy.AUTO) {
            return supportsEncoder(requested.ffmpegEncoder()) ? requested.ffmpegEncoder() : "";
        }
        if (supportsNvenc()) return "h264_nvenc";
        if (supportsQsv()) return "h264_qsv";
        if (supportsAmf()) return "h264_amf";
        return supportsLibx264() ? "libx264" : "";
    }
}
