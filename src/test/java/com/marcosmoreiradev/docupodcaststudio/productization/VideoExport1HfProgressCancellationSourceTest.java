package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoExport1HfProgressCancellationSourceTest {
    @Test
    void finalVideoExportRunsInBackgroundAndCanBeCancelled() throws Exception {
        String shell = read("presentation/shell/DocuPodcastShellView.java");
        String progressCoordinator = read("presentation/video/VideoExportProgressCoordinator.java");
        String backgroundRunner = read("presentation/process/FxBackgroundTaskRunner.java");
        String viewModel = read("presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("presentation/shell/workflow/ExportWorkflowCoordinator.java");
        String renderer = read("application/video/RenderFinalVideoPlanUseCase.java");
        String runner = read("infrastructure/process/DefaultExternalProcessRunner.java");

        assertTrue(shell.contains("exportFinalVideoInBackground"));
        assertTrue(shell.contains("videoExportProgressCoordinator.export"));
        assertTrue(progressCoordinator.contains("VideoRenderProgressView"));
        assertTrue(progressCoordinator.contains("AtomicBoolean cancelRequested"));
        assertTrue(progressCoordinator.contains("backgroundTaskRunner.start(\"docupodcast-final-video-export\", task)"));
        assertTrue(backgroundRunner.contains("Thread worker = new Thread(task"));
        assertTrue(backgroundRunner.contains("worker.setDaemon(true)"));
        assertTrue(viewModel.contains("Consumer<VideoRenderProgress> progress"));
        assertTrue(coordinator.contains("BooleanSupplier cancellationRequested"));
        assertTrue(renderer.contains("ExternalProcessObserver"));
        assertTrue(renderer.contains("cancellationRequested"));
        assertTrue(runner.contains("destroyTree"));
        assertTrue(renderer.contains("usefulProgressLine"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio").resolve(relativePath));
    }
}
