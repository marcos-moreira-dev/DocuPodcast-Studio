package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Converts resolved PDF text layers into narratable document blocks with stable visual anchors. */
public final class PdfTextLayerBlockMapper {
    private static final double OCR_PARAGRAPH_VERTICAL_GAP_FACTOR = 1.65;

    public List<DocumentBlock> blocksFromOcrLayer(PdfTextLayer layer,
                                                  int pageCount,
                                                  boolean visualRenderable,
                                                  int startIndex) {
        if (layer == null || !layer.available()) {
            return List.of();
        }
        int index = Math.max(1, startIndex);
        ArrayList<DocumentBlock> blocks = new ArrayList<>();
        for (OcrParagraph paragraph : ocrParagraphs(layer)) {
            String normalized = paragraph.text().strip();
            if (PdfNarratableTextClassifier.shouldSkip(normalized)) {
                continue;
            }
            blocks.add(ocrBlock(blockId(index), normalized, paragraph, pageCount, visualRenderable, index));
            index++;
        }
        return List.copyOf(blocks);
    }

    public static String blockId(int index) {
        return "B" + String.format(Locale.ROOT, "%04d", Math.max(1, index));
    }

    public static DocumentBlockType classifyPdfBlock(String text, int index) {
        return PdfNarratableTextClassifier.classify(text, index);
    }

    private static List<OcrParagraph> ocrParagraphs(PdfTextLayer layer) {
        List<PdfTextLine> lines = layer.lines().stream()
                .filter(line -> line != null && !line.text().isBlank() && line.region() != null)
                .filter(PdfTextLayerBlockMapper::isNarratableOcrLine)
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

    private static boolean isNarratableOcrLine(PdfTextLine line) {
        return line != null
                && !line.text().isBlank()
                && line.region() != null
                && PdfNarratableTextClassifier.narratableProse(line.text());
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
        double previousHeight = Math.max(1.0, previousRegion.yMaxPoints() - previousRegion.yMinPoints());
        double gap = currentRegion.yMinPoints() - previousRegion.yMaxPoints();
        if (gap > previousHeight * OCR_PARAGRAPH_VERTICAL_GAP_FACTOR) {
            return true;
        }
        String text = current.text();
        return text.matches("^\\s*(\\d+(\\.\\d+){0,3}|[A-Z]\\.|[*\\-])\\s+.+")
                && gap > previousHeight * 0.45;
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

    private static DocumentBlock ocrBlock(String blockId,
                                          String text,
                                          OcrParagraph paragraph,
                                          int pageCount,
                                          boolean visualRenderable,
                                          int index) {
        DocumentBlockType type = classifyPdfBlock(text, index);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourceMode", "read-only");
        metadata.put("sourceFormat", SourceDocumentFormat.PDF.name());
        metadata.put("pdfTextMapVersion", "1");
        metadata.put("nativeText", "false");
        metadata.put("ocr", "true");
        metadata.put("sourcePage", Integer.toString(paragraph.region().pageNumber()));
        metadata.put("sourcePageCount", Integer.toString(Math.max(1, pageCount)));
        metadata.put("visualRenderAvailable", Boolean.toString(visualRenderable));
        metadata.put("visualRenderEngine", visualRenderable ? "pdfbox" : "");
        metadata.put("bbox", paragraph.region().bbox());
        metadata.put("bboxUnits", "pdf-points");
        metadata.put("pageWidth", formatNumber(paragraph.region().pageWidthPoints()));
        metadata.put("pageHeight", formatNumber(paragraph.region().pageHeightPoints()));
        metadata.put("extractionMode", "ocr-local");
        metadata.put("confidence", "ocr-local");
        metadata.put("ocrConfidence", formatNumber(paragraph.confidence()));
        if (!paragraph.dominantColor().isBlank()) {
            metadata.put("dominantColor", paragraph.dominantColor());
        }
        if (!paragraph.lineFragments().isEmpty()) {
            metadata.put("lineBboxes", lineBboxes(paragraph.lineFragments()));
            metadata.put("lineCharRanges", lineCharRanges(paragraph.lineFragments()));
        }
        metadata.put("sourceLocatorLabel", "PDF OCR - pagina " + paragraph.region().pageNumber() + " - bloque " + blockId);
        return DocumentBlock.of(blockId, type, text, "PDF OCR", metadata);
    }

    private static String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
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
