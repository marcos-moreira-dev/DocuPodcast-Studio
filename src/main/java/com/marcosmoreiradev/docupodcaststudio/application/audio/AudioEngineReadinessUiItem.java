package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.Objects;

/**
 * Human, operational state of an audio origin for UI surfaces.
 *
 * <p>This is not a decorative metric. It tells the user whether the engine can be used in Documento,
 * why it may be hidden there, and what action is available in Configuración or Voces.</p>
 */
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
        boolean documentVisible = availability.usableInDocument();
        String label = availability.statusLabel();
        if (!availability.usableInDocument() && "xtts".equals(availability.engineId())) {
            label = "No aparece en Documento: falta prueba WAV válida";
        } else if (!availability.usableInDocument() && "piper".equals(availability.engineId())) {
            label = "No aparece en Documento: falta preparar Voz local simple";
        }
        return new AudioEngineReadinessUiItem(
                availability.engineId(),
                availability.displayName(),
                label,
                availability.usableInDocument(),
                documentVisible,
                availability.userMessage(),
                availability.recommendedAction());
    }

    public String compactLine() {
        String suffix = recommendedAction.isBlank() ? "" : " Acción: " + recommendedAction;
        return displayName + " · " + readinessLabel + (message.isBlank() ? "" : ". " + message) + suffix;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
