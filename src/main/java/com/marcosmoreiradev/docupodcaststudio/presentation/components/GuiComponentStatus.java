package com.marcosmoreiradev.docupodcaststudio.presentation.components;

/** Lifecycle marker for the shared GUI component inventory. */
public enum GuiComponentStatus {
    /** Existing reusable component that new GUI work must reuse instead of duplicating controls. */
    FROZEN,
    /** Small component introduced as a stable building block for the upcoming GUI redesign. */
    READY,
    /** Historical or transitional component that may remain until the redesign replaces it. */
    LEGACY_BRIDGE
}
