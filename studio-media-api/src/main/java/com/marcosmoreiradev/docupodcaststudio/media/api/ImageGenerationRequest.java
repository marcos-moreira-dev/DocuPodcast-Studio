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
        Map<String, String> options,
        EnginePresetId presetId,
        List<MediaReference> mediaReferences,
        long seed,
        int batchSize) {
    public ImageGenerationRequest {
        prompt = prompt == null ? "" : prompt.strip();
        negativePrompt = negativePrompt == null ? "" : negativePrompt.strip();
        width = Math.max(64, width);
        height = Math.max(64, height);
        references = references == null ? List.of() : List.copyOf(references);
        filenamePrefix = filenamePrefix == null || filenamePrefix.isBlank() ? "generated" : filenamePrefix.strip();
        options = options == null ? Map.of() : Map.copyOf(options);
        presetId = presetId == null ? EnginePresetId.AUTO : presetId;
        mediaReferences = mediaReferences == null ? List.of() : List.copyOf(mediaReferences);
        seed = Math.max(0L, seed);
        batchSize = Math.max(1, Math.min(32, batchSize));
        if (prompt.isBlank()) throw new IllegalArgumentException("image prompt is required");
        if (outputDirectory == null) throw new IllegalArgumentException("image output directory is required");
    }

    public ImageGenerationRequest(String prompt, String negativePrompt, int width, int height,
                                  List<Path> references, Path outputDirectory, String filenamePrefix,
                                  Map<String, String> options) {
        this(prompt, negativePrompt, width, height, references, outputDirectory, filenamePrefix,
                options, EnginePresetId.AUTO, List.of(), 0L, 1);
    }

    public ImageGenerationRequest(String prompt, String negativePrompt, int width, int height,
                                  List<MediaReference> mediaReferences, Path outputDirectory,
                                  String filenamePrefix, EnginePresetId presetId, long seed, int batchSize) {
        this(prompt, negativePrompt, width, height,
                mediaReferences == null ? List.of() : mediaReferences.stream().map(MediaReference::file).toList(),
                outputDirectory, filenamePrefix, Map.of(), presetId, mediaReferences, seed, batchSize);
    }
}
