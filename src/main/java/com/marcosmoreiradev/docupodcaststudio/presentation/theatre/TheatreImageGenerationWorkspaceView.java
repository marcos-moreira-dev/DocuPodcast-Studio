package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeRequest;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeImageStore;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationMemoryProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ComfyUiConnectionSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.FrameGenerationMode;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportEstimate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationResult;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedFrameCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageContextAsset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationJob;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationJobItem;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationJobManifestStore;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreIntermediateFrameBatchPlanner;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ImageFullscreenViewer;
import com.marcosmoreiradev.docupodcaststudio.presentation.settings.SettingsDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreAiWorkspaceLayout.balancedMasterDetail;
import static com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreAiWorkspaceLayout.detailStack;
import static com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreAiWorkspaceLayout.detachNode;
import static com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreAiWorkspaceLayout.masterDetail;
import static com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreAiWorkspaceLayout.moduleRoot;
import static com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreAiWorkspaceLayout.section;

/** Modular workspace for local theatrical image generation and frame production. */
public final class TheatreImageGenerationWorkspaceView extends BorderPane {
    private static final double PREVIEW_COLUMN_MIN_WIDTH = 320.0;
    private static final double PREVIEW_COLUMN_PREF_WIDTH = 560.0;
    private static final double PREVIEW_COLUMN_MAX_WIDTH = 640.0;
    private static final double PREVIEW_FRAME_INSET = 24.0;
    private static final double PREVIEW_FRAME_ASPECT_HEIGHT = 9.0 / 16.0;
    private static final double FRAME_CAROUSEL_CARD_WIDTH = 270.0;
    private static final double FRAME_CAROUSEL_PREVIEW_WIDTH = 250.0;
    private static final double FRAME_CAROUSEL_PREVIEW_HEIGHT = 150.0;
    private static final double FRAME_CAROUSEL_VIEWPORT_HEIGHT = 250.0;

    private final DocuPodcastShellViewModel viewModel;
    private final ObjectProperty<TheatreAiModuleId> activeModule = new SimpleObjectProperty<>(TheatreAiModuleId.HOME);
    private final ObservableList<TheatreImageGenerationUnit> units = FXCollections.observableArrayList();
    private final ObservableList<TheatreImageContextAsset> contextAssets = FXCollections.observableArrayList();
    private final ObservableList<TheatreGeneratedImageCandidate> imageCandidates = FXCollections.observableArrayList();
    private final ObservableList<TheatreGeneratedFrameCandidate> frameCandidates = FXCollections.observableArrayList();
    private final ObservableList<TheatreImageGenerationJob> jobs = FXCollections.observableArrayList();
    private final TheatreImageGenerationJobManifestStore jobManifestStore = new TheatreImageGenerationJobManifestStore();
    private final StringProperty selectedIntervention = new SimpleStringProperty("");
    private final ObjectProperty<Path> frameOutputDirectory = new SimpleObjectProperty<>();
    private final ComboBox<TheatreImageGenerationPreset> presetSelector = new ComboBox<>();
    private final ComboBox<FrameGenerationMode> frameModeSelector = new ComboBox<>();
    private final ComboBox<ImageEnhancementOutputProfile> outputProfileSelector = new ComboBox<>();
    private final ComboBox<TheatreImageAspectRatio> aspectRatioSelector = new ComboBox<>();
    private final ComboBox<ImageGenerationMemoryProfile> memoryProfileSelector = new ComboBox<>();
    private final TextArea promptEditor = new TextArea();
    private final TextArea smokePromptEditor = new TextArea();
    private final Spinner<Integer> smokeStepsSpinner = new Spinner<>();
    private final ListView<TheatreImageGenerationUnit> queueList = new ListView<>(units);
    private final ListView<TheatreImageContextAsset> contextAssetList = new ListView<>(contextAssets);
    private final ListView<TheatreGeneratedImageCandidate> imageCandidateList = new ListView<>(imageCandidates);
    private final ListView<TheatreGeneratedFrameCandidate> frameCandidateList = new ListView<>(frameCandidates);
    private final ListView<TheatreImageGenerationJob> jobList = new ListView<>(jobs);
    private final ImageView generatedPreviewImage = new ImageView();
    private final Label generatedPreviewTitle = new Label("Selecciona un candidato o frame.");
    private final Label generatedPreviewElapsed = new Label();
    private final TextField generatedPreviewPath = new TextField("El visor mostrara el PNG asociado al elemento seleccionado.");
    private final Label generatedPreviewPlaceholder = new Label("Sin frame seleccionado");
    private final StackPane generatedPreviewFrame = new StackPane(generatedPreviewPlaceholder, generatedPreviewImage);
    private final ImageView engineResultImage = new ImageView();
    private final Label engineResultTitle = new Label("Sin prueba ejecutada.");
    private final Label engineResultElapsed = new Label();
    private final Label engineResultPlaceholder = new Label("El PNG de prueba aparecera aqui.");
    private final StackPane engineResultFrame = new StackPane(engineResultPlaceholder, engineResultImage);
    private final Label engineResultPath = new Label("Sin PNG generado.");
    private final TextArea engineResultDiagnostic = new TextArea();
    private final Button engineResultFullscreen = ActionButtonFactory.iconOnly(
            AppIcon.FULLSCREEN,
            "Pantalla completa",
            this::showEngineResultFullscreen,
            "ui-action-button",
            "ui-action-button-secondary");
    private final Button engineResultDownload = ActionButtonFactory.secondary("Descargar imagen", this::downloadEngineResultImage);
    private final Button engineResultSettings = ActionButtonFactory.secondary("Abrir Configuracion", this::openEngineSettings);
    private final TextArea contextPreview = new TextArea();
    private final HBox selectedFrameCarousel = new HBox(12);
    private final ScrollPane selectedFrameCarouselScroll = new ScrollPane(selectedFrameCarousel);
    private final Label status = new Label("Motor local pendiente de verificar.");
    private final Label frameEstimate = new Label("Elige alcance y carpeta para estimar frames.");
    private final Label outputFolderLabel = new Label("Sin carpeta seleccionada.");
    private final Button intermediateBatchButton = ActionButtonFactory.secondary(
            "Generar frames intermedios",
            "Analiza la obra y genera los frames intermedios posibles con las imagenes principales asignadas.",
            this::toggleIntermediateFrameGeneration);
    private final ProgressBar frameProgress = new ProgressBar(0);
    private final Map<String, String> promptDrafts = new LinkedHashMap<>();
    private final TheatreAiWorkspaceShell shell;
    private final TheatreInterventionNavigator interventionNavigator;
    private Task<TheatreFrameGenerationResult> frameTask;
    private Task<IntermediateBatchResult> intermediateFrameTask;
    private EngineTestResult lastEngineResult = EngineTestResult.idle();
    private final StringProperty generationElapsedText = new SimpleStringProperty("Tiempo transcurrido: 00:00:00");
    private Timeline generationElapsedTimeline;
    private long generationStartedNanos;
    private int activeGenerationOperations;
    private boolean updatingPrompt;

