package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE1/CORE3: runtime playback must expose actionable diagnostics while continuity is being isolated. */
final class PlaybackCore1RuntimeDiagnosticsSourceTest {
    @Test
    void runtimeDiagnosticsSnapshotReportsCuePathPlayerAndQueueState() throws IOException {
        String diagnostic = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackRuntimeDiagnostics.java"));
        assertTrue(diagnostic.contains("record PlaybackRuntimeDiagnostics"));
        assertTrue(diagnostic.contains("audioFileExists"));
        assertTrue(diagnostic.contains("playerPlaying"));
        assertTrue(diagnostic.contains("localPositionSeconds"));
        assertTrue(diagnostic.contains("queueLabel"));
        assertTrue(diagnostic.contains("Diagnóstico playback"));
    }

    @Test
    void shellUsesDiagnosticsWhenContinuationFailsOrRunsSequentialQueue() throws IOException {
        String shell = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(shell.contains("playbackRuntimeDiagnosticLabel"));
        assertTrue(transport.contains("audioFile(projectFile, activeCue)"));
        assertTrue(shell.contains("No se pudo continuar con el siguiente fragmento"));
        assertTrue(transport.contains("No existe el audio del fragmento"));
        assertTrue(shell.contains("Continuando lectura secuencial"));
    }
}
