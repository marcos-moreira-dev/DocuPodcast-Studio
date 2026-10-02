package com.marcosmoreiradev.docupodcaststudio.application.settings;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualResolutionProfile;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageSuperResolutionRequest;

/** Global defaults for optional existing-image AI super-resolution. */
public record ImageSuperResolutionSettings(
        String generationProfile,
        boolean enabled,
        String targetProfile,
        String modelName,
        boolean refinementEnabled,
        String refinementPreset,
        String refinementEngineId) {

    public static final String DEFAULT_REFINEMENT_ENGINE = "comfyui-controlnet-tile";

    public ImageSuperResolutionSettings {
        generationProfile = VisualResolutionProfile.from(
                generationProfile, VisualResolutionProfile.P1080).name();
        targetProfile = VisualResolutionProfile.from(
                targetProfile, VisualResolutionProfile.QHD_2K).name();
        modelName = modelName == null || modelName.isBlank()
                ? ImageSuperResolutionRequest.DEFAULT_MODEL : modelName.strip();
        refinementPreset = normalizePreset(refinementPreset);
        refinementEngineId = refinementEngineId == null || refinementEngineId.isBlank()
                ? DEFAULT_REFINEMENT_ENGINE : refinementEngineId.strip();
    }

    public ImageSuperResolutionSettings(
            String generationProfile,
            boolean enabled,
            String targetProfile,
            String modelName) {
        this(generationProfile, enabled, targetProfile, modelName,
                false, "conservative", DEFAULT_REFINEMENT_ENGINE);
    }

    public static ImageSuperResolutionSettings defaults() {
        return new ImageSuperResolutionSettings(
                VisualResolutionProfile.P1080.name(), false,
                VisualResolutionProfile.QHD_2K.name(), ImageSuperResolutionRequest.DEFAULT_MODEL,
                false, "conservative", DEFAULT_REFINEMENT_ENGINE);
    }

    public VisualResolutionProfile generationResolution() {
        return VisualResolutionProfile.from(generationProfile, VisualResolutionProfile.P1080);
    }

    public VisualResolutionProfile targetResolution() {
        return VisualResolutionProfile.from(targetProfile, VisualResolutionProfile.QHD_2K);
    }

    private static String normalizePreset(String value) {
        return "balanced".equalsIgnoreCase(value == null ? "" : value.strip())
                ? "balanced" : "conservative";
    }
}
