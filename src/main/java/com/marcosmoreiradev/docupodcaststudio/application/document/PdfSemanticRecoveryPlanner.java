package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Localizes a few evidence-backed ROIs; it never tiles the complete page. */
public final class PdfSemanticRecoveryPlanner {
    private static final double ROI_MARGIN = 14.0;

    public PdfSemanticRecoveryPlan plan(PdfSemanticPageAnalysis candidate,
                                        PdfSemanticCoverageResult coverage,
                                        PdfTextLayer nativeLayer,
                                        PdfNativeTextQualityReport nativeQuality) {
        if (candidate == null || coverage == null) return new PdfSemanticRecoveryPlan(List.of());
        ArrayList<PdfSemanticRecoveryRoi> rois = new ArrayList<>();

        boolean reliable = nativeLayer != null && nativeQuality != null
                && nativeQuality.quality() == PdfNativeTextQuality.RELIABLE;
        List<PdfTextLine> missingLines = reliable
                ? locateMissingLines(nativeLayer, coverage.missingEvidence()) : List.of();

        if (coverage.reasons().contains("tableMissingVisibleCells")) {
            candidate.elements().stream()
                    .filter(element -> element.type() == PdfRegionType.TABLE)
                    .sorted(Comparator.comparingInt(PdfSemanticPageAnalysis.Element::readingOrder))
                    .limit(2)
                    .map(element -> new PdfSemanticRecoveryRoi(
                            PdfSemanticRecoveryReason.DENSE_TABLE_INCOMPLETE,
                            expanded(element.box()), PdfRegionType.TABLE,
                            evidenceInside(nativeLayer, element.box())))
                    .forEach(rois::add);
        }
        if (rois.isEmpty() && !missingLines.isEmpty()) {
            candidate.elements().stream()
                    .filter(element -> element.type() == PdfRegionType.TABLE)
                    .filter(element -> missingLines.stream().anyMatch(line ->
                            centerInside(line.region(), element.box())))
                    .limit(2)
                    .map(element -> new PdfSemanticRecoveryRoi(
                            PdfSemanticRecoveryReason.DENSE_TABLE_INCOMPLETE,
                            expanded(element.box()), PdfRegionType.TABLE,
                            evidenceInside(nativeLayer, element.box())))
                    .forEach(rois::add);
        }
        if (coverage.reasons().contains("imageMissingVisualExplanation")) {
            candidate.elements().stream()
                    .filter(element -> element.type() == PdfRegionType.IMAGE
                            && element.narrationText().isBlank())
                    .limit(1)
                    .map(element -> new PdfSemanticRecoveryRoi(
                            PdfSemanticRecoveryReason.VISUAL_REGION_INCOMPLETE,
                            expanded(element.box()), PdfRegionType.IMAGE, List.of()))
                    .forEach(rois::add);
        }

        // A partial TABLE bbox already localizes the same missing cell evidence;
        // do not multiply that diagnosis into extra line crops.
        if (reliable && rois.isEmpty() && !coverage.missingEvidence().isEmpty()) {
            for (List<PdfTextLine> group : spatialGroups(missingLines)) {
                if (rois.size() >= PdfSemanticRecoveryPlan.MAX_ROIS) break;
                PdfSemanticPageAnalysis.NormalizedBox box = normalizedUnion(group);
                PdfSemanticRecoveryReason reason = columnLike(box)
                        ? PdfSemanticRecoveryReason.MULTICOLUMN_GAP_OR_ORDER
                        : PdfSemanticRecoveryReason.TEXT_COVERAGE_GAP;
                rois.add(new PdfSemanticRecoveryRoi(reason, expanded(box),
                        PdfRegionType.PARAGRAPH,
                        group.stream().map(PdfTextLine::text).toList()));
            }
        }
        return new PdfSemanticRecoveryPlan(rois);
    }

    private static List<PdfTextLine> locateMissingLines(PdfTextLayer layer,
                                                         List<String> missing) {
        List<String> normalizedMissing = missing.stream()
                .map(PdfSemanticCoverageValidator::normalized).toList();
        return layer.lines().stream().filter(line -> {
            String value = PdfSemanticCoverageValidator.normalized(line.text());
            int valueTokens = PdfSemanticCoverageValidator.tokens(value).size();
            if (valueTokens < 3 && value.length() < 18) return false;
            return normalizedMissing.stream().anyMatch(expected ->
                    value.equals(expected)
                            || expected.length() >= 18 && value.contains(expected)
                            || value.length() >= 18 && expected.contains(value));
        }).sorted(Comparator.comparingDouble(line -> line.region().yMinPoints()))
                .toList();
    }

