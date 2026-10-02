package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

/** Best-effort observations; never confused with physical capacity or configured budget. */
public record ComputeResourceTelemetry(
        long observedHostMemoryBytes,
        Map<ComputeDeviceId, DeviceTelemetry> devices) {
    public ComputeResourceTelemetry {
        observedHostMemoryBytes = Math.max(0L, observedHostMemoryBytes);
        devices = devices == null ? Map.of() : Map.copyOf(devices);
    }

    public record DeviceTelemetry(long observedVramBytes,
                                  int activeComputeConsumers) {
        public DeviceTelemetry {
            observedVramBytes = Math.max(0L, observedVramBytes);
            activeComputeConsumers = Math.max(0, activeComputeConsumers);
        }
    }
}
