package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Text effect requested for theatrical video frames. */
public enum TheatreTextEffect {
    NONE("Ninguno"),
    SHADOW("Sombra"),
    SOLID_BORDER("Borde sólido");

    private final String label;

    TheatreTextEffect(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
