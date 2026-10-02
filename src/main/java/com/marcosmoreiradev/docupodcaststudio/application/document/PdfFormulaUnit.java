package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfFormulaKind;

import java.util.List;
import java.util.Objects;

/** A complete formula assembled from one or more extraction/OCR fragments. */
public record PdfFormulaUnit(
        String id,
        PdfFormulaKind kind,
        List<String> sourceRegionIds,
        PdfPageRegion geometry,
        double confidence
) {
    public PdfFormulaUnit {
        id = Objects.toString(id, "").strip();
        if (id.isBlank()) throw new IllegalArgumentException("formula id");
        kind = Objects.requireNonNull(kind, "kind");
        sourceRegionIds = sourceRegionIds == null ? List.of()
                : sourceRegionIds.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (sourceRegionIds.isEmpty()) {
            throw new IllegalArgumentException("formula source regions");
        }
        geometry = Objects.requireNonNull(geometry, "geometry");
        confidence = Double.isFinite(confidence)
                ? Math.max(0, Math.min(1, confidence)) : 0;
    }
}
