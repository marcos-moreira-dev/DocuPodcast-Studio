package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.util.List;

/** Product-facing plan for the advanced voice registration wizard. */
public record VoiceRegistrationWizardPlan(
        String voiceDisplayName,
        String visibleEngineLabel,
        String title,
        String subtitle,
        VoiceToneRecordingPrompt neutralPrompt,
        List<VoiceToneRecordingPrompt> recommendedPrompts,
        List<VoiceToneRecordingPrompt> theatricalPrompts,
        String expressiveReferenceNotice,
        String generatedTestPlaceholder,
        String generatedTestActionLabel
) {
    public VoiceRegistrationWizardPlan {
        voiceDisplayName = normalize(voiceDisplayName).isBlank() ? "Nueva voz" : normalize(voiceDisplayName);
        visibleEngineLabel = normalize(visibleEngineLabel).isBlank() ? "Voz IA avanzada" : normalize(visibleEngineLabel);
        title = normalize(title).isBlank() ? "Crear voz" : normalize(title);
        subtitle = normalize(subtitle).isBlank()
                ? "Graba o importa muestras de referencia para esta voz."
                : normalize(subtitle);
        neutralPrompt = neutralPrompt == null ? VoiceToneRecordingPrompt.fromTone(com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone.NEUTRAL) : neutralPrompt;
        recommendedPrompts = recommendedPrompts == null ? List.of() : List.copyOf(recommendedPrompts);
        theatricalPrompts = theatricalPrompts == null ? List.of() : List.copyOf(theatricalPrompts);
        expressiveReferenceNotice = normalize(expressiveReferenceNotice).isBlank()
                ? "Los tonos son muestras de referencia. Ayudan a orientar la lectura generada, pero el resultado puede variar segun el motor de voz."
                : normalize(expressiveReferenceNotice);
        generatedTestPlaceholder = normalize(generatedTestPlaceholder).isBlank()
                ? "Esta es una prueba de lectura con la voz seleccionada."
                : normalize(generatedTestPlaceholder);
        generatedTestActionLabel = normalize(generatedTestActionLabel).isBlank()
                ? "Generar prueba con esta voz"
                : normalize(generatedTestActionLabel);
    }

    public boolean usesVisibleAdvancedVoiceLabelOnly() {
        return !visibleEngineLabel.toLowerCase(java.util.Locale.ROOT).contains("coqui")
                && !visibleEngineLabel.toLowerCase(java.util.Locale.ROOT).contains("xtts");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
