package com.marcosmoreiradev.docupodcaststudio.application.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoiceEngineUsabilityPolicyTest {
    @Test
    void prioritizesXttsAndKeepsPiperAsFallback() {
        var primary = VoiceEngineUsabilityPolicy.defaultEngine();
        assertEquals("tts-xtts", primary.id());
        assertTrue(primary.primary());
        assertTrue(primary.requiresModel());
        assertTrue(primary.supports(VoiceEngineControl.SPEED));
        assertTrue(primary.supports(VoiceEngineControl.REFERENCE_VOICE));
        assertTrue(primary.supports(VoiceEngineControl.EMOTION_INTENT));
        assertTrue(primary.supportsExpressiveReferenceVoice());

        var fallback = VoiceEngineUsabilityPolicy.piperLightweight();
        assertEquals("tts-piper", fallback.id());
        assertFalse(fallback.primary());
        assertTrue(fallback.requiresModel());
        assertTrue(fallback.supports(VoiceEngineControl.SPEED));
        assertFalse(fallback.supportsExpressiveReferenceVoice());
    }

    @Test
    void keepsMockAsDiagnosticAndHidesRawCommandsFromNormalUser() {
        var mock = VoiceEngineUsabilityPolicy.mockDiagnostic();
        assertTrue(mock.diagnosticOnly());
        assertFalse(mock.requiresModel());
        assertFalse(VoiceEngineUsabilityPolicy.shouldExposeRawCommandToNormalUser());
        assertTrue(VoiceEngineUsabilityPolicy.find("tts-xtts").isPresent());
    }
}
