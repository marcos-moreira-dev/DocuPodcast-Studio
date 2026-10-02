package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnalyzePdfPageMathSourceConventionTest {

    @Test
    void persistsExplicitConventionForStructuredAndInlineTechnicalMath() {
        assertEquals("STRUCTURED_MATH_V1",
                AnalyzePdfPageSemanticallyUseCase.mathSourceConvention(
                        PdfRegionType.MATH, "sin x < x"));
        assertEquals("LATEX_TECHNICAL_V1",
                AnalyzePdfPageSemanticallyUseCase.mathSourceConvention(
                        PdfRegionType.PARAGRAPH, "Al sustituir $x = 0$."));
    }

    @Test
    void monetaryDollarsRemainLiteralEvidence() {
        assertEquals("", AnalyzePdfPageSemanticallyUseCase.mathSourceConvention(
                PdfRegionType.PARAGRAPH, "Precio: $25. Total: $5.50."));
    }
}
