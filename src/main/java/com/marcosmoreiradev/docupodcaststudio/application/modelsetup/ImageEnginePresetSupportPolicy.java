package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;

/** Single source of truth for local visual profiles and their integrated workflows. */
public final class ImageEnginePresetSupportPolicy {
    public static final String SD15_REFERENCE_WORKFLOW = "workflows/workflow-sd15-reference.json";
    public static final String SDXL_REFERENCE_WORKFLOW = "workflows/workflow-sdxl-reference-api.json";
    public static final String FLUX_REFERENCE_WORKFLOW = "workflows/workflow-flux-reference.json";
    public static final String FLUX_KONTEXT_WORKFLOW = "workflows/workflow-flux-kontext-reference-api.json";
    public static final String CUSTOM_WORKFLOW = "workflows/workflow-custom-comfy.json";

    private ImageEnginePresetSupportPolicy() {
    }

    public static ImageEnginePresetSupport forProfile(VisualGenerationProfile profile) {
        VisualGenerationProfile selected = profile == null
                ? VisualGenerationProfile.DIAGNOSTIC_SD15
                : profile;
        return supportFor(defaultPackage(selected));
    }

    public static ImageEnginePresetSupport forPresetId(String presetId) {
        return supportFor(ImageModelPackageProfile.fromPreset(presetId));
    }

    private static ImageEnginePresetSupport supportFor(ImageModelPackageProfile packageProfile) {
        ImageModelPackageProfile selected = packageProfile == null
                ? ImageModelPackageProfile.TEST_4GB_SD15
                : packageProfile;
        VisualGenerationProfile visualProfile = VisualGenerationProfile.from(selected.presetId());
        String checkpoint = selected.checkpointName();
        return switch (selected) {
            case TEST_4GB_SD15, SD15_DREAMSHAPER -> support(
                    selected, visualProfile, checkpoint, SD15_REFERENCE_WORKFLOW,
                    24, 7.0, 1, true,
                    "El perfil " + selected.displayName() + " usa el workflow integrado SD 1.5.");
            case PRODUCTION_SDXL_REFERENCE -> support(
                    selected, visualProfile, checkpoint, SDXL_REFERENCE_WORKFLOW,
                    32, 6.5, 1, true,
                    "El perfil SDXL usa referencias separadas para identidad, objetos, camara y boceto. "
                            + "Requiere el paquete productivo completo.");
            case ADVANCED_FLUX_KONTEXT -> support(
                    selected, visualProfile, checkpoint, FLUX_KONTEXT_WORKFLOW,
                    28, 3.5, 1, true,
                    "FLUX Kontext usa un workflow multirreferencia y requiere licencia aceptada, "
                            + "modelo Kontext, VAE, CLIP-L y T5XXL.");
            case HIGH_QUALITY_FLUX -> support(
                    selected, visualProfile, checkpoint, FLUX_REFERENCE_WORKFLOW,
                    28, 4.0, 1, true,
                    "El perfil " + selected.displayName()
                            + " usa el workflow integrado por componentes. Requiere licencia aceptada, "
                            + "modelo, VAE, CLIP-L y T5XXL.");
            case CUSTOM_COMFY_WORKFLOW -> support(
                    selected, visualProfile, checkpoint, CUSTOM_WORKFLOW,
                    24, 7.0, 1, false,
                    "El perfil " + selected.displayName()
                            + " requiere importar un workflow ComfyUI propio en models/image/"
                            + CUSTOM_WORKFLOW + ". El flujo integrado no ejecuta workflows personalizados "
                            + "automaticamente.");
        };
    }

    private static ImageEnginePresetSupport support(ImageModelPackageProfile packageProfile,
                                                    VisualGenerationProfile visualProfile,
                                                    String checkpoint,
                                                    String workflow,
                                                    int steps,
                                                    double cfg,
                                                    int batchSize,
                                                    boolean builtIn,
                                                    String userMessage) {
        return new ImageEnginePresetSupport(
                packageProfile.presetId(),
                visualProfile,
                packageProfile,
                builtIn,
                checkpoint,
                workflow,
                steps,
                cfg,
                batchSize,
                userMessage,
                diagnostic(packageProfile.presetId(), visualProfile, checkpoint, workflow, builtIn));
    }

    private static ImageModelPackageProfile defaultPackage(VisualGenerationProfile profile) {
        return switch (profile) {
            case DIAGNOSTIC_SD15 -> ImageModelPackageProfile.TEST_4GB_SD15;
            case PRODUCTION_SDXL_REFERENCE -> ImageModelPackageProfile.PRODUCTION_SDXL_REFERENCE;
            case ADVANCED_FLUX_KONTEXT -> ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT;
            case CUSTOM_COMFY_WORKFLOW -> ImageModelPackageProfile.CUSTOM_COMFY_WORKFLOW;
        };
    }

    private static String diagnostic(String presetId,
                                     VisualGenerationProfile profile,
                                     String checkpoint,
                                     String workflow,
                                     boolean builtInWorkflow) {
        return "preset=" + presetId
                + "\nprofile=" + profile.name()
                + "\ncheckpoint=" + checkpoint
                + "\nworkflow=" + workflow
                + "\nbuiltInWorkflow=" + builtInWorkflow;
    }
}
