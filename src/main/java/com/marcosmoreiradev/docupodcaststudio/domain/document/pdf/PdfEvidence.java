package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Provenance retained by a PageMap node without replacing V3 evidence. */
public record PdfEvidence(String origin, double confidence, String extractorSignature) {
    public PdfEvidence {
        origin = origin == null || origin.isBlank() ? "UNKNOWN" : origin.strip();
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        extractorSignature = extractorSignature == null ? "" : extractorSignature.strip();
    }
}
