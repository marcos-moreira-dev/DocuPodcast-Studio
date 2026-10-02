package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MouseFirstInkInputProviderTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void forwardsMouseWhenNoNativeStrokeIsActive() throws Exception {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(
                InkInputCapabilities.javafxMouse("native unavailable"));
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);
        RecordingListener recording = new RecordingListener();

        onFx(() -> {
            provider.attach(new Pane(), recording);
            mouse.fireStart(mouseSample());
            mouse.fireMove(mouseSample());
            mouse.fireEnd(mouseSample());
        });

        assertTrue(recording.completed.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("start:MOUSE", "move:MOUSE", "end:MOUSE"), recording.events);
    }

    @Test
    void nativeMouseCannotReplaceJavaFxCoordinates() throws Exception {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(InkInputCapabilities.lectureStudioStylus());
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);
        RecordingListener recording = new RecordingListener();
        InkInputSample displaced = new InkInputSample(210, 60, System.nanoTime(), 1,
                InkInputCursor.MOUSE, true, false);
        onFx(() -> {
            provider.attach(new Pane(), recording);
            mouse.fireStart(mouseSample());
            nativeProvider.fireStart(displaced);
            nativeProvider.fireMove(displaced);
            mouse.fireMove(mouseSample());
            nativeProvider.fireEnd(displaced);
            mouse.fireEnd(mouseSample());
        });
        assertTrue(recording.completed.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("start:MOUSE", "move:MOUSE", "end:MOUSE"), recording.events);
        assertTrue(recording.samples.stream().allMatch(sample -> sample.x() == 10 && sample.y() == 10));
        onFx(provider::detach);
    }

    @Test
    void nativeStrokeTemporarilySuppressesDuplicateMouseFallback() throws Exception {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(InkInputCapabilities.lectureStudioStylus());
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);
        RecordingListener recording = new RecordingListener();

        onFx(() -> {
            provider.attach(new Pane(), recording);
            nativeProvider.fireStart(nativeSample());
            mouse.fireStart(mouseSample());
        });

        assertEquals(List.of("start:PEN"), recording.events);
    }

    @Test
    void nativePenReplacesMouseStartStillInsideTheArbitrationWindow() throws Exception {
        CapturingProvider mouse = new CapturingProvider(InkInputCapabilities.javafxMouse());
        CapturingProvider nativeProvider = new CapturingProvider(InkInputCapabilities.lectureStudioStylus());
        MouseFirstInkInputProvider provider = new MouseFirstInkInputProvider(mouse, nativeProvider);
        RecordingListener recording = new RecordingListener();

        onFx(() -> {
            provider.attach(new Pane(), recording);
            mouse.fireStart(mouseSample());
            nativeProvider.fireStart(nativeSample());
            nativeProvider.fireMove(nativeSample());
            nativeProvider.fireEnd(nativeSample());
        });

        Thread.sleep(60);
        assertEquals(List.of("start:PEN", "move:PEN", "end:PEN"), recording.events);
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

    private static void onFx(Runnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
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
        private final List<InkInputSample> samples = new ArrayList<>();
        private final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            events.add("start:" + sample.cursor());
            samples.add(sample);
            return true;
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            events.add("move:" + sample.cursor());
            samples.add(sample);
            return true;
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            events.add("end:" + sample.cursor());
            samples.add(sample);
            completed.countDown();
            return true;
        }
    }
}
