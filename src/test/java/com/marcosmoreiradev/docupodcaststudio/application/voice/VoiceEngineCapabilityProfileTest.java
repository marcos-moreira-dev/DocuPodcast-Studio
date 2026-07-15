package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceEngineCapabilityProfileTest {
    private final VoiceCapabilityPolicy policy = new VoiceCapabilityPolicy();

    @Test
    void piperDisablesExpressiveAndSampleBasedControls() {
        AudioEngineDescriptor piper = AudioEngineDescriptor.process("Piper local", true, "piper", "Listo");
        VoiceEngineCapabilityProfile profile = policy.activeEngineProfile(piper);

        assertTrue(profile.piperMode());
        assertTrue(profile.supportsPiperModelVoice());
        assertFalse(profile.supportsCustomVoiceSample());
        assertFalse(profile.supportsEmotion());
        assertFalse(profile.supportsExpressiveStyle());
        assertFalse(profile.supportsVoiceCloning());
    }

    @Test
    void coquiEnablesAdvancedControlsWhenSelected() {
        AudioEngineDescriptor xtts = AudioEngineDescriptor.process("Coqui XTTS local", true, "xtts", "Listo");
        VoiceEngineCapabilityProfile profile = policy.activeEngineProfile(xtts);

        assertTrue(profile.coquiXttsMode());
        assertTrue(profile.supportsCustomVoiceSample());
        assertTrue(profile.supportsEmotion());
        assertTrue(profile.supportsExpressiveStyle());
        assertTrue(profile.supportsVoiceCloning());
    }

    @Test
    void mockDoesNotPromiseRealVoice() {
        VoiceEngineCapabilityProfile profile = policy.activeEngineProfile(AudioEngineDescriptor.mock());

        assertTrue(profile.mockMode());
        assertFalse(profile.canSynthesizeNow());
        assertFalse(profile.supportsCustomVoiceSample());
        assertTrue(profile.sidebarNotice().contains("no genera voz real"));
    }
}
