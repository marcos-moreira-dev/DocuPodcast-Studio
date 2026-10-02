package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.PdfListeningMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfOperationAttemptRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class PdfProductizationUseCasesTest {
    @TempDir Path root;

    @Test
    void understandableModesChangeRealPoliciesWithoutAutoApproval() {
        assertEquals(TableNarrationPolicy.SKIP,
                PdfListeningMode.ESSENTIAL_READING.tablePolicy());
        assertFalse(PdfListeningMode.ESSENTIAL_READING.automaticDrafts());
        assertEquals(TableNarrationPolicy.SUMMARIZE,
                PdfListeningMode.INTELLIGENT_DOCUMENTARY.tablePolicy());
        assertTrue(PdfListeningMode.INTELLIGENT_DOCUMENTARY.automaticDrafts());
        assertFalse(PdfListeningMode.ADVANCED_REVIEW.automaticDrafts());
    }

    @Test
    void batchReviewRequiresPreviewAndExplicitIds() throws Exception {
        JsonPreparedPdfDocumentRepository repository = workspace();
        ReviewPdfTreatmentBatchUseCase review =
                new ReviewPdfTreatmentBatchUseCase(repository);

        PdfTreatmentReviewBatch preview = review.preview(root, 1);

        assertEquals(2, preview.items().size());
        assertTrue(repository.loadPage(root, 1).orElseThrow()
                .derivedTreatments().stream().allMatch(value ->
                        value.state() == PdfDerivedTreatmentState.DRAFT));
        int changed = review.apply(root,
                List.of(preview.items().getFirst().treatmentId()),
                PdfDerivedTreatmentState.APPROVED);
        assertEquals(1, changed);
        PreparedPdfPage page = repository.loadPage(root, 1).orElseThrow();
        assertEquals(1, page.derivedTreatments().stream()
                .filter(PdfDerivedTreatment::approved).count());
        assertEquals(1, page.derivedTreatments().stream()
                .filter(value -> value.state() == PdfDerivedTreatmentState.DRAFT)
                .count());
    }

    @Test
    void dashboardExplainsOmittedPendingDraftAndFailedItems() throws Exception {
        JsonPreparedPdfDocumentRepository repository = workspace();
        JsonPdfOperationAttemptRepository attempts =
                new JsonPdfOperationAttemptRepository();
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        attempts.save(root, new PdfOperationAttempt(
                1, "PDF-ATTEMPT-a9", "op", 1, "IMAGE_DESCRIPTION",
                List.of("IMG-1"), "evidence", "parameters",
                PdfOperationAttemptState.FAILED, now, now,
                PdfOperationMetrics.empty("engine"), "", "motor ausente"));

        PdfReviewDashboard dashboard = new BuildPdfReviewDashboardUseCase(
                repository, attempts).build(root);

        assertEquals(1, dashboard.narratableRegions());
        assertEquals(2, dashboard.omittedRegions());
        assertEquals(1, dashboard.pendingRegions());
        assertEquals(2, dashboard.draftTreatments());
        assertEquals(1, dashboard.failedAttempts());
        assertTrue(dashboard.summary().contains("omitido: 2"));
        assertTrue(dashboard.summary().contains("fallos: 1"));
    }

    private JsonPreparedPdfDocumentRepository workspace() throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "PDF", "a.pdf", "a".repeat(64), 1, "test", now, now));
        PdfRegion prose = region("P-1", PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE);
        PdfRegion image = region("IMG-1", PdfRegionType.IMAGE,
                PdfNarratability.NON_NARRATABLE);
        PdfRegion uncertain = region("U-1", PdfRegionType.UNKNOWN,
                PdfNarratability.UNCERTAIN);
        PdfRegion omitted = region("O-1", PdfRegionType.TABLE,
                PdfNarratability.NON_NARRATABLE);
        List<PdfDerivedTreatment> drafts = List.of(
                treatment("DER-1", "IMG-1", PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, now),
                treatment("DER-2", "P-1", PdfDerivedTreatmentKind.NARRATABILITY_REVIEW, now));
        repository.savePage(root, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY_WITH_WARNINGS, 1,
                List.of(prose, image, uncertain, omitted), drafts,
                List.of(), ""));
        return repository;
    }

    private static PdfRegion region(String id, PdfRegionType type,
                                    PdfNarratability narratability) {
        return new PdfRegion(id, 1, 10, 10, 100, 40, 0, 0, id,
                type, narratability, List.of(), new PdfRegionEvidence(
                PdfRegionOrigin.OCR_LOCAL, 0.7, "ocr", "parser", "group", "class"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }

    private static PdfDerivedTreatment treatment(
            String id, String regionId, PdfDerivedTreatmentKind kind,
            Instant now) {
        return new PdfDerivedTreatment(id, kind, List.of(regionId),
                "Borrador " + id, "local", "1", 0.8, now,
                PdfDerivedTreatmentState.DRAFT, 1, "fingerprint-" + id,
                "prompt", Map.of());
    }
}
