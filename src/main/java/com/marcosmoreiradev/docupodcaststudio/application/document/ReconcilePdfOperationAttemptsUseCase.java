package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttempt;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttemptState;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/** Converts attempts abandoned by a previous application process to INTERRUPTED. */
public final class ReconcilePdfOperationAttemptsUseCase {
    private final PdfOperationAttemptRepository repository;

    public ReconcilePdfOperationAttemptsUseCase(PdfOperationAttemptRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public int reconcile(Path projectRoot) throws IOException {
        if (projectRoot == null) return 0;
        int recovered = 0;
        for (PdfOperationAttempt attempt : repository.list(projectRoot)) {
            if (attempt.state() != PdfOperationAttemptState.IN_FLIGHT) continue;
            String diagnostic = attempt.diagnostic()
                    + (attempt.diagnostic().isBlank() ? "" : "\n")
                    + "recovery=PROCESS_INTERRUPTED\nreconciledAt=" + Instant.now();
            repository.save(projectRoot, new PdfOperationAttempt(
                    attempt.schemaVersion(), attempt.id(), attempt.operationKey(),
                    attempt.pageNumber(), attempt.operation(), attempt.sourceRegionIds(),
                    attempt.evidenceFingerprint(), attempt.parameterFingerprint(),
                    PdfOperationAttemptState.INTERRUPTED, attempt.startedAt(),
                    Instant.now(), attempt.metrics(), attempt.treatmentId(), diagnostic));
            recovered++;
        }
        return recovered;
    }
}
