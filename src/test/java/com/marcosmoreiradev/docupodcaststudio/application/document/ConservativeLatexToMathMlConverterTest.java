package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConservativeLatexToMathMlConverterTest {
    private final ConservativeLatexToMathMlConverter converter =
            new ConservativeLatexToMathMlConverter();

    @Test
    void preservesNumbersVariablesOperatorsFractionsAndScripts() {
        String mathMl = converter.convert(
                "\\frac{x_1 + 2}{\\sqrt{y^2}} \\leq 3");

        assertTrue(mathMl.contains("<mfrac>"));
        assertTrue(mathMl.contains("<msub>"));
        assertTrue(mathMl.contains("<msup>"));
        assertTrue(mathMl.contains("<msqrt>"));
        assertTrue(mathMl.contains("≤"));
        assertTrue(mathMl.contains("<mn>3</mn>"));
    }

    @Test
    void rejectsUnknownComplexConstructInsteadOfInventingMeaning() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convert("\\begin{matrix}1&2\\\\3&4\\end{matrix}"));
    }
}
