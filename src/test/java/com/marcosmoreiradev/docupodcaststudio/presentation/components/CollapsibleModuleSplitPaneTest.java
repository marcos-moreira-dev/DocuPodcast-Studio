package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CollapsibleModuleSplitPaneTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void collapsedPrimaryKeepsAnExplicitRestoreControl() throws Exception {
        CollapsibleModuleSplitPane pane = fx(() -> new CollapsibleModuleSplitPane(
                "Acciones del contenido",
                new VBox(),
                "Contenido del video",
                new VBox(),
                0.43,
                true));

        fx(() -> { pane.primaryVisibleProperty().set(false); return null; });

        assertFalse(pane.primaryVisibleProperty().get());
        HBox collapsed = assertInstanceOf(HBox.class, fx(pane::getCenter));
        VBox strip = assertInstanceOf(VBox.class, collapsed.getChildren().get(0));
        Button restore = strip.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow();

        fx(() -> { restore.fire(); return null; });

        assertTrue(pane.primaryVisibleProperty().get());
        assertInstanceOf(SplitPane.class, fx(pane::getCenter));
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { value.set(action.call()); }
            catch (Throwable ex) { failure.set(ex); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
        return value.get();
    }
}
