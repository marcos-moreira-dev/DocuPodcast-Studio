package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneRecordingPrompt;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.List;

/** Resolves the tones that are really available for first-level voice tests. */
final class VoiceRegisteredTonePrompts {
    private VoiceRegisteredTonePrompts() {
    }

    static List<VoiceToneRecordingPrompt> forVoice(VoiceProfile voice, VoiceLibrary library) {
        if (voice == null || VoiceProfilePresentationPolicy.simpleVoice(voice)) {
            return List.of(VoiceToneRecordingPrompt.fromTone(VoiceReferenceTone.NEUTRAL));
        }
        List<VoiceReferenceTone> tones = library == null
                ? List.of()
                : library.referenceSampleSetByVoiceId(voice.id())
                        .map(VoiceReferenceSampleSet::registeredTones)
                        .orElse(List.of());
        if (tones.isEmpty() && VoiceProfilePresentationPolicy.advancedPredesignedNeutral(voice)) {
            tones = List.of(VoiceReferenceTone.NEUTRAL);
        }
        return tones.stream().map(VoiceToneRecordingPrompt::fromTone).toList();
    }
}
