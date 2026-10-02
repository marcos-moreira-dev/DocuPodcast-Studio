package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTranslationPreferencesTest {
    @Test
    void defaultsKeepCurrentBehaviorAndRoundTripThroughViewState() {
        DocumentTranslationPreferences defaults = DocumentTranslationPreferences.defaults();
        assertFalse(defaults.enabled());
        assertEquals(DocumentListeningLanguage.SPANISH, defaults.listeningLanguage());

        DocumentTranslationPreferences selected = new DocumentTranslationPreferences(
                true, DocumentListeningLanguage.ENGLISH);
        Map<String, String> state = selected.applyTo(Map.of("unrelated", "kept"));
        assertEquals(selected, DocumentTranslationPreferences.fromViewState(state));
        assertEquals("kept", state.get("unrelated"));
    }
}
