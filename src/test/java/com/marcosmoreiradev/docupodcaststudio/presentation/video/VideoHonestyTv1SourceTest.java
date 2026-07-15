package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoHonestyTv1SourceTest {
    @Test
    void videoExportReportsPackageHonestyInsteadOfPromisingMp4Final() throws Exception {
        String result = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/SimpleVideoPackageExportResult.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ExportSimpleVideoPackageUseCase.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));
        String readiness = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java"));

        assertTrue(result.contains("renderModeLabel"));
        assertTrue(result.contains("renderableAsMp4"));
        assertTrue(result.contains("outputFileName"));
        assertTrue(result.contains("honestStatusLabel"));
        assertTrue(useCase.contains("La exportación no crea el MP4 final por sí sola"));
        assertTrue(useCase.contains("no creo el MP4 final por si sola"));
        assertTrue(workflow.contains("Paquete de video simple preparado"));
        assertTrue(workflow.contains("result.honestStatusLabel()"));
        assertTrue(readiness.contains("la exportación no crea MP4 por sí sola"));
    }
}
