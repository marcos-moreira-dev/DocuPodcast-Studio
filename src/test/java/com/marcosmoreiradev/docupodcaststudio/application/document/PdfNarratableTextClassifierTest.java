package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfNarratableTextClassifierTest {
    @Test
    void keepsClearProseOnMixedMathAndTablePages() {
        assertTrue(PdfNarratableTextClassifier.narratableProse("and the final O(h10) approximation is"));
        assertTrue(PdfNarratableTextClassifier.narratableProse("These results are shown in Table 4.9."));
        assertTrue(PdfNarratableTextClassifier.narratableProse(
                "Nesting has reduced the relative error for the chopping approximation to less than 10%."));
        assertTrue(PdfNarratableTextClassifier.narratableProse(
                "Polynomials should always be expressed in nested form before performing an evaluation."));
    }

    @Test
    void rejectsNumericTableRowsAndFormulaDominantLines() {
        assertFalse(PdfNarratableTextClassifier.narratableProse("1.57079633 2.09439511 1.99857073 2.00000555"));
        assertFalse(PdfNarratableTextClassifier.narratableProse("R5,5 = R5,4 + 1/255 (R5,4 - R4,4) = 1.99999999"));
    }

    @Test
    void appliesTriStateWithoutDiscardingLowConfidenceProse() {
        assertEquals(PdfNarratability.NARRATABLE,
                PdfNarratableTextClassifier.decide(
                        "Este párrafo contiene prosa reconocible y una oración completa.", 0.65)
                        .narratability());
        assertEquals(PdfNarratability.UNCERTAIN,
                PdfNarratableTextClassifier.decide(
                        "Este párrafo contiene prosa reconocible pero el OCR está dañado.", 0.40)
                        .narratability());
        assertEquals(PdfNarratability.NON_NARRATABLE,
                PdfNarratableTextClassifier.decide("142", 0.99).narratability());
    }
}
