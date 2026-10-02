package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Stable physical compute-device identity; vendor is descriptive, never assumed. */
public record ComputeDeviceId(String kind, String vendor, int index)
        implements Comparable<ComputeDeviceId> {
    public static final ComputeDeviceId CPU_0 = cpu(0);
    public static final ComputeDeviceId AUTO_GPU_0 = gpu("AUTO", 0);

    public ComputeDeviceId {
        kind = normalized(kind, "CPU");
        vendor = normalized(vendor, "GENERIC");
        index = Math.max(0, index);
    }

    public static ComputeDeviceId cpu(int index) {
        return new ComputeDeviceId("CPU", "GENERIC", index);
    }

    public static ComputeDeviceId gpu(String vendor, int index) {
        return new ComputeDeviceId("GPU", vendor, index);
    }

    public static ComputeDeviceId parse(String value) {
        String safe = value == null ? "" : value.strip();
        if (safe.isBlank()) return AUTO_GPU_0;
        String[] fields = safe.replace('-', ':').split(":");
        if (fields.length == 2 && "CPU".equalsIgnoreCase(fields[0])) {
            return cpu(number(fields[1]));
        }
        if (fields.length >= 3 && "GPU".equalsIgnoreCase(fields[0])) {
            return gpu(fields[1], number(fields[2]));
        }
        return gpu("GENERIC", number(fields[fields.length - 1]));
    }

    public boolean gpu() { return "GPU".equals(kind); }
    public boolean cpu() { return "CPU".equals(kind); }

    public String value() { return kind + ":" + vendor + ":" + index; }

    @Override public int compareTo(ComputeDeviceId other) {
        return value().compareTo(other.value());
    }

    @Override public String toString() { return value(); }

    private static String normalized(String value, String fallback) {
        String safe = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        return safe.isBlank() ? fallback : safe.replace(' ', '_');
    }

    private static int number(String value) {
        try { return Math.max(0, Integer.parseInt(value)); }
        catch (RuntimeException ignored) { return 0; }
    }
}
