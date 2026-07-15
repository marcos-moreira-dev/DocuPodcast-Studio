package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VIDEO-EXPORT1 keeps the normal video export as a final MP4 flow, not a technical package. */
final class VideoExport1FinalMp4FlowSourceTest {
    @Test
    void applicationHasFinalMp4UseCaseAndRequest() throws Exception {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ExportNarrativeVideoUseCase.java");
        String renderer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/RenderFinalVideoPlanUseCase.java");
        String request = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/FinalVideoExportRequest.java");
        String result = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/FinalVideoExportResult.java");

        assertTrue(useCase.contains("standard narrative-video plan"));
        assertTrue(renderer.contains("Mode-independent MP4 renderer"));
        assertTrue(renderer.contains("readyForFinalVideo()"));
        assertTrue(request.contains("targetMp4"));
        assertTrue(result.contains("Video MP4 exportado"));
    }

    @Test
    void shellShowsQualityDialogAndSaveFileChooser() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String optionsDialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportOptionsDialog.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String commandRegistry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");

        assertTrue(shell.contains("chooseVideoResolution"));
        assertTrue(shell.contains("videoExportOptionsDialog.show"));
        assertTrue(shell.contains("Exportar video final"));
        assertTrue(optionsDialog.contains("UHD_4K"));
        assertTrue(optionsDialog.contains("QHD_2K"));
        assertTrue(optionsDialog.contains("FULL_HD_1080"));
        assertTrue(optionsDialog.contains("HD_720"));
        assertTrue(optionsDialog.contains("ComboBox<Integer> framesPerSecond"));
        assertTrue(optionsDialog.contains("Fotogramas por segundo"));
        assertTrue(optionsDialog.contains("Codificador de video"));
        assertTrue(shell.contains("VideoEncoderPolicy.NVIDIA_NVENC"));
        assertTrue(shell.contains("defaultVideoEncoderPolicy"));
        assertTrue(shell.contains("availableVideoEncoderPolicies"));
        assertTrue(shell.contains("inspectComputeEnvironment().inspect(settings)"));
        assertFalse(shell.contains("ComboBox<Integer> framesPerSecond"));
        assertFalse(shell.contains("VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.NVIDIA_NVENC"));
        assertTrue(shell.contains("showSaveDialog"));
        assertTrue(viewModel.contains("exportFinalVideo(Path targetFile, SimpleVideoResolutionPreset resolution)"));
        assertTrue(commandRegistry.contains("Exportar video"));
        assertFalse(commandRegistry.contains("Exportar paquete de video simple"));
    }

    @Test
    void exportWorkflowDelegatesToNarrativeVideoUseCase() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ExportApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java");

        assertTrue(services.contains("ExportNarrativeVideoUseCase exportNarrativeVideo"));
        assertTrue(factory.contains("new ExportNarrativeVideoUseCase"));
        assertTrue(workflow.contains("exportFinalVideo(ProjectSession session"));
        assertTrue(workflow.contains("effectiveVideoEncoderPolicy"));
        assertTrue(workflow.contains("VideoEncoderPolicy.NVIDIA_NVENC"));
        assertTrue(workflow.contains("FinalVideoExportRequest request"));
        assertTrue(workflow.contains("applicationServices.export().exportNarrativeVideo()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
