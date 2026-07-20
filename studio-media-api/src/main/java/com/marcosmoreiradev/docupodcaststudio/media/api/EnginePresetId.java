package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Extensible adapter-owned preset identifier. */
public record EnginePresetId(String value) {
    public static final EnginePresetId AUTO = new EnginePresetId("auto");

    public EnginePresetId {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("engine preset id is required");
    }
}
