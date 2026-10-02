package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.media.administration.CapabilityAdministrationService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsValidationReport;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoRuntimeStatus;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;

/** Provider-neutral administrative workspace for operational settings and media engines. */
public final class SettingsDialog {
    private static final AppCommandId[] SUPPORT_COMMANDS = {
            AppCommandId.INSPECT_PROJECT_INTEGRITY,
            AppCommandId.INSPECT_EXPORT_READINESS,
            AppCommandId.EXPORT_DIAGNOSTIC_REPORT,
            AppCommandId.EXPORT_PROJECT_BUNDLE,
            AppCommandId.OPEN_PROJECT_FOLDER,
            AppCommandId.OPEN_EXPORTS_FOLDER
    };

    private final CapabilityAdministrationService capabilities;

    public SettingsDialog(MediaEnginePlatform mediaEngines) {
        this(new CapabilityAdministrationService(mediaEngines));
    }

    public SettingsDialog(CapabilityAdministrationService capabilities) {
        this.capabilities = java.util.Objects.requireNonNull(capabilities,
                "capability administration service");
    }

    /** Compatibility constructor for isolated presentation tests. */
    public SettingsDialog() { this(MediaEnginePlatform.empty()); }

    public void show(Window owner) {
        show(owner, null, SettingsSection.READING, SettingsSupportActions.unavailable());
    }

