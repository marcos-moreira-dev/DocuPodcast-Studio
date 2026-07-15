package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimpleVideoResolutionPresetTest {
    @Test
    void defaultIs2kAndUserCanChoose720p1080pAnd4k() {
        assertEquals(SimpleVideoResolutionPreset.QHD_2K, SimpleVideoResolutionPreset.defaultPreset());
        assertEquals(1280, SimpleVideoResolutionPreset.HD_720.width());
        assertEquals(1920, SimpleVideoResolutionPreset.FULL_HD_1080.width());
        assertEquals(2560, SimpleVideoResolutionPreset.QHD_2K.width());
        assertEquals(3840, SimpleVideoResolutionPreset.UHD_4K.width());
        assertTrue(SimpleVideoResolutionPreset.QHD_2K.ffmpegScaleExpression().contains("2560:1440"));
    }
}
