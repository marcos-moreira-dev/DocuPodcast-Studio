package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Exact encoder family transported to the render adapter and resource model. */
public enum VideoEncoderKind {
    AUTO,
    CPU_X264,
    NVIDIA_NVENC,
    INTEL_QSV,
    AMD_AMF;

    public boolean hardware() {
        return this == NVIDIA_NVENC || this == INTEL_QSV || this == AMD_AMF;
    }

    public String ffmpegCodec() {
        return switch (this) {
            case NVIDIA_NVENC -> "h264_nvenc";
            case INTEL_QSV -> "h264_qsv";
            case AMD_AMF -> "h264_amf";
            case AUTO, CPU_X264 -> "libx264";
        };
    }

    public String vendor() {
        return switch (this) {
            case NVIDIA_NVENC -> "NVIDIA";
            case INTEL_QSV -> "INTEL";
            case AMD_AMF -> "AMD";
            case AUTO, CPU_X264 -> "GENERIC";
        };
    }
}
