package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Owns the engine/device settings controls for the Voices workspace. */
final class VoiceEngineSettingsControls {
    private final DocuPodcastShellViewModel viewModel;
    private final Consumer<List<String>> summarySink;
    private final ComboBox<EngineModeChoice> engineModeSelector = new ComboBox<>();
    private final ComboBox<ComputeDeviceChoice> computeDeviceSelector = new ComboBox<>();
    private final Label engineConfigurationStatus = new Label("Selecciona motor y dispositivo para sincronizarlo con Configuración.");
    private boolean refreshingEngineControls;

    VoiceEngineSettingsControls(DocuPodcastShellViewModel viewModel, Consumer<List<String>> summarySink) {
        this.viewModel = viewModel;
        this.summarySink = summarySink;
        configure();
    }

    VBox selectionSection() {
        VBox box = section("Selección de motor y dispositivo");
        VBox form = new VBox(8);
        form.getStyleClass().add("voice-engine-form");
        Label engineLabel = new Label("Motor de voz");
        engineLabel.getStyleClass().add("document-side-text");
        detachNode(engineModeSelector);
        engineModeSelector.setMaxWidth(Double.MAX_VALUE);
        Label deviceLabel = new Label("Dispositivo de renderizado");
        deviceLabel.getStyleClass().add("document-side-text");
        detachNode(computeDeviceSelector);
        computeDeviceSelector.setMaxWidth(Double.MAX_VALUE);
        detachNode(engineConfigurationStatus);
        form.getChildren().addAll(engineLabel, engineModeSelector, deviceLabel, computeDeviceSelector, engineConfigurationStatus);
        Label note = new Label("El dispositivo seleccionado se envía a los motores de voz. Si el runtime local no puede usar esa GPU, la app lo informará sin prometer aceleración falsa.");
        note.setWrapText(true);
        note.getStyleClass().add("document-side-text");
        box.getChildren().addAll(form, note);
        return box;
    }

    void refresh() {
        OperationalSettings settings = loadOperationalSettingsSafely();
        ComputeEnvironmentReport compute = viewModel.applicationServices().settings().inspectComputeEnvironment().inspect(settings);
        refreshingEngineControls = true;
        try {
            engineModeSelector.setValue(engineModeSelector.getItems().stream()
                    .filter(choice -> choice.mode().equalsIgnoreCase(settings.tts().engineMode()))
                    .findFirst()
                    .orElseGet(() -> engineModeSelector.getItems().stream()
                            .filter(choice -> choice.mode().equals(TtsEngineModes.TEST))
                            .findFirst()
                            .orElse(null)));
            computeDeviceSelector.getItems().setAll(computeDeviceChoices(compute));
            computeDeviceSelector.setValue(selectedComputeChoice(compute, settings));
            engineConfigurationStatus.setText(engineStatusText(settings, compute));
        } finally {
            refreshingEngineControls = false;
        }
    }

