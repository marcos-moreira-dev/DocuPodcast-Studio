package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Declarative engine administration view with provider-neutral forms and lifecycle. */
public final class EngineAdministrationPane extends VBox {
    private final EngineAdministrationController controller;
    private final ListView<EngineAdministrationController.EngineView> engines = new ListView<>();
    private final VBox detail = new VBox(10);
    private final Label status = new Label("Selecciona un motor para comprobarlo.");

    public EngineAdministrationPane(EngineAdministrationController controller) {
        super(12);
        this.controller = java.util.Objects.requireNonNull(controller, "engine administration controller");
        setPadding(new Insets(4));
        engines.getItems().setAll(controller.engines());
        engines.setPrefHeight(Math.max(140, engines.getItems().size() * 44.0));
        engines.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(EngineAdministrationController.EngineView item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.descriptor().displayName()
                        + " · " + item.descriptor().capability().value());
            }
        });
        engines.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> render(selected));
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-message");
        getChildren().addAll(engines, detail, status);
        if (!engines.getItems().isEmpty()) engines.getSelectionModel().selectFirst();
        else render(null);
    }

    private void render(EngineAdministrationController.EngineView view) {
        detail.getChildren().clear();
        if (view == null) {
            detail.getChildren().add(new Label("Capacidad no disponible: no hay motores registrados."));
            return;
        }
        Label title = new Label(view.descriptor().displayName());
        title.getStyleClass().add("settings-engine-status-title");
        Label capability = new Label("Capacidad: " + view.descriptor().capability().value()
                + " · Runtime: " + view.descriptor().runtimeKind());
        capability.setWrapText(true);
        detail.getChildren().addAll(title, capability);

        LinkedHashMap<String, TextInputControl> configurationInputs = fields(
                view.configuration().fields(), detail, "Configuración");
        if (!view.presets().isEmpty()) {
            Label presets = new Label("Presets: " + String.join(", ", view.presets().stream()
                    .map(EnginePresetDescriptor::displayName).toList()));
            presets.setWrapText(true);
            detail.getChildren().add(presets);
        }
        detail.getChildren().add(ActionButtonFactory.secondary("Comprobar disponibilidad", () -> check(view)));

        for (EngineActionDescriptor action : view.actions()) {
            if (EngineActionId.SMOKE_TEST.equals(action.id())) continue;
            VBox card = new VBox(6);
            Label actionTitle = new Label(action.displayName());
            Label description = new Label(action.description());
            description.setWrapText(true);
            card.getChildren().addAll(actionTitle, description);
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

    private static LinkedHashMap<String, TextInputControl> fields(
            java.util.List<EngineConfigurationField> fields, VBox host, String title) {
        LinkedHashMap<String, TextInputControl> result = new LinkedHashMap<>();
        if (fields == null || fields.isEmpty()) return result;
        if (title != null && !title.isBlank()) host.getChildren().add(new Label(title));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        int row = 0;
        for (EngineConfigurationField field : fields) {
            TextField input = new TextField(field.defaultValue());
            input.setPromptText(field.description());
            input.setAccessibleText(field.label());
            grid.add(new Label(field.label()), 0, row);
            grid.add(input, 1, row++);
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
        if (action.confirmationRequired()) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    action.description(), ButtonType.OK, ButtonType.CANCEL);
            confirmation.setHeaderText(action.displayName());
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        }
        AtomicBoolean cancelled = new AtomicBoolean();
        runButton.setDisable(true);
        cancelButton.setDisable(false);
        status.setText("Iniciando " + action.displayName() + "…");
        Task<EngineActionResult> task = new Task<>() {
            @Override protected EngineActionResult call() throws Exception {
                ExecutionContext context = new ExecutionContext("engine-admin-" + action.id().value(),
                        () -> cancelled.get() || isCancelled(),
                        (stage, amount, message) -> Platform.runLater(() -> status.setText(message)),
                        ExecutionPolicy.defaults(), ResourceLease.NONE);
                return controller.execute(view.descriptor().id(), action.id(), values, context);
            }
        };
        cancelButton.setOnAction(event -> { cancelled.set(true); task.cancel(true); });
        task.setOnSucceeded(event -> { status.setText(task.getValue().message()); finish(runButton, cancelButton); });
        task.setOnCancelled(event -> {
            status.setText("Acción cancelada; se conservaron solo recursos recuperables.");
            finish(runButton, cancelButton);
        });
        task.setOnFailed(event -> {
            status.setText("Error recuperable: " + rootMessage(task.getException()));
            finish(runButton, cancelButton);
        });
        Thread.ofVirtual().name("engine-administration").start(task);
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
}
