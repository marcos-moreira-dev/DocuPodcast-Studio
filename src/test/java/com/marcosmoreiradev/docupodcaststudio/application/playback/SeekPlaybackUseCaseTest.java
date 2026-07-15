package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SeekPlaybackUseCaseTest {
    @Test
    void seekPreservesPauseStateAndTargetsClickedSegment() {
        PlaybackCursor paused = PlaybackCursor.stopped();
        PlaybackCursor result = new SeekPlaybackUseCase().seek(paused, "SEG-007", 12.5);

        assertEquals("SEG-007", result.segmentId());
        assertEquals(12.5, result.positionSeconds());
        assertTrue(result.paused());
    }

    @Test
    void resumeAfterSeekSupportsClickablePlayback() {
        PlaybackCursor result = new SeekPlaybackUseCase()
                .seek(PlaybackCursor.stopped(), "SEG-008", 0.0)
                .resume();

        assertEquals("SEG-008", result.segmentId());
        assertFalse(result.paused());
    }
}
