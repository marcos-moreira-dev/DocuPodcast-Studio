package com.marcosmoreiradev.docupodcaststudio.ink;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkRealtimeStrokeEnginePressureTest {
    @Test
    void pressureControlsPreviewWidthWithinConfiguredMaximumImmediately() {
        CapturingSink sink = new CapturingSink();
        InkRealtimeStrokeEngine engine = new InkRealtimeStrokeEngine(sink);

        engine.begin(10, 10, 1, Color.BLACK, 12.0, false, 0.20);
        engine.move(40, 10, 2, Color.BLACK, 12.0, false, 1.0);
        engine.end(60, 10, 3, Color.BLACK, 12.0, false, 0.40);

        assertTrue(sink.widths.size() >= 3);
        assertTrue(sink.widths.stream().allMatch(width -> width <= 12.0));
        assertTrue(sink.widths.stream().anyMatch(width -> width < 6.0));
        assertEquals(1, sink.commits);
        assertEquals(0, sink.quadratics);
    }

    @Test
    void brushMathMapsPressureLinearlyFromZeroToConfiguredMaximum() {
        assertEquals(0.0, InkBrushMath.pressureWidth(10.0, 0.0), 0.001);
        assertEquals(5.0, InkBrushMath.pressureWidth(10.0, 0.5), 0.001);
        assertEquals(10.0, InkBrushMath.pressureWidth(10.0, 1.0), 0.001);
    }

    @Test
    void rapidInputBurstProducesPreviewSynchronouslyBeforeAnimationFrame() {
        CapturingSink sink = new CapturingSink();
        InkRealtimeStrokeEngine engine = new InkRealtimeStrokeEngine(sink);

        engine.begin(0, 0, 1, Color.BLACK, 6.0, false, 1.0);
        for (int i = 1; i <= 220; i++) {
            engine.move(i * 2.0, Math.sin(i / 8.0) * 12.0, i + 1, Color.BLACK, 6.0, false, 1.0);
        }

        assertTrue(sink.widths.size() > 100);
    }

    private static final class CapturingSink implements InkRealtimeStrokeEngine.Sink {
        private final List<Double> widths = new ArrayList<>();
        private int commits;
        private int quadratics;

        @Override
        public void beginLiveStroke() {
        }

        @Override
        public void previewLine(double x1, double y1, double x2, double y2,
                                Color color, double width, boolean erase) {
            widths.add(width);
        }

        @Override
        public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                                     double endX, double endY, Color color, double width, boolean erase) {
            quadratics++;
            widths.add(width);
        }

        @Override
        public void commitStroke(InkRealtimeStrokeEngine.CommittedStroke stroke) {
            commits++;
        }
    }
}
