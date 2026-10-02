package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Conservative multi-page policy for running headers and footers.
 * Position alone is never sufficient: text and geometry must repeat on a peer page.
 */
public final class PdfRepeatedMarginPolicy {
    private static final double OUTER_MARGIN = 0.045;
    private static final double MAX_GEOMETRY_DISTANCE = 0.018;

    public List<PdfRepeatedMarginEvidence> detect(
            PdfTextLayer current, List<PdfTextLayer> peers) {
        if (current == null || !current.available() || peers == null || peers.isEmpty()) {
            return List.of();
        }
        ArrayList<PdfRepeatedMarginEvidence> result = new ArrayList<>();
        for (PdfTextLine line : current.lines()) {
            if (!eligibleMarginLine(line)) continue;
            int matchingPages = 1;
            double bestGeometry = 0.0;
            for (PdfTextLayer peer : peers) {
                if (peer == null || !peer.available()) continue;
                PdfTextLine match = peer.lines().stream()
                        .filter(PdfRepeatedMarginPolicy::eligibleMarginLine)
                        .filter(value -> textSimilarity(line.text(), value.text()) >= 0.88)
                        .filter(value -> geometryDistance(line.region(), value.region())
                                <= MAX_GEOMETRY_DISTANCE)
                        .findFirst().orElse(null);
                if (match != null) {
                    matchingPages++;
                    bestGeometry = Math.max(bestGeometry,
                            1.0 - geometryDistance(line.region(), match.region())
                                    / MAX_GEOMETRY_DISTANCE);
                }
            }
            if (matchingPages < 2) continue;
            PdfPageRegion box = line.region();
            boolean top = box.yMaxPoints() / box.pageHeightPoints() <= OUTER_MARGIN;
            result.add(new PdfRepeatedMarginEvidence(line.text(), box,
                    top ? PdfRegionType.HEADER : PdfRegionType.FOOTER,
                    matchingPages, bestGeometry,
                    ((box.yMinPoints() + box.yMaxPoints()) / 2.0)
                            / box.pageHeightPoints()));
        }
        return List.copyOf(result);
    }

    public PdfSemanticPageAnalysis apply(
            PdfSemanticPageAnalysis analysis,
            List<PdfRepeatedMarginEvidence> repeated) {
        if (analysis == null || repeated == null || repeated.isEmpty()) return analysis;
        List<PdfSemanticPageAnalysis.Element> elements = analysis.elements().stream()
                .map(element -> classify(element, repeated)).toList();
        return new PdfSemanticPageAnalysis(analysis.reportedPageNumber(),
                analysis.language(), analysis.pageRole(), elements,
                analysis.confidence(), analysis.uncertainties());
    }

    private static PdfSemanticPageAnalysis.Element classify(
            PdfSemanticPageAnalysis.Element element,
            List<PdfRepeatedMarginEvidence> repeated) {
        PdfRepeatedMarginEvidence match = repeated.stream().filter(value ->
                        textSimilarity(element.sourceText(), value.text()) >= 0.78
                                || compactMarginElement(element.box(), value.type())
                                && containment(value.region(), element.box()) >= 0.62)
                .findFirst().orElse(null);
        if (match == null) return element;
        LinkedHashMap<String, String> attributes =
                new LinkedHashMap<>(element.attributes());
        attributes.put("editorialRole", match.type().name());
        attributes.put("repeatedAcrossPages", Integer.toString(match.matchingPages()));
        attributes.put("repeatedGeometrySimilarity", number(match.geometrySimilarity()));
        attributes.put("playbackTarget", "false");
        attributes.put("runningMarginPolicy", "native-repeat-v1");
        return new PdfSemanticPageAnalysis.Element(element.responseId(),
                element.readingOrder(), match.type(), element.box(),
                match.text(), "", PdfNarratability.NON_NARRATABLE,
                element.confidence(), element.uncertainties(), attributes);
    }

    private static boolean compactMarginElement(
            PdfSemanticPageAnalysis.NormalizedBox box, PdfRegionType type) {
        double height = box.yMax() - box.yMin();
        if (height > 85.0) return false;
        return type == PdfRegionType.HEADER
                ? box.yMax() <= 65.0
                : box.yMin() >= 935.0;
    }

    private static boolean eligibleMarginLine(PdfTextLine line) {
        if (line == null || line.region() == null) return false;
        PdfPageRegion box = line.region();
        double top = box.yMaxPoints() / box.pageHeightPoints();
        double bottom = box.yMinPoints() / box.pageHeightPoints();
        int tokens = PdfSemanticCoverageValidator.tokens(line.text()).size();
        return tokens >= 2 && tokens <= 18
                && (top <= OUTER_MARGIN || bottom >= 1.0 - OUTER_MARGIN);
    }

    private static double geometryDistance(PdfPageRegion left, PdfPageRegion right) {
        double lx = left.xMinPoints() / left.pageWidthPoints();
        double ly = left.yMinPoints() / left.pageHeightPoints();
        double lw = (left.xMaxPoints() - left.xMinPoints()) / left.pageWidthPoints();
        double lh = (left.yMaxPoints() - left.yMinPoints()) / left.pageHeightPoints();
        double rx = right.xMinPoints() / right.pageWidthPoints();
        double ry = right.yMinPoints() / right.pageHeightPoints();
        double rw = (right.xMaxPoints() - right.xMinPoints()) / right.pageWidthPoints();
        double rh = (right.yMaxPoints() - right.yMinPoints()) / right.pageHeightPoints();
        return Math.max(Math.max(Math.abs(lx - rx), Math.abs(ly - ry)),
                Math.max(Math.abs(lw - rw), Math.abs(lh - rh)));
    }

    private static double containment(PdfPageRegion evidence,
                                      PdfSemanticPageAnalysis.NormalizedBox region) {
        double x1 = evidence.xMinPoints() / evidence.pageWidthPoints() * 1000.0;
        double y1 = evidence.yMinPoints() / evidence.pageHeightPoints() * 1000.0;
        double x2 = evidence.xMaxPoints() / evidence.pageWidthPoints() * 1000.0;
        double y2 = evidence.yMaxPoints() / evidence.pageHeightPoints() * 1000.0;
        double intersection = Math.max(0.0, Math.min(x2, region.xMax())
                - Math.max(x1, region.xMin()))
                * Math.max(0.0, Math.min(y2, region.yMax())
                - Math.max(y1, region.yMin()));
        double area = Math.max(1.0, (x2 - x1) * (y2 - y1));
        return intersection / area;
    }

    static double textSimilarity(String left, String right) {
        List<String> a = PdfSemanticCoverageValidator.tokens(left);
        List<String> b = PdfSemanticCoverageValidator.tokens(right);
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        java.util.Set<String> intersection = new java.util.HashSet<>(a);
        intersection.retainAll(new java.util.HashSet<>(b));
        return intersection.size() / (double) Math.max(a.size(), b.size());
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.4f", value);
    }
}
