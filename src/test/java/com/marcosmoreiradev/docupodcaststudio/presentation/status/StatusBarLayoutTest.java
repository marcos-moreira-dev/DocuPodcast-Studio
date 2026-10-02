package com.marcosmoreiradev.docupodcaststudio.presentation.status;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.DocumentReadingReadinessSnapshot;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StatusBarLayoutTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void compactStatusBarKeepsActionsAndZoomInsideItsBounds() throws Exception {
        StatusBarView bar = fx(() -> {
            var view = new StatusBarView(
                    new SimpleStringProperty("Lectura preparada: 392 fragmentos, 9100 palabras, sin errores."),
                    new SimpleIntegerProperty(25),
                    () -> {}, () -> {}, () -> {}, ignored -> {},
                    new SimpleObjectProperty<>(),
                    new SimpleObjectProperty<>(),
                    new SimpleBooleanProperty(false),
                    new SimpleObjectProperty<>(),
                    new SimpleObjectProperty<>(),
                    new SimpleStringProperty("B0003"),
                    new SimpleDoubleProperty(0),
                    new SimpleBooleanProperty(false),
                    new SimpleBooleanProperty(false),
                    new SimpleObjectProperty<>(true),
                    () -> {}, () -> {}, () -> {}, () -> {}, () -> {}, () -> {});
            Scene scene = new Scene(view, 1536, 40);
            scene.getStylesheets().add(StatusBarView.class.getResource("/css/docupodcast-light.css").toExternalForm());
            view.applyCss();
            view.resize(1536, 40);
            view.layout();
            return view;
        });

        ScrollPane status = (ScrollPane) bar.lookup(".status-bar-scroll");
        assertNotNull(status);
        assertNull(bar.lookup(".status-audio-menu"));
        assertEquals(2, bar.lookupAll(".status-generation-button").stream()
                .filter(Node::isManaged).count());
        Slider zoom = (Slider) bar.lookup(".reading-zoom-slider");
        assertNotNull(zoom);
        assertTrue(zoom.getWidth() <= 112.5, "zoom slider must remain compact");
        String layout = bar.getChildren().stream()
                .map(child -> child.getClass().getSimpleName()
                        + "[x=" + child.getLayoutX()
                        + ",w=" + child.getBoundsInParent().getWidth()
                        + ",min=" + child.minWidth(-1)
                        + ",pref=" + child.prefWidth(-1) + "]")
                .collect(java.util.stream.Collectors.joining(", "));
        String zoomLayout = ((javafx.scene.layout.Pane) bar.lookup(".reading-zoom-control"))
                .getChildren().stream()
                .map(child -> child.getClass().getSimpleName()
                        + "[x=" + child.getLayoutX()
                        + ",w=" + child.getBoundsInParent().getWidth()
                        + ",visual=" + child.getBoundsInLocal() + "]")
                .collect(java.util.stream.Collectors.joining(", "));
        for (Node child : bar.getChildren()) {
            assertTrue(child.getBoundsInParent().getMaxX() <= bar.getWidth() + 0.5,
                    () -> child.getClass().getSimpleName() + " overflows the status bar: maxX="
                            + child.getBoundsInParent().getMaxX() + ", width=" + bar.getWidth() + "; " + layout
                            + "; zoom=" + zoomLayout);
        }
    }

    @Test
    void longStatusMessageRemainsHorizontallyScrollable() throws Exception {
        StatusBarView bar = fx(() -> {
            var view = new StatusBarView(new SimpleStringProperty("Mensaje operativo ".repeat(80)),
                    new SimpleIntegerProperty(25), () -> {}, () -> {}, () -> {}, ignored -> {},
                    new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                    new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                    new SimpleObjectProperty<>(), new SimpleStringProperty(""),
                    new SimpleDoubleProperty(0), new SimpleBooleanProperty(false),
                    new SimpleBooleanProperty(false),
                    new SimpleObjectProperty<>(false), null, null, null, null, null, null);
            Scene scene = new Scene(view, 1366, 40);
            scene.getStylesheets().add(StatusBarView.class.getResource("/css/docupodcast-light.css").toExternalForm());
            view.applyCss(); view.resize(1366, 40); view.layout();
            return view;
        });
        ScrollPane scroller = (ScrollPane) bar.lookup(".status-bar-scroll");
        assertNotNull(scroller);
        assertTrue(((javafx.scene.control.Label) scroller.getContent()).getWidth()
                > scroller.getViewportBounds().getWidth());
    }

    @Test
    void activeMaintenanceKeepsDetailsAndCancelButMakesFullProcessingIdempotent() throws Exception {
        AtomicInteger starts = new AtomicInteger();
        StatusBarView bar = fx(() -> {
            SimpleBooleanProperty expanded = new SimpleBooleanProperty(false);
            SimpleBooleanProperty operationRunning = new SimpleBooleanProperty(true);
            var view = new StatusBarView(
                    new SimpleStringProperty("Cancelando y esperando el trabajo de audio activo..."),
                    new SimpleIntegerProperty(18), () -> {}, () -> {}, () -> {}, ignored -> {},
                    new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                    new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                    new SimpleObjectProperty<>(), new SimpleStringProperty(""),
                    new SimpleDoubleProperty(0), expanded, operationRunning,
                    new SimpleObjectProperty<>(true), starts::incrementAndGet,
                    null, null, () -> {}, null, null);
            new Scene(view, 1536, 40);
            view.applyCss();
            view.layout();
            return view;
        });

        Button details = button(bar, "Detalles · procesamiento");
        Button restart = button(bar, "Procesar lectura completa");
        Button cancel = button(bar, "Cancelar generación");
        assertTrue(details.isManaged());
        assertTrue(restart.isManaged());
        assertTrue(restart.isDisabled());
        assertTrue(cancel.isManaged());
        fx(() -> { restart.fire(); return null; });
        assertEquals(0, starts.get());
    }

    @Test
    void intervalActionObservesTypedScopeSupportAndValidity() throws Exception {
        var scope = new SimpleObjectProperty<>(DocumentProcessingScope.FROM_SELECTION);
        var supported = new SimpleBooleanProperty(true);
        var valid = new SimpleBooleanProperty(true);
        AtomicInteger starts = new AtomicInteger();
        StatusBarView bar = fx(() -> new StatusBarView(
                new SimpleStringProperty("Listo"), new SimpleIntegerProperty(18),
                () -> {}, () -> {}, () -> {}, ignored -> {},
                new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(), new SimpleStringProperty(""),
                new SimpleDoubleProperty(0), new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false), new SimpleBooleanProperty(true),
                scope, supported, valid, () -> {}, () -> {}, starts::incrementAndGet,
                () -> {}, () -> {}, () -> {}, () -> {}));

        Button interval = button(bar, "Procesar intervalo");
        assertTrue(!interval.isManaged());
        fx(() -> { scope.set(DocumentProcessingScope.INTERVAL); return null; });
        assertTrue(interval.isManaged());
        assertTrue(!interval.isDisabled());
        fx(() -> { interval.fire(); return null; });
        assertEquals(1, starts.get());
        fx(() -> { valid.set(false); return null; });
        assertTrue(interval.isDisabled());
        fx(() -> { supported.set(false); return null; });
        assertTrue(!interval.isManaged());
    }

    @Test
    void processFromHereExistsOnlyWhileARealSelectionIsProjected() throws Exception {
        var selection = new SimpleStringProperty("");
        StatusBarView bar = fx(() -> new StatusBarView(
                new SimpleStringProperty("Listo"), new SimpleIntegerProperty(18),
                () -> {}, () -> {}, () -> {}, ignored -> {},
                new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(), selection,
                new SimpleDoubleProperty(0), new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false), new SimpleBooleanProperty(true),
                new SimpleObjectProperty<>(DocumentProcessingScope.FULL_DOCUMENT),
                new SimpleBooleanProperty(false), new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false), () -> {}, () -> {}, null,
                null, null, null, null));

        Button fromHere = button(bar, "Procesar desde aquí");
        assertTrue(!fromHere.isManaged());
        fx(() -> { selection.set("B0001"); return null; });
        assertTrue(fromHere.isManaged());
        fx(() -> { selection.set(""); return null; });
        assertTrue(!fromHere.isManaged());
    }

    @Test
    void persistedChunksExposeDeleteActionWithoutDependingOnLastJobDto()
            throws Exception {
        var managedChunks = new SimpleBooleanProperty(false);
        StatusBarView bar = fx(() -> new StatusBarView(
                new SimpleStringProperty("Listo"), new SimpleIntegerProperty(18),
                () -> {}, () -> {}, () -> {}, ignored -> {},
                new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(), new SimpleStringProperty(""),
                new SimpleDoubleProperty(0), new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false), new SimpleBooleanProperty(true),
                new SimpleObjectProperty<>(DocumentProcessingScope.INTERVAL),
                new SimpleBooleanProperty(true), new SimpleBooleanProperty(true),
                managedChunks, () -> {}, () -> {}, () -> {}, () -> {}, () -> {},
                () -> {}, () -> {}));

        Button delete = button(bar, "Eliminar todos los chunks de audio");
        assertTrue(!delete.isManaged());
        fx(() -> { managedChunks.set(true); return null; });
        assertTrue(delete.isManaged());
        fx(() -> { managedChunks.set(false); return null; });
        assertTrue(!delete.isManaged());
    }

    @Test
    void fullReadingLabelDistinguishesPreparationMissingAudioAndReprocessing() throws Exception {
        var readiness = new SimpleObjectProperty<>(
                DocumentReadingReadinessSnapshot.unavailable("en"));
        StatusBarView bar = fx(() -> new StatusBarView(
                new SimpleStringProperty("Listo"), new SimpleIntegerProperty(18),
                () -> {}, () -> {}, () -> {}, ignored -> {},
                new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(), new SimpleStringProperty(""),
                new SimpleDoubleProperty(0), new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false), new SimpleBooleanProperty(true),
                new SimpleObjectProperty<>(DocumentProcessingScope.FULL_DOCUMENT),
                new SimpleBooleanProperty(true), new SimpleBooleanProperty(true),
                new SimpleBooleanProperty(false), readiness,
                () -> {}, () -> {}, () -> {}, () -> {}, () -> {}, () -> {}, () -> {}));

        Button process = button(bar, "Procesar lectura completa");
        assertNotNull(process.getGraphic());
        Node processIcon = process.getGraphic();
        fx(() -> {
            readiness.set(new DocumentReadingReadinessSnapshot(
                    DocumentProcessingScope.FULL_DOCUMENT, true, true, "en",
                    List.of(), List.of(), 0, 17, 0, 0));
            return null;
        });
        Button completeAudio = button(bar, "Completar audio de lectura");
        assertNotNull(completeAudio.getGraphic());
        Node completeAudioIcon = completeAudio.getGraphic();
        assertTrue(completeAudio.getGraphic() != processIcon,
                "el icono debe actualizarse con la intención");
        fx(() -> {
            readiness.set(new DocumentReadingReadinessSnapshot(
                    DocumentProcessingScope.FULL_DOCUMENT, true, true, "en",
                    List.of(), List.of(), 17, 0, 0, 0));
            return null;
        });
        Button reprocess = button(bar, "Reprocesar lectura completa");
        assertNotNull(reprocess.getGraphic());
        assertTrue(reprocess.getGraphic() != completeAudioIcon,
                "el icono debe actualizarse con la intención");
    }

    @Test
    void everyPertinentStatusActionHasAMonochromeSemanticIcon() throws Exception {
        StatusBarView bar = fx(() -> new StatusBarView(
                new SimpleStringProperty("Listo"), new SimpleIntegerProperty(18),
                () -> {}, () -> {}, () -> {}, ignored -> {},
                new SimpleObjectProperty<>(), new SimpleObjectProperty<>(),
                new SimpleBooleanProperty(false), new SimpleObjectProperty<>(),
                new SimpleObjectProperty<>(), new SimpleStringProperty("B0001"),
                new SimpleDoubleProperty(0), new SimpleBooleanProperty(false),
                new SimpleBooleanProperty(false), new SimpleBooleanProperty(true),
                new SimpleObjectProperty<>(DocumentProcessingScope.INTERVAL),
                new SimpleBooleanProperty(true), new SimpleBooleanProperty(true),
                new SimpleBooleanProperty(true), () -> {}, () -> {}, () -> {},
                () -> {}, () -> {}, () -> {}, () -> {}));

        bar.lookupAll(".button")
                .stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(action -> action.getStyleClass().contains("status-generation-button")
                        || action.getStyleClass().contains("status-generation-danger-button")
                        || action.getStyleClass().contains("status-process-button"))
                .forEach(action -> assertNotNull(action.getGraphic(), action.getText()));
    }

    private static Button button(StatusBarView bar, String text) {
        return bar.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing button: " + text));
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { value.set(action.call()); }
            catch (Throwable throwable) { failure.set(throwable); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() instanceof Exception exception) throw exception;
        if (failure.get() != null) throw new AssertionError(failure.get());
        return value.get();
    }
}
