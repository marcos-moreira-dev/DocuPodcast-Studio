package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Aggregate outcome for one requested preparation scope. */
public record PdfPreparationScopeResult(
        PdfPreparationScopeResolution resolution,
        List<Integer> preparedPages,
        List<Integer> failedPages,
        List<String> uncertainRegionIds,
        boolean cancelled,
        List<PreparePdfPageResult> pageOutcomes
) {
    public PdfPreparationScopeResult {
        preparedPages = preparedPages == null ? List.of() : List.copyOf(preparedPages);
        failedPages = failedPages == null ? List.of() : List.copyOf(failedPages);
        uncertainRegionIds = uncertainRegionIds == null ? List.of() : List.copyOf(uncertainRegionIds);
        pageOutcomes = pageOutcomes == null ? List.of() : List.copyOf(pageOutcomes);
    }

    public PdfPreparationScopeResult(PdfPreparationScopeResolution resolution,
                                     List<Integer> preparedPages,
                                     List<Integer> failedPages,
                                     List<String> uncertainRegionIds,
                                     boolean cancelled) {
        this(resolution, preparedPages, failedPages, uncertainRegionIds,
                cancelled, List.of());
    }

    public List<Integer> rejectedPages() {
        return pageOutcomes.stream().filter(PreparePdfPageResult::rejected)
                .map(PreparePdfPageResult::pageNumber).toList();
    }

    public List<Integer> technicalFailurePages() {
        return pageOutcomes.stream().filter(PreparePdfPageResult::technicalFailure)
                .map(PreparePdfPageResult::pageNumber).toList();
    }

    public List<Integer> cancelledPages() {
        return pageOutcomes.stream().filter(result -> result.cancelled()
                        || result.outcomeState()
                        == com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfOperationAttemptState.CANCELLED)
                .map(PreparePdfPageResult::pageNumber).toList();
    }

    public AggregateStatus aggregateStatus() {
        int accepted = preparedPages.size();
        int nonAccepted = rejectedPages().size() + technicalFailurePages().size()
                + cancelledPages().size();
        if (nonAccepted == 0) return AggregateStatus.TOTAL_SUCCESS;
        return accepted > 0 ? AggregateStatus.PARTIAL_SUCCESS
                : AggregateStatus.TOTAL_FAILURE;
    }

    public enum AggregateStatus {
        TOTAL_SUCCESS, PARTIAL_SUCCESS, TOTAL_FAILURE
    }
}
