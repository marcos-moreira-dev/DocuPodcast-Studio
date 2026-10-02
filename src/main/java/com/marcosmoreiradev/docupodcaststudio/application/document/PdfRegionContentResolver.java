package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfContentRoute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves literal content inside an already ordered layout region. Native
 * PDF text is preferred when it is usable; an unsafe literal result promotes
 * that same region id to VLM_MIXED. This stage never creates or reorders ids.
 */
public final class PdfRegionContentResolver {
    private final PdfOcrEngine ocrEngine;

    public PdfRegionContentResolver() {
        this(null);
    }

    public PdfRegionContentResolver(PdfOcrEngine ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public Resolution resolve(PdfSemanticPageAnalysis layout,
                              PdfTextLayer nativeLayer) {
        return resolve(layout, nativeLayer, null, 0.0, 0.0, null);
    }

    public Resolution resolve(PdfSemanticPageAnalysis layout,
                              PdfTextLayer nativeLayer,
                              PreparedPdfWorkspaceRef workspace,
                              double pageWidth,
                              double pageHeight,
                              java.nio.file.Path cacheDirectory) {
        return resolve(layout, nativeLayer, workspace, pageWidth, pageHeight,
                cacheDirectory, layout == null ? 1 : layout.reportedPageNumber());
    }

    public Resolution resolve(PdfSemanticPageAnalysis layout,
                              PdfTextLayer nativeLayer,
                              PreparedPdfWorkspaceRef workspace,
                              double pageWidth,
                              double pageHeight,
                              java.nio.file.Path cacheDirectory,
                              int pageNumber) {
        PdfTextGeometryMap compatibilityGeometry = new PdfTextGeometryMap(
                pageNumber, nativeLayer, List.of(), "NATIVE_TEXT_LINES", List.of());
        return resolve(layout, nativeLayer, compatibilityGeometry, workspace,
                pageWidth, pageHeight, cacheDirectory, pageNumber);
    }

    public Resolution resolve(PdfSemanticPageAnalysis layout,
                              PdfTextLayer nativeLayer,
                              PdfTextGeometryMap textGeometry,
                              PreparedPdfWorkspaceRef workspace,
                              double pageWidth,
                              double pageHeight,
                              java.nio.file.Path cacheDirectory,
                              int pageNumber) {
        if (layout == null) return new Resolution(null, 0, 0);
        ArrayList<PdfSemanticPageAnalysis.Element> resolved = new ArrayList<>();
        int literal = 0;
        int promoted = 0;
        for (PdfSemanticPageAnalysis.Element region : layout.elements()) {
            boolean container = "CONTAINER".equals(
                    region.attributes().get("regionRole"));
            boolean playback = Boolean.parseBoolean(region.attributes()
                    .getOrDefault("playbackTarget", "true"));
            if (container || !playback) {
                resolved.add(region);
                continue;
            }
            List<PdfTextLine> crop = linesInside(nativeLayer, region.box());
            List<PdfTextLine> geometryCrop = textGeometry != null
                    ? linesInside(textGeometry.textLayer(), region.box()) : List.of();
            LineGroundingSelection grounding = supportsLineGrounding(region)
                    ? matchingLines(region, geometryCrop, layout.elements())
                    : LineGroundingSelection.unsupported();
            List<PdfTextLine> grounded = grounding.lines();
            List<PdfTextLine> nativeGrounded = supportsLineGrounding(region)
                    ? matchingLines(region, crop, layout.elements()).lines()
                    : List.of();
            LinkedHashMap<String, String> attributes =
                    new LinkedHashMap<>(region.attributes());
            if (!grounded.isEmpty()) {
                putTextGeometry(attributes, grounded, textGeometry == null
                        ? "OCR_RASTER_PAGE" : textGeometry.origin());
                attributes.put("playbackGeometryReason", grounding.reason());
                attributes.put("playbackGeometryFallback", "false");
            } else if (supportsLineGrounding(region)) {
                attributes.put("playbackGeometryReason", grounding.reason());
                attributes.put("playbackGeometryFallback", "true");
            }
            if (!"OCR_SAFE".equals(attributes.get("contentRoute"))) {
                resolved.add(copy(region, region.sourceText(), attributes));
                continue;
            }
            // Geometry and content authority are independent. Native PDF text
            // remains the first literal source; raster OCR may supply literal
            // OCR_SAFE text only when the native crop is unusable.
            List<PdfTextLine> literalLines = nativeGrounded.isEmpty()
                    ? crop : nativeGrounded;
            String text = literalLines.stream().map(PdfTextLine::text)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(java.util.stream.Collectors.joining("\n")).strip();
            if (usable(text, literalLines)) {
                attributes.put("contentResolver", "NATIVE_TEXT_CROP");
                attributes.put("contentCallSaved", "true");
                putLineGeometry(attributes, literalLines);
                literal++;
                resolved.add(copy(region, text, attributes));
            } else {
                if (usable(geometryCrop.stream().map(PdfTextLine::text)
                                .collect(java.util.stream.Collectors.joining("\n")),
                        geometryCrop)) {
                    String ocrText = geometryCrop.stream().map(PdfTextLine::text)
                            .filter(value -> value != null && !value.isBlank())
                            .collect(java.util.stream.Collectors.joining("\n")).strip();
                    attributes.put("contentResolver", "TESSERACT_PAGE_MAP");
                    attributes.put("contentCallSaved", "true");
                    putLineGeometry(attributes, geometryCrop);
                    literal++;
                    resolved.add(copy(region, ocrText, attributes));
                    continue;
                }
                OcrCropResolution ocr = resolveOcrCrop(region, workspace, pageWidth,
                        pageHeight, cacheDirectory, pageNumber);
                if (!ocr.text().isBlank()) {
                    attributes.put("contentResolver", "TESSERACT_CROP");
                    attributes.put("contentCallSaved", "true");
                    putPlaybackGeometry(attributes, ocr.lines(), "OCR_CROP_LINES");
                    putLineGeometry(attributes, ocr.lines());
                    literal++;
                    resolved.add(copy(region, ocr.text(), attributes));
                    continue;
                }
                attributes.put("contentRoute", PdfContentRoute.VLM_MIXED.name());
                attributes.put("contentResolver", "VLM_FALLBACK");
                attributes.put("literalFallbackReason", crop.isEmpty()
                        ? "literal-crop-empty" : "literal-crop-unusable");
                promoted++;
                resolved.add(copy(region, region.sourceText(), attributes));
            }
        }
        return new Resolution(new PdfSemanticPageAnalysis(
                layout.reportedPageNumber(), layout.language(), layout.pageRole(),
                resolved, layout.confidence(), layout.uncertainties()), literal,
                promoted);
    }

    private OcrCropResolution resolveOcrCrop(PdfSemanticPageAnalysis.Element region,
                                  PreparedPdfWorkspaceRef workspace,
                                  double pageWidth, double pageHeight,
                                  java.nio.file.Path cacheDirectory,
                                  int pageNumber) {
        if (ocrEngine == null || workspace == null || pageWidth <= 0.0
                || pageHeight <= 0.0) return OcrCropResolution.empty();
        PdfSemanticPageAnalysis.NormalizedBox box = region.box();
        PdfPageRegion crop = new PdfPageRegion(
                Math.max(1, pageNumber),
                box.xMin() / 1000.0 * pageWidth,
                box.yMin() / 1000.0 * pageHeight,
                box.xMax() / 1000.0 * pageWidth,
                box.yMax() / 1000.0 * pageHeight,
                pageWidth, pageHeight);
        try {
            PdfOcrPageResult result = ocrEngine.recognize(new PdfOcrRequest(
                    workspace.sourcePath(), crop.pageNumber(), 300,
                    PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                    PdfOcrRequest.DEFAULT_LANGUAGES, true, cacheDirectory,
                    6, 0, crop));
            List<PdfTextLine> remapped = result.lines().stream()
                    .map(PdfOcrLine::toTextLine)
                    .map(line -> remapCropLine(line, crop)).toList();
            String text = remapped.stream().map(PdfTextLine::text)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(java.util.stream.Collectors.joining("\n")).strip();
            double confidence = remapped.stream()
                    .mapToDouble(PdfTextLine::confidence).average().orElse(0.0);
            return usable(text, remapped) && confidence >= 0.55
                    ? new OcrCropResolution(text, remapped)
                    : OcrCropResolution.empty();
        } catch (PdfOcrException | RuntimeException ignored) {
            return OcrCropResolution.empty();
        }
    }

    private static PdfTextLine remapCropLine(PdfTextLine line, PdfPageRegion crop) {
        PdfPageRegion local = line.region();
        double cropWidth = crop.xMaxPoints() - crop.xMinPoints();
        double cropHeight = crop.yMaxPoints() - crop.yMinPoints();
        PdfPageRegion remapped = new PdfPageRegion(crop.pageNumber(),
                crop.xMinPoints() + local.xMinPoints() / local.pageWidthPoints() * cropWidth,
                crop.yMinPoints() + local.yMinPoints() / local.pageHeightPoints() * cropHeight,
                crop.xMinPoints() + local.xMaxPoints() / local.pageWidthPoints() * cropWidth,
                crop.yMinPoints() + local.yMaxPoints() / local.pageHeightPoints() * cropHeight,
                crop.pageWidthPoints(), crop.pageHeightPoints());
        return new PdfTextLine(crop.pageNumber(), line.text(), remapped,
                line.tokens(), line.confidence(), line.dominantColor());
    }

    private static LineGroundingSelection matchingLines(
            PdfSemanticPageAnalysis.Element owner,
            List<PdfTextLine> candidates,
            List<PdfSemanticPageAnalysis.Element> regions) {
        if (candidates.isEmpty()) {
            return new LineGroundingSelection(List.of(), "semantic-fallback:no-native-lines");
        }
        List<PdfTextLine> textualMatches = candidates.stream()
                .filter(line -> sufficientlyMatches(owner.sourceText(), line.text()))
                .toList();
        if (textualMatches.isEmpty()) {
            return new LineGroundingSelection(List.of(), "semantic-fallback:no-confident-text-match");
        }
        List<PdfTextLine> unambiguous = textualMatches.stream()
                .filter(line -> !claimedBySibling(owner, line, regions))
                .toList();
        if (unambiguous.isEmpty()) {
            return new LineGroundingSelection(List.of(), "semantic-fallback:ambiguous-neighbour-match");
        }
        String reason = unambiguous.size() == 1
                ? "single-confident-line-within-semantic-region"
                : "confident-lines-within-semantic-region";
        return new LineGroundingSelection(unambiguous, reason);
    }

    private static boolean claimedBySibling(
            PdfSemanticPageAnalysis.Element owner,
            PdfTextLine line,
            List<PdfSemanticPageAnalysis.Element> regions) {
        if (regions == null || regions.isEmpty()) return false;
        double x = (line.region().xMinPoints() + line.region().xMaxPoints()) / 2.0
                / line.region().pageWidthPoints() * 1000.0;
        double y = (line.region().yMinPoints() + line.region().yMaxPoints()) / 2.0
                / line.region().pageHeightPoints() * 1000.0;
        return regions.stream()
                .filter(sibling -> sibling != owner)
                .filter(sibling -> !"CONTAINER".equals(sibling.attributes().get("regionRole")))
                .filter(sibling -> Boolean.parseBoolean(sibling.attributes()
                        .getOrDefault("playbackTarget", "true")))
                .filter(sibling -> contains(sibling.box(), x, y))
                .anyMatch(sibling -> sufficientlyMatches(sibling.sourceText(), line.text()));
    }

    private static boolean sufficientlyMatches(String source, String line) {
        String normalizedSource = normalize(source);
        String normalizedLine = normalize(line);
        if (normalizedSource.isBlank() || normalizedLine.isBlank()) return false;
        Set<String> lineTokens = tokens(normalizedLine);
        if (lineTokens.isEmpty() && !normalizedSource.equals(normalizedLine)) {
            // A lone variable/character (for example "x") is ubiquitous in
            // technical PDFs and cannot establish line identity by itself.
            return false;
        }
        if (normalizedSource.contains(normalizedLine)
                || compact(normalizedSource).contains(compact(normalizedLine))) return true;
        Set<String> sourceTokens = tokens(normalizedSource);
        if (lineTokens.isEmpty()) return false;
        long common = lineTokens.stream().filter(sourceTokens::contains).count();
        int required = lineTokens.size() <= 2 ? lineTokens.size()
                : Math.max(2, (int) Math.ceil(lineTokens.size() * 0.67));
        return common >= required;
    }

    private static boolean contains(PdfSemanticPageAnalysis.NormalizedBox box,
                                    double x, double y) {
        return box != null && x >= box.xMin() && x <= box.xMax()
                && y >= box.yMin() && y <= box.yMax();
    }

    private static boolean supportsLineGrounding(PdfSemanticPageAnalysis.Element region) {
        return switch (region.type()) {
            case TITLE, HEADING, SUBHEADING, PARAGRAPH, LIST, SIDEBAR,
                    CAPTION, HEADER, FOOTER, PAGE_NUMBER -> true;
            case TABLE, MATH, IMAGE, CODE, UNKNOWN -> false;
        };
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    private static String compact(String value) {
        return value.replace(" ", "");
    }

    private static Set<String> tokens(String value) {
        return java.util.Arrays.stream(value.split("\\s+"))
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private record LineGroundingSelection(List<PdfTextLine> lines, String reason) {
        private LineGroundingSelection {
            lines = lines == null ? List.of() : List.copyOf(lines);
            reason = reason == null ? "" : reason;
        }

        static LineGroundingSelection unsupported() {
            return new LineGroundingSelection(List.of(), "semantic-object-geometry");
        }
    }

    private static void putPlaybackGeometry(LinkedHashMap<String, String> attributes,
                                            List<PdfTextLine> lines, String origin) {
        attributes.put("playbackBboxes", boxes(lines));
        attributes.put("playbackGeometryOrigin", origin);
        attributes.put("playbackGeometryConfidence", String.format(Locale.ROOT, "%.3f",
                lines.stream().mapToDouble(PdfTextLine::confidence).average().orElse(0.0)));
    }

    private static void putTextGeometry(LinkedHashMap<String, String> attributes,
                                        List<PdfTextLine> lines, String origin) {
        String encoded = boxes(lines);
        List<PdfTextToken> words = lines.stream().flatMap(line -> line.tokens().stream())
                .filter(token -> token.region() != null)
                .filter(token -> token.confidence() >= 0.55)
                .filter(token -> token.text() != null && !token.text().isBlank())
                .toList();
        attributes.put("textGeometryBboxes", encoded);
        attributes.put("textGeometryOrigin", origin);
        attributes.put("textGeometryText", lines.stream().map(PdfTextLine::text)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n")));
        if (!words.isEmpty()) {
            String wordBoxes = words.stream().map(token -> box(token.region()))
                    .collect(Collectors.joining(";"));
            attributes.put(PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                    wordBoxes);
            attributes.put(PdfTextVisualBoundsResolver.OCR_WORD_COUNT_ATTRIBUTE,
                    Integer.toString(words.size()));
            PdfPageRegion tight = PdfTextVisualBoundsResolver.union(words.stream()
                    .map(PdfTextToken::region).toList());
            attributes.put(PdfTextVisualBoundsResolver.TIGHT_BBOX_ATTRIBUTE,
                    box(tight));
            attributes.put(PdfTextVisualBoundsResolver.SOURCE_ATTRIBUTE,
                    PdfTextVisualBoundsResolver.Source.OCR_WORDS.name());
            attributes.put("playbackBboxes", box(tight));
            attributes.put("playbackGeometryOrigin", origin);
            attributes.put("playbackGeometryConfidence", String.format(Locale.ROOT, "%.3f",
                    words.stream().mapToDouble(PdfTextToken::confidence)
                            .average().orElse(0.0)));
        } else {
            attributes.put(PdfTextVisualBoundsResolver.SOURCE_ATTRIBUTE,
                    PdfTextVisualBoundsResolver.Source.SEMANTIC_FALLBACK.name());
            putPlaybackGeometry(attributes, lines, origin);
        }
    }

    private static void putLineGeometry(LinkedHashMap<String, String> attributes,
                                        List<PdfTextLine> lines) {
        attributes.put("lineBboxes", boxes(lines));
        int offset = 0;
        ArrayList<String> ranges = new ArrayList<>();
        for (PdfTextLine line : lines) {
            int end = offset + line.text().length();
            ranges.add(offset + "-" + end);
            offset = end + 1;
        }
        attributes.put("lineCharRanges", String.join(";", ranges));
    }

    private static String boxes(List<PdfTextLine> lines) {
        return lines.stream().map(line -> box(line.region()))
                .collect(Collectors.joining(";"));
    }

    private static String box(PdfPageRegion box) {
        return format(box.xMinPoints()) + "," + format(box.yMinPoints()) + ","
                + format(box.xMaxPoints()) + "," + format(box.yMaxPoints());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static PdfSemanticPageAnalysis.Element copy(
            PdfSemanticPageAnalysis.Element value, String source,
            java.util.Map<String, String> attributes) {
        return new PdfSemanticPageAnalysis.Element(value.responseId(),
                value.readingOrder(), value.type(), value.box(), source,
                value.narrationText(), value.narratability(), value.confidence(),
                value.uncertainties(), attributes);
    }

    private static List<PdfTextLine> linesInside(
            PdfTextLayer layer, PdfSemanticPageAnalysis.NormalizedBox box) {
        if (layer == null || !layer.available()) return List.of();
        return layer.lines().stream().filter(line -> {
            PdfPageRegion value = line.region();
            double x = (value.xMinPoints() + value.xMaxPoints()) / 2.0
                    / value.pageWidthPoints() * 1000.0;
            double y = (value.yMinPoints() + value.yMaxPoints()) / 2.0
                    / value.pageHeightPoints() * 1000.0;
            return x >= box.xMin() && x <= box.xMax()
                    && y >= box.yMin() && y <= box.yMax();
        }).sorted(java.util.Comparator.comparingDouble(
                line -> line.region().yMinPoints())).toList();
    }

    private static boolean usable(String text, List<PdfTextLine> lines) {
        if (text == null || text.length() < 8 || lines.isEmpty()
                || text.indexOf('\uFFFD') >= 0) return false;
        long visible = text.codePoints().filter(ch -> Character.isLetterOrDigit(ch)
                || Character.isWhitespace(ch) || ".,;:!?()'-\"".indexOf(ch) >= 0).count();
        double confidence = lines.stream().mapToDouble(PdfTextLine::confidence)
                .average().orElse(0.0);
        return visible / (double) Math.max(1, text.codePoints().count()) >= 0.78
                && confidence >= 0.55;
    }

    public record Resolution(PdfSemanticPageAnalysis analysis,
                             int literalRegions,
                             int promotedToVlmRegions) { }

    private record OcrCropResolution(String text, List<PdfTextLine> lines) {
        private OcrCropResolution {
            text = text == null ? "" : text.strip();
            lines = lines == null ? List.of() : List.copyOf(lines);
        }

        static OcrCropResolution empty() {
            return new OcrCropResolution("", List.of());
        }
    }
}
