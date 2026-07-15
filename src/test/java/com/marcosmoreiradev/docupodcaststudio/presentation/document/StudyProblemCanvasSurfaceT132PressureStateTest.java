package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyProblemCanvasSurfaceT132PressureStateTest {
    @Test
    void editableInkStateKeepsPointPressure() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java"));

        assertTrue(source.contains("InkPoint.of(point.x(), point.y(), point.nanos(), point.pressure())"));
        assertTrue(source.contains("new InkPointState(point.x(), point.y(), point.nanos(), point.pressure())"));
        assertTrue(source.contains("record InkPointState(double x, double y, long nanos, double pressure)"));
        assertTrue(source.contains("InkPointState(double x, double y, long nanos)"));
        assertTrue(source.contains("private static double safePressure(double pressure)"));
        assertTrue(source.contains("double strokeWidth = visibleWidth(width);"));
        assertFalse(source.contains("double strokeWidth = Math.max(1.0, width);"));
    }
}
