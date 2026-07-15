package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedVoiceDownloadDiagnosticsSourceTest {
    @Test
    void advancedVoiceDownloadLeavesActionableDiagnosticsForSupport() throws Exception {
        String downloader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/XttsModelDownloadReport.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java"));

        assertTrue(downloader.contains("download-diagnostics.txt"));
        assertTrue(downloader.contains("URL técnica"));
        assertTrue(downloader.contains("HTTP:"));
        assertTrue(downloader.contains("Content-Length"));
        assertTrue(downloader.contains("Faltantes de inspección"));
        assertTrue(report.contains("Path diagnosticReport"));
        assertTrue(settings.contains("describeAdvancedVoiceSetupFailure"));
        assertTrue(settings.contains("Reporte técnico para soporte"));
        assertTrue(settings.contains("adjunta el reporte técnico"));
        assertTrue(settings.contains("No quedó seleccionable porque falta"));
    }
}