    private void configure() {
        engineModeSelector.getStyleClass().add("voice-library-combo");
        engineModeSelector.getItems().setAll(
                new EngineModeChoice(TtsEngineModes.ADVANCED_AI, "Voz IA avanzada", "Voces por muestra y muchas emociones."),
                new EngineModeChoice(TtsEngineModes.LOCAL_SIMPLE, "Voz local simple", "Lectura neutral liviana."),
                new EngineModeChoice(TtsEngineModes.TEST, "Modo de prueba", "Valida el flujo sin motor real."));
        engineModeSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshingEngineControls && newValue != null) {
                saveEngineModeSelection(newValue);
            }
        });
        computeDeviceSelector.getStyleClass().add("voice-library-combo");
        computeDeviceSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshingEngineControls && newValue != null) {
                saveComputeDeviceSelection(newValue);
            }
        });
        engineConfigurationStatus.getStyleClass().add("voice-engine-status");
        engineConfigurationStatus.setWrapText(true);
    }

    private List<ComputeDeviceChoice> computeDeviceChoices(ComputeEnvironmentReport report) {
        ArrayList<ComputeDeviceChoice> choices = new ArrayList<>();
        choices.add(new ComputeDeviceChoice("auto", "Automático", true));
        choices.add(new ComputeDeviceChoice("cpu", "CPU", true));
        for (ComputeDeviceDescriptor device : report.devices()) {
            if (device.gpu()) {
                choices.add(new ComputeDeviceChoice(device.id(), device.displayName(), false));
            }
        }
        return choices;
    }

    private ComputeDeviceChoice selectedComputeChoice(ComputeEnvironmentReport report, OperationalSettings settings) {
        String selected = settings.compute().selectedDeviceId();
        if (settings.compute().policy() == ComputeDevicePolicy.CPU_ONLY) {
            selected = "cpu";
        }
        if (selected == null || selected.isBlank()) {
            selected = "auto";
        }
        String target = selected;
        return computeDeviceSelector.getItems().stream()
                .filter(choice -> choice.id().equalsIgnoreCase(target))
                .findFirst()
                .orElseGet(() -> computeDeviceSelector.getItems().isEmpty() ? null : computeDeviceSelector.getItems().get(0));
    }

    private String engineStatusText(OperationalSettings settings, ComputeEnvironmentReport compute) {
        String engine = TtsEngineModes.safeLabel(settings.tts().engineMode());
        String device = compute.selectedDevice()
                .map(ComputeDeviceDescriptor::displayName)
                .orElse(settings.compute().policy() == ComputeDevicePolicy.CPU_ONLY ? "CPU" : "Automático");
        String gpu = compute.gpuDetected() ? "GPU detectada" : "sin GPU compatible detectada";
        String warnings = compute.warnings().isEmpty() ? "" : " · " + String.join(" · ", compute.warnings());
        if (TtsEngineModes.LOCAL_SIMPLE.equalsIgnoreCase(settings.tts().engineMode())) {
            return engine + " · Dispositivo solicitado: " + device + " · " + gpu
                    + " · Voz local simple recibirá ese dispositivo si el runtime lo soporta." + warnings + ".";
        }
        return engine + " · " + device + " · " + gpu + warnings + ".";
    }

    private void saveEngineModeSelection(EngineModeChoice selection) {
        OperationalSettings current = loadOperationalSettingsSafely();
        OperationalSettings updated;
        if (TtsEngineModes.ADVANCED_AI.equals(selection.mode())) {
            updated = viewModel.applicationServices().settings().selectXttsAsEngine().select(current);
        } else if (TtsEngineModes.LOCAL_SIMPLE.equals(selection.mode())) {
            updated = viewModel.applicationServices().settings().selectPiperAsEngine().select(current);
        } else {
            updated = new OperationalSettings(current.readingDocument(), current.playbackBuffer(),
                    new OperationalSettings.TtsEngineSettings(TtsEngineModes.TEST, "", "Modo de prueba", current.tts().language(),
                            current.tts().voiceProfileId(), current.tts().timeoutSeconds(), current.tts().maxRetries(),
                            current.tts().xttsDownloadBaseUrl(), current.tts().piperRuntimeZipUrl(),
                            current.tts().piperDefaultVoiceUrl(), current.tts().piperDefaultVoiceMetadataUrl()),
                    current.video(), current.imageGeneration(), current.frameGeneration(), current.compute(),
                    current.ocr(), current.storage(), current.diagnostics());
        }
        saveOperationalSettings(updated, "Motor activo actualizado: " + selection.label() + ".");
    }

    private void saveComputeDeviceSelection(ComputeDeviceChoice selection) {
        OperationalSettings current = loadOperationalSettingsSafely();
        ComputeDevicePolicy policy = selection.cpu() && "cpu".equals(selection.id())
                ? ComputeDevicePolicy.CPU_ONLY
                : "auto".equals(selection.id()) ? ComputeDevicePolicy.AUTO : ComputeDevicePolicy.SPECIFIC_DEVICE;
        OperationalSettings updated = new OperationalSettings(current.readingDocument(), current.playbackBuffer(), current.tts(), current.video(),
                current.imageGeneration(),
                current.frameGeneration(),
                new OperationalSettings.ComputeSettings(policy, "auto".equals(selection.id()) ? "" : selection.id(), policy.canUseGpu(), current.compute().allowGpuForVideo(), current.compute().videoEncoderPolicy()),
                current.ocr(),
                current.storage(), current.diagnostics());
        saveOperationalSettings(updated, "Dispositivo de renderizado actualizado: " + selection.label() + ".");
    }

    private OperationalSettings loadOperationalSettingsSafely() {
        try {
            return viewModel.applicationServices().settings().loadOperationalSettings().load();
        } catch (IOException | RuntimeException ex) {
            return OperationalSettings.defaults();
        }
    }

    private void saveOperationalSettings(OperationalSettings settings, String message) {
        try {
            viewModel.applicationServices().settings().saveOperationalSettings().save(settings);
            summarySink.accept(List.of(message, "La misma configuración queda sincronizada con la ventana Configuración."));
            refresh();
        } catch (IOException | RuntimeException ex) {
            summarySink.accept(List.of("No se pudo guardar la configuración de voz: " + ex.getMessage()));
        }
    }

    private static VBox section(String title) {
        VBox box = new VBox(8);
        Label label = new Label(title);
        label.getStyleClass().add("document-side-section-title");
        box.getChildren().add(label);
        return box;
    }

    private static void detachNode(Node node) {
        if (node == null) {
            return;
        }
        Parent parent = node.getParent();
        if (parent instanceof Pane pane) {
            pane.getChildren().remove(node);
        }
    }

    private record EngineModeChoice(String mode, String label, String detail) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ComputeDeviceChoice(String id, String label, boolean cpu) {
        @Override
        public String toString() {
            return label;
        }
    }
}
