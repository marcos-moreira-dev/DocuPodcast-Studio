package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioExportFinalHf1SourceTest {
    @Test
    void finalAudioReadinessChecksConcreteTargetFormatBeforeExport() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectFinalAudioExportReadinessUseCase.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));
        String readiness = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/InspectExportReadinessUseCase.java"));

        assertTrue(useCase.contains("AudioExportFormat.fromTarget(targetFile)"));
        assertTrue(useCase.contains("format.compressed()"));
        assertTrue(useCase.contains("Prepara Video local/FFmpeg"));
        assertTrue(useCase.contains("PlaybackManifest"));
        assertTrue(workflow.contains("ensureFinalAudioReady(jobs, manifest, targetFile, operational)"));
        assertTrue(workflow.contains("inspectFinalAudioExportReadiness()"));
        assertTrue(workflow.contains("No se puede exportar audio final"));
        assertTrue(readiness.contains("audio-final.wav / .mp3 / .aac"));
    }
}
