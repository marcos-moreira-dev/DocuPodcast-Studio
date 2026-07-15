package com.marcosmoreiradev.docupodcaststudio.application.media;

import java.nio.file.Path;
import java.util.Objects;

/** Result of preparing an audio file for assignment, playback or diagnostics. */
public record AudioNormalizationResult(
        Path sourceAudioFile,
        Path targetAudioFile,
        AudioNormalizationProfile profile,
        boolean successful,
        String message
) {
    public AudioNormalizationResult {
        sourceAudioFile = Objects.requireNonNull(sourceAudioFile, "sourceAudioFile");
        targetAudioFile = Objects.requireNonNull(targetAudioFile, "targetAudioFile");
        profile = profile == null ? AudioNormalizationProfile.ASSIGNABLE_AUDIO : profile;
        message = message == null ? "" : message.strip();
    }
}
