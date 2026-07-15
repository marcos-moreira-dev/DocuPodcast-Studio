package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackCursorTest {
    @Test
    void canPauseResumeAndSeekToClickedSegment() {
        PlaybackCursor cursor = PlaybackCursor.stopped().seekTo("SEG-010", 42.0).resume();

        assertEquals("SEG-010", cursor.segmentId());
        assertEquals(42.0, cursor.positionSeconds(), 0.001);
        assertFalse(cursor.paused());
        assertTrue(cursor.pause().paused());
    }
}
