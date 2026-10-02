package com.marcosmoreiradev.docupodcaststudio.application.compatibility.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.EnginePresetId;

import java.util.Locale;

/** Format/settings-v1 aliases. New product code stores only neutral preset ids. */
public final class LegacyEnginePresetMapper {
    private LegacyEnginePresetMapper() { }

    public static EnginePresetId image(String legacy) {
        String value = legacy == null ? "" : legacy.strip().toUpperCase(Locale.ROOT);
        if (value.equals("CONTEXTUAL_4GB_SD15")) return new EnginePresetId("sd15-regional-identity");
        if (value.equals("SD15_DREAMSHAPER")) return new EnginePresetId("sd15-dreamshaper");
        if (value.equals("PRODUCTION_SDXL_REFERENCE")) return new EnginePresetId("sdxl-reference");
        if (value.equals("ADVANCED_FLUX_KONTEXT")) return new EnginePresetId("flux-kontext");
        if (value.equals("HIGH_QUALITY_FLUX")) return new EnginePresetId("flux-high-quality");
        if (value.equals("CUSTOM_COMFY_WORKFLOW")) return new EnginePresetId("custom-comfy-workflow");
        return new EnginePresetId("draft");
    }

    public static EnginePresetId video(String legacy) {
        String value = legacy == null ? "" : legacy.strip().toUpperCase(Locale.ROOT);
        if (value.startsWith("WAN22_I2V_14B")) return new EnginePresetId("wan22-i2v-14b-quality");
        if (value.startsWith("LTX23")) return new EnginePresetId("ltx23-i2v-portrait");
        return new EnginePresetId("wan22-ti2v-5b-balanced");
    }
}
