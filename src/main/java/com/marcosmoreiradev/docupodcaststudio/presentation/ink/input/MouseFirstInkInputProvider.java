package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import javafx.scene.Node;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps JavaFX mouse/touch as the guaranteed baseline while native stylus
 * providers add pressure and eraser data when they actually emit packets.
 */
final class MouseFirstInkInputProvider implements InkInputProvider {
    private static final long NATIVE_SUPPRESSION_NANOS = 150_000_000L;

    private final InkInputProvider mouseProvider;
    private final InkInputProvider nativeProvider;
    private final AtomicLong lastNativeStrokeNanos = new AtomicLong(0L);

    MouseFirstInkInputProvider(InkInputProvider nativeProvider) {
        this(new JavaFxMouseInputProvider(), nativeProvider);
    }

    MouseFirstInkInputProvider(InkInputProvider mouseProvider, InkInputProvider nativeProvider) {
        this.mouseProvider = mouseProvider == null ? new JavaFxMouseInputProvider() : mouseProvider;
        this.nativeProvider = nativeProvider == null ? NoopInkInputProvider.INSTANCE : nativeProvider;
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
        mouseProvider.detach();
        nativeProvider.detach();
    }

    private boolean nativeRecentlyActive() {
        long last = lastNativeStrokeNanos.get();
        return last > 0L && System.nanoTime() - last < NATIVE_SUPPRESSION_NANOS;
    }

    private void markNativeStroke() {
        lastNativeStrokeNanos.set(System.nanoTime());
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
            return !nativeRecentlyActive() && delegate.onStrokeStart(sample);
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            return !nativeRecentlyActive() && delegate.onStrokeMove(sample);
        }

        @Override
        public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
            return !nativeRecentlyActive() && delegate.onStrokeMoveBatch(samples);
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            return !nativeRecentlyActive() && delegate.onStrokeEnd(sample);
        }
    }

    private final class NativeListener implements InkInputListener {
        private final InkInputListener delegate;

        private NativeListener(InkInputListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onHover(InkInputSample sample) {
            delegate.onHover(sample);
        }

        @Override
        public boolean onStrokeStart(InkInputSample sample) {
            markNativeStroke();
            return delegate.onStrokeStart(sample);
        }

        @Override
        public boolean onStrokeMove(InkInputSample sample) {
            markNativeStroke();
            return delegate.onStrokeMove(sample);
        }

        @Override
        public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
            markNativeStroke();
            return delegate.onStrokeMoveBatch(samples);
        }

        @Override
        public boolean onStrokeEnd(InkInputSample sample) {
            markNativeStroke();
            return delegate.onStrokeEnd(sample);
        }
    }
}
