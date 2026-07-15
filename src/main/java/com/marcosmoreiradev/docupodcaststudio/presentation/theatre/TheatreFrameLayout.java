package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Layout strategy for theatrical video frames. */
public enum TheatreFrameLayout {
    TEXT_ONLY("Solo texto"),
    IMAGE_OR_TEXT("Solo imagen/texto"),
    IMAGE_WITH_TEXT("Imagen con texto"),
    IMAGE_WITH_SPATIAL_MAP("Imagen + mapa espacial");

    private final String label;

    TheatreFrameLayout(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
