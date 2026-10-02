package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Finds strong uncovered text clusters without assigning their semantic type. */
public final class PdfUncoveredProminentTextDetector {
    private static final Set<PdfRegionType> PROTECTED = Set.of(
            PdfRegionType.TABLE, PdfRegionType.MATH, PdfRegionType.IMAGE,
            PdfRegionType.CODE);

    public List<PdfProminentTextCandidate> detect(
            PdfTextLayer visualText,
            PdfSemanticPageAnalysis semantic,
            List<PdfRepeatedMarginEvidence> repeated) {
        if (visualText == null || !visualText.available() || semantic == null) {
            return List.of();
        }
        List<PdfTextLine> lines = visualText.lines().stream()
                .filter(line -> line != null && line.region() != null)
                .filter(line -> !PdfSemanticCoverageValidator.normalized(
                        line.text()).isBlank())
                .sorted(Comparator.comparingDouble(line ->
                        line.region().yMinPoints())).toList();
        double medianHeight = median(lines.stream().map(PdfTextLine::region)
                .mapToDouble(region -> region.yMaxPoints() - region.yMinPoints())
                .filter(value -> value > 1.0).sorted().toArray());
        if (medianHeight <= 0.0) return List.of();
        ArrayList<PdfProminentTextCandidate> result = new ArrayList<>();
        for (PdfTextLine line : lines) {
            PdfPageRegion box = line.region();
            List<String> tokens = PdfSemanticCoverageValidator.tokens(line.text());
            double height = box.yMaxPoints() - box.yMinPoints();
            double prominence = height / medianHeight;
            double relativeY = box.yMinPoints() / box.pageHeightPoints();
            if (tokens.size() < 4 || tokens.size() > 28
                    || line.text().length() < 18 || line.confidence() < 0.88
                    || prominence < 1.16 || relativeY < 0.012 || relativeY > 0.92) {
                continue;
            }
            if (pageNumber(line.text()) || repeated(line, repeated)
                    || covered(line, semantic, false)
                    || covered(line, semantic, true)) {
                continue;
            }
            ArrayList<String> signals = new ArrayList<>();
            signals.add("high-confidence-text");
            signals.add("line-height-above-page-median");
            signals.add("independent-uncovered-bbox");
            if (relativeY <= 0.35) signals.add("section-start-position");
            result.add(new PdfProminentTextCandidate(evidenceId(line),
                    line.text(), box, line.confidence(),
                    height / box.pageHeightPoints(), relativeY, prominence,
                    visualText.origin() == PdfTextLayerOrigin.OCR_LOCAL
                            ? "OCR_TEXT_GEOMETRY" : visualText.origin().name(),
                    signals));
        }
        return result.stream().limit(2).toList();
    }

    public PdfSemanticRecoveryPlan recoveryPlan(
            List<PdfProminentTextCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new PdfSemanticRecoveryPlan(List.of());
        }
        return new PdfSemanticRecoveryPlan(candidates.stream().limit(2)
                .map(candidate -> new PdfSemanticRecoveryRoi(
                        PdfSemanticRecoveryReason.PROMINENT_TEXT_UNCOVERED,
                        expanded(candidate.region()), PdfRegionType.UNKNOWN,
                        List.of(candidate.text()))).toList());
    }

    private static boolean repeated(PdfTextLine line,
                                    List<PdfRepeatedMarginEvidence> repeated) {
        return repeated != null && repeated.stream().anyMatch(value ->
                PdfRepeatedMarginPolicy.textSimilarity(line.text(), value.text()) >= 0.82);
    }

    private static boolean covered(PdfTextLine line,
                                   PdfSemanticPageAnalysis semantic,
                                   boolean protectedOnly) {
        PdfSemanticPageAnalysis.NormalizedBox evidence = normalized(line.region());
        for (PdfSemanticPageAnalysis.Element element : semantic.elements()) {
            if (protectedOnly != PROTECTED.contains(element.type())) continue;
            double containment = containment(evidence, element.box());
            double similarity = PdfRepeatedMarginPolicy.textSimilarity(
                    line.text(), element.sourceText());
            if (protectedOnly && containment >= 0.20) return true;
            if (!protectedOnly && (similarity >= 0.82
                    || containment >= 0.55 && similarity >= 0.28)) return true;
        }
        return false;
    }

    private static boolean pageNumber(String value) {
        return PdfSemanticCoverageValidator.normalized(value)
                .matches("(?:pagina\\s*)?\\d{1,4}");
    }

    private static PdfSemanticPageAnalysis.NormalizedBox expanded(PdfPageRegion box) {
        PdfSemanticPageAnalysis.NormalizedBox value = normalized(box);
        double marginX = 10.0;
        double marginY = 8.0;
        return new PdfSemanticPageAnalysis.NormalizedBox(
                value.xMin() - marginX, value.yMin() - marginY,
                value.xMax() + marginX, value.yMax() + marginY).clamped();
    }

    private static PdfSemanticPageAnalysis.NormalizedBox normalized(PdfPageRegion box) {
        return new PdfSemanticPageAnalysis.NormalizedBox(
                box.xMinPoints() / box.pageWidthPoints() * 1000.0,
                box.yMinPoints() / box.pageHeightPoints() * 1000.0,
                box.xMaxPoints() / box.pageWidthPoints() * 1000.0,
                box.yMaxPoints() / box.pageHeightPoints() * 1000.0);
    }

    private static double containment(PdfSemanticPageAnalysis.NormalizedBox evidence,
                                      PdfSemanticPageAnalysis.NormalizedBox region) {
        double intersection = Math.max(0.0, Math.min(evidence.xMax(), region.xMax())
                - Math.max(evidence.xMin(), region.xMin()))
                * Math.max(0.0, Math.min(evidence.yMax(), region.yMax())
                - Math.max(evidence.yMin(), region.yMin()));
        double area = Math.max(1.0, (evidence.xMax() - evidence.xMin())
                * (evidence.yMax() - evidence.yMin()));
        return intersection / area;
    }

    private static double median(double[] values) {
        if (values.length == 0) return 0.0;
        int middle = values.length / 2;
        return values.length % 2 == 0
                ? (values[middle - 1] + values[middle]) / 2.0
                : values[middle];
    }

    private static String evidenceId(PdfTextLine line) {
        PdfPageRegion box = line.region();
        return Integer.toHexString((PdfSemanticCoverageValidator.normalized(
                line.text()) + '|' + Math.round(box.xMinPoints() * 10.0)
                + '|' + Math.round(box.yMinPoints() * 10.0)).hashCode());
    }
}
