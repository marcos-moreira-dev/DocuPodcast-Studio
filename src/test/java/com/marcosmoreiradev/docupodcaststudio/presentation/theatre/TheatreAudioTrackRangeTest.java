package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TheatreAudioTrackRangeTest {
    @Test
    void acceptsOrderedRangeAndRejectsInvalidMarkers() {
        assertDoesNotThrow(() -> TheatreAudioTrackPanel.validateRange(
                1.0, 4.0, TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 8.0));
        assertThrows(IllegalArgumentException.class, () -> TheatreAudioTrackPanel.validateRange(
                4.0, 4.0, TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 8.0));
        assertThrows(IllegalArgumentException.class, () -> TheatreAudioTrackPanel.validateRange(
                5.0, 0.0, TheatreProjectLayer.AudioTrackEndMode.FILE_END, 5.0));
        assertThrows(IllegalArgumentException.class, () -> TheatreAudioTrackPanel.validateRange(
                1.0, 9.0, TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 8.0));
    }
}
