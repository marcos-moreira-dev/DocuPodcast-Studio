package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.ink.InkRealtimeStrokeEngine;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputSample;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.JavaFxMouseInputProvider;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyProblemCanvasMouseInkIntegrationTest {
    @Test
    void mouseEventsOnInkInputLayerCommitEditableStrokeToCanvasSurface() {
        StudyProblemCanvasSurface surface = new StudyProblemCanvasSurface();
        JavaFxMouseInputProvider provider = new JavaFxMouseInputProvider();
        InkRealtimeStrokeEngine engine = engineFor(surface);

        surface.inkInputLayer().setMouseTransparent(false);
        surface.inkInputLayer().setPickOnBounds(true);
        surface.inkInputTarget().setMouseTransparent(false);
        surface.inkInputTarget().setVisible(true);
        surface.inkInputTarget().setDisable(false);
        provider.attach(surface.inkInputLayer(), new InkInputListener() {
            @Override
            public boolean onStrokeStart(InkInputSample sample) {
                Point2D point = point(sample);
                engine.begin(point.getX(), point.getY(), sample.nanos(), Color.BLACK, 3.0, false, sample.pressure());
                return sample.cursor() == InkInputCursor.MOUSE;
            }

            @Override
            public boolean onStrokeMove(InkInputSample sample) {
                Point2D point = point(sample);
                engine.move(point.getX(), point.getY(), sample.nanos(), Color.BLACK, 3.0, false, sample.pressure());
                return true;
            }

            @Override
            public boolean onStrokeEnd(InkInputSample sample) {
                Point2D point = point(sample);
                engine.end(point.getX(), point.getY(), sample.nanos(), Color.BLACK, 3.0, false, sample.pressure());
                return true;
            }
        });

        surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 80, 90, true));
        surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 120, 130, true));
        surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 150, 160, false));
        engine.flushAll();

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(1, strokes.size());
        assertEquals("DRAW", strokes.getFirst().type());
        assertTrue(strokes.getFirst().points().size() >= 3);
    }

    private static InkRealtimeStrokeEngine engineFor(StudyProblemCanvasSurface surface) {
        return new InkRealtimeStrokeEngine(new InkRealtimeStrokeEngine.Sink() {
            @Override
            public void beginLiveStroke() {
                surface.beginLiveStroke();
            }

            @Override
            public void previewLine(double x1, double y1, double x2, double y2,
                                    Color color, double width, boolean erase) {
                surface.previewLine(x1, y1, x2, y2, color, width, erase);
            }

            @Override
            public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                                         double endX, double endY, Color color, double width, boolean erase) {
                surface.previewQuadratic(startX, startY, controlX, controlY, endX, endY, color, width, erase);
            }

            @Override
            public void commitStroke(InkRealtimeStrokeEngine.CommittedStroke stroke) {
                surface.commitInkStroke(toCanvasStrokeState(stroke));
            }
        });
    }

    private static StudyProblemCanvasSurface.InkStrokeState toCanvasStrokeState(
            InkRealtimeStrokeEngine.CommittedStroke stroke) {
        return new StudyProblemCanvasSurface.InkStrokeState(
                stroke.erase() ? "ERASE" : "DRAW",
                "#000000ff",
                stroke.width(),
                stroke.points().stream()
                        .map(point -> new StudyProblemCanvasSurface.InkPointState(
                                point.x(),
                                point.y(),
                                point.nanos(),
                                point.pressure()))
                        .toList());
    }

    private static Point2D point(InkInputSample sample) {
        return new Point2D(sample.x(), sample.y());
    }

    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type,
                                    double x,
                                    double y,
                                    boolean primaryDown) {
        return new MouseEvent(
                type,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                primaryDown ? 1 : 0,
                false,
                false,
                false,
                false,
                primaryDown,
                false,
                false,
                false,
                false,
                false,
                null);
    }
}
