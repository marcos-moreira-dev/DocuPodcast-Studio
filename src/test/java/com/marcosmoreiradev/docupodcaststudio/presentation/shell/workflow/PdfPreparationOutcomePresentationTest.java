package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfPreparationOutcomePresentationTest {
    @Test
    void terminalCopyDistinguishesAcceptedRejectedFailedAndCancelled() {
        assertAll(
                () -> assertEquals("Página 1 preparada.", accepted(1).terminalMessage()),
                () -> assertEquals("Página 2 no pudo interpretarse con suficiente fiabilidad.",
                        PreparePdfPageResult.rejected(2, null, List.of(), "", null)
                                .terminalMessage()),
                () -> assertEquals("No se pudo procesar la página 3.",
                        PreparePdfPageResult.technicalFailure(3, null, List.of(),
                                PdfOperationAttemptState.TRUNCATED, "TRUNCATED", "", null)
                                .terminalMessage()),
                () -> assertEquals("Preparación de la página 4 cancelada.",
                        PreparePdfPageResult.cancelled(4, null).terminalMessage()));
    }

    @Test
    void partialDialogIsNotAStackTraceOrSyntheticIllegalStateException() {
        PdfPreparationScopeResult result = result(List.of(
                accepted(1),
                PreparePdfPageResult.rejected(2, null, List.of(),
                        "No pudo interpretarse con suficiente fiabilidad.", null),
                accepted(3)));
        UserNotification notification =
                PdfNarratablePreparationCoordinator.scopeResultNotification(result);
        assertAll(
                () -> assertEquals("Preparación completada con incidencias",
                        notification.headline()),
                () -> assertTrue(notification.message().contains("Preparadas: 2")),
                () -> assertTrue(notification.message().contains("No interpretables: 1")),
                () -> assertTrue(notification.message().contains("Página 2")),
                () -> assertFalse(notification.technicalDetail()
                        .contains("IllegalStateException")));
    }

    @Test
    void technicalDialogKeepsOriginalCauseOnlyInTechnicalDetails() {
        IllegalArgumentException root = new IllegalArgumentException("root-cause");
        PdfPreparationScopeResult result = result(List.of(
                PreparePdfPageResult.technicalFailure(2, null, List.of(),
                        PdfOperationAttemptState.FAILED, "BACKEND", "Motor no disponible.", root)));
        UserNotification notification =
                PdfNarratablePreparationCoordinator.scopeResultNotification(result);
        assertAll(
                () -> assertFalse(notification.message().contains("root-cause")),
                () -> assertTrue(notification.technicalDetail().contains("root-cause")),
                () -> assertTrue(notification.technicalDetail().contains("BACKEND")));
    }

    private static PdfPreparationScopeResult result(
            List<PreparePdfPageResult> outcomes) {
        List<Integer> accepted = outcomes.stream()
                .filter(PreparePdfPageResult::succeeded)
                .map(PreparePdfPageResult::pageNumber).toList();
        List<Integer> failed = outcomes.stream()
                .filter(value -> !value.succeeded() && !value.cancelled())
                .map(PreparePdfPageResult::pageNumber).toList();
        return new PdfPreparationScopeResult(null, accepted, failed,
                List.of(), false, outcomes);
    }

    private static PreparePdfPageResult accepted(int page) {
        PreparedPdfPage prepared = new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, page, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(), List.of(),
                PdfPagePreparationMetrics.empty(), PdfPageAnalysisProfile.defaults(),
                List.of(), "");
        return new PreparePdfPageResult(page, prepared, true, false, List.of());
    }
}
