package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreImageAspectRatioTest {
    @Test
    void defaultSixteenNineKeepsFhdDimensions() {
        assertEquals(1920, TheatreImageAspectRatio.WIDE_16_9.widthFor(ImageEnhancementOutputProfile.FHD_1080));
        assertEquals(1080, TheatreImageAspectRatio.WIDE_16_9.heightFor(ImageEnhancementOutputProfile.FHD_1080));
    }

    @Test
    void alternateRatiosDeriveDimensionsFromProfileHeight() {
        assertEquals(1080, TheatreImageAspectRatio.VERTICAL_9_16.widthFor(ImageEnhancementOutputProfile.FHD_1080));
        assertEquals(1920, TheatreImageAspectRatio.VERTICAL_9_16.heightFor(ImageEnhancementOutputProfile.FHD_1080));
        assertEquals(1080, TheatreImageAspectRatio.SQUARE_1_1.widthFor(ImageEnhancementOutputProfile.FHD_1080));
        assertEquals(1080, TheatreImageAspectRatio.SQUARE_1_1.heightFor(ImageEnhancementOutputProfile.FHD_1080));
        assertEquals(2520, TheatreImageAspectRatio.CINEMA_21_9.widthFor(ImageEnhancementOutputProfile.FHD_1080));
        assertEquals(1080, TheatreImageAspectRatio.CINEMA_21_9.heightFor(ImageEnhancementOutputProfile.FHD_1080));
    }
}
