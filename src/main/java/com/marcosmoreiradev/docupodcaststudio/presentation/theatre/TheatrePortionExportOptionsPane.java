package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreMapCompanionMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoEncodingOptionsPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/** Styled act/scene scope controls embedded in Export Center. */
public final class TheatrePortionExportOptionsPane extends VBox {
    public void setPresentationMode(String mode) {
        companion.setValue(java.util.Arrays.stream(TheatreMapCompanionMode.values())
                .filter(value -> value.frameMode().equals(mode)).findFirst().orElse(TheatreMapCompanionMode.FRAGMENT_VISUALS));
    }
    private static final ScopeItem ALL_ACT = new ScopeItem("", "Todo el acto", true);

    private final List<TheatreProjectLayer.Scene> scenes;
    private final ComboBox<TheatreProjectLayer.TheatreAct> act = StudioFormControls.comboBox();
    private final ComboBox<ScopeItem> scope = StudioFormControls.comboBox();
    private final ComboBox<TheatrePortionExportOptions.Output> output = StudioFormControls.comboBox();
    private final ComboBox<TheatreMapCompanionMode> companion = StudioFormControls.comboBox();
    private final VBox companionField;
    private final VBox unassignedField;
    private final CheckBox renderUnassigned = StudioFormControls.checkBox("Renderizar fragmentos sin imagen ni lienzo");
    private final VideoEncodingOptionsPane video;

    public TheatrePortionExportOptionsPane(List<TheatreProjectLayer.TheatreAct> acts,
                                           List<TheatreProjectLayer.Scene> scenes,
                                           List<VideoEncoderPolicy> encoders,
                                           VideoEncoderPolicy defaultEncoder) {
        setSpacing(12);
        getStyleClass().add("theatre-portion-export-options");
        this.scenes = scenes == null ? List.of() : List.copyOf(scenes);
        video = new VideoEncodingOptionsPane(encoders, defaultEncoder);

        act.getItems().setAll(acts == null ? List.of() : acts);
        act.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(act, "Acto teatral que se desea exportar.");
        act.setConverter(new StringConverter<>() {
            @Override
            public String toString(TheatreProjectLayer.TheatreAct value) {
                return value == null ? "" : value.displayName();
            }

            @Override
            public TheatreProjectLayer.TheatreAct fromString(String value) {
                return act.getValue();
            }
        });

        scope.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(scope, "Exportar todo el acto o una escena concreta.");
        scope.setConverter(new StringConverter<>() {
            @Override
            public String toString(ScopeItem value) {
                return value == null ? "" : value.label();
            }

            @Override
            public ScopeItem fromString(String value) {
                return scope.getValue();
            }
        });
        act.valueProperty().addListener((obs, oldValue, newValue) -> refreshScopes());

        output.getItems().setAll(TheatrePortionExportOptions.Output.THEATRE_MAP,
                TheatrePortionExportOptions.Output.CLEAN_VIDEO);
        output.setValue(TheatrePortionExportOptions.Output.THEATRE_MAP);
        output.setMaxWidth(Double.MAX_VALUE);
        StudioFormControls.combo(output, "Renderer usado para la porcion teatral.");
        output.setConverter(new StringConverter<>() {
            @Override
            public String toString(TheatrePortionExportOptions.Output value) {
                return value == TheatrePortionExportOptions.Output.CLEAN_VIDEO
                        ? "Video limpio" : "Mapa teatral";
            }

            @Override
            public TheatrePortionExportOptions.Output fromString(String value) {
                return output.getValue();
            }
        });
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
        companionField = labelled("Acompanamiento", companion);
        StudioFormControls.installTooltip(renderUnassigned,
                "Usa fondo negro con texto blanco en los fragmentos sin visual asignado.");
        unassignedField = new VBox(4, renderUnassigned,
                help("El video limpio mostrara 'Sin fragmento visual asignado'."));
        output.valueProperty().addListener((obs, oldValue, newValue) -> updateOutputFields(newValue));

        Label title = new Label("Alcance teatral");
        title.getStyleClass().add("export-center-section-title");
        VBox scopeControls = new VBox(8, title,
                labelled("Acto", act), labelled("Escena", scope), labelled("Resultado", output),
                companionField, unassignedField);
        scopeControls.getStyleClass().add("export-options-section");
        getChildren().addAll(scopeControls, video);

        if (!act.getItems().isEmpty()) {
            act.setValue(act.getItems().getFirst());
        }
        refreshScopes();
        updateOutputFields(output.getValue());
    }

    public TheatrePortionExportOptions options() {
        TheatreProjectLayer.TheatreAct selectedAct = act.getValue();
        ScopeItem selectedScope = scope.getValue();
        TheatreExportScope exportScope;
        if (selectedAct == null) {
            exportScope = TheatreExportScope.all();
        } else if (selectedScope == null || selectedScope.allAct()) {
            exportScope = TheatreExportScope.act(selectedAct.id());
        } else {
            exportScope = TheatreExportScope.scene(selectedScope.id());
        }
        VideoExportOptions base = video.options();
        VideoExportOptions selectedVideo = new VideoExportOptions(base.resolution(), base.framesPerSecond(),
                base.encoderPolicy(), renderUnassigned.isSelected(), base.includeInferredFrames());
        return new TheatrePortionExportOptions(exportScope, output.getValue(), companion.getValue(), selectedVideo);
    }

    private void updateOutputFields(TheatrePortionExportOptions.Output selected) {
        boolean map = selected == TheatrePortionExportOptions.Output.THEATRE_MAP;
        setManagedVisible(companionField, map);
        setManagedVisible(unassignedField, !map);
    }

    private static Label help(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("dialog-help-text");
        return label;
    }

    private void refreshScopes() {
        TheatreProjectLayer.TheatreAct selectedAct = act.getValue();
        scope.getItems().clear();
        if (selectedAct == null) {
            return;
        }
        scope.getItems().add(ALL_ACT);
        scenes.stream()
                .filter(scene -> selectedAct.id().equals(scene.actId()))
                .map(scene -> new ScopeItem(scene.id(), scene.displayName(), false))
                .forEach(scope.getItems()::add);
        scope.setValue(ALL_ACT);
    }

    private static VBox labelled(String text, javafx.scene.Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("dialog-field-label");
        return new VBox(4, label, control);
    }

    private static void setManagedVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private record ScopeItem(String id, String label, boolean allAct) {
    }
}
