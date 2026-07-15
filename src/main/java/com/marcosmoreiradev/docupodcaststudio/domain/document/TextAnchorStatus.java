package com.marcosmoreiradev.docupodcaststudio.domain.document;

/** Review state of a project-side text anchor after import or source refresh. */
public enum TextAnchorStatus {
    CURRENT,
    NEEDS_REVIEW,
    RELOCATED,
    ORPHANED
}
