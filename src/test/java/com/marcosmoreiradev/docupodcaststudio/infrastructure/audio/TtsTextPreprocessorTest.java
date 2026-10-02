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
    void returnsEmptyPayloadWhenTextBecomesBlank() {
        assertEquals("", TtsTextPreprocessor.sanitize("https://example.com"));
    }

}
