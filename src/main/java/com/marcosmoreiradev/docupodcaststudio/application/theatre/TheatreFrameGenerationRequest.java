package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;

import java.nio.file.Path;
import java.util.Objects;

/** User request for generating theatrical still frames from the intervention map. */
public record TheatreFrameGenerationRequest(
        TheatreFrameGenerationScope scope,
        FrameGenerationMode mode,
        Path outputDirectory,
        TheatreImageGenerationPreset preset,
        ImageEnhancementOutputProfile outputProfile,
        TheatreImageAspectRatio aspectRatio,
        ComfyUiConnectionSettings connectionSettings,
        boolean overwriteExisting
) {
    public TheatreFrameGenerationRequest(TheatreFrameGenerationScope scope,
                                         FrameGenerationMode mode,
                                         Path outputDirectory,
                                         TheatreImageGenerationPreset preset,
                                         ComfyUiConnectionSettings connectionSettings,
                                         boolean overwriteExisting) {
        this(scope, mode, outputDirectory, preset, ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9, connectionSettings, overwriteExisting);
    }

    public TheatreFrameGenerationRequest(TheatreFrameGenerationScope scope,
                                         FrameGenerationMode mode,
                                         Path outputDirectory,
                                         TheatreImageGenerationPreset preset,
                                         ImageEnhancementOutputProfile outputProfile,
                                         ComfyUiConnectionSettings connectionSettings,
                                         boolean overwriteExisting) {
        this(scope, mode, outputDirectory, preset, outputProfile,
                TheatreImageAspectRatio.WIDE_16_9, connectionSettings, overwriteExisting);
    }

    public TheatreFrameGenerationRequest {
        scope = scope == null ? TheatreFrameGenerationScope.all() : scope;
        mode = mode == null ? FrameGenerationMode.SINGLE : mode;
        preset = preset == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        outputProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        aspectRatio = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        connectionSettings = Objects.requireNonNull(connectionSettings, "connectionSettings");
    }
}
