package com.marcosmoreiradev.docupodcaststudio.application.image;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageEnhancementProfilesTest {
    @Test
    void outputProfilesExposeDeliveryResolutions() {
        assertEquals(1280, ImageEnhancementOutputProfile.HD_720.width());
        assertEquals(720, ImageEnhancementOutputProfile.HD_720.height());
        assertEquals(1920, ImageEnhancementOutputProfile.FHD_1080.width());
        assertEquals(1080, ImageEnhancementOutputProfile.FHD_1080.height());
        assertEquals(2560, ImageEnhancementOutputProfile.QHD_2K.width());
        assertEquals(1440, ImageEnhancementOutputProfile.QHD_2K.height());
        assertEquals(3840, ImageEnhancementOutputProfile.UHD_4K.width());
        assertEquals(2160, ImageEnhancementOutputProfile.UHD_4K.height());
    }

    @Test
    void professionalProfilesUseTiledPipeline() {
        assertTrue(ImageEnhancementOutputProfile.QHD_2K.professional());
        assertTrue(ImageEnhancementOutputProfile.UHD_4K.professional());
        assertTrue(ImageEnhancementOutputProfile.QHD_2K.pipelineProfile().requiresTiling());
        assertTrue(ImageEnhancementOutputProfile.UHD_4K.pipelineProfile().requiresTiling());
        assertTrue(ImageEnhancementOutputProfile.UHD_4K.pipelineProfile().supportsLora());
    }

    @Test
    void testModelIsExperimentalWhileFluxIsHighEnd() {
        assertFalse(ImageModelPackageProfile.TEST_4GB_SD15.highEnd());
        assertTrue(ImageModelPackageProfile.HIGH_QUALITY_FLUX.highEnd());
        assertNotEquals(ImageModelPackageProfile.TEST_4GB_SD15, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
    }
}
