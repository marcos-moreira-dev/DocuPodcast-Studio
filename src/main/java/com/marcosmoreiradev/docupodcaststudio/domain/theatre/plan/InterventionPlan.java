package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import java.util.List;

public record InterventionPlan(
        String characterName,
        String sceneName,
        String origin,
        String destination,
        String interactionTarget,
        List<String> images,
        String tono,
        String cameraCue,
        boolean applyCamera,
        String stageBackdrop,
        boolean clearStageBackdrop,
        List<String> simultaneousVoiceNames,
        String aiContextText,
        int sequenceIndex,
        int sceneIndex) {
    public InterventionPlan {
        characterName = normalize(characterName);
        sceneName = normalize(sceneName);
        origin = normalize(origin);
        destination = normalize(destination);
        interactionTarget = normalize(interactionTarget);
        images = images == null ? List.of() : List.copyOf(images);
        tono = normalize(tono);
        cameraCue = normalize(cameraCue);
        stageBackdrop = normalize(stageBackdrop);
        simultaneousVoiceNames = simultaneousVoiceNames == null ? List.of() : simultaneousVoiceNames.stream()
                .map(InterventionPlan::normalize)
                .filter(value -> !value.isBlank())
                .toList();
        aiContextText = normalize(aiContextText);
    }

    public InterventionPlan(
            String characterName,
            String sceneName,
            String origin,
            String destination,
            String interactionTarget,
            List<String> images,
            String tono,
            int sequenceIndex,
            int sceneIndex) {
        this(characterName, sceneName, origin, destination, interactionTarget, images, tono,
                "", true, "", false, List.of(), "", sequenceIndex, sceneIndex);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
