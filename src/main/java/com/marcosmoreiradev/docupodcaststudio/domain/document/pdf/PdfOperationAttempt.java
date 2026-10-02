package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable audit sidecar for a page/object operation. */
public record PdfOperationAttempt(
        int schemaVersion,
        String id,
        String operationKey,
        int pageNumber,
        String operation,
        List<String> sourceRegionIds,
        String evidenceFingerprint,
        String parameterFingerprint,
        PdfOperationAttemptState state,
        Instant startedAt,
        Instant finishedAt,
        PdfOperationMetrics metrics,
        String treatmentId,
        String diagnostic
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PdfOperationAttempt {
        if (schemaVersion <= 0) schemaVersion = CURRENT_SCHEMA_VERSION;
        id = required(id, "id");
        operationKey = required(operationKey, "operationKey");
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber");
        operation = required(operation, "operation");
        sourceRegionIds = sourceRegionIds == null ? List.of()
                : sourceRegionIds.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        evidenceFingerprint = required(
                evidenceFingerprint, "evidenceFingerprint");
        parameterFingerprint = required(
                parameterFingerprint, "parameterFingerprint");
        state = Objects.requireNonNull(state, "state");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        finishedAt = Objects.requireNonNullElse(finishedAt, startedAt);
        metrics = Objects.requireNonNullElseGet(metrics,
                () -> PdfOperationMetrics.empty(""));
        treatmentId = Objects.toString(treatmentId, "").strip();
        diagnostic = Objects.toString(diagnostic, "").strip();
    }

    private static String required(String value, String field) {
        String safe = Objects.toString(value, "").strip();
        if (safe.isBlank()) throw new IllegalArgumentException(field);
        return safe;
    }
}
