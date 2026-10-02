package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ImageGenerationResult(List<Path> images, Map<String, String> diagnostics) {
    public ImageGenerationResult {
        images = images == null ? List.of() : List.copyOf(images);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
