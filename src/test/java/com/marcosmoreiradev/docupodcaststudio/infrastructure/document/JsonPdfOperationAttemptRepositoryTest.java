package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JsonPdfOperationAttemptRepositoryTest {
    @TempDir Path root;

    @Test
    void roundTripsEveryTerminalStateAndMetrics() throws Exception {
        JsonPdfOperationAttemptRepository repository =
                new JsonPdfOperationAttemptRepository();
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        int index = 0;
        for (PdfOperationAttemptState state : PdfOperationAttemptState.values()) {
            repository.save(root, new PdfOperationAttempt(
                    1, "PDF-ATTEMPT-" + Integer.toHexString(++index),
                    "operation-" + state, 2, "IMAGE_DESCRIPTION",
                    List.of("IMG-1"), "evidence", "parameters", state,
                    now, now.plusSeconds(index),
                    new PdfOperationMetrics(50, 8, 20, 10, 400,
                            1024, 2048, 7, 800, 600,
                            "engine", "model", "stop"),
                    state == PdfOperationAttemptState.COMPLETED
                            ? "DER-1" : "", "diagnostic-" + state));
        }

        List<PdfOperationAttempt> loaded = repository.list(root);

        assertEquals(List.of(PdfOperationAttemptState.IN_FLIGHT,
                        PdfOperationAttemptState.INTERRUPTED,
                        PdfOperationAttemptState.COMPLETED,
                        PdfOperationAttemptState.FAILED,
                        PdfOperationAttemptState.CANCELLED,
                        PdfOperationAttemptState.TRUNCATED,
                        PdfOperationAttemptState.INSUFFICIENT_EVIDENCE),
                loaded.stream().map(PdfOperationAttempt::state).toList());
        assertEquals(2048, loaded.getFirst().metrics().vramBytes());
        assertEquals("diagnostic-FAILED", loaded.get(3).diagnostic());
    }
}
