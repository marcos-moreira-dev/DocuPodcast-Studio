package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Free rotation from a fixed gesture origin; commits once to preserve source quality. */
public final class CanvasObjectRotationGesture {
    private Consumer<CanvasStrokeSelection.ObjectTransform> transform;
    private Point2D center;
    private double startAngle;
    private double degrees;

    public CanvasObjectRotationGesture(Node handle, Node coordinates,
            Supplier<CanvasStrokeSelection.EditableObject> selection,
            Consumer<Double> preview, Runnable checkpoint, Runnable changed) {
        handle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            var item = selection.get();
            if (item == null) return;
            var bounds = item.bounds();
            center = new Point2D(bounds.getMinX() + bounds.getWidth()/2, bounds.getMinY() + bounds.getHeight()/2);
            Point2D point = coordinates.sceneToLocal(event.getSceneX(), event.getSceneY());
            startAngle = Math.atan2(point.getY()-center.getY(), point.getX()-center.getX());
            degrees = 0;
            transform = item.captureTransform();
            checkpoint.run();
            event.consume();
        });
        handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (transform == null) return;
            Point2D point = coordinates.sceneToLocal(event.getSceneX(), event.getSceneY());
            degrees = Math.toDegrees(Math.atan2(point.getY()-center.getY(), point.getX()-center.getX())-startAngle);
            preview.accept(degrees);
            event.consume();
        });
        handle.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (transform == null || event.getButton() != MouseButton.PRIMARY) return;
            preview.accept(0.0);
            transform.accept(new CanvasStrokeSelection.ObjectTransform(center.getX(), center.getY(), 0, 0, 1, degrees));
            transform = null;
            changed.run();
            event.consume();
        });
    }
}
