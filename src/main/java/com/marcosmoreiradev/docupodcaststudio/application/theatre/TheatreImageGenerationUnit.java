package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;

/** A single theatre intervention/fragment unit that can request a generated image. */
public record TheatreImageGenerationUnit(
        String actId,
        String actName,
        String sceneId,
        String sceneName,
        String interventionId,
        String segmentId,
        String speaker,
        String fullText,
        String spatialContextText,
        Path contextPackage
) {
    public TheatreImageGenerationUnit(String actId,
                                      String actName,
                                      String sceneId,
                                      String sceneName,
                                      String interventionId,
                                      String segmentId,
                                      String speaker,
                                      String fullText,
                                      Path contextPackage) {
        this(actId, actName, sceneId, sceneName, interventionId, segmentId, speaker, fullText, "", contextPackage);
    }
}
