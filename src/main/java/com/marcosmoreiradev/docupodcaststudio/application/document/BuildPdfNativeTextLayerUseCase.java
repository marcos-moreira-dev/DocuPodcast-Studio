package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Builds the PDF text layer from imported bbox metadata, whether native or OCR-derived. */
public final class BuildPdfNativeTextLayerUseCase {
    private static final double NATIVE_LINE_CONFIDENCE = 0.90;
    private static final double APPROX_TOKEN_CONFIDENCE = 0.72;

    public List<PdfTextLayer> build(ReadableDocument document) {
        if (document == null || document.format() != SourceDocumentFormat.PDF) {
            return List.of();
        }
        Map<Integer, List<PdfTextLine>> linesByPage = new LinkedHashMap<>();
        Map<Integer, PdfTextLayerOrigin> originByPage = new LinkedHashMap<>();
        int pageCount = sourcePageCount(document);
        for (DocumentBlock block : document.blocks()) {
            Optional<PdfTextLine> line = lineFromBlock(block);
            if (line.isEmpty()) {
                continue;
            }
            pageCount = Math.max(pageCount, line.get().pageNumber());
            linesByPage.computeIfAbsent(line.get().pageNumber(), ignored -> new ArrayList<>()).add(line.get());
            originByPage.merge(line.get().pageNumber(), originFromBlock(block), BuildPdfNativeTextLayerUseCase::mergeOrigin);
        }
        if (pageCount <= 0) {
            return List.of();
        }
        List<PdfTextLayer> layers = new ArrayList<>();
        for (int page = 1; page <= pageCount; page++) {
            List<PdfTextLine> lines = linesByPage.getOrDefault(page, List.of()).stream()
                    .sorted(Comparator.comparingDouble(line -> line.region().yMinPoints()))
                    .toList();
            if (lines.isEmpty()) {
                layers.add(new PdfTextLayer(
                        page,
                        PdfTextLayerOrigin.UNAVAILABLE,
                        List.of(),
                        List.of("Pagina PDF sin texto nativo bbox disponible.")));
            } else {
                layers.add(new PdfTextLayer(page, originByPage.getOrDefault(page, PdfTextLayerOrigin.NATIVE_BBOX), lines, List.of()));
            }
        }
        return List.copyOf(layers);
    }

    private static PdfTextLayerOrigin originFromBlock(DocumentBlock block) {
        if (block != null && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))) {
            return PdfTextLayerOrigin.OCR_LOCAL;
        }
        return PdfTextLayerOrigin.NATIVE_BBOX;
    }

    private static PdfTextLayerOrigin mergeOrigin(PdfTextLayerOrigin first, PdfTextLayerOrigin second) {
        return first == PdfTextLayerOrigin.NATIVE_BBOX || second == PdfTextLayerOrigin.NATIVE_BBOX
                ? PdfTextLayerOrigin.NATIVE_BBOX
                : PdfTextLayerOrigin.OCR_LOCAL;
    }

    private static Optional<PdfTextLine> lineFromBlock(DocumentBlock block) {
        if (block == null || block.text().isBlank()) {
            return Optional.empty();
        }
        Map<String, String> metadata = block.metadata();
        if (!"pdf-points".equalsIgnoreCase(metadata.getOrDefault("bboxUnits", ""))) {
            return Optional.empty();
        }
        int page = parsePositiveInt(metadata.get("sourcePage")).orElse(0);
        if (page <= 0) {
            return Optional.empty();
        }
        Optional<PdfPageRegion> region = region(page, metadata.get("bbox"),
                metadata.get("pageWidth"), metadata.get("pageHeight"));
        if (region.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PdfTextLine(
                page,
                block.text(),
                region.get(),
                approximateTokens(block.text(), region.get()),
                confidence(metadata)));
    }

    private static Optional<PdfPageRegion> region(int page, String bbox, String pageWidth, String pageHeight) {
        double[] parts = parseBbox(bbox).orElse(null);
        double width = parsePositiveDouble(pageWidth).orElse(0.0);
        double height = parsePositiveDouble(pageHeight).orElse(0.0);
        if (parts == null || width <= 0.0 || height <= 0.0) {
            return Optional.empty();
        }
        try {
            return Optional.of(new PdfPageRegion(page, parts[0], parts[1], parts[2], parts[3], width, height));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static List<PdfTextToken> approximateTokens(String text, PdfPageRegion lineRegion) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] rawTokens = normalized.split("\\s+");
        List<String> tokens = java.util.Arrays.stream(rawTokens)
                .filter(token -> token != null && !token.isBlank())
                .toList();
        if (tokens.isEmpty()) {
            return List.of();
        }
        double width = Math.max(0.001, lineRegion.xMaxPoints() - lineRegion.xMinPoints());
        List<PdfTextToken> result = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            double xMin = lineRegion.xMinPoints() + width * i / tokens.size();
            double xMax = i == tokens.size() - 1
                    ? lineRegion.xMaxPoints()
                    : lineRegion.xMinPoints() + width * (i + 1) / tokens.size();
            result.add(new PdfTextToken(token, new PdfPageRegion(
                    lineRegion.pageNumber(),
                    xMin,
                    lineRegion.yMinPoints(),
                    xMax,
                    lineRegion.yMaxPoints(),
                    lineRegion.pageWidthPoints(),
                    lineRegion.pageHeightPoints()), APPROX_TOKEN_CONFIDENCE));
        }
        return List.copyOf(result);
    }

    private static double confidence(Map<String, String> metadata) {
        if (Boolean.parseBoolean(metadata.getOrDefault("ocr", "false"))) {
            return parsePositiveDouble(metadata.get("ocrConfidence")).map(value -> Math.min(1.0, value)).orElse(0.82);
        }
        Optional<Double> numeric = parsePositiveDouble(metadata.get("confidence"));
        if (numeric.isPresent()) {
            return Math.min(1.0, numeric.get());
        }
        String mode = metadata.getOrDefault("extractionMode", "").toLowerCase(Locale.ROOT);
        return mode.contains("bbox") ? NATIVE_LINE_CONFIDENCE : 0.80;
    }

    private static int sourcePageCount(ReadableDocument document) {
        int max = 0;
        for (DocumentBlock block : document.blocks()) {
            max = Math.max(max, parsePositiveInt(block.metadata().get("sourcePageCount")).orElse(0));
            max = Math.max(max, parsePositiveInt(block.metadata().get("sourcePage")).orElse(0));
        }
        return max;
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
}
