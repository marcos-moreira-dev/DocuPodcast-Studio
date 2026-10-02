package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkImageFileStore;
import com.marcosmoreiradev.docupodcaststudio.ink.input.NoopInkInputProvider;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreFrameSketchDialogE2ETest {
    private TheatreFrameSketchDialog dialog;

    @Test
    void mouseCoordinatesRemainLocalAfterMergingStage() throws Exception {
        var image = new java.awt.image.BufferedImage(16, 9, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var stageFile = java.nio.file.Files.createTempFile("stage-coordinate-test-", ".png");
        javax.imageio.ImageIO.write(image, "png", stageFile.toFile());
        try {
            fx(() -> {
                var context = new TheatreFrameSketchContext("SEG-001", "U001", "Frame", "Texto",
                        "", "", "", "", stageFile.toUri().toString(), "Plano", List.of());
                dialog = new TheatreFrameSketchDialog(null, context, null,
                        new com.marcosmoreiradev.docupodcaststudio.ink.input.JavaFxMouseInputProvider(),
                        DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME));
                dialog.show();
                return null;
            });
            for (boolean merge : new boolean[]{false, true, false, true}) {
                fx(() -> {
                    var checkbox = (javafx.scene.control.CheckBox) field("mergeWithStage");
                    if (checkbox.isSelected() != merge) checkbox.fire();
                    dialog.getDialogPane().applyCss();
                    dialog.getDialogPane().layout();
                    var surface = (com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface) field("surface");
                    var target = surface.inkInputTarget();
                    fireMouse(target, javafx.scene.input.MouseEvent.MOUSE_MOVED, 100, 100, false);
                    fireMouse(target, javafx.scene.input.MouseEvent.MOUSE_PRESSED, 100, 100, true);
                    fireMouse(target, javafx.scene.input.MouseEvent.MOUSE_DRAGGED, 140, 120, true);
                    fireMouse(target, javafx.scene.input.MouseEvent.MOUSE_RELEASED, 140, 120, false);
                    var strokes = surface.applicationInkStrokes();
                    assertFalse(strokes.isEmpty());
                    var first = strokes.get(strokes.size() - 1).points().get(0);
                    org.junit.jupiter.api.Assertions.assertEquals(100, first.x(), 0.01);
                    org.junit.jupiter.api.Assertions.assertEquals(100, first.y(), 0.01);
                    return null;
                });
            }
        } finally {
            java.nio.file.Files.deleteIfExists(stageFile);
        }
    }

    private static void fireMouse(javafx.scene.Node target,
                                  javafx.event.EventType<javafx.scene.input.MouseEvent> type,
                                  double x, double y, boolean down) {
        var scene = target.localToScene(x, y);
        var screen = target.localToScreen(x, y);
        javafx.event.Event.fireEvent(target, new javafx.scene.input.MouseEvent(type,
                scene.getX(), scene.getY(), screen.getX(), screen.getY(),
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false,
                down, false, false, false, false, false,
                new javafx.scene.input.PickResult(target, scene.getX(), scene.getY())));
    }

    @Test
    void editorContentDoesNotInterceptResponseButtonCoordinates() throws Exception {
        fx(() -> {
            var context = new TheatreFrameSketchContext("SEG-001", "U001", "Frame", "Texto", "", "", "", "", "", "", List.of());
            dialog = new TheatreFrameSketchDialog(null, context, null, NoopInkInputProvider.INSTANCE,
                    DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME));
            dialog.show();
            return null;
        });
        fx(() -> {
            var pane = dialog.getDialogPane();
            pane.resize(1200, 760);
            pane.applyCss();
            pane.layout();
            var content = pane.getContent();
            assertNotNull(content.getClip(), "overflowing descendants must be clipped, not just the parent's bounds");
            for (var type : pane.getButtonTypes()) {
                var button = pane.lookupButton(type);
                var center = button.localToScene(button.getBoundsInLocal().getCenterX(),
                        button.getBoundsInLocal().getCenterY());
                assertFalse(content.contains(content.sceneToLocal(center)),
                        "editor overflow must not pick clicks destined for " + type.getText());
                assertFalse(content.getClip().contains(content.sceneToLocal(center)),
                        "clip must exclude response button " + type.getText());
            }
            return null;
        });
    }

    @Test
    void drawingCanResumeAfterFillAndNativePenDoesNotNeedMouseReentry() throws Exception {
        fx(() -> {
            var context = new TheatreFrameSketchContext("SEG-001", "U001", "Frame", "Texto", "", "", "", "", "", "", List.of());
            dialog = new TheatreFrameSketchDialog(null, context, null, NoopInkInputProvider.INSTANCE,
                    DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME));
            dialog.show();
            dialog.getDialogPane().applyCss();
            dialog.getDialogPane().layout();
            var fill = (javafx.scene.control.ToggleButton) field("fillMode");
            var draw = (javafx.scene.control.ToggleButton) field("drawMode");
            fill.fire();
            assertTrue(fill.isSelected());
            assertTrue(draw.getText().contains("Volver"));
            draw.fire();
            assertFalse(fill.isSelected());
            assertTrue(draw.isSelected());
            var blocked = TheatreFrameSketchDialog.class.getDeclaredField("pointerOverControls");
            blocked.setAccessible(true);
            blocked.setBoolean(dialog, true);
            var inside = TheatreFrameSketchDialog.class.getDeclaredMethod("pointInsideCanvas",
                    com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample.class);
            inside.setAccessible(true);
            var pen = new com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample(100, 100, 1, 1,
                    com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor.PEN, true, false);
            assertNotNull(inside.invoke(dialog, pen), "native reentry must not wait for a synthetic mouse move");
            return null;
        });
    }

    private Object field(String name) throws Exception {
        var field = TheatreFrameSketchDialog.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(dialog);
    }

    private void recreateResponseButtons() {
        var types = List.copyOf(dialog.getDialogPane().getButtonTypes());
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().getButtonTypes().setAll(types);
    }

    @Test
    void hoverOverControlsBlocksInkUntilPointerReturnsToCanvas() throws Exception {
        fx(() -> {
            var context=new TheatreFrameSketchContext("SEG-001","U001","Frame","Texto","","","","","","",List.of());
            dialog=new TheatreFrameSketchDialog(null,context,null,NoopInkInputProvider.INSTANCE,
                    DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME));
            dialog.show();
            var surfaceField=TheatreFrameSketchDialog.class.getDeclaredField("surface");
            surfaceField.setAccessible(true);
            var surface=(com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface)surfaceField.get(dialog);
            var blocked=TheatreFrameSketchDialog.class.getDeclaredField("pointerOverControls");
            blocked.setAccessible(true);
            var hover=new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                    5,5,5,5,javafx.scene.input.MouseButton.NONE,0,false,false,false,false,false,false,false,false,false,false,null);
            javafx.event.Event.fireEvent(dialog.getDialogPane().lookupButton(ButtonType.CANCEL),hover);
            assertTrue(blocked.getBoolean(dialog));
            var start=TheatreFrameSketchDialog.class.getDeclaredMethod("handleStrokeStart",
                    com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample.class);
            start.setAccessible(true);
            var sample=new com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample(100,100,1,1,
                    com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor.MOUSE,true,false);
            assertFalse((Boolean)start.invoke(dialog,sample));
            javafx.event.Event.fireEvent(surface.inkInputTarget(),hover);
            assertFalse(blocked.getBoolean(dialog));
            return null;
        });
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        fx(() -> { Platform.setImplicitExit(false); return null; });
    }

    @AfterEach
    void closeDialog() throws Exception {
        if (dialog != null) fx(() -> { dialog.close(); return null; });
    }

    @Test
    void nativeSaveRunsAsynchronouslyAndProducesOneResult() throws Exception {
        dialog = fx(() -> {
            TheatreFrameSketchContext context = new TheatreFrameSketchContext(
                    "SEG-001", "U001", "Frame", "Texto del fragmento",
                    "", "", "", "", "", "", List.of());
            TheatreFrameSketchDialog created = new TheatreFrameSketchDialog(
                    null, context, null, NoopInkInputProvider.INSTANCE,
                    DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME));
            created.show();
            return created;
        });

        fx(() -> { recreateResponseButtons(); return null; });
        Button save = fx(() -> dialog.getDialogPane().getButtonTypes().stream()
                .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .map(dialog.getDialogPane()::lookupButton)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .findFirst().orElseThrow());
        Button cancel = fx(() -> (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));

        long started = System.nanoTime();
        fx(() -> { save.fire(); return null; });
        long dispatchMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(dispatchMillis < 2_000, "save feedback must be dispatched without PNG encoding on the FX thread");
        assertTrue(fx(save::isDisabled), "save must become single-flight immediately");
        assertFalse(fx(cancel::isDisabled), "native cancel must remain actionable while saving");

        TheatreFrameSketchDialog.Result result = awaitResult(dialog, 60);
        assertNotNull(result);
        assertTrue(java.nio.file.Files.isRegularFile(result.framePng()));
        InkImageFileStore.deleteTemporaryPng(result.framePng());
    }

    @Test
    void nativeCancelButtonClosesTheShownDialog() throws Exception {
        dialog = fx(() -> {
            TheatreFrameSketchContext context = new TheatreFrameSketchContext(
                    "SEG-002", "U002", "Frame", "Texto del fragmento",
                    "", "", "", "", "", "", List.of());
            TheatreFrameSketchDialog created = new TheatreFrameSketchDialog(
                    null, context, null, NoopInkInputProvider.INSTANCE,
                    DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.THEATRE_FRAME));
            created.show();
            return created;
        });

        fx(() -> { recreateResponseButtons(); return null; });
        Button cancel = fx(() -> (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL));
        assertTrue(fx(dialog::isShowing));
        fx(() -> { cancel.fire(); return null; });
        assertFalse(fx(dialog::isShowing), "the native Cancel response must close the real dialog");
    }

    private static TheatreFrameSketchDialog.Result awaitResult(TheatreFrameSketchDialog dialog,
                                                                 int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        TheatreFrameSketchDialog.Result result;
        do {
            result = fx(dialog::getResult);
            if (result != null) return result;
            Thread.sleep(25);
        } while (System.nanoTime() < deadline);
        return null;
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
        assertTrue(latch.await(60, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
        return value.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Exception; }
}
