package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSpan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds the PDF visual read/highlight layer without depending on presentation or PDFBox. */
public final class BuildPdfVisualReadingProjectionUseCase {
    private final BuildPdfNativeTextLayerUseCase buildNativeTextLayer;

    public BuildPdfVisualReadingProjectionUseCase(BuildPdfNativeTextLayerUseCase buildNativeTextLayer) {
        this.buildNativeTextLayer = buildNativeTextLayer == null
                ? new BuildPdfNativeTextLayerUseCase()
                : buildNativeTextLayer;
    }

    public PdfVisualReadingProjection build(ReadableDocument document) {
        if (document == null || document.format() != SourceDocumentFormat.PDF) {
            return new PdfVisualReadingProjection(List.of(), Map.of(), List.of("No hay PDF visual activo."));
        }
        ReadableDocument ocrDocument = document.withBlocks(document.blocks().stream()
                .filter(BuildPdfVisualReadingProjectionUseCase::isOcrBlock)
                .toList());
        List<PdfTextLayer> layers = buildNativeTextLayer.build(ocrDocument);
        Map<String, PdfVisualTextHighlight> highlights = new LinkedHashMap<>();
        List<PdfVisualTextTarget> targets = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            if (!isOcrBlock(block) || !block.narratable() || PdfNarratableTextClassifier.shouldSkip(block.text())) {
                continue;
            }
            regionFromBlock(block).ifPresent(region -> {
                PdfTextLayerOrigin origin = originFromBlock(block);
                highlights.put(block.id(), new PdfVisualTextHighlight(region.pageNumber(), region, block.text(), origin));
                targets.add(blockTarget(block, region, origin));
                targets.addAll(sentenceTargets(block, region, origin));
            });
        }
        if (highlights.isEmpty()) {
            return new PdfVisualReadingProjection(layers, highlights, targets,
                    List.of("El PDF no tiene capa textual u OCR confiable para lectura/resaltado."));
        }
        return new PdfVisualReadingProjection(layers, highlights, targets, List.of());
    }

    private static boolean isOcrBlock(DocumentBlock block) {
        return block != null
                && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))
                && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""));
    }

    private static PdfVisualTextTarget blockTarget(DocumentBlock block, PdfPageRegion region, PdfTextLayerOrigin origin) {
        DocumentTextRange range = new DocumentTextRange(block.id(), 0, block.text().length());
        return new PdfVisualTextTarget(block.id() + "-B", block.id(), range, region.pageNumber(), region,
                block.text(), origin, PdfVisualTextTargetKind.BLOCK);
    }

    private static List<PdfVisualTextTarget> sentenceTargets(DocumentBlock block, PdfPageRegion blockRegion,
                                                            PdfTextLayerOrigin origin) {
        List<DocumentSentenceSpan> spans = DocumentSentenceSplitter.split(block);
        if (spans.isEmpty()) {
            return List.of();
        }
        boolean hasLineFragments = !lineFragments(block, blockRegion).isEmpty();
        ArrayList<PdfVisualTextTarget> targets = new ArrayList<>();
        for (DocumentSentenceSpan span : spans) {
            if (span.blank() || PdfNarratableTextClassifier.shouldSkip(span.text())) {
                continue;
            }
            Optional<PdfPageRegion> region = sentenceRegion(block, blockRegion, span.range(), hasLineFragments);
            region.ifPresent(pdfRegion -> targets.add(new PdfVisualTextTarget(
                    span.id(),
                    span.blockId(),
                    span.range(),
                    pdfRegion.pageNumber(),
                    pdfRegion,
                    span.text(),
                    origin,
                    PdfVisualTextTargetKind.SENTENCE)));
        }
        return List.copyOf(targets);
    }

    private static Optional<PdfPageRegion> sentenceRegion(DocumentBlock block,
                                                          PdfPageRegion blockRegion,
                                                          DocumentTextRange range,
                                                          boolean hasLineFragments) {
        if (rangeCoversMost(range, block.text())) {
            return Optional.of(blockRegion);
        }
        if (!hasLineFragments) {
            return Optional.empty();
        }
        List<LineFragment> fragments = lineFragments(block, blockRegion);
        ArrayList<PdfPageRegion> parts = new ArrayList<>();
        for (LineFragment fragment : fragments) {
            Optional<PdfPageRegion> overlap = overlapRegion(fragment, range);
            overlap.ifPresent(parts::add);
        }
        return union(parts, blockRegion);
    }

    private static Optional<PdfPageRegion> overlapRegion(LineFragment fragment, DocumentTextRange range) {
        int start = Math.max(fragment.startOffset(), range.startOffset());
        int end = Math.min(fragment.endOffset(), range.endOffset());
        if (start >= end || fragment.endOffset() <= fragment.startOffset()) {
            return Optional.empty();
        }
        PdfPageRegion region = fragment.region();
        double width = region.xMaxPoints() - region.xMinPoints();
        double denominator = Math.max(1.0, fragment.endOffset() - fragment.startOffset());
        double xMin = region.xMinPoints() + width * (start - fragment.startOffset()) / denominator;
        double xMax = region.xMinPoints() + width * (end - fragment.startOffset()) / denominator;
        try {
            return Optional.of(new PdfPageRegion(region.pageNumber(), xMin, region.yMinPoints(), xMax,
                    region.yMaxPoints(), region.pageWidthPoints(), region.pageHeightPoints()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Optional<PdfPageRegion> union(List<PdfPageRegion> regions, PdfPageRegion fallback) {
        if (regions == null || regions.isEmpty()) {
            return Optional.empty();
        }
        double xMin = regions.stream().mapToDouble(PdfPageRegion::xMinPoints).min().orElse(fallback.xMinPoints());
        double yMin = regions.stream().mapToDouble(PdfPageRegion::yMinPoints).min().orElse(fallback.yMinPoints());
        double xMax = regions.stream().mapToDouble(PdfPageRegion::xMaxPoints).max().orElse(fallback.xMaxPoints());
        double yMax = regions.stream().mapToDouble(PdfPageRegion::yMaxPoints).max().orElse(fallback.yMaxPoints());
        try {
            return Optional.of(new PdfPageRegion(fallback.pageNumber(), xMin, yMin, xMax, yMax,
                    fallback.pageWidthPoints(), fallback.pageHeightPoints()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static boolean rangeCoversMost(DocumentTextRange range, String text) {
        int length = text == null ? 0 : text.length();
        return length <= 0 || range == null || range.length() >= Math.max(1, Math.round(length * 0.86f));
    }

    private static PdfTextLayerOrigin originFromBlock(DocumentBlock block) {
        if (block != null && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))) {
            return PdfTextLayerOrigin.OCR_LOCAL;
        }
        return PdfTextLayerOrigin.NATIVE_BBOX;
    }

    private static Optional<PdfPageRegion> regionFromBlock(DocumentBlock block) {
        if (block == null) {
            return Optional.empty();
        }
        Map<String, String> metadata = block.metadata();
        if (!"pdf-points".equalsIgnoreCase(metadata.getOrDefault("bboxUnits", ""))) {
            return Optional.empty();
        }
        Optional<Integer> page = parsePositiveInt(metadata.get("sourcePage"));
        Optional<double[]> bbox = parseBbox(metadata.get("bbox"));
        Optional<Double> width = parsePositiveDouble(metadata.get("pageWidth"));
        Optional<Double> height = parsePositiveDouble(metadata.get("pageHeight"));
        if (page.isEmpty() || bbox.isEmpty() || width.isEmpty() || height.isEmpty()) {
            return Optional.empty();
        }
        try {
            double[] parts = bbox.get();
            return Optional.of(new PdfPageRegion(page.get(), parts[0], parts[1], parts[2], parts[3],
                    width.get(), height.get()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static List<LineFragment> lineFragments(DocumentBlock block, PdfPageRegion fallback) {
        if (block == null || fallback == null) {
            return List.of();
        }
        String bboxes = block.metadata().getOrDefault("lineBboxes", "");
        String ranges = block.metadata().getOrDefault("lineCharRanges", "");
        if (bboxes.isBlank() || ranges.isBlank()) {
            return List.of();
        }
        String[] bboxParts = bboxes.split("\\s*;\\s*");
        String[] rangeParts = ranges.split("\\s*;\\s*");
        int count = Math.min(bboxParts.length, rangeParts.length);
        ArrayList<LineFragment> fragments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Optional<double[]> bbox = parseBbox(bboxParts[i]);
            Optional<int[]> range = parseRange(rangeParts[i]);
            if (bbox.isEmpty() || range.isEmpty()) {
                continue;
            }
            try {
                double[] parts = bbox.get();
                fragments.add(new LineFragment(new PdfPageRegion(
                        fallback.pageNumber(), parts[0], parts[1], parts[2], parts[3],
                        fallback.pageWidthPoints(), fallback.pageHeightPoints()),
                        range.get()[0], range.get()[1]));
            } catch (IllegalArgumentException ex) {
                // Ignore one bad auxiliary fragment; the block-level target remains available.
            }
        }
        return List.copyOf(fragments);
    }

    private static Optional<int[]> parseRange(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.strip().split("\\s*-\\s*");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            return end > start && start >= 0 ? Optional.of(new int[] { start, end }) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<double[]> parseBbox(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String[] parts = value.strip().split("\\s*,\\s*");
        if (parts.length != 4) {
            return Optional.empty();
        }
        double[] parsed = new double[4];
        for (int i = 0; i < parts.length; i++) {
            Optional<Double> number = parseDouble(parts[i]);
            if (number.isEmpty()) {
                return Optional.empty();
            }
            parsed[i] = number.get();
        }
        return Optional.of(parsed);
    }

    private static Optional<Integer> parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<Double> parsePositiveDouble(String value) {
        return parseDouble(value).filter(parsed -> parsed > 0.0);
    }

    private static Optional<Double> parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            double parsed = Double.parseDouble(value.strip());
            return Double.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private record LineFragment(PdfPageRegion region, int startOffset, int endOffset) {
        private LineFragment {
            startOffset = Math.max(0, startOffset);
            endOffset = Math.max(startOffset, endOffset);
        }
    }
}
