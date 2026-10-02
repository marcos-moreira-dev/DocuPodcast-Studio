package com.marcosmoreiradev.docupodcaststudio.presentation.batch;

import com.marcosmoreiradev.docupodcaststudio.application.batch.BatchSourceInventory;
import com.marcosmoreiradev.docupodcaststudio.application.batch.CreateDocumentVideoBatchProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.batch.DiscoverDocumentVideoBatchSourcesUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.batch.DocumentVideoBatchPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.batch.ManageDocumentVideoBatchQueueUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoBackgroundMode;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentBackgroundImageFit;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextEffect;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchBranding;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BrandingPlacement;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BrandingSize;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchDraft;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.JsonDocumentVideoBatchRepository;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioAccordion;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.beans.binding.Bindings;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Dedicated, persistent setup and queue surface for multi-document video projects. */
public final class DocumentVideoBatchWindow {
    private final javafx.scene.control.RadioButton videoOutput = StudioFormControls.radioButton("Video con audio");
    private final javafx.scene.control.RadioButton audioOutput = StudioFormControls.radioButton("Solo audio");
    private final ComboBox<com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat> audioFormat = StudioFormControls.comboBox();
    private boolean exitAfterCancellation;
    private boolean openingChild;
    private DocumentVideoBatchExecutionPort.BatchRunResult lastResult;
    private static final int PRODUCTION_STAGE_COUNT = 6;
    private static final String DRAFT_DESCRIPTOR_SUFFIX = ".docupodcast-express.json";
    private final Stage stage = new Stage();
    private final DiscoverDocumentVideoBatchSourcesUseCase discovery = new DiscoverDocumentVideoBatchSourcesUseCase();
    private final JsonDocumentVideoBatchRepository repository = new JsonDocumentVideoBatchRepository();
    private final ManageDocumentVideoBatchQueueUseCase queue = new ManageDocumentVideoBatchQueueUseCase(repository);
    private final CreateDocumentVideoBatchProjectUseCase creator = new CreateDocumentVideoBatchProjectUseCase(
            discovery, repository, new DocuPodcastProjectFileRepository());

    private final TextField title = StudioFormControls.textField("Serie de documentos");
    private final TextField sourceFolder = StudioFormControls.textField();
    private final TextField destinationFolder = StudioFormControls.textField();
    private final Label inventorySummary = new Label("Selecciona una carpeta para revisar DOCX y PDF.");
    private final java.util.Map<String, com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride> documentBackgrounds = new java.util.LinkedHashMap<>();
    private final ListView<String> sourceList = new ListView<>();
    private final ProgressIndicator busy = StudioFeedbackControls.progressIndicator();

    private final ComboBox<SimpleVideoResolutionPreset> resolution = StudioFormControls.comboBox();
    private final ComboBox<DocumentTextVideoBackgroundMode> backgroundMode = StudioFormControls.comboBox();
    private final TextField backgroundImageFile = StudioFormControls.textField();
    private final com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioImageBackgroundControls imageBackground =
            new com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioImageBackgroundControls();
    private final ComboBox<Integer> backgroundImageOpacity = imageBackground.opacity;
    private final ToggleGroup backgroundImageFitGroup = imageBackground.fitGroup;
    private final RadioButton containBackground = imageBackground.contain;
    private final RadioButton coverBackground = imageBackground.cover;
    private final RadioButton blurredBackground = imageBackground.blurred;
    private final ComboBox<String> fontFamily = StudioFormControls.comboBox();
    private final ComboBox<String> titleFontFamily = StudioFormControls.comboBox();
    private final Spinner<Integer> fontSize = StudioFormControls.spinner(18, 160, 54);
    private final ColorPicker backgroundColor = StudioFormControls.colorPicker(Color.WHITE);
    private final ColorPicker textColor = StudioFormControls.colorPicker(Color.web("#20232A"));
    private final ColorPicker accentColor = StudioFormControls.colorPicker(Color.web("#4F46E5"));
    private final CheckBox underline = StudioFormControls.checkBox("Subrayar el texto narrado");
    private final ColorPicker underlineColor = StudioFormControls.colorPicker(Color.web("#4F46E5"));
    private final Spinner<Integer> underlineThickness = StudioFormControls.spinner(0, 15, 4);
    private final ComboBox<DocumentTextEffect> textEffect = StudioFormControls.comboBox();
    private final ColorPicker textEffectColor = StudioFormControls.colorPicker(Color.BLACK);
    private final Spinner<Integer> textEffectThickness = StudioFormControls.spinner(1, 12, 3);
    private final Spinner<Double> imageSeconds = StudioFormControls.spinner(1.0, 60.0, 6.0, 0.5);
    private final CheckBox interpretImages = StudioFormControls.checkBox("Interpretar imágenes con el motor configurado");
    private final ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> voiceEngine = StudioFormControls.comboBox();
    private final ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> aiEngine = StudioFormControls.comboBox();

    private final CheckBox brandingEnabled = StudioFormControls.checkBox("Mostrar logo o mascota en todas las diapositivas");
    private final TextField brandingFile = StudioFormControls.textField();
    private final ComboBox<BrandingPlacement> brandingPlacement = StudioFormControls.comboBox();
    private final ComboBox<BrandingSize> brandingSize = StudioFormControls.comboBox();
    private final StackPane appearancePreview = new StackPane();
    private final Label previewAccent = new Label("Título del documento");
    private final Label previewText = new Label("Así se verá el texto narrado en el video");
    private final Region previewUnderline = new Region();
    private final ImageView previewLogo = new ImageView();
    private final ImageView previewBackground = new ImageView();
    private final ImageView previewForeground = new ImageView();
    private final Button createButton = ActionButtonFactory.primary("Crear proyecto y preparar cola");
    private final Button saveConfigurationButton = ActionButtonFactory.secondary("Guardar configuración");
    private final Button loadConfigurationButton = ActionButtonFactory.secondary("Cargar configuración");
    private final Label createRequirement = new Label();
    private final PauseTransition configurationFeedback = new PauseTransition(Duration.seconds(3));
    private final Consumer<Path> openChildProject;
    private final DocumentVideoBatchExecutionPort execution;
    private final Window hostWindow;
    private final Runnable returnToHome;
    private boolean hostLifecycleCompleted;
    private BatchSourceInventory inventory;
    private Path draftDescriptor;

    public DocumentVideoBatchWindow(Window owner) {
        this(owner, null, null, null);
    }

    public DocumentVideoBatchWindow(Window owner, Consumer<Path> openChildProject) {
        this(owner, openChildProject, null, null);
    }

    public DocumentVideoBatchWindow(Window owner, Consumer<Path> openChildProject,
                                    DocumentVideoBatchExecutionPort execution) {
        this(owner, openChildProject, execution, null);
    }

