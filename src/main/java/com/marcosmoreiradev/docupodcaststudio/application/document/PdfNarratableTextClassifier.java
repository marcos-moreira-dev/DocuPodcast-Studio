package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.Locale;

/** Deterministic PDF prose filter used before creating narratable blocks/targets. */
public final class PdfNarratableTextClassifier {
    private PdfNarratableTextClassifier() {
    }

    public static boolean shouldSkip(String text) {
        return !narratableProse(text);
    }

    public static boolean narratableProse(String text) {
        String normalized = normalize(text);
        if (normalized.length() < 3 || pageNumberLike(normalized) || tableLike(normalized)) {
            return false;
        }
        long letters = normalized.chars().filter(Character::isLetter).count();
        long digits = normalized.chars().filter(Character::isDigit).count();
        long operators = normalized.chars().filter(ch -> "+=*/<>^_|[]{}".indexOf(ch) >= 0).count();
        long words = java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.chars().anyMatch(Character::isLetter))
                .count();
        if (letters < 3 || words < 1) {
            return false;
        }
        if (looksMathOnly(normalized, letters, digits, operators, words)) {
            return false;
        }
        if (words >= 4 && letters >= 14 && letters >= digits) {
            return true;
        }
        return letters >= Math.max(3, normalized.length() / 5);
    }

    public static PdfNarratabilityDecision decide(String text, double confidence) {
        String normalized = normalize(text);
        double safeConfidence = Double.isFinite(confidence)
                ? Math.max(0.0, Math.min(1.0, confidence))
                : 0.0;
        if (normalized.isBlank()) {
            return new PdfNarratabilityDecision(PdfNarratability.NON_NARRATABLE, java.util.List.of("empty-text"));
        }
        if (pageNumberLike(normalized)) {
            return new PdfNarratabilityDecision(PdfNarratability.NON_NARRATABLE, java.util.List.of("page-number"));
        }
        if (tableLike(normalized)) {
            return new PdfNarratabilityDecision(PdfNarratability.NON_NARRATABLE, java.util.List.of("table-like"));
        }
        long letters = normalized.chars().filter(Character::isLetter).count();
        long digits = normalized.chars().filter(Character::isDigit).count();
        long operators = normalized.chars().filter(ch -> "+=*/<>^_|[]{}".indexOf(ch) >= 0).count();
        long words = java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.chars().anyMatch(Character::isLetter))
                .count();
        if (looksMathOnly(normalized, letters, digits, operators, words)) {
            return new PdfNarratabilityDecision(PdfNarratability.NON_NARRATABLE, java.util.List.of("math-like"));
        }
        if (narratableProse(normalized) && safeConfidence >= 0.65) {
            return new PdfNarratabilityDecision(PdfNarratability.NARRATABLE,
                    java.util.List.of("prose-shape", "ocr-confidence>=0.65"));
        }
        java.util.ArrayList<String> reasons = new java.util.ArrayList<>();
        if (safeConfidence < 0.65) reasons.add("ocr-confidence<0.65");
        if (!narratableProse(normalized)) reasons.add("weak-prose-signal");
        if (reasons.isEmpty()) reasons.add("conflicting-signals");
        return new PdfNarratabilityDecision(PdfNarratability.UNCERTAIN, reasons);
    }

    public static PdfRegionType classify(String text, int index) {
        String normalized = normalize(text);
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.equals("contents") || lower.equals("table of contents")
                || lower.equals("contenido") || lower.equals("índice") || lower.equals("indice")
                || lower.startsWith("chapter ") || lower.startsWith("capítulo ") || lower.startsWith("capitulo ")) {
            return PdfRegionType.HEADING;
        }
        if (normalized.matches("^(\\d+)(\\.\\d+){0,3}\\s+\\p{Lu}.*") && normalized.length() <= 140) {
            return normalized.chars().filter(ch -> ch == '.').count() >= 1
                    ? PdfRegionType.SUBHEADING
                    : PdfRegionType.HEADING;
        }
        if (normalized.matches("^([*\\-]|\\d+[.)])\\s+.+")) {
            return PdfRegionType.LIST;
        }
        if (lower.matches("^(la pregunta|intuici[oó]n|nota|advertencia"
                + "|lectura conceptual|idea clave|ejemplo)\\b.*")) {
            return PdfRegionType.SIDEBAR;
        }
        long letters = normalized.chars().filter(Character::isLetter).count();
        long digits = normalized.chars().filter(Character::isDigit).count();
        long operators = normalized.chars().filter(ch -> "+=*/<>^_|[]{}".indexOf(ch) >= 0).count();
        long words = java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.chars().anyMatch(Character::isLetter))
                .count();
        return looksMathOnly(normalized, letters, digits, operators, words)
                ? PdfRegionType.MATH
                : PdfRegionType.PARAGRAPH;
    }

    private static boolean pageNumberLike(String text) {
        return text.matches("^\\d{1,4}$")
                || text.matches("(?i)^(page|pagina|p\\.)\\s*\\d{1,4}$")
                || text.matches("(?i)^[ivxlcdm]{1,8}$");
    }

    private static boolean tableLike(String text) {
        String normalized = normalize(text);
        long separators = normalized.chars().filter(ch -> ch == '\t' || ch == '|').count();
        long wideGaps = java.util.regex.Pattern.compile("\\s{3,}").matcher(normalized).results().count();
        long numericTokens = java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.matches("[-+]?\\d+(?:[.,]\\d+)?%?"))
                .count();
        long words = java.util.Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.chars().anyMatch(Character::isLetter))
                .count();
        long letters = normalized.chars().filter(Character::isLetter).count();
        if (proseCue(normalized, letters, words)) {
            return false;
        }
        return (separators + wideGaps >= 2 && numericTokens >= 2)
                || (numericTokens >= 4 && numericTokens > words);
    }

    private static boolean looksMathOnly(String text, long letters, long digits, long operators, long words) {
        if (text.length() > 180 || text.isBlank()) {
            return false;
        }
        if (proseCue(text, letters, words)) {
            return false;
        }
        long mathSymbols = text.chars().filter(ch -> "+=*/<>^_".indexOf(ch) >= 0).count();
        return mathSymbols >= 2 && digits + letters >= 2 && letters < text.length() / 2
                || operators >= 3 && operators > letters;
    }

    private static boolean proseCue(String text, long letters, long words) {
        if (letters < 12 || words < 3) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.matches(".*\\b(the|and|this|these|that|with|from|for|into|can|is|are|was|were|para|que|los|las|una|uno|como|por|con|del)\\b.*")
                || text.matches(".*[.!?].*");
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').replaceAll("\\s+", " ").strip();
    }
}
