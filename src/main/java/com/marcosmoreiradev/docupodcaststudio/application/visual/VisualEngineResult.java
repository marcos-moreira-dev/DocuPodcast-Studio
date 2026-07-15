package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;

/** Result of one successful local visual-engine generation. */
public record VisualEngineResult(
        Path outputPath,
        int width,
        int height,
        String promptId,
        String diagnostic
) {
    public VisualEngineResult {
        width = Math.max(0, width);
        height = Math.max(0, height);
        promptId = promptId == null ? "" : promptId.strip();
        diagnostic = diagnostic == null ? "" : diagnostic.strip();
    }
}
