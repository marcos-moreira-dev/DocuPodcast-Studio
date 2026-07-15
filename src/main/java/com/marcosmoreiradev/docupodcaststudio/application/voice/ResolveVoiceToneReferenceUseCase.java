package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.Optional;

/** Resolves a requested expressive tone to an available reference sample, falling back to neutral when possible. */
public final class ResolveVoiceToneReferenceUseCase {
    public VoiceToneReferenceResolution resolve(VoiceLibrary library, String voiceProfileId, VoiceReferenceTone requestedTone) {
        String voiceId = voiceProfileId == null ? "" : voiceProfileId.strip();
        VoiceReferenceTone requested = requestedTone == null ? VoiceReferenceTone.NEUTRAL : requestedTone;
        if (library == null || voiceId.isBlank()) {
            return new VoiceToneReferenceResolution(voiceId, requested, VoiceReferenceTone.NEUTRAL, Optional.empty(), false,
                    "Selecciona una voz con muestra neutral antes de generar una prueba.");
        }
        Optional<VoiceReferenceSampleSet> sampleSet = library.referenceSampleSetByVoiceId(voiceId);
        if (sampleSet.isEmpty()) {
            return new VoiceToneReferenceResolution(voiceId, requested, VoiceReferenceTone.NEUTRAL, Optional.empty(), false,
                    "Esta voz todavía no tiene muestras de referencia registradas.");
        }
        Optional<VoiceReferenceSample> exact = sampleSet.get().sampleFor(requested);
        if (exact.isPresent()) {
            return new VoiceToneReferenceResolution(voiceId, requested, requested, exact, false,
                    "Se usará la muestra " + requested.displayName() + " para generar la prueba.");
        }
        Optional<VoiceReferenceSample> neutral = sampleSet.get().neutralSample();
        if (neutral.isPresent()) {
            return new VoiceToneReferenceResolution(voiceId, requested, VoiceReferenceTone.NEUTRAL, neutral, requested != VoiceReferenceTone.NEUTRAL,
                    requested == VoiceReferenceTone.NEUTRAL
                            ? "Se usará la muestra neutral para generar la prueba."
                            : "No hay muestra " + requested.displayName() + "; se usará la muestra neutral como fallback.");
        }
        return new VoiceToneReferenceResolution(voiceId, requested, VoiceReferenceTone.NEUTRAL, Optional.empty(), false,
                "Esta voz requiere una muestra neutral antes de generar pruebas.");
    }
}
