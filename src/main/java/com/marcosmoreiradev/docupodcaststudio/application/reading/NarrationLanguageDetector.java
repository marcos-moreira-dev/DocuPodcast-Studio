package com.marcosmoreiradev.docupodcaststudio.application.reading;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Conservative local ES/EN detector. It never invokes an AI model. */
public final class NarrationLanguageDetector {
    private static final Set<String> ES = Set.of(
            "el", "la", "los", "las", "de", "del", "que", "para", "con",
            "una", "por", "se", "es", "como", "esta", "este", "y");
    private static final Set<String> EN = Set.of(
            "the", "of", "and", "to", "in", "for", "with", "a", "is", "by",
            "from", "this", "that", "as", "are", "on", "an", "when", "into",
            "but", "not", "has", "one", "two", "zero");
    private static final Set<String> ES_HINTS = Set.of(
            "introduccion", "capitulo", "teorema", "demostracion", "derivada",
            "funcion", "tabla", "figura", "ejemplo", "conclusion", "igual",
            "seno", "equis", "menor", "tiende", "resultado", "rettangolo",
            "risultato", "tabella");
    private static final Set<String> EN_HINTS = Set.of(
            "introduction", "chapter", "theorem", "proof", "derivative",
            "function", "table", "figure", "example", "conclusion", "equals",
            "sine", "cosine", "less", "tends", "result", "limit", "approaches",
            "main", "topic", "reveals", "certainty");

    public String detect(String text, String fallback) {
        String normalized = Normalizer.normalize(text == null ? "" : text,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Scores scores = scores(normalized);
        int es = scores.es();
        int en = scores.en();
        if (es == en || Math.max(es, en) < 2) {
            String safe = fallback == null ? "" : fallback.strip().toLowerCase(Locale.ROOT);
            return safe.startsWith("en") ? "en" : "es";
        }
        return en > es ? "en" : "es";
    }

    /** Strict check for cache admission; unlike detect, weak text cannot inherit fallback. */
    public boolean confidentlyMatches(String text, String expectedLanguage) {
        String normalized = Normalizer.normalize(text == null ? "" : text,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Scores scores = scores(normalized);
        String expected = expectedLanguage == null ? "" : expectedLanguage.toLowerCase(Locale.ROOT);
        return expected.startsWith("en")
                ? scores.en() >= 2 && scores.en() > scores.es()
                : expected.startsWith("es")
                && scores.es() >= 2 && scores.es() > scores.en();
    }

    /** Allows language-neutral mathematical notation without weakening prose checks. */
    public boolean languageNeutralMathematicalNotation(String text) {
        String raw = text == null ? "" : text.strip();
        if (raw.isBlank() || !(raw.contains("\\") || raw.contains("=")
                || raw.contains("<") || raw.contains(">"))) return false;
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Scores scores = scores(normalized);
        return scores.es() == 0 && scores.en() <= 1;
    }

    private static Scores scores(String normalized) {
        int es = 0;
        int en = 0;
        for (String token : normalized.split("[^a-z]+")) {
            if (ES.contains(token)) es++;
            if (EN.contains(token)) en++;
            if (ES_HINTS.contains(token)) es += 2;
            if (EN_HINTS.contains(token)) en += 2;
        }
        return new Scores(es, en);
    }

    private record Scores(int es, int en) { }
}
