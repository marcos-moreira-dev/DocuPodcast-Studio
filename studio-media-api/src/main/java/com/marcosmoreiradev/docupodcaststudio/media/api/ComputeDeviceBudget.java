package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Physical capacity and DocuPodcast budget for one device; telemetry is separate. */
public record ComputeDeviceBudget(
        ComputeDeviceId device,
        long physicalVramBytes,
        long vramBudgetBytes,
        int gpuComputeUnits,
        Map<VideoEncoderKind, Integer> encoderUnits) {
    public ComputeDeviceBudget {
        device = Objects.requireNonNull(device, "compute device");
        physicalVramBytes = Math.max(0L, physicalVramBytes);
        vramBudgetBytes = Math.max(0L, vramBudgetBytes);
        if (physicalVramBytes > 0L) {
            vramBudgetBytes = Math.min(vramBudgetBytes, physicalVramBytes);
        }
        gpuComputeUnits = Math.max(0, gpuComputeUnits);
        EnumMap<VideoEncoderKind, Integer> normalized =
                new EnumMap<>(VideoEncoderKind.class);
        if (encoderUnits != null) encoderUnits.forEach((kind, units) -> {
            if (kind != null && units != null && units > 0) {
                normalized.put(kind, units);
            }
        });
        encoderUnits = Map.copyOf(normalized);
    }
}
