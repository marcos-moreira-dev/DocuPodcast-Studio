package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Draws the active speech relationship and a compact scene-cast strip without covering the stage map. */
final class TheatreSpatialOverlayLegend {
    private static final double SIDE_MARGIN = 12.0;

    private TheatreSpatialOverlayLegend() {
    }

    static void draw(GraphicsContext gc,
                     double width,
                     double height,
                     String speaker,
                     String target,
                     List<String> cast) {
        if (gc == null || width <= 0 || height <= 0) {
            return;
        }
        List<String> roster = distinctNames(cast);
        int rows = roster.size() > 16 ? 3 : roster.size() > 8 ? 2 : 1;
        double rowHeight = Math.max(18, Math.min(24, height * 0.048));
        double rosterHeight = roster.isEmpty() ? 0 : rows * rowHeight + 10;
        double rosterY = height - rosterHeight - 7;

        if (!roster.isEmpty()) {
            gc.setFill(Color.web("rgba(15, 23, 42, 0.88)"));
            gc.fillRoundRect(SIDE_MARGIN, rosterY, Math.max(1, width - SIDE_MARGIN * 2), rosterHeight, 10, 10);
            drawRoster(gc, roster, SIDE_MARGIN + 6, rosterY + 5,
                    Math.max(1, width - SIDE_MARGIN * 2 - 12), rowHeight, rows);
        }

        String from = safeName(speaker, "SIN HABLANTE");
        String to = safeName(target, "SIN DESTINATARIO");
        String relationship = from + "  →  " + to;
        double relationshipHeight = Math.max(30, Math.min(42, height * 0.078));
        double relationshipY = Math.max(7, rosterY - relationshipHeight - 7);
        double relationshipWidth = Math.min(width - SIDE_MARGIN * 2,
                Math.max(width * 0.38, 46 + relationship.length() * Math.max(6.4, width / 130.0)));
        double relationshipX = (width - relationshipWidth) / 2.0;

        gc.setFill(Color.web("rgba(15, 23, 42, 0.94)"));
        gc.fillRoundRect(relationshipX, relationshipY, relationshipWidth, relationshipHeight, 12, 12);
        gc.setStroke(Color.web("#F59E0B"));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(relationshipX, relationshipY, relationshipWidth, relationshipHeight, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(11, Math.min(16, width / 52.0))));
        gc.fillText(relationship, width / 2.0,
                relationshipY + relationshipHeight * 0.66, Math.max(1, relationshipWidth - 18));
        gc.setTextAlign(TextAlignment.LEFT);
    }

    static List<List<String>> rows(List<String> names, int requestedRows) {
        List<String> distinct = distinctNames(names);
        int rowCount = Math.max(1, Math.min(Math.max(1, requestedRows), Math.max(1, distinct.size())));
        ArrayList<List<String>> result = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            result.add(new ArrayList<>());
        }
        for (int index = 0; index < distinct.size(); index++) {
            result.get(index % rowCount).add(distinct.get(index));
        }
        return result.stream().map(List::copyOf).toList();
    }

    private static void drawRoster(GraphicsContext gc,
                                   List<String> roster,
                                   double x,
                                   double y,
                                   double width,
                                   double rowHeight,
                                   int requestedRows) {
        List<List<String>> rows = rows(roster, requestedRows);
        gc.setFill(Color.web("#E2E8F0"));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.isEmpty()) {
                continue;
            }
            double cellWidth = width / row.size();
            double fontSize = Math.max(8.5, Math.min(12.5, cellWidth / 9.0));
            gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, fontSize));
            for (int column = 0; column < row.size(); column++) {
                double centerX = x + cellWidth * (column + 0.5);
                double baseline = y + rowHeight * rowIndex + rowHeight * 0.68;
                gc.fillText(row.get(column), centerX, baseline, Math.max(1, cellWidth - 8));
            }
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static List<String> distinctNames(List<String> names) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.isBlank()) {
                    result.add(name.strip());
                }
            }
        }
        return List.copyOf(result);
    }

    private static String safeName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
