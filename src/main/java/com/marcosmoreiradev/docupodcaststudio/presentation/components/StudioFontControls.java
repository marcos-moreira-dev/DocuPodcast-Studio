package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

/** Installed-font selector shared by video export and editable canvas text. */
public final class StudioFontControls {
    private StudioFontControls() {}

    public static void configure(ComboBox<String> combo) {
        String previous = combo.getValue();
        StudioFormControls.combo(combo, "Fuente instalada: nombre y muestra tipográfica.");
        combo.getItems().setAll(Font.getFamilies().stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        combo.setEditable(false);
        combo.setCellFactory(list -> previewCell());
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String family, boolean empty) {
                super.updateItem(family, empty);
                setText(empty ? null : family);
                setGraphic(null);
            }
        });
        combo.setValue(previous == null ? Font.getDefault().getFamily() : previous);
    }

    public static ListCell<String> previewCell() {
        return new ListCell<>() {
            @Override protected void updateItem(String family, boolean empty) {
                super.updateItem(family, empty);
                setText(null); setGraphic(null); setAccessibleText(null);
                if (empty || family == null || family.isBlank()) return;
                Label name = new Label(family);
                name.setMinWidth(140); name.setPrefWidth(140);
                name.setStyle("-fx-font-family: 'System'; -fx-text-fill: #111827;");
                Label sample = new Label(family);
                sample.setStyle("-fx-font-family: '" + family.replace("\\", "\\\\").replace("'", "\\'")
                        + "'; -fx-font-size: 15px; -fx-text-fill: #111827;");
                HBox row = new HBox(14, name, sample);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row); setAccessibleText("Fuente " + family);
            }
        };
    }
}
