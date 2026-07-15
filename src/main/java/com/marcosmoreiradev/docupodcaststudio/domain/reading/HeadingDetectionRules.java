package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import java.util.List;
import java.util.Locale;

/** Rules used to reinterpret Word styles as document structure before script creation. */
public record HeadingDetectionRules(
        List<String> titleStyleKeywords,
        List<String> headingStyleKeywords,
        List<String> subheadingStyleKeywords,
        int maxShortBoldWords,
        boolean treatShortBoldParagraphAsSubheading
) {
    public HeadingDetectionRules {
        titleStyleKeywords = normalize(titleStyleKeywords);
        headingStyleKeywords = normalize(headingStyleKeywords);
        subheadingStyleKeywords = normalize(subheadingStyleKeywords);
        if (maxShortBoldWords < 1) {
            throw new IllegalArgumentException("maxShortBoldWords must be positive");
        }
    }

    public static HeadingDetectionRules academicDefaults() {
        return new HeadingDetectionRules(
                List.of("title", "titulo", "título", "document title"),
                List.of("heading 1", "titulo 1", "título 1", "encabezado 1"),
                List.of("heading 2", "heading 3", "titulo 2", "título 2", "subtitulo", "subtítulo", "encabezado 2"),
                14,
                true
        );
    }

    public boolean matchesTitleStyle(String styleText) {
        return containsAny(styleText, titleStyleKeywords);
    }

    public boolean matchesHeadingStyle(String styleText) {
        return containsAny(styleText, headingStyleKeywords);
    }

    public boolean matchesSubheadingStyle(String styleText) {
        return containsAny(styleText, subheadingStyleKeywords);
    }

    private static boolean containsAny(String source, List<String> keywords) {
        String text = normalizeText(source);
        if (text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (!keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> normalize(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(HeadingDetectionRules::normalizeText)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
