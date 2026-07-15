package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

/** Background source for Estudio documental text+audio frames. */
public enum DocumentTextVideoBackgroundMode {
    SOLID_COLOR("Color de fondo"),
    IMAGE("Imagen de fondo");

    private final String displayName;

    DocumentTextVideoBackgroundMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
