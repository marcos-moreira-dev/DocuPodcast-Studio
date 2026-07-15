package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** User-facing result of inspecting an imported/downloaded model folder. */
public record ModelInspectionResult(
        String engineId,
        Path inspectedFolder,
        ModelInspectionStatus status,
        List<String> missingRequirements,
        List<String> discoveredFiles,
        boolean checksumManifestPresent,
        String userMessage
) {
    public ModelInspectionResult {
        engineId = engineId == null ? "" : engineId.strip();
        missingRequirements = List.copyOf(missingRequirements == null ? List.of() : missingRequirements);
        discoveredFiles = List.copyOf(discoveredFiles == null ? List.of() : discoveredFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean usable() {
        return status == ModelInspectionStatus.READY || status == ModelInspectionStatus.CHECKSUM_NOT_PROVIDED;
    }

    public boolean needsManualFix() {
        return status == ModelInspectionStatus.MISSING_FOLDER || status == ModelInspectionStatus.MISSING_REQUIRED_FILES;
    }
}