    public void show(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.READING, SettingsSupportActions.unavailable());
    }

    public void show(Window owner, SettingsApplicationServices services, SettingsSupportActions supportActions) {
        show(owner, services, SettingsSection.READING,
                supportActions == null ? SettingsSupportActions.unavailable() : supportActions);
    }

    public void showVoiceEngines(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.ENGINES, SettingsSupportActions.unavailable());
    }

    public void showPerformance(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.PERFORMANCE, SettingsSupportActions.unavailable());
    }

    public void showSuperResolution(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.SUPER_RESOLUTION, SettingsSupportActions.unavailable());
    }

    public void showFirstUseSetup(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.ENGINES, SettingsSupportActions.unavailable());
    }

    private void show(Window owner, SettingsApplicationServices services, SettingsSection initial,
                      SettingsSupportActions supportActions) {
        OperationalSettings loaded;
        try { loaded = services == null ? OperationalSettings.defaults() : services.loadOperationalSettings().load(); }
        catch (IOException failure) {
            var error = com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog.create(owner, javafx.scene.control.Alert.AlertType.ERROR, "Configuración", "No se pudo cargar la configuración",
                    "No se pudo cargar la configuración. No se sustituirán tus ajustes. " + failure.getMessage(), com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog.technicalDetail(failure), ButtonType.OK);
            DialogStyler.apply(error, owner);
            error.showAndWait();
            return;
        }
        SettingsFormModel form = new SettingsFormModel(loaded);
        Label status = new Label(services == null
                ? "Configuración abierta sin persistencia; los cambios no se guardarán."
                : "Sin cambios pendientes. Las descargas e importaciones son acciones independientes.");
        status.setWrapText(true);
        refreshComputeDevices(form, services, loaded, status);
        form.markSaved();
        form.observeChanges(() -> status.setText(form.dirty() ? "Cambios pendientes: guarda antes de preparar recursos."
                : "Sin cambios pendientes."));

        Dialog<Void> dialog = StudioDialogShell.dialog();
        dialog.setTitle("Configuración de DocuPodcast Studio");
        dialog.setHeaderText("Administración transversal de capacidades y ajustes operativos");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (dialog.getDialogPane().lookupButton(ButtonType.CLOSE)
                instanceof javafx.scene.control.ButtonBase closeButton) {
            SemanticActionIcons.decorate(closeButton, "Cerrar");
        }
        dialog.getDialogPane().setContent(content(form, services, supportActions, status, initial));
        dialog.getDialogPane().setPrefSize(1040, 680);
        dialog.getDialogPane().setMinSize(900, 580);
        DialogStyler.apply(dialog, owner);
        dialog.setOnCloseRequest(event -> {
            if (form.operationRunning) {
                status.setText("Hay una operación en curso en Motores y dependencias. Espera a que termine antes de cerrar.");
                event.consume();
                return;
            }
            if (!form.dirty()) return;
            var discard = com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse.button("Descartar cambios");
            var confirm = com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog.create(dialog.getDialogPane().getScene().getWindow(), javafx.scene.control.Alert.AlertType.CONFIRMATION, "Configuración", "Cambios pendientes",
                    "Hay cambios sin guardar. ¿Quieres descartarlos?", "", discard, ButtonType.CANCEL);
            DialogStyler.apply(confirm, dialog.getDialogPane().getScene().getWindow());
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != discard) event.consume();
        });
        dialog.showAndWait();
    }

    private static void refreshComputeDevices(SettingsFormModel form,
                                              SettingsApplicationServices services,
                                              OperationalSettings settings,
                                              Label status) {
        if (services == null) return;
        try {
            var report = services.inspectComputeEnvironment().inspect(settings);
            form.refreshComputeDeviceChoices(report.devices());
            form.refreshVideoEncoderChoices(report.devices());
            if (!report.warnings().isEmpty()) {
                status.setText(String.join(" ", report.warnings()));
            }
        } catch (RuntimeException failure) {
            status.setText("No se pudo actualizar la lista de CPU/GPU: " + failure.getMessage());
        }
    }

    private Node content(SettingsFormModel form, SettingsApplicationServices services,
                         SettingsSupportActions supportActions, Label status, SettingsSection initial) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("settings-root");
        ListView<SettingsSection> navigation = StudioCollectionControls.listView();
        navigation.getItems().setAll(SettingsSection.values());
        navigation.setPrefWidth(220);
        navigation.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(SettingsSection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label);
                setGraphic(empty || item == null ? null
                        : SemanticActionIcons.graphicFor(item.label, 18));
            }
        });
        VBox pageHost = new VBox();
        pageHost.setFillWidth(true);
        java.util.Map<SettingsSection, Node> retained = new java.util.EnumMap<>(SettingsSection.class);
        navigation.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                Node next = selected == SettingsSection.ENGINES
                        ? retained.computeIfAbsent(selected, key -> page(key, form, supportActions, services,
                            target -> navigation.getSelectionModel().select(target)))
                        : page(selected, form, supportActions, services, target -> navigation.getSelectionModel().select(target));
                pageHost.getChildren().setAll(next);
            }
        });

        Button save = ActionButtonFactory.primary("Guardar configuración", () -> save(form, services, status));
        save.setDisable(services == null);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, status, spacer, save);
        actions.setPadding(new Insets(12, 0, 0, 0));
        HBox.setHgrow(status, Priority.ALWAYS);

        VBox center = new VBox(12, pageHost, actions);
        center.setPadding(new Insets(16));
        VBox.setVgrow(pageHost, Priority.ALWAYS);
        root.setLeft(navigation);
        root.setCenter(center);
        navigation.getSelectionModel().select(initial == null ? SettingsSection.READING : initial);
        return root;
    }

    private Node page(SettingsSection section, SettingsFormModel form,
                      SettingsSupportActions supportActions, SettingsApplicationServices services,
                      java.util.function.Consumer<SettingsSection> navigate) {
        Node body = switch (section) {
            case ENGINES -> new EngineAdministrationPane(
                    new EngineAdministrationController(capabilities), services).withPendingChanges(form::dirty)
                    .withOperationState(value -> form.operationRunning = value, () -> {
                        if (services == null || form.dirty()) return;
                        try { form.load(services.loadOperationalSettings().load()); form.markSaved(); }
                        catch (IOException failure) { /* Preserve the existing form when refresh fails. */ }
                    });
            case SUPER_RESOLUTION -> new VBox(14,
                    grid(
                            "Resolución de generación", form.generationResolution,
                            "Reescalar por defecto", form.upscaleEnabled,
                            "Destino predeterminado", form.upscaleTargetResolution,
                            "Modelo", form.upscaleModel,
                            "Mejorar composición tras reescalar", form.refinementEnabled,
                            "Intensidad de mejora", form.refinementPreset),
                    wrappedLabel("La instalación, importación, reparación y verificación de Real-ESRGAN "
                            + "y ControlNet Tile se administra exclusivamente en Motores y dependencias."));
            case READING -> grid(
                    "Tamaño base", form.baseFontSize,
                    "Interlineado", form.lineSpacing,
                    "Mantener frase activa centrada", form.keepActiveSentenceNearCenter);
            case PLAYBACK -> grid(
                    "Fragmentos iniciales listos", form.initialReadySegments,
                    "Fragmentos de anticipación", form.lookaheadSegments,
                    "Pausar si falta buffer", form.pauseWhenBufferMissing);
            case PERFORMANCE -> grid(
                    "Política de cómputo", form.computePolicy,
                    "Dispositivo", form.computeSelectedDeviceId,
                    "Permitir GPU para voz", form.allowGpuForTts,
                    "Permitir GPU para video", form.allowGpuForVideo,
                    "Permitir GPU para análisis documental", form.allowGpuForContentAnalysis,
                    "Permitir RAM de apoyo para análisis", form.allowRamOffloadForContentAnalysis,
                    "Preferencia de codificación", form.videoEncoderPolicy);
            case VIDEO_FINAL -> videoFinalPage(form, services, navigate);
            case STORAGE -> new VBox(12, grid("Directorio de modelos de voz", form.modelsDirectory),
                    wrappedLabel("Los recursos visuales se administran desde Motores y dependencias. "
                            + "Los destinos de exportación se eligen al exportar. Cambiar una ruta no mueve archivos existentes."));
            case SUPPORT -> supportPage(supportActions);
        };
        Label title = new Label(section.label);
        title.setGraphic(SemanticActionIcons.graphicFor(section.label, 22));
        title.getStyleClass().add("settings-page-title");
        VBox content = new VBox(14, title, body);
        ScrollPane scroll = StudioViewportControls.scrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private static Node videoFinalPage(SettingsFormModel form,
                                       SettingsApplicationServices services,
                                       java.util.function.Consumer<SettingsSection> navigate) {
        OperationalSettings settings;
        try { settings = form.currentSettings(); }
        catch (IllegalArgumentException failure) { return wrappedLabel("Corrige los ajustes antes de consultar el vídeo: " + failure.getMessage()); }
        VideoEncoderPolicy requestedEncoder = VideoEncoderPolicy.from(form.videoEncoderPolicy.getValue());
        FinalVideoRuntimeStatus runtimeStatus = services == null
                ? new FinalVideoRuntimeStatus(
                "Renderizador", "Inspector", null, null, false,
                "Configuración abierta sin servicios de diagnóstico.", "", "", "",
                requestedEncoder.name(), List.of(),
                "Abre Motores y dependencias para preparar el runtime de video.")
                : services.inspectFinalVideoRuntimeStatus().inspect(settings.video(), requestedEncoder);

        TextField rendererPath = readOnlyPath(runtimeStatus.rendererPath());
        TextField inspectorPath = readOnlyPath(runtimeStatus.inspectorPath());
        Label state = wrappedLabel(runtimeStatus.stateMessage());
        state.setId("video-final-runtime-status");
        Label versions = wrappedLabel(runtimeStatus.rendererName() + ": "
                + valueOrUnknown(runtimeStatus.rendererVersion())
                + "\n" + runtimeStatus.inspectorName() + ": "
                + valueOrUnknown(runtimeStatus.inspectorVersion()));
        String effectiveEncoder = runtimeStatus.effectiveEncoder();
        Label encoder = wrappedLabel("Codificador efectivo: "
                + (effectiveEncoder.isBlank() ? "no disponible para la preferencia actual" : effectiveEncoder)
                + ". Preferencia: " + runtimeStatus.requestedEncoder()
                + ". Disponibles: " + (runtimeStatus.availableEncoders().isEmpty()
                ? "sin enumerar" : String.join(", ", runtimeStatus.availableEncoders())));

        Button engines = ActionButtonFactory.secondary("Abrir Motores y dependencias",
                () -> navigate.accept(SettingsSection.ENGINES));
        Button performance = ActionButtonFactory.secondary("Abrir Rendimiento",
                () -> navigate.accept(SettingsSection.PERFORMANCE));
        HBox links = new HBox(10, engines, performance);
        return new VBox(14,
                grid(
                        "Resolución predeterminada", form.resolutionPreset,
                        "FPS", new Label("Administrados por el preset y el adaptador de exportación."),
                        "Duración de visual silencioso", form.silentVisualBlockSeconds,
                        "Preferir runtime de video administrado", form.preferManagedVideoRuntime,
                        runtimeStatus.rendererName() + " efectivo", rendererPath,
                        runtimeStatus.inspectorName() + " efectivo", inspectorPath),
                state,
                versions,
                encoder,
                wrappedLabel(runtimeStatus.administrationHint()),
                links);
    }

    private static TextField readOnlyPath(java.nio.file.Path path) {
        TextField field = StudioFormControls.textField();
        field.setText(path == null ? "No resuelto" : path.toString());
        field.setEditable(false);
        field.setFocusTraversable(true);
        field.setPrefColumnCount(58);
        return field;
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "no disponible" : value;
    }

    private static GridPane grid(Object... rows) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int index = 0, row = 0; index + 1 < rows.length; index += 2, row++) {
            Label label = new Label(String.valueOf(rows[index]));
            Node field = (Node) rows[index + 1];
            label.setLabelFor(field);
            field.setAccessibleText(label.getText());
            grid.add(label, 0, row);
            grid.add(field, 1, row);
        }
        return grid;
    }

    private static Node supportPage(SettingsSupportActions actions) {
        VBox buttons = new VBox(8);
        for (AppCommandId command : SUPPORT_COMMANDS) {
            Button button = ActionButtonFactory.secondary(command.name().replace('_', ' '), () -> actions.run(command));
            button.setDisable(!actions.available(command));
            buttons.getChildren().add(button);
        }
        if (!actions.anyAvailable(SUPPORT_COMMANDS)) {
            buttons.getChildren().addFirst(new Label("No hay acciones de soporte disponibles en este contexto."));
        }
        return buttons;
    }

    private static OperationalSettings load(SettingsApplicationServices services) {
        if (services == null) return OperationalSettings.defaults();
        try { return services.loadOperationalSettings().load(); }
        catch (IOException failure) { return OperationalSettings.defaults(); }
    }

    private static void save(SettingsFormModel form, SettingsApplicationServices services, Label status) {
        if (services == null) return;
        if (form.operationRunning) { status.setText("Espera a que termine la operación de recursos antes de guardar."); return; }
        try {
            form.rebaseUneditedSettings(services.loadOperationalSettings().load());
            OperationalSettingsValidationReport report = services.saveOperationalSettings().save(form.toSettings());
            if (!report.errors().isEmpty()) status.setText("No se guardó: " + String.join(" ", report.errors()));
            else if (!report.warnings().isEmpty()) status.setText("Guardado con avisos: " + String.join(" ", report.warnings()));
            else status.setText("Configuración guardada.");
            if (report.errors().isEmpty()) {
                form.load(services.loadOperationalSettings().load());
                form.markSaved();
                status.setText(report.warnings().isEmpty() ? "Configuración guardada. Se utilizará al iniciar las operaciones correspondientes."
                        : "Guardado con avisos: " + String.join(" ", report.warnings()));
            }
        } catch (IllegalArgumentException failure) {
            status.setText("No se guardó: " + failure.getMessage());
        } catch (IOException failure) {
            status.setText("Error recuperable al guardar: " + failure.getMessage());
        }
    }

    private static Label wrappedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private enum SettingsSection {
        READING("Lectura"), PLAYBACK("Reproducción"), ENGINES("Motores y dependencias"),
        SUPER_RESOLUTION("Procesamiento visual"),
        PERFORMANCE("Rendimiento"), VIDEO_FINAL("Video final"),
        STORAGE("Almacenamiento"), SUPPORT("Soporte");
        private final String label;
        SettingsSection(String label) { this.label = label; }
    }
}
