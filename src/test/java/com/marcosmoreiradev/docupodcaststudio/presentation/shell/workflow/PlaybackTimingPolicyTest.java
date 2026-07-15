package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaybackTimingPolicyTest {
    @Test
    void watchdogUsesAcceleratedDurationInsteadOfOneXDuration() {
        PlaybackTimingPolicy policy = PlaybackTimingPolicy.defaults();

        long delay = policy.watchdogDelayMillis(4.0, 0.0, 1.75);

        assertTrue(delay < 3000L, "A 1.75x un cue de 4s no debe esperar cerca de la duración 1x.");
        assertTrue(delay >= 2200L, "El watchdog conserva un margen breve para no cortar audio real.");
    }

    @Test
    void normalizesSupportedPlaybackRates() {
        PlaybackTimingPolicy policy = PlaybackTimingPolicy.defaults();

        assertEquals(1.0, policy.normalizeRate(1.2));
        assertEquals(1.5, policy.normalizeRate(1.5));
        assertEquals(1.75, policy.normalizeRate(1.75));
    }

    @Test
    void keepsShortPauseBetweenGeneratedCues() {
        assertEquals(1000L, PlaybackTimingPolicy.defaults().interCuePauseMillis());
    }
}
