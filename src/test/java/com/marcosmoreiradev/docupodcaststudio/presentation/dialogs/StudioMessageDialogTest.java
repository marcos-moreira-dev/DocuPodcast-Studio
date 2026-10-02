package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudioMessageDialogTest {
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
    void longUnicodeMessageWrapsWithoutTruncationAndKeepsDetailsCopyable() throws Exception {
        String message = "Información íntegra con ñ, tildes y una rutaSinEspacios".repeat(12);
        String detail = "C:\\proyecto\\diagnóstico\\archivo-muy-largo.json\ntraza técnica";

        Alert alert = fx(() -> StudioMessageDialog.create(
                null,
                Alert.AlertType.ERROR,
                "Codificación",
                "No se pudo completar la operación",
                message,
                detail));

        Label content = (Label) alert.getDialogPane().getContent();
        TextArea details = (TextArea) alert.getDialogPane().getExpandableContent();
        assertEquals(message, content.getText());
        assertTrue(content.isWrapText());
        assertTrue(alert.isResizable());
        assertEquals(detail, details.getText());
        assertFalse(details.isEditable());
        assertTrue(details.isWrapText());
    }

    @Test
    void presentationCodeCannotBypassTheCommonMessageContentPolicy() throws Exception {
        Path presentation = Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation");
        List<String> violations;
        try (Stream<Path> paths = Files.walk(presentation)) {
            violations = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith("StudioMessageDialog.java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path, java.nio.charset.StandardCharsets.UTF_8)
                                    .contains("setContentText(");
                        } catch (java.io.IOException ex) {
                            throw new java.io.UncheckedIOException(ex);
                        }
                    })
                    .map(Path::toString)
                    .toList();
        }
        assertTrue(violations.isEmpty(), () -> "Message dialogs bypass policy:\n"
                + String.join("\n", violations));
    }

    @Test
    void veryLongMessageUsesBoundedScrollableViewportSoWindowControlsRemainVisible() throws Exception {
        String message = ("SEG-0001, SEG-0002, SEG-0003, SEG-0004, SEG-0005\n").repeat(80);

        Alert alert = fx(() -> StudioMessageDialog.create(
                null,
                Alert.AlertType.ERROR,
                "Exportación",
                "No se pudo exportar",
                message,
                "diagnóstico completo"));

        ScrollPane viewport = (ScrollPane) alert.getDialogPane().getContent();
        Label content = (Label) viewport.getContent();
        assertEquals(message, content.getText());
        assertTrue(viewport.isFitToWidth());
        assertEquals(ScrollPane.ScrollBarPolicy.AS_NEEDED, viewport.getVbarPolicy());
        assertTrue(viewport.getMaxHeight() > 0);
        assertTrue(viewport.getMaxHeight() < javafx.stage.Screen.getPrimary()
                .getVisualBounds().getHeight());
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                value.set(action.call());
            } catch (Throwable ex) {
                failure.set(ex);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return value.get();
    }
}
