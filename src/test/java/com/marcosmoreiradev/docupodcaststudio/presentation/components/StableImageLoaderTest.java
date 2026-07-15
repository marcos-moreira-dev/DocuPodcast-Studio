package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class StableImageLoaderTest {
    @Test
    void sameUriIsLoadedOnceAndKeepsTheImageInstance() {
        AtomicInteger creations = new AtomicInteger();
        StableImageLoader loader = loader(4, creations);
        ImageView view = new ImageView();

        loader.load(view, "memory:a", 84, 54, true, ignored -> {});
        Image first = view.getImage();
        loader.load(view, "memory:a", 84, 54, true, ignored -> {});

        assertEquals(1, creations.get());
        assertSame(first, view.getImage());
    }

    @Test
    void cacheIsBoundedAndUsesLeastRecentlyUsedOrder() {
        AtomicInteger creations = new AtomicInteger();
        StableImageLoader loader = loader(2, creations);
        loader.load(new ImageView(), "memory:a", 84, 54, true, ignored -> {});
        loader.load(new ImageView(), "memory:b", 84, 54, true, ignored -> {});
        loader.load(new ImageView(), "memory:a", 84, 54, true, ignored -> {});
        loader.load(new ImageView(), "memory:c", 84, 54, true, ignored -> {});
        loader.load(new ImageView(), "memory:b", 84, 54, true, ignored -> {});

        assertEquals(2, loader.cacheSize());
        assertEquals(4, creations.get());
    }

    private static StableImageLoader loader(int limit, AtomicInteger creations) {
        return new StableImageLoader(limit, (uri, width, height, preserveRatio, smooth, backgroundLoading) -> {
            creations.incrementAndGet();
            return new WritableImage(2, 2);
        });
    }
}
