package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF5: Exportar owns final-output choices, and reusable presentation workers stay outside the shell. */
final class PresentationOrchestrationRf5ExportWorkersSourceTest {
    @Test
    void videoExportOptionsLiveOutsideShell() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportOptionsDialog.java");
        String options = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportOptions.java");

        assertTrue(shell.contains("videoExportOptionsDialog.show"));
        assertFalse(shell.contains("ComboBox<Integer> framesPerSecond"));
        assertTrue(dialog.contains("ComboBox<Integer> framesPerSecond"));
        assertTrue(dialog.contains("Fotogramas por segundo"));
        assertTrue(dialog.contains("Codificador de video"));
        assertTrue(options.contains("record VideoExportOptions"));
    }

    @Test
    void finalVideoWorkerUsesSharedPresentationTaskRunner() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String progress = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportProgressCoordinator.java");
        String runner = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/FxBackgroundTaskRunner.java");

        assertTrue(shell.contains("videoExportProgressCoordinator.export"));
        assertTrue(shell.contains("backgroundTaskRunner.start(\"docupodcast-source-import\", task)"));
        assertFalse(shell.contains("new Thread(task"));
        assertTrue(progress.contains("Task<Void> task = new Task<>()"));
        assertTrue(progress.contains("backgroundTaskRunner.start(\"docupodcast-final-video-export\", task)"));
        assertTrue(runner.contains("Thread worker = new Thread(task"));
        assertTrue(runner.contains("worker.setDaemon(true)"));
    }

    @Test
    void rf5IsRecordedInCurrentDocumentation() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");

        assertTrue(bitacora.contains("Paso RF5-01: Exportar y workers de presentación"));
        assertTrue(bitacora.contains("VideoExportOptionsDialog"));
        assertTrue(bitacora.contains("FxBackgroundTaskRunner"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF5"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
