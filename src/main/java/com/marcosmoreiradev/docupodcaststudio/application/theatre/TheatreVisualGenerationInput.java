package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBinding;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualOutputTarget;

/** Theatre-owned input translated by the shared visual context adapter. */
public record TheatreVisualGenerationInput(
        TheatreVisualGenerationContext context,
        String prompt,
        String negativePrompt,
        int width,
        int height,
        VisualGenerationProfile profile,
        VisualComputeBinding computeBinding,
        VisualOutputTarget outputTarget
) {
}
