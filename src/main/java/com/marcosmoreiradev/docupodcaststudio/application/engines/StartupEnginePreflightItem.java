package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.nio.file.Path;
import java.util.List;

/** One startup readiness item for local voice/media prerequisites. */
public record StartupEnginePreflightItem(
        String itemId,
        String displayName,
        boolean requiredForListening,
        AiEngineReadinessStatus status,
        String userMessage,
        String recommendedAction,
        Path expectedPath,
        List<String> diagnostics
) {
    public StartupEnginePreflightItem {
        itemId = itemId == null ? "" : itemId.strip();
        displayName = displayName == null ? "" : displayName.strip();
        status = status == null ? AiEngineReadinessStatus.NEEDS_CONFIGURATION : status;
        userMessage = userMessage == null ? "" : userMessage.strip();
        recommendedAction = recommendedAction == null ? "" : recommendedAction.strip();
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public boolean ready() {
        return status.usable();
    }

    public boolean missing() {
        return !ready();
    }
}
