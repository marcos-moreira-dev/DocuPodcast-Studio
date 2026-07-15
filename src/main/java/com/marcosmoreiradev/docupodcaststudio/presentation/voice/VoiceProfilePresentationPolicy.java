package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.Optional;

/** Human-facing labels for legacy/default voices in the Voices workspace. */
final class VoiceProfilePresentationPolicy {
    private static final String DEFAULT_SIMPLE_VOICE_ID = "VOC-NARRATOR";
    private static final String DEFAULT_ADVANCED_VOICE_ID = "VOC-OWN-PLACEHOLDER";

    private VoiceProfilePresentationPolicy() {
    }

    static String displayName(VoiceProfile voice) {
        if (advancedPredesignedNeutral(voice)) {
            return "Narrador prediseñado avanzado";
        }
        return voice == null ? "Voz" : voice.displayName();
    }

    static String typeLabel(VoiceProfile voice) {
        if (simpleVoice(voice)) {
            return "Voz simple";
        }
        if (advancedPredesignedNeutral(voice) || OfficialAdvancedVoicePresetCatalog.isOfficialPreset(voice)) {
            return "Voz IA avanzada";
        }
        if (voice != null && voice.engineType() == VoiceEngineType.MOCK) {
            return "Modo de prueba";
        }
        return "Voz avanzada";
    }

    static String status(VoiceProfile voice, VoiceLibrary library) {
        if (voice == null) {
            return "Sin voz";
        }
        if (hasNeutralReference(voice, library)) {
            return advancedPredesignedNeutral(voice) ? "Neutral" : "Lista";
        }
        if (simpleVoice(voice) || voice.hasSample()) {
            return "Lista";
        }
        return "Incompleta";
    }

    static String sampleLabel(VoiceProfile voice, VoiceLibrary library) {
        Optional<VoiceReferenceSampleSet> sampleSet = sampleSet(voice, library);
        int toneSamples = sampleSet.map(set -> set.samples().size()).orElse(0);
        boolean hasNeutral = sampleSet.map(VoiceReferenceSampleSet::hasNeutral).orElse(false);
        if (toneSamples == 1 && hasNeutral) {
            return "Neutral registrada";
        }
        if (toneSamples > 0) {
            return toneSamples + " tono(s) registrado(s)";
        }
        if (advancedPredesignedNeutral(voice)) {
            return "Neutral prediseñada";
        }
        if (simpleVoice(voice)) {
            return "Usa voz base del motor";
        }
        return voice != null && voice.hasSample() ? "Muestra registrada" : "Sin muestra";
    }

    static boolean simpleVoice(VoiceProfile voice) {
        if (voice == null) {
            return false;
        }
        if (DEFAULT_SIMPLE_VOICE_ID.equalsIgnoreCase(voice.id()) && !voice.hasSample()) {
            return true;
        }
        return voice.engineType() == VoiceEngineType.PIPER || voice.engineType() == VoiceEngineType.LOCAL_TTS_PROCESS;
    }

    static boolean predefinedVoice(VoiceProfile voice) {
        return voice != null && voice.type() == VoiceProfileType.PREDEFINED;
    }

    static boolean advancedVoice(VoiceProfile voice) {
        return voice != null && (advancedPredesignedNeutral(voice)
                || voice.engineType() == VoiceEngineType.XTTS
                || voice.engineType() == VoiceEngineType.HUMAN_AUDIO);
    }

    static boolean advancedPredesignedNeutral(VoiceProfile voice) {
        return voice != null && DEFAULT_ADVANCED_VOICE_ID.equalsIgnoreCase(voice.id())
                && !voice.metadata().containsKey("userManaged");
    }

    static boolean hasNeutralReference(VoiceProfile voice, VoiceLibrary library) {
        return sampleSet(voice, library)
                .map(set -> set.sampleFor(VoiceReferenceTone.NEUTRAL).isPresent())
                .orElseGet(() -> advancedPredesignedNeutral(voice));
    }

    private static Optional<VoiceReferenceSampleSet> sampleSet(VoiceProfile voice, VoiceLibrary library) {
        if (voice == null || library == null) {
            return Optional.empty();
        }
        return library.referenceSampleSetByVoiceId(voice.id());
    }
}
