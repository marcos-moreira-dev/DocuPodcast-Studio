package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

import java.util.Objects;

/** Discoverable local capability; disabled by default and prepared from Settings only. */
public record PdfDerivedTreatmentDescriptor(
        PdfDerivedTreatmentKind kind,
        String capabilityId,
        String displayName,
        boolean enabledByDefault,
        boolean localOnly
) {
    public PdfDerivedTreatmentDescriptor {
        kind = Objects.requireNonNull(kind, "kind");
        capabilityId = required(capabilityId, "capabilityId");
        displayName = required(displayName, "displayName");
        localOnly = true;
    }

    private static String required(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
