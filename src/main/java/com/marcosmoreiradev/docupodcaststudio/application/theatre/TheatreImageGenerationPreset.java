package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;

/** Presets for local theatre image generation. */
public enum TheatreImageGenerationPreset {
    TEST_4GB_SD15("Prueba 4GB", "Comfy-Org/stable-diffusion-v1-5-archive",
            "v1-5-pruned-emaonly-fp16.safetensors", 512, 512, 1, 24, 7.0, true, true, false),
    SD15_DREAMSHAPER("Estetico SD 1.5", "Lykon/DreamShaper",
            "DreamShaper_8_pruned.safetensors", 512, 512, 1, 24, 7.0, true, true, false),
    PRODUCTION_SDXL_REFERENCE("Produccion SDXL con referencias", "stabilityai/stable-diffusion-xl-base-1.0",
            "sd_xl_base_1.0.safetensors", 1024, 1024, 1, 32, 6.5, false, false, false),
    ADVANCED_FLUX_KONTEXT("Avanzado FLUX Kontext", "black-forest-labs/FLUX.1-Kontext-dev",
            "flux1-kontext-dev.safetensors", 1024, 1024, 1, 28, 3.5, false, false, false),
    /** Compatibility alias for projects created before the transverse profile catalog. */
    HIGH_QUALITY_FLUX("Alta calidad / Flux", "black-forest-labs/FLUX.1-dev",
            "flux1-dev.safetensors", 1024, 1024, 1, 28, 4.0, false, false, false),
    CUSTOM_COMFY_WORKFLOW("Workflow personalizado", "", "", 512, 512, 1, 24, 7.0, false, false, true);

    private final String displayName;
    private final String recommendedRepository;
    private final String checkpointName;
    private final int width;
    private final int height;
    private final int batchSize;
    private final int steps;
    private final double cfg;
    private final boolean lowVramRecommended;
    private final boolean sd15Compatible;
    private final boolean supportsInterpolation;

    TheatreImageGenerationPreset(String displayName, String recommendedRepository, String checkpointName,
                                 int width, int height, int batchSize, int steps, double cfg,
                                 boolean lowVramRecommended, boolean sd15Compatible, boolean supportsInterpolation) {
        this.displayName = displayName;
        this.recommendedRepository = recommendedRepository;
        this.checkpointName = checkpointName;
        this.width = width;
        this.height = height;
        this.batchSize = batchSize;
        this.steps = steps;
        this.cfg = cfg;
        this.lowVramRecommended = lowVramRecommended;
        this.sd15Compatible = sd15Compatible;
        this.supportsInterpolation = supportsInterpolation;
    }

    public String displayName() { return displayName; }
    public String recommendedRepository() { return recommendedRepository; }
    public String checkpointName() { return checkpointName; }
    public int width() { return width; }
    public int height() { return height; }
    public int batchSize() { return batchSize; }
    public int steps() { return steps; }
    public double cfg() { return cfg; }
    public boolean lowVramRecommended() { return lowVramRecommended; }
    public boolean sd15Compatible() { return sd15Compatible; }
    public boolean supportsInterpolation() { return supportsInterpolation; }

    public VisualGenerationProfile visualProfile() {
        return VisualGenerationProfile.from(name());
    }

    public boolean fluxCompatible() {
        return this == ADVANCED_FLUX_KONTEXT || this == HIGH_QUALITY_FLUX;
    }

    @Override
    public String toString() { return displayName; }
}
