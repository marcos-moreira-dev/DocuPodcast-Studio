package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoEncodingOptionsPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/** Options that belong only to the clean theatre renderer. */
public final class TheatreCleanVideoExportOptionsPane extends VBox {
    private final CheckBox renderUnassigned = StudioFormControls.checkBox("Renderizar fragmentos sin imagen ni lienzo");
    private final VideoEncodingOptionsPane video;

    public TheatreCleanVideoExportOptionsPane(List<VideoEncoderPolicy> encoders,
                                               VideoEncoderPolicy defaultEncoder) {
        setSpacing(12);
        getStyleClass().add("theatre-clean-video-export-options");
        video = new VideoEncodingOptionsPane(encoders, defaultEncoder);
        StudioFormControls.installTooltip(renderUnassigned,
                "Usa un fondo negro con texto blanco cuando el fragmento no tenga imagen ni frame dibujado.");
        Label explanation = new Label(
                "Los fragmentos sin visual se exportaran con fondo negro y el aviso "
                        + "'Sin fragmento visual asignado'.");
        explanation.setWrapText(true);
        explanation.getStyleClass().add("dialog-help-text");
        VBox fallback = new VBox(5, renderUnassigned, explanation);
        fallback.getStyleClass().add("export-options-section");
        getChildren().addAll(fallback, video);
    }

    public VideoExportOptions options() {
        VideoExportOptions base = video.options();
        return new VideoExportOptions(base.resolution(), base.framesPerSecond(), base.encoderPolicy(),
                renderUnassigned.isSelected(), base.includeInferredFrames());
    }
}
