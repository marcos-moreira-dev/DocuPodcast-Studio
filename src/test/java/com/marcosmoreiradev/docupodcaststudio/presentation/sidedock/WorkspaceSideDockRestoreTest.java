package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkspaceSideDockRestoreTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void collapseKeepsActiveModuleAndItsRailButtonRestoresTheSameViewNode() throws Exception {
        AtomicInteger creations = new AtomicInteger();
        VBox reusable = new VBox();
        SideDockModule module = new SideDockModule() {
            @Override public SideDockModuleId id() { return SideDockModuleId.DOCUMENT_STUDY_VIDEO; }
            @Override public String title() { return "Video documental"; }
            @Override public String tooltip() { return "Configurar video"; }
            @Override public String iconText() { return "video"; }
            @Override public boolean supports(SideDockContext context) { return true; }
            @Override public Parent createView(SideDockContext context) {
                creations.incrementAndGet();
                return reusable;
            }
        };
        WorkspaceSideDock dock = fx(() -> new WorkspaceSideDock(
                new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Documento"),
                new SideDockModuleRegistry().register(module), false,
                WorkspaceSideDock.RailPlacement.RIGHT));
        Node first = fx(() -> ((BorderPane) dock.getCenter()).getCenter());

        fx(() -> { dock.setCollapsed(true); return null; });

        assertEquals(SideDockModuleId.DOCUMENT_STUDY_VIDEO, dock.activeModuleId());
        assertFalse(dock.expandedProperty().get());
        assertTrue(fx(() -> ((VBox) dock.getRight()).getChildren().stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .noneMatch(button -> button.getStyleClass().contains("side-dock-rail-button-active"))));
        Button moduleButton = fx(() -> ((VBox) dock.getRight()).getChildren().stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .findFirst().orElseThrow());

        fx(() -> { moduleButton.fire(); return null; });
        Node restored = fx(() -> ((BorderPane) dock.getCenter()).getCenter());

        assertTrue(dock.expandedProperty().get());
        assertSame(first, restored);
        assertEquals(1, creations.get());
    }

    @Test
    void collapsedRailUsesTheReferenceFooterWidthRange() throws Exception {
        SideDockModule module = new SideDockModule() {
            @Override public SideDockModuleId id() { return SideDockModuleId.DOCUMENT_STUDY_VIDEO; }
            @Override public String title() { return "Video documental"; }
            @Override public String tooltip() { return "Configurar video"; }
            @Override public String iconText() { return "video"; }
            @Override public boolean supports(SideDockContext context) { return true; }
            @Override public Parent createView(SideDockContext context) { return new VBox(); }
        };
        WorkspaceSideDock dock = fx(() -> new WorkspaceSideDock(
                new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Documento"),
                new SideDockModuleRegistry().register(module), true,
                WorkspaceSideDock.RailPlacement.LEFT,
                VBox::new));

        assertEquals(WorkspaceSideDock.FOOTER_MIN_WIDTH, dock.getMinWidth());
        assertEquals(WorkspaceSideDock.FOOTER_WIDTH, dock.getPrefWidth());
        assertEquals(WorkspaceSideDock.FOOTER_MAX_WIDTH, dock.getMaxWidth());
    }

    @Test
    void productIntentCanActivateASpecificModuleWithoutDependingOnRegistrationOrder() throws Exception {
        SideDockModule first = module(SideDockModuleId.DOCUMENT_STUDY_VIDEO);
        SideDockModule intended = module(SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM);
        WorkspaceSideDock dock = fx(() -> new WorkspaceSideDock(
                new SideDockContext(WorkspaceKind.DOCUMENT_READER, "Documento"),
                new SideDockModuleRegistry().register(first).register(intended), true));

        fx(() -> { dock.activateModule(SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM); return null; });

        assertEquals(SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM, dock.activeModuleId());
        assertTrue(dock.expandedProperty().get());
    }

    private static SideDockModule module(SideDockModuleId id) {
        return new SideDockModule() {
            @Override public SideDockModuleId id() { return id; }
            @Override public String title() { return id.name(); }
            @Override public String tooltip() { return id.name(); }
            @Override public String iconText() { return "module"; }
            @Override public boolean supports(SideDockContext context) { return true; }
            @Override public Parent createView(SideDockContext context) { return new VBox(); }
        };
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
