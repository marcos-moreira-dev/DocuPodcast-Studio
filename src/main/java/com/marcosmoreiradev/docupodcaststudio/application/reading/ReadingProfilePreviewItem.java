package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;

/** One planned classification change produced by a Reading Profile preview. */
public record ReadingProfilePreviewItem(
        String blockId,
        String previewText,
        DocumentBlockType currentType,
        DocumentBlockType proposedType,
        boolean manualOverride
) {
    public boolean changed() {
        return currentType != proposedType;
    }
}
