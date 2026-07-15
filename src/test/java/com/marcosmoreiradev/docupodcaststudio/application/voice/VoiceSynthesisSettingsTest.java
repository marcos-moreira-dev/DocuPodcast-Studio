package com.marcosmoreiradev.docupodcaststudio.application.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoiceSynthesisSettingsTest {
    @Test
    void exposesSafeDefaultsForNormalReader() {
        var defaults = VoiceSynthesisSettings.defaults();
        assertEquals(1.0, defaults.speechRate());
        assertEquals("1.00x", defaults.speechRateLabel());
        assertEquals("100%", defaults.volumeLabel());
        assertEquals(0.4, defaults.sentencePauseSeconds());
        assertEquals(0.8, defaults.paragraphPauseSeconds());
        assertEquals("es", defaults.defaultLanguage());
    }

    @Test
    void rejectsExtremeValuesThatWouldBreakUsability() {
        assertThrows(IllegalArgumentException.class, () -> new VoiceSynthesisSettings(0.1, 1.0, 0.4, 0.8, "es"));
        assertThrows(IllegalArgumentException.class, () -> new VoiceSynthesisSettings(1.0, 3.0, 0.4, 0.8, "es"));
        assertThrows(IllegalArgumentException.class, () -> new VoiceSynthesisSettings(1.0, 1.0, 8.0, 0.8, "es"));
        assertThrows(IllegalArgumentException.class, () -> new VoiceSynthesisSettings(1.0, 1.0, 0.4, 12.0, "es"));
    }
}
