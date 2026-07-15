package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NativeInkCoordinateSpaceResolverTest {
    @AfterEach
    void clearForcedSpace() {
        System.clearProperty(NativeInkCoordinateSpaceResolver.COORDINATE_SPACE_PROPERTY);
    }

    @Test
    void forcedTargetLocalCoordinatesDoNotSubtractSceneOffset() {
        System.setProperty(NativeInkCoordinateSpaceResolver.COORDINATE_SPACE_PROPERTY, "target-local");
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 40, 50, false);

        assertTrue(result.accepted());
        assertEquals(40, result.point().getX(), 0.001);
        assertEquals(50, result.point().getY(), 0.001);
        assertEquals(NativeInkCoordinateSpaceResolver.CoordinateSpace.TARGET_LOCAL, result.space());
    }

    @Test
    void forcedWindowClientCoordinatesUseSceneTransform() {
        System.setProperty(NativeInkCoordinateSpaceResolver.COORDINATE_SPACE_PROPERTY, "window-client-logical");
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 80, 40, false);

        assertTrue(result.accepted());
        assertEquals(50, result.point().getX(), 0.001);
        assertEquals(30, result.point().getY(), 0.001);
        assertEquals(NativeInkCoordinateSpaceResolver.CoordinateSpace.WINDOW_CLIENT_LOGICAL, result.space());
    }

    @Test
    void autoModeRejectsAmbiguousPenPacketsWithoutAnchor() {
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 80, 40, false, InkInputCursor.PEN);

        assertFalse(result.accepted());
        assertFalse(result.rejectionReason().isBlank());
    }

    @Test
    void autoModeRejectsAmbiguousMousePacketsWithoutAnchor() {
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 80, 40, false, InkInputCursor.MOUSE);

        assertFalse(result.accepted());
        assertFalse(result.rejectionReason().isBlank());
    }

    @Test
    void autoModeUsesRecentJavaFxAnchorToDisambiguateCoordinateSpace() {
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        resolver.recordJavaFxAnchor(target, 80, 40);
        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 80, 40, false);

        assertTrue(result.accepted());
        assertEquals(NativeInkCoordinateSpaceResolver.CoordinateSpace.WINDOW_CLIENT_LOGICAL, result.space());
        assertEquals(50, result.point().getX(), 0.001);
        assertEquals(30, result.point().getY(), 0.001);
    }

    @Test
    void clearLockedSpaceRetainsRecentJavaFxAnchorForNextStroke() {
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        resolver.recordJavaFxAnchor(target, 80, 40);
        resolver.clearLockedSpace();
        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 50, 30, false, InkInputCursor.MOUSE);

        assertTrue(result.accepted());
        assertEquals(NativeInkCoordinateSpaceResolver.CoordinateSpace.TARGET_LOCAL, result.space());
        assertEquals(50, result.point().getX(), 0.001);
        assertEquals(30, result.point().getY(), 0.001);
    }

    @Test
    void autoModeRejectsAmbiguousPacketWhenJavaFxAnchorIsOutsideTarget() {
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        resolver.recordJavaFxAnchor(target, 10, 10);
        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 80, 40, false);

        assertFalse(result.accepted());
        assertFalse(result.rejectionReason().isBlank());
    }

    @Test
    void rejectsPacketsOutsideEverySupportedSpace() {
        Rectangle target = targetAtSceneOffset(30, 10);
        NativeInkCoordinateSpaceResolver resolver = new NativeInkCoordinateSpaceResolver();

        NativeInkCoordinateSpaceResolver.TranslationResult result =
                resolver.translate(target, 10_000, 10_000, false);

        assertFalse(result.accepted());
        assertFalse(result.rejectionReason().isBlank());
    }

    private static Rectangle targetAtSceneOffset(double x, double y) {
        Rectangle target = new Rectangle(0, 0, 100, 100);
        target.setTranslateX(x);
        target.setTranslateY(y);
        return target;
    }
}
