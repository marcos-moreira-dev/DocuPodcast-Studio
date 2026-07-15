package com.marcosmoreiradev.docupodcaststudio.application.ink;

import java.util.List;

/** Vector stroke stored independently from any JavaFX surface. */
public record InkStroke(InkTool tool, String color, double width, List<InkPoint> points) {
    public InkStroke {
        tool = tool == null ? InkTool.DRAW : tool;
        color = color == null || color.isBlank() ? "#000000ff" : color;
        width = Math.max(1.0, Double.isFinite(width) ? width : 1.0);
        points = points == null ? List.of() : List.copyOf(points);
    }

    public static InkStroke draw(String color, double width, List<InkPoint> points) {
        return new InkStroke(InkTool.DRAW, color, width, points);
    }

    public static InkStroke erase(double width, List<InkPoint> points) {
        return new InkStroke(InkTool.ERASE, "#00000000", width, points);
    }
}
