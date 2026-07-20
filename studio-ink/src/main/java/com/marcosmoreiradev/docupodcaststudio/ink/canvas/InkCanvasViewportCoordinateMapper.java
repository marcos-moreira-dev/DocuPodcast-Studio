package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.Optional;

/**
 * Maps native ink events into a logical ink surface and rejects events that do
 * not belong to the visible canvas target.
 */
public final class InkCanvasViewportCoordinateMapper {
    private InkCanvasViewportCoordinateMapper() {
    }

    public static Optional<Point2D> mapInside(Node inputTarget,
                                              Node canvasSurface,
                                              double targetLocalX,
                                              double targetLocalY,
                                              double logicalWidth,
                                              double logicalHeight) {
        if (inputTarget == null || canvasSurface == null) {
            return Optional.empty();
        }
        if (!insideTarget(inputTarget, targetLocalX, targetLocalY)) {
            return Optional.empty();
        }
        Point2D canvasPoint = InkCanvasCoordinateMapper.targetLocalToCanvasLocal(
                inputTarget,
                canvasSurface,
                targetLocalX,
                targetLocalY);
        return insideLogicalCanvas(canvasPoint, logicalWidth, logicalHeight)
                ? Optional.of(canvasPoint)
                : Optional.empty();
    }

    private static boolean insideTarget(Node inputTarget, double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            return false;
        }
        Bounds bounds = inputTarget.getLayoutBounds();
        return bounds != null
                && x >= bounds.getMinX()
                && y >= bounds.getMinY()
                && x <= bounds.getMaxX()
                && y <= bounds.getMaxY();
    }

    private static boolean insideLogicalCanvas(Point2D point, double width, double height) {
        return point != null
                && Double.isFinite(point.getX())
                && Double.isFinite(point.getY())
                && Double.isFinite(width)
                && Double.isFinite(height)
                && point.getX() >= 0.0
                && point.getY() >= 0.0
                && point.getX() <= width
                && point.getY() <= height;
    }
}
