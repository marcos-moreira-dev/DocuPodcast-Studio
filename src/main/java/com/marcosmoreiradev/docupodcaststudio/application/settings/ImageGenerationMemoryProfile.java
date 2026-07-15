package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.util.Locale;

/** Persistent memory strategy for the local image-generation runtime. */
public enum ImageGenerationMemoryProfile {
    SAFE_LOW_VRAM("Seguro / bajo consumo", true),
    NORMAL("Normal", false),
    VRAM_RAM_OFFLOAD("VRAM + RAM / modelos grandes", false),
    HIGH_MEMORY("VRAM completa / GPU grande", false);

    private final String displayName;
    private final boolean legacyLowVram;

    ImageGenerationMemoryProfile(String displayName, boolean legacyLowVram) {
        this.displayName = displayName;
        this.legacyLowVram = legacyLowVram;
    }

    public String displayName() {
        return displayName;
    }

    public boolean legacyLowVram() {
        return legacyLowVram;
    }

    public static ImageGenerationMemoryProfile from(String value, boolean fallbackLowVram) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!normalized.isBlank()) {
            for (ImageGenerationMemoryProfile profile : values()) {
                if (profile.name().equals(normalized)) {
                    return profile;
                }
            }
            if ("LOW_VRAM".equals(normalized) || "LOWVRAM".equals(normalized) || "SAFE".equals(normalized)) {
                return SAFE_LOW_VRAM;
            }
            if ("RAM".equals(normalized)
                    || "RAM_OFFLOAD".equals(normalized)
                    || "OFFLOAD".equals(normalized)
                    || "VRAM_RAM".equals(normalized)
                    || "VRAM_PLUS_RAM".equals(normalized)
                    || "HYBRID".equals(normalized)
                    || "LARGE_MODEL".equals(normalized)
                    || "LARGE_MODELS".equals(normalized)
                    || "MODELOS_GRANDES".equals(normalized)) {
                return VRAM_RAM_OFFLOAD;
            }
            if ("HIGH".equals(normalized) || "HIGH_VRAM".equals(normalized) || "FULL".equals(normalized)) {
                return HIGH_MEMORY;
            }
        }
        return fallbackLowVram ? SAFE_LOW_VRAM : NORMAL;
    }
}
