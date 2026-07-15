package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTtsProcessAudioGenerationGatewaySourceTest {
    @Test
    void processGatewayKeepsTtsBehindAudioGenerationGateway() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));

        assertTrue(source.contains("implements AudioGenerationGateway"));
        assertTrue(source.contains("ExternalProcessRunner"));
        assertTrue(source.contains("ExternalProcessRequest"));
        assertTrue(source.contains("ExternalProcessObserver"));
        assertTrue(source.contains("commandFor"));
        assertTrue(source.contains("segments"));
        assertTrue(source.contains("audio-manifest.json"));
        assertTrue(source.contains("resume"));
        assertTrue(source.contains("LocalTtsPreflightReport"));
        assertTrue(source.contains("TtsTextPreprocessor.sanitize"));
        assertTrue(source.contains("cancellations"));
    }

    @Test
    void infrastructureFactoryUsesSettingsAwareGatewayAndKeepsRealProcessFallback() throws Exception {
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));
        String aware = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));

        assertTrue(factory.contains("SettingsAwareAudioGenerationGateway"));
        assertTrue(aware.contains("resolveTtsCommandTemplate"));
        assertTrue(aware.contains("LocalTtsProcessAudioGenerationGateway"));
        assertTrue(aware.contains("MockAudioGenerationGateway"));
        assertTrue(aware.contains("configuration.enabled()"));
    }
}
