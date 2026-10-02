package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SideDockSplitCoordinatorTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void collapsedRightRailKeepsIntrinsicWidthAndTouchesOuterEdgeAtSupportedSizes() throws Exception {
        fx(() -> {
            for (double width : new double[]{1366, 1536, 1920}) {
                Region left = region(220, 250, 280);
                Region document = region(480, 900, Double.MAX_VALUE);
                Region dock = region(74, 78, 84);
                SplitPane split = StudioViewportControls.splitPane(left, document, dock);
                Scene scene = new Scene(split, width, 760);
                split.resize(width, 760);
                SideDockSplitCoordinator coordinator = SideDockSplitCoordinator.install(split, dock, 0.20, 480);
                split.applyCss();
                coordinator.updateNow();
                split.layout();

                assertTrue(dock.getWidth() >= 74 && dock.getWidth() <= 84,
                        () -> "rail width at " + width + "px was " + dock.getWidth());
                double workspaceRight = split.localToScene(split.getBoundsInLocal()).getMaxX();
                double dockRight = dock.localToScene(dock.getBoundsInLocal()).getMaxX();
                assertEquals(workspaceRight, dockRight, 1.0,
                        "right rail must touch the workspace outer edge");
                assertTrue(document.getWidth() >= 480);
                assertTrue(scene.getWidth() > 0); // retain the scene for the complete layout assertion
            }
            return null;
        });
    }

    @Test
    void realNestedHostReleasesExpandedWidthWhenPreferenceCollapses() throws Exception {
        fx(() -> {
            SimpleBooleanProperty expanded = new SimpleBooleanProperty(true);
            SideDockModuleRegistry registry = new SideDockModuleRegistry().register(
                    StaticSideDockModule.of(
                            SideDockModuleId.DOCUMENT_STUDY_VIDEO,
                            "Contenido del video",
                            "Configurar video documental",
                            AppIcon.VIDEO,
                            VBox::new));
            SideDockHost dock = new SideDockHost(
                    "document-study-side-dock",
                    new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Estudio documental"),
                    registry,
                    expanded,
                    SideDockLayoutPolicy.standard(560, 720),
                    WorkspaceSideDock.RailPlacement.RIGHT);
            BorderPane productWrapper = new BorderPane();
            dock.installInto(productWrapper);
            StackPane rightDockHost = new StackPane(productWrapper);
            rightDockHost.minWidthProperty().bind(productWrapper.minWidthProperty());
            rightDockHost.prefWidthProperty().bind(productWrapper.prefWidthProperty());
            rightDockHost.maxWidthProperty().bind(productWrapper.maxWidthProperty());

            Region left = region(220, 250, 280);
            Region document = region(480, 900, Double.MAX_VALUE);
            SplitPane split = StudioViewportControls.splitPane(left, document, rightDockHost);
            Scene scene = new Scene(split, 1920, 760);
            split.resize(1920, 760);
            SideDockSplitCoordinator coordinator =
                    SideDockSplitCoordinator.install(split, rightDockHost, 0.20, 480);
            split.applyCss();
            coordinator.updateNow();
            split.layout();
            assertTrue(rightDockHost.getWidth() >= 560);

            expanded.set(false);
            coordinator.updateNow();
            split.layout();

            assertTrue(rightDockHost.getWidth() >= WorkspaceSideDock.COLLAPSED_MIN_WIDTH);
            assertTrue(rightDockHost.getWidth() <= WorkspaceSideDock.COLLAPSED_MAX_WIDTH);
            double workspaceRight = split.localToScene(split.getBoundsInLocal()).getMaxX();
            double dockRight = rightDockHost.localToScene(rightDockHost.getBoundsInLocal()).getMaxX();
            assertEquals(workspaceRight, dockRight, 1.0);
            assertTrue(scene.getWidth() > 0);
            return null;
        });
    }

    @Test
    void maximizedResetIs400ButManualWidthSurvivesResizeUntilNextReset() throws Exception {
        fx(() -> {
            Region left = region(320, 400, Double.MAX_VALUE);
            Region document = region(480, 900, Double.MAX_VALUE);
            Region right = region(78, 78, 78);
            SplitPane split = StudioViewportControls.splitPane(left, document, right);
            new Scene(split, 1400, 760);
            split.resize(1400, 760);
            SideDockSplitCoordinator coordinator = SideDockSplitCoordinator.install(
                    split, left, right, 400, 480);
            split.applyCss();
            coordinator.updateNow();

            assertEquals(400.0, coordinator.desiredExpandedLeftWidth(), 0.5);

            split.getDividers().getFirst().setPosition(0.36);
            assertEquals(504.0, coordinator.desiredExpandedLeftWidth(), 1.0);
            coordinator.updateNow();
            assertEquals(0.36, split.getDividers().getFirst().getPosition(), 0.01);

            split.resize(850, 760);
            coordinator.updateNow();
            assertTrue(split.getDividers().getFirst().getPosition() * 850 <= 320.0);
            assertEquals(504.0, coordinator.desiredExpandedLeftWidth(), 1.0,
                    "the narrow-window clamp must not overwrite the user's width");

            split.resize(1400, 760);
            coordinator.updateNow();
            assertEquals(0.36, split.getDividers().getFirst().getPosition(), 0.01);

            coordinator.resetLeftDockForMaximizedLayout();
            coordinator.updateNow();
            assertEquals(400.0, coordinator.desiredExpandedLeftWidth(), 0.5);
            assertEquals(400.0 / 1400.0,
                    split.getDividers().getFirst().getPosition(), 0.01);
            return null;
        });
    }

    @Test
    void expandingCollapsedDockDoesNotTreatAutomaticLayoutAsManualResize() throws Exception {
        fx(() -> {
            Region left = region(74, 78, 84);
            Region document = region(480, 900, Double.MAX_VALUE);
            Region right = region(78, 78, 78);
            SplitPane split = StudioViewportControls.splitPane(left, document, right);
            new Scene(split, 1400, 760);
            split.resize(1400, 760);
            SideDockSplitCoordinator coordinator = SideDockSplitCoordinator.install(
                    split, left, right, 400, 480);
            split.applyCss();
            coordinator.updateNow();

            left.setMinWidth(320);
            left.setPrefWidth(400);
            left.setMaxWidth(Double.MAX_VALUE);
            split.getDividers().getFirst().setPosition(0.23);
            coordinator.updateNow();

            assertEquals(400.0 / 1400.0,
                    split.getDividers().getFirst().getPosition(), 0.01,
                    "expanding after maximization must retain the pending 400 px reset");

            split.getDividers().getFirst().setPosition(0.36);
            assertEquals(504.0, coordinator.desiredExpandedLeftWidth(), 1.0);
            left.setMinWidth(74);
            left.setPrefWidth(78);
            left.setMaxWidth(84);
            coordinator.updateNow();

            left.setMinWidth(320);
            left.setPrefWidth(400);
            left.setMaxWidth(Double.MAX_VALUE);
            split.getDividers().getFirst().setPosition(0.23);
            coordinator.updateNow();

            assertEquals(0.36, split.getDividers().getFirst().getPosition(), 0.01,
                    "ordinary collapse/expand must restore the user's last manual width");
            return null;
        });
    }

    private static Region region(double min, double pref, double max) {
        Region region = new Region();
        region.setMinWidth(min);
        region.setPrefWidth(pref);
        region.setMaxWidth(max);
        return region;
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
