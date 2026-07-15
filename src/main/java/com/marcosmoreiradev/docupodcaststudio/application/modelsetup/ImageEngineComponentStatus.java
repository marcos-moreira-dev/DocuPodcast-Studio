package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** Honest install status for a concrete image-engine artifact. */
public enum ImageEngineComponentStatus {
    MISSING("pendiente"),
    READY("listo"),
    INVALID("invalido"),
    PLACEHOLDER("placeholder"),
    OPTIONAL_MISSING("opcional pendiente");

    private final String displayName;

    ImageEngineComponentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean ready() {
        return this == READY;
    }

    public boolean blocking() {
        return this == MISSING || this == INVALID || this == PLACEHOLDER;
    }
}
