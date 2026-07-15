package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackSequentialQueueRefreshSourceTest {
    @Test
    void sequentialQueueCanRefreshManifestWhilePlaybackIsActive() throws IOException {
        String driver = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackSequentialQueueDriver.java"));
        String transport = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));

        assertTrue(driver.contains("public void refresh(PlaybackManifest manifest)"));
        assertTrue(driver.contains("refreshOnQueueThread(manifest.cues())"));
        assertTrue(driver.contains("String activeUnitId = cues.get(index).unitId()"));
        assertTrue(driver.contains("cues = List.copyOf(refreshedCues)"));
        assertTrue(transport.contains("refreshSequentialQueue(PlaybackManifest manifest)"));
        assertTrue(transport.contains("sequentialQueue.refresh(manifest)"));
    }

    @Test
    void shellFeedsNewManifestsToActiveSequentialQueueAndRechecksCompletion() throws IOException {
        String shell = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(shell.contains("playbackTransport.refreshSequentialQueue(bufferedManifest)"));
        assertTrue(shell.contains("playbackTransport.refreshSequentialQueue(completedManifest)"));
        assertTrue(shell.contains("lastSequentialCueUnitId"));
        assertTrue(shell.contains("manifest.nextCueAfterUnit(completedUnitId)"));
        assertTrue(shell.contains("Manifest actualizado. Continuando lectura secuencial"));
    }
}
