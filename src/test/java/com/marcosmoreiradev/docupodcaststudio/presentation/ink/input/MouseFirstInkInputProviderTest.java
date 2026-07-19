package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MouseFirstInkInputProviderTest {
    @Test
    void forwardsMouseWhenNoNativeStrokeIsActive() {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(
                InkInputCapabilities.javafxMouse("native unavailable"));
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);
        RecordingListener recording = new RecordingListener();

        provider.attach(new Pane(), recording);
        mouse.fireStart(mouseSample());
        mouse.fireMove(mouseSample());
        mouse.fireEnd(mouseSample());

        assertEquals(List.of("start:MOUSE", "move:MOUSE", "end:MOUSE"), recording.events);
    }

    @Test
    void nativeStrokeTemporarilySuppressesDuplicateMouseFallback() {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(InkInputCapabilities.lectureStudioStylus());
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);
        RecordingListener recording = new RecordingListener();

        provider.attach(new Pane(), recording);
        nativeProvider.fireStart(nativeSample());
        mouse.fireStart(mouseSample());

        assertEquals(List.of("start:PEN"), recording.events);
    }

    @Test
    void resetCoordinatesReachesNativeProviderOnly() {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(InkInputCapabilities.lectureStudioStylus());
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);

        provider.resetCoordinateState();

        assertEquals(0, mouse.resetCount);
        assertEquals(1, nativeProvider.resetCount);
    }

    private static InkInputSample mouseSample() {
        return new InkInputSample(10, 10, System.nanoTime(), 1.0, InkInputCursor.MOUSE, true, false);
    }

    private static InkInputSample nativeSample() {
        return new InkInputSample(10, 10, System.nanoTime(), 0.5, InkInputCursor.PEN, true, false);
    }

    private static final class CapturingProvider implements InkInputProvider {
        private final InkInputCapabilities capabilities;
        private InkInputListener listener;
        private int resetCount;

        private CapturingProvider(InkInputCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        @Override
        public InkInputCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public void attach(Node target, InkInputListener listener) {
            this.listener = listener;
        }

        @Override
        public void detach() {
            listener = null;
        }

        @Override
        public void resetCoordinateState() {
            resetCount++;
        }

        private void fireStart(InkInputSample sample) {
            listener.onStrokeStart(sample);
        }

        private void fireMove(InkInputSample sample) {
            listener.onStrokeMove(sample);
        }

        private void fireEnd(InkInputSample sample) {
            listener.onStrokeEnd(sample);
        }
    }

    private static final class RecordingListener implements InkInputListener {
        private final List<String> events = new ArrayList<>();

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            events.add("start:" + sample.cursor());
            return true;
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            events.add("move:" + sample.cursor());
            return true;
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            events.add("end:" + sample.cursor());
            return true;
        }
    }
}
