package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.Objects;

/** Versioned extraction evidence retained independently from product decisions. */
public record PdfRegionEvidence(
        PdfRegionOrigin origin,
        double confidence,
        String extractorVersion,
        String parserVersion,
        String groupingVersion,
        String classifierVersion
) {
    public PdfRegionEvidence {
        origin = Objects.requireNonNullElse(origin, PdfRegionOrigin.OCR_LOCAL);
        confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
        extractorVersion = normalize(extractorVersion, "unknown");
        parserVersion = normalize(parserVersion, "unknown");
        groupingVersion = normalize(groupingVersion, "unknown");
        classifierVersion = normalize(classifierVersion, "unknown");
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
