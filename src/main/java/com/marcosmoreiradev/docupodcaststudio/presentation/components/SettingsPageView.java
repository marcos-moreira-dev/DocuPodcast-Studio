package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Reusable settings page component for the dedicated configuration surface.
 *
 * <p>The settings dialog can grow by adding rows to this component instead of building bespoke
 * JavaFX layouts in every section.</p>
 */
public final class SettingsPageView extends VBox {
    private final GridPane grid = new GridPane();
    private int rowIndex;

    public SettingsPageView(String title, String summary) {
        super(12);
        getStyleClass().add(AppStyles.UI_SETTINGS_PAGE);
        SectionHeader header = new SectionHeader(title, summary);
        grid.getStyleClass().add(AppStyles.UI_SETTINGS_GRID);
        grid.setVgap(8);
        grid.setHgap(12);
        getChildren().addAll(header, new Separator(), grid);
    }

    public SettingsPageView addRow(String label, String value) {
        Label key = new Label(label);
        key.getStyleClass().add(AppStyles.UI_SETTINGS_KEY);
        Label rowValue = new Label(value);
        rowValue.setWrapText(true);
        rowValue.getStyleClass().add(AppStyles.UI_SETTINGS_VALUE);
        grid.add(key, 0, rowIndex);
        grid.add(rowValue, 1, rowIndex);
        rowIndex++;
        return this;
    }

    public SettingsPageView addNote(String text) {
        Label note = new Label(text);
        note.setWrapText(true);
        note.setAlignment(Pos.CENTER_LEFT);
        note.getStyleClass().addAll(AppStyles.UI_NOTE, "settings-note");
        note.setPadding(new Insets(8));
        getChildren().add(note);
        return this;
    }

    public SettingsPageView addNode(Node node) {
        getChildren().add(node);
        return this;
    }
}
