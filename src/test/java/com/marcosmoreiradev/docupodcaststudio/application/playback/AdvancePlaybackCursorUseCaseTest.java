package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancePlaybackCursorUseCaseTest {
    @Test
    void advancesAcrossCuesAndStopsAtEnd() {
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-JOB-1", "JOB-1", List.of(
                new PlaybackCue("SEG-001", 0, 1, "AUD-1", "jobs/JOB-1/audio/SEG-001.wav", "", "Uno"),
                new PlaybackCue("SEG-002", 1, 2, "AUD-2", "jobs/JOB-1/audio/SEG-002.wav", "", "Dos")
        ), "", Instant.EPOCH);
        AdvancePlaybackCursorUseCase useCase = new AdvancePlaybackCursorUseCase();

        PlaybackCursor next = useCase.advance(manifest, new PlaybackCursor("SEG-001", 0.8, false), 0.3);
        assertEquals("SEG-002", next.segmentId());
        assertEquals(1.1, next.positionSeconds(), 0.001);

        PlaybackCursor stopped = useCase.advance(manifest, next, 10.0);
        assertTrue(stopped.stoppedState());
    }
}
