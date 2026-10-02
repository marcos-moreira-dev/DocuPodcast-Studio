package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.Objects;

/** Human, operational state of an audio origin for all UI surfaces. */
public record AudioEngineReadinessUiItem(
        String engineId,
        String displayName,
        String readinessLabel,
        boolean usableInDocument,
        boolean visibleInDocument,
        String message,
        String recommendedAction
) {
    public AudioEngineReadinessUiItem {
        engineId = normalize(engineId).isBlank() ? "unknown" : normalize(engineId);
        displayName = normalize(displayName).isBlank() ? engineId : normalize(displayName);
        readinessLabel = normalize(readinessLabel).isBlank() ? "Estado no determinado" : normalize(readinessLabel);
        message = normalize(message);
        recommendedAction = normalize(recommendedAction);
    }

    public static AudioEngineReadinessUiItem fromAvailability(AudioEngineAvailability availability) {
        Objects.requireNonNull(availability, "availability");
        String label = availability.statusLabel();
        if (!availability.usableInDocument() && "xtts".equals(availability.engineId())) {
            label = "Visible pero no seleccionable: Voz IA avanzada requiere reparación";
        } else if (!availability.usableInDocument() && "piper".equals(availability.engineId())) {
            label = "Visible pero no seleccionable: Voz local simple requiere reparación";
        }
        return new AudioEngineReadinessUiItem(
                availability.engineId(), availability.displayName(), label,
                availability.usableInDocument(), true,
                availability.userMessage(), availability.recommendedAction());
    }

    public String compactLine() {
        String suffix = recommendedAction.isBlank() ? "" : " Acción: " + recommendedAction;
        return displayName + " · " + readinessLabel + (message.isBlank() ? "" : ". " + message) + suffix;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
