package com.marcosmoreiradev.docupodcaststudio.domain.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentAudioPreparationExtentTest {
    @Test
    void mapsTheDescriptiveScaleToStableFragmentLimits() {
        assertEquals(12, DocumentAudioPreparationExtent.FEW.maximumVoiceFragments());
        assertEquals(30, DocumentAudioPreparationExtent.SHORT_READING.maximumVoiceFragments());
        assertEquals(75, DocumentAudioPreparationExtent.MEDIUM_READING.maximumVoiceFragments());
        assertEquals(150, DocumentAudioPreparationExtent.LARGE_READING.maximumVoiceFragments());
        assertEquals(0, DocumentAudioPreparationExtent.ALL_FROM_SELECTION.maximumVoiceFragments());
        assertEquals(1, DocumentAudioPreparationExtent.SINGLE_FRAGMENT.maximumVoiceFragments());
        assertFalse(DocumentAudioPreparationExtent.LARGE_READING.allFromSelection());
        assertTrue(DocumentAudioPreparationExtent.ALL_FROM_SELECTION.allFromSelection());
    }

    @Test
    void snapsSliderValuesToTheNearestNamedLevel() {
        assertEquals(DocumentAudioPreparationExtent.FEW,
                DocumentAudioPreparationExtent.fromSliderValue(-2));
        assertEquals(DocumentAudioPreparationExtent.MEDIUM_READING,
                DocumentAudioPreparationExtent.fromSliderValue(2.2));
        assertEquals(DocumentAudioPreparationExtent.ALL_FROM_SELECTION,
                DocumentAudioPreparationExtent.fromSliderValue(99));
    }
}
