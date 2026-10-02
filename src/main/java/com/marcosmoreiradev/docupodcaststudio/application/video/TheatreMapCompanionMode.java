package com.marcosmoreiradev.docupodcaststudio.application.video;

/** Visual companion shown beside the spatial map during theatre export. */
public enum TheatreMapCompanionMode {
    FRAGMENT_VISUALS("Fragmentos visuales", "fragments"),
    CHARACTER_PHOTOS("Fotos de personajes", "characters"),
    CHARACTER_SCENERY("Fotos de personajes y escenografía", "scenery"),
    NONE("Sin acompanante", "none");

    private final String displayName;
    private final String frameMode;

    TheatreMapCompanionMode(String displayName, String frameMode) {
        this.displayName = displayName;
        this.frameMode = frameMode;
    }

    public String displayName() {
        return displayName;
    }

    public String frameMode() {
        return frameMode;
    }
}
