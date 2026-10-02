package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleConsumer;

/**
 * Stable shared zoom/scroll host for ink canvases. Its preferred extent depends
 * only on logical canvas size and zoom, never on viewport bounds.
 */
public final class InkCanvasZoomPane extends BorderPane {
    private final DoubleSupplier logicalWidth;
    private final DoubleSupplier logicalHeight;
    private final Group scaledContent;
    private final Scale contentScale = new Scale(1.0, 1.0, 0.0, 0.0);
    private final ZoomHost host;
    private final ScrollPane scrollPane;
    private final ReadOnlyDoubleWrapper appliedZoom = new ReadOnlyDoubleWrapper(1.0);
    private DoubleConsumer zoomApplied = ignored -> { };
    private double pendingZoom = 1.0;
    private boolean applyQueued;
    private boolean centerAfterLayout;

    public InkCanvasZoomPane(Node content, DoubleSupplier logicalWidth, DoubleSupplier logicalHeight) {
        this.logicalWidth = Objects.requireNonNull(logicalWidth, "logical width");
        this.logicalHeight = Objects.requireNonNull(logicalHeight, "logical height");
        this.scaledContent = new Group(Objects.requireNonNull(content, "content"));
        // Node.scaleX/scaleY use the centre of the node as their implicit pivot.
        // The host positions scaled extents from their top-left origin, so using
        // those properties displaced the canvas whenever zoom was below 100%.
        this.scaledContent.getTransforms().setAll(contentScale);
        this.host = new ZoomHost(scaledContent);
        this.host.getStyleClass().add("ink-canvas-zoom-host");
        this.scrollPane = new ScrollPane(host);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setFitToHeight(true);
        this.scrollPane.setPannable(false);
        this.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.scrollPane.setMinSize(0, 0);
        this.scrollPane.getStyleClass().add("ink-canvas-zoom-scroll");
        setMinSize(0, 0);
        setCenter(scrollPane);
    }

    public ScrollPane scrollPane() { return scrollPane; }
    /** Exposed for host layout diagnostics and JavaFX component tests. */
    public Pane contentHost() { return host; }
    public ReadOnlyDoubleProperty appliedZoomProperty() { return appliedZoom.getReadOnlyProperty(); }
    public double appliedZoom() { return appliedZoom.get(); }

    public void setOnZoomApplied(DoubleConsumer callback) {
        zoomApplied = callback == null ? ignored -> { } : callback;
    }

    public void setPannable(boolean pannable) { scrollPane.setPannable(pannable); }

    /** Centers the logical canvas after the pending zoom has reached a stable layout. */
    public void centerContent() {
        centerAfterLayout = true;
        if (!applyQueued && Math.abs(pendingZoom - appliedZoom.get()) < 0.000_001) {
            Platform.runLater(() -> {
                if (!centerAfterLayout) return;
                centerAfterLayout = false;
                scrollPane.setHvalue(0.5);
                scrollPane.setVvalue(0.5);
                zoomApplied.accept(appliedZoom.get());
            });
        }
    }

    public void setZoom(double requestedZoom) {
        pendingZoom = clampZoom(requestedZoom);
        if (applyQueued) return;
        applyQueued = true;
        Platform.runLater(this::applyPendingZoom);
    }

    public void refreshContentExtent() {
        host.requestLayout();
        scrollPane.requestLayout();
        requestLayout();
    }

    private void applyPendingZoom() {
        applyQueued = false;
        double next = pendingZoom;
        if (Math.abs(next - appliedZoom.get()) < 0.000_001) return;
        Bounds viewport = scrollPane.getViewportBounds();
        double viewportWidth = Math.max(0, viewport.getWidth());
        double viewportHeight = Math.max(0, viewport.getHeight());
        double oldWidth = extentWidth(appliedZoom.get());
        double oldHeight = extentHeight(appliedZoom.get());
        double centerRatioX = visibleCenterRatio(scrollPane.getHvalue(), viewportWidth, oldWidth);
        double centerRatioY = visibleCenterRatio(scrollPane.getVvalue(), viewportHeight, oldHeight);

        appliedZoom.set(next);
        contentScale.setX(next);
        contentScale.setY(next);
        host.requestLayout();
        scrollPane.requestLayout();
        requestLayout();
        Platform.runLater(() -> {
            host.applyCss();
            host.layout();
            if (centerAfterLayout) {
                centerAfterLayout = false;
                scrollPane.setHvalue(0.5);
                scrollPane.setVvalue(0.5);
            } else {
                Bounds currentViewport = scrollPane.getViewportBounds();
                scrollPane.setHvalue(scrollValueForCenter(centerRatioX, currentViewport.getWidth(), extentWidth(next)));
                scrollPane.setVvalue(scrollValueForCenter(centerRatioY, currentViewport.getHeight(), extentHeight(next)));
            }
            zoomApplied.accept(next);
        });
    }

    private double extentWidth(double zoom) { return Math.max(1.0, logicalWidth.getAsDouble()) * zoom; }
    private double extentHeight(double zoom) { return Math.max(1.0, logicalHeight.getAsDouble()) * zoom; }

    private static double visibleCenterRatio(double scroll, double viewport, double extent) {
        double overflow = Math.max(0, extent - viewport);
        if (overflow <= 0 || extent <= 0) return 0.5;
        return clamp((clamp(scroll, 0, 1) * overflow + viewport / 2.0) / extent, 0, 1);
    }

    private static double scrollValueForCenter(double ratio, double viewport, double extent) {
        double overflow = Math.max(0, extent - viewport);
        if (overflow <= 0) return 0.5;
        return clamp((ratio * extent - viewport / 2.0) / overflow, 0, 1);
    }

    private static double clampZoom(double value) {
        return Double.isFinite(value) ? clamp(value, 0.1, 8.0) : 1.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private final class ZoomHost extends Pane {
        private ZoomHost(Node content) {
            getChildren().add(content);
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        @Override protected double computePrefWidth(double height) { return extentWidth(appliedZoom.get()); }
        @Override protected double computePrefHeight(double width) { return extentHeight(appliedZoom.get()); }
        @Override protected double computeMinWidth(double height) { return extentWidth(appliedZoom.get()); }
        @Override protected double computeMinHeight(double width) { return extentHeight(appliedZoom.get()); }

        @Override protected void layoutChildren() {
            double scaledWidth = extentWidth(appliedZoom.get());
            double scaledHeight = extentHeight(appliedZoom.get());
            double x = Math.max(0, (getWidth() - scaledWidth) / 2.0);
            double y = Math.max(0, (getHeight() - scaledHeight) / 2.0);
            scaledContent.relocate(x, y);
        }
    }
}
