package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportReadinessTanda9SourceTest {
    @Test
    void creativeExportTargetsAreOwnedByApplicationCatalog() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ProjectExportTargetCatalog.java");

        assertTrue(catalog.contains("DOCUMENTARY_STUDIO"));
        assertTrue(catalog.contains("ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO"));
        assertTrue(catalog.contains("NARRATIVE_VIDEO"));
        assertTrue(catalog.contains("ExportableArtifactKind.FINAL_VIDEO_MP4"));
        assertTrue(catalog.contains("THEATRE_PRODUCTION"));
        assertTrue(catalog.contains("ExportableArtifactKind.THEATRE_WORK_VIDEO"));
        assertTrue(catalog.contains("ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO"));
        assertTrue(catalog.contains("ExportableArtifactKind.THEATRE_PORTION_VIDEO"));
        assertTrue(catalog.contains("supportTargets"));
        assertFalse(catalog.contains("presentation"));
        assertFalse(catalog.contains("AppCommandId"));
    }

    @Test
    void exportCenterUsesReadinessReportInsteadOfParallelFlags() throws Exception {
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterState.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterCoordinator.java");

        assertTrue(state.contains("ExportReadinessReport readinessReport"));
        assertTrue(coordinator.contains("inspectExportReadiness().inspect"));
        assertTrue(coordinator.contains("targetCatalog.creativeTargets"));
        assertTrue(coordinator.contains("case FINAL_VIDEO_MP4 -> AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertTrue(coordinator.contains("audioOnlyRemediable"));
        assertFalse(coordinator.contains("allAudioRendered"));
        assertFalse(coordinator.contains("hasVisualBindings"));
        assertFalse(coordinator.contains("readinessForAudio"));
        assertFalse(coordinator.contains("readinessForVideo"));
    }

    @Test
    void fullReadinessStillKeepsSupportArtifactsAuditable() throws Exception {
        String readiness = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java");
        String bundle = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/export/FileSystemProjectBundleExporter.java");

        assertTrue(readiness.contains("projectBundle(projectFile)"));
        assertTrue(readiness.contains("diagnosticReport(project)"));
        assertTrue(readiness.contains("storyboardSummary(script, storyboard)"));
        assertTrue(readiness.contains("simpleVideoPackage(script, storyboard, safeJobs)"));
        assertTrue(bundle.contains("EXPORT_READINESS.md"));
        assertTrue(bundle.contains("readiness.toMarkdown()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
