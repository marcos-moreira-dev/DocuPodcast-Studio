package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.List;
import java.util.Optional;

/** Read-only production view for audio, voices and engine readiness by fragment. */
public record AudioVoiceProductionProjection(
        List<FragmentAudioVoiceState> fragments,
        AudioVoiceReadiness readiness,
        List<AudioEngineReadinessUiItem> engineReadiness
) {
    public AudioVoiceProductionProjection {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        readiness = readiness == null
                ? new AudioVoiceReadiness(0, 0, 0, 0, 0, 0, true, List.of(), List.of())
                : readiness;
        engineReadiness = engineReadiness == null ? List.of() : List.copyOf(engineReadiness);
    }

    public Optional<FragmentAudioVoiceState> fragmentById(FragmentId fragmentId) {
        if (fragmentId == null) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.fragmentId().equals(fragmentId)).findFirst();
    }

    public Optional<FragmentAudioVoiceState> fragmentForSegment(String segmentId) {
        String target = normalize(segmentId);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return fragments.stream().filter(fragment -> fragment.segmentId().equals(target)).findFirst();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
