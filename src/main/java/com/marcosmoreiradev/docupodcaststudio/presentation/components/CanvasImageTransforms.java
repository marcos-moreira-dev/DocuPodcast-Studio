package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkSelectionGeometry;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint;

/** Image adaptation of the shared canvas selection geometry; preserves transparency. */
public final class CanvasImageTransforms {
    private CanvasImageTransforms() { }

    public static Image rotate(Image source, double degrees) {
        var transform = new InkSelectionGeometry.Transform(0, 0, 1, 1, degrees);
        var x = InkSelectionGeometry.transformPoint(InkPoint.of(1, 0, 0, 1), 0, 0, transform);
        var y = InkSelectionGeometry.transformPoint(InkPoint.of(0, 1, 0, 1), 0, 0, transform);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setTransform(new Affine(x.x(), y.x(), 0, x.y(), y.y(), 0));
        return new ImageView(source).snapshot(parameters, null);
    }
}
