package com.marcosmoreiradev.docupodcaststudio.application.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoiceEngineUsabilityPolicyTest {
    @Test
    void preservesPiperAsHistoricalDefaultWithoutBindingAdvancedVoicesToXtts() {
        var initial = VoiceEngineUsabilityPolicy.defaultEngine();
        assertEquals("tts-piper", initial.id());
        assertTrue(initial.requiresModel());
        assertTrue(initial.supports(VoiceEngineControl.SPEED));
        assertFalse(initial.supportsExpressiveReferenceVoice());

        var advancedCompatibility = VoiceEngineUsabilityPolicy.xttsHighQuality();
        assertTrue(advancedCompatibility.supports(VoiceEngineControl.REFERENCE_VOICE));
        assertTrue(advancedCompatibility.supports(VoiceEngineControl.EMOTION_INTENT));
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
