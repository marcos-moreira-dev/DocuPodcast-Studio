package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfOperationAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class ReconcilePdfOperationAttemptsUseCaseTest {
    @TempDir Path root;

    @Test void abandonedInFlightAttemptBecomesInterruptedWithoutLosingStage() throws Exception {
        var repository = new JsonPdfOperationAttemptRepository();
        Instant started = Instant.parse("2026-08-11T19:39:41Z");
        repository.save(root, new PdfOperationAttempt(1, "PDF-P3", "op-p3", 3,
                "PAGE_SEMANTIC_READING", List.of(), "sha", "params",
                PdfOperationAttemptState.IN_FLIGHT, started, started,
                PdfOperationMetrics.empty("qwen"), "",
                "stage=READINESS\nruntimePid=36408\nruntimeGeneration=7"));

        assertEquals(1, new ReconcilePdfOperationAttemptsUseCase(repository).reconcile(root));
        PdfOperationAttempt recovered = repository.list(root).getFirst();
        assertEquals(PdfOperationAttemptState.INTERRUPTED, recovered.state());
        assertEquals(started, recovered.startedAt());
        assertTrue(recovered.diagnostic().contains("stage=READINESS"));
        assertTrue(recovered.diagnostic().contains("PROCESS_INTERRUPTED"));
    }
}
