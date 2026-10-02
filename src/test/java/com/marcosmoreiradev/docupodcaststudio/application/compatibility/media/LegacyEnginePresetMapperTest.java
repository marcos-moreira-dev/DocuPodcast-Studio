package com.marcosmoreiradev.docupodcaststudio.application.compatibility.media;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LegacyEnginePresetMapperTest {
    @Test void preservesTheMeaningOfEveryLegacyImagePreset() {
        assertEquals("draft", LegacyEnginePresetMapper.image("TEST_4GB_SD15").value());
        assertEquals("sd15-dreamshaper", LegacyEnginePresetMapper.image("SD15_DREAMSHAPER").value());
        assertEquals("sdxl-reference", LegacyEnginePresetMapper.image("PRODUCTION_SDXL_REFERENCE").value());
        assertEquals("flux-kontext", LegacyEnginePresetMapper.image("ADVANCED_FLUX_KONTEXT").value());
        assertEquals("flux-high-quality", LegacyEnginePresetMapper.image("HIGH_QUALITY_FLUX").value());
        assertEquals("custom-comfy-workflow", LegacyEnginePresetMapper.image("CUSTOM_COMFY_WORKFLOW").value());
    }
}
