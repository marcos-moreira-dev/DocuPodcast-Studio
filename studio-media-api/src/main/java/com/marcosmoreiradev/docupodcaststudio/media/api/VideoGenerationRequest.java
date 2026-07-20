package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** General text-to-video request with optional image-to-video continuity frame. */
public record VideoGenerationRequest(
        String prompt,
        String negativePrompt,
        Path initialImage,
        List<MediaReference> references,
        int width,
        int height,
        int framesPerSecond,
        double durationSeconds,
        long seed,
        EnginePresetId presetId,
        Path outputDirectory,
        String filenamePrefix,
        Map<String, String> options) {
    public VideoGenerationRequest {
        prompt = prompt == null ? "" : prompt.strip();
        negativePrompt = negativePrompt == null ? "" : negativePrompt.strip();
        initialImage = initialImage == null ? null : initialImage.toAbsolutePath().normalize();
        references = references == null ? List.of() : List.copyOf(references);
        width = Math.max(320, width);
        height = Math.max(180, height);
        framesPerSecond = Math.max(1, Math.min(120, framesPerSecond));
        durationSeconds = Double.isFinite(durationSeconds) ? Math.max(0.1, durationSeconds) : 5.0;
        seed = Math.max(0L, seed);
        presetId = presetId == null ? EnginePresetId.AUTO : presetId;
        if (outputDirectory == null) throw new IllegalArgumentException("video output directory is required");
        outputDirectory = outputDirectory.toAbsolutePath().normalize();
        filenamePrefix = filenamePrefix == null || filenamePrefix.isBlank() ? "generated-video" : filenamePrefix.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
        if (prompt.isBlank() && initialImage == null) {
            throw new IllegalArgumentException("video prompt or initial image is required");
        }
    }
}
