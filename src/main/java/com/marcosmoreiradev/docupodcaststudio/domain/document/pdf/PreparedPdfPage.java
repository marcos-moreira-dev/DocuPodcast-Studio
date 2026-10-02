package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Atomic unit of prepared PDF persistence. */
public record PreparedPdfPage(
        int schemaVersion,
        int pageNumber,
        double widthPoints,
        double heightPoints,
        PdfPagePreparationStatus status,
        long revision,
        List<PdfRegion> regions,
        List<PdfDerivedTreatment> derivedTreatments,
        PdfPagePreparationMetrics preparationMetrics,
        PdfPageAnalysisProfile analysisProfile,
        List<String> warnings,
        String lastAttemptError
) {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public PreparedPdfPage {
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        if (!(widthPoints > 0.0) || !(heightPoints > 0.0)
                || !Double.isFinite(widthPoints) || !Double.isFinite(heightPoints)) {
            throw new IllegalArgumentException("PDF page dimensions must be positive");
        }
        status = Objects.requireNonNullElse(status, PdfPagePreparationStatus.READY_WITH_WARNINGS);
        revision = Math.max(1L, revision);
        regions = regions == null ? List.of() : regions.stream()
                .sorted(Comparator.comparingInt(PdfRegion::effectiveReadingOrder)
                        .thenComparingDouble(PdfRegion::yMin)
                        .thenComparingDouble(PdfRegion::xMin))
                .toList();
        if (regions.stream().anyMatch(region -> region.pageNumber() != pageNumber)) {
            throw new IllegalArgumentException("All regions must belong to the prepared page");
        }
        java.util.Map<String, PdfRegion> regionsById = regions.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        PdfRegion::id, java.util.function.Function.identity()));
        java.util.HashSet<Integer> leafOrders = new java.util.HashSet<>();
        for (PdfRegion region : regions) {
            if (region.container()) {
                if (region.playbackTarget()
                        || region.effectiveNarratability() == PdfNarratability.NARRATABLE) {
                    throw new IllegalArgumentException(
                            "PDF containers cannot be narratable playback targets");
                }
            } else if (region.playbackTarget()
                    && (region.attributes().containsKey("regionRole")
                    || region.attributes().containsKey("playbackTarget"))
                    && !leafOrders.add(region.effectiveReadingOrder())) {
                throw new IllegalArgumentException(
                        "Playback leaf readingOrder must be unique per page");
            }
            if (!region.parentId().isBlank()) {
                PdfRegion parent = regionsById.get(region.parentId());
                if (parent == null || !parent.container()) {
                    throw new IllegalArgumentException(
                            "PDF child must reference a container on the same page");
                }
                if (!contains(parent, region)) {
                    throw new IllegalArgumentException(
                            "PDF child geometry must be contained by its parent");
                }
            }
        }
        derivedTreatments = derivedTreatments == null ? List.of() : List.copyOf(derivedTreatments);
        preparationMetrics = Objects.requireNonNullElse(
                preparationMetrics, PdfPagePreparationMetrics.empty());
        analysisProfile = Objects.requireNonNullElseGet(
                analysisProfile, PdfPageAnalysisProfile::defaults);
        java.util.Set<String> regionIds = regions.stream().map(PdfRegion::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (derivedTreatments.stream().flatMap(value -> value.sourceRegionIds().stream())
                .anyMatch(id -> !regionIds.contains(id))) {
            throw new IllegalArgumentException("Derived treatments must reference regions on the same page");
        }
        warnings = warnings == null ? List.of() : warnings.stream()
                .filter(Objects::nonNull).map(String::strip).filter(value -> !value.isBlank()).distinct().toList();
        lastAttemptError = lastAttemptError == null ? "" : lastAttemptError.strip();
    }

    private static boolean contains(PdfRegion parent, PdfRegion child) {
        double epsilon = 0.75;
        return child.xMin() >= parent.xMin() - epsilon
                && child.yMin() >= parent.yMin() - epsilon
                && child.xMax() <= parent.xMax() + epsilon
                && child.yMax() <= parent.yMax() + epsilon;
    }

    public PreparedPdfPage(int schemaVersion,
                           int pageNumber,
                           double widthPoints,
                           double heightPoints,
                           PdfPagePreparationStatus status,
                           long revision,
                           List<PdfRegion> regions,
                           List<PdfDerivedTreatment> derivedTreatments,
                           List<String> warnings,
                           String lastAttemptError) {
        this(schemaVersion, pageNumber, widthPoints, heightPoints, status, revision,
                regions, derivedTreatments, PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), warnings, lastAttemptError);
    }

    public PreparedPdfPage(int schemaVersion,
                           int pageNumber,
                           double widthPoints,
                           double heightPoints,
                           PdfPagePreparationStatus status,
                           long revision,
                           List<PdfRegion> regions,
                           List<String> warnings,
                           String lastAttemptError) {
        this(schemaVersion, pageNumber, widthPoints, heightPoints, status, revision,
                regions, List.of(), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), warnings, lastAttemptError);
    }

    public PreparedPdfPage(int schemaVersion,
                           int pageNumber,
                           double widthPoints,
                           double heightPoints,
                           PdfPagePreparationStatus status,
                           long revision,
                           List<PdfRegion> regions,
                           List<PdfDerivedTreatment> derivedTreatments,
                           PdfPagePreparationMetrics preparationMetrics,
                           List<String> warnings,
                           String lastAttemptError) {
        this(schemaVersion, pageNumber, widthPoints, heightPoints, status, revision, regions,
                derivedTreatments, preparationMetrics, PdfPageAnalysisProfile.defaults(),
                warnings, lastAttemptError);
    }
}
