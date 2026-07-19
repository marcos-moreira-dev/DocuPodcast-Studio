package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;

/** Declares the concrete local workflow contract for an image generation preset. */
public record ImageEnginePresetSupport(
        String presetId,
        VisualGenerationProfile visualProfile,
        ImageModelPackageProfile modelPackageProfile,
        boolean builtInWorkflowAvailable,
        String checkpointName,
        String workflowName,
        int defaultSteps,
        double defaultCfg,
        int defaultBatchSize,
        String userMessage,
        String diagnostic
) {
    public ImageEnginePresetSupport {
        presetId = presetId == null ? "" : presetId.strip();
        visualProfile = visualProfile == null ? VisualGenerationProfile.DIAGNOSTIC_SD15 : visualProfile;
        modelPackageProfile = modelPackageProfile == null
                ? ImageModelPackageProfile.TEST_4GB_SD15
                : modelPackageProfile;
        checkpointName = checkpointName == null ? "" : checkpointName.strip();
        workflowName = workflowName == null ? "" : workflowName.strip();
        defaultSteps = Math.max(4, Math.min(80, defaultSteps));
        defaultCfg = Math.max(1.0, Math.min(20.0, defaultCfg));
        defaultBatchSize = Math.max(1, Math.min(8, defaultBatchSize));
        userMessage = userMessage == null ? "" : userMessage.strip();
        diagnostic = diagnostic == null ? "" : diagnostic.strip();
    }

    public boolean requiresImportedWorkflow() {
        return !builtInWorkflowAvailable;
    }

    public boolean fluxCompatible() {
        return modelPackageProfile == ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT
                || modelPackageProfile == ImageModelPackageProfile.HIGH_QUALITY_FLUX;
    }

    public boolean kontextWorkflow() {
        return modelPackageProfile == ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT;
    }

    public String displayName() {
        return modelPackageProfile.displayName();
    }
}
