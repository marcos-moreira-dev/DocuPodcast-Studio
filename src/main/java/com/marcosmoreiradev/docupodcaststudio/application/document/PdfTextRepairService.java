package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative repair pass for native PDF text extraction. OCR is deliberately out of scope. */
public final class PdfTextRepairService {
    private static final Pattern LETTER_SPACED_WORD = Pattern.compile("(?<![\\p{L}\\p{N}])(?:\\p{L}\\s+){2,}\\p{L}(?![\\p{L}\\p{N}])");

    public PdfTextNormalizationReport repair(String raw) {
        String base = raw == null ? "" : raw
                .replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ')
                .replace('\u000B', ' ')
                .replace('\f', '\n');
        List<String> lines = normalizeLines(base);
        HeaderFooterResult headerFooter = removeRepeatedHeadersAndFooters(lines);
        RepairResult letterSpacing = repairLetterSpacing(headerFooter.lines());
        RepairResult lineBreaks = repairArtificialLineBreaks(letterSpacing.lines());
        String normalized = String.join("\n", lineBreaks.lines())
                .replaceAll("[ ]+([,.;:])", "$1")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
        return new PdfTextNormalizationReport(
                normalized,
                letterSpacing.repairs(),
                lineBreaks.repairs(),
                headerFooter.removed());
    }

    private static List<String> normalizeLines(String raw) {
        String[] split = raw.split("\\n", -1);
        ArrayList<String> lines = new ArrayList<>(split.length);
        for (String line : split) {
            lines.add(line == null ? "" : line.replaceAll("[ ]{2,}", " ").strip());
        }
        return List.copyOf(lines);
    }

    private static HeaderFooterResult removeRepeatedHeadersAndFooters(List<String> lines) {
        if (lines.size() < 6) {
            return new HeaderFooterResult(lines, 0);
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : lines) {
            String key = repeatedLineKey(line);
            if (!key.isBlank()) {
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        ArrayList<String> cleaned = new ArrayList<>(lines.size());
        int removed = 0;
        for (String line : lines) {
            String key = repeatedLineKey(line);
            if (!key.isBlank() && counts.getOrDefault(key, 0) >= 3) {
                removed++;
                continue;
            }
            cleaned.add(line);
        }
        return new HeaderFooterResult(List.copyOf(cleaned), removed);
    }

    private static String repeatedLineKey(String line) {
        String normalized = line == null ? "" : line.replaceAll("\\s+", " ").strip();
        if (normalized.length() < 4 || normalized.length() > 80) {
            return "";
        }
        if (looksFormulaCodeOrTable(normalized) || normalized.chars().filter(Character::isLetter).count() < 3) {
            return "";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static RepairResult repairLetterSpacing(List<String> lines) {
        ArrayList<String> repaired = new ArrayList<>(lines.size());
        int count = 0;
        for (String line : lines) {
            if (looksFormulaCodeOrTable(line)) {
                repaired.add(line);
                continue;
            }
            Matcher matcher = LETTER_SPACED_WORD.matcher(line);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String candidate = matcher.group();
                String compact = candidate.replaceAll("\\s+", "");
                if (compact.length() >= 3 && compact.length() <= 32) {
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(compact));
                    count++;
                }
            }
            matcher.appendTail(buffer);
            repaired.add(buffer.toString());
        }
        return new RepairResult(List.copyOf(repaired), count);
    }

    private static RepairResult repairArtificialLineBreaks(List<String> lines) {
        ArrayList<String> repaired = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            String current = lines.get(i);
            if (current.isBlank() || looksFormulaCodeOrTable(current)) {
                repaired.add(current);
                continue;
            }
            while (i + 1 < lines.size() && shouldMerge(current, lines.get(i + 1))) {
                current = current + " " + lines.get(++i).strip();
                count++;
            }
            repaired.add(current);
        }
        return new RepairResult(List.copyOf(repaired), count);
    }

    private static boolean shouldMerge(String current, String next) {
        String left = current == null ? "" : current.strip();
        String right = next == null ? "" : next.strip();
        if (left.isBlank() || right.isBlank() || looksFormulaCodeOrTable(left) || looksFormulaCodeOrTable(right)) {
            return false;
        }
        char last = left.charAt(left.length() - 1);
        if (".?!;:".indexOf(last) >= 0) {
            return false;
        }
        if (left.length() < 18) {
            return false;
        }
        int first = right.codePointAt(0);
        return Character.isLowerCase(first) || ",)]}".indexOf(first) >= 0 || last == ',';
    }

    private static boolean looksFormulaCodeOrTable(String line) {
        String value = line == null ? "" : line.strip();
        if (value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.contains("|") || value.contains("\\") || value.contains("{") || value.contains("}")
                || lower.contains("```") || lower.contains("http://") || lower.contains("https://")) {
            return true;
        }
        long operators = value.chars().filter(ch -> "+=*/<>^_".indexOf(ch) >= 0).count();
        long digits = value.chars().filter(Character::isDigit).count();
        return operators >= 2 || digits >= 6 && operators >= 1;
    }

    private record RepairResult(List<String> lines, int repairs) {
    }

    private record HeaderFooterResult(List<String> lines, int removed) {
    }
}
