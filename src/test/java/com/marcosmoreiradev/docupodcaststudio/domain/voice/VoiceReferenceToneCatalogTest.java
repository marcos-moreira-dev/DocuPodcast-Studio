package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceReferenceToneCatalogTest {
    @Test
    void catalogContainsBasicAndTheatricalTonesWithPrompts() {
        assertTrue(VoiceReferenceTone.basicTones().contains(VoiceReferenceTone.NEUTRAL));
        assertTrue(VoiceReferenceTone.basicTones().contains(VoiceReferenceTone.BORED));
        assertTrue(VoiceReferenceTone.theatricalExtendedTones().contains(VoiceReferenceTone.HEROIC));
        assertTrue(VoiceReferenceTone.theatricalExtendedTones().contains(VoiceReferenceTone.SEDUCTIVE_NON_EXPLICIT));

        for (VoiceReferenceTone tone : VoiceReferenceTone.values()) {
            assertFalse(tone.displayName().isBlank(), tone.name());
            assertFalse(tone.suggestedRecordingPrompt().isBlank(), tone.name());
            assertTrue(tone.suggestedRecordingPrompt().length() > 40, tone.name());
        }
    }

    @Test
    void everyToneBelongsToExactlyOneCatalogCategory() {
        long basic = Arrays.stream(VoiceReferenceTone.values()).filter(VoiceReferenceTone::isBasic).count();
        long theatrical = Arrays.stream(VoiceReferenceTone.values()).filter(VoiceReferenceTone::isTheatricalExtended).count();

        assertEquals(VoiceReferenceTone.values().length, basic + theatrical);
        assertEquals(EnumSet.allOf(VoiceReferenceTone.class).size(), basic + theatrical);
    }

    @Test
    void userFacingEngineTargetsDoNotExposeTechnicalEngineNames() {
        assertEquals("Voz IA avanzada", VoiceEngineTarget.ADVANCED_AI_VOICE.displayName());
        assertEquals("Voz local simple", VoiceEngineTarget.LOCAL_SIMPLE_VOICE.displayName());
        assertEquals("Modo de prueba", VoiceEngineTarget.TEST_MODE.displayName());

        for (VoiceEngineTarget target : VoiceEngineTarget.values()) {
            assertFalse(target.displayName().contains("Coqui"));
            assertFalse(target.displayName().contains("XTTS"));
        }
    }
}
