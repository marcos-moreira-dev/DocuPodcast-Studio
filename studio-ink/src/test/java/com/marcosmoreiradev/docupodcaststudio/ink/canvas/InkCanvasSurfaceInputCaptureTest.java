package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

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
    @Test
    void densePreviewKeepsLogicalCoordinatesAndStoredStrokes() throws Exception {
        runOnFxAndWait(() -> {
            InkCanvasSurface surface=new InkCanvasSurface();
            surface.resetForFixedEditableState(640,480,javafx.scene.paint.Color.WHITE);
            surface.commitInkStroke(new InkCanvasSurface.InkStrokeState("DRAW","#000000",3,java.util.List.of(
                    new InkCanvasSurface.InkPointState(20,20,1,1),new InkCanvasSurface.InkPointState(80,80,2,1))));
            var before=surface.inkStrokeStates();
            surface.setInkPreviewScale(2);
            org.junit.jupiter.api.Assertions.assertEquals(640,surface.logicalWidth());
            org.junit.jupiter.api.Assertions.assertEquals(before,surface.inkStrokeStates());
            javafx.scene.layout.Pane strokes=(javafx.scene.layout.Pane)surface.getChildren().get(2);
            org.junit.jupiter.api.Assertions.assertEquals(1,strokes.getChildren().size(), "fixed frames must not reserve document-height tiles");
            javafx.scene.canvas.Canvas tile=(javafx.scene.canvas.Canvas)strokes.getChildren().get(0);
            org.junit.jupiter.api.Assertions.assertEquals(1536,tile.getWidth());
            org.junit.jupiter.api.Assertions.assertEquals(1024,tile.getBoundsInParent().getWidth());
            surface.releasePreviewResources();
            org.junit.jupiter.api.Assertions.assertEquals(0,tile.getWidth());
            org.junit.jupiter.api.Assertions.assertEquals(before,surface.inkStrokeStates());
            return null;
        });
    }
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
    void viewportUsesTheReferenceHitAreaAndCoversASecondHorizontalTile() throws Exception {
        runOnFxAndWait(() -> {
            InkCanvasSurface surface = new InkCanvasSurface();
            var profile = com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog.official()
                    .require(com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog.DOCUMENT_PROBLEM);
            InkCanvasViewport viewport = new InkCanvasViewport(surface, profile);

            assertSame(surface.inkInputTarget(), viewport.inputTarget());
            assertTrue(viewport.ensureCoverage(1800, 700, 1.0));
            assertTrue(viewport.logicalWidth() >= 1800);
            assertTrue(surface.inkInputTarget().getWidth() >= 1800);
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
