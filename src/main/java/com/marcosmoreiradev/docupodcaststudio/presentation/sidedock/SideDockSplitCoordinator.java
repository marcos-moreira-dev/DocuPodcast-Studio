package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

/**
 * Keeps both document docks at explicit pixel widths without fighting user divider drags.
 *
 * <p>The expanded left dock is reset to 400 px only when the application is maximized or when a
 * maximized window returns from the taskbar. Between those lifecycle events the first divider is
 * owned by the user. The right dock remains pinned to its intrinsic width.</p>
 */
public final class SideDockSplitCoordinator {
    public static final double MAXIMIZED_LEFT_DOCK_WIDTH = 400.0;
    public static final double MIN_EXPANDED_LEFT_DOCK_WIDTH = 320.0;
    private static final double POSITION_EPSILON = 0.0005;

    private final SplitPane splitPane;
    private final Region leftDock;
    private final Region rightDock;
    private final double documentMinWidth;
    private double desiredExpandedLeftWidth;
    private boolean resetLeftWidthRequested = true;
    private boolean updateQueued;
    private boolean applyingDividerPositions;
    private boolean leftDockWasCollapsed;
    private Stage observedStage;

    private final InvalidationListener requestUpdate = ignored -> requestUpdate();
    private final ChangeListener<Boolean> maximizedListener = (obs, oldValue, maximized) -> {
        if (Boolean.TRUE.equals(maximized)) resetLeftDockForMaximizedLayout();
    };
    private final ChangeListener<Boolean> iconifiedListener = (obs, oldValue, iconified) -> {
        if (Boolean.TRUE.equals(oldValue) && !Boolean.TRUE.equals(iconified)
                && observedStage != null && observedStage.isMaximized()) {
            resetLeftDockForMaximizedLayout();
        }
    };

    private SideDockSplitCoordinator(SplitPane splitPane, Region leftDock, Region rightDock,
                                     double initialLeftWidth, double documentMinWidth) {
        this.splitPane = Objects.requireNonNull(splitPane, "split pane");
        this.leftDock = Objects.requireNonNull(leftDock, "left dock");
        this.rightDock = Objects.requireNonNull(rightDock, "right dock");
        this.desiredExpandedLeftWidth = Math.max(MIN_EXPANDED_LEFT_DOCK_WIDTH, initialLeftWidth);
        this.documentMinWidth = Math.max(1.0, documentMinWidth);
        this.leftDockWasCollapsed = isLeftDockCollapsed();

        splitPane.widthProperty().addListener(requestUpdate);
        listenToIntrinsicWidth(leftDock);
        listenToIntrinsicWidth(rightDock);
        splitPane.sceneProperty().addListener((obs, oldScene, newScene) -> attachScene(newScene));
        attachScene(splitPane.getScene());
        if (splitPane.getDividers().isEmpty()) Platform.runLater(this::installFirstDividerListener);
        else installFirstDividerListener();
        requestUpdate();
    }

    public static SideDockSplitCoordinator install(SplitPane splitPane, Region leftDock,
                                                   Region rightDock, double initialLeftWidth,
                                                   double documentMinWidth) {
        return new SideDockSplitCoordinator(splitPane, leftDock, rightDock,
                initialLeftWidth, documentMinWidth);
    }

    /** Compatibility entry point for older callers; the percentage is no longer persisted. */
    public static SideDockSplitCoordinator install(SplitPane splitPane, Region rightDock,
                                                   double ignoredFirstDividerPosition,
                                                   double documentMinWidth) {
        if (splitPane == null || splitPane.getItems().isEmpty()
                || !(splitPane.getItems().getFirst() instanceof Region leftDock)) {
            throw new IllegalArgumentException("The split pane requires a Region as its left dock");
        }
        return install(splitPane, leftDock, rightDock,
                MAXIMIZED_LEFT_DOCK_WIDTH, documentMinWidth);
    }

    public void resetLeftDockForMaximizedLayout() {
        desiredExpandedLeftWidth = MAXIMIZED_LEFT_DOCK_WIDTH;
        resetLeftWidthRequested = true;
        requestUpdate();
    }

    public void requestUpdate() {
        if (updateQueued) return;
        updateQueued = true;
        Platform.runLater(() -> {
            updateQueued = false;
            updateNow();
        });
    }

