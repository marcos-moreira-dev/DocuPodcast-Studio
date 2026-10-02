package com.marcosmoreiradev.docupodcaststudio.domain.batch;

public enum BrandingPlacement {
    TOP_LEFT("Arriba a la izquierda"),
    TOP_RIGHT("Arriba a la derecha"),
    BOTTOM_LEFT("Abajo a la izquierda"),
    BOTTOM_RIGHT("Abajo a la derecha");

    private final String label;

    BrandingPlacement(String label) { this.label = label; }
    public String label() { return label; }
    @Override public String toString() { return label; }
}
