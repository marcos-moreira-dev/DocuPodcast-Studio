package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;

import java.util.Locale;

/** Single source of truth for which local image presets can run through the integrated workflow. */
public final class ImageEnginePresetSupportPolicy {
    public static final String SD15_REFERENCE_WORKFLOW = "workflows/workflow-sd15-reference.json";
    public static final String FLUX_REFERENCE_WORKFLOW = "workflows/workflow-flux-reference.json";
    public static final String CUSTOM_WORKFLOW = "workflows/workflow-custom-comfy.json";

    private ImageEnginePresetSupportPolicy() {
    }

    public static ImageEnginePresetSupport forPreset(TheatreImageGenerationPreset preset) {
        TheatreImageGenerationPreset selected = preset == null
                ? TheatreImageGenerationPreset.TEST_4GB_SD15
                : preset;
        return supportFor(selected, ImageModelPackageProfile.fromPreset(selected.name()));
    }

    public static ImageEnginePresetSupport forPresetId(String presetId) {
        String normalized = presetId == null ? "" : presetId.strip().toUpperCase(Locale.ROOT);
        TheatreImageGenerationPreset selected = TheatreImageGenerationPreset.TEST_4GB_SD15;
        for (TheatreImageGenerationPreset candidate : TheatreImageGenerationPreset.values()) {
            if (candidate.name().equalsIgnoreCase(normalized)) {
                selected = candidate;
                break;
            }
        }
        return supportFor(selected, ImageModelPackageProfile.fromPreset(normalized));
    }

    private static ImageEnginePresetSupport supportFor(TheatreImageGenerationPreset preset,
                                                       ImageModelPackageProfile profile) {
        String checkpoint = !profile.checkpointName().isBlank()
                ? profile.checkpointName()
                : preset.checkpointName();
        if (preset.sd15Compatible()) {
            return new ImageEnginePresetSupport(
                    preset,
                    profile,
                    true,
                    checkpoint,
                    SD15_REFERENCE_WORKFLOW,
                    "El preset " + preset.displayName() + " usa el workflow integrado SD 1.5.",
                    diagnostic(preset, checkpoint, SD15_REFERENCE_WORKFLOW, true));
        }
        if (preset == TheatreImageGenerationPreset.HIGH_QUALITY_FLUX) {
            return new ImageEnginePresetSupport(
                    preset,
                    profile,
                    true,
                    checkpoint,
                    FLUX_REFERENCE_WORKFLOW,
                    "El preset " + preset.displayName()
                            + " usa el workflow integrado por componentes. Requiere licencia aceptada, modelo, VAE, CLIP-L y T5XXL.",
                    diagnostic(preset, checkpoint, FLUX_REFERENCE_WORKFLOW, true));
        }
        return new ImageEnginePresetSupport(
                preset,
                profile,
                false,
                checkpoint,
                CUSTOM_WORKFLOW,
                "El preset " + preset.displayName()
                        + " requiere importar un workflow ComfyUI propio y conectarlo de forma explicita: models/image/"
                        + CUSTOM_WORKFLOW + ". El flujo integrado actual no ejecuta workflows personalizados automaticamente.",
                diagnostic(preset, checkpoint, CUSTOM_WORKFLOW, false));
    }

    private static String diagnostic(TheatreImageGenerationPreset preset,
                                     String checkpoint,
                                     String workflow,
                                     boolean builtInWorkflow) {
        return "preset=" + preset.name()
                + "\ncheckpoint=" + checkpoint
                + "\nworkflow=" + workflow
                + "\nbuiltInWorkflow=" + builtInWorkflow;
    }
}
