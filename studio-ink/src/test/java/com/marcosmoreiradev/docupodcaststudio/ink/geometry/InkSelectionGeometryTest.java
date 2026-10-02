package com.marcosmoreiradev.docupodcaststudio.ink.geometry;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InkSelectionGeometryTest {
    @Test
    void hitTestingUsesStrokeBoundedSafetyBand() {
        InkStroke stroke = stroke(List.of(p(10, 10), p(110, 10)));
        assertTrue(InkSelectionGeometry.hitTest(stroke, 55, 13));
        assertFalse(InkSelectionGeometry.hitTest(stroke, 55, 80));
    }

    @Test
    void onlyClosedContoursAreFillCandidates() {
        InkStroke closed = stroke(List.of(p(10, 10), p(90, 10), p(90, 90), p(10, 10)));
        InkStroke open = stroke(List.of(p(10, 10), p(90, 10), p(90, 90)));
        assertTrue(InkSelectionGeometry.isClosed(closed));
        var contour = InkSelectionGeometry.closedContour(closed);
        assertTrue(contour.isPresent());
        assertTrue(InkSelectionGeometry.contains(contour.orElseThrow(), 40, 30));
        assertFalse(InkSelectionGeometry.contains(contour.orElseThrow(), 120, 30));
        assertFalse(InkSelectionGeometry.isClosed(open));
        assertTrue(InkSelectionGeometry.closedContour(open).isEmpty());
    }

    @Test
    void transformRotatesAroundOwnCenterAndPreservesMetadata() {
        InkStroke source = stroke(List.of(InkPoint.of(0, 0, 1, .5), InkPoint.of(10, 0, 2, .7)));
        InkStroke result = InkSelectionGeometry.transform(source,
                new InkSelectionGeometry.Transform(5, 2, 1, 1, 90));
        assertEquals(10.0, result.points().get(0).x(), 0.0001);
        assertEquals(-3.0, result.points().get(0).y(), 0.0001);
        assertEquals(1L, result.points().get(0).nanos());
        assertEquals(.7, result.points().get(1).pressure(), 0.0001);
    }

    private static InkStroke stroke(List<InkPoint> points) {
        return InkStroke.draw("#000000ff", 4, points);
    }

    private static InkPoint p(double x, double y) {
        return InkPoint.of(x, y, 0L);
    }
}
