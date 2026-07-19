package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.Locale;

/** Shared quality/runtime profiles available to every visual-generation consumer. */
public enum VisualGenerationProfile {
    DIAGNOSTIC_SD15("Diagnostico SD 1.5", false, false),
    PRODUCTION_SDXL_REFERENCE("Produccion SDXL con referencias", true, false),
    ADVANCED_FLUX_KONTEXT("Avanzado FLUX Kontext", true, true),
    CUSTOM_COMFY_WORKFLOW("Workflow ComfyUI personalizado", false, false);

    private final String displayName;
    private final boolean referenceConditioning;
    private final boolean explicitLicenseAcceptance;

    VisualGenerationProfile(String displayName,
                            boolean referenceConditioning,
                            boolean explicitLicenseAcceptance) {
        this.displayName = displayName;
        this.referenceConditioning = referenceConditioning;
        this.explicitLicenseAcceptance = explicitLicenseAcceptance;
    }

    public String displayName() {
        return displayName;
    }

    public boolean referenceConditioning() {
        return referenceConditioning;
    }

    public boolean explicitLicenseAcceptance() {
        return explicitLicenseAcceptance;
    }

    public static VisualGenerationProfile from(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TEST_4GB_SD15", "SD15_DREAMSHAPER", "DIAGNOSTIC_SD15" -> DIAGNOSTIC_SD15;
            case "PRODUCTION_SDXL_REFERENCE", "SDXL_REFERENCE" -> PRODUCTION_SDXL_REFERENCE;
            case "HIGH_QUALITY_FLUX", "ADVANCED_FLUX_KONTEXT", "FLUX_KONTEXT" -> ADVANCED_FLUX_KONTEXT;
            case "CUSTOM_COMFY_WORKFLOW" -> CUSTOM_COMFY_WORKFLOW;
            default -> DIAGNOSTIC_SD15;
        };
    }
}
