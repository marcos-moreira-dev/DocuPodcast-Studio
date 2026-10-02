package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Format-neutral kind used by document reading, audio and video workspaces. */
public enum DocumentContentKind {
    COVER,
    PROSE,
    TABLE,
    EQUATION,
    IMAGE,
    EXTRA;

    public boolean secondarySemanticComponent() {
        return this == TABLE || this == EQUATION || this == IMAGE || this == EXTRA;
    }
}
