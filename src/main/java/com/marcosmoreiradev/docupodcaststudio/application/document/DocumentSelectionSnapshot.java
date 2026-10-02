package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;

/** Presentation-neutral resolved selection for all shared study panels. */
public record DocumentSelectionSnapshot(
        DocumentSelectionRef reference,
        String text,
        String provenance,
        String type,
        int pageNumber,
        PdfPageRegion geometry,
        long revision,
        PdfNarratability narratability
) {
    public DocumentSelectionSnapshot {
        text = text == null ? "" : text;
        provenance = provenance == null ? "" : provenance.strip();
        type = type == null ? "" : type.strip();
        pageNumber = Math.max(0, pageNumber);
        revision = Math.max(0, revision);
    }

    public boolean pdf() {
        return reference instanceof PdfRegionSelectionRef;
    }
}
