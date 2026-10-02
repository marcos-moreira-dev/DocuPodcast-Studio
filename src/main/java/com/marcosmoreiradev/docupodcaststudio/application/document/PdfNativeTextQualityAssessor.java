package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.ArrayList;
import java.util.List;

/** Conservative quality gate deciding whether native text can bypass OCR. */
public final class PdfNativeTextQualityAssessor {
    public PdfNativeTextQualityReport assess(PdfTextLayer layer) {
        if (layer == null || !layer.available() || layer.lines().isEmpty()) {
            return new PdfNativeTextQualityReport(PdfNativeTextQuality.UNUSABLE, 0.0,
                    List.of("native-text-unavailable"));
        }
        String text = layer.lines().stream().map(PdfTextLine::text)
                .collect(java.util.stream.Collectors.joining(" ")).strip();
        if (text.isBlank()) {
            return new PdfNativeTextQualityReport(PdfNativeTextQuality.UNUSABLE, 0.0,
                    List.of("native-text-empty"));
        }
        ArrayList<String> reasons = new ArrayList<>();
        long characters = text.codePoints().count();
        long invalid = text.codePoints().filter(ch -> ch == 0xFFFD
                || (Character.isISOControl(ch) && !Character.isWhitespace(ch))).count();
        List<String> words = java.util.Arrays.stream(text.split("\\s+"))
                .filter(token -> !token.isBlank()).toList();
        long singleLetters = words.stream().filter(token -> token.length() == 1
                && Character.isLetter(token.charAt(0))).count();
        double invalidRatio = invalid / (double) Math.max(1L, characters);
        double fragmentedRatio = singleLetters / (double) Math.max(1, words.size());
        double overlapRatio = overlapRatio(layer.lines());
        if (characters < 40 || words.size() < 8) reasons.add("native-text-too-short");
        if (invalidRatio > 0.01) reasons.add("invalid-characters");
        if (fragmentedRatio > 0.25) reasons.add("letter-fragmentation");
        if (overlapRatio > 0.20) reasons.add("overlapping-geometry");
        double score = 1.0
                - Math.min(0.45, invalidRatio * 8.0)
                - Math.min(0.35, fragmentedRatio)
                - Math.min(0.25, overlapRatio);
        List<PdfNativeRegionQualityReport> regional = regional(layer.lines());
        if (characters < 20 || words.size() < 4 || invalidRatio > 0.08) {
            return new PdfNativeTextQualityReport(PdfNativeTextQuality.UNUSABLE, score, reasons, regional);
        }
        PdfNativeTextQuality quality = reasons.isEmpty() && score >= 0.82
                ? PdfNativeTextQuality.RELIABLE : PdfNativeTextQuality.SUSPECT;
        return new PdfNativeTextQualityReport(quality, score, reasons, regional);
    }

    private static List<PdfNativeRegionQualityReport> regional(List<PdfTextLine> lines) {
        ArrayList<PdfNativeRegionQualityReport> result = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String text = lines.get(index).text() == null ? "" : lines.get(index).text().strip();
            ArrayList<String> reasons = new ArrayList<>();
            long characters = text.codePoints().count();
            long invalid = text.codePoints().filter(ch -> ch == 0xFFFD
                    || (Character.isISOControl(ch) && !Character.isWhitespace(ch))).count();
            List<String> words = java.util.Arrays.stream(text.split("\\s+"))
                    .filter(token -> !token.isBlank()).toList();
            long singles = words.stream().filter(token -> token.length() == 1
                    && Character.isLetter(token.charAt(0))).count();
            double invalidRatio = invalid / (double) Math.max(1L, characters);
            double fragmented = singles / (double) Math.max(1, words.size());
            if (characters < 4) reasons.add("region-too-short");
            if (invalidRatio > 0.01) reasons.add("region-invalid-characters");
            if (fragmented > 0.35 && words.size() >= 4) reasons.add("region-letter-fragmentation");
            double score = 1.0 - Math.min(0.65, invalidRatio * 8.0)
                    - Math.min(0.45, fragmented);
            PdfNativeTextQuality quality = characters < 2 || invalidRatio > 0.08
                    ? PdfNativeTextQuality.UNUSABLE
                    : reasons.isEmpty() && score >= 0.78
                    ? PdfNativeTextQuality.RELIABLE : PdfNativeTextQuality.SUSPECT;
            result.add(new PdfNativeRegionQualityReport(index, quality, score, reasons));
        }
        return List.copyOf(result);
    }

    private static double overlapRatio(List<PdfTextLine> lines) {
        if (lines.size() < 2) return 0.0;
        int overlaps = 0;
        int pairs = 0;
        for (int i = 0; i < lines.size(); i++) {
            for (int j = i + 1; j < lines.size(); j++) {
                pairs++;
                PdfPageRegion a = lines.get(i).region();
                PdfPageRegion b = lines.get(j).region();
                double area = Math.max(0.0, Math.min(a.xMaxPoints(), b.xMaxPoints())
                        - Math.max(a.xMinPoints(), b.xMinPoints()))
                        * Math.max(0.0, Math.min(a.yMaxPoints(), b.yMaxPoints())
                        - Math.max(a.yMinPoints(), b.yMinPoints()));
                if (area > 1.0) overlaps++;
            }
        }
        return overlaps / (double) Math.max(1, pairs);
    }
}
