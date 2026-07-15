package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.Locale;

/** Preferred FFmpeg encoder policy for simple video packages. */
public enum VideoEncoderPolicy {
    AUTO("Automático", "", false),
    CPU_X264("CPU — libx264", "libx264", false),
    NVIDIA_NVENC("GPU NVIDIA — NVENC", "h264_nvenc", true),
    INTEL_QSV("GPU Intel — QSV", "h264_qsv", true),
    AMD_AMF("GPU AMD — AMF", "h264_amf", true);

    private final String label;
    private final String ffmpegEncoder;
    private final boolean hardwareAccelerated;

    VideoEncoderPolicy(String label, String ffmpegEncoder, boolean hardwareAccelerated) {
        this.label = label;
        this.ffmpegEncoder = ffmpegEncoder;
        this.hardwareAccelerated = hardwareAccelerated;
    }

    public String label() {
        return label;
    }

    public String ffmpegEncoder() {
        return ffmpegEncoder;
    }

    public boolean hardwareAccelerated() {
        return hardwareAccelerated;
    }

    public String ffmpegCodecArgument() {
        return ffmpegEncoder.isBlank() ? "" : "-c:v " + ffmpegEncoder;
    }

    public static VideoEncoderPolicy from(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        for (VideoEncoderPolicy policy : values()) {
            if (policy.name().equals(normalized)) {
                return policy;
            }
        }
        if ("NVENC".equals(normalized) || "H264_NVENC".equals(normalized)) {
            return NVIDIA_NVENC;
        }
        if ("QSV".equals(normalized) || "H264_QSV".equals(normalized)) {
            return INTEL_QSV;
        }
        if ("AMF".equals(normalized) || "H264_AMF".equals(normalized)) {
            return AMD_AMF;
        }
        if ("CPU".equals(normalized) || "X264".equals(normalized) || "LIBX264".equals(normalized)) {
            return CPU_X264;
        }
        return AUTO;
    }
}
