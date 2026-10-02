package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VisualResolutionProfileTest {
    @Test
    void exposesExact540pAndModelSafeWorkingDimensions() {
        assertEquals(new VisualResolutionProfile.Dimensions(960, 540),
                VisualResolutionProfile.P540.deliveryDimensions(16, 9));
        assertEquals(new VisualResolutionProfile.Dimensions(960, 544),
                VisualResolutionProfile.P540.workingDimensions(16, 9));
        assertEquals(new VisualResolutionProfile.Dimensions(540, 960),
                VisualResolutionProfile.P540.deliveryDimensions(9, 16));
        assertEquals(new VisualResolutionProfile.Dimensions(544, 960),
                VisualResolutionProfile.P540.workingDimensions(9, 16));
    }

    @Test
    void offersOnlyStrictlyHigherDeliveryProfiles() {
        assertEquals(List.of(VisualResolutionProfile.P720, VisualResolutionProfile.P1080,
                        VisualResolutionProfile.QHD_2K, VisualResolutionProfile.UHD_4K),
                VisualResolutionProfile.higherThanProfile(VisualResolutionProfile.P540));
        assertEquals(List.of(VisualResolutionProfile.P1080, VisualResolutionProfile.QHD_2K,
                        VisualResolutionProfile.UHD_4K),
                VisualResolutionProfile.higherThanProfile(VisualResolutionProfile.P720));
        assertTrue(VisualResolutionProfile.higherThanProfile(VisualResolutionProfile.UHD_4K).isEmpty());
    }

    @Test
    void videoCatalogContainsHorizontalAndVertical540p() {
        assertEquals(960, SimpleVideoResolutionPreset.LOW_540.width());
        assertEquals(540, SimpleVideoResolutionPreset.LOW_540.height());
        assertEquals(540, SimpleVideoResolutionPreset.LOW_VERTICAL_540X960.width());
        assertEquals(960, SimpleVideoResolutionPreset.LOW_VERTICAL_540X960.height());
        assertEquals(SimpleVideoResolutionPreset.LOW_VERTICAL_540X960,
                SimpleVideoResolutionPreset.fromDimensions(540, 960));
    }
}
