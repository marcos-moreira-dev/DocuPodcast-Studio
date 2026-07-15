package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoEncodingOptionsPane;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.util.List;

/** Styled options embedded in Export Center for a complete theatre work. */
public final class TheatreWorkExportOptionsPane extends VBox {
    private final VideoEncodingOptionsPane video;
    private final CheckBox useFragmentImages = check("Usar imagenes de fragmentos", true);
    private final ComboBox<TheatreFrameLayout> frameLayout = new ComboBox<>();
    private final ColorPicker backgroundColor = new ColorPicker(Color.WHITE);
    private final TextOverlayOptionsPane text = new TextOverlayOptionsPane();
    private final CheckBox showSpatialMap = check("Mapa espacial lateral", false);
    private final CheckBox showCharacters = check("Mostrar personajes", true);
    private final CheckBox showDisplacements = check("Mostrar desplazamientos", true);

    public TheatreWorkExportOptionsPane(List<VideoEncoderPolicy> encoders, VideoEncoderPolicy defaultEncoder) {
        setSpacing(12);
        getStyleClass().add("theatre-work-export-options");
        video = new VideoEncodingOptionsPane(encoders, defaultEncoder);

        frameLayout.getItems().setAll(TheatreFrameLayout.values());
        frameLayout.setValue(TheatreFrameLayout.IMAGE_WITH_TEXT);
        frameLayout.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(frameLayout, "Distribucion visual usada al componer cada frame teatral.");
        frameLayout.setConverter(new StringConverter<>() {
            @Override
            public String toString(TheatreFrameLayout layout) {
                return layout == null ? "" : layout.label();
            }

            @Override
            public TheatreFrameLayout fromString(String value) {
                return frameLayout.getValue();
            }
        });
        StudioFormControls.colorPicker(backgroundColor, "Fondo usado cuando una intervencion no tiene imagen.");
        text.bindLayout(frameLayout);

        Label visualTitle = new Label("Composicion teatral");
        visualTitle.getStyleClass().add("export-center-section-title");
        VBox visual = new VBox(8, visualTitle, useFragmentImages,
                labelled("Diseno del frame", frameLayout), labelled("Fondo sin imagen", backgroundColor),
                showSpatialMap, showCharacters, showDisplacements);
        visual.getStyleClass().add("export-options-section");
        getChildren().addAll(video, visual, text);
    }

    public TheatreWorkExportOptions options() {
        return new TheatreWorkExportOptions(
                video.options(),
                useFragmentImages.isSelected(),
                text.showText(),
                text.fontFamily(),
                text.fontSize(),
                text.textColor(),
                text.textPosition(),
                text.textEffect(),
                frameLayout.getValue(),
                showSpatialMap.isSelected(),
                showCharacters.isSelected(),
                showDisplacements.isSelected(),
                colorHex(backgroundColor.getValue()));
    }

    private static CheckBox check(String text, boolean selected) {
        CheckBox box = new CheckBox(text);
        box.setSelected(selected);
        box.getStyleClass().add("ui-form-toggle");
        return box;
    }

    private static VBox labelled(String text, javafx.scene.Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("dialog-field-label");
        return new VBox(4, label, control);
    }

    private static String colorHex(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return "#%02X%02X%02X".formatted(
                Math.round((float) safe.getRed() * 255),
                Math.round((float) safe.getGreen() * 255),
                Math.round((float) safe.getBlue() * 255));
    }
}
