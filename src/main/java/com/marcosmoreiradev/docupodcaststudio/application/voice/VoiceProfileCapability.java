package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** User-facing capability assessment for a voice profile with the current audio engine. */
public record VoiceProfileCapability(
        String voiceId,
        String displayName,
        boolean assignable,
        boolean synthesizableNow,
        boolean referenceReady,
        boolean requiresSample,
        boolean requiresEngineConfiguration,
        boolean roadmapOnly,
        String status,
        String message
) {
    public VoiceProfileCapability {
        voiceId = normalize(voiceId);
        displayName = normalize(displayName);
        status = normalize(status).isBlank() ? "Sin clasificar" : normalize(status);
        message = normalize(message).isBlank() ? "Sin detalle de capacidad." : normalize(message);
    }

    public boolean blocked() {
        return !assignable || requiresSample || requiresEngineConfiguration;
    }

    public String summaryLabel() {
        return voiceId + " · " + displayName + " · " + status + " · " + message;
    }

    public String statusCssClass() {
        if (synthesizableNow) {
            return "voice-capability-ready";
        }
        if (referenceReady) {
            return "voice-capability-reference";
        }
        if (roadmapOnly) {
            return "voice-capability-roadmap";
        }
        return "voice-capability-blocked";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
