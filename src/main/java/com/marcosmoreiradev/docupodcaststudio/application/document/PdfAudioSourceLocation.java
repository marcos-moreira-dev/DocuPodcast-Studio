package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Navigation target from an audio unit back to its canonical PDF regions. */
public record PdfAudioSourceLocation(int pageNumber, List<String> regionIds) {
    public PdfAudioSourceLocation {
        pageNumber = Math.max(0, pageNumber);
        regionIds = regionIds == null ? List.of() : List.copyOf(regionIds);
    }
}
