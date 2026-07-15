package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedVoiceSmoke4RSourceTest {
    @Test
    void settingsHasExplicitAdvancedVoiceProofButtonAndUseCase() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String advancedVoice = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));

        assertTrue(settings.contains("Button test") && settings.contains("Probar"));
        assertTrue(settings.contains("runXttsReadinessSmoke"));
        assertTrue(advancedVoice.contains("runReadinessSmoke"));
        assertTrue(advancedVoice.contains("Generando WAV real de prueba"));
        assertTrue(advancedVoice.contains("RunXttsReadinessSmokeUseCase"));
        assertTrue(services.contains("RunXttsReadinessSmokeUseCase"));
        assertTrue(factory.contains("infrastructure.voiceTestSynthesisGateway()"));
    }

    @Test
    void advancedVoiceDownloadCanResumePartialLargeFiles() throws Exception {
        String downloader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));

        assertTrue(downloader.contains("Range"));
        assertTrue(downloader.contains("bytes="));
        assertTrue(downloader.contains("206"));
        assertTrue(downloader.contains("se reanudará"));
        assertFalse(downloader.contains("No uses Python global"), "La regla de Python local no debe ser texto suelto de UI técnica aquí.");
    }

    @Test
    void preflightSeparatesDownloadedFromGeneratedAndPlayed() throws Exception {
        String preflight = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/InspectAiEnginesPreflightUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/XttsSmokeTestReport.java"));

        assertTrue(preflight.contains("Prueba WAV real generada"));
        assertTrue(preflight.contains("falta generar una prueba WAV real"));
        assertTrue(preflight.contains("falta confirmar reproducción dentro de la app"));
        assertTrue(report.contains("generatedWavProof"));
        assertTrue(report.contains("fullyVerified"));
    }
}
