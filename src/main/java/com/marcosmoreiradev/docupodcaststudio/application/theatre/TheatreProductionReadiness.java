package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.List;

/** Export and review readiness for Produccion teatral. */
public record TheatreProductionReadiness(
        int actCount,
        int sceneCount,
        int interventionCount,
        int linkedInterventionCount,
        int orphanInterventionCount,
        int audioReadyCount,
        int audioMissingCount,
        int visualReadyCount,
        int visualMissingCount,
        int characterCount,
        int objectCount,
        int textPlacementCount,
        int spatialPositionCount,
        int actionCount,
        int brokenReferenceCount,
        List<String> workMissingRequirements,
        List<String> spatialMapMissingRequirements,
        List<String> portionMissingRequirements,
        List<String> warnings
) {
    public TheatreProductionReadiness {
        actCount = Math.max(0, actCount);
        sceneCount = Math.max(0, sceneCount);
        interventionCount = Math.max(0, interventionCount);
        linkedInterventionCount = Math.max(0, linkedInterventionCount);
        orphanInterventionCount = Math.max(0, orphanInterventionCount);
        audioReadyCount = Math.max(0, audioReadyCount);
        audioMissingCount = Math.max(0, audioMissingCount);
        visualReadyCount = Math.max(0, visualReadyCount);
        visualMissingCount = Math.max(0, visualMissingCount);
        characterCount = Math.max(0, characterCount);
        objectCount = Math.max(0, objectCount);
        textPlacementCount = Math.max(0, textPlacementCount);
        spatialPositionCount = Math.max(0, spatialPositionCount);
        actionCount = Math.max(0, actionCount);
        brokenReferenceCount = Math.max(0, brokenReferenceCount);
        workMissingRequirements = workMissingRequirements == null ? List.of() : List.copyOf(workMissingRequirements);
        spatialMapMissingRequirements = spatialMapMissingRequirements == null ? List.of() : List.copyOf(spatialMapMissingRequirements);
        portionMissingRequirements = portionMissingRequirements == null ? List.of() : List.copyOf(portionMissingRequirements);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean workExportable() {
        return workMissingRequirements.isEmpty();
    }

    public boolean spatialMapExportable() {
        return spatialMapMissingRequirements.isEmpty();
    }

    public boolean portionExportable() {
        return portionMissingRequirements.isEmpty();
    }
}
