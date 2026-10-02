package com.marcosmoreiradev.docupodcaststudio.domain.video;

/** Readability effect applied to documentary video text. */
public enum DocumentTextEffect {
    NONE("Sin efecto"),
    SHADOW("Sombra"),
    SOLID_OUTLINE("Borde sólido");

    private final String displayName;

    DocumentTextEffect(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
