package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.compute.AssessComputeAccelerationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeAccelerationAssessment;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.engines.AiEnginePreflightReport;
import com.marcosmoreiradev.docupodcaststudio.application.engines.BuildHumanEnginePreflightSummaryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.engines.HumanEnginePreflightSummary;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsValidationReport;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineOption;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineUsabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceSynthesisSettings;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SettingsPageView;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Dedicated editable configuration surface for honest, actionable product settings.
 *
 * <p>The main document screen is intentionally kept operational and friendly; this dialog is the
 * settings surface where reading comfort, playback buffer, engines, storage and diagnostics are grouped
 * without overloading the reader experience. Since T69 this surface is not a facade: it exposes
 * editable controls and persists changes through SettingsApplicationServices.</p>
 */

    // Diagnostico tecnico: scripts\39-preparar-pytorch-cuda-xtts.bat prepara PyTorch CUDA solo en el Python local.
public final class SettingsDialog {
    private static final AppCommandId[] SUPPORT_COMMANDS = {
            AppCommandId.INSPECT_PROJECT_INTEGRITY,
            AppCommandId.INSPECT_EXPORT_READINESS,
            AppCommandId.EXPORT_DIAGNOSTIC_REPORT,
            AppCommandId.EXPORT_PROJECT_BUNDLE,
            AppCommandId.OPEN_PROJECT_FOLDER,
            AppCommandId.OPEN_EXPORTS_FOLDER
    };

    private final EmbeddedDependencySetupAssistant dependencySetupAssistant = new EmbeddedDependencySetupAssistant();
    private final FxBackgroundTaskRunner backgroundTaskRunner = new FxBackgroundTaskRunner();
    private final SettingsOperationProgressCoordinator operationProgress = new SettingsOperationProgressCoordinator();
    private final AdvancedVoiceSettingsOperations advancedVoiceOperations = new AdvancedVoiceSettingsOperations(
            backgroundTaskRunner,
            operationProgress,
            this::confirmUser,
            this::refreshActiveSettingsPage,
            this::promptPiperAfterAdvancedIfMissing);
    private final PiperSettingsOperations piperOperations = new PiperSettingsOperations(
            backgroundTaskRunner,
            operationProgress,
            this::confirmUser,
            this::refreshActiveSettingsPage);
    private final VideoLocalSettingsOperations videoLocalOperations = new VideoLocalSettingsOperations(
            backgroundTaskRunner,
            operationProgress,
            this::confirmUser,
            this::refreshActiveSettingsPage);
    private final OcrSettingsCard ocrSettingsCard = new OcrSettingsCard(new OcrSettingsOperations(
            backgroundTaskRunner,
            operationProgress,
            this::refreshActiveSettingsPage));
    private final ImageEngineSettingsOperations imageEngineOperations = new ImageEngineSettingsOperations(
            backgroundTaskRunner,
            operationProgress,
            this::confirmUser,
            this::refreshActiveSettingsPage);
    private final ImageEngineSettingsCard imageEngineSettingsCard = new ImageEngineSettingsCard(imageEngineOperations);
    private final InitialSetupSettingsOperations initialSetupOperations = new InitialSetupSettingsOperations(
            backgroundTaskRunner,
            operationProgress,
            this::confirmUser,
            this::refreshActiveSettingsPage,
            piperOperations);
    private Runnable activeRefresh = () -> { };

    public void show(Window owner) {
        show(owner, null, SettingsSection.READING, false, SettingsSupportActions.unavailable());
    }

