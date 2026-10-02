package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.List;

/** One honest, user-facing readiness state for a registered voice engine. */
public record VoiceEngineOperationalState(
        String engineId,
        String displayName,
        boolean registered,
        boolean ready,
        boolean realTts,
        boolean supportsVoiceSamples,
        boolean supportsTones,
        String statusLabel,
        String message,
        List<String> issues,
        List<String> recommendedActions
) {
    public VoiceEngineOperationalState {
        engineId = clean(engineId, "unknown");
        displayName = clean(displayName, engineId);
        statusLabel = clean(statusLabel, ready ? "Listo" : "No disponible");
        message = clean(message, "");
        issues = issues == null ? List.of() : List.copyOf(issues);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }

    public String recommendedAction() {
        return String.join(" ", recommendedActions);
    }

    private static String clean(String value, String fallback) {
        String current = value == null ? "" : value.strip();
        return current.isBlank() ? fallback : current;
    }
}
