package com.marcosmoreiradev.docupodcaststudio.application.image;

/** Pipeline family required by an output profile. */
public enum ImageEnhancementPipelineProfile {
    LIGHT_UPSCALE("Escalado ligero", false, false, false, false),
    RESTORE_UPSCALE("Restauracion + upscale", false, false, true, false),
    FLUX_OUTPAINT("Flux outpainting moderado", true, false, true, true),
    TILED_PROFESSIONAL("Pipeline profesional por tiles", true, true, true, true),
    CUSTOM_WORKFLOW("Workflow personalizado", true, false, false, true);

    private final String displayName;
    private final boolean requiresFlux;
    private final boolean requiresTiling;
    private final boolean requiresAdvancedComponents;
    private final boolean supportsLora;

    ImageEnhancementPipelineProfile(String displayName,
                                    boolean requiresFlux,
                                    boolean requiresTiling,
                                    boolean requiresAdvancedComponents,
                                    boolean supportsLora) {
        this.displayName = displayName;
        this.requiresFlux = requiresFlux;
        this.requiresTiling = requiresTiling;
        this.requiresAdvancedComponents = requiresAdvancedComponents;
        this.supportsLora = supportsLora;
    }

    public String displayName() {
        return displayName;
    }

    public boolean requiresFlux() {
        return requiresFlux;
    }

    public boolean requiresTiling() {
        return requiresTiling;
    }

    public boolean requiresAdvancedComponents() {
        return requiresAdvancedComponents;
    }

    public boolean supportsLora() {
        return supportsLora;
    }
}
