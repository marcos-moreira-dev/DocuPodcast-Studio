package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Converts resolved PDF text layers into narratable document blocks with stable visual anchors. */
public final class PdfTextLayerBlockMapper {
    private static final double OCR_PARAGRAPH_VERTICAL_GAP_FACTOR = 1.65;
    private static final double OCR_MAX_PARAGRAPH_GAP_PAGE_FACTOR = 0.035;

    public PreparedPdfPage preparedPage(PdfOcrPageResult result,
                                        String sourceSha256,
                                        PreparedPdfPage previousPage) {
        if (result == null || result.pageNumber() <= 0) {
            throw new IllegalArgumentException("A valid OCR page result is required");
        }
        return preparedPage(result.textLayer(), result.pageWidthPoints(), result.pageHeightPoints(),
                sourceSha256, previousPage, PdfRegionOrigin.OCR_LOCAL,
                "tesseract", result.warnings());
    }

    public PreparedPdfPage preparedPage(PdfTextLayer layer,
                                        double widthPoints,
                                        double heightPoints,
                                        String sourceSha256,
                                        PreparedPdfPage previousPage,
                                        PdfRegionOrigin origin,
                                        String extractorVersion,
                                        List<String> extractionWarnings) {
        if (layer == null || layer.pageNumber() <= 0) {
            throw new IllegalArgumentException("A valid PDF text layer is required");
        }
        List<OcrParagraph> paragraphs = ocrParagraphs(layer);
        ArrayList<PdfRegion> candidates = new ArrayList<>();
        int order = 0;
        for (OcrParagraph paragraph : paragraphs) {
            PdfNarratabilityDecision decision = PdfNarratableTextClassifier.decide(
                    paragraph.text(), paragraph.confidence());
            PdfRegionType type = PdfNarratableTextClassifier.classify(paragraph.text(), order + 1);
            String generatedId = stableRegionId(sourceSha256, paragraph.region());
            candidates.add(new PdfRegion(
                    generatedId,
                    layer.pageNumber(),
                    paragraph.region().xMinPoints(),
                    paragraph.region().yMinPoints(),
                    paragraph.region().xMaxPoints(),
                    paragraph.region().yMaxPoints(),
                    0,
                    order++,
                    paragraph.text(),
                    type,
                    decision.narratability(),
                    decision.reasons(),
                    new PdfRegionEvidence(origin, paragraph.confidence(),
                            extractorVersion, origin == PdfRegionOrigin.NATIVE_TEXT ? "bbox-v1" : "tsv-v1",
                            "paragraph-v3", "narratability-v2"),
                    PdfRegionOverride.empty(),
                    regionAttributes(paragraph),
                    1L));
        }
        List<PdfRegion> reconciled = new PdfRegionReconciler().reconcile(
                previousPage == null ? List.of() : previousPage.regions(), candidates);
        ArrayList<String> warnings = new ArrayList<>(
                extractionWarnings == null ? List.of() : extractionWarnings);
        long uncertain = reconciled.stream()
                .filter(region -> region.effectiveNarratability() == PdfNarratability.UNCERTAIN).count();
        if (uncertain > 0) warnings.add(uncertain + " región(es) requieren revisión.");
        if (reconciled.isEmpty()) {
            warnings.add("No se detectaron regiones de texto en la página.");
        }
        PdfPagePreparationStatus status = warnings.isEmpty()
                ? PdfPagePreparationStatus.READY
                : PdfPagePreparationStatus.READY_WITH_WARNINGS;
        long revision = previousPage == null ? 1L : previousPage.revision() + 1L;
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, layer.pageNumber(),
                widthPoints, heightPoints, status, revision,
                reconciled, warnings, "");
    }

    private static List<OcrParagraph> ocrParagraphs(PdfTextLayer layer) {
        List<PdfTextLine> lines = layer.lines().stream()
                .filter(line -> line != null && !line.text().isBlank() && line.region() != null)
                .sorted(readingOrderComparator())
                .toList();
        if (lines.isEmpty()) {
            return List.of();
        }
        ArrayList<OcrParagraph> paragraphs = new ArrayList<>();
        for (List<PdfTextLine> visualGroup : visualLineGroups(lines)) {
            ArrayList<PdfTextLine> current = new ArrayList<>();
            PdfTextLine previous = null;
            for (PdfTextLine line : visualGroup) {
                if (previous != null && startsNewOcrParagraph(previous, line)) {
                    paragraphFromLines(current).ifPresent(paragraphs::add);
                    current.clear();
                }
                current.add(line);
                previous = line;
            }
            paragraphFromLines(current).ifPresent(paragraphs::add);
        }
        return paragraphs.stream()
                .sorted(java.util.Comparator
                        .comparingInt((OcrParagraph paragraph) -> paragraph.region().pageNumber())
                        .thenComparingDouble(paragraph -> paragraph.region().yMinPoints())
                        .thenComparingDouble(paragraph -> paragraph.region().xMinPoints()))
                .toList();
    }

    private static java.util.Comparator<PdfTextLine> readingOrderComparator() {
        return java.util.Comparator
                .comparingInt(PdfTextLine::pageNumber)
                .thenComparingDouble((PdfTextLine line) -> line.region().yMinPoints())
                .thenComparingDouble(line -> line.region().xMinPoints());
    }

    private static List<List<PdfTextLine>> visualLineGroups(List<PdfTextLine> lines) {
        ArrayList<List<PdfTextLine>> groups = new ArrayList<>();
        for (PdfTextLine line : lines) {
            int matchingIndex = -1;
            for (int index = 0; index < groups.size(); index++) {
                List<PdfTextLine> group = groups.get(index);
                if (!group.isEmpty() && sameVisualGroup(group.getFirst(), line)) {
                    matchingIndex = index;
                    break;
                }
            }
            if (matchingIndex < 0) {
                ArrayList<PdfTextLine> group = new ArrayList<>();
                group.add(line);
                groups.add(group);
            } else {
                groups.get(matchingIndex).add(line);
            }
        }
        groups.replaceAll(group -> group.stream().sorted(readingOrderComparator()).toList());
        return groups.stream()
                .sorted(java.util.Comparator
                        .comparingDouble((List<PdfTextLine> group) -> group.getFirst().region().yMinPoints())
                        .thenComparingDouble(group -> group.getFirst().region().xMinPoints()))
                .toList();
    }

    private static boolean sameVisualGroup(PdfTextLine reference, PdfTextLine candidate) {
        PdfPageRegion referenceRegion = reference.region();
        PdfPageRegion candidateRegion = candidate.region();
        if (referenceRegion.pageNumber() != candidateRegion.pageNumber()) {
            return false;
        }
        if (differentDominantColor(reference, candidate)) {
            return false;
        }
        return sameProximityGroup(referenceRegion, candidateRegion);
    }

    private static boolean startsNewOcrParagraph(PdfTextLine previous, PdfTextLine current) {
        PdfPageRegion previousRegion = previous.region();
        PdfPageRegion currentRegion = current.region();
        if (previousRegion.pageNumber() != currentRegion.pageNumber()) {
            return true;
        }
        if (differentDominantColor(previous, current) || !sameProximityGroup(previousRegion, currentRegion)) {
            return true;
        }
        PdfNarratability previousDecision = PdfNarratableTextClassifier
                .decide(previous.text(), previous.confidence()).narratability();
        PdfNarratability currentDecision = PdfNarratableTextClassifier
                .decide(current.text(), current.confidence()).narratability();
        if ((previousDecision == PdfNarratability.NON_NARRATABLE)
                != (currentDecision == PdfNarratability.NON_NARRATABLE)) {
            return true;
        }
        if (looksLikeStandaloneMath(previous.text())
                != looksLikeStandaloneMath(current.text())) {
            return true;
        }
        double previousHeight = Math.max(1.0, previousRegion.yMaxPoints() - previousRegion.yMinPoints());
        double gap = currentRegion.yMinPoints() - previousRegion.yMaxPoints();
        double pageHeight = Math.max(previousRegion.pageHeightPoints(),
                currentRegion.pageHeightPoints());
        double hardGapLimit = Math.max(18.0,
                pageHeight * OCR_MAX_PARAGRAPH_GAP_PAGE_FACTOR);
        double paragraphGapLimit = Math.min(
                previousHeight * OCR_PARAGRAPH_VERTICAL_GAP_FACTOR,
                hardGapLimit);
        double visibleParagraphGap = pageHeight * 0.006;
        if (gap > visibleParagraphGap
                && endsSentence(previous.text())
                && startsSentence(current.text())) {
            return true;
        }
        double pageWidth = Math.max(previousRegion.pageWidthPoints(),
                currentRegion.pageWidthPoints());
        if (gap > pageHeight * 0.004
                && Math.abs(previousRegion.xMinPoints()
                - currentRegion.xMinPoints()) > pageWidth * 0.012) {
            return true;
        }
        if (gap > paragraphGapLimit) {
            return true;
        }
        String text = current.text();
        return text.matches("^\\s*(\\d+(\\.\\d+){0,3}|[A-Z]\\.|[*\\-])\\s+.+")
                && gap > previousHeight * 0.45;
    }

    private static boolean endsSentence(String value) {
        String text = value == null ? "" : value.strip();
        return text.matches("(?s).*[.!?][\\p{Pf}\\p{Pe}\"']?\\s*$");
    }

    private static boolean startsSentence(String value) {
        String text = value == null ? "" : value.strip();
        return text.matches("^[\\p{Lu}¿¡\"'“‘].*");
    }

    private static boolean looksLikeStandaloneMath(String value) {
        String text = value == null ? "" : value.strip();
        if (text.isBlank() || text.length() > 120) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b(se lee|significa|donde|entonces|por tanto|definici[oó]n|expresi[oó]n)\\b.*")) {
            return false;
        }
        long mathSymbols = text.codePoints().filter(codePoint ->
                "=<>≤≥≠∈∉∀∃∧∨¬→←⇒⇔±×÷∑∫√^".indexOf(codePoint) >= 0).count();
        if (mathSymbols == 0) {
            return false;
        }
        long letters = text.codePoints().filter(Character::isLetter).count();
        long whitespaceSeparatedTokens = java.util.Arrays.stream(text.split("\\s+"))
                .filter(token -> !token.isBlank())
                .count();
        return mathSymbols >= 2 || (letters <= 12 && whitespaceSeparatedTokens <= 10);
    }

    private static boolean differentDominantColor(PdfTextLine previous, PdfTextLine current) {
        String previousColor = normalizedColor(previous.dominantColor());
        String currentColor = normalizedColor(current.dominantColor());
        return !previousColor.isBlank() && !currentColor.isBlank() && !previousColor.equals(currentColor);
    }

    private static String normalizedColor(String color) {
        if (color == null || color.isBlank() || "other".equalsIgnoreCase(color)) {
            return "";
        }
        return color.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean sameProximityGroup(PdfPageRegion previous, PdfPageRegion current) {
        double previousHeight = Math.max(1.0, previous.yMaxPoints() - previous.yMinPoints());
        double currentHeight = Math.max(1.0, current.yMaxPoints() - current.yMinPoints());
        double maxHeight = Math.max(previousHeight, currentHeight);
        double verticalGap = current.yMinPoints() - previous.yMaxPoints();
        if (verticalGap < -maxHeight * 0.55) {
            return false;
        }
        double horizontalOverlap = Math.min(previous.xMaxPoints(), current.xMaxPoints())
                - Math.max(previous.xMinPoints(), current.xMinPoints());
        double currentWidth = Math.max(1.0, current.xMaxPoints() - current.xMinPoints());
        double previousWidth = Math.max(1.0, previous.xMaxPoints() - previous.xMinPoints());
        double overlapRatio = horizontalOverlap / Math.min(previousWidth, currentWidth);
        double leftDelta = Math.abs(previous.xMinPoints() - current.xMinPoints());
        double pageWidth = Math.max(previous.pageWidthPoints(), current.pageWidthPoints());
        double columnTolerance = Math.max(24.0, pageWidth * 0.08);
        return overlapRatio >= 0.25 || leftDelta <= columnTolerance;
    }

    private static Optional<OcrParagraph> paragraphFromLines(List<PdfTextLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }
        List<PdfTextLine> safeLines = lines.stream().filter(line -> line != null && line.region() != null).toList();
        if (safeLines.isEmpty()) {
            return Optional.empty();
        }
        ArrayList<LineFragment> fragments = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();
        for (PdfTextLine line : safeLines) {
            String lineText = line.text().strip();
            if (lineText.isBlank()) {
                continue;
            }
            if (!textBuilder.isEmpty()) {
                textBuilder.append(' ');
            }
            int start = textBuilder.length();
            textBuilder.append(lineText);
            fragments.add(new LineFragment(line.region(), start, textBuilder.length()));
        }
        String text = textBuilder.toString().strip();
        if (text.isBlank()) {
            return Optional.empty();
        }
        double pageWidth = safeLines.getFirst().region().pageWidthPoints();
        double pageHeight = safeLines.getFirst().region().pageHeightPoints();
        double xMin = safeLines.stream().mapToDouble(line -> line.region().xMinPoints()).min().orElse(0.0);
        double yMin = safeLines.stream().mapToDouble(line -> line.region().yMinPoints()).min().orElse(0.0);
        double xMax = safeLines.stream().mapToDouble(line -> line.region().xMaxPoints()).max().orElse(pageWidth);
        double yMax = safeLines.stream().mapToDouble(line -> line.region().yMaxPoints()).max().orElse(pageHeight);
        double confidence = safeLines.stream().mapToDouble(PdfTextLine::confidence).average().orElse(0.0);
        try {
            return Optional.of(new OcrParagraph(text,
                    new PdfPageRegion(safeLines.getFirst().pageNumber(), xMin, yMin, xMax, yMax, pageWidth, pageHeight),
                    confidence,
                    dominantColor(safeLines),
                    fragments));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static String dominantColor(List<PdfTextLine> lines) {
        return lines.stream()
                .map(PdfTextLine::dominantColor)
                .map(PdfTextLayerBlockMapper::normalizedColor)
                .filter(color -> !color.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        color -> color,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private static String stableRegionId(String sourceSha256, PdfPageRegion region) {
        String source = sourceSha256 == null || sourceSha256.isBlank() ? "unknown" : sourceSha256.strip();
        String identity = source + "|" + region.pageNumber() + "|"
                + Math.round(region.xMinPoints() * 2.0) + "|"
                + Math.round(region.yMinPoints() * 2.0) + "|"
                + Math.round(region.xMaxPoints() * 2.0) + "|"
                + Math.round(region.yMaxPoints() * 2.0);
        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
        return "P%06d-R-%s".formatted(region.pageNumber(), uuid.toString());
    }

    private static Map<String, String> regionAttributes(OcrParagraph paragraph) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        if (!paragraph.dominantColor().isBlank()) {
            attributes.put("dominantColor", paragraph.dominantColor());
        }
        if (!paragraph.lineFragments().isEmpty()) {
            attributes.put("lineBboxes", lineBboxes(paragraph.lineFragments()));
            attributes.put("lineCharRanges", lineCharRanges(paragraph.lineFragments()));
        }
        return Map.copyOf(attributes);
    }

    private static String lineBboxes(List<LineFragment> fragments) {
        return fragments.stream()
                .map(fragment -> fragment.region().bbox())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private static String lineCharRanges(List<LineFragment> fragments) {
        return fragments.stream()
                .map(fragment -> fragment.startOffset() + "-" + fragment.endOffset())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private record OcrParagraph(String text,
                                PdfPageRegion region,
                                double confidence,
                                String dominantColor,
                                List<LineFragment> lineFragments) {
        private OcrParagraph {
            text = text == null ? "" : text.strip();
            confidence = Double.isFinite(confidence) ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
            dominantColor = normalizedColor(dominantColor);
            lineFragments = lineFragments == null ? List.of() : List.copyOf(lineFragments);
        }
    }

    private record LineFragment(PdfPageRegion region, int startOffset, int endOffset) {
        private LineFragment {
            startOffset = Math.max(0, startOffset);
            endOffset = Math.max(startOffset, endOffset);
        }
    }
}
