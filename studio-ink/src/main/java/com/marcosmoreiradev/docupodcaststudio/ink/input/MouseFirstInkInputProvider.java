package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.scene.Node;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps JavaFX mouse/touch available while native stylus providers supply
 * pressure and eraser data. Native packets temporarily suppress synthesized
 * mouse packets so one pen stroke is never rendered twice.
 */
final class MouseFirstInkInputProvider implements InkInputProvider {
    private static final long NATIVE_SUPPRESSION_NANOS = 250_000_000L;
    private static final Duration MOUSE_ARBITRATION_DELAY = Duration.millis(24);

    private final InkInputProvider mouseProvider;
    private final InkInputProvider nativeProvider;
    private final AtomicLong lastNativeStrokeNanos = new AtomicLong(0L);
    private final PauseTransition mouseDelay = new PauseTransition(MOUSE_ARBITRATION_DELAY);
    private final List<PendingMouseEvent> pendingMouseEvents = new ArrayList<>();
    private boolean mouseStrokeActive;
    private boolean nativeStrokeActive;
    private InkInputSample lastMouseSample;

    MouseFirstInkInputProvider(InkInputProvider nativeProvider) {
        this(new JavaFxMouseInputProvider(), nativeProvider);
    }

    MouseFirstInkInputProvider(InkInputProvider mouseProvider, InkInputProvider nativeProvider) {
        this.mouseProvider = mouseProvider == null ? new JavaFxMouseInputProvider() : mouseProvider;
        this.nativeProvider = nativeProvider == null ? NoopInkInputProvider.INSTANCE : nativeProvider;
        mouseDelay.setOnFinished(event -> flushPendingMouse());
    }

    @Override
    public InkInputCapabilities capabilities() {
        InkInputCapabilities nativeCapabilities = nativeProvider.capabilities();
        return nativeCapabilities.nativeProvider() ? nativeCapabilities : mouseProvider.capabilities();
    }

    @Override
    public void attach(Node target, InkInputListener listener) {
        detach();
        if (target == null || listener == null) {
            return;
        }
        mouseProvider.attach(target, new MouseFallbackListener(listener));
        nativeProvider.attach(target, new NativeListener(listener));
    }

    @Override
    public void detach() {
        mouseDelay.stop();
        pendingMouseEvents.clear();
        mouseStrokeActive = false;
        nativeStrokeActive = false;
        lastMouseSample = null;
        mouseProvider.detach();
        nativeProvider.detach();
    }

    @Override
    public void resetCoordinateState() {
        nativeProvider.resetCoordinateState();
    }

    private boolean nativeRecentlyActive() {
        long last = lastNativeStrokeNanos.get();
        return last > 0L && System.nanoTime() - last < NATIVE_SUPPRESSION_NANOS;
    }

    private void markNativeStroke() {
        lastNativeStrokeNanos.set(System.nanoTime());
    }

    private void queueMouse(PendingMouseEvent event) {
        pendingMouseEvents.add(event);
        if (!mouseDelay.getStatus().equals(javafx.animation.Animation.Status.RUNNING)) {
            mouseDelay.playFromStart();
        }
    }

    private void cancelPendingMouse() {
        mouseDelay.stop();
        pendingMouseEvents.clear();
    }

    private void flushPendingMouse() {
        if (nativeRecentlyActive() || pendingMouseEvents.isEmpty()) {
            pendingMouseEvents.clear();
            return;
        }
        List<PendingMouseEvent> events = List.copyOf(pendingMouseEvents);
        pendingMouseEvents.clear();
        for (PendingMouseEvent event : events) {
            switch (event.kind()) {
                case START -> mouseStrokeActive = event.listener().onStrokeStart(event.sample());
                case MOVE -> {
                    if (mouseStrokeActive) event.listener().onStrokeMove(event.sample());
                }
                case BATCH -> {
                    if (mouseStrokeActive) event.listener().onStrokeMoveBatch(event.samples());
                }
                case END -> {
                    if (mouseStrokeActive) event.listener().onStrokeEnd(event.sample());
                    mouseStrokeActive = false;
                }
            }
        }
    }

