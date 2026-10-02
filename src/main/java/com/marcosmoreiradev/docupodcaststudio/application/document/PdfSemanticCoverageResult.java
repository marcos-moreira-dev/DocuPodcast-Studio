package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Coverage decision based on native evidence and conservative type checks. */
public record PdfSemanticCoverageResult(
        PdfSemanticCoverageStatus status,
        boolean reliableNativeText,
        double coveredTokenRatio,
        List<String> reasons,
        List<String> missingEvidence
) {
    public PdfSemanticCoverageResult {
        status = status == null
                ? PdfSemanticCoverageStatus.REJECTED : status;
        coveredTokenRatio = Double.isFinite(coveredTokenRatio)
                ? Math.max(0.0, Math.min(1.0, coveredTokenRatio)) : 0.0;
        reasons = reasons == null ? List.of() : reasons.stream()
                .filter(java.util.Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
        missingEvidence = missingEvidence == null ? List.of()
                : missingEvidence.stream().filter(java.util.Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
    }

    public boolean accepted() {
        return status == PdfSemanticCoverageStatus.ACCEPTED;
    }
}
