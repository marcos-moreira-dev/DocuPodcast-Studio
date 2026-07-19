package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoBackgroundMode;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreCleanVideoExportOptionsPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreMapExportOptionsPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatrePortionExportOptionsPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoEncodingOptionsPane;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/** Modal entry point for creative exports. */
public final class ExportCenterDialog {
    private static final double EXPORT_CENTER_DIALOG_PREF_WIDTH = 960.0;
    private static final double EXPORT_CENTER_DIALOG_MIN_WIDTH = 760.0;
    private static final double TARGET_LIST_MIN_WIDTH = 300.0;
    private static final double TARGET_LIST_MAX_WIDTH = 520.0;
    private static final double DETAIL_COLUMN_MIN_WIDTH = 420.0;
    private static final double DETAIL_COLUMN_PREF_WIDTH = 620.0;
    private static final double ROW_LABEL_WIDTH = 88.0;

    public Optional<ExportCenterSelection> show(Window owner,
                                                List<ExportTargetPresentation> targets,
                                                AppCommandId preselectedCommand) {
        return show(owner, targets, preselectedCommand, ExportCenterContext.defaults(), null);
    }

    public Optional<ExportCenterSelection> show(Window owner,
                                                List<ExportTargetPresentation> targets,
                                                AppCommandId preselectedCommand,
                                                Consumer<Window> readinessAction) {
        return show(owner, targets, preselectedCommand, ExportCenterContext.defaults(),
                readinessAction == null ? null : (window, target) -> readinessAction.accept(window));
    }

