package com.marcosmoreiradev.docupodcaststudio.presentation.export;

/**
 * Explicit product intent for a creative export.
 *
 * <p>{@link #READY_ONLY} never starts documentary preparation implicitly.
 * {@link #PREPARE_FULL_DOCUMENT_AND_EXPORT} authorizes the complete, unattended
 * preparation chain before the selected artifact is exported.</p>
 */
public enum ExportExecutionMode {
    READY_ONLY,
    PREPARE_FULL_DOCUMENT_AND_EXPORT;

    public boolean preparesFullDocument() {
        return this == PREPARE_FULL_DOCUMENT_AND_EXPORT;
    }
}
