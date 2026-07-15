package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** User-facing capability assessment for a performance style with the current audio engine. */
public record PerformanceStyleCapability(
        String styleId,
        String displayName,
        boolean assignable,
        boolean honoredByCurrentEngine,
        boolean requiresEngineSupport,
        boolean roadmapOnly,
        String status,
        String message
) {
    public PerformanceStyleCapability {
        styleId = normalize(styleId);
        displayName = normalize(displayName);
        status = normalize(status).isBlank() ? "Sin clasificar" : normalize(status);
        message = normalize(message).isBlank() ? "Sin detalle de capacidad." : normalize(message);
    }

    public String summaryLabel() {
        return styleId + " · " + displayName + " · " + status + " · " + message;
    }

    public String statusCssClass() {
        if (honoredByCurrentEngine) {
            return "voice-capability-ready";
        }
        if (roadmapOnly) {
            return "voice-capability-roadmap";
        }
        return "voice-capability-reference";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
