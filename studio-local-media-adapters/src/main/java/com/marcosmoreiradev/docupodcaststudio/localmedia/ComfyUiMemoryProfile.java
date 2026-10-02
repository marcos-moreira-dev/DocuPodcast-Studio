package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.util.List;
import java.util.Locale;

/** Adapter-owned translation of the persisted memory policy into ComfyUI flags. */
public enum ComfyUiMemoryProfile {
    SAFE_LOW_VRAM(List.of("--lowvram")),
    NORMAL(List.of()),
    VRAM_RAM_OFFLOAD(List.of("--novram")),
    HIGH_MEMORY(List.of("--highvram"));

    private final List<String> launchArguments;

    ComfyUiMemoryProfile(List<String> launchArguments) {
        this.launchArguments = List.copyOf(launchArguments);
    }

    public List<String> launchArguments() {
        return launchArguments;
    }

    public static ComfyUiMemoryProfile from(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        if (normalized.equals("LOW_VRAM") || normalized.equals("LOWVRAM") || normalized.equals("SAFE")) {
            return SAFE_LOW_VRAM;
        }
        if (normalized.equals("RAM") || normalized.equals("RAM_OFFLOAD") || normalized.equals("OFFLOAD")
                || normalized.equals("VRAM_RAM") || normalized.equals("VRAM_PLUS_RAM")
                || normalized.equals("HYBRID") || normalized.equals("LARGE_MODEL")) {
            return VRAM_RAM_OFFLOAD;
        }
        if (normalized.equals("HIGH") || normalized.equals("HIGH_VRAM") || normalized.equals("FULL")) {
            return HIGH_MEMORY;
        }
        try {
            return normalized.isBlank() ? SAFE_LOW_VRAM : valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return SAFE_LOW_VRAM;
        }
    }
}
