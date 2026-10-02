package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.collections.ObservableList;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;

/** Shared styling and tooltip helpers for form controls used inside dialogs. */
public final class StudioFormControls {
    public static final String FORM_CONTROL = "ui-form-control";
    public static final String FORM_COMBO = "ui-form-combo";
    public static final String FORM_COLOR_PICKER = "ui-form-color-picker";
    public static final String FORM_SPINNER = "ui-form-spinner";
    public static final String FORM_SLIDER = "ui-form-slider";
    public static final String CHECK_BOX = "ui-check-box";
    public static final String RADIO_BUTTON = "ui-radio-button";
    public static final String TOGGLE_BUTTON = "ui-toggle-button";
    /** @deprecated use the control-specific class; kept while product code is migrated. */
    @Deprecated public static final String FORM_TOGGLE = TOGGLE_BUTTON;

    private StudioFormControls() {
    }

    public static TextField textField() { return prepareText(new TextField()); }
    public static TextField textField(String text) { return prepareText(new TextField(text)); }
    public static TextArea textArea() { return prepareText(new TextArea()); }
    public static TextArea textArea(String text) { return prepareText(new TextArea(text)); }

    private static <T extends TextInputControl> T prepareText(T input) {
        input.getStyleClass().add(FORM_CONTROL);
        return StudioControlContract.mark(input, StudioControlFamily.FORM,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static CheckBox checkBox() { return checkBox(""); }
    public static CheckBox checkBox(String text) {
        CheckBox box = new CheckBox(text);
        box.getStyleClass().add(CHECK_BOX);
        return StudioControlContract.mark(box, StudioControlFamily.SELECTION,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static RadioButton radioButton(String text) {
        RadioButton button = new RadioButton(text);
        button.getStyleClass().add(RADIO_BUTTON);
        return StudioControlContract.mark(button, StudioControlFamily.SELECTION,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static <T> ComboBox<T> comboBox() { return prepareCombo(new ComboBox<>()); }
    public static <T> ComboBox<T> comboBox(ObservableList<T> items) { return prepareCombo(new ComboBox<>(items)); }
    private static <T> ComboBox<T> prepareCombo(ComboBox<T> control) {
        control.getStyleClass().addAll(FORM_CONTROL, FORM_COMBO);
        return StudioControlContract.mark(control, StudioControlFamily.SELECTION,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static <T> Spinner<T> spinner() { return prepareSpinner(new Spinner<>()); }
    public static <T> Spinner<T> spinner(SpinnerValueFactory<T> valueFactory) {
        return prepareSpinner(new Spinner<>(valueFactory));
    }
    public static Spinner<Integer> spinner(int min, int max, int initial) {
        return prepareSpinner(new Spinner<>(min, max, initial));
    }
    public static Spinner<Integer> spinner(int min, int max, int initial, int step) {
        return prepareSpinner(new Spinner<>(min, max, initial, step));
    }
    public static Spinner<Double> spinner(double min, double max, double initial) {
        return prepareSpinner(new Spinner<>(min, max, initial));
    }
    public static Spinner<Double> spinner(double min, double max, double initial, double step) {
        return prepareSpinner(new Spinner<>(min, max, initial, step));
    }
    private static <T> Spinner<T> prepareSpinner(Spinner<T> control) {
        control.getStyleClass().addAll(FORM_CONTROL, FORM_SPINNER);
        return StudioControlContract.mark(control, StudioControlFamily.FORM,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static Slider slider() { return prepareSlider(new Slider(), StudioControlVariant.REGULAR, StudioControlDensity.REGULAR); }
    public static Slider slider(double min, double max, double value) {
        return slider(min, max, value, StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }
    public static Slider slider(double min, double max, double value,
                                StudioControlVariant variant, StudioControlDensity density) {
        return prepareSlider(new Slider(min, max, value), variant, density);
    }
    private static Slider prepareSlider(Slider control, StudioControlVariant variant, StudioControlDensity density) {
        // SliderSkin is geometry-sensitive: it has its own official class and must not inherit
        // the generic bordered form-box skin used by text/selection controls.
        control.getStyleClass().remove(FORM_CONTROL);
        addOnce(control, FORM_SLIDER);
        control.getStyleClass().removeAll("ui-slider-compact", "ui-slider-regular", "ui-slider-editor");
        addOnce(control, switch (variant) {
            case COMPACT -> "ui-slider-compact";
            case EDITOR -> "ui-slider-editor";
            default -> "ui-slider-regular";
        });
        if (!(control.getSkin() instanceof StudioSliderSkin)) control.setSkin(new StudioSliderSkin(control));
        return StudioControlContract.mark(control, StudioControlFamily.FORM,
                variant, density);
    }

    public static ColorPicker colorPicker() { return prepareColorPicker(new ColorPicker()); }
    public static ColorPicker colorPicker(Color color) { return prepareColorPicker(new ColorPicker(color)); }
    private static ColorPicker prepareColorPicker(ColorPicker control) {
        control.getStyleClass().addAll(FORM_CONTROL, FORM_COLOR_PICKER);
        return StudioControlContract.mark(control, StudioControlFamily.SELECTION,
                StudioControlVariant.EDITOR, StudioControlDensity.REGULAR);
    }

    public static ToggleButton toggleButton() { return toggleButton(""); }
    public static ToggleButton toggleButton(String text) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().addAll(FORM_CONTROL, TOGGLE_BUTTON);
        return StudioControlContract.mark(button, StudioControlFamily.SELECTION,
                StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
    }

    public static ToggleButton toggle(String text, String tooltip) {
        ToggleButton button = toggleButton(text);
        installTooltip(button, tooltip);
        return button;
    }

    public static CheckBox checkBox(String text, String tooltip) {
        CheckBox box = checkBox(text);
        installTooltip(box, tooltip);
        return box;
    }

    public static <T> ComboBox<T> combo(ComboBox<T> combo, String tooltip) {
        if (combo != null) {
            combo.getStyleClass().addAll(FORM_CONTROL, FORM_COMBO);
            StudioControlContract.mark(combo, StudioControlFamily.SELECTION,
                    StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
            installTooltip(combo, tooltip);
        }
        return combo;
    }

    public static ColorPicker colorPicker(ColorPicker picker, String tooltip) {
        if (picker != null) {
            picker.getStyleClass().addAll(FORM_CONTROL, FORM_COLOR_PICKER);
            StudioControlContract.mark(picker, StudioControlFamily.SELECTION,
                    StudioControlVariant.EDITOR, StudioControlDensity.REGULAR);
            installTooltip(picker, tooltip);
        }
        return picker;
    }

    public static <T> Spinner<T> spinner(Spinner<T> spinner, String tooltip) {
        if (spinner != null) {
            spinner.getStyleClass().addAll(FORM_CONTROL, FORM_SPINNER);
            StudioControlContract.mark(spinner, StudioControlFamily.FORM,
                    StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
            installTooltip(spinner, tooltip);
        }
        return spinner;
    }

    public static Slider slider(Slider slider, String tooltip) {
        if (slider != null) {
            prepareSlider(slider, StudioControlVariant.EDITOR, StudioControlDensity.REGULAR);
            installTooltip(slider, tooltip);
        }
        return slider;
    }

    public static <T extends TextInputControl> T textInput(T input, String tooltip) {
        if (input != null) {
            input.getStyleClass().add(FORM_CONTROL);
            StudioControlContract.mark(input, StudioControlFamily.FORM,
                    StudioControlVariant.REGULAR, StudioControlDensity.REGULAR);
            installTooltip(input, tooltip);
        }
        return input;
    }

    public static void installTooltip(Node node, String text) {
        if (node == null || text == null || text.isBlank()) {
            return;
        }
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(360);
        Tooltip.install(node, tooltip);
        if (node instanceof Control control) {
            control.setTooltip(tooltip);
            StudioControlContract.enrich(control, text, text);
        }
        node.setAccessibleText(text);
    }

    private static void addOnce(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) node.getStyleClass().add(styleClass);
    }
}
