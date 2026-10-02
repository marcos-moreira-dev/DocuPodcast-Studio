package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceReferenceToneLayerTargetTest {
    @Test void spanishLabelsAndAngryLegacyIdResolveWithoutNeutralFallback() {
        for (String label : java.util.List.of("enojado","Enojada","TONE-ANGRY","STY-ANGRY","tone-angry"))
            assertEquals(VoiceReferenceTone.ANGRY,VoiceReferenceTone.fromLayerTargetId(label).orElseThrow());
        assertEquals(VoiceReferenceTone.EUPHORIC,VoiceReferenceTone.fromLayerTargetId("eufórico").orElseThrow());
        assertTrue(VoiceReferenceTone.fromLayerTargetId("enojaod").isEmpty());
    }
    @Test
    void toneLayerTargetRoundTripsFromStableProjectId() {
        assertEquals("TONE-HEROIC", VoiceReferenceTone.HEROIC.layerTargetId());
        assertTrue(VoiceReferenceTone.fromLayerTargetId("TONE-HEROIC").isPresent());
        assertEquals(VoiceReferenceTone.HEROIC, VoiceReferenceTone.fromLayerTargetId("TONE-HEROIC").orElseThrow());
        assertEquals(VoiceReferenceTone.HEROIC, VoiceReferenceTone.fromLayerTargetId("HEROIC").orElseThrow());
    }

    @Test
    void legacyPerformanceStylesResolveToReferenceTones() {
        assertEquals(VoiceReferenceTone.HAPPY, VoiceReferenceTone.fromLayerTargetId("STY-HAPPY").orElseThrow());
        assertEquals(VoiceReferenceTone.HAPPY, VoiceReferenceTone.fromLayerTargetId("STY-CHEERFUL").orElseThrow());
        assertEquals(VoiceReferenceTone.DRAMATIC, VoiceReferenceTone.fromLayerTargetId("STY-DRAMATIC").orElseThrow());
        assertEquals(VoiceReferenceTone.CALM, VoiceReferenceTone.fromLayerTargetId("STY-WARM").orElseThrow());
    }
}
