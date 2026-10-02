package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.List;
import java.util.Objects;

/** One observable, page-normalized crop requested by the bounded recovery round. */
public record PdfSemanticRecoveryRoi(
        PdfSemanticRecoveryReason reason,
        PdfSemanticPageAnalysis.NormalizedBox box,
        PdfRegionType expectedType,
        List<String> missingEvidence
) {
    public PdfSemanticRecoveryRoi {
        reason = Objects.requireNonNullElse(reason, PdfSemanticRecoveryReason.UNKNOWN);
        box = Objects.requireNonNull(box, "box").clamped();
        if (!box.positive()) throw new IllegalArgumentException("recovery ROI must be positive");
        expectedType = Objects.requireNonNullElse(expectedType, PdfRegionType.UNKNOWN);
        missingEvidence = missingEvidence == null ? List.of() : missingEvidence.stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().limit(12).toList();
    }
}
