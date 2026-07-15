package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;
import java.util.List;

/** Result of a batch frame generation run. */
public record TheatreFrameGenerationResult(
        Path root,
        int requestedInterventions,
        int generatedFrames,
        int pendingFrames,
        List<String> warnings,
        List<TheatreGeneratedFrameCandidate> candidates,
        Path manifest
) {
    public TheatreFrameGenerationResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
