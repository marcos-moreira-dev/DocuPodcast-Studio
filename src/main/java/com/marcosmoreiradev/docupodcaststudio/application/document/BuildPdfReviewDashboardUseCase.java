package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Aggregates review state without changing pages, treatments or attempts. */
public final class BuildPdfReviewDashboardUseCase {
    private final PreparedPdfDocumentRepository pages;
    private final PdfOperationAttemptRepository attempts;

    public BuildPdfReviewDashboardUseCase(
            PreparedPdfDocumentRepository pages,
            PdfOperationAttemptRepository attempts) {
        this.pages = Objects.requireNonNull(pages, "pages");
        this.attempts = Objects.requireNonNullElseGet(
                attempts, PdfOperationAttemptRepository::disabled);
    }

    public PdfReviewDashboard build(Path projectRoot) throws IOException {
        int narratable = 0;
        int omitted = 0;
        int pending = 0;
        int drafts = 0;
        for (PreparedPdfPage page : pages.loadPages(projectRoot)) {
            drafts += (int) page.derivedTreatments().stream()
                    .filter(value -> value.state() == PdfDerivedTreatmentState.DRAFT)
                    .count();
            for (PdfRegion region : page.regions()) {
                boolean approved = page.derivedTreatments().stream()
                        .filter(PdfDerivedTreatment::approvedAndCurrent)
                        .anyMatch(value -> value.sourceRegionIds().contains(region.id()));
                if (approved || region.effectiveNarratability()
                        == PdfNarratability.NARRATABLE) {
                    narratable++;
                } else if (region.effectiveNarratability()
                        == PdfNarratability.UNCERTAIN
                        || PdfRegionReviewStatus.requiresReview(page, region)) {
                    pending++;
                } else {
                    omitted++;
                }
            }
        }
        var journal = attempts.list(projectRoot);
        return new PdfReviewDashboard(narratable, omitted, pending, drafts,
                count(journal, PdfOperationAttemptState.FAILED),
                count(journal, PdfOperationAttemptState.TRUNCATED),
                count(journal, PdfOperationAttemptState.INSUFFICIENT_EVIDENCE));
    }

    private static int count(java.util.List<PdfOperationAttempt> attempts,
                             PdfOperationAttemptState state) {
        return (int) attempts.stream().filter(value -> value.state() == state).count();
    }
}
