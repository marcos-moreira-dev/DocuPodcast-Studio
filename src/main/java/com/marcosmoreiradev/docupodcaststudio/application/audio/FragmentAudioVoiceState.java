package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceAvailability;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;

import java.util.List;
import java.util.Objects;

/** Fragment-level state for audio, selected voice and operational blockers. */
public record FragmentAudioVoiceState(
        FragmentId fragmentId,
        int order,
        String segmentId,
        String sourceBlockId,
        String title,
        boolean narratable,
        AudioSourceKind audioSourceKind,
        boolean audioReady,
        String audioRelativePath,
        String audioStatus,
        double audioDurationSeconds,
        String voiceProfileId,
        String voiceDisplayName,
        VoiceReferenceAvailability voiceAvailability,
        String voiceMessage,
        List<String> blockers
) {
    public FragmentAudioVoiceState {
        fragmentId = Objects.requireNonNull(fragmentId, "fragmentId");
        order = Math.max(0, order);
        segmentId = normalize(segmentId);
        sourceBlockId = normalize(sourceBlockId);
        title = normalize(title).isBlank() ? fragmentId.value() : normalize(title);
        audioSourceKind = audioSourceKind == null ? AudioSourceKind.NONE : audioSourceKind;
        audioRelativePath = normalize(audioRelativePath).replace('\\', '/');
        audioStatus = normalize(audioStatus);
        audioDurationSeconds = Math.max(0.0, audioDurationSeconds);
        voiceProfileId = normalize(voiceProfileId);
        voiceDisplayName = normalize(voiceDisplayName);
        voiceAvailability = voiceAvailability == null ? VoiceReferenceAvailability.MISSING_PROFILE : voiceAvailability;
        voiceMessage = normalize(voiceMessage);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public boolean voiceReady() {
        return voiceAvailability.usable();
    }

    public boolean productionReady() {
        return narratable && audioReady && voiceReady();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
