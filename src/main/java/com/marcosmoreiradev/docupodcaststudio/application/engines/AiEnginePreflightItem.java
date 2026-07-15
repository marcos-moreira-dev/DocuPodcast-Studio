package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** One row in the operational preflight for real local AI/media engines. */
public record AiEnginePreflightItem(
        String engineId,
        String displayName,
        AiEnginePurpose purpose,
        boolean mandatoryForTargetProduct,
        AiEngineReadinessStatus status,
        String userMessage,
        String recommendedAction,
        Path expectedFolder,
        List<String> diagnostics
) {
    public AiEnginePreflightItem {
        engineId = normalize(engineId);
        displayName = normalize(displayName);
        purpose = Objects.requireNonNull(purpose, "purpose");
        status = status == null ? AiEngineReadinessStatus.NEEDS_CONFIGURATION : status;
        userMessage = normalize(userMessage);
        recommendedAction = normalize(recommendedAction);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public boolean usable() {
        return status.usable();
    }

    public boolean needsWork() {
        return !usable();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
