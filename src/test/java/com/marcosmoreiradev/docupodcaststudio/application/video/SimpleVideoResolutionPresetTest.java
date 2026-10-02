package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimpleVideoResolutionPresetTest {
    @Test
    void defaultRemains2kAndUserCanChoose540pThrough4kInBothOrientations() {
        assertEquals(SimpleVideoResolutionPreset.QHD_2K, SimpleVideoResolutionPreset.defaultPreset());
        assertEquals(960, SimpleVideoResolutionPreset.LOW_540.width());
        assertEquals(540, SimpleVideoResolutionPreset.LOW_540.height());
        assertEquals(540, SimpleVideoResolutionPreset.LOW_VERTICAL_540X960.width());
        assertEquals(960, SimpleVideoResolutionPreset.LOW_VERTICAL_540X960.height());
        assertEquals(1280, SimpleVideoResolutionPreset.HD_720.width());
        assertEquals(1920, SimpleVideoResolutionPreset.FULL_HD_1080.width());
        assertEquals(2560, SimpleVideoResolutionPreset.QHD_2K.width());
        assertEquals(3840, SimpleVideoResolutionPreset.UHD_4K.width());
        assertEquals(SimpleVideoResolutionPreset.LOW_540,
                SimpleVideoResolutionPreset.fromDimensions(960, 540));
        assertEquals(SimpleVideoResolutionPreset.LOW_VERTICAL_540X960,
                SimpleVideoResolutionPreset.fromDimensions(540, 960));
        assertTrue(SimpleVideoResolutionPreset.QHD_2K.ffmpegScaleExpression().contains("2560:1440"));
    }
}
