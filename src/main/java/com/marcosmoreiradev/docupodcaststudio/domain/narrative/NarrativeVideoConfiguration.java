package com.marcosmoreiradev.docupodcaststudio.domain.narrative;

/** Global local-AI video settings for a narrative project. */
public record NarrativeVideoConfiguration(
        int width,
        int height,
        int framesPerSecond,
        double maxClipDurationSeconds,
        String imageProfile,
        String videoProfile,
        String memoryMode,
        String customWorkflowPath
) {
    public NarrativeVideoConfiguration {
        width = Math.max(360, width);
        height = Math.max(360, height);
        framesPerSecond = Math.max(12, Math.min(60, framesPerSecond));
        maxClipDurationSeconds = Math.max(1.0, Math.min(10.0, maxClipDurationSeconds));
        imageProfile = optional(imageProfile);
        if (imageProfile.isBlank()) {
            imageProfile = "PRODUCTION_SDXL_REFERENCE";
        }
        videoProfile = optional(videoProfile);
        if (videoProfile.isBlank()) {
            videoProfile = "WAN22_TI2V_5B_BALANCED";
        }
        memoryMode = optional(memoryMode);
        if (memoryMode.isBlank()) {
            memoryMode = "SAFE";
        }
        customWorkflowPath = optional(customWorkflowPath).replace('\\', '/');
    }

    public static NarrativeVideoConfiguration verticalDefaults() {
        return new NarrativeVideoConfiguration(
                720,
                1280,
                24,
                5.0,
                "PRODUCTION_SDXL_REFERENCE",
                "WAN22_TI2V_5B_BALANCED",
                "SAFE",
                "");
    }

    public String aspectRatioLabel() {
        if (width == height) return "1:1";
        return height > width ? "9:16" : "16:9";
    }

    private static String optional(String value) {
        return value == null ? "" : value.strip();
    }
}
