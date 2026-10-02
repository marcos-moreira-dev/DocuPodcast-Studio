package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaFxMouseInputProviderTest {
    @Test
    void mousePressedDraggedAndReleasedProduceInkSamples() {
        Pane target = new Pane();
        target.resize(300, 200);
        JavaFxMouseInputProvider provider = new JavaFxMouseInputProvider();
        RecordingListener recording = new RecordingListener();

        provider.attach(target, recording);
        target.fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 20, 30, true));
        target.fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 36, 48, true));
        target.fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 42, 54, false));

        assertEquals(List.of("start:MOUSE", "move:MOUSE", "end:MOUSE"), recording.events);
        assertTrue(recording.points.stream().allMatch(point -> point.x() >= 20.0 && point.y() >= 30.0));
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

    private static final class RecordingListener implements InkInputListener {
        private final List<String> events = new ArrayList<>();
        private final List<InkInputSample> points = new ArrayList<>();

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            events.add("start:" + sample.cursor());
            points.add(sample);
            return true;
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            events.add("move:" + sample.cursor());
            points.add(sample);
            return true;
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            events.add("end:" + sample.cursor());
            points.add(sample);
            return true;
        }
    }
}
