package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves a deterministic visual reading order without depending on OCR
 * completion order.
 *
 * <p>Wide regions delimit vertical bands. Inside each band, true document
 * columns are read top-to-bottom before moving to the next column. Sparse
 * labels from diagrams are not allowed to manufacture fictitious columns.</p>
 */
public final class PdfReadingOrderResolver {
    private static final double WIDE_REGION_FACTOR = 0.70;
    private static final double COLUMN_REGION_MIN_WIDTH_FACTOR = 0.24;
    private static final double COLUMN_CLUSTER_TOLERANCE_FACTOR = 0.10;

    public List<PdfRegion> resolve(PreparedPdfPage page) {
        return resolve(page, page == null ? List.of() : page.regions());
    }

    public List<PdfRegion> resolve(PreparedPdfPage page,
                                   List<PdfRegion> source) {
        if (page == null || source == null || source.isEmpty()) {
            return List.of();
        }
        List<PdfRegion> regions = source.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
        List<PdfRegion> geometryOrder =
                resolveGeometry(page, regions);
        if (regions.stream().noneMatch(
                region -> region.override().readingOrder() != null)) {
            return geometryOrder;
        }
        java.util.HashMap<String, Integer> geometryRanks =
                new java.util.HashMap<>();
        for (int index = 0; index < geometryOrder.size(); index++) {
            geometryRanks.put(geometryOrder.get(index).id(), index);
        }
        return regions.stream().sorted(Comparator
                .comparingInt((PdfRegion region) ->
                        region.override().readingOrder() == null
                                ? geometryRanks.getOrDefault(
                                        region.id(), Integer.MAX_VALUE)
                                : region.override().readingOrder())
                .thenComparingInt(region ->
                        region.override().readingOrder() == null ? 1 : 0)
                .thenComparingDouble(PdfRegion::yMin)
                .thenComparingDouble(PdfRegion::xMin)
                .thenComparing(PdfRegion::id)).toList();
    }

    private static List<PdfRegion> resolveGeometry(
            PreparedPdfPage page, List<PdfRegion> regions) {
        double pageWidth = page.widthPoints();
        List<PdfRegion> wide = regions.stream()
                .filter(region -> width(region)
                        >= pageWidth * WIDE_REGION_FACTOR)
                .sorted(visualComparator())
                .toList();
        List<PdfRegion> narrow = regions.stream()
                .filter(region -> width(region)
                        < pageWidth * WIDE_REGION_FACTOR)
                .toList();
        if (wide.isEmpty()) {
            return orderBand(narrow, pageWidth);
        }

        ArrayList<PdfRegion> result = new ArrayList<>(regions.size());
        double lowerCenter = Double.NEGATIVE_INFINITY;
        for (PdfRegion anchor : wide) {
            double anchorCenter = centerY(anchor);
            final double lower = lowerCenter;
            result.addAll(orderBand(narrow.stream()
                    .filter(region -> centerY(region) >= lower
                            && centerY(region) < anchorCenter)
                    .toList(), pageWidth));
            result.add(anchor);
            lowerCenter = anchorCenter;
        }
        final double lower = lowerCenter;
        result.addAll(orderBand(narrow.stream()
                .filter(region -> centerY(region) >= lower)
                .toList(), pageWidth));
        return List.copyOf(result);
    }

    private static List<PdfRegion> orderBand(List<PdfRegion> regions,
                                             double pageWidth) {
        if (regions.size() < 2) {
            return regions.stream().sorted(visualComparator()).toList();
        }
        List<Double> columnStarts = columnStarts(regions, pageWidth);
        if (columnStarts.size() < 2) {
            return regions.stream().sorted(visualComparator()).toList();
        }
        return regions.stream().sorted(Comparator
                .comparingInt((PdfRegion region) ->
                        nearestCluster(region.xMin(), columnStarts))
                .thenComparingDouble(PdfRegion::yMin)
                .thenComparingDouble(PdfRegion::xMin)
                .thenComparing(PdfRegion::id)).toList();
    }

    private static List<Double> columnStarts(List<PdfRegion> regions,
                                             double pageWidth) {
        List<PdfRegion> candidates = regions.stream()
                .filter(region -> width(region)
                        >= pageWidth * COLUMN_REGION_MIN_WIDTH_FACTOR)
                .filter(region -> region.effectiveText().length() >= 20)
                .filter(region -> region.effectiveType()
                        != PdfRegionType.PAGE_NUMBER)
                .filter(region -> region.effectiveType()
                        != PdfRegionType.HEADER)
                .filter(region -> region.effectiveType()
                        != PdfRegionType.FOOTER)
                .toList();
        if (candidates.size() < 2) return List.of();
        List<Double> clusters = cluster(candidates.stream()
                .map(PdfRegion::xMin).sorted().toList(),
                pageWidth * COLUMN_CLUSTER_TOLERANCE_FACTOR);
        if (clusters.size() < 2) return List.of();

        // A column must contain at least one substantial region and adjacent
        // column candidates must be visually separated.
        for (int index = 0; index < clusters.size() - 1; index++) {
            final int columnIndex = index;
            double left = clusters.get(index);
            double right = clusters.get(index + 1);
            double leftMax = candidates.stream()
                    .filter(region -> nearestCluster(region.xMin(), clusters)
                            == columnIndex)
                    .mapToDouble(PdfRegion::xMax).max()
                    .orElse(pageWidth);
            double rightMin = candidates.stream()
                    .filter(region -> nearestCluster(region.xMin(), clusters)
                            == columnIndex + 1)
                    .mapToDouble(PdfRegion::xMin).min()
                    .orElse(0);
            if (left >= right || leftMax > rightMin + pageWidth * 0.04) {
                return List.of();
            }
        }
        return clusters;
    }

    private static List<Double> cluster(List<Double> starts,
                                        double tolerance) {
        ArrayList<Double> result = new ArrayList<>();
        for (double start : starts) {
            int match = -1;
            for (int index = 0; index < result.size(); index++) {
                if (Math.abs(result.get(index) - start) <= tolerance) {
                    match = index;
                    break;
                }
            }
            if (match < 0) result.add(start);
            else result.set(match, (result.get(match) + start) / 2.0);
        }
        result.sort(Double::compareTo);
        return List.copyOf(result);
    }

    private static int nearestCluster(double x, List<Double> clusters) {
        int best = 0;
        for (int index = 1; index < clusters.size(); index++) {
            if (Math.abs(clusters.get(index) - x)
                    < Math.abs(clusters.get(best) - x)) {
                best = index;
            }
        }
        return best;
    }

    private static Comparator<PdfRegion> visualComparator() {
        return Comparator.comparingDouble(PdfRegion::yMin)
                .thenComparingDouble(PdfRegion::xMin)
                .thenComparing(PdfRegion::id);
    }

    private static double centerY(PdfRegion region) {
        return (region.yMin() + region.yMax()) / 2.0;
    }

    private static double width(PdfRegion region) {
        return region.xMax() - region.xMin();
    }
}
