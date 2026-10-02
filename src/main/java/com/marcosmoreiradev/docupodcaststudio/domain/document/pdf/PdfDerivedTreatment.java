package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Regenerable interpretation associated with source regions; never replaces extracted content. */
public record PdfDerivedTreatment(
        String id,
        PdfDerivedTreatmentKind kind,
        List<String> sourceRegionIds,
        String derivedText,
        String modelId,
        String modelVersion,
        double confidence,
        Instant createdAt,
        PdfDerivedTreatmentState state,
        long sourceRevision,
        String sourceFingerprint,
        String prompt,
        Map<String, String> metadata
) {
    public PdfDerivedTreatment {
        id = token(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        sourceRegionIds = sourceRegionIds == null ? List.of() : sourceRegionIds.stream()
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (sourceRegionIds.isEmpty()) {
            throw new IllegalArgumentException("A derived treatment requires source regions");
        }
        derivedText = derivedText == null ? "" : derivedText.strip();
        modelId = token(modelId, "modelId");
        modelVersion = token(modelVersion, "modelVersion");
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        createdAt = Objects.requireNonNullElse(createdAt, Instant.EPOCH);
        state = Objects.requireNonNullElse(state, PdfDerivedTreatmentState.DRAFT);
        sourceRevision = Math.max(1L, sourceRevision);
        sourceFingerprint = token(sourceFingerprint, "sourceFingerprint");
        prompt = prompt == null ? "" : prompt.strip();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Compatibility constructor for deterministic treatments created before review states existed. */
    public PdfDerivedTreatment(String id,
                               PdfDerivedTreatmentKind kind,
                               List<String> sourceRegionIds,
                               String derivedText,
                               String modelId,
                               String modelVersion,
                               double confidence,
                               Instant createdAt,
                               Map<String, String> metadata) {
        this(id, kind, sourceRegionIds, derivedText, modelId, modelVersion, confidence, createdAt,
                PdfDerivedTreatmentState.APPROVED, 1L, "legacy-source", "", metadata);
    }

    public boolean approved() {
        return state == PdfDerivedTreatmentState.APPROVED;
    }

    /** @deprecated Approval and source freshness are separate decisions. */
    @Deprecated(forRemoval = false)
    public boolean approvedAndCurrent() {
        return approved();
    }

    public PdfDerivedTreatment withState(PdfDerivedTreatmentState nextState) {
        return new PdfDerivedTreatment(id, kind, sourceRegionIds, derivedText, modelId, modelVersion,
                confidence, createdAt, nextState, sourceRevision, sourceFingerprint, prompt, metadata);
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