    public DocumentVideoBatchWindow(Window owner, Consumer<Path> openChildProject,
                                    DocumentVideoBatchExecutionPort execution,
                                    Runnable returnToHome) {
        this.openChildProject = openChildProject == null ? this::openPath : openChildProject;
        this.execution = execution;
        this.hostWindow = owner;
        this.returnToHome = returnToHome == null ? () -> { } : returnToHome;
        // Independent stage: hiding the editor must not hide its production queue.
        stage.initModality(Modality.NONE);
        stage.setTitle("Documentos a audio o video Express");
        stage.setResizable(true);
        loadStageIcons();
        configureControls();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: white;");
        root.setTop(header());
        VBox setup = setupContent();
        ScrollPane scroll = new ScrollPane(setup);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scroll);
        Scene scene = new Scene(root, 1180, 700);
        if (owner != null && owner.getScene() != null) scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(event -> {
            event.consume();
            if (execution != null && execution.running()) {
                cancelEntireQueue();
            } else {
                closeToHome();
            }
        });
        stage.setOnHidden(event -> restoreHostAndReturnHome());
    }

    public static void show(Window owner) {
        DocumentVideoBatchWindow window = new DocumentVideoBatchWindow(owner);
        window.showWindow();
    }

    public static void show(Window owner, Consumer<Path> openChildProject) {
        DocumentVideoBatchWindow window = new DocumentVideoBatchWindow(owner, openChildProject);
        window.showWindow();
    }

    public static void show(Window owner, Consumer<Path> openChildProject,
                            DocumentVideoBatchExecutionPort execution) {
        DocumentVideoBatchWindow window = new DocumentVideoBatchWindow(owner, openChildProject, execution);
        window.showWindow();
    }

    public static void show(Window owner, Consumer<Path> openChildProject,
                            DocumentVideoBatchExecutionPort execution, Runnable returnToHome) {
        DocumentVideoBatchWindow window = new DocumentVideoBatchWindow(
                owner, openChildProject, execution, returnToHome);
        window.showWindow();
    }

    private void showWindow() {
        stage.show();
        stage.centerOnScreen();
        if (hostWindow instanceof Stage hostStage) {
            hostStage.hide();
        }
    }

    private void viewInDocuPodcast(Path projectFile) {
        if (execution != null && execution.running()) {
            return;
        }
        openingChild = true;
        openChildProject.accept(projectFile);
        if (hostWindow instanceof Stage hostStage) {
            hostStage.setIconified(false);
            hostStage.show();
            hostStage.toFront();
        }
        stage.close();
    }

    private void restoreHostAndReturnHome() {
        if (hostLifecycleCompleted) {
            return;
        }
        hostLifecycleCompleted = true;
        if (!openingChild) returnToHome.run();
        if (hostWindow instanceof Stage hostStage) {
            hostStage.setIconified(false);
            hostStage.show();
            hostStage.toFront();
        }
    }

    private void closeToHome() {
        if (execution != null && execution.running()) return;
        try {
            if (!hostLifecycleCompleted) {
                returnToHome.run();
                hostLifecycleCompleted = true;
                if (hostWindow instanceof Stage hostStage) {
                    hostStage.setIconified(false);
                    hostStage.show();
                    hostStage.toFront();
                }
            }
            stage.close();
        } catch (RuntimeException failure) {
            showError("No se pudo guardar la cola y volver a Inicio", failure);
        }
    }

    private void configureControls() {
        var outputGroup = new javafx.scene.control.ToggleGroup();
        videoOutput.setToggleGroup(outputGroup);
        audioOutput.setToggleGroup(outputGroup);
        videoOutput.setSelected(true);
        audioFormat.setItems(FXCollections.observableArrayList(
                com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat.values()));
        audioFormat.setValue(com.marcosmoreiradev.docupodcaststudio.application.export.AudioExportFormat.MP3);
        audioOutput.selectedProperty().addListener((obs, oldValue, selected) -> {
            createButton.setText(selected ? "Crear cola de audio" : "Crear cola de video");
        });
        // Keep the native choosers, but also let users paste an exact path. This is
        // especially useful for large unattended batches and paths with deep nesting.
        sourceFolder.setEditable(true);
        destinationFolder.setEditable(true);
        brandingFile.setEditable(true);
        sourceFolder.setPromptText("Pega una ruta o usa Elegir carpeta");
        destinationFolder.setPromptText("Pega una ruta o usa Elegir ubicación");
        brandingFile.setPromptText("Pega la ruta del PNG o usa Elegir PNG");
        sourceFolder.setOnAction(event -> scanTypedSource());
        sourceFolder.textProperty().addListener((observable, before, after) -> {
            if (!Objects.equals(before, after)) {
                documentBackgrounds.clear();
                inventory = null;
                sourceList.getItems().clear();
                inventorySummary.setText("Confirma la ruta con Enter o elige la carpeta para revisar sus documentos.");
                updateCreateEnabled();
            }
        });
        sourceFolder.focusedProperty().addListener((observable, hadFocus, hasFocus) -> {
            if (hadFocus && !hasFocus && inventory == null) scanTypedSource();
        });
        destinationFolder.textProperty().addListener((observable, before, after) -> updateCreateEnabled());
        title.textProperty().addListener((observable, before, after) -> updateCreateEnabled());
        resolution.setItems(FXCollections.observableArrayList(SimpleVideoResolutionPreset.values()));
        resolution.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(SimpleVideoResolutionPreset value) { return value == null ? "" : value.label(); }
            @Override public SimpleVideoResolutionPreset fromString(String text) { return null; }
        });
        resolution.setValue(SimpleVideoResolutionPreset.defaultPreset());
        backgroundMode.setItems(FXCollections.observableArrayList(DocumentTextVideoBackgroundMode.values()));
        backgroundMode.setConverter(enumConverter(DocumentTextVideoBackgroundMode::displayName));
        backgroundMode.setValue(DocumentTextVideoBackgroundMode.SOLID_COLOR);
        backgroundImageFile.setPromptText("Pega la ruta de una imagen o usa Elegir imagen");
        backgroundImageFile.disableProperty().bind(backgroundMode.valueProperty().isNotEqualTo(DocumentTextVideoBackgroundMode.IMAGE));
        backgroundImageOpacity.disableProperty().bind(backgroundImageFile.disableProperty());
        configureBackgroundFitChoices();
        com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFontControls.configure(fontFamily);
        com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFontControls.configure(titleFontFamily);
        textEffect.setItems(FXCollections.observableArrayList(DocumentTextEffect.values()));
        textEffect.setConverter(enumConverter(DocumentTextEffect::displayName));
        textEffect.setValue(DocumentTextEffect.NONE);
        textEffectColor.disableProperty().bind(textEffect.valueProperty().isEqualTo(DocumentTextEffect.NONE));
        textEffectThickness.disableProperty().bind(textEffectColor.disableProperty());
        underline.setSelected(true);
        // The transversal Documentary compositor currently guarantees the two lower corners.
        brandingPlacement.setItems(FXCollections.observableArrayList(
                BrandingPlacement.BOTTOM_LEFT, BrandingPlacement.BOTTOM_RIGHT));
        brandingPlacement.setValue(BrandingPlacement.BOTTOM_RIGHT);
        brandingSize.setItems(FXCollections.observableArrayList(BrandingSize.values()));
        brandingSize.setConverter(enumConverter(BrandingSize::displayName));
        brandingSize.setValue(BrandingSize.MEDIUM);
        brandingFile.disableProperty().bind(brandingEnabled.selectedProperty().not());
        brandingPlacement.disableProperty().bind(brandingEnabled.selectedProperty().not());
        brandingSize.disableProperty().bind(brandingEnabled.selectedProperty().not());
        configureEngineChoices();
        aiEngine.disableProperty().bind(interpretImages.selectedProperty().not());
        installPreviewListeners();
        refreshAppearancePreview();
        sourceList.setPrefHeight(290);
        sourceList.getSelectionModel().selectedItemProperty().addListener((o, before, after) -> refreshAppearancePreview());
        sourceList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty); setText(null); setGraphic(null);
                if (!empty && path != null) setGraphic(documentBackgroundRow(path));
            }
        });
        busy.setVisible(false); busy.setManaged(false); busy.setPrefSize(28, 28);
        createButton.setDisable(true);
        createButton.setOnAction(event -> createProject());
        saveConfigurationButton.setOnAction(event -> saveDraft());
        loadConfigurationButton.setOnAction(event -> loadDraft());
        createRequirement.setWrapText(true);
        createRequirement.setStyle("-fx-text-fill: #64748B;");
        configurationFeedback.setOnFinished(event -> updateCreateEnabled());
        updateCreateEnabled();
    }

    private VBox header() {
        Label heading = new Label("Documentos a audio o video Express"); heading.getStyleClass().add("settings-title");
        Label copy = new Label("Procesa una carpeta de Word/PDF y exporta un archivo de audio o video por documento. Los originales no se modifican.");
        copy.setWrapText(true);
        Button open = ActionButtonFactory.secondary("Abrir proyecto por lotes", this::openExisting);
        HBox line = new HBox(12, heading, spacer(), open); line.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, line, copy);
    }

    private VBox setupContent() {
        VBox content = new VBox(14);
        content.setMinWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setPadding(new Insets(16, 0, 8, 0));
        TitledPane source = sourcePane();
        TitledPane video = videoPane();
        TitledPane branding = brandingPane();
        TitledPane engines = enginesPane();
        TitledPane preview = previewPane();
        source.setExpanded(true);
        for (var pane : new TitledPane[]{video, branding, preview}) {
            pane.visibleProperty().bind(audioOutput.selectedProperty().not());
            pane.managedProperty().bind(pane.visibleProperty());
        }
        Label outputTitle = new Label("Salida del lote");
        outputTitle.getStyleClass().add("settings-title");
        var choices = new javafx.scene.layout.FlowPane(16, 8, videoOutput, audioOutput);
        Label formatLabel = new Label("Formato de audio");
        formatLabel.setLabelFor(audioFormat);
        var formatRow = new HBox(12, formatLabel, audioFormat);
        formatRow.visibleProperty().bind(audioOutput.selectedProperty());
        formatRow.managedProperty().bind(formatRow.visibleProperty());
        VBox output = new VBox(9, outputTitle, choices, formatRow,
                new Label("Se aplica a todos los documentos. Se exporta un archivo por documento."));
        Label semanticNote = new Label("Opcional: añadir a la narración las descripciones de imágenes del documento.");
        semanticNote.setWrapText(true);
        VBox semantics = new VBox(6, interpretImages, semanticNote);
        VBox outputBlock = new VBox(14, output, semantics);
        outputBlock.setPadding(new Insets(14, 16, 14, 16));
        outputBlock.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; "
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 8;");
        content.getChildren().addAll(source, outputBlock, video, branding, engines, preview, footer());
        return content;
    }

    private TitledPane sourcePane() {
        GridPane grid = formGrid();
        Button chooseSource = ActionButtonFactory.secondary("Elegir carpeta", this::chooseSource);
        Button chooseDestination = ActionButtonFactory.secondary("Elegir ubicación", this::chooseDestination);
        addRow(grid, 0, "Nombre del proyecto", title, null);
        addRow(grid, 1, "Carpeta de documentos", sourceFolder, chooseSource);
        addRow(grid, 2, "Crear proyecto en", destinationFolder, chooseDestination);
        VBox box = new VBox(10, grid, inventorySummary, sourceList);
        return StudioAccordion.pane("1. Documentos y destino", box);
    }

    private javafx.scene.Node documentBackgroundRow(String path) {
        var custom = documentBackgrounds.get(path);
        Label name = new Label(path);
        name.setGraphic(com.marcosmoreiradev.docupodcaststudio.presentation.components.LucideIconView.of("file-text"));
        name.setWrapText(true); name.setMinWidth(0); name.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);
        Button choose = ActionButtonFactory.secondary(custom == null ? "Imagen personalizada…" : "Cambiar imagen", () -> {
            FileChooser chooser = NativeSourceChooser.fileChooser();
            chooser.setTitle("Imagen personalizada para " + path);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                documentBackgrounds.put(path, new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride(
                        file.getAbsolutePath(), documentBackgrounds.get(path) == null ? null : documentBackgrounds.get(path).visibility()));
                sourceList.refresh(); refreshAppearancePreview();
            }
        });
        choose.disableProperty().bind(audioOutput.selectedProperty());
        HBox heading = new HBox(8, name, choose); heading.setAlignment(Pos.CENTER_LEFT);
        VBox row = new VBox(4, heading);
        row.prefWidthProperty().bind(sourceList.widthProperty().subtract(40));
        row.setMinWidth(0);
        if (custom == null) row.getChildren().add(new Label("Usa fondo común"));
        else {
            Path image = pathOrNull(custom.imagePath());
            boolean available = image != null && java.nio.file.Files.isRegularFile(image);
            Label state = new Label(available ? "Fondo personalizado · " + image.getFileName() : "Imagen personalizada no disponible");
            state.setWrapText(true);
            ComboBox<Integer> visibility = StudioFormControls.comboBox();
            visibility.getItems().setAll(-1, 10, 20, 35, 50, 60, 65, 80, 100);
            visibility.setConverter(enumConverter(v -> v == -1 ? "Usar visibilidad común" : v + "%"));
            int value = custom.visibility() == null ? -1 : (int)Math.round(custom.visibility()*100);
            if (!visibility.getItems().contains(value)) visibility.getItems().add(value);
            visibility.setValue(value);
            visibility.valueProperty().addListener((obs, before, after) -> { if (after == null) return; documentBackgrounds.put(path,
                    new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride(custom.imagePath(),
                            visibility.getValue() == -1 ? null : visibility.getValue()/100.0)); refreshAppearancePreview(); });
            visibility.disableProperty().bind(audioOutput.selectedProperty());
            Button remove = ActionButtonFactory.secondary("Quitar personalización", () -> { documentBackgrounds.remove(path); sourceList.refresh(); refreshAppearancePreview(); });
            remove.disableProperty().bind(audioOutput.selectedProperty());
            var controls = new javafx.scene.layout.FlowPane(8, 4, state, visibility, remove);
            row.getChildren().add(controls);
        }
        return row;
    }

    private TitledPane videoPane() {
        GridPane grid = formGrid();
        Button chooseBackground = ActionButtonFactory.secondary("Elegir imagen", this::chooseBackgroundImage);
        chooseBackground.disableProperty().bind(backgroundImageFile.disableProperty());
        addRow(grid, 0, "Resolución", resolution, null);
        addRow(grid, 1, "Tipo de fondo", backgroundMode, null);
        addRow(grid, 2, "Color base", backgroundColor, null);
        addRow(grid, 3, "Imagen de fondo", backgroundImageFile, chooseBackground);
        addRow(grid, 4, "Visibilidad de la imagen", backgroundImageOpacity, null);
        addRow(grid, 5, "Ajuste de imagen (aplica a imágenes primarias, secundarias y globales)", new VBox(5,
                containBackground, coverBackground, blurredBackground), null);
        addRow(grid, 6, "Fuente del cuerpo", fontFamily, null);
        addRow(grid, 7, "Fuente de títulos y subtítulos", titleFontFamily, null);
        addRow(grid, 8, "Tamaño de texto", fontSize, null);
        addRow(grid, 9, "Texto", textColor, null);
        addRow(grid, 10, "Efecto del texto", textEffect, null);
        addRow(grid, 11, "Color del efecto", textEffectColor, null);
        addRow(grid, 12, "Grosor del efecto", textEffectThickness, null);
        addRow(grid, 13, "Color de títulos y elementos secundarios", accentColor, null);
        addRow(grid, 14, "Énfasis", underline, null);
        addRow(grid, 15, "Color del subrayado", underlineColor, null);
        addRow(grid, 16, "Grosor del subrayado", underlineThickness, null);
        addRow(grid, 17, "Duración base de imágenes (s)", imageSeconds, null);
        Label backgroundExplanation = new Label(
                "Con «Imagen de fondo», el color base llena el lienzo y la imagen se coloca encima con la visibilidad elegida.");
        backgroundExplanation.setWrapText(true);
        backgroundExplanation.visibleProperty().bind(
                backgroundMode.valueProperty().isEqualTo(DocumentTextVideoBackgroundMode.IMAGE));
        backgroundExplanation.managedProperty().bind(backgroundExplanation.visibleProperty());
        VBox content = new VBox(10, grid, backgroundExplanation);
        return StudioAccordion.pane("2. Apariencia común del video", content);
    }

    private TitledPane brandingPane() {
        GridPane grid = formGrid();
        Button choose = ActionButtonFactory.secondary("Elegir PNG", this::chooseBranding);
        addRow(grid, 0, "Uso", brandingEnabled, null); addRow(grid, 1, "Archivo", brandingFile, choose);
        addRow(grid, 2, "Lado", brandingPlacement, null);
        addRow(grid, 3, "Tamaño de la mascota", brandingSize, null);
        Label note = new Label("Se reutiliza el compositor documental: la mascota se mantiene en una esquina inferior sin convertirse en diapositiva ni texto narrable.");
        note.setWrapText(true);
        return StudioAccordion.pane("3. Logo o mascota opcional", new VBox(8, grid, note));
    }

    private TitledPane enginesPane() {
        GridPane grid = formGrid();
        addRow(grid, 0, "Motor de voz", voiceEngine, null);
        addRow(grid, 1, "Motor de IA", aiEngine, null);
        Label note = new Label("Estas selecciones se guardan en el proyecto y se aplican a todos los documentos del lote. "
                + "El motor de IA se usa al interpretar imágenes.");
        note.setWrapText(true);
        return StudioAccordion.pane("4. Configuración avanzada de motores", new VBox(9, grid, note));
    }

    private TitledPane previewPane() {
        previewAccent.setStyle("-fx-font-weight: 700;");
        previewText.setWrapText(true);
        previewText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        previewText.setAlignment(Pos.CENTER);
        previewText.setMaxWidth(620);
        previewUnderline.setMaxWidth(360);
        previewUnderline.setPrefHeight(4);
        VBox copy = new VBox(18, previewAccent, previewText, previewUnderline);
        copy.setAlignment(Pos.CENTER);

        previewLogo.setPreserveRatio(true);
        previewLogo.setMouseTransparent(true);
        previewBackground.setPreserveRatio(false);
        // The background follows the preview size, so it must not also participate in
        // calculating that size. Otherwise JavaFX creates a layout feedback loop that
        // makes the whole setup surface grow horizontally on every pulse.
        previewBackground.setManaged(false);
        previewBackground.fitWidthProperty().bind(appearancePreview.widthProperty());
        previewBackground.fitHeightProperty().bind(appearancePreview.heightProperty());
        previewBackground.setMouseTransparent(true);
        previewForeground.setManaged(false);
        previewForeground.setPreserveRatio(true);
        previewForeground.fitWidthProperty().bind(appearancePreview.widthProperty());
        previewForeground.fitHeightProperty().bind(appearancePreview.heightProperty());
        previewForeground.setMouseTransparent(true);
        appearancePreview.getChildren().setAll(previewBackground, previewForeground, copy, previewLogo);
        appearancePreview.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            SimpleVideoResolutionPreset preset = resolution.getValue() == null
                    ? SimpleVideoResolutionPreset.defaultPreset() : resolution.getValue();
            double width = appearancePreview.getWidth();
            return width > 0 ? width * preset.height() / (double) preset.width() : 360.0;
        }, appearancePreview.widthProperty(), resolution.valueProperty()));
        appearancePreview.minHeightProperty().bind(appearancePreview.prefHeightProperty());
        appearancePreview.setMaxWidth(Double.MAX_VALUE);
        appearancePreview.setPadding(new Insets(26));

        Label note = new Label("Los textos son ejemplos de presentación y no se añaden al video. "
                + "La muestra cambia con la fuente, el tamaño, los colores, el subrayado y el PNG configurado.");
        note.setWrapText(true);
        TitledPane pane = StudioAccordion.pane("5. Previsualización", new VBox(9, note, appearancePreview));
        pane.setExpanded(true);
        return pane;
    }

    private void installPreviewListeners() {
        fontFamily.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        fontFamily.getEditor().textProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        titleFontFamily.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        titleFontFamily.getEditor().textProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        fontSize.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        backgroundColor.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        backgroundMode.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        backgroundImageFile.textProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        backgroundImageOpacity.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        backgroundImageFitGroup.selectedToggleProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        textColor.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        textEffect.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        textEffectColor.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        textEffectThickness.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        accentColor.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        underline.selectedProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        underlineColor.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        underlineThickness.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        brandingEnabled.selectedProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        brandingFile.textProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        brandingPlacement.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
        brandingSize.valueProperty().addListener((obs, before, after) -> refreshAppearancePreview());
    }

    private void configureEngineChoices() {
        DocumentVideoBatchExecutionPort.EngineConfiguration configuration = execution == null
                ? DocumentVideoBatchExecutionPort.EngineConfiguration.defaults()
                : execution.engineConfiguration();
        voiceEngine.setItems(FXCollections.observableArrayList(configuration.voiceEngines()));
        aiEngine.setItems(FXCollections.observableArrayList(configuration.aiEngines()));
        configureEngineCombo(voiceEngine);
        configureEngineCombo(aiEngine);
        selectEngine(voiceEngine, configuration.selectedVoiceEngineId());
        selectEngine(aiEngine, configuration.selectedAiEngineId());
        voiceEngine.valueProperty().addListener((obs, previous, selected) -> {
            if (execution == null || selected == null || selected.id().isBlank() || !selected.available()) return;
            try {
                execution.selectGlobalVoiceEngine(selected.id());
            } catch (IOException failure) {
                showError("No se pudo guardar el motor de voz para DocuPodcast", failure);
                if (previous != null) voiceEngine.setValue(previous);
            }
        });
    }

    private static void configureEngineCombo(ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> combo) {
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setCellFactory(ignored -> engineChoiceCell());
        combo.setButtonCell(engineChoiceCell());
    }

    private static ListCell<DocumentVideoBatchExecutionPort.EngineChoice> engineChoiceCell() {
        return new ListCell<>() {
            @Override protected void updateItem(DocumentVideoBatchExecutionPort.EngineChoice item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
                setDisable(item != null && !item.available());
                setOpacity(item != null && !item.available() ? 0.58 : 1.0);
            }
        };
    }

    private static void selectEngine(ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> combo, String id) {
        String requested = id == null ? "" : id.strip();
        DocumentVideoBatchExecutionPort.EngineChoice selected = combo.getItems().stream()
                .filter(choice -> choice.id().equalsIgnoreCase(requested) && choice.available())
                .findFirst()
                .orElseGet(() -> combo.getItems().stream().filter(DocumentVideoBatchExecutionPort.EngineChoice::available)
                        .findFirst().orElse(null));
        combo.setValue(selected);
    }

    private void refreshAppearancePreview() {
        Color background = backgroundColor.getValue() == null ? Color.WHITE : backgroundColor.getValue();
        Color text = textColor.getValue() == null ? Color.web("#20232A") : textColor.getValue();
        Color accent = accentColor.getValue() == null ? Color.web("#4F46E5") : accentColor.getValue();
        Color line = underlineColor.getValue() == null ? accent : underlineColor.getValue();
        String family = selectedFontFamily();
        String titleFamily = selectedTitleFontFamily();
        int configuredSize = fontSize.getValue() == null ? 54 : fontSize.getValue();
        double previewSize = Math.max(14.0, Math.min(68.0, configuredSize * 0.72));

        appearancePreview.setStyle("-fx-background-color: " + hex(background)
                + "; -fx-background-radius: 10; -fx-border-color: #D9E1F2; -fx-border-radius: 10;");
        previewAccent.setStyle("-fx-font-family: '" + cssFont(titleFamily) + "'; -fx-font-size: 15px;"
                + " -fx-font-weight: 700; -fx-text-fill: " + hex(accent) + ";");
        previewText.setStyle("-fx-font-family: '" + cssFont(family) + "'; -fx-font-size: "
                + String.format(java.util.Locale.ROOT, "%.2f", previewSize) + "px; -fx-text-fill: "
                + hex(text) + ";" + previewTextEffectCss());
        previewUnderline.setVisible(underline.isSelected() && underlineThickness.getValue() > 0);
        previewUnderline.setManaged(previewUnderline.isVisible());
        previewUnderline.setPrefHeight(Math.max(1, underlineThickness.getValue()));
        previewUnderline.setStyle("-fx-background-color: " + hex(line) + "; -fx-background-radius: 99;");
        refreshPreviewBackground();
        refreshPreviewLogo();
    }

    private void refreshPreviewBackground() {
        previewBackground.setImage(null);
        previewBackground.setViewport(null);
        previewBackground.setEffect(null);
        previewBackground.setVisible(false);
        previewForeground.setImage(null);
        previewForeground.setVisible(false);
        var custom = documentBackgrounds.get(sourceList.getSelectionModel().getSelectedItem());
        String imagePath = custom == null ? backgroundImageFile.getText() : custom.imagePath();
        if ((custom == null && backgroundMode.getValue() != DocumentTextVideoBackgroundMode.IMAGE) || imagePath.isBlank()) return;
        try {
            Path image = Path.of(imagePath).toAbsolutePath().normalize();
            if (!java.nio.file.Files.isRegularFile(image)) return;
            Image loaded = new Image(image.toUri().toString(), true);
            double opacity = (backgroundImageOpacity.getValue() == null ? 35 : backgroundImageOpacity.getValue()) / 100.0;
            if (custom != null && custom.visibility() != null) opacity = custom.visibility();
            DocumentBackgroundImageFit fit = selectedBackgroundImageFit();
            if (fit == DocumentBackgroundImageFit.COVER) {
                showCoverPreview(loaded, opacity, false);
            } else if (fit == DocumentBackgroundImageFit.BLUR_AND_CONTAIN) {
                showCoverPreview(loaded, opacity, true);
                showContainedPreview(loaded, opacity);
            } else {
                showContainedPreview(loaded, opacity);
            }
        } catch (RuntimeException ignored) {
            // Incomplete pasted paths are ignored until they become valid.
        }
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
        double frameWidth = appearancePreview.getWidth(), frameHeight = appearancePreview.getHeight();
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
        double width = appearancePreview.getWidth(), height = appearancePreview.getHeight();
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

    private void configureBackgroundFitChoices() {
        appearancePreview.widthProperty().addListener((obs, before, after) -> {
            updateCoverViewport();
            centerPreviewForeground();
            updatePreviewLogoSize();
        });
        appearancePreview.heightProperty().addListener((obs, before, after) -> {
            updateCoverViewport();
            centerPreviewForeground();
            updatePreviewLogoSize();
        });
    }

    private DocumentBackgroundImageFit selectedBackgroundImageFit() {
        Toggle selected = backgroundImageFitGroup.getSelectedToggle();
        return selected != null && selected.getUserData() instanceof DocumentBackgroundImageFit fit
                ? fit : DocumentBackgroundImageFit.COVER;
    }

    private String previewTextEffectCss() {
        DocumentTextEffect effect = textEffect.getValue() == null ? DocumentTextEffect.NONE : textEffect.getValue();
        if (effect == DocumentTextEffect.NONE) return "";
        Color color = textEffectColor.getValue() == null ? Color.BLACK : textEffectColor.getValue();
        int thickness = textEffectThickness.getValue() == null ? 3 : textEffectThickness.getValue();
        if (effect == DocumentTextEffect.SHADOW) {
            return " -fx-effect: dropshadow(gaussian, " + hex(color) + ", " + Math.max(2, thickness * 2)
                    + ", 0.35, " + thickness + ", " + thickness + ");";
        }
        return " -fx-effect: dropshadow(gaussian, " + hex(color) + ", " + Math.max(2, thickness)
                + ", 1.0, 0, 0);";
    }

    private void refreshPreviewLogo() {
        previewLogo.setImage(null);
        previewLogo.setVisible(false);
        previewLogo.setManaged(false);
        if (!brandingEnabled.isSelected() || brandingFile.getText().isBlank()) return;
        try {
            Path logo = Path.of(brandingFile.getText()).toAbsolutePath().normalize();
            if (!java.nio.file.Files.isRegularFile(logo)) return;
            previewLogo.setImage(new Image(logo.toUri().toString(), true));
            updatePreviewLogoSize();
            previewLogo.setVisible(true);
            previewLogo.setManaged(true);
            Pos alignment = brandingPlacement.getValue() == BrandingPlacement.BOTTOM_LEFT
                    ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT;
            StackPane.setAlignment(previewLogo, alignment);
            StackPane.setMargin(previewLogo, new Insets(12));
        } catch (RuntimeException ignored) {
            // An incomplete pasted path simply leaves the preview without a logo.
        }
    }

    private void updatePreviewLogoSize() {
        BrandingSize selected = brandingSize.getValue() == null ? BrandingSize.MEDIUM : brandingSize.getValue();
        double frameWidth = appearancePreview.getWidth();
        double frameHeight = appearancePreview.getHeight();
        double reference = frameWidth > 0 ? frameWidth : 900.0;
        double pixels = reference * selected.percent() / 100.0;
        if (frameHeight > 0) pixels = Math.min(pixels, frameHeight * 0.64);
        pixels = Math.max(48.0, pixels);
        previewLogo.setFitWidth(pixels);
        previewLogo.setFitHeight(pixels);
    }

    private static String cssFont(String family) {
        return family.replace("\\", "").replace("'", "").replace("\"", "");
    }

    private String selectedFontFamily() {
        String family = fontFamily.getValue();
        return family == null || family.isBlank() ? Font.getDefault().getFamily() : family;
    }

    private String selectedTitleFontFamily() {
        String family = titleFontFamily.getValue();
        return family == null || family.isBlank() ? selectedFontFamily() : family;
    }

    private static <T> javafx.util.StringConverter<T> enumConverter(java.util.function.Function<T, String> label) {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : label.apply(value); }
            @Override public T fromString(String value) { return null; }
        };
    }

    private static javafx.util.StringConverter<Integer> percentConverter() {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(Integer value) { return value == null ? "" : value + "%"; }
            @Override public Integer fromString(String value) { return null; }
        };
    }

    private void chooseBackgroundImage() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Elegir imagen de fondo documental");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Imágenes (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) backgroundImageFile.setText(selected.getAbsolutePath());
    }

    private VBox footer() {
        Label safety = new Label("Se copiarán únicamente DOCX/PDF y el logo elegido. Los demás archivos se ignoran.");
        safety.setWrapText(true);
        HBox actions = new HBox(9, loadConfigurationButton, saveConfigurationButton, busy, createButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        createRequirement.setAlignment(Pos.CENTER_RIGHT);
        createRequirement.setMaxWidth(Double.MAX_VALUE);
        VBox footer = new VBox(6, safety, createRequirement, actions);
        footer.setAlignment(Pos.CENTER_RIGHT);
        return footer;
    }

    private void saveDraft() {
        Path target = draftDescriptor;
        boolean updating = target != null && java.nio.file.Files.isRegularFile(target);
        if (target == null || !java.nio.file.Files.isRegularFile(target)) {
            FileChooser chooser = NativeSourceChooser.fileChooser();
            chooser.setTitle("Guardar configuración de Video Express");
            chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter(
                    "Configuración de Video Express (*" + DRAFT_DESCRIPTOR_SUFFIX + ")",
                    "*" + DRAFT_DESCRIPTOR_SUFFIX));
            chooser.setInitialFileName(safeFileName(title.getText()) + DRAFT_DESCRIPTOR_SUFFIX);
            File selected = chooser.showSaveDialog(stage);
            if (selected == null) return;
            target = ensureDraftSuffix(selected.toPath());
        }
        try {
            repository.saveDraft(new DocumentVideoBatchDraft(title.getText(), sourceFolder.getText(),
                    destinationFolder.getText(), profile()), target);
            draftDescriptor = target.toAbsolutePath().normalize();
            saveConfigurationButton.setText("Actualizar configuración");
            showConfigurationSavedFeedback(updating
                    ? "Configuración actualizada correctamente."
                    : "Configuración guardada correctamente.");
        } catch (IOException failure) {
            showError("No se pudo guardar la configuración", failure);
        }
    }

    private void showConfigurationSavedFeedback(String message) {
        configurationFeedback.stop();
        createRequirement.setText(message);
        createRequirement.setStyle("-fx-text-fill: #15803D; -fx-font-weight: 700;");
        configurationFeedback.playFromStart();
    }

    private void loadDraft() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Cargar configuración de Video Express");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter(
                "Configuración de Video Express (*" + DRAFT_DESCRIPTOR_SUFFIX + ")",
                "*" + DRAFT_DESCRIPTOR_SUFFIX));
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;
        try {
            Path selectedPath = selected.toPath().toAbsolutePath().normalize();
            DocumentVideoBatchDraft draft = repository.openDraft(selectedPath);
            draftDescriptor = selectedPath;
            saveConfigurationButton.setText("Actualizar configuración");
            applyDraft(draft);
        } catch (IOException | RuntimeException failure) {
            showError("No se pudo cargar la configuración", failure);
        }
    }

    private void applyDraft(DocumentVideoBatchDraft draft) {
        title.setText(draft.title());
        sourceFolder.setText(draft.sourceRoot());
        destinationFolder.setText(draft.destinationRoot());
        applyProfile(draft.profile());
        Path source = pathOrNull(draft.sourceRoot());
        if (source != null && java.nio.file.Files.isDirectory(source)) scan(source);
        else {
            inventory = null;
            inventorySummary.setText("La configuración se cargó. Selecciona una carpeta válida con DOCX o PDF.");
            sourceList.getItems().clear();
            updateCreateEnabled();
        }
    }

    private void applyProfile(DocumentVideoBatchProfile loaded) {
        documentBackgrounds.clear(); documentBackgrounds.putAll(loaded.documentBackgrounds());
        sourceList.refresh();
        DocumentTextVideoOptions video = loaded.video();
        if (loaded.audioOnly()) audioOutput.setSelected(true); else videoOutput.setSelected(true);
        audioFormat.setValue(loaded.audioFormat());
        resolution.setValue(video.resolution());
        backgroundMode.setValue(video.backgroundMode());
        backgroundColor.setValue(colorOr(video.backgroundColor(), Color.WHITE));
        backgroundImageFile.setText(video.backgroundImagePath());
        backgroundImageOpacity.setValue((int) Math.round(video.backgroundImageOpacity() * 100));
        backgroundImageFitGroup.getToggles().stream()
                .filter(toggle -> toggle.getUserData() == video.backgroundImageFit()).findFirst()
                .ifPresent(toggle -> toggle.setSelected(true));
        fontFamily.setValue(video.fontFamily());
        titleFontFamily.setValue(video.titleFontFamily());
        fontSize.getValueFactory().setValue(video.fontSize());
        textColor.setValue(colorOr(video.textColor(), Color.web("#20232A")));
        accentColor.setValue(colorOr(video.accentColor(), Color.web("#4F46E5")));
        underline.setSelected(video.underlineNarratedText());
        underlineColor.setValue(colorOr(video.narratedUnderlineColor(), Color.web("#4F46E5")));
        underlineThickness.getValueFactory().setValue(video.narratedUnderlineThicknessPx());
        textEffect.setValue(video.textEffect());
        textEffectColor.setValue(colorOr(video.textEffectColor(), Color.BLACK));
        textEffectThickness.getValueFactory().setValue(video.textEffectThicknessPx());
        imageSeconds.getValueFactory().setValue(loaded.imageSlideSeconds());
        interpretImages.setSelected(loaded.interpretImages());
        brandingEnabled.setSelected(loaded.branding().enabled());
        brandingFile.setText(loaded.branding().sourcePath());
        brandingPlacement.setValue(loaded.branding().placement());
        brandingSize.setValue(BrandingSize.fromPercent(loaded.branding().sizePercent()));
        selectEngine(voiceEngine, loaded.voiceEngineId());
        selectEngine(aiEngine, loaded.aiEngineId());
        refreshAppearancePreview();
    }

    private static Path ensureDraftSuffix(Path selected) {
        String value = selected.toString();
        return value.toLowerCase(java.util.Locale.ROOT).endsWith(DRAFT_DESCRIPTOR_SUFFIX)
                ? selected : Path.of(value + DRAFT_DESCRIPTOR_SUFFIX);
    }

    private static String safeFileName(String value) {
        String safe = value == null ? "configuracion-video-express"
                : value.strip().replaceAll("[\\\\/:*?\"<>|]+", "-");
        return safe.isBlank() ? "configuracion-video-express" : safe;
    }

    private static Path pathOrNull(String value) {
        try { return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize(); }
        catch (RuntimeException invalid) { return null; }
    }

    private static Color colorOr(String value, Color fallback) {
        try { return Color.web(value); } catch (RuntimeException invalid) { return fallback; }
    }

    private void chooseSource() {
        var chooser = NativeSourceChooser.directoryChooser(); chooser.setTitle("Carpeta con documentos DOCX o PDF");
        File selected = chooser.showDialog(stage); if (selected == null) return;
        sourceFolder.setText(selected.getAbsolutePath()); scan(Path.of(selected.getAbsolutePath()));
    }

    private void chooseDestination() {
        var chooser = NativeSourceChooser.directoryChooser(); chooser.setTitle("Ubicación del proyecto por lotes");
        File selected = chooser.showDialog(stage); if (selected != null) destinationFolder.setText(selected.getAbsolutePath());
        updateCreateEnabled();
    }

    private void chooseBranding() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Elegir logo o mascota");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG, JPG o WebP", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File selected = chooser.showOpenDialog(stage); if (selected != null) brandingFile.setText(selected.getAbsolutePath());
    }

    private void scanTypedSource() {
        String value = sourceFolder.getText();
        if (value == null || value.isBlank()) return;
        try {
            scan(Path.of(value.strip()));
        } catch (RuntimeException invalidPath) {
            showError("La ruta de documentos no es válida", invalidPath);
        }
    }

    private void scan(Path root) {
        setBusy(true); inventorySummary.setText("Revisando documentos…"); sourceList.getItems().clear();
        Task<BatchSourceInventory> task = new Task<>() { @Override protected BatchSourceInventory call() throws Exception { return discovery.discover(root); } };
        task.setOnSucceeded(event -> {
            inventory = task.getValue();
            inventorySummary.setText(inventory.documents().size() + " documento(s), " + inventory.embeddedMediaCount()
                    + " imagen(es) incrustada(s), " + inventory.ignoredFileCount() + " archivo(s) ajeno(s) ignorado(s).");
            sourceList.setItems(FXCollections.observableArrayList(inventory.documents().stream()
                    .map(item -> item.relativePath()).toList()));
            setBusy(false); updateCreateEnabled();
        });
        task.setOnFailed(event -> { setBusy(false); showError("No se pudo revisar la carpeta", task.getException()); });
        new Thread(task, "docupodcast-batch-discovery").start();
    }

    private void createProject() {
        if (!audioOutput.isSelected() && backgroundMode.getValue() == DocumentTextVideoBackgroundMode.IMAGE
                && backgroundImageFile.getText().isBlank()) {
            showError("Falta la imagen de fondo",
                    new IllegalArgumentException("Elige una imagen o cambia el tipo de fondo a color."));
            return;
        }
        if (!audioOutput.isSelected() && brandingEnabled.isSelected() && brandingFile.getText().isBlank()) {
            showError("Falta el logo o mascota", new IllegalArgumentException("Elige el archivo o desactiva la opción.")); return;
        }
        setBusy(true);
        DocumentVideoBatchProfile profile = profile();
        Task<CreateDocumentVideoBatchProjectUseCase.CreatedBatch> task = new Task<>() {
            @Override protected CreateDocumentVideoBatchProjectUseCase.CreatedBatch call() throws Exception {
                return creator.create(title.getText(), Path.of(sourceFolder.getText()), Path.of(destinationFolder.getText()), profile);
            }
        };
        task.setOnSucceeded(event -> { setBusy(false); showProject(task.getValue().project(), task.getValue().descriptor()); });
        task.setOnFailed(event -> { setBusy(false); showError("No se pudo crear el proyecto", task.getException()); });
        new Thread(task, "docupodcast-batch-create").start();
    }

    private DocumentVideoBatchProfile profile() {
        DocumentTextVideoOptions video = new DocumentTextVideoOptions(resolution.getValue(),
                backgroundMode.getValue(), hex(backgroundColor.getValue()), backgroundImageFile.getText(), hex(textColor.getValue()),
                hex(accentColor.getValue()), selectedFontFamily(), selectedTitleFontFamily(),
                fontSize.getValue(), underline.isSelected(),
                hex(underlineColor.getValue()), underlineThickness.getValue(),
                (backgroundImageOpacity.getValue() == null ? 35 : backgroundImageOpacity.getValue()) / 100.0,
                selectedBackgroundImageFit(),
                textEffect.getValue(), hex(textEffectColor.getValue()), textEffectThickness.getValue());
        BatchBranding branding = new BatchBranding(brandingEnabled.isSelected(), brandingFile.getText(), "",
                brandingPlacement.getValue(),
                (brandingSize.getValue() == null ? BrandingSize.MEDIUM : brandingSize.getValue()).percent(), 1.0);
        return new DocumentVideoBatchProfile(video, imageSeconds.getValue(),
                interpretImages.isSelected(), branding,
                audioOutput.isSelected() ? com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchOutputKind.AUDIO
                        : com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchOutputKind.VIDEO, audioFormat.getValue(),
                selectedEngineId(voiceEngine), selectedEngineId(aiEngine), documentBackgrounds);
    }

    private static String selectedEngineId(ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> combo) {
        var value = combo.getValue();
        return value == null || !value.available() ? "" : value.id();
    }

    private void openExisting() {
        FileChooser chooser = NativeSourceChooser.fileChooser();
        chooser.setTitle("Abrir proyecto o configuración de Video Express");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Video Express: proyectos y configuraciones",
                        "*" + DocumentVideoBatchPathPolicy.DESCRIPTOR_SUFFIX, "*" + DRAFT_DESCRIPTOR_SUFFIX),
                new FileChooser.ExtensionFilter("Proyecto por lotes DocuPodcast",
                        "*" + DocumentVideoBatchPathPolicy.DESCRIPTOR_SUFFIX),
                new FileChooser.ExtensionFilter("Configuración previa de Video Express",
                        "*" + DRAFT_DESCRIPTOR_SUFFIX));
        File file = chooser.showOpenDialog(stage); if (file == null) return;
        Path selected = file.toPath().toAbsolutePath().normalize();
        try {
            if (selected.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                    .endsWith(DRAFT_DESCRIPTOR_SUFFIX)) {
                DocumentVideoBatchDraft draft = repository.openDraft(selected);
                draftDescriptor = selected;
                saveConfigurationButton.setText("Actualizar configuración");
                applyDraft(draft);
            } else {
                showProject(queue.recoverInterrupted(repository.open(selected), selected), selected);
            }
        }
        catch (IOException ex) { showError("No se pudo abrir el proyecto", ex); }
    }

    private void showProject(DocumentVideoBatchProject project, Path descriptor) {
        showProject(project, descriptor, true);
    }

    private void queueAction(DocumentVideoBatchProject project, Path descriptor, DocumentVideoBatchItem item, String action) {
        try {
            DocumentVideoBatchProject latest = repository.open(descriptor);
            DocumentVideoBatchProject updated = switch (action) {
                case "pause" -> queue.pause(latest, descriptor, item.id());
                case "resume" -> queue.resume(latest, descriptor, item.id());
                case "skip" -> queue.skip(latest, descriptor, item.id());
                case "retry" -> queue.retry(latest, descriptor, item.id());
                case "up" -> queue.move(latest, descriptor, item.id(), -1);
                case "down" -> queue.move(latest, descriptor, item.id(), 1);
                default -> latest;
            };
            showProject(updated, descriptor, false);
        } catch (IOException ex) {
            showError("No se pudo actualizar la cola", ex);
        }
    }

    private void showProject(DocumentVideoBatchProject project, Path descriptor, boolean notify) {
        VBox rows = new VBox(7);
        int total = project.items().size();
        for (DocumentVideoBatchItem item : project.items()) {
            Label order = new Label(String.format("%02d", item.order() + 1));
            Label titleLabel = new Label(item.title());
            titleLabel.setStyle("-fx-font-weight: 700;");
            String stageContext = item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.RUNNING
                    ? "Documento " + (item.order() + 1) + " de " + total + " · Etapa "
                    + stageNumber(item.stage()) + " de " + stageCount(project) + ": " + stageLabel(item.stage())
                    : stageLabel(item.stage());
            Label state = new Label(stateLabel(item.state()) + " · " + stageContext
                    + (item.message().isBlank() ? "" : " · " + item.message()));
            state.setWrapText(true);
            HBox.setHgrow(state, Priority.ALWAYS);
            ProgressBar itemProgress = new ProgressBar(stageProgress(item));
            itemProgress.setMaxWidth(Double.MAX_VALUE);
            itemProgress.setAccessibleText("Progreso de la etapa: "
                    + Math.round(stageProgress(item) * 100) + " por ciento");
            Button child = ActionButtonFactory.secondary("Ver en DocuPodcast",
                    () -> viewInDocuPodcast(descriptor.getParent().resolve(item.childProjectRelativePath())));
            Button pause = ActionButtonFactory.secondary(item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.PAUSED ? "Reanudar" : "Pausar",
                    () -> queueAction(project, descriptor, item, item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.PAUSED ? "resume" : "pause"));
            Button skip = ActionButtonFactory.secondary("Omitir", () -> queueAction(project, descriptor, item, "skip"));
            Button retry = ActionButtonFactory.secondary("Reintentar", () -> queueAction(project, descriptor, item, "retry"));
            Button up = ActionButtonFactory.secondary("↑", "Subir documento en la cola",
                    () -> queueAction(project, descriptor, item, "up"));
            Button down = ActionButtonFactory.secondary("↓", "Bajar documento en la cola",
                    () -> queueAction(project, descriptor, item, "down"));
            boolean terminal = item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.COMPLETED;
            pause.setDisable(terminal || item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.SKIPPED);
            skip.setDisable(terminal || item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.RUNNING);
            retry.setDisable(item.state() != com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.FAILED
                    && item.state() != com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.CANCELLED
                    && item.state() != com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.SKIPPED);
            child.setDisable(execution != null && execution.running());
            HBox identity = new HBox(10, order, titleLabel, state);
            identity.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(state, Priority.ALWAYS);
            HBox actions = new HBox(7, up, down, pause, skip, retry, child);
            actions.setAlignment(Pos.CENTER_RIGHT);
            VBox card = new VBox(7, identity, itemProgress, actions);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: -docu-bg-card-soft; -fx-background-radius: 8; "
                    + "-fx-border-color: -docu-border-soft; -fx-border-radius: 8;");
            rows.getChildren().add(card);
        }
        Button start = ActionButtonFactory.primary(execution != null && execution.running()
                ? "Producción en curso" : "Exportar pendientes · " + project.profile().outputLabel());
        start.setDisable(execution == null || execution.running() || project.items().stream().noneMatch(
                item -> item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.PENDING));
        start.setOnAction(event -> startProduction(project, descriptor));
        Button pauseQueue = ActionButtonFactory.secondary("Pausar cola", () -> {
            if (execution != null) execution.requestPause();
        });
        pauseQueue.setDisable(execution == null || !execution.running());
        Button cancelCurrent = ActionButtonFactory.danger("Cancelar documento actual", () -> {
            if (execution != null) execution.cancelCurrent();
        });
        cancelCurrent.setDisable(execution == null || !execution.running());
        Button cancelAll = ActionButtonFactory.danger("Cancelar toda la producción", this::cancelEntireQueue);
        cancelAll.setDisable(execution == null || !execution.running());
        Button folder = ActionButtonFactory.secondary("Abrir carpeta de salida", () -> openPath(descriptor.getParent().resolve(project.profile().outputDirectory())));
        Label heading = new Label(project.title()); heading.getStyleClass().add("settings-title");
        Label summary = new Label(project.items().size() + " documentos · " + project.profile().outputLabel()
                + " · " + project.profile().outputDirectory());
        summary.setWrapText(true);
        Label progressSummary = new Label(exitAfterCancellation ? "Deteniendo toda la producción… Esperando el cierre del trabajo activo." : queueProgressText(project));
        progressSummary.setWrapText(true);
        progressSummary.setStyle("-fx-font-weight: 700;");
        ProgressBar queueProgress = new ProgressBar(queueProgress(project));
        queueProgress.setMaxWidth(Double.MAX_VALUE);
        queueProgress.setAccessibleText(progressSummary.getText());
        Label policy = new Label("La cola conserva pausas, omisiones, reintentos y orden. Se procesa un documento por vez y se reutilizan los derivados válidos; Pausar se aplica en el siguiente punto seguro.");
        policy.setWrapText(true);
        ScrollPane list = new ScrollPane(rows); list.setFitToWidth(true);
        list.setStyle("-fx-background: white; -fx-background-color: white;");
        Button accept = ActionButtonFactory.secondary(lastResult != null && !lastResult.paused()
                ? "Aceptar y volver a Inicio" : "Guardar cola y volver a Inicio", this::closeToHome);
        accept.setDisable(execution != null && execution.running());
        var operations = new javafx.scene.layout.FlowPane(10, 8, start, pauseQueue, cancelCurrent, cancelAll, folder, accept);
        operations.setAlignment(Pos.CENTER_LEFT);
        if (exitAfterCancellation) {
            start.setDisable(true); pauseQueue.setDisable(true); cancelCurrent.setDisable(true); cancelAll.setDisable(true);
        }
        VBox progressBlock = new VBox(5, progressSummary, queueProgress);
        VBox view = new VBox(12, heading, summary, progressBlock, policy, operations, list);
        if (lastResult != null) {
            Label result = new Label((lastResult.paused() ? "Producción pausada" : "Producción terminada")
                    + " · Completados: " + lastResult.completed() + " · Fallidos: " + lastResult.failed()
                    + " · Omitidos: " + lastResult.skipped() + " · Cancelados: " + lastResult.cancelled());
            result.setWrapText(true);
            view.getChildren().add(3, result);
        }
        view.setPadding(new Insets(18));
        view.setStyle("-fx-background-color: white;");
        VBox.setVgrow(list, Priority.ALWAYS);
        stage.getScene().setRoot(view);
        if (notify) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.initOwner(stage); alert.setTitle("Proyecto preparado");
            alert.setHeaderText("La cola documental está lista");
            alert.setContentText(project.items().size() + " documentos copiados y verificados.\n" + descriptor.getParent()); alert.show();
        }
    }

    private void startProduction(DocumentVideoBatchProject project, Path descriptor) {
        if (execution == null || execution.running()) return;
        lastResult = null;
        exitAfterCancellation = false;
        execution.start(project, descriptor, new DocumentVideoBatchExecutionPort.Listener() {
            @Override public Window notificationOwner() { return stage; }
            @Override public void projectChanged(DocumentVideoBatchProject updated) {
                Platform.runLater(() -> showProject(updated, descriptor, false));
            }

            @Override public void finished(DocumentVideoBatchProject updated,
                                           DocumentVideoBatchExecutionPort.BatchRunResult result) {
                Platform.runLater(() -> {
                    lastResult = result;
                    showProject(updated, descriptor, false);
                    if (exitAfterCancellation) closeToHome();
                });
            }
        });
    }

    private void cancelEntireQueue() {
        if (execution == null || !execution.running() || exitAfterCancellation) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Se detendrá el documento actual y se cancelarán todos los pendientes. "
                        + "Los archivos terminados se conservarán. Después volverás a Inicio.", ButtonType.YES, ButtonType.NO);
        confirmation.initOwner(stage);
        confirmation.setTitle("Cancelar toda la producción");
        confirmation.setHeaderText("¿Cancelar todos los documentos pendientes?");
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        exitAfterCancellation = true;
        execution.cancelAll();
    }

    private void openPath(Path path) {
        try { if (!Desktop.isDesktopSupported()) throw new IOException("El sistema no permite abrir rutas."); Desktop.getDesktop().open(path.toFile()); }
        catch (IOException ex) { showError("No se pudo abrir la ruta", ex); }
    }

    private void loadStageIcons() {
        for (int size : new int[]{32, 64, 256}) {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    DocumentVideoBatchWindow.class.getResource("/branding/docupodcast-icon-" + size + ".png"),
                    "Missing product icon " + size + "px").toExternalForm()));
        }
    }

    private static String queueProgressText(DocumentVideoBatchProject project) {
        int total = project.items().size();
        DocumentVideoBatchItem running = project.items().stream()
                .filter(item -> item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.RUNNING)
                .findFirst().orElse(null);
        if (running != null) {
            return "Proceso " + (running.order() + 1) + " de " + total + " · Etapa "
                    + stageNumber(running.stage()) + " de " + stageCount(project)
                    + ": " + stageLabel(running.stage());
        }
        long closed = project.items().stream().filter(DocumentVideoBatchWindow::isTerminal).count();
        return closed + " de " + total + " documentos cerrados · La cola conserva sus checkpoints";
    }

    private static double queueProgress(DocumentVideoBatchProject project) {
        if (project.items().isEmpty()) return 0.0;
        double units = 0.0;
        for (DocumentVideoBatchItem item : project.items()) {
            if (isTerminal(item)) units += 1.0;
            else if (item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.RUNNING) {
                units += ((stageNumber(item.stage()) - 1) + stageProgress(item)) / stageCount(project);
            }
        }
        return Math.max(0.0, Math.min(1.0, units / project.items().size()));
    }

    private static boolean isTerminal(DocumentVideoBatchItem item) {
        return switch (item.state()) {
            case COMPLETED, FAILED, SKIPPED, CANCELLED -> true;
            default -> false;
        };
    }

    private static int stageNumber(com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage stage) {
        return switch (stage) {
            case DISCOVERED, SOURCE_COPIED, PROJECT_CREATED, DOCUMENT_PREPARATION -> 1;
            case AUDIO_GENERATION -> 2;
            case AUDIO_VERIFICATION -> 3;
            case AUDIO_EXPORT -> 4;
            case AUDIO_OUTPUT_VERIFICATION -> 5;
            case VISUAL_PLAN -> 4;
            case VIDEO_RENDER -> 5;
            case VIDEO_VERIFICATION, FINISHED -> 6;
        };
    }

    /** Progress inside the current stage, kept separate from the persisted whole-document checkpoint. */
    private static double stageProgress(DocumentVideoBatchItem item) {
        if (item.state() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState.COMPLETED
                || item.stage() == com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage.FINISHED) return 1.0;
        double progress = item.progress();
        double local = switch (item.stage()) {
            case DISCOVERED, SOURCE_COPIED, PROJECT_CREATED -> 0.0;
            case DOCUMENT_PREPARATION -> progress / 0.20;
            case AUDIO_GENERATION -> (progress - 0.20) / 0.30;
            case AUDIO_VERIFICATION -> Math.max(0.0, (progress - 0.50) / 0.02);
            case AUDIO_EXPORT -> (progress - 0.55) / 0.40;
            case AUDIO_OUTPUT_VERIFICATION -> (progress - 0.95) / 0.05;
            case VISUAL_PLAN -> Math.max(0.0, (progress - 0.50) / 0.05);
            case VIDEO_RENDER -> (progress - 0.55) / 0.40;
            case VIDEO_VERIFICATION -> (progress - 0.95) / 0.05;
            case FINISHED -> 1.0;
        };
        return Math.max(0.0, Math.min(1.0, local));
    }

    private void setBusy(boolean value) {
        busy.setVisible(value); busy.setManaged(value);
        createButton.setDisable(value || !ready());
        saveConfigurationButton.setDisable(value);
        loadConfigurationButton.setDisable(value);
        if (!value) updateCreateEnabled();
    }
    private static int stageCount(DocumentVideoBatchProject project) { return project.profile().audioOnly() ? 5 : PRODUCTION_STAGE_COUNT; }
    private void updateCreateEnabled() {
        configurationFeedback.stop();
        String reason = missingCreateRequirement();
        createButton.setDisable(!reason.isBlank());
        createRequirement.setText(reason.isBlank() ? "Todo listo para crear la cola." : reason);
        createRequirement.setStyle("-fx-text-fill: #64748B;");
        createButton.setTooltip(new Tooltip(reason.isBlank() ? "Crear y guardar el proyecto por lotes." : reason));
    }
    private boolean ready() { return inventory != null && !inventory.documents().isEmpty() && !destinationFolder.getText().isBlank() && !title.getText().isBlank(); }
    private String missingCreateRequirement() {
        if (title.getText() == null || title.getText().isBlank()) return "Escribe el nombre del proyecto.";
        if (inventory == null) return "Selecciona o confirma una carpeta de documentos.";
        if (inventory.documents().isEmpty()) return "La carpeta no contiene documentos DOCX o PDF.";
        if (destinationFolder.getText() == null || destinationFolder.getText().isBlank()) return "Elige dónde crear el proyecto.";
        return "";
    }
    private static RegionSpacer spacer() { return new RegionSpacer(); }
    private static final class RegionSpacer extends javafx.scene.layout.Region { RegionSpacer() { HBox.setHgrow(this, Priority.ALWAYS); } }
    private static GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(9);
        grid.setMinWidth(0);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints labels = new ColumnConstraints(120, 220, 260);
        ColumnConstraints fields = new ColumnConstraints(0, 360, Double.MAX_VALUE);
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        ColumnConstraints actions = new ColumnConstraints();
        grid.getColumnConstraints().setAll(labels, fields, actions);
        return grid;
    }
    private static void addRow(GridPane grid, int row, String label, javafx.scene.Node control, javafx.scene.Node action) {
        Label name = new Label(label); name.setWrapText(true); name.setMinWidth(0); name.setMaxWidth(260); grid.add(name, 0, row); grid.add(control, 1, row); GridPane.setHgrow(control, Priority.ALWAYS);
        if (control instanceof javafx.scene.control.Control c) {
            c.setMinWidth(0);
            c.setMaxWidth(Double.MAX_VALUE);
        }
        if (action != null) grid.add(action, 2, row);
    }
    private static String hex(Color color) { return String.format("#%02X%02X%02X", Math.round(color.getRed()*255), Math.round(color.getGreen()*255), Math.round(color.getBlue()*255)); }
    private static String stateLabel(com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState state) {
        return switch (state) {
            case PENDING -> "Pendiente";
            case RUNNING -> "En proceso";
            case PAUSE_REQUESTED -> "Pausa solicitada";
            case PAUSED -> "Pausado";
            case COMPLETED -> "Completado";
            case FAILED -> "Falló";
            case SKIPPED -> "Omitido";
            case CANCELLED -> "Cancelado";
        };
    }
    private static String stageLabel(com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage stage) {
        return switch (stage) {
            case DISCOVERED -> "Descubierto";
            case SOURCE_COPIED -> "Fuente copiada";
            case PROJECT_CREATED -> "Proyecto preparado";
            case DOCUMENT_PREPARATION -> "Preparando documento";
            case AUDIO_GENERATION -> "Generando voz";
            case AUDIO_VERIFICATION -> "Verificando audio";
            case AUDIO_EXPORT -> "Exportando audio";
            case AUDIO_OUTPUT_VERIFICATION -> "Verificando archivo de audio";
            case VISUAL_PLAN -> "Construyendo video";
            case VIDEO_RENDER -> "Renderizando MP4";
            case VIDEO_VERIFICATION -> "Verificando MP4";
            case FINISHED -> "Finalizado";
        };
    }
    private void showError(String title, Throwable error) {
        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.initOwner(stage); alert.setTitle("DocuPodcast Studio"); alert.setHeaderText(title); alert.setContentText(error == null ? "Error desconocido" : error.getMessage()); alert.showAndWait(); });
    }
}
