package com.marcosmoreiradev.docupodcaststudio.media.api;

public enum VideoEncodingPreference {
    AUTO,
    CPU,
    HARDWARE_PREFERRED,
    NVIDIA_NVENC,
    INTEL_QSV,
    AMD_AMF;

    public VideoEncoderKind exactKind() {
        return switch (this) {
            case CPU -> VideoEncoderKind.CPU_X264;
            case NVIDIA_NVENC, HARDWARE_PREFERRED -> VideoEncoderKind.NVIDIA_NVENC;
            case INTEL_QSV -> VideoEncoderKind.INTEL_QSV;
            case AMD_AMF -> VideoEncoderKind.AMD_AMF;
            case AUTO -> VideoEncoderKind.AUTO;
        };
    }
}
