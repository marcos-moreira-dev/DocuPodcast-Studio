package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Objects;

/**
 * Provider-neutral compute preference for one engine operation.
 *
 * <p>The preference is deliberately advisory for {@link Mode#AUTO} and
 * {@link Mode#PREFER_GPU}. A provider must report the effective device and must
 * never switch from an explicitly selected device to CPU without informing the
 * caller.</p>
 */
public record ComputePreference(Mode mode, String deviceId, boolean allowHostMemory) {
    public ComputePreference {
        mode = Objects.requireNonNullElse(mode, Mode.AUTO);
        deviceId = Objects.toString(deviceId, "").strip();
        if (mode != Mode.SPECIFIC_DEVICE) {
            deviceId = "";
        } else if (deviceId.isBlank()) {
            throw new IllegalArgumentException("A specific compute device requires a device id");
        }
    }

    public enum Mode {
        AUTO,
        CPU_ONLY,
        PREFER_GPU,
        SPECIFIC_DEVICE
    }

    public static ComputePreference automatic() {
        return new ComputePreference(Mode.AUTO, "", true);
    }

    public static ComputePreference cpuOnly(boolean allowHostMemory) {
        return new ComputePreference(Mode.CPU_ONLY, "", allowHostMemory);
    }

    public static ComputePreference preferGpu(boolean allowHostMemory) {
        return new ComputePreference(Mode.PREFER_GPU, "", allowHostMemory);
    }

    public static ComputePreference specificDevice(String deviceId, boolean allowHostMemory) {
        return new ComputePreference(Mode.SPECIFIC_DEVICE, deviceId, allowHostMemory);
    }
}
