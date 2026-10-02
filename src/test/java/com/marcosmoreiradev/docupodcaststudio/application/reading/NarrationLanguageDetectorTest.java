package com.marcosmoreiradev.docupodcaststudio.application.reading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class NarrationLanguageDetectorTest {
    private final NarrationLanguageDetector detector = new NarrationLanguageDetector();

    @Test
    void acceptsShortEnglishAcademicTitle() {
        assertTrue(detector.confidentlyMatches(
                "Main topic: When an indeterminate fraction reveals a certainty.",
                "en"));
    }

    @Test
    void acceptsLanguageNeutralLatexButRejectsSpanishVerbalization() {
        assertTrue(detector.languageNeutralMathematicalNotation(
                "lim_{x \\to 0^+} \\frac{\\sin x}{x} = 1."));
        assertFalse(detector.languageNeutralMathematicalNotation(
                "seno de equis = uno"));
        assertFalse(detector.confidentlyMatches("seno de equis = uno", "en"));
    }
}
