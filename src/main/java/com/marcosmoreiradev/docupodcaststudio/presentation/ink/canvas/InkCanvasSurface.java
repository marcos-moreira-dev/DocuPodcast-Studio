package com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Shared ink canvas surface for document study and theatre storyboard sketches. */
public class InkCanvasSurface extends TiledInkCanvasSurface {
    private double fixedLogicalWidth = Double.NaN;
    private double fixedLogicalHeight = Double.NaN;
    private boolean fixedLogicalViewport;

    public InkCanvasSurface() {
        super();
    }

    public void resetForFixedEditableState(double width, double height, Color color) {
        clearFixedLogicalViewport();
        resetForEditableState(width, height, color);
        setFixedLogicalViewport(width, height);
    }

    public void setFixedLogicalViewport(double width, double height) {
        fixedLogicalWidth = Math.max(1.0, width);
        fixedLogicalHeight = Math.max(1.0, height);
        fixedLogicalViewport = true;
        applyFixedViewportSize();
    }

    public void clearFixedLogicalViewport() {
        fixedLogicalViewport = false;
        fixedLogicalWidth = Double.NaN;
        fixedLogicalHeight = Double.NaN;
        setClip(null);
    }

    /**
     * Applies the shared z-order and picking contract used by every ink editor.
     * The transparent hit area stays behind optional controls that may live in
     * the input layer, while the complete layer remains above images and ink.
     */
    public void configureInputCapture(boolean layerInteractive, boolean inkActive) {
        if (layerInteractive) {
            inkInputLayer().toFront();
            if (inkActive) {
                inkInputTarget().toBack();
            }
        }
        inkInputLayer().setMouseTransparent(!layerInteractive);
        inkInputLayer().setPickOnBounds(inkActive);
        inkInputTarget().setMouseTransparent(!inkActive);
        inkInputTarget().setVisible(inkActive);
        inkInputTarget().setDisable(!inkActive);
    }

    @Override
    public double logicalWidth() {
        return fixedLogicalViewport ? fixedLogicalWidth : super.logicalWidth();
    }

    @Override
    public double logicalHeight() {
        return fixedLogicalViewport ? fixedLogicalHeight : super.logicalHeight();
    }

    private void applyFixedViewportSize() {
        if (!Double.isFinite(fixedLogicalWidth) || !Double.isFinite(fixedLogicalHeight)) {
            return;
        }
        setMinSize(fixedLogicalWidth, fixedLogicalHeight);
        setPrefSize(fixedLogicalWidth, fixedLogicalHeight);
        setMaxSize(fixedLogicalWidth, fixedLogicalHeight);
        resize(fixedLogicalWidth, fixedLogicalHeight);
        for (Node node : getChildrenUnmodifiable()) {
            if (node instanceof Region region) {
                region.setMinSize(fixedLogicalWidth, fixedLogicalHeight);
                region.setPrefSize(fixedLogicalWidth, fixedLogicalHeight);
                region.setMaxSize(fixedLogicalWidth, fixedLogicalHeight);
                region.resizeRelocate(0, 0, fixedLogicalWidth, fixedLogicalHeight);
            }
        }
        inkInputTarget().setX(0);
        inkInputTarget().setY(0);
        inkInputTarget().setWidth(fixedLogicalWidth);
        inkInputTarget().setHeight(fixedLogicalHeight);
        setClip(new Rectangle(fixedLogicalWidth, fixedLogicalHeight));
    }
}
