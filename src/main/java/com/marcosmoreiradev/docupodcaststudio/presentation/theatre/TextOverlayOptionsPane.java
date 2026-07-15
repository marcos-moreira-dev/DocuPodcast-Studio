package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.util.List;

/** Shared theatrical text-overlay controls backed by options used by the frame renderer. */
public final class TextOverlayOptionsPane extends VBox {
    private final CheckBox showText = new CheckBox("Mostrar texto en pantalla");
    private final ComboBox<String> fontFamily = new ComboBox<>();
    private final Spinner<Integer> fontSize = new Spinner<>(12, 96, 42);
    private final ColorPicker textColor = new ColorPicker(Color.web("#111827"));
    private final ComboBox<TheatreTextPosition> textPosition = enumCombo(
            TheatreTextPosition.values(), TheatreTextPosition.CENTER);
    private final ComboBox<TheatreTextEffect> textEffect = enumCombo(
            TheatreTextEffect.values(), TheatreTextEffect.SHADOW);
    private final GridPane form = new GridPane();

    public TextOverlayOptionsPane() {
        setSpacing(9);
        getStyleClass().addAll("export-options-section", "text-overlay-options");
        showText.setSelected(true);
        showText.getStyleClass().add("ui-form-toggle");
        StudioFormControls.installTooltip(showText, "Incluir el texto de la intervencion en cada frame teatral.");

        fontFamily.getItems().setAll(preferredFonts());
        fontFamily.setValue(fontFamily.getItems().contains("Inter") ? "Inter" : fontFamily.getItems().getFirst());
        fontFamily.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(fontFamily, "Tipografia aplicada al texto teatral.");
        fontSize.setEditable(true);
        StudioFormControls.spinner(fontSize, "Tamano del texto teatral.");
        StudioFormControls.colorPicker(textColor, "Color del texto teatral.");

        form.setHgap(12);
        form.setVgap(8);
        addRow(form, 0, "Fuente", fontFamily);
        addRow(form, 1, "Tamano", fontSize);
        addRow(form, 2, "Color", textColor);
        addRow(form, 3, "Region", textPosition);
        addRow(form, 4, "Efecto", textEffect);
        form.disableProperty().bind(showText.selectedProperty().not());

        Label title = new Label("Texto en pantalla");
        title.getStyleClass().add("export-center-section-title");
        getChildren().addAll(title, showText, form);
    }

    public boolean showText() {
        return showText.isSelected();
    }

    public String fontFamily() {
        return fontFamily.getValue();
    }

    public int fontSize() {
        return fontSize.getValue();
    }

    public String textColor() {
        return hex(textColor.getValue());
    }

    public TheatreTextPosition textPosition() {
        return textPosition.getValue();
    }

    public TheatreTextEffect textEffect() {
        return textEffect.getValue();
    }

    public void bindLayout(ComboBox<TheatreFrameLayout> layout) {
        if (layout == null) {
            return;
        }
        Runnable refresh = () -> {
            TheatreFrameLayout value = layout.getValue();
            boolean supportsText = value != TheatreFrameLayout.IMAGE_WITH_SPATIAL_MAP;
            showText.setDisable(!supportsText);
            if (!supportsText) {
                showText.setSelected(false);
            }
        };
        layout.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();
    }

    private static <T extends Enum<T>> ComboBox<T> enumCombo(T[] values, T selected) {
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().setAll(values);
        combo.setValue(selected);
        combo.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(combo, "Seleccion teatral aplicada al texto del frame.");
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                if (value instanceof TheatreTextEffect effect) return effect.label();
                if (value instanceof TheatreTextPosition position) return position.label();
                return value == null ? "" : value.name();
            }

            @Override
            public T fromString(String value) {
                return combo.getValue();
            }
        });
        return combo;
    }

    private static List<String> preferredFonts() {
        List<String> installed = Font.getFamilies();
        List<String> preferred = List.of("Inter", "Arial", "Calibri", "Verdana", "Georgia", "Times New Roman");
        List<String> available = preferred.stream().filter(installed::contains).toList();
        return available.isEmpty() ? List.of("SansSerif") : available;
    }

    private static void addRow(GridPane form, int row, String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("dialog-field-label");
        form.add(label, 0, row);
        form.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private static String hex(Color color) {
        Color safe = color == null ? Color.web("#111827") : color;
        return "#%02X%02X%02X".formatted(
                Math.round((float) safe.getRed() * 255),
                Math.round((float) safe.getGreen() * 255),
                Math.round((float) safe.getBlue() * 255));
    }
}
