package com.marcosmoreiradev.docupodcaststudio.application.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ImageGenerationSettingsTest {
    @Test
    void defaultsToSixHourGenerationTimeout() {
        assertEquals(21_600, ImageGenerationSettings.defaults().timeoutSeconds());
        assertEquals("PRODUCTION_SDXL_REFERENCE", ImageGenerationSettings.defaults().preset());
        assertEquals("sd_xl_base_1.0.safetensors", ImageGenerationSettings.defaults().modelName());
    }

    @Test
    void acceptsSixHoursAndRepairsValuesAboveTheLimit() {
        ImageGenerationSettings sixHours = settings(21_600);
        ImageGenerationSettings tooLarge = settings(21_601);

        assertEquals(21_600, sixHours.timeoutSeconds());
        assertEquals(21_600, tooLarge.timeoutSeconds());
    }

    private static ImageGenerationSettings settings(int timeoutSeconds) {
        return new ImageGenerationSettings(
                "managed-local",
                "http://127.0.0.1:8188",
                "AUTO",
                "TEST_4GB_SD15",
                "v1-5-pruned-emaonly-fp16.safetensors",
                "models/image/adapters",
                timeoutSeconds,
                true);
    }
}
