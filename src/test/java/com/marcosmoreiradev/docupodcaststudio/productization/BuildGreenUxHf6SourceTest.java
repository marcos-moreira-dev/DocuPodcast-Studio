package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-HF6 guards the compile fixes after the playback/download patch. */
final class BuildGreenUxHf6SourceTest {
    @Test
    void playbackCueLookupDoesNotCaptureMutableManifestInLambda() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(source.contains("cueForCursor(PlaybackManifest manifest, PlaybackCursor cursor)"));
        assertTrue(source.contains("PlaybackManifest rebuiltManifest = rebuildPlaybackManifestFromLatestJob()"));
        assertTrue(source.contains("currentPlaybackManifest.set(rebuiltManifest)"));
        assertTrue(source.lines().count() <= 2700);
    }

    @Test
    void settingsDialogHasDetailedPiperSummaryInsteadOfTypeMismatch() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String piper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java"));
        assertTrue(dialog.contains("summarizeMissingDetailed(PiperSetupReadinessReport report)"));
        assertTrue(dialog.contains("PiperSettingsOperations.summarizeMissingDetailed(report)"));
        assertTrue(piper.contains("summarizeMissingDetailed(readiness)"));
    }
}
