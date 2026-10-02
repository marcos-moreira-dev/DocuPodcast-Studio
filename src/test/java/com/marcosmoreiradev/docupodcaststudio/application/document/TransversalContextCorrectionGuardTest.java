package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransversalContextCorrectionGuardTest {
    @Test
    void identifiesFiguresUnitsNamesAndOperatorsThatMustSurvive() {
        var tokens = TransversalContextCorrectionPdfTreatmentEngine.protectedTokens(
                "Burden calcula 1000 cm y r + 0,25 = h.");

        assertTrue(tokens.contains("Burden"));
        assertTrue(tokens.contains("1000 cm"));
        assertTrue(tokens.contains("0,25"));
        assertTrue(tokens.contains("+"));
        assertTrue(tokens.contains("="));
    }

    @Test
    void doesNotInflateRepeatedProtectedTokens() {
        assertEquals(1, TransversalContextCorrectionPdfTreatmentEngine
                .protectedTokens("12 + 12").stream().filter("12"::equals).count());
    }
}
