package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/**
 * Readiness report for the local Coqui/XTTS setup used by the high-quality reader.
 *
 * <p>The report is intentionally local/offline: it verifies the wrapper, portable Python,
 * model folder and neutral speaker sample that already exist under the application layout.
 * Downloading/importing artifacts must be handled by a manifest-driven workflow.</p>
 */
public record XttsSetupReadinessReport(
        Path applicationRoot,
        Path setupScript,
        Path pythonExecutable,
        Path wrapperScript,
        Path modelDirectory,
        Path speakerWav,
        ModelInspectionResult modelInspection,
        List<String> missingRequirements,
        List<String> warnings,
        String userMessage
) {
    public XttsSetupReadinessReport {
        missingRequirements = List.copyOf(missingRequirements == null ? List.of() : missingRequirements);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean runtimeReady() {
        return missingRequirements.stream().noneMatch(item -> item.startsWith("Runtime"));
    }

    public boolean modelUsable() {
        return modelInspection != null && modelInspection.usable();
    }

    public boolean ready() {
        return missingRequirements.isEmpty() && modelUsable();
    }

    public boolean canBeSelectedAsEngine() {
        return ready();
    }
}
