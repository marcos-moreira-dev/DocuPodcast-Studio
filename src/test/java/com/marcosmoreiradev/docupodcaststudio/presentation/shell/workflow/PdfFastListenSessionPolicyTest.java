package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfFastListenSessionPolicyTest {
    @Test
    void technicalFailureRequiresAnExplicitRetryInTheSameSession() {
        String message = PdfNarratablePreparationCoordinator
                .fastListenSkipMessage(2, true, false);

        assertTrue(message.contains("fallo técnico en esta sesión"));
        assertTrue(message.contains("Procesar o Reintentar"));
    }

    @Test
    void insufficientEvidenceRemainsDistinctFromTechnicalFailure() {
        String message = PdfNarratablePreparationCoordinator
                .fastListenSkipMessage(3, false, true);

        assertTrue(message.contains("evidencia suficiente"));
        assertTrue(message.contains("reintentar manualmente"));
    }
}
