package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

/** Compact catalogue shared by the normal and Express technical editor. */
enum TechnicalShape {
    LINE("Línea", "M 12 12 L 88 88"),
    RECTANGLE("Rectángulo", "M 12 22 H 88 V 78 H 12 Z"),
    SQUARE("Cuadrado", "M 15 15 H 85 V 85 H 15 Z"),
    ELLIPSE("Elipse", "M 12 50 A 38 28 0 1 0 88 50 A 38 28 0 1 0 12 50 Z"),
    CIRCLE("Círculo", "M 12 50 A 38 38 0 1 0 88 50 A 38 38 0 1 0 12 50 Z"),
    TRIANGLE("Triángulo", "M 50 12 L 88 85 H 12 Z"),
    RIGHT_TRIANGLE("Triángulo rectángulo", "M 15 15 V 85 H 85 Z"),
    DIAMOND("Rombo", "M 50 10 L 90 50 L 50 90 L 10 50 Z"),
    PENTAGON("Pentágono", "M 50 10 L 90 40 L 75 88 H 25 L 10 40 Z"),
    HEXAGON("Hexágono", "M 30 15 H 70 L 92 50 L 70 85 H 30 L 8 50 Z"),
    ARROW_RIGHT("Flecha derecha", "M 10 35 H 60 V 15 L 90 50 L 60 85 V 65 H 10 Z"),
    ARROW_LEFT("Flecha izquierda", "M 90 35 H 40 V 15 L 10 50 L 40 85 V 65 H 90 Z"),
    ARROW_UP("Flecha arriba", "M 35 90 V 40 H 15 L 50 10 L 85 40 H 65 V 90 Z"),
    ARROW_DOWN("Flecha abajo", "M 35 10 V 60 H 15 L 50 90 L 85 60 H 65 V 10 Z"),
    STAR("Estrella", "M 50 8 L 61 36 L 92 38 L 68 58 L 76 90 L 50 72 L 24 90 L 32 58 L 8 38 L 39 36 Z"),
    BUBBLE("Bocadillo", "M 15 15 H 85 V 70 H 48 L 25 90 V 70 H 15 Z");

    final String label;
    private final String path;
    TechnicalShape(String label, String path) { this.label = label; this.path = path; }

    SVGPath graphic(Color color, double width) {
        SVGPath shape = new SVGPath();
        shape.setContent(path);
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(color);
        shape.setStrokeWidth(width);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return shape;
    }

    com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject object(Color color, double width) {
        return new com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.CanvasShapeObject(
                path, color.toString(), Math.max(0.5, width / 2.4), "transparent", 0);
    }

    WritableImage image(Color color, double width) {
        Group group = new Group(graphic(color, Math.max(0.5, width / 2.4)));
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setTransform(new Scale(10, 10));
        return group.snapshot(parameters, null);
    }
}
