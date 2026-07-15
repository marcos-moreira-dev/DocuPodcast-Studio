package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StreamingPlaybackWindowTest {
    @Test
    void reportsInitialBufferLookaheadAndGapWaiting() {
        PlaybackBufferPolicy policy = PlaybackBufferPolicy.defaultPolicy();

        StreamingPlaybackWindow warmingUp = StreamingPlaybackWindow.of(policy, 3, 20, 0);
        assertFalse(warmingUp.readyToStart());
        assertTrue(warmingUp.readerStatusLabel().contains("Preparando buffer inicial"));

        StreamingPlaybackWindow playing = StreamingPlaybackWindow.of(policy, 8, 20, 2);
        assertTrue(playing.readyToStart());
        assertTrue(playing.nextSegmentReady());
        assertTrue(playing.readerStatusLabel().contains("Reproduciendo con buffer"));

        StreamingPlaybackWindow waiting = StreamingPlaybackWindow.of(policy, 6, 20, 5);
        assertTrue(waiting.waitingForNextChunk());
        assertTrue(waiting.readerStatusLabel().contains("Cargando el siguiente fragmento"));
    }
}
