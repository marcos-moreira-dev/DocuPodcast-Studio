package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Product-level readiness for the managed local theatre image engine. */
public record ImageEngineReadinessReport(
        ImageEngineRuntimeState state,
        Path applicationRoot,
        Path runtimeDirectory,
        Path modelDirectory,
        boolean runtimePrepared,
        boolean modelInstalled,
        boolean engineResponding,
        ImageEngineArtifactInspectionReport artifactInspection,
        List<String> missingRequirements,
        String userMessage,
        String nextAction
) {
    public ImageEngineReadinessReport(ImageEngineRuntimeState state,
                                      Path applicationRoot,
                                      Path runtimeDirectory,
                                      Path modelDirectory,
                                      boolean runtimePrepared,
                                      boolean modelInstalled,
                                      boolean engineResponding,
                                      List<String> missingRequirements,
                                      String userMessage,
                                      String nextAction) {
        this(state, applicationRoot, runtimeDirectory, modelDirectory, runtimePrepared, modelInstalled,
                engineResponding, null, missingRequirements, userMessage, nextAction);
    }

    public ImageEngineReadinessReport {
        state = state == null ? ImageEngineRuntimeState.ERROR : state;
        missingRequirements = List.copyOf(missingRequirements == null ? List.of() : missingRequirements);
        userMessage = userMessage == null ? "" : userMessage.strip();
        nextAction = nextAction == null ? "" : nextAction.strip();
    }

    public boolean ready() {
        return state == ImageEngineRuntimeState.READY;
    }

    public String statusLabel() {
        return state.displayName() + ". " + userMessage;
    }
}
