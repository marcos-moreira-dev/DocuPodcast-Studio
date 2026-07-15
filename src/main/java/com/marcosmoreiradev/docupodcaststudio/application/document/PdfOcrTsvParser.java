package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Parses Tesseract TSV output into PDF point coordinates. */
public final class PdfOcrTsvParser {
    public PdfOcrPageResult parse(String tsv,
                                  int pageNumber,
                                  int dpi,
                                  int imageWidthPixels,
                                  int imageHeightPixels,
                                  double pageWidthPoints,
                                  double pageHeightPoints) {
        if (tsv == null || tsv.isBlank() || imageWidthPixels <= 0 || imageHeightPixels <= 0
                || pageWidthPoints <= 0.0 || pageHeightPoints <= 0.0) {
            PdfTextLayer unavailable = new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE,
                    List.of(), List.of("OCR TSV vacio o dimensiones invalidas."));
            return new PdfOcrPageResult(pageNumber, dpi, imageWidthPixels, imageHeightPixels,
                    pageWidthPoints, pageHeightPoints, List.of(), List.of(), unavailable, unavailable.warnings());
        }
        String[] lines = tsv.split("\\R");
        if (lines.length <= 1) {
            PdfTextLayer unavailable = new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE,
                    List.of(), List.of("OCR TSV sin filas de palabras."));
            return new PdfOcrPageResult(pageNumber, dpi, imageWidthPixels, imageHeightPixels,
                    pageWidthPoints, pageHeightPoints, List.of(), List.of(), unavailable, unavailable.warnings());
        }
        String[] header = lines[0].split("\\t", -1);
        Map<String, Integer> columns = columns(header);
        List<PdfOcrWord> words = new ArrayList<>();
        Map<String, List<PdfOcrWord>> wordsByLine = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            Optional<ParsedWord> parsed = parseWord(lines[i], columns, pageNumber, imageWidthPixels, imageHeightPixels,
                    pageWidthPoints, pageHeightPoints);
            if (parsed.isEmpty()) {
                continue;
            }
            words.add(parsed.get().word());
            wordsByLine.computeIfAbsent(parsed.get().lineKey(), ignored -> new ArrayList<>()).add(parsed.get().word());
        }
        List<PdfOcrLine> ocrLines = new ArrayList<>();
        for (List<PdfOcrWord> lineWords : wordsByLine.values()) {
            lineFromWords(pageNumber, pageWidthPoints, pageHeightPoints, lineWords).ifPresent(ocrLines::add);
        }
        PdfTextLayer textLayer = ocrLines.isEmpty()
                ? new PdfTextLayer(pageNumber, PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of("OCR no detecto texto narrable."))
                : new PdfTextLayer(pageNumber, PdfTextLayerOrigin.OCR_LOCAL,
                ocrLines.stream().map(PdfOcrLine::toTextLine).toList(), List.of());
        return new PdfOcrPageResult(pageNumber, dpi, imageWidthPixels, imageHeightPixels,
                pageWidthPoints, pageHeightPoints, ocrLines, words, textLayer, textLayer.warnings());
    }

    private static Map<String, Integer> columns(String[] header) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            result.put(header[i].strip().toLowerCase(Locale.ROOT), i);
        }
        return result;
    }

    private static Optional<ParsedWord> parseWord(String row,
                                                  Map<String, Integer> columns,
                                                  int pageNumber,
                                                  int imageWidthPixels,
                                                  int imageHeightPixels,
                                                  double pageWidthPoints,
                                                  double pageHeightPoints) {
        String[] fields = row.split("\\t", -1);
        int level = intField(fields, columns, "level").orElse(-1);
        if (level != 5) {
            return Optional.empty();
        }
        String text = stringField(fields, columns, "text");
        if (text.isBlank()) {
            return Optional.empty();
        }
        double confidence = doubleField(fields, columns, "conf").orElse(-1.0);
        if (confidence < 0.0) {
            return Optional.empty();
        }
        int left = intField(fields, columns, "left").orElse(0);
        int top = intField(fields, columns, "top").orElse(0);
        int width = intField(fields, columns, "width").orElse(0);
        int height = intField(fields, columns, "height").orElse(0);
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        double xMin = pageWidthPoints * clamp(left, 0, imageWidthPixels) / imageWidthPixels;
        double xMax = pageWidthPoints * clamp(left + width, 0, imageWidthPixels) / imageWidthPixels;
        double yMin = pageHeightPoints * clamp(top, 0, imageHeightPixels) / imageHeightPixels;
        double yMax = pageHeightPoints * clamp(top + height, 0, imageHeightPixels) / imageHeightPixels;
        PdfOcrWord word = new PdfOcrWord(text, new PdfPageRegion(
                pageNumber, xMin, yMin, xMax, yMax, pageWidthPoints, pageHeightPoints), confidence / 100.0);
        String key = intField(fields, columns, "block_num").orElse(0) + ":"
                + intField(fields, columns, "par_num").orElse(0) + ":"
                + intField(fields, columns, "line_num").orElse(0);
        return Optional.of(new ParsedWord(key, word));
    }

    private static Optional<PdfOcrLine> lineFromWords(int pageNumber,
                                                      double pageWidthPoints,
                                                      double pageHeightPoints,
                                                      List<PdfOcrWord> words) {
        if (words == null || words.isEmpty()) {
            return Optional.empty();
        }
        double xMin = words.stream().mapToDouble(word -> word.region().xMinPoints()).min().orElse(0.0);
        double yMin = words.stream().mapToDouble(word -> word.region().yMinPoints()).min().orElse(0.0);
        double xMax = words.stream().mapToDouble(word -> word.region().xMaxPoints()).max().orElse(0.0);
        double yMax = words.stream().mapToDouble(word -> word.region().yMaxPoints()).max().orElse(0.0);
        String text = String.join(" ", words.stream().map(PdfOcrWord::text).toList()).strip();
        double confidence = words.stream().mapToDouble(PdfOcrWord::confidence).average().orElse(0.0);
        if (text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new PdfOcrLine(pageNumber, text,
                new PdfPageRegion(pageNumber, xMin, yMin, xMax, yMax, pageWidthPoints, pageHeightPoints),
                words, confidence));
    }

    private static Optional<Integer> intField(String[] fields, Map<String, Integer> columns, String name) {
        try {
            int index = columns.getOrDefault(name, -1);
            return index >= 0 && index < fields.length ? Optional.of(Integer.parseInt(fields[index].strip())) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<Double> doubleField(String[] fields, Map<String, Integer> columns, String name) {
        try {
            int index = columns.getOrDefault(name, -1);
            return index >= 0 && index < fields.length ? Optional.of(Double.parseDouble(fields[index].strip())) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static String stringField(String[] fields, Map<String, Integer> columns, String name) {
        int index = columns.getOrDefault(name, -1);
        return index >= 0 && index < fields.length ? fields[index].strip() : "";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ParsedWord(String lineKey, PdfOcrWord word) {
    }
}
