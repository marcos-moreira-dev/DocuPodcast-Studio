package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

import java.util.Locale;
import java.util.regex.Pattern;

/** Final deterministic gate between generated PDF prose and TTS. */
public final class PdfSemanticNarrationSafetyValidator {
    public static final String VERSION = "pdf-semantic-tts-v2";
    public static final String AUTOMATIC_ADMISSION = "USER_POLICY_AUTOMATIC";
    private static final Pattern RAW_MATH = Pattern.compile(
            "(?i)(\\\\(?:sqrt|frac|begin|end|left|right)\\b|<math\\b|</math>|"
                    + "\\$[^$]+\\$|[A-Za-z0-9})\\]]\\s*[\\^_]\\s*[A-Za-z0-9({\\[]|"
                    + "(?:sqrt|frac)\\s*\\(|[A-Za-z0-9})\\]]\\s*/\\s*[A-Za-z0-9({\\[])");

    public boolean safeForAutomaticAdmission(PdfDerivedTreatment treatment) {
        if (treatment == null || treatment.derivedText().isBlank()) return false;
        if (!AUTOMATIC_ADMISSION.equals(
                treatment.metadata().get("automaticAdmission"))) return false;
        if (!"OK".equalsIgnoreCase(treatment.metadata().getOrDefault(
                "groundingStatus", ""))) return false;
        if (!"true".equalsIgnoreCase(treatment.metadata().getOrDefault(
                "ttsSafetyValidated", "false"))) return false;
        if (!"true".equalsIgnoreCase(treatment.metadata().getOrDefault(
                "sourceFingerprintValidated", "false"))) return false;
        if ("legacy-source".equals(treatment.sourceFingerprint())) return false;
        return safeText(treatment.kind(), treatment.derivedText(),
                treatment.metadata().getOrDefault("semanticStrategy", ""));
    }

    public boolean safeApprovedText(PdfDerivedTreatment treatment) {
        if (treatment == null || treatment.derivedText().isBlank()) return false;
        return treatment.kind() != PdfDerivedTreatmentKind.MATHEMATICAL_READING
                || !containsRawMath(treatment.derivedText());
    }

    public boolean safeText(PdfDerivedTreatmentKind kind, String text,
                            String strategy) {
        String value = text == null ? "" : text.strip();
        if (value.isBlank()) return false;
        if (kind == PdfDerivedTreatmentKind.MATHEMATICAL_READING
                && containsRawMath(value)) return false;
        if (kind == PdfDerivedTreatmentKind.TABLE_NARRATION
                || kind == PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION) {
            if ("FULL_TEXT".equalsIgnoreCase(strategy)) {
                return value.length() <= 700;
            }
            return words(value) <= 60;
        }
        if (kind == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION
                || kind == PdfDerivedTreatmentKind.LIGHTWEIGHT_LANGUAGE_MODEL) {
            return words(value) <= 90;
        }
        return words(value) <= 60;
    }

    public boolean containsRawMath(String text) {
        return RAW_MATH.matcher(text == null ? "" : text).find();
    }

    private static int words(String value) {
        String normalized = value == null ? "" : value.strip()
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? 0 : normalized.split("\\s+").length;
    }
}
