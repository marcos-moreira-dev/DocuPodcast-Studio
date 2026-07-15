package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

/** Dialog for theatrical video export properties. */
public final class TheatreWorkExportOptionsDialog {
    public Optional<TheatreWorkExportOptions> show(Window owner,
                                                   List<VideoEncoderPolicy> availableEncoderPolicies,
                                                   VideoEncoderPolicy defaultEncoderPolicy) {
        Dialog<TheatreWorkExportOptions> dialog = new Dialog<>();
        dialog.setTitle("Exportar obra teatral");
        dialog.setHeaderText("Elige imagen, texto y mapa espacial para el video de la obra");
        DialogStyler.apply(dialog, owner);

        ComboBox<SimpleVideoResolutionPreset> resolution = resolutionCombo();
        ComboBox<Integer> framesPerSecond = framesPerSecondCombo();
        ComboBox<VideoEncoderPolicy> encoder = encoderCombo(availableEncoderPolicies, defaultEncoderPolicy);

        CheckBox useFragmentImages = checked("Usar imagenes de fragmentos", true);
        CheckBox showText = checked("Mostrar texto", true);
        ComboBox<String> fontFamily = stringCombo(List.of("Inter", "Arial", "Calibri", "Verdana", "Georgia", "Times New Roman"), "Inter");
        Spinner<Integer> fontSize = new Spinner<>(12, 96, 42);
        fontSize.setEditable(true);
        TextField textColor = new TextField("#111827");
        ComboBox<TheatreTextPosition> textPosition = enumCombo(TheatreTextPosition.values(), TheatreTextPosition.CENTER);
        ComboBox<TheatreTextEffect> textEffect = enumCombo(TheatreTextEffect.values(), TheatreTextEffect.SHADOW);
        ComboBox<TheatreFrameLayout> frameLayout = enumCombo(TheatreFrameLayout.values(), TheatreFrameLayout.IMAGE_WITH_TEXT);
        CheckBox showSpatialMap = checked("Mapa espacial lateral", false);
        CheckBox showCharacters = checked("Mostrar personajes", true);
        CheckBox showDisplacements = checked("Mostrar desplazamientos", true);
        TextField backgroundColor = new TextField("#FFFFFF");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(8);
        form.setPadding(new Insets(4));
        int row = 0;
        row = addRow(form, row, "Resolucion", resolution);
        row = addRow(form, row, "Fotogramas por segundo", framesPerSecond);
        row = addRow(form, row, "Codificador de video", encoder);
        row = addRow(form, row, "Tipografia", fontFamily);
        row = addRow(form, row, "Tamano de letra", fontSize);
        row = addRow(form, row, "Color del texto", textColor);
        row = addRow(form, row, "Region del texto", textPosition);
        row = addRow(form, row, "Efecto del texto", textEffect);
        row = addRow(form, row, "Diseno del frame", frameLayout);
        addRow(form, row, "Fondo sin imagen", backgroundColor);

        VBox content = new VBox(12,
                new Label("La exportacion usa el audio ya generado. Si falta mapa espacial, el video puede continuar solo con imagen/texto."),
                useFragmentImages,
                showText,
                form,
                showSpatialMap,
                showCharacters,
                showDisplacements);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new TheatreWorkExportOptions(
                        new VideoExportOptions(resolution.getValue(), framesPerSecond.getValue(), encoder.getValue()),
                        useFragmentImages.isSelected(),
                        showText.isSelected(),
                        fontFamily.getValue(),
                        fontSize.getValue(),
                        textColor.getText(),
                        textPosition.getValue(),
                        textEffect.getValue(),
                        frameLayout.getValue(),
                        showSpatialMap.isSelected(),
                        showCharacters.isSelected(),
                        showDisplacements.isSelected(),
                        backgroundColor.getText())
                : null);
        return dialog.showAndWait();
    }

    private static ComboBox<SimpleVideoResolutionPreset> resolutionCombo() {
        ComboBox<SimpleVideoResolutionPreset> combo = new ComboBox<>();
        combo.getItems().setAll(
                SimpleVideoResolutionPreset.UHD_4K,
                SimpleVideoResolutionPreset.QHD_2K,
                SimpleVideoResolutionPreset.FULL_HD_1080,
                SimpleVideoResolutionPreset.HD_720);
        combo.setValue(SimpleVideoResolutionPreset.defaultPreset());
        combo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(combo);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(SimpleVideoResolutionPreset preset) {
                return preset == null ? "" : preset.label() + " - " + preset.width() + "x" + preset.height();
            }

            @Override
            public SimpleVideoResolutionPreset fromString(String value) {
                return combo.getValue();
            }
        });
        return combo;
    }

    private static ComboBox<Integer> framesPerSecondCombo() {
        ComboBox<Integer> combo = new ComboBox<>();
        combo.getItems().setAll(24, 30, 48, 60);
        combo.setValue(30);
        combo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(combo);
        return combo;
    }

    private static ComboBox<VideoEncoderPolicy> encoderCombo(List<VideoEncoderPolicy> availableEncoderPolicies,
                                                             VideoEncoderPolicy defaultEncoderPolicy) {
        List<VideoEncoderPolicy> encoders = availableEncoderPolicies == null || availableEncoderPolicies.isEmpty()
                ? List.of(VideoEncoderPolicy.CPU_X264, VideoEncoderPolicy.AUTO)
                : availableEncoderPolicies;
        ComboBox<VideoEncoderPolicy> combo = new ComboBox<>();
        combo.getItems().setAll(encoders);
        combo.setValue(encoders.contains(defaultEncoderPolicy) ? defaultEncoderPolicy : encoders.get(0));
        combo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(combo);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(VideoEncoderPolicy policy) {
                return policy == null ? "" : policy.label();
            }

            @Override
            public VideoEncoderPolicy fromString(String value) {
                return combo.getValue();
            }
        });
        return combo;
    }

    private static CheckBox checked(String label, boolean selected) {
        CheckBox box = new CheckBox(label);
        box.setSelected(selected);
        return box;
    }

    private static ComboBox<String> stringCombo(List<String> values, String selected) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(values);
        combo.setValue(selected);
        combo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(combo);
        return combo;
    }

    private static <T extends Enum<T>> ComboBox<T> enumCombo(T[] values, T selected) {
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().setAll(values);
        combo.setValue(selected);
        combo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(combo);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                if (value instanceof TheatreTextEffect effect) {
                    return effect.label();
                }
                if (value instanceof TheatreFrameLayout layout) {
                    return layout.label();
                }
                if (value instanceof TheatreTextPosition position) {
                    return position.label();
                }
                return value == null ? "" : value.name();
            }

            @Override
            public T fromString(String value) {
                return combo.getValue();
            }
        });
        return combo;
    }

    private static int addRow(GridPane form, int row, String label, javafx.scene.Node control) {
        Label fieldLabel = new Label(label);
        form.add(fieldLabel, 0, row);
        form.add(control, 1, row);
        GridPane.setHgrow(control, javafx.scene.layout.Priority.ALWAYS);
        return row + 1;
    }

    private static void styleCombo(ComboBox<?> combo) {
        combo.getStyleClass().add("voice-library-combo");
    }
}
