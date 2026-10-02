package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttemptState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PdfManualRetryReasonTest {
    @Test
    void distinguishesTechnicalAndSemanticManualRetries() {
        assertEquals("MANUAL_RETRY_AFTER_TECHNICAL_FAILURE",
                AnalyzePdfPageSemanticallyUseCase.manualRetryReason(
                        PdfOperationAttemptState.TRUNCATED));
        assertEquals("MANUAL_RETRY_AFTER_TECHNICAL_FAILURE",
                AnalyzePdfPageSemanticallyUseCase.manualRetryReason(
                        PdfOperationAttemptState.FAILED));
        assertEquals("MANUAL_RETRY_AFTER_SEMANTIC_REJECTION",
                AnalyzePdfPageSemanticallyUseCase.manualRetryReason(
                        PdfOperationAttemptState.INSUFFICIENT_EVIDENCE));
    }
}
