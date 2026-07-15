package com.marcosmoreiradev.docupodcaststudio.application.compute;

/** Lightweight description of a local compute device candidate. */
public record ComputeDeviceDescriptor(
        String id,
        String displayName,
        ComputeDeviceType type,
        String vendor,
        boolean detected
) {
    public ComputeDeviceDescriptor {
        id = normalize(id).isBlank() ? "unknown" : normalize(id);
        displayName = normalize(displayName).isBlank() ? id : normalize(displayName);
        type = type == null ? ComputeDeviceType.UNKNOWN : type;
        vendor = normalize(vendor).isBlank() ? "desconocido" : normalize(vendor);
    }

    public static ComputeDeviceDescriptor cpu(String displayName) {
        return new ComputeDeviceDescriptor("cpu", displayName, ComputeDeviceType.CPU, "CPU", true);
    }

    public static ComputeDeviceDescriptor gpu(String id, String displayName, String vendor) {
        return new ComputeDeviceDescriptor(id, displayName, ComputeDeviceType.GPU, vendor, true);
    }

    public boolean gpu() {
        return type == ComputeDeviceType.GPU;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
