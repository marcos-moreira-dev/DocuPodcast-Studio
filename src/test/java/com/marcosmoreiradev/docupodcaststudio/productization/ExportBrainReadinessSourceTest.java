package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportBrainReadinessSourceTest {
    @Test
    void exportBrainHasCentralReadinessReportAndNoUiDependency() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportReadinessReport.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ExportApplicationServices.java"));

        assertTrue(useCase.contains("ProjectExportFormatPolicy"));
        assertTrue(useCase.contains("ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE"));
        assertTrue(useCase.contains("ExportableArtifactKind.PODCAST_WAV"));
        assertTrue(useCase.contains("ExportableArtifactKind.PROJECT_BUNDLE"));
        assertTrue(useCase.contains("No hay audio final completo"));
        assertTrue(report.contains("Limitaciones honestas"));
        assertTrue(services.contains("InspectExportReadinessUseCase inspectExportReadiness"));
        assertTrue(!useCase.contains("javafx"));
    }

    @Test
    void projectBundleIncludesReadinessReportInAuditableReports() throws Exception {
        String exporter = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/export/FileSystemProjectBundleExporter.java"));
        assertTrue(exporter.contains("reports.resolve(\"EXPORT_READINESS.md\")"));
        assertTrue(exporter.contains("readiness.toMarkdown()"));
        assertTrue(exporter.contains("Estado exportaciones"));
    }
}
