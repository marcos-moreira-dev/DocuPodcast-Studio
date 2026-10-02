package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;

import java.util.List;
import java.util.Objects;

/** Explainable automatic narratability decision; it never removes source text. */
public record PdfNarratabilityDecision(PdfNarratability narratability, List<String> reasons) {
    public PdfNarratabilityDecision {
        narratability = Objects.requireNonNullElse(narratability, PdfNarratability.UNCERTAIN);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
