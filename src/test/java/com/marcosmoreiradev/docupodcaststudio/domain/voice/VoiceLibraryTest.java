package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLibraryTest {
    @Test
    void defaultsContainNarratorVoiceCharacterAndNeutralStyle() {
        VoiceLibrary library = VoiceLibrary.defaults();

        assertTrue(library.voiceById("VOC-NARRATOR").isPresent());
        assertTrue(library.voiceById(OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID).isPresent());
        assertTrue(library.characterById("CHR-NARRATOR").isPresent());
        assertTrue(library.styleById("STY-NEUTRAL").isPresent());
        assertTrue(library.supportsSegmentVoice("CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL"));
        assertTrue(library.voiceById("VOC-OWN-PLACEHOLDER").isEmpty());
    }

    @Test
    void rejectsDuplicatedVoiceIds() {
        VoiceProfile narrator = VoiceProfile.predefinedNarrator();

        assertThrows(IllegalArgumentException.class, () -> new VoiceLibrary(
                "VOICE-LIBRARY-TEST",
                List.of(narrator, narrator),
                VoiceLibrary.defaults().characters(),
                VoiceLibrary.defaults().styles(),
                java.time.Instant.now(),
                ""
        ));
    }

    @Test
    void authorizedVoiceWithSampleRequiresConsentNote() {
        assertThrows(IllegalArgumentException.class, () -> new VoiceProfile(
                "VOC-FRIEND",
                "Voz amiga",
                VoiceProfileType.AUTHORIZED,
                VoiceEngineType.LOCAL_TTS_PROCESS,
                "es",
                "VOICE-SAMPLE-001",
                "",
                VoiceQualityPreset.HIGH_QUALITY,
                true,
                "",
                Map.of()
        ));
    }

    @Test
    void canAppendStyleWithoutMutatingOriginal() {
        VoiceLibrary library = VoiceLibrary.defaults();
        VoiceLibrary updated = library.withStyle(new PerformanceStyle("STY-WHISPER", "Susurrado", "Intención suave", true, Map.of()));

        assertEquals(8, library.styles().size());
        assertEquals(9, updated.styles().size());
        assertTrue(updated.styleById("STY-WHISPER").isPresent());
    }

    @Test
    void storesReferenceSampleSetsByVoiceWithoutMutatingOriginal() {
        VoiceReferenceSample neutral = new VoiceReferenceSample(
                "VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-NEUTRAL",
                "VOC-OWN-PLACEHOLDER",
                VoiceReferenceTone.NEUTRAL,
                "voices/samples/neutral.wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                0,
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );

        VoiceLibrary library = mutableLegacyVoiceLibrary();
        VoiceLibrary updated = library.withReferenceSample(neutral);

        assertTrue(library.referenceSampleSets().isEmpty());
        assertEquals(1, updated.referenceSampleSets().size());
        assertTrue(updated.referenceSampleSetByVoiceId("VOC-OWN-PLACEHOLDER").orElseThrow()
                .sampleFor(VoiceReferenceTone.NEUTRAL).isPresent());
    }

    private static VoiceLibrary mutableLegacyVoiceLibrary() {
        VoiceLibrary defaults = VoiceLibrary.defaults();
        return new VoiceLibrary(
                "VOICE-LIBRARY-TEST",
                List.of(VoiceProfile.predefinedNarrator(), VoiceProfile.ownVoicePlaceholder()),
                defaults.characters(),
                defaults.styles(),
                List.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                "Biblioteca de prueba"
        );
    }
}
