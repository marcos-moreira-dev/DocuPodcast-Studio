package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportVoiceSamplesTe1SourceTest {
    @Test
    void bundleExporterWritesVoiceSampleReportsAndManifestCounters() throws Exception {
        String exporter = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/export/FileSystemProjectBundleExporter.java"));
        assertTrue(exporter.contains("BuildVoiceReferenceSamplesExportReportUseCase"));
        assertTrue(exporter.contains("VOICE_REFERENCE_SAMPLES.md"));
        assertTrue(exporter.contains("VOICE_REFERENCE_SAMPLES.tsv"));
        assertTrue(exporter.contains("Muestras de voz por tono"));
        assertTrue(exporter.contains("voiceSamplesReport.sampleCount()"));
        assertTrue(exporter.contains("voiceSamplesReport.missingFileCount()"));
        assertFalse(exporter.contains("storyboard_resumen.md"));
    }

    @Test
    void resultAndCoordinatorExposeVoiceSampleCountWithoutTechnicalEngineNames() throws Exception {
        String result = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ProjectBundleExportResult.java"));
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));
        assertTrue(result.contains("Path voiceReferenceSamplesReportFile"));
        assertTrue(result.contains("Path voiceReferenceSamplesIndexFile"));
        assertTrue(result.contains("int voiceReferenceSampleCount"));
        assertTrue(coordinator.contains("muestras de voz"));
        assertFalse(coordinator.contains("Piper"));
        assertFalse(coordinator.contains("Coqui"));
        assertFalse(coordinator.contains("XTTS"));
    }
}
