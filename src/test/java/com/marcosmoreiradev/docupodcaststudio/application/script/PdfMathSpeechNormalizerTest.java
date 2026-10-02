package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class PdfMathSpeechNormalizerTest {
    private final PdfMathSpeechNormalizer normalizer = new PdfMathSpeechNormalizer();

    @Test
    void speaksBalancedVlmLatexWithoutSendingDollarDelimitersToTts() {
        PdfRegion region = region(PdfRegionType.PARAGRAPH,
                "Al sustituir $x = 0$, queda $\\sin x / x$.",
                PdfRegionOrigin.VLM_SEMANTIC);

        String spoken = normalizer.normalize(region, region.effectiveText());

        assertFalse(spoken.contains("$"));
        assertTrue(spoken.contains("equis igual a cero"));
        assertTrue(spoken.contains("seno de equis dividido para equis"));
        assertEquals(PdfMathSpeechNormalizer.CONVENTION_LEGACY_VLM,
                normalizer.convention(region, region.effectiveText()));
    }

    @Test
    void preservesRealCurrencyBecauseItIsLiteralEvidenceNotBalancedMathMarkup() {
        PdfRegion region = region(PdfRegionType.PARAGRAPH,
                "Precio: $25. Total US$ 100 y promoción de $5.50.",
                PdfRegionOrigin.NATIVE_TEXT);

        String spoken = normalizer.normalize(region, region.effectiveText());

        assertEquals(region.effectiveText(), spoken);
        assertTrue(spoken.contains("$25"));
        assertTrue(spoken.contains("US$ 100"));
        assertEquals(PdfMathSpeechNormalizer.CONVENTION_LITERAL,
                normalizer.convention(region, region.effectiveText()));
    }

    @Test
    void supportsFractionsPowersRootsFunctionsPiLimitsAndInequalities() {
        String spoken = normalizer.speakExpression(
                "\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1; "
                        + "0 < x < \\pi/2; \\sqrt{x^2}");

        assertTrue(spoken.contains("límite cuando equis tiende a cero"));
        assertTrue(spoken.contains("seno de equis dividido para equis"));
        assertTrue(spoken.contains("cero menor que equis"));
        assertTrue(spoken.contains("pi sobre dos"));
        assertTrue(spoken.contains("raíz cuadrada de equis al cuadrado"));
        assertFalse(spoken.matches(".*[\\\\${}].*"));
    }

    @Test
    void mathRegionProvidesStructuralAuthorityWithoutDollarHeuristics() {
        PdfRegion region = region(PdfRegionType.MATH,
                "sin x < x < tan x", PdfRegionOrigin.OCR_LOCAL);

        String spoken = normalizer.normalize(region, region.effectiveText());

        assertTrue(spoken.contains("seno de equis menor que equis"));
        assertTrue(spoken.contains("tangente de equis"));
        assertEquals(PdfMathSpeechNormalizer.CONVENTION_EXPLICIT,
                normalizer.convention(region, region.effectiveText()));
    }

    @Test
    void normalizesUnicodeLimitNotationWithoutChangingSpanishConjunctionY() {
        String spoken = normalizer.speakExpression(
                "cos x → 1 y 1 → 1; limₓ→₀⁺ sin x/x = 1; ½r²x");

        assertTrue(spoken.contains("coseno de equis tiende a uno y uno tiende a uno"));
        assertTrue(spoken.contains("límite cuando equis tiende a cero por la derecha"));
        assertTrue(spoken.contains("un medio por"));
        assertTrue(spoken.contains("al cuadrado"));
        assertFalse(spoken.contains("ye uno"));
    }

    private static PdfRegion region(PdfRegionType type, String text,
                                    PdfRegionOrigin origin) {
        return new PdfRegion("R1", 1, 10, 10, 500, 80,
                0, 0, text, type, PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(origin, 1.0, "test", "test", "test", "test"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
