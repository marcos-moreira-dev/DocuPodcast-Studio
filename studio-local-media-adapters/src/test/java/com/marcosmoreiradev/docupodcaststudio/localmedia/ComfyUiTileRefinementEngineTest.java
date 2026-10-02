package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComfyUiTileRefinementEngineTest {
    @Test
    void tilePositionsCoverLandscapePortraitAndSquareEdges() {
        assertCovered(3840, 512, 64);
        assertCovered(2160, 512, 64);
        assertCovered(1024, 512, 64);
        assertEquals(List.of(0), ComfyUiTileRefinementEngine.positions(384, 512, 64));
    }

    @Test
    void edgePaddingUsesMultiplesOfEightWithoutInventingTransparentBorders() {
        BufferedImage source = new BufferedImage(541, 539, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(540, 538, 0x7f123456);

        BufferedImage padded = ComfyUiTileRefinementEngine.padToMultipleOfEight(source);

        assertEquals(544, padded.getWidth());
        assertEquals(544, padded.getHeight());
        assertEquals(0x7f123456, padded.getRGB(543, 543));
    }

    @Test
    void finalMergePreservesTheOriginalAlphaChannel() {
        BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00112233);
        source.setRGB(1, 0, 0x7f445566);

        BufferedImage merged = ComfyUiTileRefinementEngine.merge(
                source,
                new float[]{255, 0},
                new float[]{0, 255},
                new float[]{0, 0},
                new float[]{1, 1});

        assertEquals(0, (merged.getRGB(0, 0) >>> 24) & 0xff);
        assertEquals(0x7f, (merged.getRGB(1, 0) >>> 24) & 0xff);
    }

    private static void assertCovered(int length, int tile, int overlap) {
        List<Integer> positions = ComfyUiTileRefinementEngine.positions(length, tile, overlap);
        assertEquals(0, positions.getFirst());
        assertEquals(length - tile, positions.getLast());
        for (int index = 1; index < positions.size(); index++) {
            int gap = positions.get(index) - positions.get(index - 1);
            assertTrue(gap > 0 && gap <= tile - overlap);
        }
    }
}
