package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ImageGenerationRequest(
        String prompt,
        String negativePrompt,
        int width,
        int height,
        List<Path> references,
        Path outputDirectory,
        String filenamePrefix,
        Map<String, String> options) {
    public ImageGenerationRequest {
        prompt = prompt == null ? "" : prompt.strip();
        negativePrompt = negativePrompt == null ? "" : negativePrompt.strip();
        width = Math.max(64, width);
        height = Math.max(64, height);
        references = references == null ? List.of() : List.copyOf(references);
        filenamePrefix = filenamePrefix == null || filenamePrefix.isBlank() ? "generated" : filenamePrefix.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
        if (prompt.isBlank()) throw new IllegalArgumentException("image prompt is required");
        if (outputDirectory == null) throw new IllegalArgumentException("image output directory is required");
    }
}
