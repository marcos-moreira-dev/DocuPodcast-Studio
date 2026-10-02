package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Editable text source retained independently of its display/export image. */
public record CanvasTextObject(String text, String family, String color, String effect,
                               String effectColor, double angle) {
    public Image render() {
        Text node = new Text(text);
        node.setFont(Font.font(family, 32));
        node.setWrappingWidth(Math.min(1600, Math.max(32, node.getLayoutBounds().getWidth())));
        node.setFill(Color.web(color));
        if ("Borde sólido".equals(effect)) {
            node.setStroke(Color.web(effectColor));
            node.setStrokeWidth(1.2);
            node.setStrokeType(javafx.scene.shape.StrokeType.OUTSIDE);
        } else if ("Sombra".equals(effect)) {
            node.setEffect(new DropShadow(5, 2, 2, Color.web(effectColor)));
        }
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setTransform(new Scale(2, 2));
        Image image = node.snapshot(parameters, null);
        return Math.abs(angle) < 0.0001 ? image
                : com.marcosmoreiradev.docupodcaststudio.presentation.components.CanvasImageTransforms.rotate(image, angle);
    }

    public CanvasTextObject rotated(double degrees) {
        return new CanvasTextObject(text, family, color, effect, effectColor, angle + degrees);
    }

    public String encode() {
        return java.util.stream.Stream.of(text, family, color, effect, effectColor, Double.toString(angle))
                .map(s -> Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)))
                .collect(java.util.stream.Collectors.joining("."));
    }

    public static CanvasTextObject decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String[] values = java.util.Arrays.stream(encoded.split("\\.", -1))
                    .map(s -> new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8)).toArray(String[]::new);
            return new CanvasTextObject(values[0], values[1], values[2], values[3], values[4], Double.parseDouble(values[5]));
        } catch (RuntimeException malformed) { return null; }
    }
}
