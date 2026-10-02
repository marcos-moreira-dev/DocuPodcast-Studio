package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkCanvasZoomPaneTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void continuousChangesAreCoalescedAndExtentNeverDependsOnViewport() throws Exception {
        AtomicInteger callbacks = new AtomicInteger();
        InkCanvasZoomPane pane = fx(() -> {
            InkCanvasZoomPane created = new InkCanvasZoomPane(new Rectangle(1280, 720), () -> 1280, () -> 720);
            created.setOnZoomApplied(ignored -> callbacks.incrementAndGet());
            StackPane root = new StackPane(created);
            new Scene(root, 800, 600);
            root.resize(800, 600);
            root.applyCss();
            root.layout();
            for (int percent = 25; percent <= 200; percent++) created.setZoom(percent / 100.0);
            return created;
        });

        fx(() -> null); // apply the coalesced zoom
        fx(() -> {
            assertEquals(1, callbacks.get(), "one visual update is allowed per JavaFX pulse");
            assertEquals(2.0, pane.appliedZoom(), 0.000_001);
            assertEquals(2560, pane.contentHost().prefWidth(-1), 0.001);
            assertEquals(1440, pane.contentHost().prefHeight(-1), 0.001);
            double width = pane.contentHost().prefWidth(-1);
            double height = pane.contentHost().prefHeight(-1);
            pane.resize(1366, 700);
            pane.applyCss();
            pane.layout();
            assertEquals(width, pane.contentHost().prefWidth(-1), 0.001);
            assertEquals(height, pane.contentHost().prefHeight(-1), 0.001);
            return null;
        });
    }

    @Test
    void zoomedOutCanvasIsCenteredFromItsVisualBounds() throws Exception {
        InkCanvasZoomPane pane = fx(() -> {
            InkCanvasZoomPane created = new InkCanvasZoomPane(
                    new Rectangle(1280, 720), () -> 1280, () -> 720);
            StackPane root = new StackPane(created);
            new Scene(root, 1600, 1000);
            root.resize(1600, 1000);
            root.applyCss();
            root.layout();
            created.setZoom(0.5);
            return created;
        });

        fx(() -> null); // apply zoom
        fx(() -> null); // restore/centre after layout
        fx(() -> {
            pane.applyCss();
            pane.layout();
            Node visualCanvas = pane.contentHost().getChildrenUnmodifiable().getFirst();
            Bounds bounds = visualCanvas.getBoundsInParent();
            assertEquals(640.0, bounds.getWidth(), 0.001);
            assertEquals(360.0, bounds.getHeight(), 0.001);
            assertEquals((pane.contentHost().getWidth() - bounds.getWidth()) / 2.0,
                    bounds.getMinX(), 1.0);
            assertEquals((pane.contentHost().getHeight() - bounds.getHeight()) / 2.0,
                    bounds.getMinY(), 1.0);
            return null;
        });
    }

    private static <T> T fx(ThrowingSupplier<T> action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { value.set(action.get()); }
            catch (Throwable ex) { failure.set(ex); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
        return value.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Exception; }
}
