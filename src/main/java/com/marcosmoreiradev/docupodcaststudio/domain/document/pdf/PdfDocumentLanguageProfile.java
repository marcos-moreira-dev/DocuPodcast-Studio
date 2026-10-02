package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.Locale;

/** Detected document language with an optional manual override. */
public record PdfDocumentLanguageProfile(
        String detectedLanguage,
        double confidence,
        String overrideLanguage
) {
    public static final String UNDETERMINED = "und";

    public PdfDocumentLanguageProfile {
        detectedLanguage = language(detectedLanguage);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        overrideLanguage = overrideLanguage == null || overrideLanguage.isBlank()
                ? null : language(overrideLanguage);
    }

    public static PdfDocumentLanguageProfile undetermined() {
        return new PdfDocumentLanguageProfile(UNDETERMINED, 0.0, null);
    }

    public String effectiveLanguage() {
        return overrideLanguage == null ? detectedLanguage : overrideLanguage;
    }

    public boolean manuallyOverridden() {
        return overrideLanguage != null;
    }

    private static String language(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replace('_', '-');
        if (normalized.isBlank()) return UNDETERMINED;
        if (!normalized.matches("[a-z]{2,3}(?:-[a-z0-9]{2,8})*|und")) {
            throw new IllegalArgumentException("Invalid language tag: " + value);
        }
        return normalized;
    }
}
