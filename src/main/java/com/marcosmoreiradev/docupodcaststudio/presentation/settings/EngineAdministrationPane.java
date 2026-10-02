package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioCollectionControls;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.services.DependencyPreparationService;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/** Declarative engine administration view with provider-neutral forms and lifecycle. */
public final class EngineAdministrationPane extends VBox {
    private java.util.function.BooleanSupplier pendingChanges = () -> false;
    private java.util.function.Consumer<Boolean> operationState = ignored -> {};
    private Runnable configurationChanged = () -> {};
    private boolean busy;
    public EngineAdministrationPane withOperationState(java.util.function.Consumer<Boolean> state, Runnable changed) {
        operationState = state;
        configurationChanged = changed;
        return this;
    }
    private void busy(boolean value) { busy = value; operationState.accept(value); }
    public EngineAdministrationPane withPendingChanges(java.util.function.BooleanSupplier pending) {
        pendingChanges = pending;
        return this;
    }
    private boolean preparationAllowed() {
        if (busy) { status.setText("Espera a que termine la operación de recursos en curso."); return false; }
        if (!pendingChanges.getAsBoolean()) return true;
        status.setText("Guarda los cambios de configuración antes de preparar recursos. No se inició ninguna operación.");
        return false;
    }
    private final EngineAdministrationController controller;
    private final SettingsApplicationServices settingsServices;
    private final DependencyPreparationService dependencyPreparation;
    private final Path applicationRoot;
    private final ListView<EngineAdministrationController.EngineView> engines = StudioCollectionControls.listView();
    private final VBox detail = new VBox(10);
    private final Label status = new Label("Selecciona un motor para comprobarlo.");
    private final ListView<ComputeQueueRow> computeQueue =
            StudioCollectionControls.listView();
    private final Label computeQueueStatus =
            new Label("Cola de cómputo sin trabajos.");

    public EngineAdministrationPane(EngineAdministrationController controller) {
        this(controller, null, null);
    }

    public EngineAdministrationPane(EngineAdministrationController controller, CapabilityId capabilityFilter) {
        this(controller, capabilityFilter, null);
    }

    public EngineAdministrationPane(EngineAdministrationController controller,
                                    SettingsApplicationServices settingsServices) {
        this(controller, null, settingsServices);
    }

