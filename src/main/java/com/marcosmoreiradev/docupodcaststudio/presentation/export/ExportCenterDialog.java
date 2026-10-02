package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoBackgroundMode;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentBackgroundImageFit;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextEffect;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
        Dialog<ExportCenterSelection> dialog = StudioDialogShell.dialog();
        dialog.setTitle("Centro de exportaciones");
        dialog.setHeaderText("Elige una salida creativa del proyecto actual");
        DialogStyler.apply(dialog, owner);
        dialog.getDialogPane().getStyleClass().add("export-center-dialog");

        ListView<ExportTargetPresentation> list = StudioCollectionControls.listView();
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
        CheckBox prepareBeforeExport = StudioFormControls.checkBox(
                "Completar lo autorizado y exportar",
                "Completa la lectura según la configuración actual, sin activar categorías omitidas; después exporta automáticamente.");
        Label prepareBeforeExportHelp = new Label(
                "Respeta el alcance y las categorías configuradas por el usuario. No activa imágenes, tablas ni IA omitidas; reutiliza los derivados vigentes y genera solamente lo pendiente.");
        prepareBeforeExportHelp.setWrapText(true);
        prepareBeforeExportHelp.getStyleClass().add("export-center-note-text");
        VBox preparationMode = new VBox(5, prepareBeforeExport, prepareBeforeExportHelp);
        preparationMode.getStyleClass().add("export-center-preparation-mode");
        setVisible(preparationMode, false);
        DocumentTextVideoControls textVideoControls = new DocumentTextVideoControls(owner);
        VideoEncodingOptionsPane videoOptions = new VideoEncodingOptionsPane(
                safeContext.availableEncoders(), safeContext.defaultEncoder(), safeContext.projectMode());
        TheatreCleanVideoExportOptionsPane theatreCleanOptions = new TheatreCleanVideoExportOptionsPane(
                safeContext.availableEncoders(), safeContext.defaultEncoder());
        TheatreMapExportOptionsPane theatreMapOptions = new TheatreMapExportOptionsPane(
                safeContext.availableEncoders(), safeContext.defaultEncoder());
        TheatrePortionExportOptionsPane theatrePortionOptions = new TheatrePortionExportOptionsPane(
                safeContext.acts(), safeContext.scenes(), safeContext.availableEncoders(), safeContext.defaultEncoder());
        theatreMapOptions.setPresentationMode(safeContext.theatrePresentationMode());
        theatrePortionOptions.setPresentationMode(safeContext.theatrePresentationMode());
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
        VBox detailBox = new VBox(16, details, preparationMode, textVideoControls.root(), videoOptions, theatreCleanOptions,
                theatreMapOptions, theatrePortionOptions, detailSections, noteBox);
        detailBox.getStyleClass().add("export-center-detail-pane");
        detailBox.setMinWidth(DETAIL_COLUMN_MIN_WIDTH);
        detailBox.setPrefWidth(DETAIL_COLUMN_PREF_WIDTH);
        detailBox.setMaxWidth(Double.MAX_VALUE);
        ScrollPane detailScroll = StudioViewportControls.scrollPane(detailBox);
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

        ButtonType export = NativeDialogResponse.button("Exportar", ButtonBar.ButtonData.OK_DONE);
        ButtonType readinessButton = NativeDialogResponse.button("Ver estado", ButtonBar.ButtonData.APPLY);
        ButtonType cancel = NativeDialogResponse.button("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(export, readinessButton, cancel);

        Runnable updateDetails = () -> {
            ExportTargetPresentation selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                format.setText("-");
                audioFormat.setVisible(false);
                audioFormat.setManaged(false);
                textVideoControls.setVisible(false);
                setVisible(preparationMode, false);
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
            boolean preparationSupported = audioSelected || documentVideoSelected;
            format.setText(audioSelected ? "" : selected.format());
            audioFormat.setVisible(audioSelected);
            audioFormat.setManaged(audioSelected);
            textVideoControls.setVisible(documentVideoSelected);
            setVisible(preparationMode, preparationSupported);
            prepareBeforeExport.setDisable(!selected.preparable());
            if (!preparationSupported || !selected.preparable()) {
                prepareBeforeExport.setSelected(false);
            }
            setVisible(videoOptions, commonVideoSelected);
            setVisible(theatreCleanOptions, theatreCleanSelected);
            setVisible(theatreMapOptions, theatreMapSelected);
            setVisible(theatrePortionOptions, theatrePortionSelected);
            target.setText(audioSelected ? "audio-final" + audioFormat.getValue().extension() : selected.targetHint());
            readiness.setText(selected.readinessLabel());
            if (prepareBeforeExport.isSelected() && preparationSupported) {
                readiness.setText(selected.executable()
                        ? "Se verificará y reutilizará lo existente"
                        : "Se preparará antes de exportar");
            }
            processes.setText(selected.relatedProcessSummary());
            note.setText(prepareBeforeExport.isSelected() && preparationSupported
                    ? "Autorizaste completar lo permitido por la configuración actual. No se activarán categorías omitidas; los derivados vigentes se reutilizarán y podrás cancelar desde el progreso."
                    : "El centro no genera derivados faltantes en silencio. Activa 'Completar lo autorizado y exportar' para completar únicamente lo permitido por tu configuración.");
            renderDetailSections(detailTitle, evidenceTitle, evidence, limitationsTitle, limitations, selected.detail());
        };
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateDetails.run());
        audioFormat.valueProperty().addListener((obs, oldValue, newValue) -> updateDetails.run());
        prepareBeforeExport.selectedProperty().addListener((obs, oldValue, newValue) -> updateDetails.run());
        selectInitial(list, safeTargets, preselectedCommand);
        updateDetails.run();

        Button exportButton = (Button) dialog.getDialogPane().lookupButton(export);
        Button readinessNode = (Button) dialog.getDialogPane().lookupButton(readinessButton);
        // A data-entry dialog must not interpret Enter in an editable control as
        // confirmation of a potentially hours-long export.
        exportButton.setDefaultButton(false);
        styleDialogButton(exportButton, "ui-action-button-primary");
        styleDialogButton((Button) dialog.getDialogPane().lookupButton(cancel), "ui-action-button-secondary");
        styleDialogButton(readinessNode, "ui-action-button-secondary");
        readinessNode.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            if (readinessAction != null) {
                readinessAction.accept(dialogWindow(dialog, owner), selectionFrom(
                        list.getSelectionModel().getSelectedItem(), prepareBeforeExport, audioFormat, textVideoControls, videoOptions,
                        theatreCleanOptions, theatreMapOptions, theatrePortionOptions));
            }
        });
        exportButton.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull()
                .or(javafx.beans.binding.Bindings.createBooleanBinding(
                        () -> {
                            ExportTargetPresentation selected = list.getSelectionModel().getSelectedItem();
                            if (selected == null) return false;
                            return !selected.executable()
                                    && !(prepareBeforeExport.isSelected() && selected.preparable());
                        },
                        list.getSelectionModel().selectedItemProperty(),
                        prepareBeforeExport.selectedProperty())));
        prepareBeforeExport.selectedProperty().addListener((obs, oldValue, selected) ->
                exportButton.setText(Boolean.TRUE.equals(selected)
                        ? "Completar y exportar" : "Exportar"));

        dialog.setResultConverter(button -> {
            if (button == export) {
                return selectionFrom(list.getSelectionModel().getSelectedItem(), audioFormat, textVideoControls,
                        videoOptions, theatreCleanOptions, theatreMapOptions, theatrePortionOptions,
                        prepareBeforeExport);
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private static ExportCenterSelection selectionFrom(
            ExportTargetPresentation selected,
            CheckBox prepareBeforeExport,
            ComboBox<AudioExportFormat> audioFormat,
            DocumentTextVideoControls textVideoControls,
            VideoEncodingOptionsPane videoOptions,
            TheatreCleanVideoExportOptionsPane theatreCleanOptions,
            TheatreMapExportOptionsPane theatreMapOptions,
            TheatrePortionExportOptionsPane theatrePortionOptions) {
        return selected == null ? null : new ExportCenterSelection(
                ExportCenterAction.EXPORT_SELECTED,
                selected.commandId(),
                prepareBeforeExport != null && prepareBeforeExport.isSelected()
                        ? ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT
                        : ExportExecutionMode.READY_ONLY,
                audioFormat.getValue(),
                textVideoControls.options(),
                selected.commandId() == AppCommandId.EXPORT_THEATRE_WORK
                        ? theatreCleanOptions.options() : videoOptions.options(),
                theatreMapOptions.options(),
                theatrePortionOptions.options());
    }

    private static ExportCenterSelection selectionFrom(
            ExportTargetPresentation selected,
            ComboBox<AudioExportFormat> audioFormat,
            DocumentTextVideoControls textVideoControls,
            VideoEncodingOptionsPane videoOptions,
            TheatreCleanVideoExportOptionsPane theatreCleanOptions,
            TheatreMapExportOptionsPane theatreMapOptions,
            TheatrePortionExportOptionsPane theatrePortionOptions,
            CheckBox prepareBeforeExport) {
        return selectionFrom(selected, prepareBeforeExport, audioFormat, textVideoControls,
                videoOptions, theatreCleanOptions, theatreMapOptions, theatrePortionOptions);
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
        ComboBox<AudioExportFormat> combo = StudioFormControls.comboBox(FXCollections.observableArrayList(AudioExportFormat.values()));
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
                StudioFormControls.comboBox(FXCollections.observableArrayList(DocumentTextVideoBackgroundMode.values()));
        private final ColorPicker backgroundColor = StudioFormControls.colorPicker(Color.WHITE);
        private final ColorPicker textColor = StudioFormControls.colorPicker(Color.web("#20232A"));
        private final Slider backgroundImageOpacity = StudioFormControls.slider(
                StudioFormControls.slider(5, 100, 35), "Visibilidad de la imagen de fondo.");
        private final Label backgroundImageOpacityValue = valueLabel();
        private final ToggleGroup backgroundImageFitGroup = new ToggleGroup();
        private final RadioButton containBackground = new RadioButton("Imagen completa con franjas");
        private final RadioButton coverBackground = new RadioButton("Rellenar con recorte/zoom");
        private final RadioButton blurredBackground = new RadioButton("Fondo difuminado + imagen completa");
        private final ComboBox<DocumentTextEffect> textEffect =
                StudioFormControls.comboBox(FXCollections.observableArrayList(DocumentTextEffect.values()));
        private final ColorPicker textEffectColor = StudioFormControls.colorPicker(Color.BLACK);
        private final Spinner<Integer> textEffectThickness = StudioFormControls.spinner(1, 12, 3);
        private final CheckBox underlineNarratedText = StudioFormControls.checkBox(
                "Subrayar el fragmento narrado",
                "Subraya únicamente el texto que corresponde al audio de la unidad actual.");
        private final ColorPicker narratedUnderlineColor = StudioFormControls.colorPicker(Color.web("#4F46E5"));
        private final Slider narratedUnderlineThickness = StudioFormControls.slider(
                StudioFormControls.slider(0, 15, 4),
                "Grosor del subrayado narrativo, entre 0 y 15 píxeles.");
        private final Label narratedUnderlineThicknessValue = valueLabel();
        private final ComboBox<String> fontFamily = StudioFormControls.comboBox();
        private final ComboBox<String> titleFontFamily = StudioFormControls.comboBox();
        private final Spinner<Integer> fontSize = StudioFormControls.spinner(18, 160, 54);
        private final Label imageLabel = valueLabel();
        private final StackPane previewFrame = new StackPane();
        private final ImageView previewBackground = new ImageView();
        private final ImageView previewForeground = new ImageView();
        private final Label previewTitle = new Label("La luz escribe despacio sobre la pagina");
        private final Label previewBody = new Label("Un fragmento hipotetico respira en pantalla mientras la voz acompana el estudio documental.");
        private final Region previewNarratedUnderline = new Region();
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
            configureFontCombo(fontFamily);
            StudioFormControls.combo(fontFamily, "Fuente aplicada al cuerpo del texto documental.");
            fontFamily.getStyleClass().add("voice-library-combo");
            titleFontFamily.getItems().setAll(fontFamily.getItems());
            titleFontFamily.setValue(fontFamily.getValue());
            titleFontFamily.setMaxWidth(Double.MAX_VALUE);
            configureFontCombo(titleFontFamily);
            StudioFormControls.combo(titleFontFamily, "Fuente aplicada a títulos y subtítulos documentales.");
            titleFontFamily.getStyleClass().add("voice-library-combo");
            StudioFormControls.colorPicker(backgroundColor, "Color de fondo usado cuando el modo de fondo es color solido.");
            StudioFormControls.colorPicker(textColor, "Color del texto del frame documental.");
            backgroundImageOpacity.setMajorTickUnit(20);
            backgroundImageOpacity.setBlockIncrement(5);
            backgroundImageOpacityValue.setMinWidth(52);
            backgroundImageOpacity.disableProperty().bind(backgroundMode.valueProperty().isNotEqualTo(DocumentTextVideoBackgroundMode.IMAGE));
            backgroundImageOpacityValue.disableProperty().bind(backgroundImageOpacity.disableProperty());
            configureBackgroundFitChoices();
            textEffect.setValue(DocumentTextEffect.NONE);
            textEffect.setConverter(new StringConverter<>() {
                @Override public String toString(DocumentTextEffect effect) { return effect == null ? "" : effect.displayName(); }
                @Override public DocumentTextEffect fromString(String value) { return textEffect.getValue(); }
            });
            textEffectColor.disableProperty().bind(textEffect.valueProperty().isEqualTo(DocumentTextEffect.NONE));
            textEffectThickness.disableProperty().bind(textEffectColor.disableProperty());
            underlineNarratedText.setSelected(true);
            StudioFormControls.colorPicker(narratedUnderlineColor,
                    "Color del subrayado que acompaña al fragmento narrado.");
            narratedUnderlineThickness.setMajorTickUnit(5);
            narratedUnderlineThickness.setMinorTickCount(4);
            narratedUnderlineThickness.setBlockIncrement(1);
            narratedUnderlineThickness.setSnapToTicks(true);
            narratedUnderlineThicknessValue.setMinWidth(52);
            narratedUnderlineColor.disableProperty().bind(underlineNarratedText.selectedProperty().not());
            narratedUnderlineThickness.disableProperty().bind(underlineNarratedText.selectedProperty().not());
            narratedUnderlineThicknessValue.disableProperty().bind(underlineNarratedText.selectedProperty().not());
            StudioFormControls.spinner(fontSize, "Tamano base de la tipografia del frame documental.");
            fontSize.setEditable(true);
            configurePreviewSpinner();
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
                    field("Visibilidad", percentageControl(backgroundImageOpacity, backgroundImageOpacityValue)),
                    field("Ajuste de la imagen", new VBox(5,
                            containBackground, coverBackground, blurredBackground)),
                    field("Color de texto", textColor),
                    field("Efecto de texto", textEffect),
                    field("Color efecto", textEffectColor),
                    field("Grosor efecto", textEffectThickness),
                    underlineNarratedText,
                    field("Color subrayado", narratedUnderlineColor),
                    field("Grosor", underlineThicknessControl()),
                    field("Fuente del cuerpo", fontFamily),
                    field("Fuente de títulos y subtítulos", titleFontFamily),
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
                    titleFontFamily.getValue(),
                    fontSize.getValue(),
                    underlineNarratedText.isSelected(),
                    hex(narratedUnderlineColor.getValue()),
                    (int) Math.round(narratedUnderlineThickness.getValue()),
                    backgroundImageOpacity.getValue() / 100.0,
                    selectedBackgroundImageFit(),
                    textEffect.getValue(), hex(textEffectColor.getValue()), textEffectThickness.getValue());
        }

        private HBox underlineThicknessControl() {
            HBox row = new HBox(10, narratedUnderlineThickness, narratedUnderlineThicknessValue);
            HBox.setHgrow(narratedUnderlineThickness, Priority.ALWAYS);
            narratedUnderlineThickness.setMaxWidth(Double.MAX_VALUE);
            return row;
        }

        private void chooseBackgroundImage(Window owner) {
            FileChooser chooser = NativeSourceChooser.fileChooser();
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
            previewNarratedUnderline.setMaxWidth(Double.MAX_VALUE);
            VBox copy = new VBox(8, previewTitle, previewBody, previewNarratedUnderline);
            copy.getStyleClass().add("export-center-frame-preview-copy");
            StackPane.setAlignment(copy, javafx.geometry.Pos.CENTER_LEFT);
            previewBackground.setPreserveRatio(false);
            previewBackground.fitWidthProperty().bind(previewFrame.widthProperty());
            previewBackground.fitHeightProperty().bind(previewFrame.heightProperty());
            previewBackground.setMouseTransparent(true);
            previewForeground.setManaged(false);
            previewForeground.setPreserveRatio(true);
            previewForeground.fitWidthProperty().bind(previewFrame.widthProperty());
            previewForeground.fitHeightProperty().bind(previewFrame.heightProperty());
            previewForeground.setMouseTransparent(true);
            previewFrame.getChildren().setAll(previewBackground, previewForeground, copy);
            backgroundMode.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            backgroundColor.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            backgroundImageOpacity.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            backgroundImageFitGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            textColor.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            textEffect.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            textEffectColor.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            textEffectThickness.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            underlineNarratedText.selectedProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            narratedUnderlineColor.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            narratedUnderlineThickness.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            fontFamily.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            titleFontFamily.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            fontSize.valueProperty().addListener((obs, oldValue, newValue) -> updatePreview());
            updatePreview();
        }

        private void configurePreviewSpinner() {
            fontSize.getEditor().setOnAction(event -> {
                commitFontSizeEditor();
                event.consume();
            });
            fontSize.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if (event.getCode() == KeyCode.ENTER) event.consume();
            });
        }

        private void commitFontSizeEditor() {
            var factory = fontSize.getValueFactory();
            if (factory == null || factory.getConverter() == null) return;
            try {
                Integer parsed = factory.getConverter().fromString(fontSize.getEditor().getText());
                factory.setValue(parsed);
            } catch (RuntimeException invalidValue) {
                fontSize.getEditor().setText(factory.getConverter().toString(factory.getValue()));
            }
            updatePreview();
        }

        private void updatePreview() {
            String foreground = hex(textColor.getValue());
            String family = fontFamily.getValue() == null || fontFamily.getValue().isBlank()
                    ? "SansSerif"
                    : fontFamily.getValue();
            String titleFamily = titleFontFamily.getValue() == null || titleFontFamily.getValue().isBlank()
                    ? family
                    : titleFontFamily.getValue();
            int size = fontSize.getValue() == null ? 54 : fontSize.getValue();
            previewTitle.setTextFill(textColor.getValue() == null ? Color.web("#20232A") : textColor.getValue());
            previewBody.setTextFill(textColor.getValue() == null ? Color.web("#20232A") : textColor.getValue());
            previewTitle.setFont(Font.font(titleFamily, Math.max(18, size * 0.46)));
            previewBody.setFont(Font.font(family, Math.max(12, size * 0.28)));
            int underlinePx = (int) Math.round(narratedUnderlineThickness.getValue());
            backgroundImageOpacityValue.setText((int) Math.round(backgroundImageOpacity.getValue()) + "%");
            String effectCss = textEffectCss(textEffect.getValue(), textEffectColor.getValue(), textEffectThickness.getValue());
            previewTitle.setStyle(effectCss);
            previewBody.setStyle(effectCss);
            narratedUnderlineThicknessValue.setText(underlinePx + " px");
            boolean showUnderline = underlineNarratedText.isSelected() && underlinePx > 0;
            previewNarratedUnderline.setVisible(showUnderline);
            previewNarratedUnderline.setManaged(showUnderline);
            previewNarratedUnderline.setMinHeight(underlinePx);
            previewNarratedUnderline.setPrefHeight(underlinePx);
            previewNarratedUnderline.setMaxHeight(underlinePx);
            Color underlineColor = narratedUnderlineColor.getValue() == null
                    ? Color.web("#4F46E5") : narratedUnderlineColor.getValue();
            previewNarratedUnderline.setBackground(new Background(new BackgroundFill(
                    underlineColor, new CornerRadii(Math.max(1, underlinePx / 2.0)),
                    javafx.geometry.Insets.EMPTY)));
            previewFrame.setBackground(new Background(new BackgroundFill(
                    backgroundColor.getValue() == null ? Color.WHITE : backgroundColor.getValue(),
                    new CornerRadii(6), javafx.geometry.Insets.EMPTY)));
            boolean imageMode = backgroundMode.getValue() == DocumentTextVideoBackgroundMode.IMAGE
                    && backgroundImage != null;
            previewBackground.setImage(null);
            previewBackground.setViewport(null);
            previewBackground.setEffect(null);
            previewBackground.setVisible(false);
            previewBackground.setManaged(false);
            previewForeground.setImage(null);
            previewForeground.setVisible(false);
            if (imageMode) {
                Image image = new Image(backgroundImage.toUri().toString(), true);
                double opacity = backgroundImageOpacity.getValue() / 100.0;
                DocumentBackgroundImageFit fit = selectedBackgroundImageFit();
                if (fit == DocumentBackgroundImageFit.COVER) {
                    showCoverPreview(image, opacity, false);
                } else if (fit == DocumentBackgroundImageFit.BLUR_AND_CONTAIN) {
                    showCoverPreview(image, opacity, true);
                    showContainedPreview(image, opacity);
                } else {
                    showContainedPreview(image, opacity);
                }
            }
            previewFrame.setBorder(frameBorder());
            previewFrame.setAccessibleText("Vista previa del frame documental con texto " + foreground + ".");
        }

        private void configureBackgroundFitChoices() {
            containBackground.setToggleGroup(backgroundImageFitGroup);
            coverBackground.setToggleGroup(backgroundImageFitGroup);
            blurredBackground.setToggleGroup(backgroundImageFitGroup);
            containBackground.setUserData(DocumentBackgroundImageFit.CONTAIN);
            coverBackground.setUserData(DocumentBackgroundImageFit.COVER);
            blurredBackground.setUserData(DocumentBackgroundImageFit.BLUR_AND_CONTAIN);
            coverBackground.setSelected(true);
            for (RadioButton choice : List.of(containBackground, coverBackground, blurredBackground)) {
                choice.disableProperty().bind(backgroundMode.valueProperty().isNotEqualTo(DocumentTextVideoBackgroundMode.IMAGE));
            }
            previewFrame.widthProperty().addListener((obs, before, after) -> {
                updateCoverViewport();
                centerPreviewForeground();
            });
            previewFrame.heightProperty().addListener((obs, before, after) -> {
                updateCoverViewport();
                centerPreviewForeground();
            });
        }

        private DocumentBackgroundImageFit selectedBackgroundImageFit() {
            Toggle selected = backgroundImageFitGroup.getSelectedToggle();
            return selected != null && selected.getUserData() instanceof DocumentBackgroundImageFit fit
                    ? fit : DocumentBackgroundImageFit.COVER;
        }

        private void showCoverPreview(Image image, double opacity, boolean blurred) {
            previewBackground.setImage(image);
            previewBackground.setOpacity(opacity);
            previewBackground.setEffect(blurred ? new javafx.scene.effect.GaussianBlur(28) : null);
            previewBackground.setVisible(true);
            image.progressProperty().addListener((obs, before, after) -> updateCoverViewport());
            updateCoverViewport();
        }

        private void showContainedPreview(Image image, double opacity) {
            previewForeground.setImage(image);
            previewForeground.setOpacity(opacity);
            previewForeground.setVisible(true);
            image.progressProperty().addListener((obs, before, after) -> centerPreviewForeground());
            centerPreviewForeground();
        }

        private void centerPreviewForeground() {
            Image image = previewForeground.getImage();
            double frameWidth = previewFrame.getWidth(), frameHeight = previewFrame.getHeight();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                    || frameWidth <= 0 || frameHeight <= 0) return;
            double scale = Math.min(frameWidth / image.getWidth(), frameHeight / image.getHeight());
            double renderedWidth = image.getWidth() * scale;
            double renderedHeight = image.getHeight() * scale;
            previewForeground.setLayoutX((frameWidth - renderedWidth) / 2.0);
            previewForeground.setLayoutY((frameHeight - renderedHeight) / 2.0);
        }

        private void updateCoverViewport() {
            Image image = previewBackground.getImage();
            double width = previewFrame.getWidth(), height = previewFrame.getHeight();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || width <= 0 || height <= 0) return;
            double imageRatio = image.getWidth() / image.getHeight();
            double frameRatio = width / height;
            if (imageRatio > frameRatio) {
                double cropWidth = image.getHeight() * frameRatio;
                previewBackground.setViewport(new javafx.geometry.Rectangle2D(
                        (image.getWidth() - cropWidth) / 2.0, 0, cropWidth, image.getHeight()));
            } else {
                double cropHeight = image.getWidth() / frameRatio;
                previewBackground.setViewport(new javafx.geometry.Rectangle2D(
                        0, (image.getHeight() - cropHeight) / 2.0, image.getWidth(), cropHeight));
            }
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

        private static HBox percentageControl(Slider slider, Label value) {
            HBox row = new HBox(10, slider, value);
            HBox.setHgrow(slider, Priority.ALWAYS);
            slider.setMaxWidth(Double.MAX_VALUE);
            return row;
        }

        private static String textEffectCss(DocumentTextEffect effect, Color color, Integer thicknessValue) {
            if (effect == null || effect == DocumentTextEffect.NONE) return "";
            int thickness = thicknessValue == null ? 3 : thicknessValue;
            String effectColor = hex(color == null ? Color.BLACK : color);
            if (effect == DocumentTextEffect.SHADOW) {
                return "-fx-effect: dropshadow(gaussian, " + effectColor + ", " + Math.max(2, thickness * 2)
                        + ", 0.35, " + thickness + ", " + thickness + ");";
            }
            return "-fx-effect: dropshadow(gaussian, " + effectColor + ", " + Math.max(2, thickness)
                    + ", 1.0, 0, 0);";
        }

        private static Border frameBorder() {
            return new Border(new BorderStroke(
                    Color.web("#D9E1F2"),
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(6),
                    BorderWidths.DEFAULT));
        }

        private static void configureFontCombo(ComboBox<String> combo) {
            com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFontControls.configure(combo);
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
