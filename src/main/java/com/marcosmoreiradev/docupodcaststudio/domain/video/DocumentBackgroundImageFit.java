package com.marcosmoreiradev.docupodcaststudio.domain.video;

/** How a documentary background image occupies the video frame. */
public enum DocumentBackgroundImageFit {
    CONTAIN("Imagen completa con franjas"),
    COVER("Rellenar el frame con recorte"),
    BLUR_AND_CONTAIN("Fondo difuminado e imagen completa");

    private final String displayName;

    DocumentBackgroundImageFit(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
