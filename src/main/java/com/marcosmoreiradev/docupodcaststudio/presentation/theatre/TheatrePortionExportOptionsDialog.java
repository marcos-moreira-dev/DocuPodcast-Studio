package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;
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

/** Dialog for exporting one act or scene of the theatre work. */
public final class TheatrePortionExportOptionsDialog {
    public Optional<TheatrePortionExportOptions> show(Window owner,
                                                      List<TheatreProjectLayer.TheatreAct> acts,
                                                      List<TheatreProjectLayer.Scene> scenes,
                                                      List<VideoEncoderPolicy> availableEncoderPolicies,
                                                      VideoEncoderPolicy defaultEncoderPolicy) {
        Dialog<TheatrePortionExportOptions> dialog = StudioDialogShell.dialog();
        dialog.setTitle("Exportar porcion de obra");
        dialog.setHeaderText("Elige que parte de la obra exportar");
        DialogStyler.apply(dialog, owner);

        ComboBox<ScopeType> scopeType = StudioFormControls.comboBox();
        scopeType.getItems().setAll(ScopeType.SCENE, ScopeType.ACT);
        scopeType.setValue((scenes == null || scenes.isEmpty()) && acts != null && !acts.isEmpty()
                ? ScopeType.ACT : ScopeType.SCENE);
        scopeType.setMaxWidth(Double.MAX_VALUE);
        styleCombo(scopeType);
        scopeType.setConverter(new StringConverter<>() {
            @Override
            public String toString(ScopeType value) {
                return value == ScopeType.ACT ? "Acto" : "Escena";
            }

            @Override
            public ScopeType fromString(String value) {
                return scopeType.getValue();
            }
        });

        ComboBox<ScopeItem> scopeItem = StudioFormControls.comboBox();
        scopeItem.setMaxWidth(Double.MAX_VALUE);
        styleCombo(scopeItem);
        scopeItem.setConverter(new StringConverter<>() {
            @Override
            public String toString(ScopeItem value) {
                return value == null ? "" : value.label();
            }

            @Override
            public ScopeItem fromString(String value) {
                return scopeItem.getValue();
            }
        });
        Runnable refreshScopeItems = () -> {
            scopeItem.getItems().clear();
            if (scopeType.getValue() == ScopeType.ACT) {
                for (TheatreProjectLayer.TheatreAct act : acts == null ? List.<TheatreProjectLayer.TheatreAct>of() : acts) {
                    scopeItem.getItems().add(new ScopeItem(act.id(), act.displayName()));
                }
            } else {
                for (TheatreProjectLayer.Scene scene : scenes == null ? List.<TheatreProjectLayer.Scene>of() : scenes) {
                    scopeItem.getItems().add(new ScopeItem(scene.id(), scene.displayName()));
                }
            }
            if (!scopeItem.getItems().isEmpty()) {
                scopeItem.setValue(scopeItem.getItems().get(0));
            }
        };
        refreshScopeItems.run();
        scopeType.valueProperty().addListener((obs, oldValue, newValue) -> refreshScopeItems.run());

        ComboBox<TheatrePortionExportOptions.Output> output = StudioFormControls.comboBox();
        output.getItems().setAll(TheatrePortionExportOptions.Output.THEATRE_MAP, TheatrePortionExportOptions.Output.CLEAN_VIDEO);
        output.setValue(TheatrePortionExportOptions.Output.THEATRE_MAP);
        output.setMaxWidth(Double.MAX_VALUE);
        styleCombo(output);
        output.setConverter(new StringConverter<>() {
            @Override
            public String toString(TheatrePortionExportOptions.Output value) {
                return value == TheatrePortionExportOptions.Output.CLEAN_VIDEO
                        ? "Video limpio"
                        : "Mapa teatral";
            }

            @Override
            public TheatrePortionExportOptions.Output fromString(String value) {
                return output.getValue();
            }
        });

        ComboBox<SimpleVideoResolutionPreset> resolution = StudioFormControls.comboBox();
        resolution.getItems().setAll(SimpleVideoResolutionPreset.UHD_4K, SimpleVideoResolutionPreset.QHD_2K,
                SimpleVideoResolutionPreset.FULL_HD_1080, SimpleVideoResolutionPreset.HD_720,
                SimpleVideoResolutionPreset.LOW_540);
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

        ComboBox<Integer> fps = StudioFormControls.comboBox();
        fps.getItems().setAll(24, 30, 48, 60);
        fps.setValue(30);
        fps.setMaxWidth(Double.MAX_VALUE);
        styleCombo(fps);

        List<VideoEncoderPolicy> encoders = availableEncoderPolicies == null || availableEncoderPolicies.isEmpty()
                ? List.of(VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.AUTO)
                : availableEncoderPolicies;
        ComboBox<VideoEncoderPolicy> encoder = StudioFormControls.comboBox();
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
                new Label("Tipo de porcion"),
                scopeType,
                new Label("Porcion"),
                scopeItem,
                new Label("Resultado"),
                output,
                new Label("Resolucion"),
                resolution,
                new Label("Fotogramas por segundo"),
                fps,
                new Label("Codificador de video"),
                encoder);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK || scopeItem.getValue() == null) {
                return null;
            }
            TheatreExportScope scope = scopeType.getValue() == ScopeType.ACT
                    ? TheatreExportScope.act(scopeItem.getValue().id())
                    : TheatreExportScope.scene(scopeItem.getValue().id());
            return new TheatrePortionExportOptions(
                    scope,
                    output.getValue(),
                    com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode.FRAGMENT_VISUALS,
                    new VideoExportOptions(resolution.getValue(), fps.getValue(), encoder.getValue()));
        });
        return dialog.showAndWait();
    }

    private enum ScopeType {
        ACT,
        SCENE
    }

    private record ScopeItem(String id, String label) {
    }

    private static void styleCombo(ComboBox<?> combo) {
        combo.getStyleClass().add("voice-library-combo");
    }
}