    private static List<List<PdfTextLine>> spatialGroups(List<PdfTextLine> lines) {
        ArrayList<List<PdfTextLine>> result = new ArrayList<>();
        ArrayList<PdfTextLine> current = new ArrayList<>();
        for (PdfTextLine line : lines) {
            if (!current.isEmpty()) {
                PdfTextLine previous = current.getLast();
                double height = Math.max(1.0, line.region().pageHeightPoints());
                double gap = line.region().yMinPoints() - previous.region().yMaxPoints();
                // Nearby cells and formula fragments on the same visual band belong
                // to one recovery crop even when their horizontal boxes do not overlap.
                // Splitting on columns produced microscopic one-letter ROIs for P2.
                if (gap / height > 0.035) {
                    result.add(List.copyOf(current));
                    current.clear();
                }
            }
            current.add(line);
        }
        if (!current.isEmpty()) result.add(List.copyOf(current));
        return result;
    }

    private static double horizontalOverlap(PdfPageRegion a, PdfPageRegion b) {
        double intersection = Math.max(0.0,
                Math.min(a.xMaxPoints(), b.xMaxPoints())
                        - Math.max(a.xMinPoints(), b.xMinPoints()));
        return intersection / Math.max(1.0, Math.min(
                a.xMaxPoints() - a.xMinPoints(), b.xMaxPoints() - b.xMinPoints()));
    }

    private static PdfSemanticPageAnalysis.NormalizedBox normalizedUnion(
            List<PdfTextLine> lines) {
        PdfPageRegion first = lines.getFirst().region();
        double xMin = lines.stream().map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::xMinPoints).min().orElse(0.0);
        double yMin = lines.stream().map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::yMinPoints).min().orElse(0.0);
        double xMax = lines.stream().map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::xMaxPoints).max().orElse(first.pageWidthPoints());
        double yMax = lines.stream().map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::yMaxPoints).max().orElse(first.pageHeightPoints());
        return new PdfSemanticPageAnalysis.NormalizedBox(
                xMin / first.pageWidthPoints() * 1000.0,
                yMin / first.pageHeightPoints() * 1000.0,
                xMax / first.pageWidthPoints() * 1000.0,
                yMax / first.pageHeightPoints() * 1000.0);
    }

    private static List<String> evidenceInside(PdfTextLayer layer,
                                                PdfSemanticPageAnalysis.NormalizedBox box) {
        if (layer == null || !layer.available()) return List.of();
        return layer.lines().stream().filter(line -> {
            PdfPageRegion region = line.region();
            double cx = ((region.xMinPoints() + region.xMaxPoints()) / 2.0)
                    / region.pageWidthPoints() * 1000.0;
            double cy = ((region.yMinPoints() + region.yMaxPoints()) / 2.0)
                    / region.pageHeightPoints() * 1000.0;
            return cx >= box.xMin() && cx <= box.xMax()
                    && cy >= box.yMin() && cy <= box.yMax();
        }).map(PdfTextLine::text).filter(value -> !value.isBlank())
                .limit(24).toList();
    }

    private static boolean centerInside(
            PdfPageRegion region,
            PdfSemanticPageAnalysis.NormalizedBox box) {
        double cx = ((region.xMinPoints() + region.xMaxPoints()) / 2.0)
                / region.pageWidthPoints() * 1000.0;
        double cy = ((region.yMinPoints() + region.yMaxPoints()) / 2.0)
                / region.pageHeightPoints() * 1000.0;
        return cx >= box.xMin() && cx <= box.xMax()
                && cy >= box.yMin() && cy <= box.yMax();
    }

    private static boolean columnLike(PdfSemanticPageAnalysis.NormalizedBox box) {
        return box.xMax() - box.xMin() < 560.0
                && (box.xMax() < 620.0 || box.xMin() > 380.0);
    }

    private static PdfSemanticPageAnalysis.NormalizedBox expanded(
            PdfSemanticPageAnalysis.NormalizedBox box) {
        return new PdfSemanticPageAnalysis.NormalizedBox(
                box.xMin() - ROI_MARGIN, box.yMin() - ROI_MARGIN,
                box.xMax() + ROI_MARGIN, box.yMax() + ROI_MARGIN).clamped();
    }
}