    public void show(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSupportActions.unavailable());
    }

    public void show(Window owner, SettingsApplicationServices services, SettingsSupportActions supportActions) {
        show(owner, services, SettingsSection.READING, false, supportActions);
    }

    public void showVoiceEngines(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.ENGINES, false, SettingsSupportActions.unavailable());
    }

    public void showPerformance(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.PERFORMANCE, false, SettingsSupportActions.unavailable());
    }

    public void showFirstUseSetup(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.ENGINES, true, SettingsSupportActions.unavailable());
    }

    private void show(Window owner, SettingsApplicationServices services, SettingsSection initialSection,
                      boolean promptInitialSetup, SettingsSupportActions supportActions) {
        OperationalSettings settings = loadSettings(services);
        OperationalSettingsValidationReport report = services == null
                ? servicesUnavailableReport()
                : services.validateOperationalSettings().validate(settings);
        SettingsFormModel form = new SettingsFormModel(settings);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Configuración de DocuPodcast Studio");
        dialog.setHeaderText("Ajustes del programa para lectura, voz, audio y video");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(buildContent(form, report, services,
                initialSection == null ? SettingsSection.READING : initialSection,
                supportActions == null ? SettingsSupportActions.unavailable() : supportActions));
        dialog.getDialogPane().setPrefSize(1040, 680);
        dialog.getDialogPane().setMinSize(980, 620);
        DialogStyler.apply(dialog, owner);
        if (promptInitialSetup) {
            Platform.runLater(() -> promptInitialSetup(dialog.getDialogPane(), form, services));
        }
        dialog.showAndWait();
    }

    private OperationalSettings loadSettings(SettingsApplicationServices services) {
        if (services == null) {
            return OperationalSettings.defaults();
        }
        try {
            return services.loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }

    private OperationalSettingsValidationReport servicesUnavailableReport() {
        return OperationalSettingsValidationReport.ok(
                List.of("Configuración abierta sin conexión a servicios: los cambios no se guardarán en esta instancia."),
                List.of());
    }

    private Node buildContent(SettingsFormModel form, OperationalSettingsValidationReport report, SettingsApplicationServices services,
                              SettingsSection initialSection, SettingsSupportActions supportActions) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("settings-root");

        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("settings-sidebar");
        Label title = new Label("Configuración");
        title.getStyleClass().add("settings-sidebar-title");
        Label subtitle = new Label("Ajustes operativos persistentes");
        subtitle.getStyleClass().add("settings-sidebar-subtitle");

        ListView<SettingsSection> sections = new ListView<>();
        sections.getStyleClass().add("settings-section-list");
        sections.getItems().setAll(SettingsSection.values());
        sections.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SettingsSection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        VBox.setVgrow(sections, Priority.ALWAYS);
        sidebar.getChildren().addAll(title, subtitle, sections);

        BorderPane pageHost = new BorderPane();
        pageHost.getStyleClass().add("settings-page-host");
        activeRefresh = () -> Platform.runLater(() -> {
            SettingsSection selected = sections.getSelectionModel().getSelectedItem();
            if (selected != null) {
                pageHost.setCenter(scrollPage(pages(form, report, services, supportActions).get(selected)));
            }
        });
        sections.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                pageHost.setCenter(scrollPage(pages(form, report, services, supportActions).get(newValue)));
            }
        });
        sections.getSelectionModel().select(initialSection == null ? SettingsSection.READING : initialSection);

        root.setLeft(sidebar);
        root.setCenter(pageHost);
        root.setBottom(actionBar(form, report, services));
        return root;
    }

    private ScrollPane scrollPage(Node node) {
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("settings-page-scroll");
        return scroll;
    }

    private Node actionBar(SettingsFormModel form, OperationalSettingsValidationReport report, SettingsApplicationServices services) {
        return SettingsActionBar.create(
                () -> form.load(OperationalSettings.defaults()),
                initialStatus(report, services),
                services != null,
                status -> saveSettings(form, services, status));
    }

    private String initialStatus(OperationalSettingsValidationReport report, SettingsApplicationServices services) {
        if (services == null) {
            return "Configuración abierta en modo de revisión: los cambios no se guardarán en esta instancia.";
        }
        return report.usable()
                ? "Configuración cargada. Edita y guarda para aplicar en el próximo uso de voz, audio o video."
                : "Configuración cargada con advertencias: revisa rutas, modelos o motores antes de usar voz/audio/video.";
    }

    private void saveSettings(SettingsFormModel form, SettingsApplicationServices services, Label status) {
        if (services == null) {
            status.setText("No hay servicios de configuración conectados; no se guardó nada.");
            return;
        }
        OperationalSettings next = form.toSettings();
        try {
            OperationalSettingsValidationReport report = services.saveOperationalSettings().save(next);
            if (report.errors().isEmpty()) {
                status.setText("Configuración guardada. Algunos cambios se aplican al reiniciar motores o abrir nuevos procesos de audio/video.");
            } else {
                status.setText("No se guardó: " + String.join(" · ", report.errors()));
            }
        } catch (IOException | RuntimeException ex) {
            status.setText("No se pudo guardar la configuración: " + ex.getMessage());
        }
    }

    private void refreshActiveSettingsPage() {
        activeRefresh.run();
    }

    private Map<SettingsSection, Node> pages(SettingsFormModel form, OperationalSettingsValidationReport report,
                                             SettingsApplicationServices services, SettingsSupportActions supportActions) {
        EnumMap<SettingsSection, Node> pages = new EnumMap<>(SettingsSection.class);
        pages.put(SettingsSection.READING, readingDocumentPage(form));
        pages.put(SettingsSection.PLAYBACK, playbackBufferPage(form));
        pages.put(SettingsSection.ENGINES, guidedEnginesPage(form, services));
        pages.put(SettingsSection.PERFORMANCE, computeDevicePage(form, services));
        pages.put(SettingsSection.VIDEO_FINAL, storyboardVideoPage(form));
        pages.put(SettingsSection.STORAGE, storagePage(form));
        pages.put(SettingsSection.SUPPORT, diagnosticsPage(form, report,
                supportActions == null ? SettingsSupportActions.unavailable() : supportActions));
        return pages;
    }

    private Node readingDocumentPage(SettingsFormModel form) {
        SettingsPageView page = new SettingsPageView(
                "Lectura",
                "Controla cómo se ve el documento narrable sin modificar el archivo original.");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Tamaño base del documento", form.baseFontSize);
        addFormRow(grid, 1, "Interlineado", form.lineSpacing);
        addFormRow(grid, 2, "Mantener oración activa cerca del centro", form.keepActiveSentenceNearCenter);
        page.addNode(grid);
        page.addRow("Valor recomendado", "18 px por defecto para lectura cómoda");
        page.addNote("Fuente solo lectura: los cambios visuales se guardan como preferencias del programa, no dentro de Word/PDF/Markdown/TXT.");
        return page;
    }

    private Node playbackBufferPage(SettingsFormModel form) {
        SettingsPageView page = new SettingsPageView(
                "Reproducción",
                "Ajusta el buffer y la continuidad al escuchar documentos largos.");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Fragmentos listos antes de empezar", form.initialReadySegments);
        addFormRow(grid, 1, "Fragmentos adelantados", form.lookaheadSegments);
        addFormRow(grid, 2, "Pausar si falta audio", form.pauseWhenBufferMissing);
        page.addNode(grid);
        page.addRow("Audio por fragmento", "La app reproduce WAV generados por fragmento para poder empezar antes de que termine todo el documento")
                .addRow("Silencios", "Las pausas de lectura se controlan desde el motor y la exportación final")
                .addRow("Soporte", "Si un audio se corta o falta, el reporte se revisa desde Soporte y diagnóstico")
                .addNote("Estos valores gobiernan el prebuffer tipo YouTube: reproducir lo listo, preparar lo siguiente y continuar cuando el fragmento esté disponible.");
        return page;
    }

    private Node computeDevicePage(SettingsFormModel form, SettingsApplicationServices services) {
        SettingsPageView page = new SettingsPageView(
                "Rendimiento",
                "Elige CPU/GPU para voz y video sin cambiar el contenido del documento.");
        OperationalSettings currentSettings = form.toSettings();
        Path applicationRoot = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        ComputeEnvironmentReport report = services == null
                ? new com.marcosmoreiradev.docupodcaststudio.application.compute.InspectComputeEnvironmentUseCase().inspect(currentSettings)
                : services.inspectComputeEnvironment().inspect(currentSettings);
        XttsCudaSmokeReport cudaSmoke = services == null
                ? new InspectXttsCudaSmokeUseCase().inspect(applicationRoot)
                : services.inspectXttsCudaSmoke().inspect(applicationRoot);
        ComputeAccelerationAssessment acceleration = services == null
                ? new AssessComputeAccelerationUseCase().assess(currentSettings, report, cudaSmoke)
                : services.assessComputeAcceleration().assess(currentSettings, report, cudaSmoke);
        form.refreshComputeDeviceChoices(report.devices());
        form.refreshVideoEncoderChoices(report.devices());
        applyComputePresentationLabels(form, report);
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Política de procesamiento", form.computePolicy);
        addFormRow(grid, 1, "Dispositivo para voz", form.computeSelectedDeviceId);
        addFormRow(grid, 2, "Permitir GPU para voz", form.allowGpuForTts);
        addFormRow(grid, 3, "Permitir GPU para video", form.allowGpuForVideo);
        addFormRow(grid, 4, "Encoder de video", form.videoEncoderPolicy);
        page.addNode(grid);
        Button cudaPrepare = ActionButtonFactory.secondary("Preparar CUDA para Voz IA avanzada",
                () -> prepareXttsPytorchCuda(form, services, applicationRoot, report, page));
        Button cudaTest = ActionButtonFactory.secondary("Probar GPU para Voz IA avanzada",
                () -> runXttsCudaSmoke(form, services, applicationRoot, report, page));
        HBox actions = new HBox(8, cudaPrepare, cudaTest);
        actions.getStyleClass().add("settings-engine-card-actions");
        page.addNode(actions);
        page.addRow("Detección CPU/GPU", friendlyEngineText(report.summary()))
                .addRow("GPU detectada", report.gpuDetected() ? "sí" : "no")
                .addRow("Voz IA avanzada", acceleration.voiceExecutionMode())
                .addRow("CUDA Voz IA avanzada", describeXttsCudaSmoke(cudaSmoke))
                .addRow("Video", acceleration.videoExecutionMode())
                .addRow("Promesa GPU", acceleration.gpuPromiseLabel())
                .addRow("Advertencias", Integer.toString(acceleration.warnings().size()))
                .addNote("El dispositivo elegido se propaga a los procesos de voz. En Automático la app conserva una ruta segura; en Dispositivo específico se intentará el dispositivo solicitado y el runtime informará si no lo soporta.");
        return page;
    }

    private void prepareXttsPytorchCuda(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                                        ComputeEnvironmentReport currentReport, Node anchor) {
        advancedVoiceOperations.prepareCuda(form, services, applicationRoot, currentReport, anchor);
    }


    private void runXttsCudaSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                                  ComputeEnvironmentReport currentReport, Node anchor) {
        advancedVoiceOperations.runCudaSmoke(form, services, applicationRoot, currentReport, anchor);
    }

    private static String describeXttsCudaSmoke(XttsCudaSmokeReport report) {
        return AdvancedVoiceSettingsOperations.describeCudaSmoke(report);
    }

    private static void applyComputePresentationLabels(SettingsFormModel form, ComputeEnvironmentReport report) {
        installStringLabels(form.computePolicy, SettingsDialog::humanComputePolicyLabel);
        installStringLabels(form.computeSelectedDeviceId, value -> humanComputeDeviceLabel(value, report));
        installStringLabels(form.videoEncoderPolicy, SettingsDialog::humanVideoEncoderLabel);
        form.computePolicy.setEditable(false);
        form.computeSelectedDeviceId.setEditable(false);
        form.videoEncoderPolicy.setEditable(false);
    }

    private static void installStringLabels(ComboBox<String> combo, Function<String, String> labeler) {
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(String value) {
                return labeler.apply(value);
            }

            @Override public String fromString(String value) {
                return value;
            }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labeler.apply(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labeler.apply(item));
            }
        });
    }

    private static String humanComputePolicyLabel(String value) {
        return ComputeDevicePolicy.from(value).label();
    }

    private static String humanComputeDeviceLabel(String value, ComputeEnvironmentReport report) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank() || "auto".equalsIgnoreCase(normalized)) {
            return "Automático";
        }
        if ("cpu".equalsIgnoreCase(normalized)) {
            return "CPU";
        }
        if (report != null) {
            for (ComputeDeviceDescriptor device : report.devices()) {
                if (normalized.equalsIgnoreCase(device.id())) {
                    return device.displayName();
                }
            }
        }
        return "Dispositivo no detectado";
    }

    private static String humanVideoEncoderLabel(String value) {
        return VideoEncoderPolicy.from(value).label();
    }

    private Node voiceRuntimeSettings(SettingsFormModel form) {
        VoiceSynthesisSettings defaults = VoiceSynthesisSettings.defaults();
        VoiceEngineOption primary = VoiceEngineUsabilityPolicy.xttsHighQuality();
        VoiceEngineOption fallback = VoiceEngineUsabilityPolicy.piperLightweight();
        VoiceEngineOption diagnostic = VoiceEngineUsabilityPolicy.mockDiagnostic();
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");
        Label title = new Label("Motor activo y parametros de voz");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("Define el motor activo, idioma y límites operativos. Las muestras y tonos se administran desde Voces.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Modo de motor", form.ttsEngineMode);
        addFormRow(grid, 1, "Nombre visible", form.ttsDisplayName);
        addFormRow(grid, 2, "Idioma", form.ttsLanguage);
        addFormRow(grid, 3, "Voz predeterminada", form.ttsVoiceProfileId);
        addFormRow(grid, 4, "Tiempo máximo por fragmento", form.ttsTimeoutSeconds);
        addFormRow(grid, 5, "Reintentos si falla", form.ttsMaxRetries);
        Label summary = new Label("Recomendado: " + primary.displayName() + " · " + primary.tier()
                + ". Liviano: " + fallback.displayName()
                + ". Prueba: " + diagnostic.displayName()
                + ". Ritmo base: " + defaults.speechRateLabel()
                + " · pausa de " + defaults.sentencePauseSeconds() + "s por oración. "
                + "La emoción solo aplica cuando el motor activo la soporte. Los comandos internos no se muestran aquí.");
        summary.setWrapText(true);
        summary.getStyleClass().add("settings-engine-status-folder");
        card.getChildren().addAll(title, description, grid, summary);
        return card;
    }

    private Node storyboardVideoPage(SettingsFormModel form) {
        SettingsPageView page = new SettingsPageView(
                "Video final",
                "Preferencias del MP4 final y duración de bloques visuales sin narración.");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Herramienta de video local", form.ffmpegExecutable);
        addFormRow(grid, 1, "Resolución", form.resolutionPreset);
        addFormRow(grid, 2, "Usar video local integrado", form.preferEmbeddedFfmpeg);
        addFormRow(grid, 3, "Bloque visual sin narración (segundos)", form.silentVisualBlockSeconds);
        page.addNode(grid);
        page.addRow("Frame", "Imagen asociada + duración del audio o bloque visual silencioso")
                .addRow("Modo render", "Bloquear lectura, edición y nuevas exportaciones mientras se renderiza")
                .addRow("Asociación", "La imagen vive como capa del proyecto, no dentro del Word")
                .addRow("Bloques visuales", "Imagen, tabla o fórmula fuente solo entran al video si el usuario les asigna visual; sin texto se renderizan en silencio")
                .addNote("La preparación de FFmpeg vive en Motores y dependencias. Aquí solo se ajusta cómo saldrá el video final.");
        return page;
    }

    private Node storagePage(SettingsFormModel form) {
        SettingsPageView page = new SettingsPageView(
                "Almacenamiento",
                "Ubicación de modelos locales y exportaciones generadas por la app.");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Carpeta de recursos de voz", form.modelsDirectory);
        addFormRow(grid, 1, "Carpeta de exportaciones", form.exportsDirectory);
        page.addNode(grid);
        page.addRow("Modelos y runtimes", "Se guardan en carpetas locales de la app, normalmente dentro de tools/ y models/")
                .addRow("Proyecto", "Cada proyecto conserva sus documentos importados, imágenes, audios generados y trabajos")
                .addRow("Exportaciones", "La carpeta de exportaciones define la salida normal para audio/video finales")
                .addNote("El documento fuente externo se conserva como fuente solo lectura; los resultados transportables viven dentro del proyecto.");
        return page;
    }

    private Node diagnosticsPage(SettingsFormModel form, OperationalSettingsValidationReport report, SettingsSupportActions supportActions) {
        SettingsPageView page = new SettingsPageView(
                "Soporte y diagnóstico",
                "Verifica proyecto, exportaciones, motores, audio, video y reportes cuando algo no funciona.");
        GridPane grid = formGrid();
        addFormRow(grid, 0, "Verificar al iniciar", form.preflightOnStartup);
        addFormRow(grid, 1, "Guardar registro de soporte", form.writeLogs);
        addFormRow(grid, 2, "Exportar reportes de soporte", form.exportManifests);
        page.addNode(grid);
        page.addRow("Estado actual", report.usable() ? "usable" : "requiere revisión")
                .addRow("Qué revisa", "integridad del proyecto, readiness de exportación, paquetes de soporte, logs y carpetas operativas")
                .addRow("Advertencias", report.warnings().isEmpty() ? "sin advertencias" : String.join(" · ", report.warnings().stream().map(SettingsDialog::friendlyEngineText).toList()));
        page.addNode(supportActionsPanel(supportActions));
        page.addNote("Estas acciones son para resolver bloqueos o pedir soporte. La lectura normal no depende de abrir esta sección.");
        return page;
    }

    private Node supportActionsPanel(SettingsSupportActions supportActions) {
        SettingsSupportActions actions = supportActions == null
                ? SettingsSupportActions.unavailable()
                : supportActions;
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");
        Label title = new Label("Acciones de soporte");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("Usa estas acciones para validar el proyecto actual, revisar exportación o preparar un reporte cuando algo quede bloqueado.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        HBox firstRow = new HBox(8,
                supportActionButton(actions, AppCommandId.INSPECT_PROJECT_INTEGRITY, "Validar proyecto"),
                supportActionButton(actions, AppCommandId.INSPECT_EXPORT_READINESS, "Ver estado de exportación"),
                supportActionButton(actions, AppCommandId.EXPORT_DIAGNOSTIC_REPORT, "Exportar reporte de soporte"));
        HBox secondRow = new HBox(8,
                supportActionButton(actions, AppCommandId.EXPORT_PROJECT_BUNDLE, "Exportar paquete de soporte"),
                supportActionButton(actions, AppCommandId.OPEN_PROJECT_FOLDER, "Abrir carpeta del proyecto"),
                supportActionButton(actions, AppCommandId.OPEN_EXPORTS_FOLDER, "Abrir exportaciones"));
        firstRow.getStyleClass().add("settings-engine-card-actions");
        secondRow.getStyleClass().add("settings-engine-card-actions");
        card.getChildren().addAll(title, description, firstRow, secondRow);
        if (!actions.anyAvailable(SUPPORT_COMMANDS)) {
            Label unavailable = new Label("Abre Configuración desde la ventana principal con un proyecto cargado para usar estas acciones.");
            unavailable.setWrapText(true);
            unavailable.getStyleClass().add("settings-engine-status-folder");
            card.getChildren().add(unavailable);
        }
        return card;
    }

    private Button supportActionButton(SettingsSupportActions actions, AppCommandId commandId, String label) {
        Button button = ActionButtonFactory.secondary(label, () -> actions.run(commandId));
        button.setDisable(actions == null || !actions.available(commandId));
        return button;
    }

    private Node guidedEnginesPage(SettingsFormModel form, SettingsApplicationServices services) {
        SettingsPageView page = new SettingsPageView(
                "Motores y dependencias",
                "Prepara voces, modelos y herramientas locales de audio y video cuando los necesites.");
        Path applicationRoot = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        AiEnginePreflightReport preflight = services == null
                ? new com.marcosmoreiradev.docupodcaststudio.application.engines.InspectAiEnginesPreflightUseCase().inspect(form.toSettings(), applicationRoot)
                : services.inspectAiEnginesPreflight().inspect(form.toSettings(), applicationRoot);
        BuildHumanEnginePreflightSummaryUseCase humanPreflight = services == null
                ? new BuildHumanEnginePreflightSummaryUseCase()
                : services.buildHumanEnginePreflightSummary();
        HumanEnginePreflightSummary humanSummary = humanPreflight.fromAiEngineReport(preflight);
        page.addNode(engineOperationalSummary(form, preflight, humanSummary, services, applicationRoot));
        Button prepareRecommended = ActionButtonFactory.primary("Preparar dependencias recomendadas",
                () -> dependencySetupAssistant.showRecommendedSetup(page, services));
        prepareRecommended.setDisable(services == null);
        HBox recommendedActions = actionRow(prepareRecommended);
        page.addNode(recommendedActions);
        page.addNode(ocrSettingsCard.create(form, services, applicationRoot));
        page.addNode(voiceRuntimeSettings(form));
        page.addNode(xttsSetupActions(form, services, applicationRoot));
        page.addNode(piperSetupActions(form, services, applicationRoot));
        page.addNode(ffmpegSetupActions(form, services, applicationRoot));
        page.addNode(imageEngineSettingsCard.create(form, services, applicationRoot));
        page.addNote("Configuración inicial prioriza Voz local simple. El perfil visual SD 1.5 de diagnóstico solo se descarga si lo marcas y confirmas aparte.");
        return page;
    }

    private Node engineOperationalSummary(SettingsFormModel form, AiEnginePreflightReport preflight,
                                          HumanEnginePreflightSummary humanSummary,
                                          SettingsApplicationServices services, Path applicationRoot) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("settings-engine-status-card", "settings-engine-summary-card");
        Label title = new Label("Resumen operativo");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("Vista rápida del stack local. Preparar dependencias recomendadas cubre voz, video y OCR; la generación visual conserva descarga e importación separadas por tamaño.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-engine-summary-grid");
        grid.setHgap(12);
        grid.setVgap(6);
        addEngineStatusRow(grid, 0, "Estado", humanSummary.state().label());
        addEngineStatusRow(grid, 1, "Motor de voz activo", TtsEngineModes.safeLabel(form.toSettings().tts().engineMode()));
        addEngineStatusRow(grid, 2, "Prueba de voz", preflight.minimalVoiceDemoReady() ? "lista" : "pendiente");
        addEngineStatusRow(grid, 3, "Video local", preflight.ffmpegReady() ? "listo" : "pendiente");
        addEngineStatusRow(grid, 4, "OCR PDF local", preflight.ocrReady() ? "listo" : "pendiente");
        addEngineStatusRow(grid, 5, "Generación visual local", imageEngineReady(form, services, applicationRoot) ? "lista" : "pendiente");
        addEngineStatusRow(grid, 6, "Siguiente paso", friendlyEngineText(humanSummary.primaryAction()));
        card.getChildren().addAll(title, description, grid);
        return card;
    }

    private static void addEngineStatusRow(GridPane grid, int row, String key, String value) {
        Label label = new Label(key);
        label.getStyleClass().add("settings-key");
        Label text = new Label(value == null ? "" : value);
        text.setWrapText(true);
        text.getStyleClass().add("settings-engine-status-message");
        grid.add(label, 0, row);
        grid.add(text, 1, row);
        GridPane.setHgrow(text, Priority.ALWAYS);
    }

    private boolean imageEngineReady(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        if (services == null) {
            return false;
        }
        return ImageEngineSettingsCard.ready(form, services, applicationRoot);
    }

    private Node xttsSetupActions(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");
        Label title = new Label("Voz IA avanzada");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("Voz de alta calidad para narracion expresiva. La accion normal es verificar, preparar o importar recursos locales dentro del programa.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        Label status = new Label(TtsEngineModes.ADVANCED_AI.equals(form.toSettings().tts().engineMode())
                ? "Voz IA avanzada está seleccionada como motor activo. Usa Verificar para confirmar archivos locales."
                : "Pendiente de verificación. Solo se prepara cuando lo pides aquí.");
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-action");

        Button verify = ActionButtonFactory.secondary("Verificar", () -> verifyXtts(form, services, applicationRoot, status));
        Button prepare = ActionButtonFactory.primary("Preparar", () -> confirmAndPrepareXtts(form, services, applicationRoot, status));
        Button downloadModel = ActionButtonFactory.secondary("Descargar", () -> confirmAndDownloadXttsModel(form, services, applicationRoot, status));
        Button importModel = ActionButtonFactory.secondary("Importar", () -> importXttsModelFolder(form, services, applicationRoot, status));
        Button select = ActionButtonFactory.secondary("Usar", () -> selectXttsIfReady(form, services, applicationRoot, status));
        Button test = ActionButtonFactory.secondary("Probar", () -> runXttsReadinessSmoke(form, services, applicationRoot, status));
        Button playTest = ActionButtonFactory.secondary("Reproducir prueba", () -> playAndConfirmXttsSmoke(form, services, applicationRoot, status));
        TitledPane sources = collapsedDetails("Fuentes y rutas",
                downloadUrlControl("Página oficial del modelo de voz", form.xttsDownloadBaseUrl,
                        "Usa la pagina oficial del modelo; no pegues enlaces directos de descarga. La app identifica internamente los archivos necesarios."));
        card.getChildren().addAll(title, description, status,
                actionRow(verify, prepare, downloadModel, importModel, select, test, playTest), sources);
        return card;
    }

    static Node downloadUrlControl(String labelText, TextField field, String hintText) {
        VBox box = new VBox(4);
        box.getStyleClass().add("settings-download-url-box");
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-key");
        field.getStyleClass().add("settings-edit-control");
        field.setPrefColumnCount(72);
        field.setMaxWidth(Double.MAX_VALUE);
        if ((field.getText() == null || field.getText().isBlank())
                && field.getPromptText() != null
                && field.getPromptText().startsWith("http")) {
            field.setText(field.getPromptText());
        }
        Label hint = new Label(hintText == null ? "" : hintText);
        hint.setWrapText(true);
        hint.getStyleClass().add("settings-engine-status-folder");
        box.getChildren().addAll(label, field, hint);
        return box;
    }

    private void verifyXtts(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.verify(form, services, applicationRoot, status);
    }

    private void confirmAndPrepareXtts(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.confirmAndPrepare(form, services, applicationRoot, status);
    }

    private void confirmAndDownloadXttsModel(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.confirmAndDownloadModel(form, services, applicationRoot, status);
    }


    private boolean confirmUser(Node anchor, String title, String header, String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(header);
        Label body = new Label(message == null ? "" : message);
        body.setWrapText(true);
        body.setMinWidth(640);
        body.setPrefWidth(700);
        body.setMaxWidth(760);
        body.setMinHeight(Region.USE_PREF_SIZE);
        confirm.getDialogPane().setContent(body);
        confirm.getDialogPane().setMinWidth(780);
        confirm.getDialogPane().setPrefWidth(820);
        Window owner = anchor != null && anchor.getScene() != null ? anchor.getScene().getWindow() : null;
        DialogStyler.apply(confirm, owner);
        Optional<ButtonType> choice = confirm.showAndWait();
        return choice.isPresent() && choice.get() == ButtonType.OK;
    }

    private void promptPiperAfterAdvancedIfMissing(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Node anchor) {
        if (services == null) {
            return;
        }
        PiperSetupReadinessReport report = services.inspectPiperSetupReadiness().inspect(form.toSettings(), applicationRoot);
        if (report.canBeSelectedAsEngine()) {
            return;
        }
        if (!confirmUser(anchor, "Preparar Voz local simple", "¿Quieres preparar también la voz liviana?",
                "Voz IA avanzada quedó configurada.\n\n"
                        + "DocuPodcast también puede preparar Voz local simple como opción rápida y liviana para documentos largos.\n\n"
                        + "Puedes aceptar ahora o hacerlo después desde Configuración.")) {
            return;
        }
        Label status = anchor instanceof Label label ? label : new Label();
        runPiperPreparation(form, services, applicationRoot, status, anchor);
    }

    private void importXttsModelFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.importModelFolder(form, services, applicationRoot, status);
    }


    private void runXttsReadinessSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.runReadinessSmoke(form, services, applicationRoot, status);
    }


    private void playAndConfirmXttsSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.playAndConfirmSmoke(form, services, applicationRoot, status);
    }

    private void selectXttsIfReady(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        advancedVoiceOperations.selectIfReady(form, services, applicationRoot, status);
    }

    private String selectAndSaveXtts(SettingsFormModel form, SettingsApplicationServices services) {
        return advancedVoiceOperations.selectAndSave(form, services);
    }

    private static String summarizeMissing(PiperSetupReadinessReport report) {
        return PiperSettingsOperations.summarizeMissing(report);
    }

    private static String summarizeMissingDetailed(PiperSetupReadinessReport report) {
        return PiperSettingsOperations.summarizeMissingDetailed(report);
    }

    private Node piperSetupActions(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");
        Label title = new Label("Voz local simple");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("Voz liviana para escuchar documentos rapido. Consume menos recursos y no habilita voces por muestra humana ni estilos expresivos.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        Label status = new Label("Pendiente de verificación local.");
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-action");

        Button verify = ActionButtonFactory.secondary("Verificar", () -> verifyPiper(form, services, applicationRoot, status));
        Button prepare = ActionButtonFactory.primary("Preparar", () -> confirmAndPreparePiper(form, services, applicationRoot, status));
        Button importVoice = ActionButtonFactory.secondary("Importar", () -> importPiperVoiceFolder(form, services, applicationRoot, status));
        Button select = ActionButtonFactory.secondary("Usar", () -> selectPiperIfReady(form, services, applicationRoot, status));
        VBox sourcesBox = new VBox(8,
                downloadUrlControl("URL runtime local", form.piperRuntimeZipUrl,
                        "Ruta de descarga del ejecutable local simple. También puede definirse desde variables de entorno o propiedades del sistema."),
                downloadUrlControl("URL voz neutral", form.piperDefaultVoiceUrl,
                        "Ruta de descarga de la voz neutral usada por el motor local simple."),
                downloadUrlControl("URL datos de voz neutral", form.piperDefaultVoiceMetadataUrl,
                        "Archivo de metadatos de la voz neutral local."));
        sourcesBox.getStyleClass().add("settings-engine-status-list");
        card.getChildren().addAll(title, description, status, actionRow(verify, prepare, importVoice, select),
                collapsedDetails("Fuentes y rutas", sourcesBox));
        return card;
    }

    private void verifyPiper(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        piperOperations.verify(form, services, applicationRoot, status);
    }

    private void confirmAndPreparePiper(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        piperOperations.confirmAndPrepare(form, services, applicationRoot, status);
    }

    private void runPiperPreparation(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status, Node anchor) {
        piperOperations.runPreparation(form, services, applicationRoot, status, anchor);
    }

    private void selectPiperIfReady(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        piperOperations.selectIfReady(form, services, applicationRoot, status);
    }

    private String selectAndSavePiper(SettingsFormModel form, SettingsApplicationServices services) {
        return piperOperations.selectAndSave(form, services);
    }

    private void importPiperVoiceFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        piperOperations.importVoiceFolder(form, services, applicationRoot, status);
    }


    private Node ffmpegSetupActions(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");
        Label title = new Label("Video local");
        title.getStyleClass().add("settings-engine-status-title");
        Label description = new Label("FFmpeg y FFprobe embebidos para exportar video final sin depender de herramientas globales del sistema.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");
        Label status = new Label("Pendiente de verificación local.");
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-action");
        Button verify = ActionButtonFactory.secondary("Verificar", () -> verifyVideoLocal(form, services, applicationRoot, status));
        Button prepare = ActionButtonFactory.primary("Preparar", () -> confirmAndPrepareVideoLocal(form, services, applicationRoot, status));
        Button importFfmpeg = ActionButtonFactory.secondary("Importar carpeta", () -> importFfmpegRuntimeFolder(form, services, applicationRoot, status));
        TitledPane sources = collapsedDetails("Fuentes y rutas",
                downloadUrlControl("URL para preparar video local", form.ffmpegDownloadUrl,
                        "Ruta oficial del paquete de video. La descarga automática queda centralizada aquí; Preparar copia los componentes necesarios dentro del programa y verifica exportacion final."));
        card.getChildren().addAll(title, description, status, actionRow(verify, prepare, importFfmpeg), sources);
        return card;
    }

    private static HBox actionRow(Button... buttons) {
        HBox actions = new HBox(8, buttons);
        actions.getStyleClass().add("settings-engine-card-actions");
        return actions;
    }

    private static TitledPane collapsedDetails(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        pane.getStyleClass().add("settings-engine-details-pane");
        return pane;
    }

    private void verifyVideoLocal(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        videoLocalOperations.verify(form, services, applicationRoot, status);
    }

    private void confirmAndPrepareVideoLocal(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        videoLocalOperations.confirmAndPrepare(form, services, applicationRoot, status);
    }

    private void downloadFfmpegPortableRuntime(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        videoLocalOperations.downloadPortableRuntime(form, services, applicationRoot, status);
    }

    private void importFfmpegRuntimeFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        videoLocalOperations.importRuntimeFolder(form, services, applicationRoot, status);
    }

    private void promptInitialSetup(Node anchor, SettingsFormModel form, SettingsApplicationServices services) {
        initialSetupOperations.prompt(anchor, form, services);
    }

    private void runInitialSetup(Node anchor, SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        initialSetupOperations.run(anchor, form, services, applicationRoot);
    }

    static String friendlyProgress(String message) {
        return SettingsTechnicalMessageHumanizer.progress(message);
    }

    static String friendlyEngineText(String text) {
        return SettingsTechnicalMessageHumanizer.engineText(text);
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-edit-grid");
        grid.setHgap(12);
        grid.setVgap(8);
        return grid;
    }

    private void addFormRow(GridPane grid, int row, String label, Node field) {
        Label key = new Label(label);
        key.getStyleClass().add("settings-key");
        field.getStyleClass().add("settings-edit-control");
        grid.add(key, 0, row);
        grid.add(field, 1, row);
    }

    private enum SettingsSection {
        READING("Lectura"),
        PLAYBACK("Reproducción"),
        ENGINES("Motores y dependencias"),
        PERFORMANCE("Rendimiento"),
        VIDEO_FINAL("Video final"),
        STORAGE("Almacenamiento"),
        SUPPORT("Soporte y diagnóstico");

        private final String label;

        SettingsSection(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
