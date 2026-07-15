package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputSample;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemDialogMouseInkIntegrationTest {
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
    void dialogInitializedCanvasDoesNotCommitMouseStrokeFromInkInputLayer() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);

        assertInkLayerCapturesMouse(surface);
        runOnFxAndWait(() -> {
            surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 92, 110, true));
            surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 140, 156, true));
            surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 188, 204, false));
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(0, strokes.size());
    }

    @Test
    void dialogVisibleTreeDoesNotCommitMouseStrokeFromInkHitArea() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);

        assertInkLayerCapturesMouse(surface);
        runOnFxAndWait(() -> {
            attachDialogPaneToScene(dialog);
            fireInkTargetMouseGesture(surface);
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(0, strokes.size());
    }

    @Test
    void transferredImageLeavesCanvasReadyWithoutMouseInkFallback() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);

        runOnFxAndWait(() -> {
            invoke(dialog, "transferSourceImage", new Class<?>[]{javafx.scene.image.Image.class},
                    new WritableImage(300, 140));
            return null;
        });

        BooleanProperty imageInteractionMode = field(dialog, "imageInteractionMode", BooleanProperty.class);
        assertTrue(!imageInteractionMode.get(), "transferred images must not steal drawing mode by default");
        assertInkLayerCapturesMouse(surface);
        runOnFxAndWait(() -> {
            surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 110, 130, true));
            surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 170, 175, true));
            surface.inkInputLayer().fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 225, 210, false));
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(0, strokes.size());
    }

    @Test
    void imageInteractionModeSuppressesNativeInkStrokeHandling() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);

        runOnFxAndWait(() -> {
            BooleanProperty imageInteractionMode = field(dialog, "imageInteractionMode", BooleanProperty.class);
            imageInteractionMode.set(true);
            invokeBoolean(dialog, "handleInkStrokeStart", nativeSample(120, 150, 1));
            invokeBoolean(dialog, "handleInkStrokeMove", nativeSample(180, 210, 2));
            invokeBoolean(dialog, "handleInkStrokeEnd", nativeSample(240, 270, 3));
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(0, strokes.size());
    }

    private static TechnicalProblemDialog newDialog() throws Exception {
        Constructor<TechnicalProblemDialog> constructor = TechnicalProblemDialog.class
                .getDeclaredConstructor(Window.class, List.class, Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, List.<DocumentBlock>of(), Map.of());
    }

    private static void attachDialogPaneToScene(TechnicalProblemDialog owner) throws Exception {
        Dialog<?> dialog = field(owner, "dialog", Dialog.class);
        if (dialog.getDialogPane().getScene() == null) {
            new Scene(dialog.getDialogPane(), 1220, 760);
        }
        dialog.getDialogPane().applyCss();
        dialog.getDialogPane().layout();
    }

    private static void fireInkTargetMouseGesture(StudyProblemCanvasSurface surface) {
        surface.applyCss();
        surface.layout();
        Node target = surface.inkInputTarget();
        target.fireEvent(mouseForTarget(surface.inkInputLayer(), target, MouseEvent.MOUSE_PRESSED, 92, 110, true));
        target.fireEvent(mouseForTarget(surface.inkInputLayer(), target, MouseEvent.MOUSE_DRAGGED, 140, 156, true));
        target.fireEvent(mouseForTarget(surface.inkInputLayer(), target, MouseEvent.MOUSE_RELEASED, 188, 204, false));
    }

    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }

    private static void invoke(Object owner, String name) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(owner);
    }

    private static void invoke(Object owner, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        method.invoke(owner, args);
    }

    private static boolean invokeBoolean(Object owner, String name, InkInputSample sample) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, InkInputSample.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(owner, sample);
    }

    private static InkInputSample nativeSample(double x, double y, long tick) {
        return new InkInputSample(x, y, tick, 1.0, InkInputCursor.PEN, true, false);
    }

    private static void assertInkLayerCapturesMouse(StudyProblemCanvasSurface surface) {
        assertTrue(!surface.inkInputLayer().isMouseTransparent());
        assertTrue(surface.inkInputLayer().isPickOnBounds());
        assertTrue(!surface.inkInputTarget().isMouseTransparent());
        assertTrue(surface.inkInputTarget().isVisible());
        assertTrue(!surface.inkInputTarget().isDisable());
    }

    private static <T> T runOnFxAndWait(ThrowingSupplier<T> supplier) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(supplier.get());
            } catch (Throwable ex) {
                error.set(ex);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get();
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

    private static MouseEvent mouseForTarget(Node inputLayer,
                                             Node eventTarget,
                                             javafx.event.EventType<MouseEvent> type,
                                             double inputLayerX,
                                             double inputLayerY,
                                             boolean primaryDown) {
        Point2D scene = inputLayer.localToScene(inputLayerX, inputLayerY);
        Point2D targetLocal = eventTarget.sceneToLocal(scene);
        return new MouseEvent(
                type,
                targetLocal.getX(),
                targetLocal.getY(),
                scene.getX(),
                scene.getY(),
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
