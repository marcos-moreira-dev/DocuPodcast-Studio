package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudySlideLayoutTest {
    @Test
    void standardFullHdSlideUsesTheSamePanoramicRegionAsTheEditor() {
        int margin = 1920 / 16;
        int availableWidth = 1920 - margin * 2;
        int illustrationWidth = DocumentStudySlideLayout.illustrationWidth(availableWidth);

        assertEquals(DocumentStudySlideLayout.ILLUSTRATION_WIDTH, illustrationWidth);
        assertEquals(DocumentStudySlideLayout.ILLUSTRATION_HEIGHT,
                DocumentStudySlideLayout.illustrationHeight(illustrationWidth, 1080));
        assertEquals(28.0 / 9.0, DocumentStudySlideLayout.ILLUSTRATION_ASPECT_RATIO, 0.0001);
    }

    @Test
    void mascotIsSmallerAndFlushWithTheSelectedBottomCorner() {
        Rectangle left = DocumentStudySlideCompositor.mascotBounds(
                1920, 1080, 432, 336, 600, 900, DocumentMascotPosition.BOTTOM_LEFT);
        Rectangle right = DocumentStudySlideCompositor.mascotBounds(
                1920, 1080, 432, 336, 600, 900, DocumentMascotPosition.BOTTOM_RIGHT);

        assertEquals(0, left.x);
        assertEquals(1080, left.y + left.height);
        assertEquals(1920, right.x + right.width);
        assertEquals(1080, right.y + right.height);
        assertEquals(left.width, right.width);
        assertEquals(left.height, right.height);
        assertTrue(left.width < 336);
    }

    @Test
    void namedMascotSizesGrowMonotonically() {
        Rectangle small = DocumentStudySlideCompositor.mascotBounds(
                1920, 1080, 432, 336, 600, 900, DocumentMascotPosition.BOTTOM_RIGHT, 10);
        Rectangle medium = DocumentStudySlideCompositor.mascotBounds(
                1920, 1080, 432, 336, 600, 900, DocumentMascotPosition.BOTTOM_RIGHT, 20);
        Rectangle large = DocumentStudySlideCompositor.mascotBounds(
                1920, 1080, 432, 336, 600, 900, DocumentMascotPosition.BOTTOM_RIGHT, 30);

        assertTrue(small.width < medium.width);
        assertTrue(medium.width < large.width);
        assertEquals(1920, large.x + large.width);
        assertEquals(1080, large.y + large.height);
    }
}
