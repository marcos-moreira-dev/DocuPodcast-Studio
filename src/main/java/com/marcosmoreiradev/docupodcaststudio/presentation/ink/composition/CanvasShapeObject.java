package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Editable geometry and appearance, independent of the display image. */
public record CanvasShapeObject(String path, String stroke, double width, String fill, double angle) {
    public boolean closed() { return path.strip().toUpperCase(java.util.Locale.ROOT).endsWith("Z"); }

    public Image render() {
        SVGPath node = new SVGPath();
        node.setContent(path);
        node.setFill(closed() ? Color.web(fill) : Color.TRANSPARENT);
        node.setStroke(Color.web(stroke));
        node.setStrokeWidth(width);
        node.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Group group = new Group(node);
        group.setRotate(angle);
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setTransform(new Scale(10, 10));
        return group.snapshot(parameters, null);
    }

    public CanvasShapeObject filled(String color) { return new CanvasShapeObject(path, stroke, width, color, angle); }
    public CanvasShapeObject rotated(double degrees) { return new CanvasShapeObject(path, stroke, width, fill, (angle + degrees) % 360); }

    public String encode() {
        return java.util.stream.Stream.of(path, stroke, Double.toString(width), fill, Double.toString(angle))
                .map(s -> Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)))
                .collect(java.util.stream.Collectors.joining("."));
    }

    public static CanvasShapeObject decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String[] v = java.util.Arrays.stream(encoded.split("\\.", -1))
                    .map(s -> new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8)).toArray(String[]::new);
            double width = Double.parseDouble(v[2]), angle = Double.parseDouble(v[4]);
            if (v.length != 5 || !Double.isFinite(width) || width <= 0 || !Double.isFinite(angle)) return null;
            Color.web(v[1]); Color.web(v[3]);
            return new CanvasShapeObject(v[0], v[1], width, v[3], angle);
        } catch (RuntimeException malformed) { return null; }
    }
}
