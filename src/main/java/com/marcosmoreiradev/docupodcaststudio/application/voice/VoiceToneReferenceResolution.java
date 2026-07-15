package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.Optional;

/** Result of resolving the best reference sample for a requested voice tone. */
public record VoiceToneReferenceResolution(
        String voiceProfileId,
        VoiceReferenceTone requestedTone,
        VoiceReferenceTone resolvedTone,
        Optional<VoiceReferenceSample> sample,
        boolean fallbackToNeutral,
        String userMessage
) {
    public VoiceToneReferenceResolution {
        voiceProfileId = voiceProfileId == null ? "" : voiceProfileId.strip();
        requestedTone = requestedTone == null ? VoiceReferenceTone.NEUTRAL : requestedTone;
        resolvedTone = resolvedTone == null ? VoiceReferenceTone.NEUTRAL : resolvedTone;
        sample = sample == null ? Optional.empty() : sample;
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public boolean available() {
        return sample.isPresent();
    }

    public String sampleAssetId() {
        return sample.map(VoiceReferenceSample::id).orElse("");
    }
}
