package com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkCanvasSurfaceInputCaptureTest {
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
    void activeCaptureKeepsInputLayerOnTopAndHitAreaBehindTools() throws Exception {
        runOnFxAndWait(() -> {
            InkCanvasSurface surface = new InkCanvasSurface();
            Rectangle tool = new Rectangle(24, 24);
            surface.inkInputLayer().getChildren().add(tool);
            surface.imageLayer().getChildren().add(new Rectangle(80, 60));

            surface.configureInputCapture(true, true);

            assertSame(surface.inkInputLayer(), last(surface.getChildrenUnmodifiable()));
            assertSame(surface.inkInputTarget(), surface.inkInputLayer().getChildren().get(0));
            assertFalse(surface.inkInputLayer().isMouseTransparent());
            assertFalse(surface.inkInputTarget().isMouseTransparent());
            assertFalse(surface.inkInputTarget().isDisabled());
            assertTrue(surface.inkInputTarget().isVisible());
            return null;
        });
    }

    @Test
    void inactiveCaptureCannotStealEventsFromPanOrImageModes() throws Exception {
        runOnFxAndWait(() -> {
            InkCanvasSurface surface = new InkCanvasSurface();

            surface.configureInputCapture(false, false);

            assertTrue(surface.inkInputLayer().isMouseTransparent());
            assertTrue(surface.inkInputTarget().isMouseTransparent());
            assertTrue(surface.inkInputTarget().isDisabled());
            assertFalse(surface.inkInputTarget().isVisible());
            return null;
        });
    }

    private static Node last(java.util.List<Node> nodes) {
        return nodes.get(nodes.size() - 1);
    }

    private static <T> T runOnFxAndWait(ThrowingSupplier<T> supplier) throws Exception {
        if (Platform.isFxApplicationThread()) return supplier.get();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                value.set(supplier.get());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() instanceof Exception exception) throw exception;
        if (failure.get() != null) throw new AssertionError(failure.get());
        return value.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
