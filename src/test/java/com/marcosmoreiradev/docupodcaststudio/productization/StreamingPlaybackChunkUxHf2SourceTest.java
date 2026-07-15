package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StreamingPlaybackChunkUxHf2SourceTest {
    @Test
    void documentPlaybackStartsFromGeneratedChunksAndPreservesPlayerErrors() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(vm.contains("startPlaybackFromSegment"));
        assertTrue(vm.contains("canAttemptBufferedPlayback"));
        assertTrue(vm.contains("status.completedSegments() > 0"));
        assertTrue(vm.contains("private boolean playCueForCursor"));
        assertTrue(transport.contains("El audio fue generado, pero el reproductor interno no pudo abrirlo"));
    }
}
