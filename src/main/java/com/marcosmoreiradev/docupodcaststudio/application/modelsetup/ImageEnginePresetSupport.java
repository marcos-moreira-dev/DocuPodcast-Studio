package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;

/** Declares the concrete local workflow contract for an image generation preset. */
public record ImageEnginePresetSupport(
        TheatreImageGenerationPreset preset,
        ImageModelPackageProfile profile,
        boolean builtInWorkflowAvailable,
        String checkpointName,
        String workflowName,
        String userMessage,
        String diagnostic
) {
    public ImageEnginePresetSupport {
        preset = preset == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        profile = profile == null ? ImageModelPackageProfile.TEST_4GB_SD15 : profile;
        checkpointName = checkpointName == null ? "" : checkpointName.strip();
        workflowName = workflowName == null ? "" : workflowName.strip();
        userMessage = userMessage == null ? "" : userMessage.strip();
        diagnostic = diagnostic == null ? "" : diagnostic.strip();
    }

    public boolean requiresImportedWorkflow() {
        return !builtInWorkflowAvailable;
    }
}
