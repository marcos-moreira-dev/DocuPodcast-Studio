package com.marcosmoreiradev.docupodcaststudio.domain.storyboard;

/** How an associated image should be displayed in the live storyboard. */
public enum StoryboardDisplayMode {
    FIT_CONTAIN("Ajustar completa"),
    FILL_CROP("Llenar recortando"),
    ORIGINAL_SIZE("Tamaño original"),
    CENTERED("Centrada");

    private final String displayName;

    StoryboardDisplayMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
