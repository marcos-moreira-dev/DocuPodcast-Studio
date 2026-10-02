package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** Provider-neutral request that enhances an existing image without invoking image generation. */
public record ImageSuperResolutionRequest(
        Path source,
        Path outputDirectory,
        String filenamePrefix,
        int targetWidth,
        int targetHeight,
        String modelName,
        boolean containWithoutCrop,
        Map<String, String> options) {

    public static final String DEFAULT_MODEL = "RealESRGAN_x4plus.pth";

    public ImageSuperResolutionRequest {
        if (source == null) throw new IllegalArgumentException("source image is required");
        if (outputDirectory == null) throw new IllegalArgumentException("output directory is required");
        filenamePrefix = filenamePrefix == null || filenamePrefix.isBlank()
                ? "upscaled" : filenamePrefix.strip();
        targetWidth = Math.max(64, targetWidth);
        targetHeight = Math.max(64, targetHeight);
        modelName = modelName == null || modelName.isBlank() ? DEFAULT_MODEL : modelName.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
