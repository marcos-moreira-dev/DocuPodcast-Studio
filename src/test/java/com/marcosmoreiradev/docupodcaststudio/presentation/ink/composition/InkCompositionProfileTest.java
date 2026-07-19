package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkCompositionProfileTest {
    @Test
    void documentaryProfileIsWhitePanoramicAndAllowsMultipleImages() {
        InkCompositionProfile profile = InkCompositionProfile.documentaryIllustration();

        assertEquals(1344, profile.logicalWidth());
        assertEquals(432, profile.logicalHeight());
        assertEquals(28.0 / 9.0, profile.aspectRatio(), 0.0001);
        assertEquals(Color.WHITE, profile.initialBackground());
        assertTrue(profile.multipleImages());
    }
}
