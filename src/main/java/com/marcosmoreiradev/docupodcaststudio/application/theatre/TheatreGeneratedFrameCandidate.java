package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;

/** A generated frame imported as a project asset and awaiting explicit approval. */
public record TheatreGeneratedFrameCandidate(
        String unitId,
        String sceneId,
        String interventionId,
        String segmentId,
        int frameIndex,
        boolean transitionFrame,
        String nextInterventionId,
        String assetId,
        Path outputPath,
        boolean approved
) {
}
