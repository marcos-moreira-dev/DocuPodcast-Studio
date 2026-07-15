package com.marcosmoreiradev.docupodcaststudio.presentation.ink;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkRealtimeStrokeEngineT132PressureSourceTest {
    @Test
    void pressureDoesNotFragmentActiveStroke() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java"));

        assertFalse(source.contains("Math.abs(point.width() - width) > 0.01"));
        assertTrue(source.contains("width = safeWidth(point.width());"));
        assertTrue(source.contains("pressure = safePressure(point.pressure());"));
        assertTrue(source.contains("record CommittedPoint(double x, double y, long nanos, double pressure)"));
        assertTrue(source.contains("new CommittedPoint(point.x(), point.y(), point.nanos(), pressure)"));
    }
}
