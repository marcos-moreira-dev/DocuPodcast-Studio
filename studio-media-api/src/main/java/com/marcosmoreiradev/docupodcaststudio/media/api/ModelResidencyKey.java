package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/** Identity of shareable model weights and placement, distinct from a request context. */
public record ModelResidencyKey(
        String engineId,
        String modelIdentity,
        String runtimeProfile,
        ComputeDeviceId device) implements Comparable<ModelResidencyKey> {
    public ModelResidencyKey {
        engineId = normalized(engineId, "engine");
        modelIdentity = normalized(modelIdentity, "model");
        runtimeProfile = normalized(runtimeProfile, "default");
        device = Objects.requireNonNullElse(device, ComputeDeviceId.CPU_0);
    }

    public String value() {
        return engineId + "/" + modelIdentity + "/" + runtimeProfile
                + "@" + device.value();
    }

    @Override public int compareTo(ModelResidencyKey other) {
        return value().compareTo(other.value());
    }

    private static String normalized(String value, String fallback) {
        String safe = value == null ? "" : value.strip();
        return safe.isBlank() ? fallback : safe;
    }
}
