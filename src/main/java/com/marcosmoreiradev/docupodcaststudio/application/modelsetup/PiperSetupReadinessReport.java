package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Readiness report for Piper, the local intermediate/lightweight reader engine. */
public record PiperSetupReadinessReport(
        Path applicationRoot,
        Path wrapperScript,
        Path piperExecutable,
        Path voiceDirectory,
        Path voiceModel,
        Path voiceMetadata,
        ModelInspectionResult modelInspection,
        List<String> missingRequirements,
        List<String> warnings,
        String userMessage
) {
    public PiperSetupReadinessReport {
        missingRequirements = List.copyOf(missingRequirements == null ? List.of() : missingRequirements);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean runtimeReady() {
        return missingRequirements.stream().noneMatch(item -> item.startsWith("Runtime"));
    }

    public boolean voiceUsable() {
        return modelInspection != null && modelInspection.usable()
                && missingRequirements.stream().noneMatch(item -> item.startsWith("Voz Piper"));
    }

    public boolean ready() {
        return missingRequirements.isEmpty() && voiceUsable();
    }

    public boolean canBeSelectedAsEngine() {
        return ready();
    }
}
