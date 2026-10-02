package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.HashMap;
import java.util.Map;

/** Tool cursors reuse the toolbar icon, with a precise crosshair at the click point. Use on the FX thread. */
public final class ToolIconCursor {
    private static final Map<String, Cursor> CACHE = new HashMap<>();

    private ToolIconCursor() { }

    public static Cursor of(String icon) {
        return CACHE.computeIfAbsent(icon, ToolIconCursor::create);
    }

    private static Cursor create(String icon) {
        Pane pane = new Pane();
        pane.resize(32, 32);
        LucideIconView symbol = LucideIconView.of(icon);
        symbol.resizeRelocate(11, 11, 20, 20);
        // A white outline keeps the marker legible over dark images as well as white paper.
        symbol.setEffect(new javafx.scene.effect.DropShadow(2, Color.WHITE));
        pane.getChildren().add(symbol);
        for (double width : new double[]{3, 1}) {
            for (Line line : new Line[]{new Line(1, 5, 9, 5), new Line(5, 1, 5, 9)}) {
                line.setStroke(width == 3 ? Color.WHITE : Color.BLACK);
                line.setStrokeWidth(width);
                pane.getChildren().add(line);
            }
        }
        pane.applyCss();
        pane.layout();
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        parameters.setViewport(new javafx.geometry.Rectangle2D(0, 0, 32, 32));
        return new ImageCursor(pane.snapshot(parameters, null), 5, 5);
    }
}