    private final class MouseFallbackListener implements InkInputListener {
        private final InkInputListener delegate;

        private MouseFallbackListener(InkInputListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onHover(InkInputSample sample) {
            if (!nativeRecentlyActive()) {
                delegate.onHover(sample);
            }
        }

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            if (nativeRecentlyActive() || nativeStrokeActive) return false;
            lastMouseSample = sample;
            queueMouse(PendingMouseEvent.start(delegate, sample));
            return true;
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            if (nativeRecentlyActive() || nativeStrokeActive) return false;
            lastMouseSample = sample;
            if (mouseStrokeActive) return delegate.onStrokeMove(sample);
            if (!pendingMouseEvents.isEmpty()) {
                queueMouse(PendingMouseEvent.move(delegate, sample));
                return true;
            }
            return false;
        }

        @Override
        public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
            if (nativeRecentlyActive() || nativeStrokeActive || samples == null || samples.isEmpty()) return false;
            lastMouseSample = samples.get(samples.size() - 1);
            if (mouseStrokeActive) return delegate.onStrokeMoveBatch(samples);
            if (!pendingMouseEvents.isEmpty()) {
                queueMouse(PendingMouseEvent.batch(delegate, samples));
                return true;
            }
            return false;
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            if (nativeRecentlyActive() || nativeStrokeActive) return false;
            lastMouseSample = sample;
            if (mouseStrokeActive) {
                mouseStrokeActive = false;
                return delegate.onStrokeEnd(sample);
            }
            if (!pendingMouseEvents.isEmpty()) {
                queueMouse(PendingMouseEvent.end(delegate, sample));
                return true;
            }
            return false;
        }
    }

    private final class NativeListener implements InkInputListener {
        private final InkInputListener delegate;

        private NativeListener(InkInputListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onHover(InkInputSample sample) {
            if (sample.cursor() == InkInputCursor.MOUSE) return;
            delegate.onHover(sample);
        }

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            // Native hooks also report ordinary mouse packets in driver coordinates.
            // JavaFX already provides the authoritative transformed mouse position.
            if (sample.cursor() == InkInputCursor.MOUSE) return false;
            markNativeStroke();
            cancelPendingMouse();
            if (mouseStrokeActive && lastMouseSample != null) {
                delegate.onStrokeEnd(lastMouseSample);
                mouseStrokeActive = false;
            }
            nativeStrokeActive = delegate.onStrokeStart(sample);
            return nativeStrokeActive;
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            if (sample.cursor() == InkInputCursor.MOUSE) return false;
            markNativeStroke();
            return nativeStrokeActive && delegate.onStrokeMove(sample);
        }

        @Override
        public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
            if (samples == null || samples.isEmpty()) return false;
            samples = samples.stream().filter(sample -> sample.cursor() != InkInputCursor.MOUSE).toList();
            if (samples.isEmpty()) return false;
            markNativeStroke();
            return nativeStrokeActive && delegate.onStrokeMoveBatch(samples);
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            if (sample.cursor() == InkInputCursor.MOUSE) return false;
            markNativeStroke();
            if (!nativeStrokeActive) return false;
            nativeStrokeActive = false;
            return delegate.onStrokeEnd(sample);
        }
    }

    private enum PendingKind { START, MOVE, BATCH, END }

    private record PendingMouseEvent(PendingKind kind, InkInputListener listener,
                                     InkInputSample sample, List<InkInputSample> samples) {
        static PendingMouseEvent start(InkInputListener listener, InkInputSample sample) {
            return new PendingMouseEvent(PendingKind.START, listener, sample, List.of());
        }

        static PendingMouseEvent move(InkInputListener listener, InkInputSample sample) {
            return new PendingMouseEvent(PendingKind.MOVE, listener, sample, List.of());
        }

        static PendingMouseEvent batch(InkInputListener listener, List<InkInputSample> samples) {
            return new PendingMouseEvent(PendingKind.BATCH, listener, null, List.copyOf(samples));
        }

        static PendingMouseEvent end(InkInputListener listener, InkInputSample sample) {
            return new PendingMouseEvent(PendingKind.END, listener, sample, List.of());
        }
    }
}
