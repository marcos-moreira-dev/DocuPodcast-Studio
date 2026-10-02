package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

import java.util.List;

/** Read-only preview required before an explicit page/document review action. */
public record PdfTreatmentReviewBatch(List<Item> items) {
    public PdfTreatmentReviewBatch {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(String treatmentId, int pageNumber,
                       PdfDerivedTreatmentKind kind, String preview) { }
}
