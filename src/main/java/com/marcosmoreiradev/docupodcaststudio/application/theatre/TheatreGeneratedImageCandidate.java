package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;

/** Generated image imported into the project; approval/assignment is explicit unless requested by the user. */
public record TheatreGeneratedImageCandidate(
        String unitId,
        String sceneId,
        String interventionId,
        String segmentId,
        String assetId,
        Path outputPath,
        boolean approved
) {
}
