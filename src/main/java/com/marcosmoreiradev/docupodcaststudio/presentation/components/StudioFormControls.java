package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Spinner;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

/** Shared styling and tooltip helpers for form controls used inside dialogs. */
public final class StudioFormControls {
    public static final String FORM_CONTROL = "ui-form-control";
    public static final String FORM_COMBO = "ui-form-combo";
    public static final String FORM_COLOR_PICKER = "ui-form-color-picker";
    public static final String FORM_SPINNER = "ui-form-spinner";
    public static final String FORM_SLIDER = "ui-form-slider";
    public static final String FORM_TOGGLE = "ui-form-toggle";

    private StudioFormControls() {
    }

    public static ToggleButton toggle(String text, String tooltip) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().addAll(FORM_CONTROL, FORM_TOGGLE);
        installTooltip(button, tooltip);
        return button;
    }

    public static <T> ComboBox<T> combo(ComboBox<T> combo, String tooltip) {
        if (combo != null) {
            combo.getStyleClass().addAll(FORM_CONTROL, FORM_COMBO);
            installTooltip(combo, tooltip);
        }
        return combo;
    }

    public static ColorPicker colorPicker(ColorPicker picker, String tooltip) {
        if (picker != null) {
            picker.getStyleClass().addAll(FORM_CONTROL, FORM_COLOR_PICKER);
            installTooltip(picker, tooltip);
        }
        return picker;
    }

    public static <T> Spinner<T> spinner(Spinner<T> spinner, String tooltip) {
        if (spinner != null) {
            spinner.getStyleClass().addAll(FORM_CONTROL, FORM_SPINNER);
            installTooltip(spinner, tooltip);
        }
        return spinner;
    }

    public static Slider slider(Slider slider, String tooltip) {
        if (slider != null) {
            slider.getStyleClass().addAll(FORM_CONTROL, FORM_SLIDER);
            installTooltip(slider, tooltip);
        }
        return slider;
    }

    public static <T extends TextInputControl> T textInput(T input, String tooltip) {
        if (input != null) {
            input.getStyleClass().add(FORM_CONTROL);
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
        }
        node.setAccessibleText(text);
    }
}