    public Optional<ExportCenterSelection> show(Window owner,
                                                List<ExportTargetPresentation> targets,
                                                AppCommandId preselectedCommand,
                                                ExportCenterContext context,
                                                BiConsumer<Window, ExportCenterSelection> readinessAction) {
        List<ExportTargetPresentation> safeTargets = targets == null ? List.of() : List.copyOf(targets);
        ExportCenterContext safeContext = context == null ? ExportCenterContext.defaults() : context;
        Dialog<ExportCenterSelection> dialog = new Dialog<>();
        dialog.setTitle("Centro de exportaciones");
        dialog.setHeaderText("Elige una salida creativa del proyecto actual");
        DialogStyler.apply(dialog, owner);
        dialog.getDialogPane().getStyleClass().add("export-center-dialog");

        ListView<ExportTargetPresentation> list = new ListView<>();
        list.getStyleClass().add("export-center-target-list");
        list.getItems().setAll(safeTargets);
        list.setMinWidth(TARGET_LIST_MIN_WIDTH);
        list.setPrefWidth(TARGET_LIST_MIN_WIDTH);
        list.setMaxWidth(TARGET_LIST_MAX_WIDTH);
        list.setMinHeight(0);
        list.setPrefHeight(Region.USE_COMPUTED_SIZE);
        list.setMaxHeight(Double.MAX_VALUE);
        list.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(ExportTargetPresentation item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : targetCell(item));
            }
        });

        Label format = valueLabel();
        ComboBox<AudioExportFormat> audioFormat = audioFormatCombo();
        HBox formatCell = new HBox(8, format, audioFormat);
        Label target = valueLabel();
        Label readiness = valueLabel();
        Label processes = valueLabel();
        Label detailTitle = sectionTitle("Detalle");
        Label evidenceTitle = detailHeading("Evidencia");
        Label evidence = valueLabel();
        Label limitationsTitle = detailHeading("Limitaciones");
        Label limitations = valueLabel();
        DocumentTextVideoControls textVideoControls = new DocumentTextVideoControls(owner);
        VideoEncodingOptionsPane videoOptions = new VideoEncodingOptionsPane(
                safeContext.availableEncoders(), safeContext.defaultEncoder(), safeContext.projectMode());
        TheatreCleanVideoExportOptionsPane theatreCleanOptions = new TheatreCleanVideoExportOptionsPane(
                safeContext.availableEncoders(), safeContext.defaultEncoder());
        TheatreMapExportOptionsPane theatreMapOptions = new TheatreMapExportOptionsPane(
                safeContext.availableEncoders(), safeContext.defaultEncoder());
        TheatrePortionExportOptionsPane theatrePortionOptions = new TheatrePortionExportOptionsPane(
                safeContext.acts(), safeContext.scenes(), safeContext.availableEncoders(), safeContext.defaultEncoder());
        setVisible(videoOptions, false);
        setVisible(theatreCleanOptions, false);
        setVisible(theatreMapOptions, false);
        setVisible(theatrePortionOptions, false);

        GridPane details = new GridPane();
        details.getStyleClass().add("export-center-summary-grid");
        details.setHgap(10);
        details.setVgap(8);
        details.setPrefWidth(DETAIL_COLUMN_PREF_WIDTH);
        details.setMaxWidth(Double.MAX_VALUE);
        details.add(rowLabel("Formato"), 0, 0);
        details.add(formatCell, 1, 0);
        details.add(rowLabel("Destino"), 0, 1);
        details.add(target, 1, 1);
        details.add(rowLabel("Estado"), 0, 2);
        details.add(readiness, 1, 2);
        details.add(rowLabel("Procesos"), 0, 3);
        details.add(processes, 1, 3);
        GridPane.setHgrow(formatCell, Priority.ALWAYS);
        GridPane.setHgrow(target, Priority.ALWAYS);
        GridPane.setHgrow(readiness, Priority.ALWAYS);
        GridPane.setHgrow(processes, Priority.ALWAYS);

        Label note = new Label("El centro no genera audio, imagenes ni assets faltantes en silencio. Usa Ver estado para revisar bloqueos completos.");
        note.setWrapText(true);
        HBox noteBox = noteBox(note);
        VBox detailSections = new VBox(7, detailTitle, evidenceTitle, evidence, limitationsTitle, limitations);
        detailSections.getStyleClass().add("export-center-detail-sections");
        VBox detailBox = new VBox(16, details, textVideoControls.root(), videoOptions, theatreCleanOptions,
                theatreMapOptions, theatrePortionOptions, detailSections, noteBox);
        detailBox.getStyleClass().add("export-center-detail-pane");
        detailBox.setMinWidth(DETAIL_COLUMN_MIN_WIDTH);
        detailBox.setPrefWidth(DETAIL_COLUMN_PREF_WIDTH);
        detailBox.setMaxWidth(Double.MAX_VALUE);
        ScrollPane detailScroll = new ScrollPane(detailBox);
        detailScroll.getStyleClass().add("export-center-detail-scroll");
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailScroll.setMinWidth(DETAIL_COLUMN_MIN_WIDTH);
        detailScroll.setPrefWidth(DETAIL_COLUMN_PREF_WIDTH);
        detailScroll.setMaxWidth(Double.MAX_VALUE);
        HBox content = new HBox(28, list, detailScroll);
        content.getStyleClass().add("export-center-content");
        content.setPadding(new Insets(12));
        content.setPrefWidth(EXPORT_CENTER_DIALOG_PREF_WIDTH - 36.0);
        content.setFillHeight(true);
        HBox.setHgrow(list, Priority.SOMETIMES);
        HBox.setHgrow(detailScroll, Priority.ALWAYS);
        content.widthProperty().addListener((obs, oldValue, newValue) ->
                list.setPrefWidth(adaptiveTargetListWidth(newValue.doubleValue())));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(EXPORT_CENTER_DIALOG_PREF_WIDTH);
        dialog.getDialogPane().setMinWidth(EXPORT_CENTER_DIALOG_MIN_WIDTH);

        ButtonType export = new ButtonType("Exportar", ButtonBar.ButtonData.OK_DONE);
        ButtonType readinessButton = new ButtonType("Ver estado", ButtonBar.ButtonData.APPLY);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(export, readinessButton, cancel);

        Runnable updateDetails = () -> {
            ExportTargetPresentation selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                format.setText("-");
                audioFormat.setVisible(false);
                audioFormat.setManaged(false);
                textVideoControls.setVisible(false);
                setVisible(videoOptions, false);
                setVisible(theatreCleanOptions, false);
                setVisible(theatreMapOptions, false);
                setVisible(theatrePortionOptions, false);
                target.setText("-");
                readiness.setText("Sin salida seleccionada");
                processes.setText("Sin procesos relacionados registrados.");
                renderDetailSections(detailTitle, evidenceTitle, evidence, limitationsTitle, limitations, "");
                return;
            }
            boolean audioSelected = selected.commandId() == AppCommandId.EXPORT_PODCAST_WAV;
            boolean documentVideoSelected = selected.commandId() == AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO;
            boolean commonVideoSelected = selected.commandId() == AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE;
            boolean theatreCleanSelected = selected.commandId() == AppCommandId.EXPORT_THEATRE_WORK;
            boolean theatreMapSelected = selected.commandId() == AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW;
            boolean theatrePortionSelected = selected.commandId() == AppCommandId.EXPORT_THEATRE_PORTION;
            format.setText(audioSelected ? "" : selected.format());
            audioFormat.setVisible(audioSelected);
            audioFormat.setManaged(audioSelected);
            textVideoControls.setVisible(documentVideoSelected);
            setVisible(videoOptions, commonVideoSelected);
            setVisible(theatreCleanOptions, theatreCleanSelected);
            setVisible(theatreMapOptions, theatreMapSelected);
            setVisible(theatrePortionOptions, theatrePortionSelected);
            target.setText(audioSelected ? "audio-final" + audioFormat.getValue().extension() : selected.targetHint());
            readiness.setText(selected.readinessLabel());
            processes.setText(selected.relatedProcessSummary());
            renderDetailSections(detailTitle, evidenceTitle, evidence, limitationsTitle, limitations, selected.detail());
        };
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateDetails.run());
        audioFormat.valueProperty().addListener((obs, oldValue, newValue) -> updateDetails.run());
        selectInitial(list, safeTargets, preselectedCommand);
        updateDetails.run();

        Button exportButton = (Button) dialog.getDialogPane().lookupButton(export);
        Button readinessNode = (Button) dialog.getDialogPane().lookupButton(readinessButton);
        styleDialogButton(exportButton, "ui-action-button-primary");
        styleDialogButton((Button) dialog.getDialogPane().lookupButton(cancel), "ui-action-button-secondary");
        styleDialogButton(readinessNode, "ui-action-button-secondary");
        readinessNode.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            if (readinessAction != null) {
                readinessAction.accept(dialogWindow(dialog, owner), selectionFrom(
                        list.getSelectionModel().getSelectedItem(), audioFormat, textVideoControls, videoOptions,
                        theatreCleanOptions, theatreMapOptions, theatrePortionOptions));
            }
        });
        exportButton.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull()
                .or(javafx.beans.binding.Bindings.createBooleanBinding(
                        () -> {
                            ExportTargetPresentation selected = list.getSelectionModel().getSelectedItem();
                            return selected != null && !selected.executable();
                        },
                        list.getSelectionModel().selectedItemProperty())));

        dialog.setResultConverter(button -> {
            if (button == export) {
                return selectionFrom(list.getSelectionModel().getSelectedItem(), audioFormat, textVideoControls,
                        videoOptions, theatreCleanOptions, theatreMapOptions, theatrePortionOptions);
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private static ExportCenterSelection selectionFrom(
            ExportTargetPresentation selected,
            ComboBox<AudioExportFormat> audioFormat,
            DocumentTextVideoControls textVideoControls,
            VideoEncodingOptionsPane videoOptions,
            TheatreCleanVideoExportOptionsPane theatreCleanOptions,
            TheatreMapExportOptionsPane theatreMapOptions,
            TheatrePortionExportOptionsPane theatrePortionOptions) {
        return selected == null ? null : new ExportCenterSelection(
                ExportCenterAction.EXPORT_SELECTED,
                selected.commandId(),
                audioFormat.getValue(),
                textVideoControls.options(),
                selected.commandId() == AppCommandId.EXPORT_THEATRE_WORK
                        ? theatreCleanOptions.options() : videoOptions.options(),
                theatreMapOptions.options(),
                theatrePortionOptions.options());
    }

    private static Window dialogWindow(Dialog<?> dialog, Window fallback) {
        return dialog.getDialogPane().getScene() == null
                ? fallback
                : dialog.getDialogPane().getScene().getWindow();
    }

    private static VBox targetCell(ExportTargetPresentation item) {
        Label title = new Label(item.title());
        title.getStyleClass().add("export-center-target-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label(item.readinessLabel());
        status.getStyleClass().add("export-center-target-status");
        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(5, title, status);
        card.getStyleClass().add("export-center-target-card");
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static double adaptiveTargetListWidth(double contentWidth) {
        double preferred = Math.max(TARGET_LIST_MIN_WIDTH, contentWidth * 0.32);
        return Math.min(TARGET_LIST_MAX_WIDTH, preferred);
    }

    private static void styleDialogButton(Button button, String variant) {
        if (button == null) {
            return;
        }
        button.getStyleClass().addAll("ui-action-button", variant);
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("export-center-section-title");
        return label;
    }

    private static Label detailHeading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("export-center-detail-heading");
        return label;
    }

    private static HBox noteBox(Label note) {
        Label icon = new Label("i");
        icon.getStyleClass().add("export-center-note-icon");
        note.getStyleClass().add("export-center-note-text");
        HBox box = new HBox(12, icon, note);
        box.getStyleClass().add("export-center-note");
        HBox.setHgrow(note, Priority.ALWAYS);
        return box;
    }

    private static void renderDetailSections(Label title,
                                             Label evidenceTitle,
                                             Label evidence,
                                             Label limitationsTitle,
                                             Label limitations,
                                             String detail) {
        DetailSections sections = DetailSections.from(detail);
        boolean hasEvidence = !sections.evidence().isBlank();
        boolean hasLimitations = !sections.limitations().isBlank();
        setVisible(title, hasEvidence || hasLimitations);
        setVisible(evidenceTitle, hasEvidence);
        setVisible(evidence, hasEvidence);
        setVisible(limitationsTitle, hasLimitations);
        setVisible(limitations, hasLimitations);
        evidence.setText(sections.evidence());
        limitations.setText(sections.limitations());
    }

    private static void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static ComboBox<AudioExportFormat> audioFormatCombo() {
        ComboBox<AudioExportFormat> combo = new ComboBox<>(FXCollections.observableArrayList(AudioExportFormat.values()));
        combo.setValue(AudioExportFormat.WAV);
        combo.setMaxWidth(180);
        StudioFormControls.combo(combo, "Formato de audio final para la salida seleccionada.");
        combo.getStyleClass().add("voice-library-combo");
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AudioExportFormat format) {
                return format == null ? "" : format.displayName();
            }

            @Override
            public AudioExportFormat fromString(String value) {
                return combo.getValue();
            }
        });
        return combo;
    }

    private static final class DocumentTextVideoControls {
        private final VBox root = new VBox(8);
        private final ComboBox<DocumentTextVideoBackgroundMode> backgroundMode =
                new ComboBox<>(FXCollections.observableArrayList(DocumentTextVideoBackgroundMode.values()));
        private final ColorPicker backgroundColor = new ColorPicker(Color.WHITE);
        private final ColorPicker textColor = new ColorPicker(Color.web("#20232A"));
        private final ComboBox<String> fontFamily = new ComboBox<>();
        private final Spinner<Integer> fontSize = new Spinner<>(18, 160, 54);
        private final Label imageLabel = valueLabel();
        private final StackPane previewFrame = new StackPane();
        private final Label previewTitle = new Label("La luz escribe despacio sobre la pagina");
        private final Label previewBody = new Label("Un fragmento hipotetico respira en pantalla mientras la voz acompana el estudio documental.");
        private Path backgroundImage;

        private DocumentTextVideoControls(Window owner) {
            root.getStyleClass().add("export-center-document-video-options");
            Label title = new Label("Opciones de video documental");
            title.getStyleClass().add("dialog-field-label");
            backgroundMode.setValue(DocumentTextVideoBackgroundMode.SOLID_COLOR);
            backgroundMode.setMaxWidth(Double.MAX_VALUE);
            StudioFormControls.combo(backgroundMode, "Elegir si el frame usa color solido o imagen de fondo.");
            backgroundMode.getStyleClass().add("voice-library-combo");
            backgroundMode.setConverter(new StringConverter<>() {
                @Override
                public String toString(DocumentTextVideoBackgroundMode mode) {
                    return mode == null ? "" : mode.displayName();
                }

                @Override
                public DocumentTextVideoBackgroundMode fromString(String value) {
                    return backgroundMode.getValue();
                }
            });
            fontFamily.getItems().setAll(Font.getFamilies());
            fontFamily.setValue(defaultFontFamily(fontFamily.getItems()));
            fontFamily.setMaxWidth(Double.MAX_VALUE);
            StudioFormControls.combo(fontFamily, "Fuente aplicada al texto del frame documental.");
            fontFamily.getStyleClass().add("voice-library-combo");
            StudioFormControls.colorPicker(backgroundColor, "Color de fondo usado cuando el modo de fondo es color solido.");
            StudioFormControls.colorPicker(textColor, "Color del texto del frame documental.");
            StudioFormControls.spinner(fontSize, "Tamano base de la tipografia del frame documental.");
            fontSize.setEditable(true);
            imageLabel.setText("Sin imagen de fondo");
            Button chooseImage = ActionButtonFactory.secondary(
                    "Elegir imagen de fondo",
                    "Usar una imagen local como fondo del video documental.",
                    () -> chooseBackgroundImage(owner));
            chooseImage.disableProperty().bind(backgroundMode.valueProperty().isNotEqualTo(DocumentTextVideoBackgroundMode.IMAGE));
            imageLabel.disableProperty().bind(chooseImage.disableProperty());
            configurePreview();
            root.getChildren().addAll(title,
                    field("Fondo", backgroundMode),
                    field("Color de fondo", backgroundColor),
                    field("Imagen", new HBox(8, chooseImage, imageLabel)),
                    field("Color de texto", textColor),
                    field("Fuente", fontFamily),
                    field("Tamano", fontSize),
                    previewFrame);
            setVisible(false);
        }

        private VBox root() {
            return root;
        }

        private void setVisible(boolean visible) {
            root.setVisible(visible);
            root.setManaged(visible);
        }

        private DocumentTextVideoOptions options() {
            return new DocumentTextVideoOptions(
                    SimpleVideoResolutionPreset.defaultPreset(),
                    backgroundMode.getValue(),
                    hex(backgroundColor.getValue()),
                    backgroundImage == null ? "" : backgroundImage.toString(),
                    hex(textColor.getValue()),
                    "#4F46E5",
                    fontFamily.getValue(),
                    fontSize.getValue());
        }

        private void chooseBackgroundImage(Window owner) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Elegir imagen de fondo documental");
            chooser.getExtensionFilters().setAll(
                    new FileChooser.ExtensionFilter("Imagenes (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG (*.jpg, *.jpeg)", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(owner);
            if (file != null) {
                backgroundImage = file.toPath();
                imageLabel.setText(file.getName());
                updatePreview();
            }
        }

        private void configurePreview() {
            previewFrame.getStyleClass().add("export-center-frame-preview");
            previewTitle.getStyleClass().add("export-center-frame-preview-title");
            previewBody.getStyleClass().add("export-center-frame-preview-body");
            previewTitle.setWrapText(true);
            previewBody.setWrapText(true);
            VBox copy = new VBox(8, previewTitle, previewBody);
            copy.getStyleClass().add("export-center-frame-preview-copy");
            StackPane.setAlignment(copy, javafx.geometry.Pos.CENTER_LEFT);
            previewFrame.getChildren().setAll(copy);
            backgroundMode.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            backgroundColor.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            textColor.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            fontFamily.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            fontSize.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            updatePreview();
        }

        private void updatePreview() {
            String foreground = hex(textColor.getValue());
            String family = fontFamily.getValue() == null || fontFamily.getValue().isBlank()
                    ? "SansSerif"
                    : fontFamily.getValue();
            int size = fontSize.getValue() == null ? 54 : fontSize.getValue();
            previewTitle.setTextFill(textColor.getValue() == null ? Color.web("#20232A") : textColor.getValue());
            previewBody.setTextFill(textColor.getValue() == null ? Color.web("#20232A") : textColor.getValue());
            previewTitle.setFont(Font.font(family, Math.max(18, size * 0.46)));
            previewBody.setFont(Font.font(family, Math.max(12, size * 0.28)));
            if (backgroundMode.getValue() == DocumentTextVideoBackgroundMode.IMAGE && backgroundImage != null) {
                previewFrame.setBackground(imageBackground(backgroundImage));
            } else {
                previewFrame.setBackground(new Background(new BackgroundFill(
                        backgroundColor.getValue() == null ? Color.WHITE : backgroundColor.getValue(),
                        new CornerRadii(6),
                        javafx.geometry.Insets.EMPTY)));
            }
            previewFrame.setBorder(frameBorder());
            previewFrame.setAccessibleText("Vista previa del frame documental con texto " + foreground + ".");
        }

        private static Background imageBackground(Path imagePath) {
            Image image = new Image(imagePath.toUri().toString(), true);
            BackgroundImage background = new BackgroundImage(
                    image,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true));
            return new Background(background);
        }

        private static Border frameBorder() {
            return new Border(new BorderStroke(
                    Color.web("#D9E1F2"),
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(6),
                    BorderWidths.DEFAULT));
        }
    }

    private static HBox field(String label, javafx.scene.Node node) {
        Label key = rowLabel(label);
        key.setMinWidth(112);
        HBox row = new HBox(10, key, node);
        HBox.setHgrow(node, Priority.ALWAYS);
        return row;
    }

    private static String defaultFontFamily(List<String> families) {
        if (families == null || families.isEmpty()) {
            return "SansSerif";
        }
        for (String candidate : List.of("Arial", "Calibri", "Inter", "SansSerif")) {
            if (families.contains(candidate)) {
                return candidate;
            }
        }
        return families.getFirst();
    }

    private static String hex(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return "#%02X%02X%02X".formatted(
                Math.round((float) safe.getRed() * 255),
                Math.round((float) safe.getGreen() * 255),
                Math.round((float) safe.getBlue() * 255));
    }

    private static void selectInitial(ListView<ExportTargetPresentation> list,
                                      List<ExportTargetPresentation> targets,
                                      AppCommandId commandId) {
        if (targets.isEmpty()) {
            return;
        }
        if (commandId != null) {
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i).commandId() == commandId) {
                    list.getSelectionModel().select(i);
                    return;
                }
            }
        }
        list.getSelectionModel().select(0);
    }

    private static Label rowLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("dialog-field-label", "export-center-field-label");
        label.setMinWidth(ROW_LABEL_WIDTH);
        label.setPrefWidth(ROW_LABEL_WIDTH);
        label.setMaxWidth(ROW_LABEL_WIDTH);
        return label;
    }

    private static Label valueLabel() {
        Label label = new Label();
        label.getStyleClass().add("export-center-value");
        label.setWrapText(true);
        return label;
    }

    private record DetailSections(String evidence, String limitations) {
        private static DetailSections from(String detail) {
            String text = detail == null ? "" : detail.strip();
            if (text.isBlank()) {
                return new DetailSections("", "");
            }
            String evidenceMarker = "Evidencia:";
            String limitationsMarker = "Limitaciones:";
            int evidenceStart = text.indexOf(evidenceMarker);
            int limitationsStart = text.indexOf(limitationsMarker);
            if (evidenceStart < 0 && limitationsStart < 0) {
                return new DetailSections(text, "");
            }
            int firstMarker = firstMarker(evidenceStart, limitationsStart);
            String intro = firstMarker > 0 ? text.substring(0, firstMarker).strip() : "";
            String evidenceText = "";
            String limitationsText = "";
            if (evidenceStart >= 0) {
                int start = evidenceStart + evidenceMarker.length();
                int end = limitationsStart > evidenceStart ? limitationsStart : text.length();
                evidenceText = text.substring(start, end).strip();
            }
            if (limitationsStart >= 0) {
                limitationsText = text.substring(limitationsStart + limitationsMarker.length()).strip();
            }
            if (!intro.isBlank()) {
                evidenceText = evidenceText.isBlank() ? intro : intro + "\n" + evidenceText;
            }
            return new DetailSections(evidenceText, limitationsText);
        }

        private static int firstMarker(int evidenceStart, int limitationsStart) {
            if (evidenceStart < 0) {
                return limitationsStart;
            }
            if (limitationsStart < 0) {
                return evidenceStart;
            }
            return Math.min(evidenceStart, limitationsStart);
        }
    }
}
