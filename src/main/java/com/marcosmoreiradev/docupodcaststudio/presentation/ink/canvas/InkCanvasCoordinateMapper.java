package com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas;

import javafx.geometry.Point2D;
import javafx.scene.Node;

/** Maps input events from their real hit target into the logical ink canvas. */
public final class InkCanvasCoordinateMapper {
    private InkCanvasCoordinateMapper() {
    }

    public static Point2D targetLocalToCanvasLocal(Node inputTarget, Node canvasSurface, double x, double y) {
        if (inputTarget == null || canvasSurface == null) {
            return new Point2D(x, y);
        }
        Point2D scenePoint = inputTarget.localToScene(x, y);
        if (scenePoint == null) {
            return new Point2D(x, y);
        }
        return canvasSurface.sceneToLocal(scenePoint);
    }
}
