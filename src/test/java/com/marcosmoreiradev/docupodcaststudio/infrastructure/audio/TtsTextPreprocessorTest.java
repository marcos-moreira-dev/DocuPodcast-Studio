package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TtsTextPreprocessorTest {
    @Test
    void stripsMarkdownUrlsControlsAndEmojiForExternalTts() {
        String sanitized = TtsTextPreprocessor.sanitize("# Título\n> Hola **mundo** https://example.com 😀\n[enlace](https://x.test)");

        assertTrue(sanitized.contains("Título"));
        assertTrue(sanitized.contains("Hola mundo"));
        assertTrue(sanitized.contains("enlace"));
        assertFalse(sanitized.contains("https://"));
        assertFalse(sanitized.contains("**"));
        assertFalse(sanitized.contains("😀"));
    }

    @Test
    void returnsSafePauseWhenTextBecomesBlank() {
        assertEquals("Pausa breve.", TtsTextPreprocessor.sanitize("https://example.com"));
    }

    @Test
    void removesPeriodsForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "Estoy bien. Valor 3.14.", "Voz IA avanzada", "");

        assertEquals("Estoy bien Valor 314", sanitized);
    }

    @Test
    void removesPeriodsInMiddleAndEndForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "Hola mundo. Esto sigue.", "Voz IA avanzada", "");

        assertEquals("Hola mundo Esto sigue", sanitized);
    }

    @Test
    void removesFinalPeriodOnlyForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "Fin.", "Voz IA avanzada", "");

        assertEquals("Fin", sanitized);
    }

    @Test
    void replacesEllipsisWithPausaForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "...y luego.", "Voz IA avanzada", "");

        assertEquals("pausa y luego", sanitized);
    }

    @Test
    void standaloneEllipsisBecomesPausaForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "...", "Voz IA avanzada", "");

        assertEquals("pausa", sanitized);
    }

    @Test
    void decimalNumberLosesDotForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "El valor es 3.14", "Voz IA avanzada", "");

        assertEquals("El valor es 314", sanitized);
    }

    @Test
    void keepsPeriodsForNonXttsEngine() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "Hola mundo.", "Voz local simple", "");

        assertEquals("Hola mundo.", sanitized);
    }

    @Test
    void removesAccentsForPiperLocalVoicePayloadOnly() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "áéíóú ñ ÁÉÍÓÚ Ñ", "Voz local simple", "piper.exe");

        assertEquals("aeiou n AEIOU N", sanitized);
    }

    @Test
    void noPeriodTextUnchangedForXtts() {
        String sanitized = TtsTextPreprocessor.sanitizeForEngine(
                "Hola mundo", "Voz IA avanzada", "");

        assertEquals("Hola mundo", sanitized);
    }
}
