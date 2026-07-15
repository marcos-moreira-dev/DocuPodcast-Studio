package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.value.ChangeListener;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Loads local UI images once and swaps them only after the requested image is ready. */
public final class StableImageLoader {
    private static final int DEFAULT_CACHE_LIMIT = 128;
    private static final String REQUEST_TOKEN = StableImageLoader.class.getName() + ".requestToken";
    private static final String CURRENT_URI = StableImageLoader.class.getName() + ".currentUri";
    private static final String PENDING_URI = StableImageLoader.class.getName() + ".pendingUri";
    private static final StableImageLoader SHARED = new StableImageLoader(DEFAULT_CACHE_LIMIT, Image::new);

    private final int cacheLimit;
    private final ImageFactory imageFactory;
    private final AtomicLong requestSequence = new AtomicLong();
    private final Map<CacheKey, Image> cache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Image> eldest) {
            return size() > StableImageLoader.this.cacheLimit;
        }
    };

    public static StableImageLoader shared() {
        return SHARED;
    }

    StableImageLoader(int cacheLimit, ImageFactory imageFactory) {
        this.cacheLimit = Math.max(1, cacheLimit);
        this.imageFactory = Objects.requireNonNull(imageFactory, "imageFactory");
    }

    public void load(
            ImageView target,
            String uri,
            double requestedWidth,
            double requestedHeight,
            boolean retainCurrentUntilReady,
            Consumer<LoadState> stateConsumer) {
        Objects.requireNonNull(target, "target");
        Consumer<LoadState> consumer = stateConsumer == null ? ignored -> {} : stateConsumer;
        String normalized = normalize(uri);
        if (normalized.isBlank()) {
            invalidate(target);
            target.setImage(null);
            consumer.accept(LoadState.EMPTY);
            return;
        }
        if (normalized.equals(target.getProperties().get(CURRENT_URI)) && target.getImage() != null) {
            consumer.accept(LoadState.READY);
            return;
        }
        if (normalized.equals(target.getProperties().get(PENDING_URI))) {
            return;
        }

        long token = requestSequence.incrementAndGet();
        target.getProperties().put(REQUEST_TOKEN, token);
        target.getProperties().put(PENDING_URI, normalized);
        Image image = cachedImage(normalized, requestedWidth, requestedHeight);
        if (image.isError()) {
            fail(target, token, consumer);
            return;
        }
        if (image.getProgress() >= 1.0) {
            complete(target, normalized, image, token, consumer);
            return;
        }
        if (!retainCurrentUntilReady) {
            target.setImage(null);
        }
        consumer.accept(LoadState.LOADING);
        observeCompletion(target, normalized, image, token, consumer);
    }

    private void observeCompletion(
            ImageView target,
            String uri,
            Image image,
            long token,
            Consumer<LoadState> consumer) {
        @SuppressWarnings("unchecked")
        ChangeListener<Number>[] progressListener = new ChangeListener[1];
        @SuppressWarnings("unchecked")
        ChangeListener<Boolean>[] errorListener = new ChangeListener[1];
        Runnable detach = () -> {
            image.progressProperty().removeListener(progressListener[0]);
            image.errorProperty().removeListener(errorListener[0]);
        };
        progressListener[0] = (observable, oldValue, newValue) -> {
            if (newValue != null && newValue.doubleValue() >= 1.0) {
                detach.run();
                if (image.isError()) {
                    fail(target, token, consumer);
                } else {
                    complete(target, uri, image, token, consumer);
                }
            }
        };
        errorListener[0] = (observable, oldValue, error) -> {
            if (Boolean.TRUE.equals(error)) {
                detach.run();
                fail(target, token, consumer);
            }
        };
        image.progressProperty().addListener(progressListener[0]);
        image.errorProperty().addListener(errorListener[0]);
        if (image.isError()) {
            detach.run();
            fail(target, token, consumer);
        } else if (image.getProgress() >= 1.0) {
            detach.run();
            complete(target, uri, image, token, consumer);
        }
    }

    private synchronized Image cachedImage(String uri, double width, double height) {
        CacheKey key = new CacheKey(uri, normalizedSize(width), normalizedSize(height));
        return cache.computeIfAbsent(key, ignored -> imageFactory.create(
                uri, key.width(), key.height(), true, true, true));
    }

    private void complete(
            ImageView target,
            String uri,
            Image image,
            long token,
            Consumer<LoadState> consumer) {
        if (!requestMatches(target, token)) {
            return;
        }
        target.setImage(image);
        target.getProperties().put(CURRENT_URI, uri);
        target.getProperties().remove(PENDING_URI);
        consumer.accept(LoadState.READY);
    }

    private void fail(ImageView target, long token, Consumer<LoadState> consumer) {
        if (!requestMatches(target, token)) {
            return;
        }
        if (target.getImage() == null) {
            target.getProperties().remove(CURRENT_URI);
        }
        target.getProperties().remove(PENDING_URI);
        consumer.accept(LoadState.ERROR);
    }

    private void invalidate(ImageView target) {
        target.getProperties().put(REQUEST_TOKEN, requestSequence.incrementAndGet());
        target.getProperties().remove(CURRENT_URI);
        target.getProperties().remove(PENDING_URI);
    }

    private static boolean requestMatches(ImageView target, long token) {
        return Long.valueOf(token).equals(target.getProperties().get(REQUEST_TOKEN));
    }

    synchronized int cacheSize() {
        return cache.size();
    }

    synchronized void clearCache() {
        cache.clear();
    }

    private static double normalizedSize(double value) {
        return Math.max(1.0, Math.rint(value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public enum LoadState {
        EMPTY,
        LOADING,
        READY,
        ERROR
    }

    @FunctionalInterface
    interface ImageFactory {
        Image create(String uri, double width, double height, boolean preserveRatio, boolean smooth, boolean backgroundLoading);
    }

    private record CacheKey(String uri, double width, double height) {}
}
