package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Readiness report for the local theatre image generation engine. */
public record LocalTheatreImageSetupReadinessReport(
        Path applicationRoot,
        Path runtimeDirectory,
        Path modelDirectory,
        ModelInspectionResult modelInspection,
        ImageEngineArtifactInspectionReport artifactInspection,
        boolean runtimePrepared,
        List<String> missingRequirements,
        String userMessage
) {
    public LocalTheatreImageSetupReadinessReport(Path applicationRoot,
                                                 Path runtimeDirectory,
                                                 Path modelDirectory,
                                                 ModelInspectionResult modelInspection,
                                                 boolean runtimePrepared,
                                                 List<String> missingRequirements,
                                                 String userMessage) {
        this(applicationRoot, runtimeDirectory, modelDirectory, modelInspection, null,
                runtimePrepared, missingRequirements, userMessage);
    }

    public LocalTheatreImageSetupReadinessReport {
        missingRequirements = List.copyOf(missingRequirements == null ? List.of() : missingRequirements);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean modelUsable() {
        if (artifactInspection != null) {
            return artifactInspection.modelReady();
        }
        return modelInspection != null && modelInspection.usable();
    }

    public boolean ready() {
        return runtimePrepared && modelUsable() && missingRequirements.isEmpty();
    }

    public String statusLabel() {
        return ready() ? "Imagen IA teatral lista." : userMessage;
    }
}
