package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

/**
 * Decides whether an uncertain source region still needs user-facing review.
 *
 * <p>An approved local review resolves the gate even when its conservative
 * result remains {@link PdfNarratability#UNCERTAIN}: in that case the source is
 * preserved and excluded from narration. An approved descriptive derivative
 * resolves it by providing narratable replacement text.</p>
 */
public final class PdfRegionReviewStatus {
    private PdfRegionReviewStatus() {
    }

    public static boolean requiresReview(PreparedPdfPage page, PdfRegion region) {
        if (region.effectiveNarratability() != PdfNarratability.UNCERTAIN) {
            return false;
        }
        return page.derivedTreatments().stream()
                .filter(treatment -> treatment.state()
                        == com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfDerivedTreatmentState.APPROVED)
                .filter(treatment -> treatment.sourceRegionIds().contains(region.id()))
                .filter(treatment -> currentFor(treatment, region))
                .noneMatch(PdfRegionReviewStatus::resolvesReview);
    }

    private static boolean currentFor(PdfDerivedTreatment treatment,
                                      PdfRegion region) {
        if (treatment.kind() == PdfDerivedTreatmentKind.NARRATABILITY_REVIEW) {
            // Applying the approved decision creates exactly one new region
            // revision. The review itself remains the evidence for that change.
            return treatment.sourceRevision() == region.revision()
                    || treatment.sourceRevision() + 1 == region.revision();
        }
        return treatment.sourceRevision() == region.revision();
    }

    private static boolean resolvesReview(PdfDerivedTreatment treatment) {
        return treatment.kind() == PdfDerivedTreatmentKind.NARRATABILITY_REVIEW
                || treatment.kind() == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION
                || treatment.kind() == PdfDerivedTreatmentKind.MATHEMATICAL_READING
                || treatment.kind() == PdfDerivedTreatmentKind.TABLE_NARRATION
                || treatment.kind() == PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION;
    }
}
