package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Searches OCR-only PDF text layers and returns visual hit regions. */
public final class SearchPdfTextUseCase {
    private final BuildPdfResolvedTextLayerUseCase resolvedTextLayer;

    public SearchPdfTextUseCase(BuildPdfResolvedTextLayerUseCase resolvedTextLayer) {
        this.resolvedTextLayer = resolvedTextLayer;
    }

    public PdfTextSearchProjection search(PdfTextSearchRequest request) {
        ReadableDocument document = request == null ? null : request.document();
        String query = request == null ? "" : request.query();
        if (document == null || document.format() != SourceDocumentFormat.PDF || query.isBlank()) {
            return new PdfTextSearchProjection(query, List.of(), 0, List.of());
        }
        List<PdfTextLayer> nativeLayers = new BuildPdfNativeTextLayerUseCase().build(ocrOnlyDocument(document));
        List<Integer> targetPages = request.includeOcr()
                ? nativeLayers.stream()
                .filter(layer -> !layer.available())
                .map(PdfTextLayer::pageNumber)
                .limit(request.maxOcrPages())
                .toList()
                : List.of();
        PdfResolvedTextLayerProjection resolved = resolvedTextLayer == null
                ? new PdfResolvedTextLayerProjection(nativeLayers, List.of(), List.of())
                : resolvedTextLayer.resolve(new PdfResolvedTextLayerRequest(
                document,
                targetPages,
                request.includeOcr() ? PdfTextResolutionPolicy.OCR_WHEN_UNAVAILABLE : PdfTextResolutionPolicy.NATIVE_ONLY,
                request.cacheDirectory(),
                PdfOcrRequest.DEFAULT_DPI,
                PdfOcrRequest.DEFAULT_LANGUAGES));
        Map<String, String> blockByBbox = blockByBbox(document);
        String needle = normalize(query);
        List<PdfTextSearchResult> results = new ArrayList<>();
        int ordinal = 0;
        for (PdfTextLayer layer : resolved.layers()) {
            if (!layer.available()) {
                continue;
            }
            for (PdfTextLine line : layer.lines()) {
                String haystack = normalize(line.text());
                if (!haystack.contains(needle)) {
                    continue;
                }
                String blockId = blockIdForLine(document, layer.pageNumber(), line, needle, blockByBbox);
                results.add(new PdfTextSearchResult(
                        "pdf-search-" + (++ordinal),
                        layer.pageNumber(),
                        blockId,
                        snippet(line.text(), query),
                        line.region(),
                        layer.origin(),
                        line.confidence()));
                if (results.size() >= request.maxResults()) {
                    return new PdfTextSearchProjection(query, results, resolved.layers().size(), resolved.warnings());
                }
            }
        }
        return new PdfTextSearchProjection(query, results.stream()
                .sorted(Comparator.comparingInt(PdfTextSearchResult::pageNumber))
                .toList(), resolved.layers().size(), resolved.warnings());
    }

    private static String blockIdForLine(ReadableDocument document,
                                         int pageNumber,
                                         PdfTextLine line,
                                         String normalizedQuery,
                                         Map<String, String> blockByBbox) {
        String exact = blockByBbox.getOrDefault(pageNumber + "|" + line.region().bbox(), "");
        if (!exact.isBlank()) {
            return exact;
        }
        double centerX = (line.region().xMinPoints() + line.region().xMaxPoints()) / 2.0;
        double centerY = (line.region().yMinPoints() + line.region().yMaxPoints()) / 2.0;
        String fallback = "";
        for (DocumentBlock block : document.blocks()) {
            if (!isOcrBlock(block)) {
                continue;
            }
            if (pageNumber != sourcePage(block) || !normalize(block.text()).contains(normalizedQuery)) {
                continue;
            }
            Optional<PdfPageRegion> region = regionFromBlock(block);
            if (region.isPresent() && (contains(region.get(), centerX, centerY) || intersects(region.get(), line.region()))) {
                return block.id();
            }
            if (fallback.isBlank()) {
                fallback = block.id();
            }
        }
        return fallback;
    }

    private static Map<String, String> blockByBbox(ReadableDocument document) {
        Map<String, String> map = new LinkedHashMap<>();
        for (DocumentBlock block : document.blocks()) {
            if (!isOcrBlock(block)) {
                continue;
            }
            String page = block.metadata().getOrDefault("sourcePage", "").strip();
            String bbox = block.metadata().getOrDefault("bbox", "").strip();
            if (!page.isBlank() && !bbox.isBlank()) {
                map.put(page + "|" + bbox, block.id());
            }
        }
        return map;
    }

    private static int sourcePage(DocumentBlock block) {
        if (block == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(block.metadata().getOrDefault("sourcePage", "0").strip()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Optional<PdfPageRegion> regionFromBlock(DocumentBlock block) {
        if (block == null || !"pdf-points".equalsIgnoreCase(block.metadata().getOrDefault("bboxUnits", ""))) {
            return Optional.empty();
        }
        double[] bbox = parseBbox(block.metadata().get("bbox")).orElse(null);
        double width = parseDouble(block.metadata().get("pageWidth")).orElse(0.0);
        double height = parseDouble(block.metadata().get("pageHeight")).orElse(0.0);
        if (bbox == null || width <= 0.0 || height <= 0.0) {
            return Optional.empty();
        }
        try {
            return Optional.of(new PdfPageRegion(sourcePage(block), bbox[0], bbox[1], bbox[2], bbox[3], width, height));
        } catch (IllegalArgumentException ex) {
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

    private static boolean contains(PdfPageRegion region, double x, double y) {
        return x >= region.xMinPoints() && x <= region.xMaxPoints()
                && y >= region.yMinPoints() && y <= region.yMaxPoints();
    }

    private static boolean intersects(PdfPageRegion a, PdfPageRegion b) {
        return a.pageNumber() == b.pageNumber()
                && a.xMinPoints() <= b.xMaxPoints() && a.xMaxPoints() >= b.xMinPoints()
                && a.yMinPoints() <= b.yMaxPoints() && a.yMaxPoints() >= b.yMinPoints();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static String snippet(String text, String query) {
        String safe = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        String normalized = normalize(safe);
        String needle = normalize(query);
        int index = normalized.indexOf(needle);
        if (index < 0) {
            return safe.length() > 140 ? safe.substring(0, 140).strip() + "..." : safe;
        }
        int start = Math.max(0, index - 48);
        int end = Math.min(safe.length(), index + needle.length() + 72);
        String prefix = start > 0 ? "... " : "";
        String suffix = end < safe.length() ? " ..." : "";
        return prefix + safe.substring(start, end).strip() + suffix;
    }

    private static ReadableDocument ocrOnlyDocument(ReadableDocument document) {
        return document.withBlocks(document.blocks().stream()
                .filter(block -> isOcrBlock(block) || isVisualFallbackBlock(block) || !block.metadata().containsKey("sourcePage"))
                .toList());
    }

    private static boolean isOcrBlock(DocumentBlock block) {
        return block != null
                && "true".equalsIgnoreCase(block.metadata().getOrDefault("ocr", "false"))
                && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""));
    }

    private static boolean isVisualFallbackBlock(DocumentBlock block) {
        return block != null
                && ("visual-fallback".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""))
                || "true".equalsIgnoreCase(block.metadata().getOrDefault("visualBlock", "false")));
    }
}
