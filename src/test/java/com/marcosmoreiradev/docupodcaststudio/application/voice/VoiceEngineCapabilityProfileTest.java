package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceEngineCapabilityProfileTest {
    private final VoiceCapabilityPolicy policy = new VoiceCapabilityPolicy();

    @Test
    void piperDisablesExpressiveAndSampleBasedControls() {
        AudioEngineDescriptor piper = AudioEngineDescriptor.process(
                "Piper local", true, "piper", "Listo",
                java.util.Set.of(EngineFeature.PACKAGED_VOICE));
        VoiceEngineCapabilityProfile profile = policy.activeEngineProfile(piper);

        assertTrue(profile.simpleLocalMode());
        assertTrue(profile.supportsPackagedModelVoice());
        assertFalse(profile.supportsCustomVoiceSample());
        assertFalse(profile.supportsEmotion());
        assertFalse(profile.supportsExpressiveStyle());
        assertFalse(profile.supportsVoiceCloning());
    }

    @Test
    void coquiEnablesAdvancedControlsWhenSelected() {
        AudioEngineDescriptor xtts = AudioEngineDescriptor.process(
                "Coqui XTTS local", true, "xtts", "Listo",
                java.util.Set.of(EngineFeature.REFERENCE_VOICE,
                        EngineFeature.EXPRESSIVE_STYLE));
        VoiceEngineCapabilityProfile profile = policy.activeEngineProfile(xtts);

        assertTrue(profile.advancedAiMode());
        assertTrue(profile.supportsCustomVoiceSample());
        assertTrue(profile.supportsEmotion());
        assertTrue(profile.supportsExpressiveStyle());
        assertTrue(profile.supportsVoiceCloning());
    }

    @Test
    void mockDoesNotPromiseRealVoice() {
        VoiceEngineCapabilityProfile profile = policy.activeEngineProfile(AudioEngineDescriptor.mock());

        assertTrue(profile.diagnosticMode());
        assertFalse(profile.canSynthesizeNow());
        assertFalse(profile.supportsCustomVoiceSample());
        assertTrue(profile.sidebarNotice().contains("no genera voz real"));
    }

    @Test
    void capabilitiesComeFromFeatureFlagsNotProviderNames() {
        AudioEngineDescriptor misleadingName = AudioEngineDescriptor.process(
                "Coqui XTTS por nombre solamente", true, "xtts", "Listo");
        AudioEngineDescriptor unfamiliarReferenceEngine = AudioEngineDescriptor.process(
                "Motor futuro", true, "runtime-local", "Listo",
                java.util.Set.of(EngineFeature.REFERENCE_VOICE));

        assertFalse(policy.activeEngineProfile(misleadingName)
                .supportsVoiceCloning());
        assertTrue(policy.activeEngineProfile(unfamiliarReferenceEngine)
                .supportsVoiceCloning());
        assertFalse(policy.activeEngineProfile(unfamiliarReferenceEngine)
                .supportsExpressiveStyle());
    }
}
