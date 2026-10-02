package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfSemanticNarrationSafetyValidatorTest {
    private final PdfSemanticNarrationSafetyValidator validator =
            new PdfSemanticNarrationSafetyValidator();

    @Test
    void rejectsRawMathNotationBeforeTts() {
        assertFalse(validator.safeText(
                PdfDerivedTreatmentKind.MATHEMATICAL_READING,
                "sqrt(x) dividido por y", "SPOKEN_MATH"));
        assertFalse(validator.safeText(
                PdfDerivedTreatmentKind.MATHEMATICAL_READING,
                "La expresión \\frac{x}{y}", "SPOKEN_MATH"));
        assertTrue(validator.safeText(
                PdfDerivedTreatmentKind.MATHEMATICAL_READING,
                "la raíz cuadrada de equis dividida por ye",
                "SPOKEN_MATH"));
    }

    @Test
    void automaticAdmissionRequiresQwenMetadataAndGrounding() {
        PdfDerivedTreatment safe = treatment(Map.of(
                "automaticAdmission",
                PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION,
                "groundingStatus", "OK",
                "ttsSafetyValidated", "true",
                "sourceFingerprintValidated", "true",
                "semanticStrategy", "BRIEF_DESCRIPTION"));
        assertTrue(validator.safeForAutomaticAdmission(safe));
        assertFalse(validator.safeForAutomaticAdmission(treatment(Map.of(
                "groundingStatus", "INSUFFICIENT_EVIDENCE",
                "ttsSafetyValidated", "true"))));
    }

    @Test
    void visualDescriptionsMayBeUsefulWithoutBecomingUnbounded() {
        assertTrue(validator.safeText(PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                words(90), "BRIEF_DESCRIPTION"));
        assertFalse(validator.safeText(PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                words(91), "BRIEF_DESCRIPTION"));
    }

    private static String words(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> "detalle")
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static PdfDerivedTreatment treatment(Map<String, String> metadata) {
        return new PdfDerivedTreatment("DER", PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                List.of("IMG"), "La figura compara dos curvas.",
                "qwen3-vl:4b-instruct-q8_0", "1", 0.9, Instant.now(),
                PdfDerivedTreatmentState.DRAFT, 1, "fingerprint", "prompt",
                metadata);
    }
}
