package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

/** Dialog for final-video export properties: resolution, FPS and encoder. */
public final class VideoExportOptionsDialog {
    public Optional<VideoExportOptions> show(Window owner,
                                             List<VideoEncoderPolicy> availableEncoderPolicies,
                                             VideoEncoderPolicy defaultEncoderPolicy) {
        Dialog<VideoExportOptions> dialog = new Dialog<>();
        dialog.setTitle("Exportar video");
        dialog.setHeaderText("Elige las propiedades del video final");
        DialogStyler.apply(dialog, owner);

        ComboBox<SimpleVideoResolutionPreset> resolution = new ComboBox<>();
        resolution.getItems().setAll(
                SimpleVideoResolutionPreset.UHD_4K,
                SimpleVideoResolutionPreset.QHD_2K,
                SimpleVideoResolutionPreset.FULL_HD_1080,
                SimpleVideoResolutionPreset.HD_720);
        resolution.setValue(SimpleVideoResolutionPreset.defaultPreset());
        resolution.setMaxWidth(Double.MAX_VALUE);
        styleCombo(resolution);
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

        ComboBox<Integer> framesPerSecond = new ComboBox<>();
        framesPerSecond.getItems().setAll(24, 30, 48, 60);
        framesPerSecond.setValue(30);
        framesPerSecond.setMaxWidth(Double.MAX_VALUE);
        styleCombo(framesPerSecond);

        List<VideoEncoderPolicy> encoders = availableEncoderPolicies == null || availableEncoderPolicies.isEmpty()
                ? List.of(VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.AUTO)
                : availableEncoderPolicies;
        ComboBox<VideoEncoderPolicy> encoder = new ComboBox<>();
        encoder.getItems().setAll(encoders);
        encoder.setValue(encoders.contains(defaultEncoderPolicy) ? defaultEncoderPolicy : encoders.get(0));
        encoder.setMaxWidth(Double.MAX_VALUE);
        styleCombo(encoder);
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

        VBox content = new VBox(10,
                new Label("La app generara un archivo MP4 final. Los paquetes tecnicos quedan reservados para soporte avanzado."),
                new Label("Resolucion"),
                resolution,
                new Label("Fotogramas por segundo"),
                framesPerSecond,
                new Label("Codificador de video"),
                encoder);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new VideoExportOptions(resolution.getValue(), framesPerSecond.getValue(), encoder.getValue())
                : null);
        return dialog.showAndWait();
    }

    private static void styleCombo(ComboBox<?> combo) {
        combo.getStyleClass().add("voice-library-combo");
    }
}
