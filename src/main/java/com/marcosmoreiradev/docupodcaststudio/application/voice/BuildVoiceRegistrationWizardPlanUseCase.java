package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.List;

/** Builds the user-facing wizard plan for registering an advanced voice. */
public final class BuildVoiceRegistrationWizardPlanUseCase {
    public VoiceRegistrationWizardPlan build(String voiceDisplayName, boolean includeTheatricalExtended) {
        VoiceToneRecordingPrompt neutral = VoiceToneRecordingPrompt.fromTone(VoiceReferenceTone.NEUTRAL);
        List<VoiceToneRecordingPrompt> recommended = VoiceReferenceTone.basicTones().stream()
                .filter(tone -> tone != VoiceReferenceTone.NEUTRAL)
                .map(VoiceToneRecordingPrompt::fromTone)
                .toList();
        List<VoiceToneRecordingPrompt> theatrical = includeTheatricalExtended
                ? VoiceReferenceTone.theatricalExtendedTones().stream()
                    .map(VoiceToneRecordingPrompt::fromTone)
                    .toList()
                : List.of();
        return new VoiceRegistrationWizardPlan(
                voiceDisplayName,
                "Voz IA avanzada",
                "Crear voz",
                "Graba o importa una muestra neutral y, si lo deseas, tonos de referencia para lectura expresiva.",
                neutral,
                recommended,
                theatrical,
                "Los tonos son muestras de referencia. Ayudan a orientar la lectura generada, pero el resultado puede variar segun el motor de voz.",
                "Esta es una prueba de lectura con la voz seleccionada.",
                "Generar prueba con esta voz"
        );
    }
}
