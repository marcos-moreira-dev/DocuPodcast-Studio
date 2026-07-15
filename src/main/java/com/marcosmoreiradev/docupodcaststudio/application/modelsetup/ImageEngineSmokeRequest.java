package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;

import java.nio.file.Path;

/** User-selected parameters for a local theatre image engine smoke generation. */
public record ImageEngineSmokeRequest(
        TheatreImageGenerationPreset preset,
        ImageEnhancementOutputProfile outputProfile,
        TheatreImageAspectRatio aspectRatio,
        String promptText,
        int steps,
        Path outputDirectory
) {
    public ImageEngineSmokeRequest {
        preset = preset == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        outputProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        aspectRatio = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        promptText = promptText == null ? "" : promptText.strip();
        steps = Math.max(4, Math.min(80, steps));
    }

    public static ImageEngineSmokeRequest defaults() {
        return new ImageEngineSmokeRequest(
                TheatreImageGenerationPreset.TEST_4GB_SD15,
                ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9,
                "",
                TheatreImageGenerationPreset.TEST_4GB_SD15.steps(),
                null);
    }

    public int targetWidth() {
        return aspectRatio.widthFor(outputProfile);
    }

    public int targetHeight() {
        return aspectRatio.heightFor(outputProfile);
    }
}
