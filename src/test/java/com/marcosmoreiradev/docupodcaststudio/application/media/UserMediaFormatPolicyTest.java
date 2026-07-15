package com.marcosmoreiradev.docupodcaststudio.application.media;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UserMediaFormatPolicyTest {
    private final UserMediaFormatPolicy policy = new UserMediaFormatPolicy();

    @Test
    void acceptsMp3AndWavAsAudioFiles() {
        assertEquals(UserMediaAssetKind.AUDIO_FILE, policy.classify(Path.of("lucia-hablando.mp3")));
        assertEquals(UserMediaAssetKind.AUDIO_FILE, policy.classify(Path.of("ambiente.wav")));
    }

    @Test
    void acceptsCommonVideoFormatsOnlyForAudioExtraction() {
        assertEquals(UserMediaAssetKind.VIDEO_FOR_AUDIO, policy.classify(Path.of("escena.mp4")));
        assertEquals(UserMediaAssetKind.VIDEO_FOR_AUDIO, policy.classify(Path.of("obra.webm")));
    }

    @Test
    void documentsSupportedFormatSummaryForSimpleUiCopy() {
        assertTrue(policy.supportedFormatSummary().contains("MP3/WAV"));
        assertTrue(policy.supportedFormatSummary().contains("video para extraer audio"));
    }
}
