package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.Locale;

/** User-facing policy for choosing where local inference/render work should run. */
public enum ComputeDevicePolicy {
    AUTO("Automático recomendado"),
    CPU_ONLY("Solo CPU"),
    PREFER_GPU("Preferir GPU si está disponible"),
    SPECIFIC_DEVICE("Dispositivo específico");

    private final String label;

    ComputeDevicePolicy(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean canUseGpu() {
        return this == AUTO || this == PREFER_GPU || this == SPECIFIC_DEVICE;
    }

    public static ComputeDevicePolicy from(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        for (ComputeDevicePolicy policy : values()) {
            if (policy.name().equals(normalized)) {
                return policy;
            }
        }
        if ("CPU".equals(normalized)) {
            return CPU_ONLY;
        }
        if ("GPU".equals(normalized) || "GPU_PREFERRED".equals(normalized)) {
            return PREFER_GPU;
        }
        return AUTO;
    }
}
