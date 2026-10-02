package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.ListVoiceEngineOperationalStatesUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.VoiceEngineOperationalState;
import com.marcosmoreiradev.docupodcaststudio.application.compatibility.media.LegacyVoiceEngineSettingsMapper;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.VoiceSynthesisEngine;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.detachNode;
import static com.marcosmoreiradev.docupodcaststudio.presentation.voice.VoiceWorkspaceLayout.section;

/** Owns the engine/device settings controls embedded in the Voices workspace. */
final class VoiceEngineSettingsControls {
    static final String ENGINE_SELECTOR_ID = "voiceEngineSelector";
    static final String DEVICE_SELECTOR_ID = "voiceComputeDeviceSelector";
    static final String STATUS_ID = "voiceEngineOperationalStatus";

    private final Backend backend;
    private final Consumer<List<String>> summarySink;
    private final ComboBox<EngineModeChoice> engineModeSelector = StudioFormControls.comboBox();
    private final ComboBox<ComputeDeviceChoice> computeDeviceSelector = StudioFormControls.comboBox();
    private final Label engineConfigurationStatus = new Label(
            "Selecciona motor y dispositivo para sincronizarlos con Configuración.");
    private boolean refreshingEngineControls;

    VoiceEngineSettingsControls(DocuPodcastShellViewModel viewModel,
                                Consumer<List<String>> summarySink) {
        this(new ApplicationBackend(viewModel), summarySink);
    }

