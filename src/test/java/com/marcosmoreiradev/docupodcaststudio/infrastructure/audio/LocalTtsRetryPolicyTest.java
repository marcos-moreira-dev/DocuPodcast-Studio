package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LocalTtsRetryPolicyTest {
    @Test
    void defaultsRepresentFourTotalAttemptsPerChunk() {
        assertEquals(3, OperationalSettings.defaults().tts().maxRetries());
        assertEquals(3, LocalTtsProcessConfiguration.from(new Properties(), Map.of()).maxRetries());
        assertEquals(4, GenerationAttemptPolicy.audioMaxAttemptsFromRetries(OperationalSettings.defaults().tts().maxRetries()));
        assertEquals(4, GenerationAttemptPolicy.robustAudioMaxAttemptsFromRetries(1));
    }
}
