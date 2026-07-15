package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportReadinessUx2SourceTest {
    @Test
    void readinessIncludesFinalMp4AndBlocksBeforeLateFfmpegRenderFailure() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java"));
        String kind = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportableArtifactKind.java"));
        String format = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/DocuPodcastExportFormat.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));

        assertTrue(kind.contains("FINAL_VIDEO_MP4"));
        assertTrue(format.contains("MP4"));
        assertTrue(useCase.contains("finalVideoMp4"));
        assertTrue(useCase.contains("No hay fragmentos con imagen asociada"));
        assertTrue(useCase.contains("Falta audio listo"));
        assertTrue(workflow.contains("ensureFinalVideoReady"));
        assertTrue(workflow.contains("No se puede exportar MP4 final"));
        assertTrue(!useCase.contains("javafx"));
    }
}
