package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoEncodingOptionsPane;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/** Minimal map composition controls embedded in Export Center. */
public final class TheatreMapExportOptionsPane extends VBox {
    private final ComboBox<TheatreMapCompanionMode> companion = new ComboBox<>();
    private final VideoEncodingOptionsPane video;

    public TheatreMapExportOptionsPane(List<VideoEncoderPolicy> encoders, VideoEncoderPolicy defaultEncoder) {
        setSpacing(12);
        getStyleClass().add("theatre-map-export-options");
        video = new VideoEncodingOptionsPane(encoders, defaultEncoder);
        companion.getItems().setAll(TheatreMapCompanionMode.values());
        companion.setValue(TheatreMapCompanionMode.FRAGMENT_VISUALS);
        companion.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(companion, "Contenido que acompana al mapa espacial.");
        companion.setConverter(new StringConverter<>() {
            @Override
            public String toString(TheatreMapCompanionMode value) {
                return value == null ? "" : value.displayName();
            }

            @Override
            public TheatreMapCompanionMode fromString(String value) {
                return companion.getValue();
            }
        });
        Label title = new Label("Mapa teatral");
        title.getStyleClass().add("export-center-section-title");
        Label label = new Label("Acompanamiento");
        label.getStyleClass().add("dialog-field-label");
        getChildren().addAll(title, new VBox(4, label, companion), video);
    }

    public TheatreMapExportOptions options() {
        return new TheatreMapExportOptions(video.options(), companion.getValue());
    }
}
