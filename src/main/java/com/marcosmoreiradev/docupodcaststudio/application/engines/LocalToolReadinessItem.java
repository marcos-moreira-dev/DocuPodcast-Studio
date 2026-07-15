package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** One local product tool checked before real voice/media smoke execution. */
public record LocalToolReadinessItem(
        String itemId,
        String displayName,
        boolean requiredForProductSmoke,
        AiEngineReadinessStatus status,
        String userMessage,
        String recommendedAction,
        Path expectedPath,
        List<String> diagnostics
) {
    public LocalToolReadinessItem {
        itemId = normalize(itemId);
        displayName = normalize(displayName);
        status = status == null ? AiEngineReadinessStatus.NEEDS_CONFIGURATION : status;
        userMessage = normalize(userMessage);
        recommendedAction = normalize(recommendedAction);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public boolean ready() {
        return status.usable();
    }

    public boolean missing() {
        return !ready();
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").strip();
    }
}
