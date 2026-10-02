package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Specific anti-placeholder inspection for the local theatre image engine. */
public record ImageEngineArtifactInspectionReport(
        Path runtimeDirectory,
        Path modelDirectory,
        ImageEngineComponentStatus runtimeStatus,
        ImageEngineComponentStatus checkpointStatus,
        ImageEngineComponentStatus workflowStatus,
        ImageEngineComponentStatus adapterStatus,
        ImageEngineComponentStatus loraStatus,
        String checkpointName,
        String workflowName,
        List<String> missingRequirements,
        List<String> warnings,
        List<String> discoveredFiles,
        String userMessage
) {
    public ImageEngineArtifactInspectionReport {
        runtimeStatus = runtimeStatus == null ? ImageEngineComponentStatus.MISSING : runtimeStatus;
        checkpointStatus = checkpointStatus == null ? ImageEngineComponentStatus.MISSING : checkpointStatus;
        workflowStatus = workflowStatus == null ? ImageEngineComponentStatus.MISSING : workflowStatus;
        adapterStatus = adapterStatus == null ? ImageEngineComponentStatus.OPTIONAL_MISSING : adapterStatus;
        loraStatus = loraStatus == null ? ImageEngineComponentStatus.OPTIONAL_MISSING : loraStatus;
        checkpointName = checkpointName == null ? "" : checkpointName.strip();
        workflowName = workflowName == null ? "" : workflowName.strip();
        missingRequirements = List.copyOf(missingRequirements == null ? List.of() : missingRequirements);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        discoveredFiles = List.copyOf(discoveredFiles == null ? List.of() : discoveredFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean runtimeReady() {
        return runtimeStatus.ready();
    }

    public boolean modelReady() {
        return checkpointStatus.ready() && workflowStatus.ready()
                && missingRequirements.stream().allMatch(requirement -> requirement.startsWith("Runtime local")
                    || requirement.startsWith("Lanzador compatible"));
    }

    public boolean basicReady() {
        return runtimeReady() && modelReady();
    }

    public boolean advancedConsistencyReady() {
        return adapterStatus.ready() || loraStatus.ready();
    }
}