    void updateNow() {
        if (splitPane.getDividers().size() < 2 || splitPane.getWidth() <= 1.0) return;
        double total = splitPane.getWidth();
        double rightWidth = intrinsicWidth(rightDock);
        boolean leftDockCollapsed = isLeftDockCollapsed();
        double leftWidth = desiredLeftWidth(total, rightWidth);
        double maximumRight = Math.max(0.0, total - leftWidth - documentMinWidth);
        rightWidth = Math.min(rightWidth, maximumRight);

        double first = clamp(leftWidth / total, 0.0001, 0.95);
        double second = clamp((total - rightWidth) / total,
                first + documentMinWidth / total, 0.9999);
        applyingDividerPositions = true;
        try {
            setIfDifferent(splitPane.getDividers().get(0), first);
            setIfDifferent(splitPane.getDividers().get(1), second);
        } finally {
            applyingDividerPositions = false;
            leftDockWasCollapsed = leftDockCollapsed;
            if (!leftDockCollapsed) resetLeftWidthRequested = false;
        }
    }

    double desiredExpandedLeftWidth() {
        return desiredExpandedLeftWidth;
    }

    private double desiredLeftWidth(double total, double rightWidth) {
        double intrinsic = intrinsicWidth(leftDock);
        boolean collapsed = leftDock.prefWidth(-1) <= WorkspaceSideDock.FOOTER_MAX_WIDTH + 1.0;
        if (collapsed) return intrinsic;
        double maximum = Math.max(1.0, total - rightWidth - documentMinWidth);
        double preferred = resetLeftWidthRequested
                ? MAXIMIZED_LEFT_DOCK_WIDTH : desiredExpandedLeftWidth;
        return clamp(preferred, Math.min(MIN_EXPANDED_LEFT_DOCK_WIDTH, maximum), maximum);
    }

    private void installFirstDividerListener() {
        if (splitPane.getDividers().isEmpty()) return;
        splitPane.getDividers().getFirst().positionProperty().addListener((obs, oldValue, position) -> {
            if (applyingDividerPositions || resetLeftWidthRequested || splitPane.getWidth() <= 1.0) return;
            boolean collapsed = isLeftDockCollapsed();
            if (!collapsed && leftDockWasCollapsed) return;
            if (!collapsed) {
                desiredExpandedLeftWidth = Math.max(MIN_EXPANDED_LEFT_DOCK_WIDTH,
                        position.doubleValue() * splitPane.getWidth());
            }
        });
    }

    private void listenToIntrinsicWidth(Region region) {
        region.minWidthProperty().addListener(requestUpdate);
        region.prefWidthProperty().addListener(requestUpdate);
        region.maxWidthProperty().addListener(requestUpdate);
        region.visibleProperty().addListener(requestUpdate);
        region.managedProperty().addListener(requestUpdate);
        region.parentProperty().addListener(requestUpdate);
    }

    private void attachScene(Scene scene) {
        detachStage();
        if (scene == null) return;
        scene.windowProperty().addListener((obs, oldWindow, newWindow) -> attachWindow(newWindow));
        attachWindow(scene.getWindow());
    }

    private boolean isLeftDockCollapsed() {
        return leftDock.prefWidth(-1) <= WorkspaceSideDock.FOOTER_MAX_WIDTH + 1.0;
    }

    private void attachWindow(Window window) {
        detachStage();
        if (!(window instanceof Stage stage)) return;
        observedStage = stage;
        stage.maximizedProperty().addListener(maximizedListener);
        stage.iconifiedProperty().addListener(iconifiedListener);
        if (stage.isMaximized() && !stage.isIconified()) resetLeftDockForMaximizedLayout();
    }

    private void detachStage() {
        if (observedStage == null) return;
        observedStage.maximizedProperty().removeListener(maximizedListener);
        observedStage.iconifiedProperty().removeListener(iconifiedListener);
        observedStage = null;
    }

    private static void setIfDifferent(SplitPane.Divider divider, double position) {
        if (Math.abs(divider.getPosition() - position) > POSITION_EPSILON) {
            divider.setPosition(position);
        }
    }

    private static double intrinsicWidth(Region region) {
        double min = Math.max(0.0, region.minWidth(-1));
        double max = Math.max(min, region.maxWidth(-1));
        double pref = region.prefWidth(-1);
        if (!Double.isFinite(pref) || pref < 0.0) pref = min;
        return clamp(pref, min, max);
    }

    private static double clamp(double value, double min, double max) {
        double safeMax = Math.max(min, max);
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(safeMax, value));
    }
}
