package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationPriority;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisibleTextPreparationCoordinatorTest {
    @Test
    void visibleRetryRequestsSemanticPreparationInsteadOfForcingOcr() {
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                Path.of("project"), Path.of("source.pdf"), "a".repeat(64));

        var request = PdfVisibleTextPreparationCoordinator
                .explicitSemanticRetryRequest(workspace, Path.of("cache"), 3,
                        PdfPreparationPriority.URGENT, "explicit-retry-test");

        assertFalse(request.forceOcr());
        assertEquals(PdfPreparationOrigin.EXPLICIT_RETRY, request.origin());
        assertTrue(request.retryAllowed());
        assertEquals(3, request.pageNumber());
    }
}
