package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentLanguageProfile;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Deterministic local language hint used only for OCR and narration defaults. */
public final class PdfLocalLanguageDetector {
    private static final Set<String> SPANISH = Set.of(
            "el", "la", "los", "las", "de", "del", "que", "para", "con", "una", "por", "se");
    private static final Set<String> ENGLISH = Set.of(
            "the", "of", "and", "to", "in", "for", "with", "a", "is", "by", "from");

    public PdfDocumentLanguageProfile detect(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^a-z]+");
        int spanish = 0;
        int english = 0;
        int counted = 0;
        for (String token : tokens) {
            if (token.isBlank()) continue;
            counted++;
            if (SPANISH.contains(token)) spanish++;
            if (ENGLISH.contains(token)) english++;
        }
        if (counted < 6 || Math.max(spanish, english) < 2) {
            return PdfDocumentLanguageProfile.undetermined();
        }
        int winner = Math.max(spanish, english);
        double confidence = Math.min(0.98, 0.55 + winner / (double) Math.max(8, counted));
        return new PdfDocumentLanguageProfile(spanish > english ? "es" : "en", confidence, null);
    }
}
