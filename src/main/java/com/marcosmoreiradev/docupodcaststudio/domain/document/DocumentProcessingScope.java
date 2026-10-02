package com.marcosmoreiradev.docupodcaststudio.domain.document;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.DocumentAudioPreparationExtent;

/** Stable user intent for choosing how much of a document is processed. */
public enum DocumentProcessingScope {
    SINGLE_FRAGMENT("Solo este fragmento", true,
            DocumentAudioPreparationExtent.SINGLE_FRAGMENT),
    FROM_SELECTION("Desde aquí", true,
            DocumentAudioPreparationExtent.ALL_FROM_SELECTION),
    INTERVAL("Intervalo", true,
            DocumentAudioPreparationExtent.ALL_FROM_SELECTION),
    FULL_DOCUMENT("Lectura completa", false,
            DocumentAudioPreparationExtent.ALL_FROM_SELECTION);

    private final String displayName;
    private final boolean legacyPortionEnabled;
    private final DocumentAudioPreparationExtent legacyExtent;

    DocumentProcessingScope(String displayName, boolean legacyPortionEnabled,
                            DocumentAudioPreparationExtent legacyExtent) {
        this.displayName = displayName;
        this.legacyPortionEnabled = legacyPortionEnabled;
        this.legacyExtent = legacyExtent;
    }

    public boolean legacyPortionEnabled() {
        return legacyPortionEnabled;
    }

    public DocumentAudioPreparationExtent legacyExtent() {
        return legacyExtent;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
