package com.marcosmoreiradev.docupodcaststudio.domain.project;

/** Optional project overrides; blank/null values inherit the global application defaults. */
public record ProjectVisualProcessingSettings(
        String generationProfile,
        Boolean upscaleEnabled,
        String upscaleTargetProfile,
        String upscaleModelName,
        Boolean refineAfterUpscale,
        String refinementPreset,
        String refinementEngineId) {
    public ProjectVisualProcessingSettings {
        generationProfile = clean(generationProfile);
        upscaleTargetProfile = clean(upscaleTargetProfile);
        upscaleModelName = clean(upscaleModelName);
        refinementPreset = clean(refinementPreset);
        refinementEngineId = clean(refinementEngineId);
    }

    public ProjectVisualProcessingSettings(
            String generationProfile,
            Boolean upscaleEnabled,
            String upscaleTargetProfile,
            String upscaleModelName) {
        this(generationProfile, upscaleEnabled, upscaleTargetProfile, upscaleModelName,
                null, "", "");
    }

    public static ProjectVisualProcessingSettings inherited() {
        return new ProjectVisualProcessingSettings("", null, "", "", null, "", "");
    }

    public boolean hasOverrides() {
        return !generationProfile.isBlank() || upscaleEnabled != null
                || !upscaleTargetProfile.isBlank() || !upscaleModelName.isBlank()
                || refineAfterUpscale != null || !refinementPreset.isBlank()
                || !refinementEngineId.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
