package com.marcosmoreiradev.docupodcaststudio.ink.input;

import org.junit.jupiter.api.Test;
import org.lecturestudio.stylus.StylusButton;
import org.lecturestudio.stylus.StylusCursor;
import org.lecturestudio.stylus.StylusEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LectureStudioStylusInputProviderTest {
    @Test
    void mapsPenPressureAndCoordinatesFromSyntheticStylusEvent() {
        StylusEvent event = event(StylusButton.LEFT, StylusCursor.PEN, 42.5, 91.25, 0.62);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, true);

        assertEquals(42.5, sample.x(), 0.001);
        assertEquals(91.25, sample.y(), 0.001);
        assertEquals(0.62, sample.pressure(), 0.001);
        assertEquals(0.62, sample.rawPressure(), 0.001);
        assertEquals("LectureStudio", sample.inputSource());
        assertEquals(InkInputCursor.PEN, sample.cursor());
        assertTrue(sample.primaryButtonDown());
        assertFalse(sample.requestsEraser());
        assertTrue(LectureStudioStylusInputProvider.isPrimaryButton(event));
    }

    @Test
    void mapsEraserCursorAndNonPrimaryButtonHonesty() {
        StylusEvent event = event(StylusButton.RIGHT, StylusCursor.ERASER, 12.0, 18.0, 0.40);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, false);

        assertEquals(InkInputCursor.ERASER, sample.cursor());
        assertTrue(sample.requestsEraser());
        assertFalse(sample.primaryButtonDown());
        assertFalse(LectureStudioStylusInputProvider.isPrimaryButton(event));
    }

    @Test
    void normalizesLegacyRawPressureRangesWithoutZeroWidthStrokes() {
        StylusEvent event = event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 512.0);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, true);

        assertEquals(0.5, sample.pressure(), 0.001);
    }

    @Test
    void normalizesObservedTabletPressureRangesWithoutSaturating() {
        assertEquals(0.5, LectureStudioStylusInputProvider
                .sampleFromStylusEvent(event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 2048.0), true)
                .pressure(), 0.001);
        assertEquals(0.5, LectureStudioStylusInputProvider
                .sampleFromStylusEvent(event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 32768.0), true)
                .pressure(), 0.001);
    }

    @Test
    void normalizesAnyLegacyPressureAboveOneAsTabletRange() {
        StylusEvent event = event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 2.0);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, true);

        assertEquals(2.0 / 1024.0, sample.pressure(), 0.0001);
    }

    @Test
    void keepsZeroPressureAsSoftPressureInsteadOfPromotingToFullWidth() {
        StylusEvent event = event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 0.0);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, true);

        assertEquals(0.0, sample.pressure(), 0.001);
    }

    @Test
    void stylusContactRequiresPositivePressureSoHoverDoesNotDraw() {
        StylusEvent hover = event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 0.0);
        StylusEvent noise = event(StylusButton.LEFT, StylusCursor.PEN, 1.0, 2.0, 0.005);
        StylusEvent contact = event(StylusButton.NONE, StylusCursor.PEN, 1.0, 2.0, 0.45);

        assertFalse(LectureStudioStylusInputProvider.hasInkContact(hover));
        assertFalse(LectureStudioStylusInputProvider.hasInkContact(noise));
        assertTrue(LectureStudioStylusInputProvider.hasInkContact(contact));
    }

    @Test
    void mouseContactRequiresTrackedPrimaryButtonState() {
        StylusEvent hover = event(StylusButton.NONE, StylusCursor.MOUSE, 1.0, 2.0, 1.0);
        StylusEvent contact = event(StylusButton.LEFT, StylusCursor.MOUSE, 1.0, 2.0, 1.0);

        assertFalse(LectureStudioStylusInputProvider.hasInkContact(hover));
        assertFalse(LectureStudioStylusInputProvider.hasInkContact(contact));
        assertTrue(LectureStudioStylusInputProvider.hasInkContact(contact, true));
    }

    @Test
    void mouseMovePacketsCanKeepContactFromPriorButtonDown() {
        StylusEvent moveWithoutButton = event(StylusButton.NONE, StylusCursor.MOUSE, 1.0, 2.0, 1.0);

        assertFalse(LectureStudioStylusInputProvider.hasInkContact(moveWithoutButton, false));
        assertTrue(LectureStudioStylusInputProvider.hasInkContact(moveWithoutButton, true));
    }

    @Test
    void mapsLectureStudioMousePacketHonestlyWithoutPromotingItToPen() {
        StylusEvent event = event(StylusButton.LEFT, StylusCursor.MOUSE, 7.0, 9.0, 1.0);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, true);

        assertEquals(InkInputCursor.MOUSE, sample.cursor());
        assertEquals(1.0, sample.rawPressure(), 0.001);
        assertEquals(1.0, sample.pressure(), 0.001);
        assertEquals("LectureStudio", sample.inputSource());
    }

    @Test
    void mousePacketUsesFullWidthWhenNativePressureIsZero() {
        StylusEvent event = event(StylusButton.LEFT, StylusCursor.MOUSE, 7.0, 9.0, 0.0);

        InkInputSample sample = LectureStudioStylusInputProvider.sampleFromStylusEvent(event, true);

        assertEquals(InkInputCursor.MOUSE, sample.cursor());
        assertEquals(0.0, sample.rawPressure(), 0.001);
        assertEquals(1.0, sample.pressure(), 0.001);
    }

    private static StylusEvent event(StylusButton button, StylusCursor cursor,
                                     double x, double y, double pressure) {
        return new StylusEvent(button, cursor, new double[]{x, y, pressure, 0.0, 0.0, 0.0, 0.0});
    }
}
