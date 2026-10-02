package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Conservative structural filter for native Word text.
 *
 * <p>This policy never rewrites source text. It only decides whether a plain
 * DOCX paragraph is safe for automatic narration, looks like mathematical
 * notation, or needs review because its character stream is damaged.</p>
 */
final class DocxNarratabilityPolicy {
    enum Decision {
        NARRATABLE,
        MATHEMATICAL,
        UNCERTAIN
    }

    record Assessment(Decision decision, String reason) {
    }

    private static final Pattern LATEX = Pattern.compile(
            "(?s).*?(\\\\\\(|\\\\\\[|\\$\\$|\\\\(?:frac|sqrt|sum|int|begin|alpha|beta|gamma)\\b).*");
    private static final Pattern EQUATION_OPERATOR = Pattern.compile(
            "[=+\\-*/^×÷√∑∫≤≥≈≠→←∈∉⊂⊆⊇∞±]");
    private static final Pattern WORD = Pattern.compile("\\p{L}{2,}");
    private static final Pattern REPEATED_SYMBOL = Pattern.compile("([^\\p{L}\\p{N}\\s])\\1{5,}");

    private DocxNarratabilityPolicy() {
    }

    static Assessment assess(String raw) {
        String text = raw == null ? "" : Normalizer.normalize(raw, Normalizer.Form.NFKC).strip();
        if (text.isBlank()) {
            return new Assessment(Decision.UNCERTAIN, "empty-native-text");
        }
        if (containsDamagedCharacters(text)) {
            return new Assessment(Decision.UNCERTAIN, "invalid-or-private-use-characters");
        }
        if (LATEX.matcher(text).matches() || looksLikeEquation(text)) {
            return new Assessment(Decision.MATHEMATICAL, "equation-like-native-text");
        }
        if (looksLikeDamagedTokenStream(text)) {
            return new Assessment(Decision.UNCERTAIN, "fragmented-or-symbol-heavy-text");
        }
        return new Assessment(Decision.NARRATABLE, "native-word-prose");
    }

    private static boolean looksLikeEquation(String text) {
        int operators = 0;
        var matcher = EQUATION_OPERATOR.matcher(text);
        while (matcher.find()) {
            operators++;
        }
        int words = 0;
        matcher = WORD.matcher(text);
        while (matcher.find()) {
            words++;
        }
        long mathSymbols = text.codePoints()
                .filter(codePoint -> Character.getType(codePoint) == Character.MATH_SYMBOL)
                .count();
        boolean hasEquality = text.indexOf('=') >= 0 || text.indexOf('≈') >= 0
                || text.indexOf('≤') >= 0 || text.indexOf('≥') >= 0;
        return (hasEquality && operators >= 1 && words <= 5)
                || (operators >= 3 && words <= 3)
                || (mathSymbols >= 2 && words <= 5);
    }

    private static boolean looksLikeDamagedTokenStream(String text) {
        if (REPEATED_SYMBOL.matcher(text).find()) {
            return true;
        }
        int visible = 0;
        int symbols = 0;
        for (int codePoint : text.codePoints().toArray()) {
            if (Character.isWhitespace(codePoint)) {
                continue;
            }
            visible++;
            int type = Character.getType(codePoint);
            if (type == Character.OTHER_SYMBOL
                    || type == Character.MODIFIER_SYMBOL
                    || type == Character.PRIVATE_USE) {
                symbols++;
            }
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("\\s+");
        long oneCharacterTokens = java.util.Arrays.stream(tokens)
                .filter(token -> token.codePointCount(0, token.length()) == 1)
                .count();
        return visible >= 8 && symbols * 100 >= visible * 35
                || tokens.length >= 8 && oneCharacterTokens * 100 >= tokens.length * 65;
    }

    private static boolean containsDamagedCharacters(String text) {
        return text.codePoints().anyMatch(codePoint -> codePoint == 0xFFFD
                || Character.getType(codePoint) == Character.PRIVATE_USE
                || Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint));
    }
}