    private EngineAdministrationPane(EngineAdministrationController controller,
                                     CapabilityId capabilityFilter,
                                     SettingsApplicationServices settingsServices) {
        super(12);
        this.controller = java.util.Objects.requireNonNull(controller, "engine administration controller");
        this.settingsServices = settingsServices;
        this.dependencyPreparation = settingsServices == null ? null : settingsServices.dependencyPreparation();
        this.applicationRoot = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        setPadding(new Insets(4));
        engines.getItems().setAll(controller.engines().stream()
                .filter(view -> capabilityFilter == null
                        || capabilityFilter.equals(view.descriptor().capability()))
                .toList());
        engines.setPrefHeight(Math.max(140, engines.getItems().size() * 44.0));
        engines.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(EngineAdministrationController.EngineView item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.descriptor().displayName()
                        + " · " + item.descriptor().capability().value());
                setGraphic(empty || item == null ? null
                        : SemanticActionIcons.graphicFor(
                                item.descriptor().displayName() + " "
                                        + item.descriptor().capability().value(), 18));
            }
        });
        engines.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> render(selected));
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-message");
        Label summaryTitle = new Label("Resumen operativo");
        summaryTitle.getStyleClass().add("settings-engine-status-title");
        Label summary = new Label(engines.getItems().isEmpty()
                ? "No hay componentes registrados."
                : engines.getItems().size() + " componentes registrados. Selecciona uno para verificar, "
                + "instalar, importar, reparar, probar o abrir sus recursos.");
        summary.setWrapText(true);
        Button prepare = ActionButtonFactory.primary("Preparar voz local y herramientas de vídeo",
                this::prepareRecommended);
        getChildren().addAll(summaryTitle, summary, prepare,
                computeQueueSection(), status, engines, detail);
        if (settingsServices != null) {
            getChildren().add(1, preparationActions());
        }
        if (!engines.getItems().isEmpty()) engines.getSelectionModel().selectFirst();
        else render(null);
    }

    private VBox computeQueueSection() {
        Label title = new Label("Cola transversal de cómputo");
        title.getStyleClass().add("settings-engine-status-title");
        computeQueueStatus.setWrapText(true);
        computeQueue.setPrefHeight(120);
        computeQueue.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(ComputeQueueRow row,
                                                boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null
                        : (row.active() ? "En ejecución" : "En espera")
                        + " · " + row.entry().effectivePriority()
                        + " · " + row.entry().workload()
                        + " · " + row.entry().operationId());
                setGraphic(empty || row == null ? null
                        : SemanticActionIcons.graphicFor(
                                row.active() ? "En ejecución" : "En espera", 17));
            }
        });
        Button refresh = ActionButtonFactory.secondary(
                "Actualizar cola", this::refreshComputeQueue);
        Button cancel = ActionButtonFactory.danger(
                "Cancelar seleccionado", () -> {
                    ComputeQueueRow selected =
                            computeQueue.getSelectionModel().getSelectedItem();
                    if (selected == null) return;
                    controller.cancelCompute(
                            selected.entry().admissionId());
                    refreshComputeQueue();
                });
        VBox section = new VBox(7, title, computeQueueStatus, computeQueue,
                new HBox(8, refresh, cancel));
        refreshComputeQueue();
        return section;
    }

    private void refreshComputeQueue() {
        ComputeQueueSnapshot snapshot = controller.computeQueue();
        java.util.ArrayList<ComputeQueueRow> rows =
                new java.util.ArrayList<>();
        snapshot.active().forEach(entry -> rows.add(
                new ComputeQueueRow(entry, true)));
        snapshot.queued().forEach(entry -> rows.add(
                new ComputeQueueRow(entry, false)));
        computeQueue.getItems().setAll(rows);
        computeQueueStatus.setText(rows.isEmpty()
                ? "Cola de cómputo sin trabajos."
                : snapshot.active().size() + " en ejecución y "
                + snapshot.queued().size() + " en espera. La cancelación "
                + "activa se aplica en el siguiente límite seguro.");
    }

    private void render(EngineAdministrationController.EngineView view) {
        detail.getChildren().clear();
        if (view == null) {
            detail.getChildren().add(new Label("Capacidad no disponible: no hay motores registrados."));
            return;
        }
        Label title = new Label(view.descriptor().displayName());
        title.setGraphic(SemanticActionIcons.graphicFor(
                view.descriptor().displayName() + " "
                        + view.descriptor().capability().value(), 20));
        title.getStyleClass().add("settings-engine-status-title");
        Label capability = new Label("Capacidad: " + view.descriptor().capability().value()
                + " · Runtime: " + view.descriptor().runtimeKind());
        capability.setWrapText(true);
        detail.getChildren().addAll(title, capability);

        LinkedHashMap<String, TextInputControl> configurationInputs = fields(
                view.configuration().fields(), detail, "Parámetros de las acciones (no se guardan con la configuración general)");
        if (!view.presets().isEmpty()) {
            Label presets = new Label("Presets: " + String.join(", ", view.presets().stream()
                    .map(EnginePresetDescriptor::displayName).toList()));
            presets.setWrapText(true);
            detail.getChildren().add(presets);
        }
        detail.getChildren().add(ActionButtonFactory.secondary("Comprobar instalación actual", () -> check(view)));

        String currentComponent = "";
        for (EngineActionDescriptor action : view.actions()) {
            String actionComponent = action.metadata().getOrDefault("component", "");
            if (!actionComponent.isBlank() && !actionComponent.equals(currentComponent)) {
                long componentBytes = view.actions().stream()
                        .filter(candidate -> actionComponent.equals(
                                candidate.metadata().getOrDefault("component", "")))
                        .mapToLong(EngineActionDescriptor::approximateBytes)
                        .sum();
                Label componentTitle = new Label(actionComponent);
                componentTitle.getStyleClass().add("settings-engine-status-title");
                Label componentDescription = new Label(
                        "Depende del motor visual local y SD 1.5 · "
                                + String.format(java.util.Locale.ROOT, "%.2f GB adicionales",
                                componentBytes / 1_000_000_000.0)
                                + " · identidad y vestuario regionales para una intervención.");
                componentDescription.setWrapText(true);
                detail.getChildren().addAll(componentTitle, componentDescription);
                currentComponent = actionComponent;
            }
            VBox card = new VBox(6);
            Label actionTitle = new Label(action.displayName());
            Label description = new Label(action.description());
            description.setWrapText(true);
            card.getChildren().addAll(actionTitle, description);
            if (action.approximateBytes() > 0 || !action.metadata().isEmpty()) {
                String size = action.approximateBytes() <= 0 ? ""
                        : String.format(java.util.Locale.ROOT, "%.1f MiB",
                        action.approximateBytes() / (1024.0 * 1024.0));
                String license = action.metadata().getOrDefault("license", "");
                String source = action.metadata().getOrDefault("source", "");
                Label metadata = new Label(String.join(" · ",
                        java.util.stream.Stream.of(size,
                                        license.isBlank() ? "" : "Licencia " + license,
                                        source.isBlank() ? "" : "Fuente " + source)
                                .filter(value -> !value.isBlank()).toList()));
                metadata.setWrapText(true);
                card.getChildren().add(metadata);
            }
            LinkedHashMap<String, TextInputControl> actionInputs = fields(action.inputs(), card, "");
            Button run = ActionButtonFactory.secondary(action.displayName(), null);
            Button cancel = ActionButtonFactory.secondary("Cancelar", null);
            cancel.setDisable(true);
            run.setOnAction(event -> {
                LinkedHashMap<String, String> values = values(configurationInputs);
                values.putAll(values(actionInputs));
                run(view, action, values, run, cancel);
            });
            card.getChildren().add(new HBox(8, run, cancel));
            detail.getChildren().add(card);
        }
    }

    private record ComputeQueueRow(
            ComputeQueueSnapshot.Entry entry, boolean active) { }

    private LinkedHashMap<String, TextInputControl> fields(
            java.util.List<EngineConfigurationField> fields, VBox host, String title) {
        LinkedHashMap<String, TextInputControl> result = new LinkedHashMap<>();
        if (fields == null || fields.isEmpty()) return result;
        if (title != null && !title.isBlank()) host.getChildren().add(new Label(title));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        int row = 0;
        for (EngineConfigurationField field : fields) {
            TextField input = field.type() == ConfigurationFieldType.SECRET ? new PasswordField()
                    : StudioFormControls.textField(field.defaultValue());
            if (field.type() == ConfigurationFieldType.SECRET) input.setText(field.defaultValue());
            input.setPromptText(field.description());
            input.setAccessibleText(field.label());
            grid.add(new Label(field.label()), 0, row);
            if (field.type() == ConfigurationFieldType.READ_ONLY_PATH) {
                input.setEditable(false);
                Button open = ActionButtonFactory.secondary("Abrir carpeta", null);
                open.setOnAction(event -> openManagedLocation(input.getText()));
                grid.add(new HBox(8, input, open), 1, row++);
            } else if (field.type() == ConfigurationFieldType.FILE
                    || field.type() == ConfigurationFieldType.DIRECTORY) {
                Button browse = ActionButtonFactory.secondary("Examinar…", null);
                browse.setAccessibleText("Seleccionar " + field.label());
                browse.setOnAction(event -> {
                    java.io.File selected;
                    if (field.type() == ConfigurationFieldType.DIRECTORY) {
                        javafx.stage.DirectoryChooser chooser = NativeSourceChooser.directoryChooser();
                        chooser.setTitle(field.label());
                        selected = chooser.showDialog(host.getScene() == null ? null : host.getScene().getWindow());
                    } else {
                        javafx.stage.FileChooser chooser = NativeSourceChooser.fileChooser();
                        chooser.setTitle(field.label());
                        selected = chooser.showOpenDialog(host.getScene() == null ? null : host.getScene().getWindow());
                    }
                    if (selected != null) input.setText(selected.toPath().toAbsolutePath().normalize().toString());
                });
                grid.add(new HBox(8, input, browse), 1, row++);
            } else {
                grid.add(input, 1, row++);
            }
            result.put(field.key(), input);
        }
        host.getChildren().add(grid);
        return result;
    }

    private void check(EngineAdministrationController.EngineView view) {
        status.setText("Comprobando disponibilidad…");
        Task<EngineReadiness> task = new Task<>() {
            @Override protected EngineReadiness call() { return controller.readiness(view.descriptor().id()); }
        };
        task.setOnSucceeded(event -> status.setText(task.getValue().summary()
                + (task.getValue().recommendedActions().isEmpty() ? ""
                : " " + String.join(" ", task.getValue().recommendedActions()))));
        task.setOnFailed(event -> status.setText("Error recuperable: " + rootMessage(task.getException())));
        Thread.ofVirtual().name("engine-readiness").start(task);
    }

    private void run(EngineAdministrationController.EngineView view, EngineActionDescriptor action,
                     Map<String, String> values, Button runButton, Button cancelButton) {
        if (!preparationAllowed()) return;
        for (EngineConfigurationField field : java.util.stream.Stream.concat(
                view.configuration().fields().stream(), action.inputs().stream()).toList()) {
            String value = values.getOrDefault(field.key(), "").strip();
            try {
                if (field.required() && value.isBlank()) throw new IllegalArgumentException();
                if (!value.isBlank()) {
                    if (field.type() == ConfigurationFieldType.INTEGER) Integer.parseInt(value);
                    if (field.type() == ConfigurationFieldType.DECIMAL && !Double.isFinite(Double.parseDouble(value)))
                        throw new IllegalArgumentException();
                    if (field.type() == ConfigurationFieldType.BOOLEAN && !value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                        throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException invalid) {
                status.setText("Revisa el campo «" + field.label() + "»: falta un valor o su formato no es válido. No se inició la acción.");
                return;
            }
        }
        boolean managedDownload = controller.isManagedDownload(view.descriptor().id(), action.id());
        ManagedDownloadDecision downloadDecision = null;
        if (managedDownload) {
            try {
                ManagedDownloadPreflight preflight =
                        controller.inspectDownload(view.descriptor().id(), action.id(), values);
                downloadDecision = decideManagedDownload(action.displayName(), preflight);
                if (downloadDecision == ManagedDownloadDecision.CANCEL) return;
            } catch (IOException failure) {
                showError("No se pudo inspeccionar el recurso local", failure);
                return;
            }
        } else if (action.confirmationRequired() || EngineActionId.SMOKE_TEST.equals(action.id())) {
            Alert confirmation = StudioMessageDialog.create(
                    owner(),
                    Alert.AlertType.CONFIRMATION,
                    "Motores y dependencias",
                    action.displayName(),
                    action.description(),
                    action.metadata().isEmpty() ? "" : action.metadata().toString(),
                    ButtonType.OK,
                    ButtonType.CANCEL);
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        }
        ManagedDownloadDecision selectedDecision = downloadDecision;
        AtomicBoolean cancelled = new AtomicBoolean();
        busy(true);
        runButton.setDisable(true);
        cancelButton.setDisable(false);
        status.setText("Iniciando " + action.displayName() + "…");
        Task<EngineActionResult> task = new Task<>() {
            @Override protected EngineActionResult call() throws Exception {
                ExecutionContext context = new ExecutionContext("engine-admin-" + action.id().value(),
                        () -> cancelled.get() || isCancelled(),
                        (stage, amount, message) -> Platform.runLater(() -> status.setText(message)),
                        ExecutionPolicy.defaults(), ResourceLease.NONE);
                if (managedDownload) {
                    return controller.executeDownload(
                            view.descriptor().id(), action.id(), values, selectedDecision, context);
                }
                return controller.execute(view.descriptor().id(), action.id(), values, context);
            }
        };
        cancelButton.setOnAction(event -> { cancelled.set(true); task.cancel(true); });
        task.setOnSucceeded(event -> {
            busy(false);
            status.setText(task.getValue().message());
            finish(runButton, cancelButton);
        });
        task.setOnCancelled(event -> {
            busy(false);
            status.setText("Acción cancelada; se conservaron solo recursos recuperables.");
            finish(runButton, cancelButton);
        });
        task.setOnFailed(event -> {
            busy(false);
            status.setText("Error recuperable: " + rootMessage(task.getException()));
            finish(runButton, cancelButton);
        });
        Thread.ofVirtual().name("engine-administration").start(task);
    }

    private ManagedDownloadDecision decideManagedDownload(
            String actionName,
            ManagedDownloadPreflight preflight) {
        ButtonType useExisting = NativeDialogResponse.button(
                "Usar el instalado", ButtonBar.ButtonData.OK_DONE);
        ButtonType redownload = NativeDialogResponse.button(
                preflight.state() == ManagedDownloadState.MISSING
                        ? "Descargar" : "Volver a descargar",
                ButtonBar.ButtonData.APPLY);
        ButtonType cancel = NativeDialogResponse.button(
                "Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog;
        if (preflight.valid()) {
            dialog = StudioMessageDialog.create(
                    owner(),
                    Alert.AlertType.CONFIRMATION,
                    "Motores y dependencias",
                    "El recurso ya está instalado y es válido",
                    "Puedes reutilizarlo sin conexión, volver a descargarlo explícitamente o cancelar.",
                    preflight.technicalDetails(),
                    useExisting,
                    redownload,
                    cancel);
        } else {
            String humanMessage = switch (preflight.state()) {
                case MISSING -> "El recurso no está instalado. La descarga solo comenzará si la confirmas.";
                case INVALID -> "El recurso local no es válido. Puedes repararlo mediante una nueva descarga "
                        + "o cancelar y usar la acción Importar.";
                case PARTIAL -> "Hay una descarga parcial. Puedes repararla mediante una nueva descarga "
                        + "o cancelar y usar la acción Importar.";
                case VALID -> throw new IllegalStateException("Estado válido tratado previamente.");
            };
            dialog = StudioMessageDialog.create(
                    owner(),
                    Alert.AlertType.CONFIRMATION,
                    "Motores y dependencias",
                    actionName,
                    humanMessage,
                    preflight.technicalDetails(),
                    redownload,
                    cancel);
        }
        ButtonType selected = dialog.showAndWait().orElse(cancel);
        if (selected == useExisting) return ManagedDownloadDecision.USE_EXISTING;
        if (selected == redownload) return ManagedDownloadDecision.REDOWNLOAD;
        return ManagedDownloadDecision.CANCEL;
    }

    private static LinkedHashMap<String, String> values(Map<String, TextInputControl> inputs) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        inputs.forEach((key, input) -> values.put(key, input.getText()));
        return values;
    }

    private static void finish(Button runButton, Button cancelButton) {
        runButton.setDisable(false);
        cancelButton.setDisable(true);
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != null) current = current.getCause();
        return current == null || current.getMessage() == null
                ? "operación no disponible" : current.getMessage();
    }

    private void prepareRecommended() {
        if (!preparationAllowed()) return;
        if (settingsServices != null) {
            OperationalSettings operational = operationalSettings();
            ManagedDownloadDecision voiceDecision = decideManagedDownload(
                    "Preparar voz local",
                    dependencyPreparation.preflightLocalVoice(operational, applicationRoot));
            if (voiceDecision == ManagedDownloadDecision.CANCEL) return;
            ManagedDownloadDecision videoDecision = decideManagedDownload(
                    "Preparar herramientas de video",
                    dependencyPreparation.preflightVideoTools(operational, applicationRoot));
            if (videoDecision == ManagedDownloadDecision.CANCEL) return;
            runPreparation("Preparando dependencias recomendadas", () -> {
                ModelSetupProgressListener progress = this::reportPreparationProgress;
                String voice = dependencyPreparation.downloadLocalVoice(
                        operational, applicationRoot, progress, voiceDecision);
                String video = dependencyPreparation.downloadVideoTools(
                        operational, applicationRoot, progress, videoDecision);
                return voice + " " + video;
            });
            return;
        }
        if (engines.getItems().isEmpty()) {
            status.setText("No hay componentes registrados para preparar.");
            return;
        }
        status.setText("Comprobando los componentes recomendados. "
                + "Las descargas pesadas nunca se iniciarán sin confirmación.");
        engines.getSelectionModel().selectFirst();
        check(engines.getItems().getFirst());
    }

    private javafx.scene.layout.FlowPane preparationActions() {
        Button voice = ActionButtonFactory.secondary("Descargar voz local", () ->
                preflightAndRun("Descargar voz local",
                        () -> dependencyPreparation.preflightLocalVoice(
                                operationalSettings(), applicationRoot),
                        decision -> dependencyPreparation.downloadLocalVoice(
                                operationalSettings(), applicationRoot,
                                this::reportPreparationProgress, decision)));
        Button video = ActionButtonFactory.secondary("Descargar herramientas de video", () ->
                preflightAndRun("Descargar herramientas de video",
                        () -> dependencyPreparation.preflightVideoTools(
                                operationalSettings(), applicationRoot),
                        decision -> dependencyPreparation.downloadVideoTools(
                                operationalSettings(), applicationRoot,
                                this::reportPreparationProgress, decision)));
        Button image = ActionButtonFactory.secondary("Descargar modelo visual", () -> {
            if (!preparationAllowed()) return;
            ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(
                    operationalSettings().imageGeneration().preset());
            if (profile.requiresAuthentication()) {
                importProviderModel(profile);
                return;
            }
            preflightAndRun("Descargar " + profile.displayName(),
                    () -> dependencyPreparation.preflightVisualModel(
                            operationalSettings(), applicationRoot),
                    decision -> dependencyPreparation.downloadVisualModel(
                            operationalSettings(), applicationRoot,
                            this::reportPreparationProgress, decision));
        });
        Button ocr = ActionButtonFactory.secondary("Importar OCR local", this::importOcr);
        Button openFolder = ActionButtonFactory.secondary("Abrir carpeta administrada", () -> {
            try {
                java.awt.Desktop.getDesktop().open(applicationRoot.toFile());
            } catch (Exception failure) {
                status.setText("No se pudo abrir la carpeta: " + rootMessage(failure));
            }
        });
        Button providerImport = ActionButtonFactory.secondary("Importar FLUX descargado…", () -> {
            if (!preparationAllowed()) return;
            ChoiceDialog<ImageModelPackageProfile> choose = new ChoiceDialog<>(
                    ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT,
                    ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
            choose.setTitle("Modelo descargado");
            choose.setHeaderText("Selecciona el modelo que descargaste del proveedor");
            com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler.apply(choose, owner());
            choose.showAndWait().ifPresent(this::importProviderModel);
        });
        var actions = new javafx.scene.layout.FlowPane(8, 8, voice, video, image, providerImport, ocr, openFolder);
        return actions;
    }

    private void importProviderModel(ImageModelPackageProfile profile) {
        Label instructions = new Label("Descarga los archivos desde la web del proveedor. Inicia sesión y acepta allí sus condiciones. "
                + "Después selecciona los archivos o la carpeta descargada. DocuPodcast no solicita tu contraseña ni token.\n\n"
                + profile.manualSetupInstructions());
        instructions.setWrapText(true);
        Hyperlink website = new Hyperlink(profile.providerUrl());
        website.setOnAction(event -> {
            try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(profile.providerUrl())); }
            catch (Exception failure) { showError("No se pudo abrir la web del proveedor", failure); }
        });
        CheckBox accepted = StudioFormControls.checkBox();
        accepted.setText("Ya tengo acceso y he aceptado las condiciones del proveedor para este modelo.");
        ButtonType files = new ButtonType("Seleccionar archivos…");
        ButtonType folder = new ButtonType("Seleccionar carpeta…");
        Dialog<ButtonType> dialog = com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioDialogShell.dialog();
        dialog.setTitle("Importar " + profile.displayName());
        dialog.getDialogPane().setContent(new VBox(12, instructions, website, accepted));
        dialog.getDialogPane().getButtonTypes().setAll(files, folder, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(files).disableProperty().bind(accepted.selectedProperty().not());
        dialog.getDialogPane().lookupButton(folder).disableProperty().bind(accepted.selectedProperty().not());
        dialog.setResultConverter(button -> button);
        com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler.apply(dialog, owner());
        ButtonType choice = dialog.showAndWait().orElse(ButtonType.CANCEL);
        java.util.List<Path> sources;
        if (choice == folder) {
            var chooser = NativeSourceChooser.directoryChooser();
            var selected = chooser.showDialog(owner());
            if (selected == null) return;
            sources = java.util.List.of(selected.toPath());
        } else if (choice == files) {
            var chooser = NativeSourceChooser.fileChooser();
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Modelos descargados", "*.safetensors"));
            var selected = chooser.showOpenMultipleDialog(owner());
            if (selected == null || selected.isEmpty()) return;
            sources = selected.stream().map(java.io.File::toPath).toList();
        } else return;
        runPreparation("Importando componentes descargados", () -> {
            java.util.List<String> messages = new java.util.ArrayList<>();
            for (Path source : sources) {
                var result = settingsServices.importFluxComponents().importFrom(source, applicationRoot,
                        this::reportPreparationProgress, profile);
                messages.add(result.userMessage());
                if (!result.success()) throw new java.io.IOException(String.join("\n", messages));
            }
            new com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxLicenseAcceptanceStore()
                    .accept(applicationRoot, profile);
            return String.join("\n", messages) + "\n" + dependencyPreparation.configureImportedVisualModel(profile, applicationRoot);
        });
    }

    private void importOcr() {
        if (!preparationAllowed()) return;
        javafx.stage.DirectoryChooser chooser = NativeSourceChooser.directoryChooser();
        chooser.setTitle("Seleccionar runtime OCR local con idiomas spa y eng");
        java.io.File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        runPreparation("Importando OCR local", () ->
                dependencyPreparation.importLocalOcr(
                        selected.toPath(), applicationRoot, this::reportPreparationProgress));
    }

    private void preflightAndRun(
            String title,
            Callable<ManagedDownloadPreflight> inspection,
            java.util.function.Function<ManagedDownloadDecision, String> operation) {
        if (!preparationAllowed()) return;
        try {
            ManagedDownloadDecision decision = decideManagedDownload(title, inspection.call());
            if (decision != ManagedDownloadDecision.CANCEL) {
                runPreparation(title, () -> operation.apply(decision));
            }
        } catch (Exception failure) {
            showError("No se pudo inspeccionar la dependencia", failure);
        }
    }

    private void runPreparation(String label, Callable<String> operation) {
        if (!preparationAllowed()) return;
        busy(true);
        status.setText(label + "…");
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> {
            busy(false);
            status.setText(task.getValue());
            configurationChanged.run();
        });
        task.setOnFailed(event -> {
            busy(false);
            status.setText("No se pudo completar la preparación: " + rootMessage(task.getException()));
        });
        Thread.ofVirtual().name("dependency-preparation").start(task);
    }

    private OperationalSettings operationalSettings() {
        try {
            return settingsServices.loadOperationalSettings().load();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("No se pudo cargar la configuración operativa.", failure);
        }
    }

    private void reportPreparationProgress(String message) {
        if (message != null && !message.isBlank()) {
            Platform.runLater(() -> status.setText(message));
        }
    }

    private javafx.stage.Window owner() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void openManagedLocation(String value) {
        try {
            Path location = Path.of(value).toAbsolutePath().normalize();
            Path folder = java.nio.file.Files.isDirectory(location) ? location : location.getParent();
            if (folder == null) throw new IOException("La ubicación no tiene una carpeta administrada.");
            java.nio.file.Files.createDirectories(folder);
            java.awt.Desktop.getDesktop().open(folder.toFile());
        } catch (Exception failure) {
            showError("No se pudo abrir la carpeta administrada", failure);
        }
    }

    private void showError(String header, Throwable failure) {
        StudioMessageDialog.create(
                owner(),
                Alert.AlertType.ERROR,
                "Motores y dependencias",
                header,
                rootMessage(failure),
                StudioMessageDialog.technicalDetail(failure),
                ButtonType.OK).showAndWait();
    }
}
