package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

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
}
