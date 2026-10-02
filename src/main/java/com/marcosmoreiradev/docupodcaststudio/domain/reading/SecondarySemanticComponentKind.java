package com.marcosmoreiradev.docupodcaststudio.domain.reading;

/** Stable product categories for non-prose PDF content. */
public enum SecondarySemanticComponentKind {
    TABLE,
    EQUATION,
    IMAGE,
    EXTRA,
    NONE;

    public boolean secondary() {
        return this != NONE;
    }
}
