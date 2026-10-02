package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import java.util.Locale;

/** Stable storyboard visual slots used by theatre playback and export. */
public enum TheatreVisualVariant {
    OFFICIAL("official", "Imagen asignada"),
    GENERATED("generated", "Imagen generada por IA"),
    DRAWN("drawn", "Frame dibujado"),
    SCENERY("scenery", "Personajes y escenografía");

    private final String metadataValue;
    private final String displayName;

    TheatreVisualVariant(String metadataValue, String displayName) {
        this.metadataValue = metadataValue;
        this.displayName = displayName;
    }

    public String metadataValue() { return metadataValue; }

    public String displayName() { return displayName; }

    public static TheatreVisualVariant fromMetadata(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        for (TheatreVisualVariant variant : values()) {
            if (variant.metadataValue.equals(normalized)) return variant;
        }
        return OFFICIAL;
    }
}
