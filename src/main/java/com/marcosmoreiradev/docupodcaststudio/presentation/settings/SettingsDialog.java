package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsValidationReport;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
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

    private final MediaEnginePlatform mediaEngines;

    public SettingsDialog(MediaEnginePlatform mediaEngines) {
        this.mediaEngines = java.util.Objects.requireNonNull(mediaEngines, "media engine platform");
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

    public void showFirstUseSetup(Window owner, SettingsApplicationServices services) {
        show(owner, services, SettingsSection.ENGINES, SettingsSupportActions.unavailable());
    }

    private void show(Window owner, SettingsApplicationServices services, SettingsSection initial,
                      SettingsSupportActions supportActions) {
        SettingsFormModel form = new SettingsFormModel(load(services));
        Label status = new Label(services == null
                ? "Configuración abierta sin persistencia; los cambios no se guardarán."
                : "Cambios pendientes.");
        status.setWrapText(true);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Configuración de DocuPodcast Studio");
        dialog.setHeaderText("Administración transversal de capacidades y ajustes operativos");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(content(form, services, supportActions, status, initial));
        dialog.getDialogPane().setPrefSize(1040, 680);
        dialog.getDialogPane().setMinSize(900, 580);
        DialogStyler.apply(dialog, owner);
        dialog.showAndWait();
    }

    private Node content(SettingsFormModel form, SettingsApplicationServices services,
                         SettingsSupportActions supportActions, Label status, SettingsSection initial) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("settings-root");
        ListView<SettingsSection> navigation = new ListView<>();
        navigation.getItems().setAll(SettingsSection.values());
        navigation.setPrefWidth(220);
        navigation.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(SettingsSection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label);
            }
        });
        VBox pageHost = new VBox();
        pageHost.setFillWidth(true);
        navigation.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) pageHost.getChildren().setAll(page(selected, form, supportActions));
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

    private Node page(SettingsSection section, SettingsFormModel form, SettingsSupportActions supportActions) {
        Node body = switch (section) {
            case ENGINES -> new EngineAdministrationPane(new EngineAdministrationController(mediaEngines));
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
                    "Preferencia de codificación", form.videoEncoderPolicy);
            case VIDEO_FINAL -> grid(
                    "Resolución", form.resolutionPreset,
                    "FPS administrados por el preset", new Label("La petición final se traduce en el adaptador registrado."),
                    "Duración de visual silencioso", form.silentVisualBlockSeconds);
            case STORAGE -> grid(
                    "Directorio de modelos", form.modelsDirectory,
                    "Directorio de exportaciones", form.exportsDirectory,
                    "Logs", form.writeLogs,
                    "Manifests", form.exportManifests);
            case SUPPORT -> supportPage(supportActions);
        };
        Label title = new Label(section.label);
        title.getStyleClass().add("settings-page-title");
        VBox content = new VBox(14, title, body);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private static GridPane grid(Object... rows) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int index = 0, row = 0; index + 1 < rows.length; index += 2, row++) {
            Label label = new Label(String.valueOf(rows[index]));
            Node field = (Node) rows[index + 1];
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
        try {
            OperationalSettingsValidationReport report = services.saveOperationalSettings().save(form.toSettings());
            if (!report.errors().isEmpty()) status.setText("No se guardó: " + String.join(" ", report.errors()));
            else if (!report.warnings().isEmpty()) status.setText("Guardado con avisos: " + String.join(" ", report.warnings()));
            else status.setText("Configuración guardada.");
        } catch (IOException failure) {
            status.setText("Error recuperable al guardar: " + failure.getMessage());
        }
    }

    private enum SettingsSection {
        READING("Lectura"), PLAYBACK("Reproducción"), ENGINES("Motores"),
        PERFORMANCE("Rendimiento"), VIDEO_FINAL("Video final"),
        STORAGE("Almacenamiento"), SUPPORT("Soporte");
        private final String label;
        SettingsSection(String label) { this.label = label; }
    }
}
