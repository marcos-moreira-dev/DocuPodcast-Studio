package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Preferred text region for theatrical video frames. */
public enum TheatreTextPosition {
    TOP("Arriba"),
    CENTER("Centro"),
    BOTTOM("Abajo"),
    LEFT("Izquierda"),
    RIGHT("Derecha");

    private final String label;

    TheatreTextPosition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
