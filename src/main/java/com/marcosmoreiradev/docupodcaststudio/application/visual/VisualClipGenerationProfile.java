package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.Locale;

/** Category-neutral local image-to-video profiles. */
public enum VisualClipGenerationProfile {
    WAN22_TI2V_5B_BALANCED("Wan 2.2 TI2V 5B equilibrado", 5.0, false),
    WAN22_I2V_14B_QUALITY("Wan 2.2 I2V 14B calidad", 5.0, false),
    LTX23_I2V_PORTRAIT("LTX 2.3 I2V retrato", 5.0, false),
    CUSTOM_COMFY_VIDEO("Workflow ComfyUI de video", 10.0, true);

    private final String displayName;
    private final double defaultMaxClipSeconds;
    private final boolean customWorkflow;

    VisualClipGenerationProfile(String displayName,
                                double defaultMaxClipSeconds,
                                boolean customWorkflow) {
        this.displayName = displayName;
        this.defaultMaxClipSeconds = defaultMaxClipSeconds;
        this.customWorkflow = customWorkflow;
    }

    public String displayName() {
        return displayName;
    }

    public double defaultMaxClipSeconds() {
        return defaultMaxClipSeconds;
    }

    public boolean customWorkflow() {
        return customWorkflow;
    }

    public static VisualClipGenerationProfile from(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        for (VisualClipGenerationProfile profile : values()) {
            if (profile.name().equals(normalized)) {
                return profile;
            }
        }
        return WAN22_TI2V_5B_BALANCED;
    }
}
