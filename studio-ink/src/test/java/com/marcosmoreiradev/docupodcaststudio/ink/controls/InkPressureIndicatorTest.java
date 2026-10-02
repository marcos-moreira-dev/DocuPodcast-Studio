package com.marcosmoreiradev.docupodcaststudio.ink.controls;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkPressureIndicatorTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void exposesOnlyCoalescedPressureWithFixedGeometry() throws Exception {
        SimpleObjectProperty<InkInputStatus> status = new SimpleObjectProperty<>();
        InkPressureIndicator indicator = fx(() -> new InkPressureIndicator(status));
        fx(() -> {
            status.set(nativePressure(0.12));
            status.set(nativePressure(0.78));
            return null;
        });
        fx(() -> null);

        fx(() -> {
            assertEquals("Presión:  78%", indicator.getText());
            assertFalse(indicator.getText().contains("LectureStudio"));
            assertEquals(indicator.getMinWidth(), indicator.getPrefWidth(), 0.001);
            assertEquals(indicator.getPrefWidth(), indicator.getMaxWidth(), 0.001);
            indicator.close();
            return null;
        });
    }

    private static InkInputStatus nativePressure(double pressure) {
        return new InkInputStatus("LectureStudio stylus", "LectureStudio", InkInputCursor.PEN,
                pressure, pressure, true, true, "");
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
