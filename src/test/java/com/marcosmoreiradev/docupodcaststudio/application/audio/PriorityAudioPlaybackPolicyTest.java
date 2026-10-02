package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PriorityAudioPlaybackPolicyTest {
    private final PriorityAudioPlaybackPolicy policy = new PriorityAudioPlaybackPolicy();

    @Test
    void keepsCompatiblePlaybackOnlyForPriorityGapFill() {
        assertTrue(policy.keepPlayback(true, new PlaybackCursor("SEG-READY", 1.5, false)));
        assertTrue(policy.keepPlayback(true, new PlaybackCursor("SEG-READY", 1.5, true)),
                "a paused compatible cue must remain resumable while gaps are filled");
        assertFalse(policy.keepPlayback(false, new PlaybackCursor("SEG-READY", 1.5, false)));
        assertFalse(policy.keepPlayback(true, PlaybackCursor.stopped()));
    }
}
