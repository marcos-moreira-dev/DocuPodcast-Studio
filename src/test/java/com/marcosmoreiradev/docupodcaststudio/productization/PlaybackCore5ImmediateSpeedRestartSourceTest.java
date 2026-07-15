package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE5: speed buttons restart the active cue immediately instead of waiting old duration. */
final class PlaybackCore5ImmediateSpeedRestartSourceTest {
    @Test
    void speedChangeRestartsActiveCueImmediatelyFromCueStart() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(shell.contains("restartActiveCueImmediatelyAtRate(activeCue.get(), normalized)"));
        assertTrue(shell.contains("private void restartActiveCueImmediatelyAtRate(PlaybackCue cue, double normalizedRate)"));
        assertTrue(shell.contains("playbackTransport.restartActiveCueAtRate(normalizedRate)"));
        assertTrue(transport.contains("player.stop();"));
        assertTrue(transport.contains("continuation.stop();"));
        assertTrue(transport.contains("player.setPlaybackRate(rate);"));
        assertTrue(shell.contains("playbackCursor.set(new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false))"));
        assertTrue(shell.contains("boolean restarted = playExactCue(cue, 0.0)"));
        assertTrue(shell.contains("playbackTransport.setSequentialPlaybackRate(normalizedRate, 0.0)"));
    }

    @Test
    void activeSpeedChangeDoesNotUsePlayerInternalPositionBasedRestart() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        int activeBranch = shell.indexOf("if (current != null && current.playing() && activeCue.isPresent())");
        int elseBranch = shell.indexOf("} else {", activeBranch);
        String branch = shell.substring(activeBranch, elseBranch);
        assertFalse(branch.contains("currentPositionSeconds()"));
        assertFalse(branch.contains("startCue(activeCue.get(), local"));
    }
}
