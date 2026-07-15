package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceReferenceToneLayerTargetTest {
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
