package com.marcosmoreiradev.docupodcaststudio.application.compatibility.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.EnginePresetId;

import java.util.Locale;

/** Format/settings-v1 aliases. New product code stores only neutral preset ids. */
public final class LegacyEnginePresetMapper {
    private LegacyEnginePresetMapper() { }

    public static EnginePresetId image(String legacy) {
        return new EnginePresetId("draft");
    }

    public static EnginePresetId video(String legacy) {
        String value = legacy == null ? "" : legacy.strip().toUpperCase(Locale.ROOT);
        if (value.startsWith("WAN22_I2V_14B")) return new EnginePresetId("wan22-i2v-14b-quality");
        if (value.startsWith("LTX23")) return new EnginePresetId("ltx23-i2v-portrait");
        return new EnginePresetId("wan22-ti2v-5b-balanced");
    }
}
