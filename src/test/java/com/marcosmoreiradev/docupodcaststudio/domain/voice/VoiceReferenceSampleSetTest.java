package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceReferenceSampleSetTest {
    @Test
    void advancedVoiceRequiresNeutralSample() {
        VoiceReferenceSample happy = sample("S-HAPPY", VoiceReferenceTone.HAPPY, "voices/maria-happy.wav");

        assertThrows(IllegalArgumentException.class,
                () -> VoiceReferenceSampleSet.forAdvancedVoice("VOC-MARIA", List.of(happy)));
    }

    @Test
    void missingToneFallsBackToNeutralSample() {
        VoiceReferenceSample neutral = sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL, "voices/maria-neutral.wav");
        VoiceReferenceSampleSet set = VoiceReferenceSampleSet.forAdvancedVoice("VOC-MARIA", List.of(neutral));

        assertTrue(set.missingToneUsesNeutral(VoiceReferenceTone.HEROIC));
        assertSame(neutral, set.sampleForOrNeutral(VoiceReferenceTone.HEROIC));
        assertFalse(set.missingToneUsesNeutral(VoiceReferenceTone.NEUTRAL));
    }

    @Test
    void rejectsDuplicatedToneSamples() {
        VoiceReferenceSample one = sample("S-NEUTRAL-1", VoiceReferenceTone.NEUTRAL, "voices/one.wav");
        VoiceReferenceSample two = sample("S-NEUTRAL-2", VoiceReferenceTone.NEUTRAL, "voices/two.wav");

        assertThrows(IllegalArgumentException.class,
                () -> new VoiceReferenceSampleSet("VOC-MARIA", List.of(one, two)));
    }

    @Test
    void ownershipControlsManagedDeletion() {
        VoiceReferenceSample managed = new VoiceReferenceSample(
                "S-MANAGED",
                "VOC-MARIA",
                VoiceReferenceTone.NEUTRAL,
                "voices/maria-neutral.wav",
                VoiceSampleOrigin.RECORDED_IN_APP,
                VoiceFileOwnership.PROJECT_ASSET,
                3000,
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );
        VoiceReferenceSample external = new VoiceReferenceSample(
                "S-EXTERNAL",
                "VOC-MARIA",
                VoiceReferenceTone.HAPPY,
                "C:/Users/Omar/Desktop/maria-happy.wav",
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.EXTERNAL_REFERENCE,
                3000,
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );

        assertTrue(managed.canDeleteManagedFile());
        assertFalse(external.canDeleteManagedFile());
        assertEquals(VoiceReferenceTone.HAPPY, external.tone());
    }

    private static VoiceReferenceSample sample(String id, VoiceReferenceTone tone, String path) {
        return new VoiceReferenceSample(
                id,
                "VOC-MARIA",
                tone,
                path,
                VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET,
                2500,
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );
    }
}
