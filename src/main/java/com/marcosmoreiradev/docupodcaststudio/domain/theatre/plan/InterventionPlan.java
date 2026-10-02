package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreInterventionState;

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
        int sceneIndex,
        String interventionId,
        TheatreInterventionState.InheritanceMode inheritanceMode,
        String inheritsFromInterventionId,
        List<TheatreInterventionState.CharacterState> characterStates,
        List<TheatreInterventionState.ObjectState> objectStates,
        List<TheatreInterventionState.StageEvent> stageEvents,
        String microexpression,
        String emoji,
        String spokenText) {
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
        interventionId = normalize(interventionId);
        inheritanceMode = inheritanceMode == null ? TheatreInterventionState.InheritanceMode.PREVIOUS : inheritanceMode;
        inheritsFromInterventionId = normalize(inheritsFromInterventionId);
        characterStates = characterStates == null ? List.of() : List.copyOf(characterStates);
        objectStates = objectStates == null ? List.of() : List.copyOf(objectStates);
        stageEvents = stageEvents == null ? List.of() : List.copyOf(stageEvents);
        microexpression = normalize(microexpression);
        emoji = normalize(emoji);
        spokenText = normalize(spokenText);
    }

    public InterventionPlan(String characterName, String sceneName, String origin, String destination,
                            String interactionTarget, List<String> images, String tono, String cameraCue,
                            boolean applyCamera, String stageBackdrop, boolean clearStageBackdrop,
                            List<String> simultaneousVoiceNames, String aiContextText, int sequenceIndex, int sceneIndex,
                            String interventionId, TheatreInterventionState.InheritanceMode inheritanceMode,
                            String inheritsFromInterventionId, List<TheatreInterventionState.CharacterState> characterStates,
                            List<TheatreInterventionState.ObjectState> objectStates, List<TheatreInterventionState.StageEvent> stageEvents,
                            String microexpression, String emoji) {
        this(characterName, sceneName, origin, destination, interactionTarget, images, tono, cameraCue,
                applyCamera, stageBackdrop, clearStageBackdrop, simultaneousVoiceNames, aiContextText, sequenceIndex,
                sceneIndex, interventionId, inheritanceMode, inheritsFromInterventionId, characterStates, objectStates,
                stageEvents, microexpression, emoji, "");
    }

    public InterventionPlan withSpokenText(String text) {
        return new InterventionPlan(characterName, sceneName, origin, destination, interactionTarget, images, tono,
                cameraCue, applyCamera, stageBackdrop, clearStageBackdrop, simultaneousVoiceNames, aiContextText,
                sequenceIndex, sceneIndex, interventionId, inheritanceMode, inheritsFromInterventionId,
                characterStates, objectStates, stageEvents, microexpression, emoji, text);
    }

    public String stableInterventionId() {
        if (interventionId.isBlank()) return "INTERVENCION-" + sequenceIndex;
        String id = interventionId.toUpperCase(java.util.Locale.ROOT);
        return id.matches("\\d+") ? "INTERVENCION-" + Integer.parseInt(id) : id;
    }

    public boolean stageDirection() {
        return "ACOTACION".equalsIgnoreCase(characterName)
                || "ACOTACIÓN".equalsIgnoreCase(characterName)
                || "DIDASCALIA".equalsIgnoreCase(characterName);
    }

    public InterventionPlan withSimultaneousVoiceNames(List<String> names) {
        return new InterventionPlan(characterName, sceneName, origin, destination, interactionTarget, images, tono,
                cameraCue, applyCamera, stageBackdrop, clearStageBackdrop, names, aiContextText,
                sequenceIndex, sceneIndex, interventionId, inheritanceMode, inheritsFromInterventionId,
                characterStates, objectStates, stageEvents, microexpression, emoji, spokenText);
    }

    public InterventionPlan(String characterName, String sceneName, String origin, String destination,
                            String interactionTarget, List<String> images, String tono, String cameraCue,
                            boolean applyCamera, String stageBackdrop, boolean clearStageBackdrop,
                            List<String> simultaneousVoiceNames, String aiContextText, int sequenceIndex,
                            int sceneIndex) {
        this(characterName, sceneName, origin, destination, interactionTarget, images, tono, cameraCue,
                applyCamera, stageBackdrop, clearStageBackdrop, simultaneousVoiceNames, aiContextText,
                sequenceIndex, sceneIndex, "", TheatreInterventionState.InheritanceMode.PREVIOUS, "",
                List.of(), List.of(), List.of(), "", "");
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