    public TheatreImageGenerationWorkspaceView(DocuPodcastShellViewModel viewModel) {
        this.viewModel = viewModel;
        getStyleClass().add("voice-library-workspace");
        getStyleClass().add("theatre-ai-workspace");
        setPadding(new Insets(10));
        TheatreAiModuleNavigation navigation = new TheatreAiModuleNavigation(moduleDescriptors(), activeModule);
        shell = new TheatreAiWorkspaceShell(navigation);
        interventionNavigator = new TheatreInterventionNavigator(viewModel, selectedIntervention,
                this::processAct,
                this::processScene,
                this::processIntervention,
                this::exportContextForAlias);
        setCenter(shell);
        configureControls();
        restorePersistedJobs();
        jobs.addListener((ListChangeListener<TheatreImageGenerationJob>) change -> persistJobs());
        activeModule.addListener((obs, oldValue, newValue) -> render());
        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refreshAfterMediaRevision());
        rebuild();
        render();
    }

    private void restorePersistedJobs() {
        viewModel.currentProjectDirectory().ifPresent(projectRoot -> {
            try {
                jobs.setAll(jobManifestStore.load(projectRoot));
            } catch (IOException ex) {
                status.setText("No se pudo recuperar el historial de imagenes: " + ex.getMessage());
            }
        });
    }

    private void persistJobs() {
        viewModel.currentProjectDirectory().ifPresent(projectRoot -> {
            try {
                jobManifestStore.save(projectRoot, jobs);
            } catch (IOException ex) {
                status.setText("No se pudo guardar el historial de imagenes: " + ex.getMessage());
            }
        });
    }

    private static List<TheatreAiModuleDescriptor> moduleDescriptors() {
        return List.of(
                TheatreAiModuleDescriptor.of(TheatreAiModuleId.HOME, "OPERACION"),
                TheatreAiModuleDescriptor.of(TheatreAiModuleId.ENGINE, "MOTOR"),
                TheatreAiModuleDescriptor.of(TheatreAiModuleId.GENERATE, "PRODUCCION"),
                TheatreAiModuleDescriptor.of(TheatreAiModuleId.JOBS, "REVISION")
        );
    }

    private void configureControls() {
        presetSelector.getItems().setAll(TheatreImageGenerationPreset.values());
        presetSelector.setValue(currentPreset());
        presetSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            TheatreImageGenerationPreset selected = newValue == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : newValue;
            if (smokeStepsSpinner.getValueFactory() != null) {
                smokeStepsSpinner.getValueFactory().setValue(selected.steps());
            }
            ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(selected.name());
            if (profile.highEnd() && memoryProfileSelector.getValue() == ImageGenerationMemoryProfile.SAFE_LOW_VRAM) {
                memoryProfileSelector.setValue(ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD);
            }
        });
        memoryProfileSelector.getItems().setAll(ImageGenerationMemoryProfile.values());
        memoryProfileSelector.setValue(currentMemoryProfile());
        memoryProfileSelector.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ImageGenerationMemoryProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        });
        memoryProfileSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(ImageGenerationMemoryProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayName());
            }
        });
        ImageModelPackageProfile initialProfile = ImageModelPackageProfile.fromPreset(presetSelector.getValue().name());
        if (initialProfile.highEnd() && memoryProfileSelector.getValue() == ImageGenerationMemoryProfile.SAFE_LOW_VRAM) {
            memoryProfileSelector.setValue(ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD);
        }
        frameModeSelector.getItems().setAll(FrameGenerationMode.values());
        frameModeSelector.setValue(FrameGenerationMode.SINGLE);
        outputProfileSelector.getItems().setAll(ImageEnhancementOutputProfile.values());
        outputProfileSelector.setValue(ImageEnhancementOutputProfile.FHD_1080);
        outputProfileSelector.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ImageEnhancementOutputProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : outputProfileLabel(item));
            }
        });
        outputProfileSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(ImageEnhancementOutputProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : outputProfileLabel(item));
            }
        });
        aspectRatioSelector.getItems().setAll(TheatreImageAspectRatio.values());
        aspectRatioSelector.setValue(TheatreImageAspectRatio.WIDE_16_9);
        aspectRatioSelector.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TheatreImageAspectRatio item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        aspectRatioSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(TheatreImageAspectRatio item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label());
            }
        });
        smokeStepsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 80, currentPreset().steps(), 1));
        smokeStepsSpinner.setEditable(true);
        smokeStepsSpinner.setPrefWidth(120);
        smokePromptEditor.setPromptText("Escribe aqui la descripcion de la imagen para probar el motor.");
        smokePromptEditor.setPrefRowCount(4);
        smokePromptEditor.setWrapText(true);

        queueList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TheatreImageGenerationUnit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.interventionId() + " - " + item.speaker());
            }
        });
        queueList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.interventionId().equals(selectedIntervention.get())) {
                selectedIntervention.set(newValue.interventionId());
            }
            renderContext(newValue);
        });
        selectedIntervention.addListener((obs, oldValue, newValue) -> {
            savePromptDraft(oldValue);
            selectUnit(newValue);
            updateSelectedFrameOverview();
        });
        frameCandidates.addListener((ListChangeListener<TheatreGeneratedFrameCandidate>) change -> updateSelectedFrameOverview());
        imageCandidates.addListener((ListChangeListener<TheatreGeneratedImageCandidate>) change -> updateSelectedFrameOverview());

        contextAssetList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TheatreImageContextAsset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label title = new Label(item.label());
                title.getStyleClass().add("theatre-ai-context-title");
                Label meta = new Label(item.role() + " - " + item.relativePath());
                meta.setWrapText(true);
                VBox copy = new VBox(3, title, meta);
                HBox row = new HBox(8, thumbnail(item), copy);
                row.setAlignment(Pos.CENTER_LEFT);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(row);
            }
        });
        imageCandidateList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TheatreGeneratedImageCandidate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.approved() ? "Aprobado " : "Candidato ") + item.interventionId() + " - " + item.assetId());
            }
        });
        imageCandidateList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            frameCandidateList.getSelectionModel().clearSelection();
            selectedIntervention.set(newValue.interventionId());
            renderGeneratedPreview(newValue);
        });
        frameCandidateList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TheatreGeneratedFrameCandidate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String prefix = item.transitionFrame() ? "Intermedio " : "Frame ";
                setText((item.approved() ? "Aprobado " : prefix) + item.interventionId() + " #" + item.frameIndex());
            }
        });
        frameCandidateList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            imageCandidateList.getSelectionModel().clearSelection();
            selectedIntervention.set(newValue.interventionId());
            renderGeneratedPreview(newValue);
        });
        jobList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(TheatreImageGenerationJob item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(scopeLabel(item.scope()) + " - " + item.status() + " - " + item.items().size()
                        + " unidades - " + item.appliedReferences().size() + " referencias");
            }
        });
        contextPreview.setWrapText(true);
        contextPreview.setEditable(true);
        selectedFrameCarousel.getStyleClass().add("theatre-ai-frame-carousel");
        selectedFrameCarousel.setMinWidth(Region.USE_PREF_SIZE);
        selectedFrameCarousel.setMaxWidth(Region.USE_PREF_SIZE);
        selectedFrameCarouselScroll.getStyleClass().add("theatre-ai-frame-carousel-scroll");
        selectedFrameCarouselScroll.setFitToHeight(true);
        selectedFrameCarouselScroll.setFitToWidth(false);
        selectedFrameCarouselScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        selectedFrameCarouselScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        selectedFrameCarouselScroll.setMinHeight(FRAME_CAROUSEL_VIEWPORT_HEIGHT);
        selectedFrameCarouselScroll.setPrefViewportHeight(FRAME_CAROUSEL_VIEWPORT_HEIGHT);
        promptEditor.setWrapText(true);
        promptEditor.setPrefRowCount(7);
        promptEditor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingPrompt) {
                savePromptDraft(selectedIntervention.get(), newValue);
            }
        });
        frameProgress.setMaxWidth(Double.MAX_VALUE);
        generatedPreviewTitle.setWrapText(true);
        generatedPreviewTitle.getStyleClass().add("theatre-ai-generated-preview-title");
        generatedPreviewPath.setEditable(false);
        generatedPreviewPath.setFocusTraversable(true);
        generatedPreviewPath.setMaxWidth(Double.MAX_VALUE);
        generatedPreviewPath.getStyleClass().add("document-side-text");
        generatedPreviewPlaceholder.setWrapText(true);
        generatedPreviewPlaceholder.getStyleClass().add("theatre-ai-generated-preview-placeholder");
        configurePreviewFrame(generatedPreviewFrame, generatedPreviewImage, 260);
        configureGenerationElapsedLabel(generatedPreviewElapsed);
        generatedPreviewImage.setVisible(false);
        engineResultTitle.setWrapText(true);
        engineResultTitle.getStyleClass().add("theatre-ai-generated-preview-title");
        engineResultPath.setWrapText(true);
        engineResultPath.setMaxWidth(Double.MAX_VALUE);
        engineResultPath.getStyleClass().add("document-side-text");
        engineResultPlaceholder.setWrapText(true);
        engineResultPlaceholder.getStyleClass().add("theatre-ai-generated-preview-placeholder");
        configurePreviewFrame(engineResultFrame, engineResultImage, 280);
        configureGenerationElapsedLabel(engineResultElapsed);
        engineResultImage.setVisible(false);
        engineResultDiagnostic.setEditable(false);
        engineResultDiagnostic.setWrapText(true);
        engineResultDiagnostic.setPrefRowCount(4);
        engineResultDiagnostic.setMaxWidth(Double.MAX_VALUE);
        engineResultDiagnostic.getStyleClass().add("document-side-text");
        updateEngineResultPanel(EngineTestResult.idle());
    }

    private void rebuild() {
        units.setAll(viewModel.theatreImageGenerationQueue(TheatreContextExportScope.all()));
        interventionNavigator.refresh();
        if (!units.isEmpty() && queueList.getSelectionModel().getSelectedItem() == null) {
            queueList.getSelectionModel().select(0);
        }
        status.setText("Intervenciones actualizadas: " + units.size() + " listas, 0 pendientes.");
    }

    private void refreshAfterMediaRevision() {
        TheatreImageGenerationUnit selected = selectedUnit();
        String selectedId = selected == null ? selectedIntervention.get() : selected.interventionId();
        rebuild();
        if (selectedId != null && !selectedId.isBlank()) {
            selectUnit(selectedId);
        }
        renderContext(selectedUnit());
    }

    private void render() {
        VBox module = switch (activeModule.get()) {
            case HOME -> homeModule();
            case ENGINE -> engineModule();
            case GENERATE -> generateModule();
            case JOBS -> jobsModule();
        };
        shell.setModuleContent(module);
    }

    private VBox homeModule() {
        VBox box = moduleRoot("Inicio", "Gestionar frames de la obra", "Configura el motor local, selecciona una intervencion, genera candidatos y revisa resultados.");
        VBox overview = section("Estado operativo");
        overview.getChildren().addAll(
                statusLine("Motor", imageEngineSummary()),
                statusLine("Cola", units.size() + " intervenciones disponibles."),
                statusLine("Trabajos", jobs.size() + " trabajos, " + (imageCandidates.size() + frameCandidates.size()) + " candidatos."));
        Button engine = ActionButtonFactory.primary("Configurar motor", () -> activeModule.set(TheatreAiModuleId.ENGINE));
        Button generate = ActionButtonFactory.secondary("Generar intervencion", () -> activeModule.set(TheatreAiModuleId.GENERATE));
        Button jobsButton = ActionButtonFactory.secondary("Ver trabajos", () -> activeModule.set(TheatreAiModuleId.JOBS));
        overview.getChildren().add(actionFlow(engine, generate, jobsButton));
        VBox detail = section("Siguiente accion");
        detail.getChildren().addAll(
                note("1. Prepara o verifica Imagen IA teatral local en Configuracion."),
                note("2. En Generar, selecciona la intervencion y revisa su contexto."),
                note("3. En Generar, elige intervencion, escena, acto u obra completa."));
        box.getChildren().add(balancedMasterDetail(overview, detail));
        return box;
    }

    private VBox engineModule() {
        VBox box = moduleRoot("Configurar motor", "Motor local de imagen IA", "Configura una prueba real y revisa el PNG generado sin salir del workspace.");
        VBox selection = section("Motor");
        detachNode(outputProfileSelector);
        detachNode(aspectRatioSelector);
        detachNode(memoryProfileSelector);
        selection.getChildren().addAll(
                statusLine("Modo", "Local autocontenido. Internet solo para descargar paquete de modelos."),
                row("Preset", presetSelector),
                row("Perfil", outputProfileSelector),
                row("Relacion de aspecto", aspectRatioSelector),
                devicePerformanceLine(),
                row("Memoria", memoryProfileSelector),
                note("VRAM + RAM permite probar modelos grandes con offload; sera mas lento si la GPU no tiene suficiente VRAM."),
                row("Pasadas", smokeStepsSpinner),
                smokePromptBox(),
                note("La prueba FLUX usa una base ligera de diagnostico y luego prepara la salida elegida. La generacion de produccion conserva su resolucion completa."));
        Button settings = ActionButtonFactory.primary("Abrir Configuracion", this::openEngineSettings);
        Button test = ActionButtonFactory.primary("Probar motor", this::testLocalEngine);
        Button rebuild = ActionButtonFactory.secondary("Actualizar intervenciones", this::rebuild);
        selection.getChildren().add(actionFlow(settings, test, rebuild));
        VBox result = engineResultPanel();
        HBox layout = masterDetail(selection, result);
        constrainPreviewColumn(result);
        box.getChildren().add(layout);
        return box;
    }

    private VBox engineResultPanel() {
        VBox result = section("Resultado de prueba");
        detachNode(engineResultTitle);
        detachNode(engineResultFrame);
        detachNode(engineResultElapsed);
        detachNode(engineResultPath);
        detachNode(engineResultDiagnostic);
        detachNode(engineResultFullscreen);
        detachNode(engineResultDownload);
        detachNode(engineResultSettings);
        result.getChildren().addAll(
                engineResultTitle,
                engineResultFrame,
                engineResultElapsed,
                engineResultPath,
                actionFlow(engineResultFullscreen, engineResultDownload, engineResultSettings),
                engineResultDiagnostic);
        updateEngineResultPanel(lastEngineResult);
        return result;
    }

    private VBox generateModule() {
        VBox box = moduleRoot("Generar", "Contexto y frames", "Procesa desde el mapa de la obra y revisa el contexto seleccionado.");
        VBox output = section("Salida teatral");
        detachNode(outputProfileSelector);
        detachNode(aspectRatioSelector);
        detachNode(frameModeSelector);
        Button chooseFolder = ActionButtonFactory.secondary("Elegir carpeta de salida", this::chooseFrameOutputDirectory);
        Button generateAll = ActionButtonFactory.primary(
                "Procesar obra completa",
                () -> processScope(TheatreFrameGenerationScope.all(), "Obra completa"));
        detachNode(intermediateBatchButton);
        output.getChildren().addAll(
                row("Perfil", outputProfileSelector),
                row("Relacion de aspecto", aspectRatioSelector),
                row("Modo", frameModeSelector),
                note("El modo intermedios crea continuidad entre intervenciones consecutivas cuando el perfil lo soporta."),
                outputFolderLabel,
                actionFlow(chooseFolder, generateAll, intermediateBatchButton),
                note("Perfil por defecto: 1080p - 1920x1080. Relacion por defecto: 16:9. Se aplica al procesar intervenciones, escenas y actos."));
        VBox navigator = section("Intervenciones");
        detachNode(interventionNavigator);
        navigator.getChildren().addAll(
                note("Clic derecho sobre una intervención para procesarla o exportar su paquete IA."),
                interventionNavigator,
                selectedUnitLabel());

        VBox actions = section("Paquetes IA");
        Button exportOne = ActionButtonFactory.secondary(
                "Exportar paquete seleccionado",
                "Guarda el contexto de IA de la intervencion seleccionada con sus imagenes de referencia.",
                this::exportSelectedContextPackage);
        Button exportAll = ActionButtonFactory.secondary(
                "Exportar paquetes IA",
                "Guarda un paquete de contexto por cada intervencion disponible del alcance actual.",
                this::exportBulkContextPackages);
        actions.getChildren().addAll(actionFlow(exportOne, exportAll),
                note("Los paquetes copian imagenes repetidas por intervencion; puede ocupar bastante disco."));

        Node top = balancedMasterDetail(navigator, detailStack(contextPanel(), actions));
        box.getChildren().addAll(output, selectedFramesOverview(), top, status);
        return box;
    }

    private VBox jobsModule() {
        VBox box = moduleRoot("Trabajos", "Cola, resultados y errores", "Revisa trabajos activos, fallidos y terminados. Los frames principales de lote se registran como variante IA.");
        VBox activeJobs = section("Trabajos activos y recientes");
        detachNode(jobList);
        jobList.setPrefHeight(180);
        Button cancel = ActionButtonFactory.danger("Cancelar", this::cancelFrameGeneration);
        Button retry = ActionButtonFactory.secondary("Reintentar", this::retrySelectedJob);
        Button open = ActionButtonFactory.secondary("Abrir carpeta", this::openSelectedJobFolder);
        activeJobs.getChildren().addAll(
                jobList,
                actionFlow(cancel, retry, open),
                frameEstimate,
                frameProgress);

        VBox images = section("Candidatos individuales");
        detachNode(imageCandidateList);
        imageCandidateList.setPrefHeight(220);
        Button approveImage = ActionButtonFactory.primary("Aprobar candidato", this::approveSelectedImageCandidate);
        images.getChildren().addAll(
                imageCandidateList,
                approveImage);
        VBox frames = section("Frames de trabajos");
        detachNode(generatedPreviewTitle);
        detachNode(generatedPreviewFrame);
        detachNode(generatedPreviewElapsed);
        detachNode(generatedPreviewPath);
        detachNode(frameCandidateList);
        frameCandidateList.setPrefHeight(150);
        Button approveFrame = ActionButtonFactory.primary("Aprobar frame-001", this::approveSelectedFrameCandidate);
        frames.getChildren().addAll(
                generatedPreviewTitle,
                generatedPreviewFrame,
                generatedPreviewElapsed,
                generatedPreviewPath,
                note("Selecciona un candidato o frame para revisar su imagen aqui."),
                frameCandidateList,
                approveFrame);
        HBox review = balancedMasterDetail(images, frames);
        constrainPreviewColumn(frames);
        box.getChildren().addAll(activeJobs, review);
        return box;
    }

    private static void configurePreviewFrame(StackPane frame, ImageView image, double minHeight) {
        frame.getStyleClass().add("theatre-ai-generated-preview-frame");
        frame.setMinWidth(PREVIEW_COLUMN_MIN_WIDTH);
        frame.setPrefWidth(PREVIEW_COLUMN_PREF_WIDTH);
        frame.setMaxWidth(PREVIEW_COLUMN_MAX_WIDTH);
        frame.setMinSize(PREVIEW_COLUMN_MIN_WIDTH, minHeight);
        frame.setPrefSize(PREVIEW_COLUMN_PREF_WIDTH, PREVIEW_COLUMN_PREF_WIDTH * PREVIEW_FRAME_ASPECT_HEIGHT + PREVIEW_FRAME_INSET);
        frame.setMaxSize(PREVIEW_COLUMN_MAX_WIDTH, PREVIEW_COLUMN_MAX_WIDTH * PREVIEW_FRAME_ASPECT_HEIGHT + PREVIEW_FRAME_INSET);
        frame.setMinHeight(minHeight);
        frame.setPrefHeight(PREVIEW_COLUMN_PREF_WIDTH * PREVIEW_FRAME_ASPECT_HEIGHT + PREVIEW_FRAME_INSET);
        frame.setMaxHeight(PREVIEW_COLUMN_MAX_WIDTH * PREVIEW_FRAME_ASPECT_HEIGHT + PREVIEW_FRAME_INSET);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.fitWidthProperty().bind(frame.widthProperty().subtract(PREVIEW_FRAME_INSET));
        image.fitHeightProperty().bind(frame.heightProperty().subtract(PREVIEW_FRAME_INSET));
    }

    private static void constrainPreviewColumn(Region region) {
        region.setMinWidth(PREVIEW_COLUMN_MIN_WIDTH);
        region.setPrefWidth(PREVIEW_COLUMN_PREF_WIDTH);
        region.setMaxWidth(PREVIEW_COLUMN_MAX_WIDTH);
        HBox.setHgrow(region, Priority.ALWAYS);
    }

    private VBox contextPanel() {
        VBox box = section("Contexto limpio para frames");
        Label images = note("Referencias que se aplicaran al motor");
        Label text = note("Texto del contexto");
        Button saveContext = ActionButtonFactory.primary("Guardar contexto textual", this::saveContextText);
        detachNode(contextAssetList);
        detachNode(contextPreview);
        contextAssetList.setPrefHeight(220);
        VBox.setVgrow(contextPreview, Priority.ALWAYS);
        box.getChildren().addAll(images, contextAssetList, text, contextPreview, saveContext);
        return box;
    }

    private VBox selectedFramesOverview() {
        VBox box = section("Frames de la intervencion seleccionada");
        detachNode(selectedFrameCarouselScroll);
        box.getChildren().add(selectedFrameCarouselScroll);
        updateSelectedFrameOverview();
        return box;
    }

    private void processAct(TheatreProjectLayer.TheatreAct act) {
        if (act == null) {
            status.setText("Selecciona un acto.");
            return;
        }
        processScope(TheatreFrameGenerationScope.act(act.id()), "Acto " + act.displayName());
    }

    private void processScene(TheatreProjectLayer.Scene scene) {
        if (scene == null) {
            status.setText("Selecciona una escena.");
            return;
        }
        processScope(TheatreFrameGenerationScope.scene(scene.id()), "Escena " + scene.displayName());
    }

    private void processIntervention(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        if (alias == null) {
            status.setText("Selecciona una intervencion.");
            return;
        }
        selectedIntervention.set(alias.alias());
        generateSelectedCandidate();
    }

    private void processScope(TheatreFrameGenerationScope scope, String label) {
        if (scope == null) {
            status.setText("Selecciona un alcance.");
            return;
        }
        if (!ensureFrameOutputDirectory()) {
            status.setText("Trabajo cancelado: no se eligio carpeta de salida.");
            return;
        }
        generateFrames(scope, label == null || label.isBlank() ? scopeLabel(scope) : label);
    }

    private void selectUnit(String interventionId) {
        if (interventionId == null || interventionId.isBlank()) {
            return;
        }
        units.stream()
                .filter(item -> item.interventionId().equals(interventionId))
                .findFirst()
                .ifPresent(value -> queueList.getSelectionModel().select(value));
    }

    private TheatreImageGenerationUnit selectedUnit() {
        TheatreImageGenerationUnit selected = queueList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected;
        }
        String id = selectedIntervention.get();
        return units.stream().filter(unit -> unit.interventionId().equals(id)).findFirst().orElse(null);
    }

    private void renderContext(TheatreImageGenerationUnit unit) {
        if (unit == null) {
            contextAssets.clear();
            contextPreview.clear();
            setPromptText("");
            updateSelectedFrameOverview();
            return;
        }
        contextAssets.setAll(viewModel.theatreImageGenerationContextAssets(unit));
        contextPreview.setText(contextPreviewText(unit));
        setPromptText(promptFor(unit));
        updateSelectedFrameOverview();
    }

    private void updateSelectedFrameOverview() {
        selectedFrameCarousel.getChildren().clear();
        TheatreImageGenerationUnit current = selectedUnit();
        if (current == null) {
            selectedFrameCarousel.getChildren().add(frameCarouselCard(
                    "Sin intervencion seleccionada",
                    "",
                    Optional.empty(),
                    "Selecciona una intervencion para revisar sus frames.",
                    null,
                    false,
                    ""));
            return;
        }

        TheatreImageGenerationUnit next = nextUnit(current);
        Optional<String> currentImage = principalImageUri(current.interventionId());
        Optional<String> nextImage = next == null ? Optional.empty() : principalImageUri(next.interventionId());
        String currentLabel = TheatreZigzagLayout.displayLabel(current.interventionId());

        selectedFrameCarousel.getChildren().add(frameCarouselCard(
                "Frame principal",
                currentLabel,
                currentImage,
                "No hay frame principal generado para " + currentLabel + ".",
                () -> generateCandidateForUnit(current),
                true,
                ""));

        if (next != null) {
            String nextLabel = TheatreZigzagLayout.displayLabel(next.interventionId());
            Optional<String> intermediate = selectedTransitionCandidate(current.interventionId(), next.interventionId())
                    .flatMap(candidate -> imageUri(candidate.outputPath()));
            boolean canReprocessIntermediate = currentImage.isPresent() && nextImage.isPresent();
            selectedFrameCarousel.getChildren().add(frameCarouselCard(
                    "Frame intermedio",
                    currentLabel + " -> " + nextLabel,
                    intermediate,
                    "No hay frame intermedio generado entre las dos intervenciones.",
                    () -> regenerateTransition(current, next),
                    canReprocessIntermediate,
                    canReprocessIntermediate ? "" : "Genera primero los frames principales actual y siguiente."));
            selectedFrameCarousel.getChildren().add(frameCarouselCard(
                    "Frame siguiente",
                    nextLabel,
                    nextImage,
                    "No hay frame principal generado para " + nextLabel + ".",
                    () -> generateCandidateForUnit(next),
                    true,
                    ""));
        } else {
            selectedFrameCarousel.getChildren().add(frameCarouselCard(
                    "Frame intermedio",
                    "Sin siguiente intervencion",
                    Optional.empty(),
                    "No hay siguiente intervencion para generar un frame intermedio.",
                    null,
                    false,
                    ""));
        }
    }

    private VBox frameCarouselCard(String title,
                                   String subtitle,
                                   Optional<String> imageUri,
                                   String placeholderText,
                                   Runnable reprocessAction,
                                   boolean reprocessEnabled,
                                   String disabledReason) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("theatre-ai-frame-carousel-title");
        Label subtitleLabel = new Label(subtitle == null ? "" : subtitle);
        subtitleLabel.getStyleClass().add("theatre-ai-frame-carousel-subtitle");
        subtitleLabel.setWrapText(true);

        Label placeholder = new Label(placeholderText);
        placeholder.setWrapText(true);
        placeholder.getStyleClass().add("theatre-ai-frame-carousel-placeholder");
        ImageView image = new ImageView();
        image.setPreserveRatio(true);
        image.setSmooth(true);
        StackPane preview = new StackPane(placeholder, image);
        preview.getStyleClass().add("theatre-ai-frame-carousel-preview");
        preview.setMinSize(FRAME_CAROUSEL_PREVIEW_WIDTH, FRAME_CAROUSEL_PREVIEW_HEIGHT);
        preview.setPrefSize(FRAME_CAROUSEL_PREVIEW_WIDTH, FRAME_CAROUSEL_PREVIEW_HEIGHT);
        preview.setMaxSize(FRAME_CAROUSEL_PREVIEW_WIDTH, FRAME_CAROUSEL_PREVIEW_HEIGHT);
        placeholder.setMaxWidth(FRAME_CAROUSEL_PREVIEW_WIDTH - 16.0);
        StackPane.setAlignment(placeholder, Pos.CENTER);
        image.fitWidthProperty().bind(preview.widthProperty().subtract(16));
        image.fitHeightProperty().bind(preview.heightProperty().subtract(16));
        renderCarouselImage(image, placeholder, imageUri, placeholderText);

        VBox card = new VBox(6, titleLabel, subtitleLabel, preview);
        card.getStyleClass().add("theatre-ai-frame-carousel-card");
        card.setMinWidth(FRAME_CAROUSEL_CARD_WIDTH);
        card.setPrefWidth(FRAME_CAROUSEL_CARD_WIDTH);
        card.setMaxWidth(FRAME_CAROUSEL_CARD_WIDTH);
        if (reprocessAction != null) {
            MenuItem reprocess = new MenuItem("Reprocesar " + title.toLowerCase(Locale.ROOT));
            reprocess.setDisable(!reprocessEnabled);
            reprocess.setOnAction(event -> reprocessAction.run());
            ContextMenu menu = new ContextMenu(reprocess);
            card.setOnContextMenuRequested(event -> menu.show(card, event.getScreenX(), event.getScreenY()));
            Tooltip.install(card, new Tooltip(reprocessEnabled
                    ? "Clic derecho para reprocesar este frame."
                    : disabledReason));
        }
        return card;
    }

    private static void renderCarouselImage(ImageView image,
                                            Label placeholder,
                                            Optional<String> imageUri,
                                            String placeholderText) {
        if (imageUri.isEmpty()) {
            image.setImage(null);
            image.setVisible(false);
            placeholder.setText(placeholderText);
            placeholder.setVisible(true);
            return;
        }
        Image loaded = new Image(imageUri.get(), false);
        if (loaded.isError()) {
            image.setImage(null);
            image.setVisible(false);
            placeholder.setText("No se pudo cargar la imagen.");
            placeholder.setVisible(true);
            return;
        }
        image.setImage(loaded);
        image.setVisible(true);
        placeholder.setVisible(false);
    }

    private TheatreImageGenerationUnit nextUnit(TheatreImageGenerationUnit current) {
        int currentIndex = units.indexOf(current);
        return currentIndex >= 0 && currentIndex + 1 < units.size() ? units.get(currentIndex + 1) : null;
    }

    private Optional<String> principalImageUri(String interventionId) {
        return principalFrameReference(interventionId).flatMap(reference -> imageUri(reference.path()));
    }

    private Optional<PrincipalFrameReference> principalFrameReference(String interventionId) {
        if (interventionId == null || interventionId.isBlank()) {
            return Optional.empty();
        }
        return viewModel.theatrePrimaryVisualReference(interventionId)
                .map(reference -> new PrincipalFrameReference(reference.assetId(), reference.absolutePath()));
    }

    private Optional<TheatreGeneratedFrameCandidate> selectedTransitionCandidate(String interventionId,
                                                                                   String nextInterventionId) {
        if (interventionId == null || interventionId.isBlank()
                || nextInterventionId == null || nextInterventionId.isBlank()) {
            return Optional.empty();
        }
        for (int i = frameCandidates.size() - 1; i >= 0; i--) {
            TheatreGeneratedFrameCandidate candidate = frameCandidates.get(i);
            if (candidate.transitionFrame()
                    && interventionId.equals(candidate.interventionId())
                    && nextInterventionId.equals(candidate.nextInterventionId())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> imageUri(Path outputPath) {
        return outputPath != null && Files.isRegularFile(outputPath)
                ? Optional.of(outputPath.toUri().toString())
                : Optional.empty();
    }

    private record PrincipalFrameReference(String assetId, Path path) {
    }

    private void openEngineSettings() {
        new SettingsDialog().showVoiceEngines(getScene() == null ? null : getScene().getWindow(), viewModel.applicationServices().settings());
    }

    private void openPerformanceSettings() {
        new SettingsDialog().showPerformance(getScene() == null ? null : getScene().getWindow(), viewModel.applicationServices().settings());
    }

    private void testLocalEngine() {
        status.setText("Probando motor local...");
        updateEngineResultPanel(EngineTestResult.running("Verificando runtime..."));
        beginGenerationTiming();

        Task<EngineTestResult> task = new Task<>() {
            @Override protected EngineTestResult call() {
                Path appRoot = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
                OperationalSettings settings = operationalSettingsForEngineTest();
                updateMessage("Verificando runtime...");
                ImageEngineReadinessReport readiness = viewModel.applicationServices().settings()
                        .inspectLocalTheatreImageEngine()
                        .inspect(settings, appRoot);
                updateMessage("Verificando modelo...");
                if (!readiness.runtimePrepared() || !readiness.modelInstalled()) {
                    return EngineTestResult.from(readiness);
                }
                updateMessage(readiness.engineResponding()
                        ? "Generando PNG..."
                        : "Iniciando motor y generando PNG...");
                ImageEngineSmokeReport report = viewModel.applicationServices().settings()
                        .runLocalTheatreImageSmoke()
                        .runDetailed(settings, appRoot, smokeRequest());
                return EngineTestResult.from(report);
            }
        };
        task.messageProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                updateEngineResultPanel(EngineTestResult.running(newValue));
            }
        });
        task.setOnSucceeded(event -> {
            finishGenerationTiming();
            showEngineResult(task.getValue());
        });
        task.setOnFailed(event -> {
            finishGenerationTiming();
            Throwable ex = task.getException();
            showEngineResult(EngineTestResult.error("No se pudo probar el motor: "
                    + (ex == null ? "error desconocido" : ex.getMessage())));
        });
        Thread thread = new Thread(task, "docupodcast-theatre-ai-engine-test");
        thread.setDaemon(true);
        thread.start();
    }

    private void showEngineResult(EngineTestResult result) {
        EngineTestResult safe = result == null ? EngineTestResult.error("Prueba terminada sin detalle.") : result;
        status.setText(safe.message());
        updateEngineResultPanel(safe);
    }

    private void showEngineResult(String message) {
        showEngineResult(EngineTestResult.error(message));
    }

    private ImageEngineSmokeRequest smokeRequest() {
        Path output = ImageEngineSmokeImageStore.outputDirectory();
        return new ImageEngineSmokeRequest(
                presetSelector.getValue() == null
                        ? com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile.DIAGNOSTIC_SD15
                        : presetSelector.getValue().visualProfile(),
                selectedOutputProfile(),
                selectedAspectRatio().visualAspectRatio(),
                smokePromptText(),
                smokeSteps(),
                output);
    }

    private VBox smokePromptBox() {
        Label label = new Label("Descripcion de prueba");
        label.getStyleClass().add("voice-library-body");
        Label helper = note("Si la dejas vacia, la prueba usa la intervencion seleccionada como microcontexto textual.");
        VBox box = new VBox(6, label, smokePromptEditor, helper);
        VBox.setVgrow(smokePromptEditor, Priority.NEVER);
        return box;
    }

    private String smokePromptText() {
        String custom = smokePromptEditor.getText();
        if (custom != null && !custom.isBlank()) {
            return custom.strip();
        }
        TheatreImageGenerationUnit unit = selectedUnit();
        if (unit != null) {
            return promptFor(unit);
        }
        return "Escena teatral de aviadores comicos en un hangar, luz calida, vestuario de epoca, composicion cinematografica.";
    }

    private int smokeSteps() {
        Integer value = smokeStepsSpinner.getValue();
        return value == null ? currentPreset().steps() : Math.max(4, Math.min(80, value));
    }

    private void updateEngineResultPanel(EngineTestResult result) {
        EngineTestResult safe = result == null ? EngineTestResult.idle() : result;
        lastEngineResult = safe;
        engineResultTitle.setText(safe.message());
        Path output = safe.outputImage();
        boolean hasImage = output != null && Files.isRegularFile(output);
        engineResultPath.setText(hasImage
                ? "PNG temporal listo. Usa Descargar imagen para guardarlo fuera de la carpeta interna."
                : "Sin PNG generado.");
        if (hasImage) {
            Image image = new Image(output.toUri().toString(), false);
            if (image.isError()) {
                engineResultImage.setImage(null);
                engineResultImage.setVisible(false);
                engineResultPlaceholder.setVisible(true);
                engineResultPlaceholder.setText("No se pudo cargar el PNG generado.");
            } else {
                engineResultImage.setImage(image);
                engineResultImage.setVisible(true);
                engineResultPlaceholder.setVisible(false);
            }
        } else {
            engineResultImage.setImage(null);
            engineResultImage.setVisible(false);
            engineResultPlaceholder.setVisible(true);
            engineResultPlaceholder.setText(safe.running() ? "Generando PNG..." : "El PNG de prueba aparecera aqui.");
        }
        String diagnostic = safe.diagnostic();
        engineResultDiagnostic.setText(diagnostic);
        boolean hasDiagnostic = diagnostic != null && !diagnostic.isBlank();
        engineResultDiagnostic.setVisible(hasDiagnostic);
        engineResultDiagnostic.setManaged(hasDiagnostic);
        setNodeVisible(engineResultFullscreen, hasImage);
        setNodeVisible(engineResultDownload, hasImage);
        setNodeVisible(engineResultSettings, safe.runtimeMissing() || safe.modelMissing());
    }

    private void showEngineResultFullscreen() {
        if (lastEngineResult == null || lastEngineResult.outputImage() == null) {
            status.setText("No hay PNG de prueba para abrir en pantalla completa.");
            return;
        }
        Image image = engineResultImage.getImage();
        if (image == null || image.isError()) {
            image = new Image(lastEngineResult.outputImage().toUri().toString(), false);
        }
        ImageFullscreenViewer.show(
                image,
                getScene() == null ? null : getScene().getWindow(),
                getScene() == null ? List.of() : getScene().getStylesheets(),
                "Resultado de Imagen IA teatral",
                "No se pudo abrir el PNG de prueba",
                status::setText);
    }

    private void downloadEngineResultImage() {
        if (lastEngineResult == null || lastEngineResult.outputImage() == null) {
            status.setText("No hay PNG de prueba para descargar.");
            return;
        }
        Path source = lastEngineResult.outputImage();
        if (!Files.isRegularFile(source)) {
            status.setText("El PNG temporal ya no esta disponible.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar imagen de prueba");
        chooser.setInitialFileName(defaultEngineResultFileName(lastEngineResult));
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"));
        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            status.setText("Imagen de prueba guardada.");
            engineResultPath.setText("Imagen guardada en: " + target.toPath());
        } catch (IOException | RuntimeException ex) {
            status.setText("No se pudo guardar la imagen: " + ex.getMessage());
        }
    }

    private static String defaultEngineResultFileName(EngineTestResult result) {
        String profile = result == null || result.outputProfile().isBlank() ? "prueba" : result.outputProfile().toLowerCase(Locale.ROOT);
        String aspect = result == null || result.aspectRatio().isBlank()
                ? "imagen"
                : result.aspectRatio().replace(':', 'x').toLowerCase(Locale.ROOT);
        String dimensions = result != null && result.width() > 0 && result.height() > 0
                ? "-" + result.width() + "x" + result.height()
                : "";
        return "docupodcast-imagen-prueba-" + profile + "-" + aspect + dimensions + ".png";
    }

    private void generateSelectedCandidate() {
        TheatreImageGenerationUnit unit = selectedUnit();
        if (unit == null) {
            status.setText("Selecciona una intervencion.");
            return;
        }
        generateCandidateForUnit(unit);
    }

    private void generateCandidateForUnit(TheatreImageGenerationUnit unit) {
        if (unit == null) {
            status.setText("Selecciona una intervencion.");
            return;
        }
        TheatreImageGenerationUnit promptUnit = unitWithPrompt(unit, promptFor(unit));
        status.setText("Generando candidato...");
        TheatreImageGenerationJob job = singleUnitJob("CANDIDATO-" + unit.interventionId(), unit,
                "En cola", true);
        jobs.add(0, job);
        jobList.getSelectionModel().select(job);
        frameProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        frameEstimate.setText("En cola: " + unit.interventionId() + ".");
        activeModule.set(TheatreAiModuleId.JOBS);
        runAsync(() -> viewModel.generateTheatreImageCandidate(promptUnit, settings(), presetSelector.getValue(),
                selectedOutputProfile(), selectedAspectRatio(), false,
                message -> Platform.runLater(() -> {
                    frameEstimate.setText(message);
                    replaceJobById(job.id(), job.withStatus(message, true));
                })), candidate -> {
            imageCandidates.add(0, candidate);
            imageCandidateList.getSelectionModel().select(candidate);
            imageCandidateList.scrollTo(candidate);
            renderGeneratedPreview(candidate);
            replaceJobById(job.id(), job.withStatus("Candidato generado para " + candidate.interventionId(), false));
            frameProgress.setProgress(1);
            frameEstimate.setText("Candidato generado para " + candidate.interventionId() + ".");
            status.setText("Candidato generado para " + candidate.interventionId() + ".");
        }, message -> {
            replaceJobById(job.id(), job.withStatus(message, false));
            frameProgress.setProgress(0);
            frameEstimate.setText(message);
            status.setText(message);
        });
    }

    private void regenerateTransition(TheatreImageGenerationUnit current, TheatreImageGenerationUnit next) {
        if (current == null || next == null) {
            status.setText("Selecciona dos intervenciones consecutivas.");
            return;
        }
        Optional<PrincipalFrameReference> previousReference = principalFrameReference(current.interventionId());
        Optional<PrincipalFrameReference> nextReference = principalFrameReference(next.interventionId());
        if (previousReference.isEmpty() || nextReference.isEmpty()) {
            status.setText("Genera primero los frames principales actual y siguiente.");
            return;
        }
        PrincipalFrameReference previousFrame = previousReference.get();
        PrincipalFrameReference nextFrame = nextReference.get();
        TheatreImageGenerationUnit transition = transitionUnit(current, next);
        TheatreImageGenerationJob job = singleUnitJob(
                "INTERMEDIO-" + current.interventionId() + "-TO-" + next.interventionId(),
                transition,
                "En cola",
                true);
        jobs.add(0, job);
        jobList.getSelectionModel().select(job);
        frameProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        frameEstimate.setText("En cola: frame intermedio " + current.interventionId() + " -> " + next.interventionId() + ".");
        activeModule.set(TheatreAiModuleId.JOBS);
        runAsync(() -> viewModel.generateTheatreTransitionImageCandidate(transition,
                previousFrame.assetId(), previousFrame.path(), nextFrame.assetId(), nextFrame.path(),
                settings(), presetSelector.getValue(),
                selectedOutputProfile(), selectedAspectRatio(),
                message -> Platform.runLater(() -> {
                    frameEstimate.setText(message);
                    replaceJobById(job.id(), job.withStatus(message, true));
                })), generated -> {
            TheatreGeneratedFrameCandidate frame = new TheatreGeneratedFrameCandidate(
                    current.interventionId() + "#frame-2",
                    generated.sceneId(),
                    current.interventionId(),
                    current.segmentId(),
                    2,
                    true,
                    next.interventionId(),
                    generated.assetId(),
                    generated.outputPath(),
                    false);
            frameCandidates.add(0, frame);
            frameCandidateList.getSelectionModel().select(frame);
            frameCandidateList.scrollTo(frame);
            renderGeneratedPreview(frame);
            replaceJobById(job.id(), job.withStatus("Frame intermedio generado", false));
            frameProgress.setProgress(1);
            frameEstimate.setText("Frame intermedio generado para "
                    + current.interventionId() + " -> " + next.interventionId() + ".");
            status.setText("Frame intermedio generado; apruebalo manualmente si deseas conservarlo.");
        }, message -> {
            replaceJobById(job.id(), job.withStatus(message, false));
            frameProgress.setProgress(0);
            frameEstimate.setText(message);
            status.setText(message);
        });
    }

    private void toggleIntermediateFrameGeneration() {
        if (intermediateFrameTask != null && intermediateFrameTask.isRunning()) {
            intermediateFrameTask.cancel();
            frameEstimate.setText("Cancelando generacion de frames intermedios...");
            status.setText("Cancelando generacion de frames intermedios...");
            return;
        }
        if (frameTask != null && frameTask.isRunning()) {
            status.setText("Espera a que termine o cancela el trabajo de frames actual.");
            return;
        }
        TheatreIntermediateFrameBatchPlanner.Plan plan = intermediateBatchPlan();
        if (plan == null) {
            return;
        }
        Optional<IntermediateBatchSelection> selection = confirmIntermediateBatch(plan);
        if (selection.isEmpty()) {
            return;
        }
        startIntermediateFrameBatch(selection.get().items(), selection.get().mode());
    }

    private TheatreIntermediateFrameBatchPlanner.Plan intermediateBatchPlan() {
        Optional<DocuPodcastProject> project = viewModel.currentProject();
        Optional<Path> root = viewModel.currentProjectDirectory();
        if (project.isEmpty() || root.isEmpty()) {
            status.setText("Guarda el proyecto antes de generar frames intermedios.");
            return null;
        }
        List<TheatreImageGenerationUnit> allUnits = viewModel.theatreImageGenerationQueue(TheatreContextExportScope.all());
        units.setAll(allUnits);
        return new TheatreIntermediateFrameBatchPlanner().plan(
                project.get(),
                viewModel.currentStoryboardProperty().get(),
                viewModel.currentScriptProperty().get(),
                allUnits,
                root.get());
    }

    private Optional<IntermediateBatchSelection> confirmIntermediateBatch(TheatreIntermediateFrameBatchPlanner.Plan plan) {
        Alert alert = new Alert(plan.hasAllGenerablePairs()
                ? Alert.AlertType.CONFIRMATION
                : Alert.AlertType.INFORMATION);
        alert.initOwner(getScene() == null ? null : getScene().getWindow());
        alert.setTitle("Frames intermedios");
        alert.setHeaderText("Analisis de frames intermedios de toda la obra");
        alert.setContentText(intermediatePlanSummary(plan));
        if (!plan.hasAllGenerablePairs()) {
            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.showAndWait();
            status.setText("No hay frames intermedios generables con las imagenes actuales.");
            return Optional.empty();
        }
        ButtonType missing = new ButtonType("Generar faltantes", ButtonBar.ButtonData.OK_DONE);
        ButtonType overwrite = new ButtonType("Generar todos / sobrescribir", ButtonBar.ButtonData.APPLY);
        ButtonType smoke = new ButtonType("Probar primer par", ButtonBar.ButtonData.OTHER);
        ArrayList<ButtonType> buttons = new ArrayList<>();
        if (plan.hasMissingGenerablePairs()) {
            buttons.add(missing);
        }
        buttons.add(overwrite);
        buttons.add(smoke);
        buttons.add(ButtonType.CANCEL);
        alert.getButtonTypes().setAll(buttons);
        Optional<ButtonType> selected = alert.showAndWait();
        if (selected.filter(missing::equals).isPresent()) {
            return Optional.of(new IntermediateBatchSelection(
                    IntermediateBatchMode.MISSING_ONLY, plan.missingItems()));
        }
        if (selected.filter(overwrite::equals).isPresent()) {
            return Optional.of(new IntermediateBatchSelection(
                    IntermediateBatchMode.OVERWRITE_ALL, plan.allGenerableItems()));
        }
        if (selected.filter(smoke::equals).isPresent()) {
            List<TheatreIntermediateFrameBatchPlanner.Item> items = plan.hasMissingGenerablePairs()
                    ? List.of(plan.missingItems().getFirst())
                    : List.of(plan.allGenerableItems().getFirst());
            return Optional.of(new IntermediateBatchSelection(IntermediateBatchMode.SMOKE_TEST, items));
        }
        return Optional.empty();
    }

    private static String intermediatePlanSummary(TheatreIntermediateFrameBatchPlanner.Plan plan) {
        return "Transiciones revisadas: " + plan.totalPairs() + "\n"
                + "Frames intermedios ya existentes: " + plan.existingPairs() + "\n"
                + "Frames faltantes generables: " + plan.generablePairs() + "\n"
                + "Frames generables totales: " + plan.allGenerablePairs() + "\n"
                + "Frames existentes que se sobrescribirian: " + plan.overwritePairs() + "\n"
                + "Pares bloqueados: " + plan.blockedPairs() + "\n"
                + "Intervenciones que necesitan imagen principal: " + plan.missingPrincipalInterventions() + "\n\n"
                + (plan.hasAllGenerablePairs()
                ? "Elige si generar solo faltantes, regenerar todos los pares posibles o probar el primer par."
                : "Asigna o genera imagen principal en las intervenciones faltantes para poder completar los intermedios.");
    }

    private void startIntermediateFrameBatch(List<TheatreIntermediateFrameBatchPlanner.Item> requestedItems,
                                             IntermediateBatchMode mode) {
        List<TheatreIntermediateFrameBatchPlanner.Item> selectedItems = requestedItems == null
                ? List.of()
                : List.copyOf(requestedItems);
        if (selectedItems.isEmpty()) {
            status.setText("No hay frames intermedios generables con las imagenes actuales.");
            return;
        }
        Path outputDirectory = intermediateFrameOutputDirectory();
        TheatreImageGenerationJob job = intermediateBatchJob(selectedItems, outputDirectory, "En cola", true, mode);
        jobs.add(0, job);
        jobList.getSelectionModel().select(job);
        activeModule.set(TheatreAiModuleId.JOBS);
        frameProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        frameEstimate.setText("En cola: " + selectedItems.size() + " " + intermediateQueuedLabel(mode) + ".");
        status.setText(intermediateRunningStatus(mode));
        setIntermediateBatchRunning(true);
        AtomicInteger generatedCount = new AtomicInteger();
        intermediateFrameTask = new Task<>() {
            @Override protected IntermediateBatchResult call() throws Exception {
                ArrayList<TheatreGeneratedFrameCandidate> approvedFrames = new ArrayList<>();
                ComfyUiConnectionSettings generationSettings = intermediateFrameSettings(outputDirectory);
                Platform.runLater(() -> {
                    frameEstimate.setText("Verificando motor local de imagen IA...");
                    replaceJobById(job.id(), job.withStatus("Verificando motor local", true));
                });
                ensureIntermediateFrameEngineAvailable(generationSettings);
                int total = selectedItems.size();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    TheatreIntermediateFrameBatchPlanner.Item item = selectedItems.get(i);
                    String pairLabel = item.current().interventionId() + " -> " + item.next().interventionId();
                    int currentIndex = i + 1;
                    String actionLabel = intermediateActionLabel(mode, item);
                    Platform.runLater(() -> {
                        frameEstimate.setText(actionLabel + " " + currentIndex + "/" + total + ": " + pairLabel + ".");
                        replaceJobById(job.id(), job.withStatus(actionLabel + " " + pairLabel, true));
                    });
                    TheatreGeneratedFrameCandidate candidate = viewModel.generateTheatreRifeIntermediateFrameCandidate(
                            item,
                            generationSettings,
                            message -> Platform.runLater(() -> {
                                frameEstimate.setText(message);
                                replaceJobById(job.id(), job.withStatus(message, true));
                            }));
                    TheatreGeneratedFrameCandidate approved = viewModel.approveTheatreGeneratedFrameCandidate(candidate);
                    approvedFrames.add(approved);
                    generatedCount.incrementAndGet();
                    updateProgress(generatedCount.get(), Math.max(1, total));
                    Platform.runLater(() -> {
                        frameCandidates.add(0, approved);
                        frameCandidateList.getSelectionModel().select(approved);
                        frameCandidateList.scrollTo(approved);
                        renderGeneratedPreview(approved);
                        updateSelectedFrameOverview();
                        frameProgress.setProgress((double) generatedCount.get() / Math.max(1, total));
                    });
                }
                return new IntermediateBatchResult(generatedCount.get(), selectedItems.size(), approvedFrames);
            }
        };
        intermediateFrameTask.setOnSucceeded(event -> {
            IntermediateBatchResult result = intermediateFrameTask.getValue();
            saveProjectIfPossible();
            frameProgress.setProgress(1);
            String message = intermediateCompletionMessage(mode, result.generated());
            frameEstimate.setText(message);
            replaceJobById(job.id(), job.withStatus(message, false));
            status.setText(message);
            setIntermediateBatchRunning(false);
        });
        intermediateFrameTask.setOnCancelled(event -> {
            saveProjectIfPossible();
            frameProgress.setProgress(0);
            frameEstimate.setText("Generacion de frames intermedios cancelada.");
            replaceJobById(job.id(), job.withStatus("Cancelado. Guardados: " + generatedCount.get(), false));
            status.setText("Generacion cancelada. Los frames completados se conservaron.");
            setIntermediateBatchRunning(false);
        });
        intermediateFrameTask.setOnFailed(event -> {
            saveProjectIfPossible();
            Throwable ex = intermediateFrameTask.getException();
            String message = intermediateFailurePrefix(mode)
                    + (ex == null ? "error desconocido" : ex.getMessage());
            frameProgress.setProgress(0);
            frameEstimate.setText(message);
            replaceJobById(job.id(), job.withStatus(message, false));
            status.setText(message);
            setIntermediateBatchRunning(false);
        });
        Thread worker = new Thread(intermediateFrameTask, "docupodcast-theatre-ai-intermediate-frames");
        worker.setDaemon(true);
        worker.start();
    }

    private TheatreImageGenerationJob intermediateBatchJob(List<TheatreIntermediateFrameBatchPlanner.Item> selectedItems,
                                                           Path outputDirectory,
                                                           String jobStatus,
                                                           boolean running,
                                                           IntermediateBatchMode mode) {
        List<TheatreImageGenerationJobItem> items = selectedItems.stream()
                .map(item -> new TheatreImageGenerationJobItem(
                        item.current().interventionId(),
                        item.current().sceneId(),
                        item.current().interventionId() + " -> " + item.next().interventionId(),
                        "pendiente",
                        null))
                .toList();
        String idPrefix = mode == IntermediateBatchMode.SMOKE_TEST ? "PRUEBA-INTERMEDIO-" : "INTERMEDIOS-";
        return new TheatreImageGenerationJob(idPrefix + (jobs.size() + 1),
                TheatreFrameGenerationScope.all(),
                FrameGenerationMode.DOUBLE_STOP_MOTION,
                presetSelector.getValue(),
                outputDirectory,
                items,
                List.of("PREVIOUS_FRAME", "NEXT_FRAME"),
                jobStatus,
                running);
    }

    private void ensureIntermediateFrameEngineAvailable(ComfyUiConnectionSettings settings) throws IOException {
        var result = viewModel.testComfyUi(settings);
        if (!result.available()) {
            throw new IOException(result.message() + "\n" + result.diagnostic());
        }
    }

    private static String intermediateQueuedLabel(IntermediateBatchMode mode) {
        return switch (mode) {
            case MISSING_ONLY -> "frames intermedios faltantes";
            case OVERWRITE_ALL -> "frames intermedios para generar o sobrescribir";
            case SMOKE_TEST -> "prueba de frame intermedio";
        };
    }

    private static String intermediateRunningStatus(IntermediateBatchMode mode) {
        return switch (mode) {
            case MISSING_ONLY -> "Generando frames intermedios faltantes...";
            case OVERWRITE_ALL -> "Generando todos los frames intermedios posibles...";
            case SMOKE_TEST -> "Probando generacion de frame intermedio...";
        };
    }

    private static String intermediateActionLabel(IntermediateBatchMode mode,
                                                  TheatreIntermediateFrameBatchPlanner.Item item) {
        return switch (mode) {
            case MISSING_ONLY -> "Generando faltante";
            case OVERWRITE_ALL -> item.existingIntermediate() ? "Sobrescribiendo" : "Generando faltante";
            case SMOKE_TEST -> item.existingIntermediate() ? "Probando y sobrescribiendo" : "Probando";
        };
    }

    private static String intermediateCompletionMessage(IntermediateBatchMode mode, int generated) {
        return switch (mode) {
            case MISSING_ONLY -> generated + " frames intermedios faltantes generados y guardados.";
            case OVERWRITE_ALL -> generated + " frames intermedios generados o sobrescritos y guardados.";
            case SMOKE_TEST -> generated + " prueba de frame intermedio generada y guardada.";
        };
    }

    private static String intermediateFailurePrefix(IntermediateBatchMode mode) {
        return mode == IntermediateBatchMode.SMOKE_TEST
                ? "No se pudo probar frame intermedio: "
                : "No se pudieron generar frames intermedios: ";
    }

    private Path intermediateFrameOutputDirectory() {
        return viewModel.currentProjectDirectory()
                .map(root -> root.resolve("generated").resolve("teatro-ia").resolve("intermedios"))
                .orElseGet(() -> settings().outputDirectory());
    }

    private ComfyUiConnectionSettings intermediateFrameSettings(Path outputDirectory) {
        ComfyUiConnectionSettings base = settings();
        Path output = outputDirectory == null ? base.outputDirectory() : outputDirectory;
        return new ComfyUiConnectionSettings(base.baseUrl(), base.timeout(), output);
    }

    private void saveProjectIfPossible() {
        viewModel.currentProjectFile().ifPresent(file -> {
            try {
                viewModel.saveCurrentProjectAs(file);
            } catch (IOException ex) {
                status.setText("Frames guardados en memoria, pero no se pudo guardar el proyecto: " + ex.getMessage());
            }
        });
    }

    private void setIntermediateBatchRunning(boolean running) {
        intermediateBatchButton.setText(running
                ? "Detener generacion de frames intermedios"
                : "Generar frames intermedios");
    }

    private void approveSelectedImageCandidate() {
        TheatreGeneratedImageCandidate candidate = imageCandidateList.getSelectionModel().getSelectedItem();
        if (candidate == null) {
            status.setText("Selecciona un candidato.");
            return;
        }
        TheatreGeneratedImageCandidate approved = viewModel.approveTheatreGeneratedImageCandidate(candidate);
        imageCandidates.set(imageCandidates.indexOf(candidate), approved);
        imageCandidateList.getSelectionModel().select(approved);
        renderGeneratedPreview(approved);
        updateSelectedFrameOverview();
        status.setText("Candidato aprobado para " + approved.interventionId() + ".");
    }

    private void enhanceSelectedCandidate(ImageEnhancementOutputProfile profile,
                                          ImageAspectStrategy strategy,
                                          String label) {
        TheatreGeneratedImageCandidate candidate = imageCandidateList.getSelectionModel().getSelectedItem();
        if (candidate == null) {
            status.setText("Selecciona un candidato para mejorar.");
            return;
        }
        TheatreImageGenerationJob job = enhancementJob(candidate, profile, label, true);
        jobs.add(0, job);
        jobList.getSelectionModel().select(job);
        frameProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        frameEstimate.setText(label + ": " + candidate.interventionId() + " -> " + profile.displayName() + ".");
        status.setText(label + "...");
        activeModule.set(TheatreAiModuleId.JOBS);
        runAsync(() -> viewModel.enhanceTheatreImageCandidate(candidate, profile, strategy), enhanced -> {
            imageCandidates.add(0, enhanced);
            imageCandidateList.getSelectionModel().select(enhanced);
            imageCandidateList.scrollTo(enhanced);
            renderGeneratedPreview(enhanced);
            replaceJob(job, enhancementJob(enhanced, profile, "Mejora generada: " + profile.displayName(), false));
            frameProgress.setProgress(1);
            frameEstimate.setText("Mejora lista: " + profile.displayName() + ".");
            status.setText("Imagen mejorada para " + enhanced.interventionId() + ".");
        }, message -> {
            replaceJob(job, job.withStatus(message, false));
            frameProgress.setProgress(0);
            frameEstimate.setText(message);
            status.setText(message);
        });
    }

    private TheatreImageGenerationJob enhancementJob(TheatreGeneratedImageCandidate candidate,
                                                     ImageEnhancementOutputProfile profile,
                                                     String jobStatus,
                                                     boolean running) {
        TheatreImageGenerationJobItem item = new TheatreImageGenerationJobItem(
                candidate.interventionId(),
                candidate.sceneId(),
                candidate.interventionId() + " - " + profile.displayName(),
                jobStatus,
                candidate.outputPath());
        return new TheatreImageGenerationJob(
                "MEJORA-" + candidate.interventionId() + "-" + profile.name() + "-" + (jobs.size() + 1),
                TheatreFrameGenerationScope.intervention(candidate.interventionId()),
                FrameGenerationMode.SINGLE,
                presetSelector.getValue(),
                outputFolderFor(candidate.outputPath()),
                List.of(item),
                jobStatus,
                running);
    }

    private void approveSelectedFrameCandidate() {
        TheatreGeneratedFrameCandidate candidate = frameCandidateList.getSelectionModel().getSelectedItem();
        if (candidate == null) {
            status.setText("Selecciona un frame.");
            return;
        }
        TheatreGeneratedFrameCandidate approved = viewModel.approveTheatreGeneratedFrameCandidate(candidate);
        frameCandidates.set(frameCandidates.indexOf(candidate), approved);
        frameCandidateList.getSelectionModel().select(approved);
        renderGeneratedPreview(approved);
        updateSelectedFrameOverview();
        if (approved.transitionFrame()) {
            status.setText("Frame inferido guardado para " + approved.interventionId()
                    + " -> " + approved.nextInterventionId() + ".");
        } else {
            status.setText("Frame aprobado para " + approved.interventionId() + ".");
        }
    }

    private void exportContextForAlias(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        if (alias == null) {
            return;
        }
        selectedIntervention.set(alias.alias());
        exportSelectedContextPackage();
    }

    private void exportSelectedContextPackage() {
        TheatreImageGenerationUnit unit = selectedUnit();
        if (unit == null) {
            status.setText("Selecciona una intervencion.");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Carpeta para paquete IA");
        File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return;
        }
        try {
            Path folder = viewModel.exportTheatreInterventionContext(unit.sceneId(), unit.interventionId(), selected.toPath());
            status.setText("Paquete creado: " + folder + ".");
        } catch (IOException | RuntimeException ex) {
            status.setText("No se pudo exportar paquete: " + ex.getMessage());
        }
    }

    private void exportBulkContextPackages() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Carpeta para paquetes IA teatrales");
        File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return;
        }
        TheatreContextExportEstimate estimate = viewModel.estimateTheatreContextPackages(TheatreContextExportScope.all());
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Exportar paquetes IA");
        confirm.setHeaderText("Se crearan " + estimate.packages() + " paquetes.");
        confirm.setContentText("Estimacion: " + humanBytes(estimate.estimatedBytes()) + ". La exportacion duplica recursos por intervencion.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }
        try {
            var result = viewModel.exportTheatreContextPackages(TheatreContextExportScope.all(), selected.toPath());
            status.setText("Paquetes creados: " + result.packages() + " en " + result.root() + ".");
        } catch (IOException | RuntimeException ex) {
            status.setText("No se pudieron exportar paquetes: " + ex.getMessage());
        }
    }

    private boolean chooseFrameOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Carpeta de salida para frames");
        File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return false;
        }
        frameOutputDirectory.set(selected.toPath());
        outputFolderLabel.setText(selected.toPath().toString());
        estimateFrames();
        return true;
    }

    private boolean ensureFrameOutputDirectory() {
        return frameOutputDirectory.get() != null || chooseFrameOutputDirectory();
    }

    private void estimateFrames() {
        estimateFrames(TheatreFrameGenerationScope.all());
    }

    private void estimateFrames(TheatreFrameGenerationScope scope) {
        try {
            TheatreFrameGenerationRequest request = frameRequest(scope);
            var estimate = viewModel.estimateTheatreFrames(request);
            frameEstimate.setText(estimate.interventions() + " intervenciones, " + estimate.frames()
                    + " frames planificados, " + estimate.pending() + " pendientes esperados.");
        } catch (RuntimeException ex) {
            frameEstimate.setText("No se pudo estimar: " + ex.getMessage());
        }
    }

    private void generateFrames() {
        processScope(TheatreFrameGenerationScope.all(), "Obra completa");
    }

    private void generateFrames(TheatreFrameGenerationScope scope, String scopeLabel) {
        TheatreFrameGenerationRequest request;
        try {
            request = frameRequest(scope);
        } catch (RuntimeException ex) {
            status.setText(ex.getMessage());
            return;
        }
        if (request.outputDirectory() == null) {
            status.setText("Trabajo cancelado: no se eligio carpeta de salida.");
            return;
        }
        frameProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        frameEstimate.setText("En cola: " + scopeLabel + ".");
        TheatreImageGenerationJob job = jobForRequest(request, "En cola", true);
        jobs.add(0, job);
        jobList.getSelectionModel().select(job);
        activeModule.set(TheatreAiModuleId.JOBS);
        frameTask = new Task<>() {
            @Override protected TheatreFrameGenerationResult call() throws Exception {
                return viewModel.generateTheatreFrames(request,
                        message -> Platform.runLater(() -> {
                            frameEstimate.setText(message);
                            replaceJobById(job.id(), job.withStatus(message, true));
                        }),
                        this::isCancelled);
            }
        };
        frameTask.setOnSucceeded(event -> {
            TheatreFrameGenerationResult result = frameTask.getValue();
            List<TheatreGeneratedFrameCandidate> candidates = result.candidates().stream()
                    .map(this::autoAssignGeneratedFrame)
                    .toList();
            frameCandidates.addAll(candidates);
            frameProgress.setProgress(1);
            frameEstimate.setText(result.generatedFrames() + " frames generados. Manifest: " + result.manifest() + ".");
            replaceJob(job, job.withStatus(result.generatedFrames() + " frames generados", false));
            activeModule.set(TheatreAiModuleId.JOBS);
        });
        frameTask.setOnFailed(event -> {
            frameProgress.setProgress(0);
            Throwable ex = frameTask.getException();
            String message = "No se pudieron generar frames: " + (ex == null ? "error desconocido" : ex.getMessage());
            frameEstimate.setText(message);
            replaceJob(job, job.withStatus(message, false));
        });
        Thread worker = new Thread(frameTask, "docupodcast-theatre-ai-frames");
        worker.setDaemon(true);
        worker.start();
    }

    private TheatreGeneratedFrameCandidate autoAssignGeneratedFrame(TheatreGeneratedFrameCandidate candidate) {
        if (candidate == null || candidate.frameIndex() != 1 || candidate.transitionFrame()) {
            return candidate;
        }
        try {
            return viewModel.approveTheatreGeneratedFrameCandidate(candidate);
        } catch (RuntimeException ex) {
            status.setText("Frame generado, pero no se pudo asignar a " + candidate.interventionId() + ": " + ex.getMessage());
            return candidate;
        }
    }

    private void cancelFrameGeneration() {
        if (frameTask != null && frameTask.isRunning()) {
            frameTask.cancel();
            frameEstimate.setText("Cancelando trabajo...");
        }
    }

    private void retrySelectedJob() {
        TheatreImageGenerationJob job = jobList.getSelectionModel().getSelectedItem();
        if (job != null) {
            applyJobConfiguration(job);
            frameEstimate.setText("Reintentando " + job.id() + ".");
            processScope(job.scope(), scopeLabel(job.scope()));
            return;
        }
        status.setText("Selecciona un trabajo para reintentar.");
    }

    private void openSelectedJobFolder() {
        Path folder = selectedOutputFolder();
        if (folder == null) {
            status.setText("No hay carpeta de trabajo seleccionada.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
                status.setText("Carpeta abierta: " + folder + ".");
            } else {
                status.setText("El sistema no permite abrir carpetas automaticamente: " + folder + ".");
            }
        } catch (IOException | RuntimeException ex) {
            status.setText("No se pudo abrir la carpeta: " + ex.getMessage());
        }
    }

    private Path selectedOutputFolder() {
        TheatreGeneratedImageCandidate imageCandidate = imageCandidateList.getSelectionModel().getSelectedItem();
        Path imageFolder = outputFolderFor(imageCandidate == null ? null : imageCandidate.outputPath());
        if (imageFolder != null) {
            return imageFolder;
        }
        TheatreGeneratedFrameCandidate frameCandidate = frameCandidateList.getSelectionModel().getSelectedItem();
        Path frameFolder = outputFolderFor(frameCandidate == null ? null : frameCandidate.outputPath());
        if (frameFolder != null) {
            return frameFolder;
        }
        TheatreImageGenerationJob job = jobList.getSelectionModel().getSelectedItem();
        if (job != null) {
            if (job.outputDirectory() != null) {
                return job.outputDirectory();
            }
            for (TheatreImageGenerationJobItem item : job.items()) {
                Path folder = outputFolderFor(item.outputPath());
                if (folder != null) {
                    return folder;
                }
            }
        }
        return frameOutputDirectory.get();
    }

    private static Path outputFolderFor(Path outputPath) {
        if (outputPath == null) {
            return null;
        }
        if (Files.isDirectory(outputPath)) {
            return outputPath;
        }
        return outputPath.getParent();
    }

    private TheatreImageGenerationJob singleUnitJob(String id, TheatreImageGenerationUnit unit, String jobStatus, boolean running) {
        TheatreImageGenerationJobItem item = new TheatreImageGenerationJobItem(
                unit.interventionId(),
                unit.sceneId(),
                unit.interventionId() + " - " + unit.speaker(),
                jobStatus,
                null);
        return new TheatreImageGenerationJob(id, TheatreFrameGenerationScope.intervention(unit.interventionId()),
                FrameGenerationMode.SINGLE, presetSelector.getValue(), frameOutputDirectory.get(), List.of(item),
                referenceLabels(List.of(unit)), jobStatus, running);
    }

    private TheatreImageGenerationJob jobForRequest(TheatreFrameGenerationRequest request, String jobStatus, boolean running) {
        List<TheatreImageGenerationUnit> matchingUnits = units.stream()
                .filter(unit -> matchesScope(unit, request.scope()))
                .toList();
        List<TheatreImageGenerationJobItem> items = matchingUnits.stream()
                .map(unit -> new TheatreImageGenerationJobItem(
                        unit.interventionId(),
                        unit.sceneId(),
                        unit.interventionId() + " - " + unit.speaker(),
                        "pendiente",
                        null))
                .toList();
        return new TheatreImageGenerationJob("TRABAJO-" + (jobs.size() + 1), request.scope(), request.mode(),
                request.preset(), request.outputDirectory(), items, referenceLabels(matchingUnits), jobStatus, running);
    }

    private List<String> referenceLabels(List<TheatreImageGenerationUnit> targetUnits) {
        return targetUnits.stream()
                .flatMap(unit -> viewModel.theatreImageGenerationContextAssets(unit).stream())
                .map(asset -> asset.role() + ": " + asset.label())
                .distinct()
                .toList();
    }

    private static boolean matchesScope(TheatreImageGenerationUnit unit, TheatreFrameGenerationScope scope) {
        if (unit == null || scope == null || scope.isAll()) {
            return true;
        }
        return switch (scope.kind()) {
            case ACT -> unit.actId().equals(scope.id());
            case SCENE -> unit.sceneId().equals(scope.id());
            case INTERVENTION -> unit.interventionId().equals(scope.id());
            case ALL -> true;
        };
    }

    private void replaceJob(TheatreImageGenerationJob oldJob, TheatreImageGenerationJob newJob) {
        int index = jobs.indexOf(oldJob);
        if (index >= 0) {
            jobs.set(index, newJob);
            jobList.getSelectionModel().select(newJob);
        }
    }

    private void replaceJobById(String jobId, TheatreImageGenerationJob newJob) {
        String id = jobId == null ? "" : jobId;
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).id().equals(id)) {
                jobs.set(i, newJob);
                jobList.getSelectionModel().select(newJob);
                return;
            }
        }
    }

    private void applyJobConfiguration(TheatreImageGenerationJob job) {
        if (job == null) {
            return;
        }
        frameModeSelector.setValue(job.mode());
        presetSelector.setValue(job.preset());
        frameOutputDirectory.set(job.outputDirectory());
        outputFolderLabel.setText(job.outputDirectory() == null ? "Sin carpeta seleccionada." : job.outputDirectory().toString());
        TheatreFrameGenerationScope scope = job.scope();
        if (scope != null && scope.kind() == TheatreFrameGenerationScope.Kind.INTERVENTION) {
            selectedIntervention.set(scope.id());
        }
    }

    private void restoreDefaultPrompt() {
        TheatreImageGenerationUnit unit = selectedUnit();
        if (unit == null) {
            status.setText("Selecciona una intervencion.");
            return;
        }
        promptDrafts.remove(unit.interventionId());
        setPromptText(defaultPrompt(unit));
        status.setText("Prompt base restaurado para " + unit.interventionId() + ".");
    }

    private void saveContextText() {
        TheatreImageGenerationUnit unit = selectedUnit();
        if (unit == null) {
            status.setText("Selecciona una intervencion.");
            return;
        }
        viewModel.saveTheatreContextText(unit.segmentId(), contextPreview.getText());
        promptDrafts.remove(unit.interventionId());
        setPromptText(promptFor(unit));
        status.setText("Contexto textual guardado para " + unit.interventionId() + ".");
    }

    private TheatreFrameGenerationRequest frameRequest(TheatreFrameGenerationScope scope) {
        return new TheatreFrameGenerationRequest(
                scope == null ? TheatreFrameGenerationScope.all() : scope,
                frameModeSelector.getValue(),
                frameOutputDirectory.get(),
                presetSelector.getValue(),
                selectedOutputProfile(),
                selectedAspectRatio(),
                settings(),
                false);
    }

    private ComfyUiConnectionSettings settings() {
        Path output = viewModel.currentProjectDirectory().map(path -> path.resolve("generated/teatro-ia")).orElse(null);
        ImageGenerationSettings imageSettings;
        try {
            imageSettings = viewModel.applicationServices().settings().loadOperationalSettings().load().imageGeneration();
        } catch (IOException ex) {
            imageSettings = ImageGenerationSettings.defaults();
        }
        return new ComfyUiConnectionSettings(
                imageSettings.baseUrl(),
                Duration.ofSeconds(Math.max(
                        ImageGenerationSettings.DEFAULT_TIMEOUT_SECONDS,
                        imageSettings.timeoutSeconds())),
                output);
    }

    private TheatreImageGenerationPreset currentPreset() {
        try {
            ImageGenerationSettings imageSettings = viewModel.applicationServices().settings().loadOperationalSettings().load().imageGeneration();
            return TheatreImageGenerationPreset.valueOf(imageSettings.preset());
        } catch (IOException | RuntimeException ex) {
            return TheatreImageGenerationPreset.TEST_4GB_SD15;
        }
    }

    private ImageGenerationMemoryProfile currentMemoryProfile() {
        try {
            return viewModel.applicationServices().settings().loadOperationalSettings().load().imageGeneration().memoryProfileValue();
        } catch (IOException | RuntimeException ex) {
            return ImageGenerationMemoryProfile.SAFE_LOW_VRAM;
        }
    }

    private ImageGenerationMemoryProfile selectedMemoryProfile() {
        ImageGenerationMemoryProfile selected = memoryProfileSelector.getValue();
        return selected == null ? currentMemoryProfile() : selected;
    }

    private ImageEnhancementOutputProfile selectedOutputProfile() {
        ImageEnhancementOutputProfile selected = outputProfileSelector.getValue();
        return selected == null ? ImageEnhancementOutputProfile.FHD_1080 : selected;
    }

    private TheatreImageAspectRatio selectedAspectRatio() {
        TheatreImageAspectRatio selected = aspectRatioSelector.getValue();
        return selected == null ? TheatreImageAspectRatio.WIDE_16_9 : selected;
    }

    private static String outputProfileLabel(ImageEnhancementOutputProfile profile) {
        if (profile == null) {
            return "1080p - 1920x1080";
        }
        return profile.displayName() + " - " + profile.width() + "x" + profile.height();
    }

    private String imageEngineSummary() {
        try {
            return viewModel.applicationServices().settings().inspectLocalTheatreImageEngine()
                    .inspect(operationalSettingsForEngineTest(), RuntimePathResolver.defaultResolver().resolve().applicationRoot())
                    .statusLabel();
        } catch (RuntimeException ex) {
            return "Motor local de imagen IA. Verifica antes de generar.";
        }
    }

    private OperationalSettings operationalSettings() {
        try {
            return viewModel.applicationServices().settings().loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }

    private OperationalSettings operationalSettingsForEngineTest() {
        OperationalSettings current = operationalSettings();
        ImageGenerationSettings image = current.imageGeneration();
        TheatreImageGenerationPreset preset = presetSelector.getValue() == null
                ? currentPreset()
                : presetSelector.getValue();
        ImageGenerationMemoryProfile memory = selectedMemoryProfile();
        ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(preset.name());
        String modelName = profile.checkpointName().isBlank()
                ? image.modelName()
                : profile.checkpointName();
        ImageGenerationSettings imageSettings = new ImageGenerationSettings(
                image.engineMode(),
                image.baseUrl(),
                image.devicePolicy(),
                preset.name(),
                modelName,
                image.adaptersDirectory(),
                Math.max(ImageGenerationSettings.DEFAULT_TIMEOUT_SECONDS, image.timeoutSeconds()),
                memory.legacyLowVram(),
                memory.name(),
                image.maxAttempts());
        return new OperationalSettings(
                current.readingDocument(),
                current.playbackBuffer(),
                current.tts(),
                current.video(),
                imageSettings,
                current.frameGeneration(),
                current.compute(),
                current.ocr(),
                current.storage(),
                current.diagnostics());
    }

    private TheatreImageGenerationUnit unitWithPrompt(TheatreImageGenerationUnit unit, String prompt) {
        String text = prompt == null || prompt.isBlank() ? unit.fullText() : prompt.strip();
        return new TheatreImageGenerationUnit(unit.actId(), unit.actName(), unit.sceneId(), unit.sceneName(),
                unit.interventionId(), unit.segmentId(), unit.speaker(), text, unit.spatialContextText(), unit.contextPackage());
    }

    private static TheatreImageGenerationUnit transitionUnit(TheatreImageGenerationUnit current,
                                                             TheatreImageGenerationUnit next) {
        String id = current.interventionId() + "-to-" + next.interventionId();
        String text = "Temporal interpolation frame between the supplied previous frame and next frame. "
                + "Preserve identity, costume, props, staging and lighting from the two image references.";
        return new TheatreImageGenerationUnit(
                current.actId(),
                current.actName(),
                current.sceneId(),
                current.sceneName(),
                id,
                current.segmentId() + "-to-" + next.segmentId(),
                current.speaker() + " / " + next.speaker(),
                text,
                "",
                null);
    }

    private static String normalizedText(String value) {
        return value == null || value.isBlank() ? "Sin disposicion espacial adicional." : value.strip();
    }

    private static String spatialContextForDisplay(String value) {
        String normalized = normalizedText(value);
        String heading = "Disposicion espacial configurada:";
        return normalized.startsWith(heading)
                ? normalized.substring(heading.length()).strip()
                : normalized;
    }

    private void savePromptDraft(String interventionId) {
        savePromptDraft(interventionId, promptEditor.getText());
    }

    private void savePromptDraft(String interventionId, String text) {
        if (interventionId == null || interventionId.isBlank() || text == null) {
            return;
        }
        promptDrafts.put(interventionId, text);
    }

    private String promptFor(TheatreImageGenerationUnit unit) {
        String draft = promptDrafts.get(unit.interventionId());
        if (draft != null && !draft.isBlank()) {
            return draft;
        }
        String saved = viewModel.theatreContextTextOverride(unit.segmentId());
        return saved == null || saved.isBlank() ? defaultPrompt(unit) : saved;
    }

    private String contextPreviewText(TheatreImageGenerationUnit unit) {
        String saved = viewModel.theatreContextTextOverride(unit.segmentId());
        if (saved != null && !saved.isBlank()) {
            return saved;
        }
        return "Acto: " + unit.actName()
                + "\nEscena: " + unit.sceneName()
                + "\nCharacter: " + unit.speaker()
                + "\nReferencias aplicadas al motor: " + contextAssets.size()
                + backdropInstructionForDisplay()
                + "\n\nDisposicion espacial configurada:\n"
                + (unit.spatialContextText() == null || unit.spatialContextText().isBlank()
                ? "Sin disposicion espacial adicional para esta intervencion."
                : spatialContextForDisplay(unit.spatialContextText()))
                + "\n\nTexto completo:\n" + unit.fullText();
    }

    private void setPromptText(String text) {
        updatingPrompt = true;
        try {
            promptEditor.setText(text == null ? "" : text);
        } finally {
            updatingPrompt = false;
        }
    }

    private static String defaultPrompt(TheatreImageGenerationUnit unit) {
        return "Escena teatral coherente para " + unit.sceneName()
                + ". Personaje hablante: " + unit.speaker()
                + ". Mantener caras, vestuario, objetos y ubicacion escenica segun contexto. "
                + "Si hay referencia de fondo de escenario, usarla como telon o paisaje de fondo del escenario, no como objeto principal.\n\n"
                + unit.fullText();
    }

    private String backdropInstructionForDisplay() {
        return contextAssets.stream()
                .filter(asset -> "entorno".equals(asset.role()))
                .map(TheatreImageContextAsset::label)
                .filter(label -> label != null && !label.isBlank())
                .findFirst()
                .map(label -> "\nFondo de escenario: usa \"" + label
                        + "\" como telon o paisaje de fondo del escenario, no como objeto principal.")
                .orElse("");
    }

    private Node selectedUnitLabel() {
        TheatreImageGenerationUnit unit = selectedUnit();
        return note(unit == null ? "Sin intervencion seleccionada." : unit.interventionId() + " - " + unit.speaker());
    }

    private static String scopeLabel(TheatreFrameGenerationScope scope) {
        if (scope == null || scope.isAll()) {
            return "Obra";
        }
        return switch (scope.kind()) {
            case ACT -> "Acto";
            case SCENE -> "Escena";
            case INTERVENTION -> "Intervencion";
            case ALL -> "Obra";
        };
    }

    private static FlowPane actionFlow(Node... nodes) {
        FlowPane flow = new FlowPane(8, 8);
        flow.setAlignment(Pos.CENTER_LEFT);
        flow.getStyleClass().add("ui-action-bar");
        flow.getChildren().addAll(nodes);
        return flow;
    }

    private static Label statusLine(String label, String value) {
        Label text = new Label(label + ": " + value);
        text.setWrapText(true);
        text.getStyleClass().add("voice-library-body");
        return text;
    }

    private Node devicePerformanceLine() {
        Label prefix = new Label("Dispositivo: Segun Configuracion >");
        prefix.setWrapText(true);
        prefix.getStyleClass().add("voice-library-body");
        Hyperlink performance = new Hyperlink("Rendimiento");
        performance.getStyleClass().add("voice-library-body");
        performance.setOnAction(event -> openPerformanceSettings());
        HBox line = new HBox(4, prefix, performance);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-text");
        return label;
    }

    private static void setNodeVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private static HBox row(String label, Node field) {
        Label l = new Label(label);
        l.setMinWidth(110);
        HBox row = new HBox(8, l, field);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField textField) {
            textField.setMaxWidth(Double.MAX_VALUE);
        }
        if (field instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.getStyleClass().add("voice-library-combo");
        }
        return row;
    }

    private static Node thumbnail(TheatreImageContextAsset item) {
        if (item.imageUri() == null || item.imageUri().isBlank()) {
            Label missing = new Label("Sin miniatura");
            missing.setMinSize(96, 68);
            missing.setAlignment(Pos.CENTER);
            missing.getStyleClass().add("theatre-ai-missing-thumb");
            return missing;
        }
        ImageView view = new ImageView(new Image(item.imageUri(), 96, 68, true, true, true));
        view.setFitWidth(96);
        view.setFitHeight(68);
        view.setPreserveRatio(true);
        return view;
    }

    private void renderGeneratedPreview(TheatreGeneratedImageCandidate candidate) {
        if (candidate == null) {
            clearGeneratedPreview("Selecciona un candidato o frame.");
            return;
        }
        renderGeneratedPreview(
                (candidate.approved() ? "Candidato aprobado " : "Candidato ")
                        + candidate.interventionId() + " - " + candidate.assetId(),
                candidate.outputPath());
    }

    private void renderGeneratedPreview(TheatreGeneratedFrameCandidate candidate) {
        if (candidate == null) {
            clearGeneratedPreview("Selecciona un candidato o frame.");
            return;
        }
        String type = candidate.transitionFrame() ? "Frame intermedio " : "Frame ";
        renderGeneratedPreview(
                (candidate.approved() ? "Frame aprobado " : type)
                        + candidate.interventionId() + " #" + candidate.frameIndex(),
                candidate.outputPath());
    }

    private void renderGeneratedPreview(String title, Path outputPath) {
        generatedPreviewTitle.setText(title == null || title.isBlank() ? "Frame seleccionado" : title);
        if (outputPath == null) {
            clearGeneratedPreview("El elemento seleccionado no tiene ruta de imagen.");
            return;
        }
        generatedPreviewPath.setText(outputPath.toString());
        if (!Files.isRegularFile(outputPath)) {
            generatedPreviewImage.setImage(null);
            generatedPreviewImage.setVisible(false);
            generatedPreviewPlaceholder.setText("PNG no encontrado");
            generatedPreviewPlaceholder.setVisible(true);
            return;
        }
        Image image = new Image(outputPath.toUri().toString(), false);
        if (image.isError()) {
            generatedPreviewImage.setImage(null);
            generatedPreviewImage.setVisible(false);
            generatedPreviewPlaceholder.setText("No se pudo cargar la imagen");
            generatedPreviewPlaceholder.setVisible(true);
            return;
        }
        generatedPreviewImage.setImage(image);
        generatedPreviewImage.setVisible(true);
        generatedPreviewPlaceholder.setVisible(false);
    }

    private void clearGeneratedPreview(String message) {
        generatedPreviewTitle.setText(message == null || message.isBlank() ? "Selecciona un candidato o frame." : message);
        generatedPreviewPath.setText("El visor mostrara el PNG asociado al elemento seleccionado.");
        generatedPreviewImage.setImage(null);
        generatedPreviewImage.setVisible(false);
        generatedPreviewPlaceholder.setText("Sin frame seleccionado");
        generatedPreviewPlaceholder.setVisible(true);
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }

    private <T> void runAsync(ThrowingSupplier<T> supplier, java.util.function.Consumer<T> onSuccess, java.util.function.Consumer<String> onFailure) {
        beginGenerationTiming();
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return supplier.get(); } };
        task.setOnSucceeded(event -> {
            finishGenerationTiming();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            finishGenerationTiming();
            Throwable ex = task.getException();
            onFailure.accept("No se pudo completar: " + (ex == null ? "error desconocido" : ex.getMessage()));
        });
        Thread thread = new Thread(task, "docupodcast-theatre-ai-image");
        thread.setDaemon(true);
        thread.start();
    }

    private void configureGenerationElapsedLabel(Label label) {
        label.textProperty().bind(generationElapsedText);
        label.getStyleClass().add("voice-library-body");
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
    }

    private void beginGenerationTiming() {
        if (activeGenerationOperations++ > 0) {
            return;
        }
        generationStartedNanos = System.nanoTime();
        setGenerationElapsedVisible(true);
        refreshGenerationElapsed();
        if (generationElapsedTimeline == null) {
            generationElapsedTimeline = new Timeline(new KeyFrame(
                    javafx.util.Duration.seconds(1),
                    event -> refreshGenerationElapsed()));
            generationElapsedTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        generationElapsedTimeline.playFromStart();
    }

    private void finishGenerationTiming() {
        activeGenerationOperations = Math.max(0, activeGenerationOperations - 1);
        refreshGenerationElapsed();
        if (activeGenerationOperations == 0 && generationElapsedTimeline != null) {
            generationElapsedTimeline.stop();
        }
    }

    private void setGenerationElapsedVisible(boolean visible) {
        generatedPreviewElapsed.setVisible(visible);
        generatedPreviewElapsed.setManaged(visible);
        engineResultElapsed.setVisible(visible);
        engineResultElapsed.setManaged(visible);
    }

    private void refreshGenerationElapsed() {
        long elapsedSeconds = generationStartedNanos <= 0
                ? 0
                : Math.max(0, (System.nanoTime() - generationStartedNanos) / 1_000_000_000L);
        generationElapsedText.set("Tiempo transcurrido: " + formatElapsed(elapsedSeconds));
    }

    static String formatElapsed(long elapsedSeconds) {
        long total = Math.max(0, elapsedSeconds);
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Exception; }

    private enum IntermediateBatchMode {
        MISSING_ONLY,
        OVERWRITE_ALL,
        SMOKE_TEST
    }

    private record IntermediateBatchSelection(
            IntermediateBatchMode mode,
            List<TheatreIntermediateFrameBatchPlanner.Item> items) {
        private IntermediateBatchSelection {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    private record IntermediateBatchResult(int generated,
                                           int requested,
                                           List<TheatreGeneratedFrameCandidate> approvedFrames) {
        private IntermediateBatchResult {
            approvedFrames = approvedFrames == null ? List.of() : List.copyOf(approvedFrames);
        }
    }

    private record EngineTestResult(boolean success,
                                    String message,
                                    Path outputImage,
                                    String diagnostic,
                                    boolean runtimeMissing,
                                    boolean modelMissing,
                                    boolean running,
                                    int width,
                                    int height,
                                    String outputProfile,
                                    String aspectRatio) {
        static EngineTestResult idle() {
            return new EngineTestResult(false, "Sin prueba ejecutada.", null, "", false, false,
                    false, 0, 0, "", "");
        }

        static EngineTestResult running(String message) {
            return new EngineTestResult(false, clean(message), null, "", false, false,
                    true, 0, 0, "", "");
        }

        static EngineTestResult from(ImageEngineReadinessReport report) {
            if (report == null) {
                return error("No se pudo inspeccionar Imagen IA teatral.");
            }
            boolean runtimeMissing = !report.runtimePrepared();
            boolean modelMissing = report.runtimePrepared() && !report.modelInstalled();
            String message;
            if (runtimeMissing && report.modelInstalled()) {
                message = "Modelo instalado; falta runtime local compatible. " + report.nextAction();
            } else if (runtimeMissing) {
                message = "Falta runtime local compatible. " + report.nextAction();
            } else if (modelMissing) {
                message = "Falta descargar o importar paquete. " + report.nextAction();
            } else {
                message = report.userMessage() + " " + report.nextAction();
            }
            return new EngineTestResult(report.ready(), clean(message), null, "", runtimeMissing, modelMissing,
                    false, 0, 0, "", "");
        }

        static EngineTestResult from(ImageEngineSmokeReport report) {
            if (report == null) {
                return error("Prueba terminada sin detalle.");
            }
            String message = report.userMessage();
            if (report.success() && report.outputImage() != null) {
                message = report.width() > 0 && report.height() > 0
                        ? "Prueba completada. PNG temporal generado (" + report.width() + "x" + report.height() + ")."
                        : "Prueba completada. PNG temporal generado.";
            }
            boolean runtimeMissing = message.toLowerCase(Locale.ROOT).contains("runtime local compatible")
                    || message.toLowerCase(Locale.ROOT).contains("lanzador local");
            boolean modelMissing = message.toLowerCase(Locale.ROOT).contains("descargar o importar paquete")
                    || message.toLowerCase(Locale.ROOT).contains("paquete de modelos");
            String diagnostic = report.diagnostic();
            if (report.width() > 0 && report.height() > 0) {
                diagnostic = (diagnostic == null || diagnostic.isBlank() ? "" : diagnostic + "\n")
                        + "dimensions=" + report.width() + "x" + report.height()
                        + "\nprofile=" + report.outputProfile()
                        + "\naspectRatio=" + report.aspectRatio();
            }
            return new EngineTestResult(report.success(), clean(message), report.outputImage(),
                    diagnostic, runtimeMissing, modelMissing, false, report.width(), report.height(),
                    report.outputProfile(), report.aspectRatio());
        }

        static EngineTestResult error(String message) {
            return new EngineTestResult(false, clean(message), null, "", false, false,
                    false, 0, 0, "", "");
        }

        private static String clean(String message) {
            return message == null || message.isBlank() ? "Prueba terminada sin detalle." : message.strip();
        }
    }
}
