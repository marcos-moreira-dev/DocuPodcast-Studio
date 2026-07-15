package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;

/** One unit inside a local theatre image generation job. */
public record TheatreImageGenerationJobItem(
        String interventionId,
        String sceneId,
        String label,
        String status,
        Path outputPath) {
    public TheatreImageGenerationJobItem {
        interventionId = interventionId == null ? "" : interventionId;
        sceneId = sceneId == null ? "" : sceneId;
        label = label == null ? "" : label;
        status = status == null ? "" : status;
    }
}
