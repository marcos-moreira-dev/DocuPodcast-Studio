package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ActiveReadingAnchorTest {
    @Test
    void keepsActiveNarrationBlockInStableUpperReadingBand() {
        ActiveReadingAnchor anchor = ActiveReadingAnchor.defaultAnchor();

        assertEquals(0.0, anchor.scrollValueFor(0, 20));
        assertTrue(anchor.scrollValueFor(10, 20) > 0.0);
        assertTrue(anchor.scrollValueFor(19, 20) <= 1.0);
        assertTrue(anchor.userLabel().contains("zona estable de lectura"));
    }
}
