package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;
import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProviderFactory;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
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
    void rotationHandleDragsFreelyAndKeepsShapeEditable() throws Exception {
        runOnFxAndWait(() -> {
            var editor = newDialog();
            invoke(editor, "insertShape", new Class<?>[]{TechnicalShape.class}, TechnicalShape.RECTANGLE);
            tool(editor, "Lápiz").fire();
            var item = field(editor, "selectedCanvasImage", Object.class);
            var view = field(item, "view", javafx.scene.image.ImageView.class);
            var surface = field(editor, "drawingSurface", StudyProblemCanvasSurface.class);
            var handle = field(editor, "imageRotateHandle", javafx.scene.control.Button.class);
            var bounds = view.getBoundsInParent();
            double cx = (bounds.getMinX()+bounds.getMaxX())/2;
            double cy = (bounds.getMinY()+bounds.getMaxY())/2;
            var start = surface.localToScene(cx, cy-150);
            // Deliberately not a multiple of the toolbar's 15-degree step.
            double angle = Math.toRadians(37);
            var end = surface.localToScene(cx+150*Math.sin(angle), cy-150*Math.cos(angle));
            for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_RELEASED)) {
                var p = type == MouseEvent.MOUSE_PRESSED ? start : end;
                handle.fireEvent(new MouseEvent(type, p.getX(), p.getY(), p.getX(), p.getY(), MouseButton.PRIMARY,
                        1, false, false, false, false, type != MouseEvent.MOUSE_RELEASED, false, false, false, false, false, null));
            }
            var shape = field(item, "shape", com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject.class);
            assertEquals(37, shape.angle(), 0.001);
            assertEquals(0, view.getRotate(), 0.001);
            assertEquals(cx, (view.getBoundsInParent().getMinX()+view.getBoundsInParent().getMaxX())/2, 0.1);
            invoke(editor, "undo");
            var restored = field(editor, "canvasImages", List.class).get(0);
            assertEquals(0, field(restored, "shape", com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject.class).angle(), 0.001);
            invoke(editor, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void emptyCanvasClickClearsObjectSelectionOnlyInNeutralMode() throws Exception {
        runOnFxAndWait(() -> {
            var constructor = TechnicalProblemDialog.class.getDeclaredConstructor(
                    Window.class, boolean.class, InkInputProvider.class, DrawingProfile.class);
            constructor.setAccessible(true);
            for (boolean express : List.of(false, true)) {
                TechnicalProblemDialog editor = express ? constructor.newInstance(null,true,InkInputProviderFactory.createDefault(),
                        DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM)) : newDialog();
                var surface = field(editor,"drawingSurface",StudyProblemCanvasSurface.class);
                var dialog = field(editor, "dialog", javafx.scene.control.Dialog.class);
                assertEquals(!express, dialog.getDialogPane().getButtonTypes().contains(javafx.scene.control.ButtonType.CANCEL));
                if (express) {
                    assertEquals(null, dialog.getHeaderText());
                    assertEquals(null, field(editor, "dialogHeaderText", String.class));
                }
                assertTrue(surface.getCursor() instanceof javafx.scene.ImageCursor);
                var scroll = field(editor,"canvasScroll",javafx.scene.control.ScrollPane.class);
                scroll.setSkin(new javafx.scene.control.skin.ScrollPaneSkin(scroll));
                invoke(editor,"insertShape",new Class<?>[]{TechnicalShape.class},TechnicalShape.RECTANGLE);
                tool(editor,"Lápiz").fire();
                assertEquals(javafx.scene.Cursor.DEFAULT, surface.getCursor());
                var images = field(editor,"canvasImages",List.class);
                Object shape = images.get(0);
                invoke(editor,"selectCanvasImage",new Class<?>[]{shape.getClass()},shape);
                var view = field(shape,"view",javafx.scene.image.ImageView.class);
                surface.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED,900,700,900,700,MouseButton.PRIMARY,1,
                        false,false,false,false,false,false,false,false,false,true,null));
                assertEquals(null,field(editor,"selectedCanvasImage",Object.class));
                assertTrue(!view.getStyleClass().contains("technical-problem-canvas-image-selected"));
                assertTrue(!field(editor,"objectSelectionVisible",BooleanProperty.class).get());
                // A drag-release is not an empty click and must keep the selection.
                invoke(editor,"selectCanvasImage",new Class<?>[]{shape.getClass()},shape);
                surface.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED,900,700,900,700,MouseButton.PRIMARY,1,
                        false,false,false,false,false,false,false,false,false,false,null));
                assertEquals(shape,field(editor,"selectedCanvasImage",Object.class));
                invoke(editor,"disposeCanvasInput");
            }
            return null;
        });
    }

    @Test
    void toolbarLayoutKeepsNamedActionsReadable() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            var pane = field(editor,"dialog",javafx.scene.control.Dialog.class).getDialogPane();
            pane.resize(1200, 800);
            pane.applyCss(); pane.layout();
            var toolbar = field(editor,"toolBarNode",javafx.scene.Node.class);
            for (var node : toolbar.lookupAll(".button")) {
                var button = (javafx.scene.control.Button)node;
                if (button.getText() != null && !button.getText().isBlank())
                    assertTrue(button.getContentDisplay() != javafx.scene.control.ContentDisplay.GRAPHIC_ONLY, button.getText());
            }
            Method encode = TechnicalProblemDialog.class.getDeclaredMethod("imageToBase64", javafx.scene.image.Image.class);
            encode.setAccessible(true);
            java.nio.file.Files.write(java.nio.file.Path.of("target","technical-toolbar-preview.png"),
                    java.util.Base64.getDecoder().decode((String)encode.invoke(null,pane.snapshot(null,null))));
            invoke(editor,"disposeCanvasInput");
            return null;
        });
    }

    @Test
    void everyToolCanToggleOffWithoutActivatingAnotherToolInBothEditors() throws Exception {
        runOnFxAndWait(() -> {
            var constructor = TechnicalProblemDialog.class.getDeclaredConstructor(
                    Window.class, boolean.class, InkInputProvider.class, DrawingProfile.class);
            constructor.setAccessible(true);
            for (boolean express : List.of(false, true)) {
                TechnicalProblemDialog editor = express ? constructor.newInstance(null, true,
                        InkInputProviderFactory.createDefault(), DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM)) : newDialog();
                var names = List.of("Lápiz", "Línea", "Medir ángulo", "Borrador", "Mover lienzo", "Seleccionar región", "Agregar texto");
                tool(editor, "Lápiz").fire();
                for (String name : names) {
                    var button = tool(editor, name);
                    button.fire();
                    assertTrue(button.isSelected(), name);
                    assertTrue(field(editor, "drawingSurface", StudyProblemCanvasSurface.class).getCursor()
                            instanceof javafx.scene.ImageCursor, name);
                    assertEquals(1, names.stream().filter(n -> {
                        try { return tool(editor, n).isSelected(); } catch (Exception e) { throw new RuntimeException(e); }
                    }).count());
                    button.fire();
                    assertTrue(!button.isSelected(), name);
                    assertEquals(javafx.scene.Cursor.DEFAULT,
                            field(editor, "drawingSurface", StudyProblemCanvasSurface.class).getCursor(), name);
                    assertEquals("NONE", field(editor, "activeCanvasTool", javafx.beans.property.ObjectProperty.class).get().toString());
                    for (String other : names) assertTrue(!tool(editor, other).isSelected(), other);
                    assertTrue(!field(editor, "drawMode", BooleanProperty.class).get());
                    assertTrue(!field(editor, "canvasRegionSelectionMode", BooleanProperty.class).get());
                    assertTrue(!field(editor, "canvasScroll", javafx.scene.control.ScrollPane.class).isPannable());
                    click(editor, 200, 200, 100);
                    invoke(editor, "flushInk");
                    assertTrue(field(editor, "drawingSurface", StudyProblemCanvasSurface.class).applicationInkStrokes().isEmpty());
                    assertTrue(field(editor, "angleMeasurementPoints", List.class).isEmpty());
                    assertEquals(null, field(editor, "canvasTextEditor", javafx.scene.control.TextArea.class));
                }
                invoke(editor, "disposeCanvasInput");
            }
            return null;
        });
    }

    @Test
    void resourceOpacityTransfersExportsAndSurvivesHistoryAndReload() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            WritableImage red = new WritableImage(2, 2);
            for (int y=0; y<2; y++) for (int x=0; x<2; x++) red.getPixelWriter().setColor(x,y,javafx.scene.paint.Color.RED);
            Method encode = TechnicalProblemDialog.class.getDeclaredMethod("imageToBase64", javafx.scene.image.Image.class);
            encode.setAccessible(true);
            Class<?> sourceType = Class.forName(TechnicalProblemDialog.class.getName() + "$StatementSource");
            var constructor = sourceType.getDeclaredConstructor(String.class, String.class, String.class, java.nio.file.Path.class, String.class);
            constructor.setAccessible(true);
            Object source = constructor.newInstance("opacity-test", "", "", null, encode.invoke(null, red));
            Method visual = TechnicalProblemDialog.class.getDeclaredMethod("sourceVisualNode", sourceType);
            visual.setAccessible(true);
            var box = (javafx.scene.layout.VBox)((java.util.Optional<?>)visual.invoke(editor,source)).orElseThrow();
            var row = (javafx.scene.layout.HBox)box.getChildren().get(1);
            var slider = (javafx.scene.control.Slider)row.getChildren().get(1);
            slider.setValue(50);
            ((javafx.scene.control.Button)box.getChildren().get(2)).fire();
            var images = field(editor,"canvasImages",List.class);
            var view = field(images.get(0),"view",javafx.scene.image.ImageView.class);
            assertEquals(0.5,view.getOpacity(),0.001);
            var surface = field(editor,"drawingSurface",StudyProblemCanvasSurface.class);
            var exported = surface.snapshotRegion(List.of(view),view.getLayoutX()+10,view.getLayoutY()+10,2,2);
            var pixel = exported.getPixelReader().getColor(0,0);
            assertEquals(1, pixel.getRed(), 0.01);
            assertEquals(0, pixel.getGreen(), 0.01);
            assertEquals(0.5, pixel.getOpacity(), 0.01);
            slider.setValue(0);
            assertEquals(0,view.getOpacity());
            invoke(editor,"undo");
            assertEquals(50,slider.getValue(),0.01);
            assertEquals(0.5,field(images.get(0),"view",javafx.scene.image.ImageView.class).getOpacity(),0.001);
            Method serialize = TechnicalProblemDialog.class.getDeclaredMethod("canvasStateJson");
            serialize.setAccessible(true);
            TechnicalProblemDialog reopened = newDialog();
            invoke(reopened,"restoreCanvasJson",new Class<?>[]{String.class},(String)serialize.invoke(editor));
            var restored = field(reopened,"canvasImages",List.class);
            assertEquals(0.5,field(restored.get(0),"view",javafx.scene.image.ImageView.class).getOpacity(),0.001);
            invoke(editor,"disposeCanvasInput"); invoke(reopened,"disposeCanvasInput");
            return null;
        });
    }

    @Test
    void objectOrderChangesStackingAndCanBeUndone() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            for (var shape : List.of(TechnicalShape.RECTANGLE,TechnicalShape.CIRCLE,TechnicalShape.TRIANGLE))
                invoke(editor,"insertShape",new Class<?>[]{TechnicalShape.class},shape);
            var images = field(editor,"canvasImages",List.class);
            Object top = images.get(2);
            invoke(editor,"reorderCanvasObjects",new Class<?>[]{int.class},-2);
            assertEquals(top,images.get(0));
            var surface = field(editor,"drawingSurface",StudyProblemCanvasSurface.class);
            assertEquals(field(top,"view",javafx.scene.image.ImageView.class),surface.imageLayer().getChildren().get(0));
            invoke(editor,"undo");
            var specType = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject.class;
            assertEquals(TechnicalShape.TRIANGLE.object(javafx.scene.paint.Color.BLACK,2).path(),field(images.get(2),"shape",specType).path());
            invoke(editor,"disposeCanvasInput");
            return null;
        });
    }

    @Test
    void shapeFillSurvivesRotationUndoCopyAndReload() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            invoke(editor, "insertShape", new Class<?>[]{TechnicalShape.class}, TechnicalShape.RECTANGLE);
            var images = field(editor, "canvasImages", List.class);
            var specType = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject.class;
            Object item = images.get(0);
            var view = field(item, "view", javafx.scene.image.ImageView.class);
            double initialWidth = view.getFitWidth();
            field(editor, "shapeFillColor", javafx.scene.control.ColorPicker.class).setValue(javafx.scene.paint.Color.RED);
            field(editor, "fillShapeButton", javafx.scene.control.Button.class).fire();
            var image = view.getImage();
            assertEquals(javafx.scene.paint.Color.RED, image.getPixelReader().getColor((int)image.getWidth()/2, (int)image.getHeight()/2));
            assertEquals(initialWidth, view.getFitWidth(), 0.01);
            invoke(editor, "undo");
            assertEquals("transparent", field(images.get(0), "shape", specType).fill());
            invoke(editor, "redo");
            item = images.get(0);
            invoke(editor, "selectCanvasImage", new Class<?>[]{item.getClass()}, item);
            view = field(item, "view", javafx.scene.image.ImageView.class);
            double unrotatedHeight = view.getImage().getHeight();
            invoke(editor, "rotateSelectedImage", new Class<?>[]{double.class}, 15.0);
            assertTrue(view.getImage().getHeight() > unrotatedHeight);
            assertEquals(15, field(item, "shape", specType).angle());
            tool(editor, "Seleccionar región").fire();
            var selection = field(editor, "vectorSelection", com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.class);
            selection.selectAll(); selection.copy(); selection.paste();
            assertEquals(2, images.size());
            assertEquals(field(images.get(0), "shape", specType), field(images.get(1), "shape", specType));
            Method serialize = TechnicalProblemDialog.class.getDeclaredMethod("canvasStateJson");
            serialize.setAccessible(true);
            TechnicalProblemDialog reopened = newDialog();
            invoke(reopened, "restoreCanvasJson", new Class<?>[]{String.class}, (String)serialize.invoke(editor));
            var restored = field(reopened, "canvasImages", List.class);
            assertEquals(field(images.get(0), "shape", specType), field(restored.get(0), "shape", specType));
            Object restoredItem = restored.get(0);
            invoke(reopened, "selectCanvasImage", new Class<?>[]{restoredItem.getClass()}, restoredItem);
            invoke(reopened, "applyShapeFill", new Class<?>[]{String.class}, "transparent");
            assertEquals("transparent", field(restoredItem, "shape", specType).fill());
            invoke(reopened, "insertShape", new Class<?>[]{TechnicalShape.class}, TechnicalShape.LINE);
            assertTrue(field(reopened, "fillShapeButton", javafx.scene.control.Button.class).isDisabled());
            invoke(editor, "disposeCanvasInput"); invoke(reopened, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void canvasTextStaysEditableAfterStylingRotationCopyAndReload() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            tool(editor, "Agregar texto").fire();
            invoke(editor, "handleCanvasTextClick", new Class<?>[]{MouseEvent.class}, mouse(MouseEvent.MOUSE_CLICKED, 200, 200, true));
            var input = field(editor, "canvasTextEditor", javafx.scene.control.TextArea.class);
            input.setText("Fuerza = masa × aceleración");
            input.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,
                    "", "", javafx.scene.input.KeyCode.ENTER, false, true, false, false));
            var images = field(editor, "canvasImages", List.class);
            assertEquals(1, images.size());
            Object item = images.get(0);
            var specType = com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasTextObject.class;
            assertEquals("Fuerza = masa × aceleración", field(item, "text", specType).text());
            field(editor, "textEffect", javafx.scene.control.ComboBox.class).setValue("Borde sólido");
            invoke(editor, "applySelectedTextStyle");
            invoke(editor, "rotateSelectedImage", new Class<?>[]{double.class}, 15.0);
            assertEquals(15, field(item, "text", specType).angle());
            invoke(editor, "handleCanvasTextClick", new Class<?>[]{MouseEvent.class}, new MouseEvent(
                    MouseEvent.MOUSE_CLICKED, 210, 210, 210, 210, MouseButton.PRIMARY, 2,
                    false, false, false, false, false, false, false, false, false, false, null));
            input = field(editor, "canvasTextEditor", javafx.scene.control.TextArea.class);
            input.setText("Texto corregido");
            input.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,
                    "", "", javafx.scene.input.KeyCode.ENTER, false, true, false, false));
            assertEquals("Texto corregido", field(item, "text", specType).text());
            assertEquals(15, field(item, "text", specType).angle());
            tool(editor, "Seleccionar región").fire();
            var selection = field(editor, "vectorSelection", com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.class);
            selection.selectAll(); selection.copy(); selection.paste();
            assertEquals(2, images.size());
            assertEquals(field(images.get(0), "text", specType), field(images.get(1), "text", specType));
            Method serialize = TechnicalProblemDialog.class.getDeclaredMethod("canvasStateJson");
            serialize.setAccessible(true);
            String json = (String) serialize.invoke(editor);
            TechnicalProblemDialog reopened = newDialog();
            invoke(reopened, "restoreCanvasJson", new Class<?>[]{String.class}, json);
            var restored = field(reopened, "canvasImages", List.class);
            assertEquals(2, restored.size());
            assertEquals("Texto corregido", field(restored.get(0), "text", specType).text());
            assertEquals("Borde sólido", field(restored.get(0), "text", specType).effect());
            assertTrue(field(reopened, "drawingSurface", StudyProblemCanvasSurface.class).applicationInkStrokes().isEmpty());
            invoke(editor, "disposeCanvasInput"); invoke(reopened, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void bothEntryPointsShareToolsAndFullscreenExitKeys() throws Exception {
        runOnFxAndWait(() -> {
            Constructor<TechnicalProblemDialog> constructor = TechnicalProblemDialog.class.getDeclaredConstructor(
                    Window.class, boolean.class, InkInputProvider.class, DrawingProfile.class);
            constructor.setAccessible(true);
            TechnicalProblemDialog normal = newDialog();
            TechnicalProblemDialog express = constructor.newInstance(null, true, InkInputProviderFactory.createDefault(),
                    DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM));
            field(normal, "title", javafx.scene.control.TextField.class).setText("Ejercicio compartido");
            field(normal, "notes", javafx.scene.control.TextArea.class).setText("Conservar estas notas");
            field(normal, "drawingSurface", StudyProblemCanvasSurface.class).setPaperPattern("grid");
            Method serialize = TechnicalProblemDialog.class.getDeclaredMethod("canvasStateJson");
            serialize.setAccessible(true);
            var sidecar = java.nio.file.Files.createTempFile("technical-parity-", ".json");
            try {
                java.nio.file.Files.writeString(sidecar, (String) serialize.invoke(normal));
                invoke(express, "restoreCanvasState", new Class<?>[]{java.nio.file.Path.class}, sidecar);
                assertEquals("Ejercicio compartido", field(express, "title", javafx.scene.control.TextField.class).getText());
                assertEquals("Conservar estas notas", field(express, "notes", javafx.scene.control.TextArea.class).getText());
                assertEquals("grid", field(express, "drawingSurface", StudyProblemCanvasSurface.class).paperPattern());
            } finally { java.nio.file.Files.deleteIfExists(sidecar); }
            for (TechnicalProblemDialog editor : List.of(normal, express)) {
                for (String name : List.of("Lápiz", "Línea", "Medir ángulo", "Mover lienzo", "Seleccionar región"))
                    assertTrue(!tool(editor, name).isDisabled(), name);
                javafx.scene.control.Dialog<?> window = field(editor, "dialog", javafx.scene.control.Dialog.class);
                for (var key : List.of(javafx.scene.input.KeyCode.ESCAPE, javafx.scene.input.KeyCode.F11)) {
                    invoke(editor, "toggleResolverFullscreen");
                    javafx.event.Event.fireEvent(window.getDialogPane().getScene(), new javafx.scene.input.KeyEvent(
                            javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
                    assertTrue(!field(editor, "resolverFullscreen", Boolean.class), "Exit via " + key);
                }
                invoke(editor, "disposeCanvasInput");
            }
            return null;
        });
    }

    @Test
    void unusedCanvasShrinksWithoutRemovingDistantObjectsOrInk() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            StudyProblemCanvasSurface surface = field(editor, "drawingSurface", StudyProblemCanvasSurface.class);
            tool(editor, "Línea").fire();
            invokeBoolean(editor, "handleInkStrokeStart", nativeSample(100, 120, 1));
            invokeBoolean(editor, "handleInkStrokeEnd", nativeSample(160, 180, 2));
            invoke(editor, "flushInk");
            var strokes = surface.applicationInkStrokes();
            invoke(editor, "addCanvasImage", new Class<?>[]{javafx.scene.image.Image.class, double.class, double.class, double.class, boolean.class},
                    new WritableImage(100, 100), 2100.0, 2200.0, 100.0, false);
            surface.ensureLogicalSize(5000, 7000);
            assertTrue(surface.trimUnusedSpace(980, 1800, 300));
            assertTrue(surface.logicalWidth() >= 2500 && surface.logicalWidth() < 5000);
            assertTrue(surface.logicalHeight() >= 2600 && surface.logicalHeight() < 7000);
            assertEquals(strokes, surface.applicationInkStrokes());
            assertEquals(1, surface.imageLayer().getChildren().size());
            Method export = TechnicalProblemDialog.class.getDeclaredMethod("exportCanvas",
                    com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions.class);
            export.setAccessible(true);
            var png = (com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportResult) export.invoke(editor,
                    new com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions(1, 24000000, false, true, 0, 720, 520));
            assertTrue(png.logicalWidth() <= 2500 && png.logicalHeight() <= 2600, "Export excludes unused expanded tiles");
            invoke(editor, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void regionMovesInkImagesAndShapesTogetherAndUndoRestoresThem() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            StudyProblemCanvasSurface surface = field(editor, "drawingSurface", StudyProblemCanvasSurface.class);
            tool(editor, "Línea").fire();
            invokeBoolean(editor, "handleInkStrokeStart", nativeSample(100, 120, 10));
            invokeBoolean(editor, "handleInkStrokeEnd", nativeSample(160, 180, 11));
            invoke(editor, "flushInk");
            invoke(editor, "addCanvasImage", new Class<?>[]{javafx.scene.image.Image.class, double.class, double.class, double.class, boolean.class},
                    new WritableImage(80, 60), 200.0, 120.0, 80.0, false);
            invoke(editor, "addCanvasImage", new Class<?>[]{javafx.scene.image.Image.class, double.class, double.class, double.class, boolean.class},
                    TechnicalShape.values()[0].image(javafx.scene.paint.Color.BLACK, 2), 300.0, 120.0, 80.0, false);
            invoke(editor, "addCanvasImage", new Class<?>[]{javafx.scene.image.Image.class, double.class, double.class, double.class, boolean.class},
                    new WritableImage(20, 20), 700.0, 600.0, 20.0, false);
            tool(editor, "Seleccionar región").fire();
            var objectCheckbox = field(editor, "toolBarNode", javafx.scene.Node.class).lookupAll(".check-box").stream()
                    .map(javafx.scene.control.CheckBox.class::cast).filter(c -> c.getText().equals("Editar objetos"))
                    .findFirst().orElseThrow();
            assertTrue(!objectCheckbox.isDisabled() && objectCheckbox.isSelected());
            var selection = field(editor, "vectorSelection", com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.class);
            invoke(selection, "selectIntersecting", new Class<?>[]{javafx.geometry.Rectangle2D.class, java.util.Set.class},
                    new javafx.geometry.Rectangle2D(90, 100, 320, 150), java.util.Set.of());
            selection.translate(30, 20);
            assertEquals(130, surface.applicationInkStrokes().get(0).points().get(0).x(), 0.001);
            var images = surface.imageLayer().getChildren().stream().filter(javafx.scene.image.ImageView.class::isInstance)
                    .map(javafx.scene.image.ImageView.class::cast).toList();
            assertEquals(230, images.get(0).getLayoutX(), 0.001);
            assertEquals(330, images.get(1).getLayoutX(), 0.001);
            assertEquals(700, images.get(2).getLayoutX(), 0.001);
            selection.transform(1.2, 15);
            assertEquals(1, surface.applicationInkStrokes().size());
            assertEquals(3, images.size());
            selection.copy(); selection.paste();
            assertEquals(2, surface.applicationInkStrokes().size());
            assertEquals(5, surface.imageLayer().getChildren().stream().filter(javafx.scene.image.ImageView.class::isInstance).count());
            selection.deleteSelection();
            assertEquals(1, surface.applicationInkStrokes().size());
            assertEquals(3, surface.imageLayer().getChildren().stream().filter(javafx.scene.image.ImageView.class::isInstance).count());
            invoke(editor, "undo");
            assertEquals(2, surface.applicationInkStrokes().size());
            assertEquals(5, surface.imageLayer().getChildren().stream().filter(javafx.scene.image.ImageView.class::isInstance).count());
            invoke(selection, "selectIntersecting", new Class<?>[]{javafx.geometry.Rectangle2D.class, java.util.Set.class},
                    new javafx.geometry.Rectangle2D(690, 590, 40, 40), java.util.Set.of());
            selection.translate(10, 0);
            assertTrue(surface.imageLayer().getChildren().stream().filter(javafx.scene.image.ImageView.class::isInstance)
                    .anyMatch(node -> Math.abs(node.getLayoutX()-710) < 0.001));
            objectCheckbox.setSelected(false);
            assertTrue(!objectCheckbox.isDisabled());
            invoke(editor, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void fullscreenHidesChromeAndKeepsTitleInCanvasSettings() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            javafx.scene.Node title = field(editor, "titleNode", javafx.scene.Node.class);
            javafx.scene.Parent settings = title.getParent();
            javafx.scene.Node notes = field(editor, "notes", javafx.scene.Node.class);
            assertEquals(settings, notes.getParent());
            javafx.scene.Node ribbon = field(editor, "toolbarShellNode", javafx.scene.Node.class);
            javafx.scene.layout.BorderPane root = field(editor, "rootPane", javafx.scene.layout.BorderPane.class);
            Field fullscreen = TechnicalProblemDialog.class.getDeclaredField("resolverFullscreen");
            fullscreen.setAccessible(true);
            fullscreen.setBoolean(editor, true);
            invoke(editor, "applyResolverFullscreenState");
            assertTrue(!ribbon.isManaged());
            assertTrue(!root.getBottom().isManaged());
            fullscreen.setBoolean(editor, false);
            invoke(editor, "applyResolverFullscreenState");
            assertTrue(ribbon.isManaged());
            assertEquals(settings, title.getParent());
            assertTrue(root.getTop() == null, "the title must not return to the header");
            invoke(editor, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void secondaryDragPansWithoutCreatingInkOrChangingPen() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog editor = newDialog();
            StudyProblemCanvasSurface surface = field(editor, "drawingSurface", StudyProblemCanvasSurface.class);
            javafx.scene.control.ScrollPane scroll = field(editor, "canvasScroll", javafx.scene.control.ScrollPane.class);
            // The unopened dialog has no skin yet; attach its content to the event-dispatch tree.
            scroll.setSkin(new javafx.scene.control.skin.ScrollPaneSkin(scroll));
            scroll.resize(600, 400);
            scroll.applyCss();
            scroll.layout();
            scroll.setVvalue(0.5);
            for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_RELEASED)) {
                double y = type == MouseEvent.MOUSE_PRESSED ? 150 : 200;
                surface.inkInputTarget().fireEvent(new MouseEvent(type, 150, y, 150, y,
                        MouseButton.SECONDARY, 1, false, false, false, false,
                        false, false, type != MouseEvent.MOUSE_RELEASED, false, false, false, null));
            }
            invoke(editor, "flushInk");
            assertTrue(scroll.getVvalue() < 0.5, "v=" + scroll.getVvalue()
                    + " content=" + scroll.getContent().getBoundsInLocal() + " viewport=" + scroll.getViewportBounds()
                    + " targetParent=" + surface.inkInputTarget().getParent());
            assertTrue(surface.inkStrokeStates().isEmpty());
            assertTrue(tool(editor, "Lápiz").isSelected());
            invoke(editor, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void normalAndExpressKeepIdenticalMouseSamplesAndRenderedInk() throws Exception {
        java.util.List<StudyProblemCanvasSurface.InkStrokeState> expected = null;
        WritableImage expectedImage = null;
        for (boolean express : List.of(false, true)) {
            var editor = runOnFxAndWait(() -> {
                var constructor = TechnicalProblemDialog.class.getDeclaredConstructor(
                        Window.class, boolean.class, InkInputProvider.class, DrawingProfile.class);
                constructor.setAccessible(true);
                return express ? constructor.newInstance(null, true, new com.marcosmoreiradev.docupodcaststudio.ink.input.JavaFxMouseInputProvider(),
                        DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM)) : newDialog(new com.marcosmoreiradev.docupodcaststudio.ink.input.JavaFxMouseInputProvider());
            });
            var surface = field(editor, "drawingSurface", StudyProblemCanvasSurface.class);
            // Allow deferred input attachment and initial canvas restoration before sending events.
            runOnFxAndWait(() -> {
                double[][] points = {{100,200},{130,170},{160,200},{180,230},{210,200}};
                for (int i=0; i<points.length; i++) {
                    var type = i==0 ? MouseEvent.MOUSE_PRESSED : i==points.length-1
                            ? MouseEvent.MOUSE_RELEASED : MouseEvent.MOUSE_DRAGGED;
                    surface.inkInputTarget().fireEvent(mouse(type, points[i][0], points[i][1], i<points.length-1));
                }
                invoke(editor, "flushInk");
                return null;
            });
            var strokes = surface.inkStrokeStates();
            assertEquals(1, strokes.size(), "express="+express);
            assertTrue(strokes.getFirst().points().size() >= 5);
            assertTrue(surface.inkCommandStates().stream().anyMatch(command -> command.quadratic()),
                    "both editors must retain curved ink after releasing the mouse: express=" + express);
            var rendered = runOnFxAndWait(surface::snapshotDrawing);
            if (expected != null) {
                var actualPoints = strokes.getFirst().points();
                var expectedPoints = expected.getFirst().points();
                assertEquals(expectedPoints.size(), actualPoints.size());
                for (int i=0; i<actualPoints.size(); i++) {
                    assertEquals(expectedPoints.get(i).x(), actualPoints.get(i).x());
                    assertEquals(expectedPoints.get(i).y(), actualPoints.get(i).y());
                }
                for (int y=150; y<250; y++) for (int x=80; x<240; x++)
                    assertEquals(expectedImage.getPixelReader().getArgb(x,y), rendered.getPixelReader().getArgb(x,y));
            }
            expected = strokes;
            expectedImage = rendered;
            runOnFxAndWait(() -> { invoke(editor, "disposeCanvasInput"); return null; });
        }
    }

    @Test
    void dialogInitializedCanvasCommitsMouseStrokeFromInputLayer() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);

        assertInkLayerCapturesMouse(surface);
        runOnFxAndWait(() -> {
            surface.inkInputTarget().fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 92, 110, true));
            surface.inkInputTarget().fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 140, 156, true));
            surface.inkInputTarget().fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 188, 204, false));
            return null;
        });
        Thread.sleep(60);
        runOnFxAndWait(() -> {
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(1, strokes.size());
    }

    @Test
    void transferredImageLeavesCanvasReadyForMouseInkFallback() throws Exception {
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
            surface.inkInputTarget().fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 110, 130, true));
            surface.inkInputTarget().fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 170, 175, true));
            surface.inkInputTarget().fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 225, 210, false));
            return null;
        });
        Thread.sleep(60);
        runOnFxAndWait(() -> {
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(1, strokes.size());
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

    @Test
    void growingCanvasAcceptsPressureStrokeAcrossTheFormer980PixelBoundary() throws Exception {
        CapturingInkProvider provider = new CapturingInkProvider();
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog(provider));
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);

        runOnFxAndWait(() -> {
            surface.ensureLogicalSize(1960, 1024);
            provider.start(nativeSample(900, 300, .15, .20, 1));
            provider.move(nativeSample(1100, 320, .55, .60, 2));
            provider.end(nativeSample(1500, 340, .90, .95, 3));
            invoke(dialog, "flushInk");
            return null;
        });

        List<StudyProblemCanvasSurface.InkStrokeState> strokes = surface.inkStrokeStates();
        assertEquals(1, strokes.size());
        assertTrue(strokes.getFirst().points().stream().anyMatch(point -> point.x() > 1024),
                "the second horizontal tile must retain ink points");
        assertTrue(strokes.getFirst().points().stream().mapToDouble(StudyProblemCanvasSurface.InkPointState::pressure)
                        .distinct().count() > 1,
                "native pressure must remain variable in the persisted stroke");
    }

    private static TechnicalProblemDialog newDialog() throws Exception {
        return newDialog(InkInputProviderFactory.createDefault());
    }

    @Test
    void compactToolsHaveOneGraphicAndNoDuplicateGlyphLabel() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog dialog = newDialog();
            javafx.scene.Node toolbar = field(dialog, "toolBarNode", javafx.scene.Node.class);
            var tools = toolbar.lookupAll(".technical-problem-icon-button");
            assertTrue(tools.size() >= 2, "Undo and redo must be audited");
            for (var node : tools) {
                var button = (javafx.scene.control.Button) node;
                assertEquals("", button.getText());
                assertEquals(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY, button.getContentDisplay());
                assertTrue(button.getGraphic() != null);
                assertTrue(!button.getAccessibleText().isBlank());
                assertEquals(button.getTooltip().getText(), button.getAccessibleText());
            }
            return null;
        });
    }

    @Test
    void toolbarExposesExplicitNotebookToolsAndStraightLinePersistsAsVectorInk() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);
        runOnFxAndWait(() -> {
            ToggleButton line = tool(dialog, "Línea");
            line.fire();
            invokeBoolean(dialog, "handleInkStrokeStart", nativeSample(100, 120, 1));
            invokeBoolean(dialog, "handleInkStrokeMove", nativeSample(180, 190, 2));
            invokeBoolean(dialog, "handleInkStrokeEnd", nativeSample(260, 220, 3));
            return null;
        });

        assertEquals(1, surface.inkStrokeStates().size());
        assertEquals(2, surface.inkStrokeStates().getFirst().points().size());
        assertEquals(100.0, surface.inkStrokeStates().getFirst().points().getFirst().x());
        assertEquals(260.0, surface.inkStrokeStates().getFirst().points().getLast().x());
    }

    @Test
    void angleMeasurementUsesThreePointsWithoutPollutingTheExportedInk() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);
        runOnFxAndWait(() -> {
            tool(dialog, "Medir ángulo").fire();
            List<javafx.geometry.Point2D> points = field(dialog, "angleMeasurementPoints", List.class);
            assertEquals(3, points.size());
            javafx.geometry.Point2D first = points.get(0);
            javafx.geometry.Point2D vertex = points.get(1);
            invokeBoolean(dialog, "handleInkStrokeStart", nativeSample(first.getX(), first.getY(), 1));
            invokeBoolean(dialog, "handleInkStrokeMove", nativeSample(vertex.getX() - 120, vertex.getY(), 2));
            invokeBoolean(dialog, "handleInkStrokeEnd", nativeSample(vertex.getX() - 120, vertex.getY(), 3));
            assertEquals(vertex.getX() - 120, points.get(0).getX());
            return null;
        });

        Label status = field(dialog, "measurementStatus", Label.class);
        assertTrue(status.getText().contains("90.0°"));
        assertTrue(status.getText().contains("1.571 rad"));
        runOnFxAndWait(() -> {
            assertTrue(surface.inkInputLayer().getChildren().stream().anyMatch(javafx.scene.shape.Arc.class::isInstance));
            tool(dialog, "Medir ángulo").fire();
            assertTrue(field(dialog, "angleMeasurementPoints", List.class).isEmpty());
            return null;
        });
        assertEquals(0, surface.inkStrokeStates().size(),
                "measurement guides must remain temporary and outside the exported ink");
    }

    private static TechnicalProblemDialog newDialog(InkInputProvider provider) throws Exception {
        Constructor<TechnicalProblemDialog> constructor = TechnicalProblemDialog.class
                .getDeclaredConstructor(Window.class, List.class, Map.class,
                        InkInputProvider.class, DrawingProfile.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, List.<DocumentBlock>of(), Map.of(), provider,
                DrawingFeatureCatalog.official().require(DrawingFeatureCatalog.DOCUMENT_PROBLEM));
    }

    @Test
    void catalogueObjectsCanBeResizedDeletedUndoneAndSerialized() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog dialog = newDialog();
            invoke(dialog, "insertShape", new Class<?>[]{TechnicalShape.class}, TechnicalShape.RECTANGLE);
            StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);
            javafx.scene.image.ImageView image = (javafx.scene.image.ImageView) surface.imageLayer().getChildren().getFirst();
            assertEquals(240, image.getFitWidth());
            invoke(dialog, "resizeSelectedImage", new Class<?>[]{double.class}, 1.5);
            assertEquals(360, image.getFitWidth());
            surface.setPaperPattern("grid");
            Method serialize = dialog.getClass().getDeclaredMethod("canvasStateJson");
            serialize.setAccessible(true);
            String json = (String) serialize.invoke(dialog);
            var saved = com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceStateSerializer.fromJson(json);
            assertEquals(1, saved.images().size());
            java.nio.file.Path sidecar = java.nio.file.Files.createTempFile("technical-shape-", ".json");
            try {
                java.nio.file.Files.writeString(sidecar, json);
                TechnicalProblemDialog reopened = newDialog();
                invoke(reopened, "restoreCanvasState", new Class<?>[]{java.nio.file.Path.class}, sidecar);
                StudyProblemCanvasSurface restored = field(reopened, "drawingSurface", StudyProblemCanvasSurface.class);
                assertEquals("grid", restored.paperPattern());
                assertEquals(1, restored.imageLayer().getChildren().size());
                assertEquals(360, ((javafx.scene.image.ImageView) restored.imageLayer().getChildren().getFirst()).getFitWidth());
                invoke(reopened, "disposeCanvasInput");
            } finally {
                java.nio.file.Files.deleteIfExists(sidecar);
            }
            invoke(dialog, "deleteSelectedImage");
            assertEquals(0, surface.imageLayer().getChildren().size());
            invoke(dialog, "undo");
            assertEquals(1, surface.imageLayer().getChildren().size());
            invoke(dialog, "undo");
            assertEquals(240, ((javafx.scene.image.ImageView) surface.imageLayer().getChildren().getFirst()).getFitWidth());
            invoke(dialog, "redo");
            assertEquals(360, ((javafx.scene.image.ImageView) surface.imageLayer().getChildren().getFirst()).getFitWidth());
            invoke(dialog, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void shapeRotationCanBeUndone() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog dialog = newDialog();
            invoke(dialog, "insertShape", new Class<?>[]{TechnicalShape.class}, TechnicalShape.RECTANGLE);
            StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);
            javafx.scene.image.ImageView view = (javafx.scene.image.ImageView) surface.imageLayer().getChildren().getFirst();
            double width = view.getImage().getWidth();
            double height = view.getImage().getHeight();
            invoke(dialog, "rotateSelectedImage", new Class<?>[]{double.class}, 90.0);
            assertEquals(height, view.getImage().getWidth(), 2.0);
            assertEquals(width, view.getImage().getHeight(), 2.0);
            invoke(dialog, "undo");
            view = (javafx.scene.image.ImageView) surface.imageLayer().getChildren().getFirst();
            assertEquals(width, view.getImage().getWidth());
            invoke(dialog, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void notebookPagesKeepIndependentContentAfterReopening() throws Exception {
        runOnFxAndWait(() -> {
            TechnicalProblemDialog dialog = newDialog();
            invoke(dialog, "insertShape", new Class<?>[]{TechnicalShape.class}, TechnicalShape.RECTANGLE);
            invoke(dialog, "addNotebookPage");
            StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);
            assertEquals(0, surface.imageLayer().getChildren().size());
            surface.setPaperPattern("ruled");
            invoke(dialog, "selectNotebookPage", new Class<?>[]{int.class}, 0);
            assertEquals(1, surface.imageLayer().getChildren().size());
            invoke(dialog, "selectNotebookPage", new Class<?>[]{int.class}, 1);
            assertEquals("ruled", surface.paperPattern());
            assertEquals(0, surface.imageLayer().getChildren().size());
            Method serialize = dialog.getClass().getDeclaredMethod("canvasStateJson");
            serialize.setAccessible(true);
            java.nio.file.Path file = java.nio.file.Files.createTempFile("notebook-", ".json");
            try {
                java.nio.file.Files.writeString(file, (String) serialize.invoke(dialog));
                TechnicalProblemDialog reopened = newDialog();
                invoke(reopened, "restoreCanvasState", new Class<?>[]{java.nio.file.Path.class}, file);
                assertEquals(2, field(reopened, "notebookPages", List.class).size());
                invoke(reopened, "selectNotebookPage", new Class<?>[]{int.class}, 0);
                assertEquals(1, field(reopened, "drawingSurface", StudyProblemCanvasSurface.class).imageLayer().getChildren().size());
                invoke(reopened, "disposeCanvasInput");
            } finally { java.nio.file.Files.deleteIfExists(file); }
            invoke(dialog, "disposeCanvasInput");
            return null;
        });
    }

    @Test
    void regionTransformsRemainVectorInk() throws Exception {
        TechnicalProblemDialog dialog = runOnFxAndWait(() -> newDialog());
        runOnFxAndWait(() -> {
            tool(dialog, "Línea").fire();
            invokeBoolean(dialog, "handleInkStrokeStart", nativeSample(100, 120, 1));
            invokeBoolean(dialog, "handleInkStrokeEnd", nativeSample(260, 220, 3));
            tool(dialog, "Seleccionar región").fire();
            var selection = field(dialog, "vectorSelection", com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasStrokeSelection.class);
            selection.selectAll();
            selection.translate(30, 20);
            StudyProblemCanvasSurface surface = field(dialog, "drawingSurface", StudyProblemCanvasSurface.class);
            assertEquals(130, surface.applicationInkStrokes().getFirst().points().getFirst().x());
            selection.transform(1.5, 15);
            assertEquals(1, surface.applicationInkStrokes().size());
            assertEquals(2, surface.applicationInkStrokes().getFirst().points().size());
            assertEquals(0, surface.imageLayer().getChildren().size());
            selection.copy(); selection.paste();
            assertEquals(2, surface.applicationInkStrokes().size());
            selection.deleteSelection();
            assertEquals(1, surface.applicationInkStrokes().size());
            invoke(dialog, "disposeCanvasInput");
            return null;
        });
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

    private static ToggleButton tool(TechnicalProblemDialog dialog, String text) throws Exception {
        javafx.scene.Node toolbar = field(dialog, "toolBarNode", javafx.scene.Node.class);
        return toolbar.lookupAll(".toggle-button").stream()
                .map(ToggleButton.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst().orElseThrow();
    }

    private static void click(TechnicalProblemDialog dialog, double x, double y, long tick) throws Exception {
        invokeBoolean(dialog, "handleInkStrokeStart", nativeSample(x, y, tick));
        invokeBoolean(dialog, "handleInkStrokeEnd", nativeSample(x, y, tick + 1));
    }

    private static InkInputSample nativeSample(double x, double y, long tick) {
        return new InkInputSample(x, y, tick, 1.0, InkInputCursor.PEN, true, false);
    }

    private static InkInputSample nativeSample(double x, double y, double pressure, double rawPressure, long tick) {
        return new InkInputSample(x, y, tick, pressure, InkInputCursor.PEN, true, false,
                rawPressure, "LectureStudio stylus");
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static final class CapturingInkProvider implements InkInputProvider {
        private InkInputListener listener;

        @Override
        public InkInputCapabilities capabilities() {
            return InkInputCapabilities.lectureStudioStylus();
        }

        @Override
        public void attach(javafx.scene.Node target, InkInputListener listener) {
            this.listener = listener;
        }

        @Override
        public void detach() {
            listener = null;
        }

        void start(InkInputSample sample) { listener.onStrokeStart(sample); }
        void move(InkInputSample sample) { listener.onStrokeMove(sample); }
        void end(InkInputSample sample) { listener.onStrokeEnd(sample); }
    }
}
