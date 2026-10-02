package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfRegionReviewStatusTest {

    @Test
    void approvedCurrentDescriptionClosesTheUncertainReviewGate() {
        PdfRegion region = doubtfulRegion();
        PdfDerivedTreatment description = treatment(
                PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                PdfDerivedTreatmentState.APPROVED,
                region.revision());

        assertFalse(PdfRegionReviewStatus.requiresReview(
                page(region, description), region));
    }

    @Test
    void staleOrDraftDescriptionDoesNotCloseTheReviewGate() {
        PdfRegion region = doubtfulRegion();

        assertTrue(PdfRegionReviewStatus.requiresReview(
                page(region, treatment(
                        PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                        PdfDerivedTreatmentState.DRAFT,
                        region.revision())),
                region));
        assertTrue(PdfRegionReviewStatus.requiresReview(
                page(region, treatment(
                        PdfDerivedTreatmentKind.NARRATABILITY_REVIEW,
                        PdfDerivedTreatmentState.DRAFT,
                        region.revision())),
                region));
        assertTrue(PdfRegionReviewStatus.requiresReview(
                page(region, treatment(
                        PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                        PdfDerivedTreatmentState.APPROVED,
                        region.revision() - 1)),
                region));
    }

    @Test
    void approvedConservativeReviewAlsoClosesTheGate() {
        PdfRegion region = doubtfulRegion();
        PdfDerivedTreatment review = treatment(
                PdfDerivedTreatmentKind.NARRATABILITY_REVIEW,
                PdfDerivedTreatmentState.APPROVED,
                region.revision());

        assertFalse(PdfRegionReviewStatus.requiresReview(
                page(region, review), region));
    }

    private static PdfRegion doubtfulRegion() {
        return new PdfRegion(
                "R-1", 1, 10, 10, 200, 40, 0, 0,
                "x / ? |", PdfRegionType.UNKNOWN,
                PdfNarratability.UNCERTAIN, List.of("ambiguous"),
                new PdfRegionEvidence(
                        PdfRegionOrigin.OCR_LOCAL, 0.4,
                        "ocr", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 3);
    }

    private static PdfDerivedTreatment treatment(
            PdfDerivedTreatmentKind kind,
            PdfDerivedTreatmentState state,
            long sourceRevision) {
        return new PdfDerivedTreatment(
                "PDF-DER-1", kind, List.of("R-1"),
                "Descripción clara.", "local-test", "1",
                0.95, Instant.EPOCH, state, sourceRevision,
                "fingerprint", "prompt", Map.of());
    }

    private static PreparedPdfPage page(
            PdfRegion region,
            PdfDerivedTreatment treatment) {
        return new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY,
                1, List.of(region), List.of(treatment), List.of(), "");
    }
}
