package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import com.marcosmoreiradev.docupodcaststudio.ink.input.JavaFxMouseInputProvider;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkCompositionWorkspaceInputTest {
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
    void attachesBeforeWindowVisibilityAndCommitsPressureSamples() throws Exception {
        FakeNativeProvider provider = new FakeNativeProvider();
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        provider));

        runOnFxAndWait(() -> {
            assertNotNull(provider.target);
            assertNotNull(provider.listener);
            provider.listener.onStrokeStart(sample(120, 90, 0.20));
            provider.listener.onStrokeMove(sample(220, 130, 0.55));
            provider.listener.onStrokeEnd(sample(320, 170, 0.90));
            return null;
        });

        InkCompositionResult result = runOnFxAndWait(workspace::result);
        assertEquals(1, result.state().strokes().size());
        assertEquals(0.20, result.state().strokes().get(0).points().get(0).pressure(), 0.001);
        assertTrue(result.state().strokes().get(0).points().stream()
                .anyMatch(point -> point.pressure() >= 0.89));

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void activatingVisibleWorkspaceRebindsNativeHookAndResetsItsCoordinates() throws Exception {
        FakeNativeProvider provider = new FakeNativeProvider();
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        provider));

        runOnFxAndWait(() -> {
            workspace.activateInput();
            assertEquals(2, provider.attachCount);
            assertEquals(1, provider.detachCount);
            assertEquals(1, provider.resetCount);
            return null;
        });

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void documentaryWorkspaceFallsBackToMouseWhenNativeInkIsUnavailable() throws Exception {
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(InkCompositionProfile.documentaryIllustration(), null));

        InkInputCapabilities capabilities = runOnFxAndWait(workspace::inputCapabilitiesForTesting);

        assertEquals("JavaFX mouse", capabilities.providerName());
        assertFalse(capabilities.nativeProvider());

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void realMouseEventsOnCanvasCommitAStroke() throws Exception {
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        new JavaFxMouseInputProvider()));

        runOnFxAndWait(() -> {
            Node target = workspace.inputTargetForTesting();
            Event.fireEvent(target, mouseEvent(MouseEvent.MOUSE_PRESSED, 120, 90, true));
            Event.fireEvent(target, mouseEvent(MouseEvent.MOUSE_DRAGGED, 220, 130, true));
            Event.fireEvent(target, mouseEvent(MouseEvent.MOUSE_RELEASED, 320, 170, false));
            return null;
        });

        InkCompositionResult result = runOnFxAndWait(workspace::result);
        assertEquals(1, result.state().strokes().size());
        assertTrue(result.state().strokes().get(0).points().size() >= 2);

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void controlZAndControlYUndoAndRedoCommittedStrokes() throws Exception {
        FakeNativeProvider provider = new FakeNativeProvider();
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        provider));

        runOnFxAndWait(() -> {
            Scene scene = new Scene(workspace, 1_200, 700);
            provider.listener.onStrokeStart(sample(120, 90, 0.20));
            provider.listener.onStrokeMove(sample(220, 130, 0.55));
            provider.listener.onStrokeEnd(sample(320, 170, 0.90));
            assertEquals(1, workspace.result().state().strokes().size());

            Event.fireEvent(scene, shortcut(KeyCode.Z));
            assertEquals(0, workspace.result().state().strokes().size());

            Event.fireEvent(scene, shortcut(KeyCode.Y));
            assertEquals(1, workspace.result().state().strokes().size());
            return null;
        });

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void completedStrokeResetsNativeCoordinatesBeforeTheNextContact() throws Exception {
        FakeNativeProvider provider = new FakeNativeProvider();
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        provider));

        runOnFxAndWait(() -> {
            provider.listener.onStrokeStart(sample(120, 90, 0.20));
            provider.listener.onStrokeEnd(sample(180, 120, 0.80));
            return null;
        });

        runOnFxAndWait(() -> {
            assertTrue(provider.resetCount > 0);
            workspace.close();
            return null;
        });
    }

    @Test
    void zoomedOutCanvasKeepsAFixedHostThatStackPaneCanCenter() throws Exception {
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        new FakeNativeProvider()));

        runOnFxAndWait(() -> {
            workspace.setZoomForTesting(0.50);
            return null;
        });
        runOnFxAndWait(() -> {
            double expectedWidth = InkCompositionProfile.documentaryIllustration().logicalWidth() * 0.50;
            double expectedHeight = InkCompositionProfile.documentaryIllustration().logicalHeight() * 0.50;
            assertEquals(expectedWidth, workspace.zoomHostForTesting().minWidth(-1), 0.001);
            assertEquals(expectedWidth, workspace.zoomHostForTesting().prefWidth(-1), 0.001);
            assertEquals(expectedHeight, workspace.zoomHostForTesting().minHeight(-1), 0.001);
            assertEquals(expectedHeight, workspace.zoomHostForTesting().prefHeight(-1), 0.001);
            assertEquals(Double.MAX_VALUE, workspace.zoomHostForTesting().getMaxWidth());
            assertEquals(Double.MAX_VALUE, workspace.zoomHostForTesting().getMaxHeight());
            assertEquals(javafx.geometry.Pos.CENTER,
                    workspace.centeredCanvasForTesting().getAlignment());
            return null;
        });

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void switchingToolsKeepsViewportGeometryAndScrollbarPoliciesStable() throws Exception {
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        new FakeNativeProvider()));

        runOnFxAndWait(() -> {
            workspace.setZoomForTesting(0.75);
            return null;
        });
        runOnFxAndWait(() -> {
            double hostWidth = workspace.zoomHostForTesting().prefWidth(-1);
            double hostHeight = workspace.zoomHostForTesting().prefHeight(-1);
            ScrollPane scroll = workspace.canvasScrollForTesting();

            assertTrue(scroll.isFitToWidth());
            assertTrue(scroll.isFitToHeight());
            assertEquals(Region.USE_COMPUTED_SIZE,
                    workspace.centeredCanvasForTesting().getMinWidth(), 0.001);
            assertEquals(Region.USE_COMPUTED_SIZE,
                    workspace.centeredCanvasForTesting().getMinHeight(), 0.001);

            workspace.selectPanModeForTesting();
            assertTrue(scroll.isPannable());
            assertEquals(hostWidth, workspace.zoomHostForTesting().prefWidth(-1), 0.001);
            assertEquals(hostHeight, workspace.zoomHostForTesting().prefHeight(-1), 0.001);

            workspace.selectOrganizeModeForTesting();
            assertFalse(scroll.isPannable());
            workspace.selectDrawModeForTesting();
            assertFalse(scroll.isPannable());
            assertEquals(ScrollPane.ScrollBarPolicy.AS_NEEDED, scroll.getHbarPolicy());
            assertEquals(ScrollPane.ScrollBarPolicy.AS_NEEDED, scroll.getVbarPolicy());
            assertEquals(hostWidth, workspace.zoomHostForTesting().prefWidth(-1), 0.001);
            assertEquals(hostHeight, workspace.zoomHostForTesting().prefHeight(-1), 0.001);
            assertEquals(Region.USE_COMPUTED_SIZE,
                    workspace.centeredCanvasForTesting().getMinWidth(), 0.001);
            assertEquals(Region.USE_COMPUTED_SIZE,
                    workspace.centeredCanvasForTesting().getMinHeight(), 0.001);
            return null;
        });

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void repeatedToolSwitchesDoNotMoveTheCompositionAfterRealLayout() throws Exception {
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        new FakeNativeProvider()));

        runOnFxAndWait(() -> {
            new Scene(workspace, 1_360, 760);
            workspace.resize(1_360, 760);
            workspace.applyCss();
            workspace.layout();
            return null;
        });

        // Allow the one-time initial fit queued by the viewport listener to finish.
        runOnFxAndWait(() -> {
            workspace.setZoomForTesting(0.75);
            workspace.applyCss();
            workspace.layout();

            ScrollPane scroll = workspace.canvasScrollForTesting();
            double viewportWidth = scroll.getViewportBounds().getWidth();
            double viewportHeight = scroll.getViewportBounds().getHeight();
            double hostX = workspace.zoomHostForTesting().getLayoutX();
            double hostY = workspace.zoomHostForTesting().getLayoutY();
            double hostWidth = workspace.zoomHostForTesting().getWidth();
            double hostHeight = workspace.zoomHostForTesting().getHeight();
            double centerWidth = workspace.centeredCanvasForTesting().getWidth();
            double centerHeight = workspace.centeredCanvasForTesting().getHeight();

            for (int i = 0; i < 20; i++) {
                workspace.selectPanModeForTesting();
                workspace.layout();
                assertStableGeometry(workspace, viewportWidth, viewportHeight,
                        hostX, hostY, hostWidth, hostHeight, centerWidth, centerHeight);

                workspace.selectOrganizeModeForTesting();
                workspace.layout();
                assertStableGeometry(workspace, viewportWidth, viewportHeight,
                        hostX, hostY, hostWidth, hostHeight, centerWidth, centerHeight);

                workspace.selectDrawModeForTesting();
                workspace.layout();
                assertStableGeometry(workspace, viewportWidth, viewportHeight,
                        hostX, hostY, hostWidth, hostHeight, centerWidth, centerHeight);
            }
            return null;
        });

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    @Test
    void importedImageInteractionUsesStableLayoutAndExpressStyleHandles() throws Exception {
        InkCompositionWorkspace workspace = runOnFxAndWait(() ->
                new InkCompositionWorkspace(
                        InkCompositionProfile.documentaryIllustration(),
                        null,
                        new FakeNativeProvider()));

        runOnFxAndWait(() -> {
            new Scene(workspace, 1_200, 700);
            workspace.applyCss();
            workspace.resize(1_200, 700);
            workspace.layout();

            ImageView image = workspace.addImageForTesting(
                    new WritableImage(320, 180),
                    100,
                    80,
                    240);
            workspace.selectOrganizeModeForTesting();
            workspace.applyCss();
            workspace.layout();

            assertFalse(workspace.imageLayerForTesting().isCache());
            assertFalse(image.isCache());
            assertTrue(image.getStyleClass().contains("technical-problem-canvas-image"));
            assertEquals(4, workspace.imageResizeHandlesForTesting().size());

            double initialX = image.getLayoutX();
            double initialY = image.getLayoutY();
            Event.fireEvent(image, mouseEvent(MouseEvent.MOUSE_PRESSED, 130, 100, true));
            assertTrue(workspace.imageResizeHandlesForTesting().stream().allMatch(Node::isVisible));
            Event.fireEvent(image, mouseEvent(MouseEvent.MOUSE_DRAGGED, 190, 145, true));
            Event.fireEvent(image, mouseEvent(MouseEvent.MOUSE_RELEASED, 190, 145, false));

            assertEquals(0.0, image.getTranslateX(), 0.001);
            assertEquals(0.0, image.getTranslateY(), 0.001);
            assertTrue(image.getLayoutX() != initialX || image.getLayoutY() != initialY);
            return null;
        });

        runOnFxAndWait(() -> {
            workspace.close();
            return null;
        });
    }

    private static MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type,
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

    private static KeyEvent shortcut(KeyCode code) {
        return new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                code,
                false,
                true,
                false,
                false);
    }

    private static void assertStableGeometry(InkCompositionWorkspace workspace,
                                             double viewportWidth,
                                             double viewportHeight,
                                             double hostX,
                                             double hostY,
                                             double hostWidth,
                                             double hostHeight,
                                             double centerWidth,
                                             double centerHeight) {
        ScrollPane scroll = workspace.canvasScrollForTesting();
        assertEquals(viewportWidth, scroll.getViewportBounds().getWidth(), 0.001);
        assertEquals(viewportHeight, scroll.getViewportBounds().getHeight(), 0.001);
        assertEquals(hostX, workspace.zoomHostForTesting().getLayoutX(), 0.001);
        assertEquals(hostY, workspace.zoomHostForTesting().getLayoutY(), 0.001);
        assertEquals(hostWidth, workspace.zoomHostForTesting().getWidth(), 0.001);
        assertEquals(hostHeight, workspace.zoomHostForTesting().getHeight(), 0.001);
        assertEquals(centerWidth, workspace.centeredCanvasForTesting().getWidth(), 0.001);
        assertEquals(centerHeight, workspace.centeredCanvasForTesting().getHeight(), 0.001);
    }

    private static InkInputSample sample(double x, double y, double pressure) {
        return new InkInputSample(
                x,
                y,
                System.nanoTime(),
                pressure,
                InkInputCursor.PEN,
                true,
                false,
                pressure,
                "test-native");
    }

    private static <T> T runOnFxAndWait(ThrowingSupplier<T> supplier) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
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
        if (failure.get() != null) {
            if (failure.get() instanceof Exception exception) {
                throw exception;
            }
            throw new AssertionError(failure.get());
        }
        return value.get();
    }

    private static final class FakeNativeProvider implements InkInputProvider {
        private Node target;
        private InkInputListener listener;
        private int attachCount;
        private int detachCount;
        private int resetCount;

        @Override
        public InkInputCapabilities capabilities() {
            return InkInputCapabilities.nativeStylus("test-native", true, false, true);
        }

        @Override
        public void attach(Node target, InkInputListener listener) {
            this.target = target;
            this.listener = listener;
            attachCount++;
        }

        @Override
        public void detach() {
            target = null;
            listener = null;
            detachCount++;
        }

        @Override
        public void resetCoordinateState() {
            resetCount++;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
