package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfPlaybackVisualGuardTest {

    @Test
    void rejectsAPaintEventFromThePreviousPlaybackTransition() {
        PdfPlaybackVisualGuard guard = new PdfPlaybackVisualGuard();
        assertTrue(guard.request(1, "R1"));
        assertTrue(guard.request(2, "R2"));

        assertFalse(guard.painted(1, "R1"));
        assertEquals("R2", guard.snapshot().expectedRegionId());
        assertEquals("", guard.snapshot().paintedRegionId());

        assertTrue(guard.painted(2, "R2"));
        assertTrue(guard.snapshot().identityMatches());
    }

    @Test
    void keepsExpectedAndPaintedIdentityAlignedAcrossConsecutiveRegions() {
        PdfPlaybackVisualGuard guard = new PdfPlaybackVisualGuard();
        for (int index = 1; index <= 7; index++) {
            String regionId = "R" + index;
            assertTrue(guard.request(index, regionId));
            assertTrue(guard.painted(index, regionId));
            assertEquals(regionId, guard.snapshot().expectedRegionId());
            assertEquals(regionId, guard.snapshot().paintedRegionId());
        }
    }
}
