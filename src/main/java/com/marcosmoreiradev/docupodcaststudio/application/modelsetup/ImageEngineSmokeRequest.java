package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;

import java.nio.file.Path;

/** User-selected parameters for a category-neutral local visual-engine smoke generation. */
public record ImageEngineSmokeRequest(
        VisualGenerationProfile profile,
        ImageEnhancementOutputProfile outputProfile,
        VisualAspectRatio aspectRatio,
        String promptText,
        int steps,
        Path outputDirectory
) {
    public ImageEngineSmokeRequest {
        profile = profile == null ? VisualGenerationProfile.DIAGNOSTIC_SD15 : profile;
        outputProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        aspectRatio = aspectRatio == null ? VisualAspectRatio.WIDE_16_9 : aspectRatio;
        promptText = promptText == null ? "" : promptText.strip();
        steps = Math.max(4, Math.min(80, steps));
    }

    public static ImageEngineSmokeRequest defaults() {
        return new ImageEngineSmokeRequest(
                VisualGenerationProfile.DIAGNOSTIC_SD15,
                ImageEnhancementOutputProfile.FHD_1080,
                VisualAspectRatio.WIDE_16_9,
                "",
                24,
                null);
    }

    public int targetWidth() {
        return aspectRatio.widthFor(outputProfile);
    }

    public int targetHeight() {
        return aspectRatio.heightFor(outputProfile);
    }
}
