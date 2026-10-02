package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/** Physical encoder lane associated with its owning device. */
public record EncoderResourceDemand(
        VideoEncoderKind kind,
        ComputeDeviceId device,
        int units) {
    public EncoderResourceDemand {
        kind = Objects.requireNonNullElse(kind, VideoEncoderKind.AUTO);
        device = Objects.requireNonNullElse(device, ComputeDeviceId.CPU_0);
        units = Math.max(0, units);
    }

    public static EncoderResourceDemand none() {
        return new EncoderResourceDemand(VideoEncoderKind.AUTO,
                ComputeDeviceId.CPU_0, 0);
    }
}
