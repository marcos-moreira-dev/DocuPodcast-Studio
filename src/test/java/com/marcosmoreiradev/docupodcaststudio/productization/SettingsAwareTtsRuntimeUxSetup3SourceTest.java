package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-SETUP3 ensures audio generation uses the settings saved while the app is already running. */
final class SettingsAwareTtsRuntimeUxSetup3SourceTest {
    @Test
    void infrastructureUsesSettingsAwareGateways() throws Exception {
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));
        assertTrue(factory.contains("SettingsAwareAudioGenerationGateway"));
        assertTrue(factory.contains("SettingsAwareVoiceTestSynthesisGateway"));
        assertTrue(factory.contains("settingsRepository"));
    }

    @Test
    void audioGatewayReloadsPersistedSettingsAtSubmitTime() throws Exception {
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));
        assertTrue(gateway.contains("settingsRepository.load()"));
        assertTrue(gateway.contains("delegateForCurrentSettings"));
        assertTrue(gateway.contains("XttsTtsCommandTemplate.resolve"));
        assertTrue(gateway.contains("PiperTtsCommandTemplate.resolve"));
        assertTrue(gateway.contains("submit(AudioGenerationRequest"));
    }

    @Test
    void voiceTestGatewayReloadsPersistedSettingsBeforeSynthesis() throws Exception {
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareVoiceTestSynthesisGateway.java"));
        assertTrue(gateway.contains("settingsRepository.load()"));
        assertTrue(gateway.contains("SettingsAwareAudioGenerationGateway.resolveTtsCommandTemplate"));
        assertTrue(gateway.contains("No hay una voz real lista"));
    }
}
