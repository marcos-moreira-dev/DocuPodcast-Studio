package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/** Shared styled controls for MP4 resolution, frame rate and encoder. */
public final class VideoEncodingOptionsPane extends VBox {
    private final ComboBox<SimpleVideoResolutionPreset> resolution = StudioFormControls.comboBox();
    private final ComboBox<Integer> framesPerSecond = StudioFormControls.comboBox();
    private final ComboBox<VideoEncoderPolicy> encoder = StudioFormControls.comboBox();
    private final CheckBox includeInferredFrames = StudioFormControls.checkBox("Incluir frames inferidos");

    public VideoEncodingOptionsPane(List<VideoEncoderPolicy> availableEncoders,
                                    VideoEncoderPolicy defaultEncoder) {
        this(availableEncoders, defaultEncoder, ProjectMode.defaultMode());
    }

    public VideoEncodingOptionsPane(List<VideoEncoderPolicy> availableEncoders,
                                    VideoEncoderPolicy defaultEncoder,
                                    ProjectMode projectMode) {
        setSpacing(10);
        getStyleClass().addAll("export-options-section", "video-encoding-options");

        boolean narrativeVideo = projectMode == ProjectMode.NARRATIVE_VIDEO;
        if (narrativeVideo) {
            resolution.getItems().setAll(
                    SimpleVideoResolutionPreset.LOW_VERTICAL_540X960,
                    SimpleVideoResolutionPreset.HD_VERTICAL_720X1280,
                    SimpleVideoResolutionPreset.FULL_HD_VERTICAL_1080X1920,
                    SimpleVideoResolutionPreset.LOW_540,
                    SimpleVideoResolutionPreset.HD_720,
                    SimpleVideoResolutionPreset.FULL_HD_1080,
                    SimpleVideoResolutionPreset.QHD_2K,
                    SimpleVideoResolutionPreset.UHD_4K);
            resolution.setValue(SimpleVideoResolutionPreset.HD_VERTICAL_720X1280);
        } else {
            resolution.getItems().setAll(
                    SimpleVideoResolutionPreset.UHD_4K,
                    SimpleVideoResolutionPreset.QHD_2K,
                    SimpleVideoResolutionPreset.FULL_HD_1080,
                    SimpleVideoResolutionPreset.HD_720,
                    SimpleVideoResolutionPreset.LOW_540);
            resolution.setValue(SimpleVideoResolutionPreset.defaultPreset());
        }
        resolution.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(resolution, "Resolucion del archivo MP4 final.");
        resolution.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleVideoResolutionPreset preset) {
                return preset == null ? "" : preset.label() + " - " + preset.width() + "x" + preset.height();
            }

            @Override
            public SimpleVideoResolutionPreset fromString(String value) {
                return resolution.getValue();
            }
        });

        framesPerSecond.getItems().setAll(24, 30, 48, 60);
        framesPerSecond.setValue(narrativeVideo ? 24 : 30);
        framesPerSecond.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(framesPerSecond, "Fotogramas por segundo del video final.");

        List<VideoEncoderPolicy> safeEncoders = availableEncoders == null || availableEncoders.isEmpty()
                ? List.of(VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.AUTO)
                : List.copyOf(availableEncoders);
        encoder.setItems(FXCollections.observableArrayList(safeEncoders));
        encoder.setValue(safeEncoders.contains(defaultEncoder) ? defaultEncoder : safeEncoders.getFirst());
        encoder.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(encoder, "Preferencia de codificación usada por el renderizador seleccionado.");
        encoder.setConverter(new StringConverter<>() {
            @Override
            public String toString(VideoEncoderPolicy policy) {
                return policy == null ? "" : policy.label();
            }

            @Override
            public VideoEncoderPolicy fromString(String value) {
                return encoder.getValue();
            }
        });

        Label title = new Label("Video y codificacion");
        title.getStyleClass().add("export-center-section-title");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(8);
        addRow(form, 0, "Resolucion", resolution);
        addRow(form, 1, "FPS", framesPerSecond);
        addRow(form, 2, "Codificador", encoder);
        StudioFormControls.installTooltip(includeInferredFrames,
                "Usa frames intermedios ya guardados entre intervenciones adyacentes; no lanza inferencia durante la exportacion.");
        includeInferredFrames.setVisible(!narrativeVideo);
        includeInferredFrames.setManaged(!narrativeVideo);
        getChildren().addAll(title, form, includeInferredFrames);
    }

    public VideoExportOptions options() {
        return new VideoExportOptions(resolution.getValue(), framesPerSecond.getValue(), encoder.getValue(),
                false, includeInferredFrames.isSelected());
    }

    private static void addRow(GridPane form, int row, String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("dialog-field-label");
        form.add(label, 0, row);
        form.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }
}
