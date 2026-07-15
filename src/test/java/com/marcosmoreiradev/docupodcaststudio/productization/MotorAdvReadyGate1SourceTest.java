package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class MotorAdvReadyGate1SourceTest {
    @Test
    void advancedVoiceHasDocumentGenerationGateBeyondDownload() throws Exception {
        String gate = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsDocumentGenerationReadinessUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/XttsDocumentGenerationReadinessReport.java"));
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));

        assertTrue(gate.contains("downloaded") || gate.contains("download"));
        assertTrue(gate.contains("generatedWavProof"));
        assertTrue(report.contains("canGenerateDocumentAudio"));
        assertTrue(gateway.contains("disabledAdvancedVoiceGateway"));
        assertTrue(gateway.contains("canGenerateDocumentAudio"));
    }

    @Test
    void advancedVoiceMissingSmokeIsNotMarkedUsableInPreflight() throws Exception {
        String status = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/AiEngineReadinessStatus.java"));
        String preflight = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/InspectAiEnginesPreflightUseCase.java"));

        assertTrue(status.contains("NEEDS_VERIFICATION"));
        assertTrue(preflight.contains("falta generar una prueba WAV real"));
        assertTrue(preflight.contains("AiEngineReadinessStatus.NEEDS_VERIFICATION"));
    }
}