    VoiceEngineSettingsControls(Backend backend, Consumer<List<String>> summarySink) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.summarySink = summarySink == null ? ignored -> { } : summarySink;
        configure();
    }

    VBox selectionSection() {
        VBox box = section("Selección de motor y dispositivo");
        VBox form = new VBox(8);
        form.getStyleClass().add("voice-engine-form");

        Label engineLabel = fieldLabel("Motor de voz", engineModeSelector);
        detachNode(engineModeSelector);
        engineModeSelector.setMaxWidth(Double.MAX_VALUE);

        Label deviceLabel = fieldLabel("Dispositivo de renderizado", computeDeviceSelector);
        detachNode(computeDeviceSelector);
        computeDeviceSelector.setMaxWidth(Double.MAX_VALUE);

        detachNode(engineConfigurationStatus);
        form.getChildren().addAll(engineLabel, engineModeSelector, deviceLabel,
                computeDeviceSelector, engineConfigurationStatus);

        Label note = new Label("Automático prioriza una GPU compatible; CPU evita competir con otros "
                + "trabajos GPU. El diagnóstico indicará el dispositivo realmente utilizado.");
        note.setWrapText(true);
        note.getStyleClass().add("document-side-text");
        box.getChildren().addAll(form, note);
        return box;
    }

    void refresh() {
        OperationalSettings settings = loadOperationalSettingsSafely();
        ComputeEnvironmentReport compute = backend.inspectCompute(settings);
        List<EngineModeChoice> choices = engineChoices();
        refreshingEngineControls = true;
        try {
            engineModeSelector.getItems().setAll(choices);
            engineModeSelector.setValue(selectedEngineChoice(choices, settings));
            computeDeviceSelector.getItems().setAll(computeDeviceChoices(compute));
            computeDeviceSelector.setValue(selectedComputeChoice(settings));
            engineConfigurationStatus.setText(engineStatusText(
                    engineModeSelector.getValue(), settings, compute));
        } finally {
            refreshingEngineControls = false;
        }
    }

    ComboBox<EngineModeChoice> engineModeSelector() {
        return engineModeSelector;
    }

    ComboBox<ComputeDeviceChoice> computeDeviceSelector() {
        return computeDeviceSelector;
    }

    Label operationalStatus() {
        return engineConfigurationStatus;
    }

    private void configure() {
        engineModeSelector.setId(ENGINE_SELECTOR_ID);
        engineModeSelector.getStyleClass().add("voice-library-combo");
        engineModeSelector.setAccessibleText("Motor de voz");
        StudioFormControls.installTooltip(engineModeSelector,
                "Elige uno de los motores de voz registrados en DocuPodcast Studio.");
        engineModeSelector.setCellFactory(list -> engineChoiceCell());
        engineModeSelector.setButtonCell(engineChoiceCell());
        engineModeSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (refreshingEngineControls || newValue == null) return;
            if (!newValue.available()) {
                refreshingEngineControls = true;
                try { engineModeSelector.setValue(oldValue); }
                finally { refreshingEngineControls = false; }
                summarySink.accept(List.of(newValue.label() + " no está disponible.", newValue.detail()));
                return;
            }
            saveEngineModeSelection(newValue);
        });

        computeDeviceSelector.setId(DEVICE_SELECTOR_ID);
        computeDeviceSelector.getStyleClass().add("voice-library-combo");
        computeDeviceSelector.setAccessibleText("Dispositivo de renderizado de voz");
        StudioFormControls.installTooltip(computeDeviceSelector,
                "Elige asignación automática, CPU o una GPU detectada para la voz.");
        computeDeviceSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshingEngineControls && newValue != null) saveComputeDeviceSelection(newValue);
        });

        engineConfigurationStatus.setId(STATUS_ID);
        engineConfigurationStatus.getStyleClass().add("voice-engine-status");
        engineConfigurationStatus.setWrapText(true);
        engineConfigurationStatus.setAccessibleRoleDescription("Estado operativo del motor de voz");
    }

    private static ListCell<EngineModeChoice> engineChoiceCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(EngineModeChoice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setAccessibleText(null);
                    return;
                }
                setText(item.label() + (item.available() ? "" : " · No disponible"));
                setDisable(!item.available());
                setAccessibleText(item.label() + ". " + item.detail());
            }
        };
    }

    private List<EngineModeChoice> engineChoices() {
        return backend.operationalStates().stream()
                .map(VoiceEngineSettingsControls::choice)
                .toList();
    }

    private static EngineModeChoice choice(VoiceEngineOperationalState state) {
        String detail = state.message().isBlank() ? state.statusLabel() : state.message();
        if (!state.ready() && !state.recommendedAction().isBlank()) {
            detail = detail + " " + state.recommendedAction();
        }
        return new EngineModeChoice(state.engineId(), state.displayName(), detail,
                state.registered() && state.ready());
    }

    private static List<ComputeDeviceChoice> computeDeviceChoices(ComputeEnvironmentReport report) {
        ArrayList<ComputeDeviceChoice> choices = new ArrayList<>();
        choices.add(new ComputeDeviceChoice("auto", "Automático", false));
        choices.add(new ComputeDeviceChoice("cpu", "CPU", true));
        report.devices().stream().filter(ComputeDeviceDescriptor::gpu)
                .map(device -> new ComputeDeviceChoice(device.id(), device.displayName(), false))
                .forEach(choices::add);
        return List.copyOf(choices);
    }

    private EngineModeChoice selectedEngineChoice(List<EngineModeChoice> choices,
                                                  OperationalSettings settings) {
        EngineId selected = SelectedMediaEngines.from(settings).voice();
        String id = selected == null ? settings.tts().engineMode() : selected.value();
        return choices.stream().filter(choice -> choice.id().equalsIgnoreCase(id))
                .findFirst().orElseGet(() -> choices.stream()
                        .filter(choice -> TtsEngineModes.LOCAL_SIMPLE.equals(choice.id()))
                        .findFirst().orElse(choices.getFirst()));
    }

    private ComputeDeviceChoice selectedComputeChoice(OperationalSettings settings) {
        String selected = switch (settings.compute().policy()) {
            case CPU_ONLY -> "cpu";
            case AUTO, PREFER_GPU -> "auto";
            case SPECIFIC_DEVICE -> settings.compute().selectedDeviceId();
        };
        return computeDeviceSelector.getItems().stream()
                .filter(choice -> choice.id().equalsIgnoreCase(selected))
                .findFirst().orElse(computeDeviceSelector.getItems().getFirst());
    }

    private static String engineStatusText(EngineModeChoice selected,
                                           OperationalSettings settings,
                                           ComputeEnvironmentReport compute) {
        String engine = selected == null ? TtsEngineModes.safeLabel(settings.tts().engineMode())
                : selected.label();
        String operational = selected == null ? "Estado no disponible"
                : selected.available() ? "Listo para sintetizar" : selected.detail();
        String device = switch (settings.compute().policy()) {
            case CPU_ONLY -> "CPU";
            case AUTO, PREFER_GPU -> "Automático";
            case SPECIFIC_DEVICE -> compute.selectedDevice()
                    .map(ComputeDeviceDescriptor::displayName)
                    .orElse(settings.compute().selectedDeviceId());
        };
        String warnings = compute.warnings().isEmpty() ? ""
                : " · " + String.join(" · ", compute.warnings());
        return engine + " · " + operational + " · Dispositivo: " + device + warnings + ".";
    }

    private void saveEngineModeSelection(EngineModeChoice selection) {
        OperationalSettings current = loadOperationalSettingsSafely();
        OperationalSettings updated = backend.selectEngine(current, selection.id());
        saveOperationalSettings(updated, "Motor activo actualizado: " + selection.label() + ".");
    }

    private void saveComputeDeviceSelection(ComputeDeviceChoice selection) {
        OperationalSettings current = loadOperationalSettingsSafely();
        ComputeDevicePolicy policy = "cpu".equals(selection.id())
                ? ComputeDevicePolicy.CPU_ONLY
                : "auto".equals(selection.id()) ? ComputeDevicePolicy.AUTO
                : ComputeDevicePolicy.SPECIFIC_DEVICE;
        OperationalSettings.ComputeSettings previous = current.compute();
        OperationalSettings.ComputeSettings compute = new OperationalSettings.ComputeSettings(
                policy, "auto".equals(selection.id()) ? "" : selection.id(),
                policy.canUseGpu(), previous.allowGpuForVideo(), previous.videoEncoderPolicy(),
                previous.allowGpuForContentAnalysis(), previous.allowRamOffloadForContentAnalysis());
        OperationalSettings updated = new OperationalSettings(current.readingDocument(),
                current.playbackBuffer(), current.tts(), current.video(), current.imageGeneration(),
                current.imageSuperResolution(), current.mediaEngines(), current.frameGeneration(),
                compute, current.ocr(), current.storage(), current.diagnostics());
        saveOperationalSettings(updated,
                "Dispositivo de renderizado actualizado: " + selection.label() + ".");
    }

    private OperationalSettings loadOperationalSettingsSafely() {
        try {
            return backend.load();
        } catch (IOException | RuntimeException ex) {
            summarySink.accept(List.of("No se pudo leer la configuración de voz: "
                    + Objects.toString(ex.getMessage(), ex.getClass().getSimpleName())));
            return OperationalSettings.defaults();
        }
    }

    private void saveOperationalSettings(OperationalSettings settings, String message) {
        try {
            backend.save(settings);
            backend.resetVoiceTest(
                    "La configuración de motor o dispositivo cambió. Genera una nueva voz de prueba.");
            summarySink.accept(List.of(message,
                    "La misma configuración queda sincronizada con Audio y Configuración."));
            refresh();
        } catch (IOException | RuntimeException ex) {
            summarySink.accept(List.of("No se pudo guardar la configuración de voz: "
                    + Objects.toString(ex.getMessage(), ex.getClass().getSimpleName())));
        }
    }

    private static Label fieldLabel(String text, javafx.scene.Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("document-side-text");
        label.setLabelFor(control);
        return label;
    }

    interface Backend {
        OperationalSettings load() throws IOException;
        void save(OperationalSettings settings) throws IOException;
        ComputeEnvironmentReport inspectCompute(OperationalSettings settings);
        List<VoiceEngineOperationalState> operationalStates();
        OperationalSettings selectEngine(OperationalSettings current, String engineId);
        default void resetVoiceTest(String message) { }
    }

    private static final class ApplicationBackend implements Backend {
        private final DocuPodcastShellViewModel viewModel;

        private ApplicationBackend(DocuPodcastShellViewModel viewModel) {
            this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        }

        @Override
        public OperationalSettings load() throws IOException {
            return viewModel.administrationWorkspace().settings().loadOperationalSettings().load();
        }

        @Override
        public void save(OperationalSettings settings) throws IOException {
            viewModel.administrationWorkspace().settings().saveOperationalSettings().save(settings);
        }

        @Override
        public ComputeEnvironmentReport inspectCompute(OperationalSettings settings) {
            return viewModel.administrationWorkspace().settings().inspectComputeEnvironment().inspect(settings);
        }

        @Override
        public List<VoiceEngineOperationalState> operationalStates() {
            MediaEnginePlatform platform = viewModel.administrationWorkspace().mediaEngines();
            return new ListVoiceEngineOperationalStatesUseCase(platform).list();
        }

        @Override
        public OperationalSettings selectEngine(OperationalSettings current, String engineId) {
            MediaEnginePlatform platform = viewModel.administrationWorkspace().mediaEngines();
            VoiceSynthesisEngine engine = platform.voiceEngines().find(new EngineId(engineId))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El motor " + engineId + " no está registrado en el lanzador activo."));
            return LegacyVoiceEngineSettingsMapper.select(current, engine.descriptor());
        }

        @Override
        public void resetVoiceTest(String message) {
            viewModel.resetGeneratedVoiceTestForSelection(message);
        }
    }

    record EngineModeChoice(String id, String label, String detail, boolean available) {
        EngineModeChoice {
            id = normalize(id, TtsEngineModes.TEST).toLowerCase(Locale.ROOT);
            label = normalize(label, id);
            detail = normalize(detail, available ? "Listo" : "No disponible");
        }

        @Override public String toString() { return label; }
    }

    record ComputeDeviceChoice(String id, String label, boolean cpu) {
        ComputeDeviceChoice {
            id = normalize(id, "auto");
            label = normalize(label, id);
        }

        @Override public String toString() { return label; }
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
