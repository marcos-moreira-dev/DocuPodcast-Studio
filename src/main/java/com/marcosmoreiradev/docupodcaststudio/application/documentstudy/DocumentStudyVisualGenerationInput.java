package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBinding;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualOutputTarget;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualReferenceBundle;

import java.util.Map;

/** Future documentary consumer input for the shared visual engine. */
public record DocumentStudyVisualGenerationInput(
        String prompt,
        String negativePrompt,
        VisualReferenceBundle references,
        int width,
        int height,
        VisualGenerationProfile profile,
        VisualComputeBinding computeBinding,
        VisualOutputTarget outputTarget,
        Map<String, String> metadata
) {
}
