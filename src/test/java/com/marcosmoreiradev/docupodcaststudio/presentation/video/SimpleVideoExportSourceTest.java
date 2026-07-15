package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimpleVideoExportSourceTest {
    @Test
    void shellExposesSimpleVideoExportWithoutTurningDocumentIntoVideoEditor() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ExportSimpleVideoPackageUseCase.java"));

        assertTrue(shell.contains("commandItem(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE)"));
        assertTrue(shell.contains("handleExportSimpleVideo"));
        assertTrue(viewModel.contains("exportSimpleVideoPackage"));
        assertTrue(useCase.contains("VIDEO_SIMPLE_PLAN.md"));
        assertTrue(useCase.contains("frames.csv"));
        assertTrue(useCase.contains("ffmpeg-concat.txt"));
        assertTrue(useCase.contains("RENDER_MANIFEST.json"));
        assertTrue(useCase.contains("render-commands.txt"));
        assertTrue(useCase.contains("paquete renderizable"));
        assertTrue(useCase.contains("La exportación no crea el MP4 final por sí sola"));
    }
}
