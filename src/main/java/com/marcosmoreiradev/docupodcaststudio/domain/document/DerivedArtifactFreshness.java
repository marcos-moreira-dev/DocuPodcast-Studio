package com.marcosmoreiradev.docupodcaststudio.domain.document;

/**
 * Freshness of artifacts derived from the source document after a source refresh.
 */
public enum DerivedArtifactFreshness {
    CURRENT("vigente"),
    STALE("obsoleto"),
    REVIEW_REQUIRED("en revisión"),
    UNKNOWN("sin evaluar");

    private final String displayName;

    DerivedArtifactFreshness(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
