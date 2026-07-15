package com.marcosmoreiradev.docupodcaststudio.presentation.status;

import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.IntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.function.IntConsumer;

/** Compact control for changing reading text size without scaling the whole canvas. */
public final class ReadingZoomControl extends HBox {
    private boolean updatingFromModel;

    public ReadingZoomControl(
            IntegerProperty readingFontSize,
            Runnable decreaseAction,
            Runnable resetAction,
            Runnable increaseAction,
            IntConsumer setReadingFontSize
    ) {
        getStyleClass().add("reading-zoom-control");
        setAlignment(Pos.CENTER_RIGHT);
        setSpacing(6);
        setMinWidth(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);

        Label caption = new Label("Lectura");
        caption.getStyleClass().add("reading-zoom-caption");
        keepReadable(caption);

        Button decrease = zoomButton("−", "Reducir tamaño de lectura", decreaseAction);
        Button reset = zoomButton("100%", "Restaurar tamaño de lectura", resetAction);
        Button increase = zoomButton("+", "Aumentar tamaño de lectura", increaseAction);

        Slider slider = new Slider(
                DocuPodcastShellViewModel.MIN_READING_FONT_SIZE,
                DocuPodcastShellViewModel.MAX_READING_FONT_SIZE,
                readingFontSize.get());
        slider.getStyleClass().add("reading-zoom-slider");
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(2);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        Tooltip.install(slider, new Tooltip("Cambia el tamaño del texto de lectura. La hoja refluye como documento; no es zoom de lienzo."));

        Label percent = new Label(percentLabel(readingFontSize.get()));
        percent.getStyleClass().add("reading-zoom-percent");
        keepReadable(percent);

        readingFontSize.addListener((obs, oldValue, newValue) -> {
            updatingFromModel = true;
            slider.setValue(newValue.intValue());
            percent.setText(percentLabel(newValue.intValue()));
            updatingFromModel = false;
        });
        slider.valueChangingProperty().addListener((obs, wasChanging, changing) -> {
            if (!changing) {
                commitSlider(slider, setReadingFontSize);
            }
        });
        slider.setOnMouseReleased(event -> commitSlider(slider, setReadingFontSize));
        slider.setOnKeyReleased(event -> commitSlider(slider, setReadingFontSize));

        getChildren().addAll(caption, decrease, slider, percent, reset, increase);
    }

    private Button zoomButton(String label, String tooltip, Runnable action) {
        Button button = new Button(label);
        button.getStyleClass().add("reading-zoom-button");
        button.setFocusTraversable(false);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setOnAction(event -> action.run());
        Tooltip.install(button, new Tooltip(tooltip));
        return button;
    }

    private static void keepReadable(Label label) {
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setMaxWidth(Region.USE_PREF_SIZE);
    }

    private void commitSlider(Slider slider, IntConsumer setReadingFontSize) {
        if (updatingFromModel) {
            return;
        }
        setReadingFontSize.accept((int) Math.round(slider.getValue()));
    }

    private static String percentLabel(int fontSize) {
        int percent = Math.round((fontSize * 100.0f) / DocuPodcastShellViewModel.DEFAULT_READING_FONT_SIZE);
        return percent + "%";
    }
}
