package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.List;

/** Detects textual supersegments before they can become canonical playback units. */
public final class PdfSemanticGranularityValidator {
    public static final double GIANT_TEXTUAL_AREA_RATIO = 0.45;

    public List<PdfSemanticPageAnalysis.Element> refinementCandidates(
            PdfSemanticPageAnalysis analysis, PdfTextLayer nativeLayer) {
        if (analysis == null) return List.of();
        return analysis.elements().stream()
                .filter(PdfSemanticGranularityValidator::textual)
                .filter(element -> areaRatio(element.box())
                        >= GIANT_TEXTUAL_AREA_RATIO)
                .filter(element -> evidenceOfIndependentUnits(
                        element, nativeLayer, analysis))
                .toList();
    }

    public void validateCanonical(PdfSemanticPageAnalysis analysis) {
        if (analysis == null) return;
        for (PdfSemanticPageAnalysis.Element element : analysis.elements()) {
            boolean container = "CONTAINER".equals(
                    element.attributes().get("regionRole"));
            boolean playback = Boolean.parseBoolean(
                    element.attributes().getOrDefault("playbackTarget", "true"));
            if (container && playback) {
                throw new IllegalArgumentException(
                        "A PDF container cannot be a playback target");
            }
            if (!container && playback && textual(element)
                    && areaRatio(element.box()) >= 0.80) {
                throw new IllegalArgumentException(
                        "A giant textual leaf cannot be published for playback");
            }
        }
    }

    private static boolean evidenceOfIndependentUnits(
            PdfSemanticPageAnalysis.Element giant, PdfTextLayer nativeLayer,
            PdfSemanticPageAnalysis page) {
        long enclosedSemanticPeers = page.elements().stream()
                .filter(value -> value != giant)
                .filter(value -> centerInside(value.box(), giant.box()))
                .count();
        if (enclosedSemanticPeers >= 2) return true;
        if (nativeLayer == null || !nativeLayer.available()) return false;
        List<PdfTextLine> inside = nativeLayer.lines().stream()
                .filter(line -> centerInside(line.region(), giant.box()))
                .filter(line -> line.text().length() >= 8)
                .sorted(java.util.Comparator.comparingDouble(
                        line -> line.region().yMinPoints())).toList();
        if (inside.size() < 6) return false;
        int separatedBands = 1;
        for (int index = 1; index < inside.size(); index++) {
            PdfPageRegion previous = inside.get(index - 1).region();
            PdfPageRegion current = inside.get(index).region();
            double gap = current.yMinPoints() - previous.yMaxPoints();
            if (gap / current.pageHeightPoints() >= 0.018) separatedBands++;
        }
        return separatedBands >= 3;
    }

    private static boolean textual(PdfSemanticPageAnalysis.Element value) {
        return switch (value.type()) {
            case PARAGRAPH, SIDEBAR, LIST, UNKNOWN -> true;
            default -> false;
        };
    }

    public static double areaRatio(PdfSemanticPageAnalysis.NormalizedBox box) {
        return Math.max(0.0, box.xMax() - box.xMin())
                * Math.max(0.0, box.yMax() - box.yMin()) / 1_000_000.0;
    }

    private static boolean centerInside(
            PdfSemanticPageAnalysis.NormalizedBox child,
            PdfSemanticPageAnalysis.NormalizedBox parent) {
        double x = (child.xMin() + child.xMax()) / 2.0;
        double y = (child.yMin() + child.yMax()) / 2.0;
        return x >= parent.xMin() && x <= parent.xMax()
                && y >= parent.yMin() && y <= parent.yMax();
    }

    private static boolean centerInside(PdfPageRegion child,
                                        PdfSemanticPageAnalysis.NormalizedBox parent) {
        double x = (child.xMinPoints() + child.xMaxPoints()) / 2.0
                / child.pageWidthPoints() * 1000.0;
        double y = (child.yMinPoints() + child.yMaxPoints()) / 2.0
                / child.pageHeightPoints() * 1000.0;
        return x >= parent.xMin() && x <= parent.xMax()
                && y >= parent.yMin() && y <= parent.yMax();
    }
}
