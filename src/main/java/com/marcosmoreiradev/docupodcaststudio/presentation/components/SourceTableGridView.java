package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Transversal read-only table preview used by document visual blocks.
 *
 * <p>This is intentionally not a spreadsheet/table editor. It renders table-shaped source
 * evidence with product styling so DOCX tables remain legible without becoming narratable
 * or editable content.</p>
 */
public final class SourceTableGridView extends VBox {
    private static final int DEFAULT_MAX_ROWS = Integer.MAX_VALUE;

    public SourceTableGridView(List<List<String>> rows) {
        this(rows, DEFAULT_MAX_ROWS);
    }

    public SourceTableGridView(List<List<String>> rows, int maxRows) {
        getStyleClass().add(AppStyles.UI_SOURCE_TABLE_PREVIEW);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        setMaxWidth(Double.MAX_VALUE);
        if (rows == null || rows.isEmpty()) {
            Label empty = new Label("Tabla sin vista previa legible.");
            empty.getStyleClass().add(AppStyles.UI_SOURCE_TABLE_EMPTY);
            getChildren().add(empty);
            return;
        }
        int columnCount = Math.max(1, rows.stream().mapToInt(List::size).max().orElse(1));
        int visibleRows = Math.min(rows.size(), Math.max(1, maxRows));
        GridPane grid = new GridPane();
        grid.getStyleClass().add(AppStyles.UI_SOURCE_TABLE_GRID);
        grid.setMaxWidth(Double.MAX_VALUE);
        for (int colIndex = 0; colIndex < columnCount; colIndex++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setPercentWidth(100.0 / columnCount);
            grid.getColumnConstraints().add(constraints);
        }
        for (int rowIndex = 0; rowIndex < visibleRows; rowIndex++) {
            List<String> row = rows.get(rowIndex);
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                String value = colIndex < row.size() ? row.get(colIndex) : "";
                Label cell = new Label(value == null || value.isBlank() ? " " : value.strip());
                cell.setWrapText(true);
                cell.setTextOverrun(OverrunStyle.CLIP);
                cell.setMinWidth(90);
                cell.setMinHeight(Region.USE_PREF_SIZE);
                cell.setPrefHeight(Region.USE_COMPUTED_SIZE);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMaxHeight(Double.MAX_VALUE);
                cell.getStyleClass().add(AppStyles.UI_SOURCE_TABLE_CELL);
                GridPane.setHgrow(cell, Priority.ALWAYS);
                GridPane.setVgrow(cell, Priority.ALWAYS);
                if (rowIndex == 0) {
                    cell.getStyleClass().add(AppStyles.UI_SOURCE_TABLE_HEADER_CELL);
                }
                grid.add(cell, colIndex, rowIndex);
            }
        }
        getChildren().add(grid);
    }
}
