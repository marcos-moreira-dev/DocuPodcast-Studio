package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class SemanticActionButtonLayoutTest {
    @Test
    void everyBundledSvgProducesShapesAndEveryPngDecodes() throws Exception {
        var directory = java.nio.file.Path.of(getClass().getResource("/icons/lucide").toURI());
        java.util.List<String> names;
        try (var files = java.nio.file.Files.list(directory)) {
            names = files.filter(p -> p.toString().endsWith(".svg"))
                    .map(p -> p.getFileName().toString().replace(".svg", "")).toList();
        }
        assertFalse(names.isEmpty());
        onFx(() -> {
            for (String name : names) {
                LucideIconView icon = new LucideIconView(name, 18);
                assertInstanceOf(javafx.scene.Group.class, icon.getChildren().getFirst(), name);
                assertFalse(((javafx.scene.Group) icon.getChildren().getFirst()).getChildren().isEmpty(), name);
            }
            for (AppIcon icon : AppIcon.values()) {
                var resource = getClass().getResource(icon.resourcePath());
                assertNotNull(resource, icon.name());
                javafx.scene.image.Image image = new javafx.scene.image.Image(resource.toExternalForm());
                assertFalse(image.isError(), icon.name());
                assertTrue(image.getWidth() > 0, icon.name());
            }
        });
    }
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void compactTheatreActionsRenderOneIconWithinTheirBounds() throws Exception {
        onFx(() -> {
            AtomicInteger clicks = new AtomicInteger();
            Button delete = ActionButtonFactory.danger("X", "Eliminar escena", clicks::incrementAndGet);
            Button edit = ActionButtonFactory.secondary("✎", "Editar escena", clicks::incrementAndGet);
            HBox root = new HBox(8, edit, delete);
            Scene scene = new Scene(root, 200, 60);
            scene.getStylesheets().add(getClass().getResource("/css/docupodcast-light.css").toExternalForm());
            for (Button button : new Button[]{edit, delete}) {
                button.getStyleClass().add("theatre-title-edit-button");
                button.setMinSize(24, 24);
                button.setPrefSize(24, 24);
                button.setMaxSize(24, 24);
            }
            for (double scale : new double[]{0.78, 1.0, 1.25}) {
                root.setScaleX(scale);
                root.setScaleY(scale);
                root.applyCss();
                root.layout();
                for (Button button : new Button[]{edit, delete}) {
                    assertEquals(ContentDisplay.GRAPHIC_ONLY, button.getContentDisplay());
                    assertTrue(button.lookupAll(".text").stream()
                            .noneMatch(node -> node.isVisible() && node instanceof Text text && !text.getText().isEmpty()),
                            "The old glyph must not render next to the graphic");
                    Bounds graphic = button.getGraphic().getBoundsInParent();
                    assertTrue(graphic.getMinX() >= 0 && graphic.getMaxX() <= button.getWidth());
                    assertTrue(graphic.getMinY() >= 0 && graphic.getMaxY() <= button.getHeight());
                    assertEquals(button.getTooltip().getText(), button.getAccessibleText());
                }
            }
            edit.fire();
            delete.fire();
            assertEquals(2, clicks.get());
        });
    }

    @Test
    void descriptiveLabelsAndExplicitGraphicsKeepTheirPresentation() throws Exception {
        onFx(() -> {
            Button normal = ActionButtonFactory.danger("Eliminar escena");
            assertEquals(ContentDisplay.LEFT, normal.getContentDisplay());
            assertEquals("Eliminar escena", normal.getText());
            Button custom = new Button("X", new Text("custom"));
            SemanticActionIcons.decorate(custom, "X");
            assertEquals(ContentDisplay.LEFT, custom.getContentDisplay());
            assertEquals("custom", ((Text) custom.getGraphic()).getText());
        });
    }

    @Test
    void refreshingSymbolToTextRestoresTheOriginalDisplay() throws Exception {
        onFx(() -> {
            Button button = new Button("X");
            button.setContentDisplay(ContentDisplay.TOP);
            SemanticActionIcons.decorate(button, button.getText());
            SemanticActionIcons.refresh(button, button.getText());
            assertEquals(ContentDisplay.GRAPHIC_ONLY, button.getContentDisplay());
            button.setText("Cerrar panel");
            SemanticActionIcons.refresh(button, button.getText());
            assertEquals(ContentDisplay.TOP, button.getContentDisplay());
            button.setText("Acción propia");
            SemanticActionIcons.refresh(button, button.getText());
            assertNull(button.getGraphic());
            assertEquals(ContentDisplay.TOP, button.getContentDisplay());
        });
    }

    @Test
    void queueArrowsRenderAsOneIconInsteadOfIconPlusGlyph() throws Exception {
        onFx(() -> {
            Button up = ActionButtonFactory.secondary("↑", "Subir documento", () -> {});
            Button down = ActionButtonFactory.secondary("↓", "Bajar documento", () -> {});
            assertEquals(ContentDisplay.GRAPHIC_ONLY, up.getContentDisplay());
            assertEquals(ContentDisplay.GRAPHIC_ONLY, down.getContentDisplay());
            assertNotNull(up.getGraphic());
            assertNotNull(down.getGraphic());
            assertEquals("Subir documento", up.getAccessibleText());
            assertEquals("Bajar documento", down.getAccessibleText());
        });
    }

    private static void onFx(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }
}
