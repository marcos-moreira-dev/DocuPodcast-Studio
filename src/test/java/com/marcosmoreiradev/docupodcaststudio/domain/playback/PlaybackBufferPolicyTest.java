package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackBufferPolicyTest {
    @Test
    void defaultPolicyStartsWithFiveSegmentsOrAllAvailableForShortDocuments() {
        PlaybackBufferPolicy policy = PlaybackBufferPolicy.defaultPolicy();

        assertEquals(5, policy.initialReadySegments());
        assertEquals(10, policy.lookaheadSegments());
        assertTrue(policy.pauseWhenBufferMissing());
        assertFalse(policy.canStart(4, 20));
        assertTrue(policy.canStart(5, 20));
        assertTrue(policy.canStart(2, 2));
        assertFalse(policy.canStart(0, 0));
        assertTrue(policy.userLabel().contains("Buffer: inicia con 5 fragmentos"));
    }
}
