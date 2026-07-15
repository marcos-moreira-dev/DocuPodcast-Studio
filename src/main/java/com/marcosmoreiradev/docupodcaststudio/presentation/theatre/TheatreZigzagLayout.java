package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/** Shared zigzag grid math for theatre intervention sequence canvases. */
public final class TheatreZigzagLayout {
    public static final int COLUMNS = 4;
    public static final double TOP = 44;
    public static final double ROW_HEIGHT = 84;
    public static final double BOTTOM_PADDING = 44;
    public static final double WIDTH = 800;
    public static final double ACTION_BUTTON_SIZE = 16;
    static final double LEFT = 28;
    static final double CELL_W = 130;
    static final double CELL_H = 56;

    private TheatreZigzagLayout() {
    }

    public static String displayLabel(String alias) {
        if (alias == null || alias.isBlank() || !alias.startsWith("INTERVENCION-")) {
            return alias == null ? "" : alias;
        }
        String numberPart = alias.substring("INTERVENCION-".length());
        try {
            Integer.parseInt(numberPart);
            return "Intervención " + numberPart;
        } catch (NumberFormatException e) {
            return alias;
        }
    }

    public static List<StagePoint> pointsFor(int count, double width) {
        return pointsFor(count, width, LEFT, TOP, CELL_W, CELL_H, ROW_HEIGHT, COLUMNS);
    }

    public static List<StagePoint> pointsFor(int count,
                                             double width,
                                             double left,
                                             double top,
                                             double cellWidth,
                                             double cellHeight,
                                             double rowHeight,
                                             int columns) {
        List<StagePoint> points = new ArrayList<>();
        int safeColumns = Math.max(1, columns);
        double safeCellWidth = Math.max(1.0, cellWidth);
        double safeCellHeight = Math.max(1.0, cellHeight);
        double dx = Math.min(
                Math.max(safeCellWidth + 40, (width - 80 - left) / Math.max(1, safeColumns - 1)),
                safeCellWidth + 80);
        for (int i = 0; i < count; i++) {
            int row = i / safeColumns;
            int col = i % safeColumns;
            if (row % 2 == 1) {
                col = safeColumns - 1 - col;
            }
            points.add(new StagePoint(left + col * dx, top + row * rowHeight, safeCellWidth, safeCellHeight));
        }
        return points;
    }

    public static double heightFor(int aliasCount) {
        return heightFor(aliasCount, TOP, ROW_HEIGHT, BOTTOM_PADDING, COLUMNS);
    }

    public static double heightFor(int aliasCount, double top, double rowHeight, double bottomPadding, int columns) {
        int safeColumns = Math.max(1, columns);
        int rows = Math.max(1, (int) Math.ceil(Math.max(1, aliasCount) / (double) safeColumns));
        return Math.max(300, top + rows * rowHeight + bottomPadding);
    }

    public static void drawArrow(GraphicsContext gc, StagePoint from, StagePoint to) {
        gc.setStroke(Color.web("#90A4C7"));
        gc.setFill(Color.web("#90A4C7"));
        gc.setLineWidth(2);
        double fromX = from.centerX();
        double fromY = from.centerY();
        double toX = to.centerX();
        double toY = to.centerY();
        gc.strokeLine(fromX, fromY, toX, toY);
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowSize = 8;
        double tipX = toX - Math.cos(angle) * 32;
        double tipY = toY - Math.sin(angle) * 20;
        gc.fillPolygon(new double[]{tipX, tipX - arrowSize * Math.cos(angle - Math.PI / 6), tipX - arrowSize * Math.cos(angle + Math.PI / 6)}, new double[]{tipY, tipY - arrowSize * Math.sin(angle - Math.PI / 6), tipY - arrowSize * Math.sin(angle + Math.PI / 6)}, 3);
    }

    public static void drawEditActionButton(GraphicsContext gc, double x, double y) {
        drawActionButtonShell(gc, x, y, "#EEF6FF", "#8FB2EA");
        gc.setStroke(Color.web("#1E3A8A"));
        gc.setFill(Color.web("#1E3A8A"));
        gc.setLineWidth(1.6);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(x + 5.1, y + 11.2, x + 10.8, y + 5.5);
        gc.strokeLine(x + 9.5, y + 4.2, x + 11.9, y + 6.6);
        gc.fillPolygon(
                new double[]{x + 4.3, x + 5.1, x + 6.2},
                new double[]{y + 12.1, y + 10.7, y + 11.8},
                3);
    }

    public static void drawDownloadActionButton(GraphicsContext gc, double x, double y) {
        drawActionButtonShell(gc, x, y, "#F8FAFC", "#94A3B8");
        gc.setStroke(Color.web("#0F172A"));
        gc.setFill(Color.web("#0F172A"));
        gc.setLineWidth(1.55);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(x + 8, y + 3.8, x + 8, y + 9.1);
        gc.strokeLine(x + 5.5, y + 7.2, x + 8, y + 9.7);
        gc.strokeLine(x + 10.5, y + 7.2, x + 8, y + 9.7);
        gc.strokeLine(x + 4.7, y + 11.8, x + 11.3, y + 11.8);
        gc.strokeLine(x + 4.7, y + 11.8, x + 4.7, y + 10.1);
        gc.strokeLine(x + 11.3, y + 11.8, x + 11.3, y + 10.1);
    }

    private static void drawActionButtonShell(GraphicsContext gc, double x, double y, String fill, String stroke) {
        gc.setFill(Color.web(fill));
        gc.setStroke(Color.web(stroke));
        gc.setLineWidth(1.2);
        gc.fillRoundRect(x, y, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, 4, 4);
        gc.strokeRoundRect(x, y, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, 4, 4);
    }

    public static String speakerLabel(String preview) {
        String text = preview == null ? "" : preview.strip();
        if (text.isBlank()) {
            return "";
        }
        if (text.startsWith("(") || text.startsWith("[")) {
            return "ACOTACION";
        }
        int colon = text.indexOf(':');
        if (colon <= 0 || colon > 42) {
            return "";
        }
        return text.substring(0, colon).strip();
    }

    public static String ellipsize(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).stripTrailing() + "...";
    }

    public record StagePoint(double x, double y, double width, double height) {
        public double centerX() {
            return x + width / 2.0;
        }

        public double centerY() {
            return y + height / 2.0;
        }
    }

    public record HitBox(String alias, String blockId, double x, double y, double width, double height,
                          double editX, double editY, double editWidth, double editHeight,
                          double downloadX, double downloadY, double downloadWidth, double downloadHeight) {
        public boolean contains(double px, double py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }

        public boolean containsEdit(double px, double py) {
            return px >= editX && px <= editX + editWidth && py >= editY && py <= editY + editHeight;
        }

        public boolean containsDownload(double px, double py) {
            return px >= downloadX && px <= downloadX + downloadWidth && py >= downloadY && py <= downloadY + downloadHeight;
        }
    }
}
