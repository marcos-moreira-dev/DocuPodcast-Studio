package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageSuperResolutionSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/** Form control state plus mapping to persistent operational settings. */
final class SettingsFormModel {
    private static final int DEFAULT_DOCUMENT_FONT_SIZE = 18;
    private static final int DEFAULT_PREBUFFER_SENTENCES = 5;
    private static final int DEFAULT_LOOKAHEAD_SENTENCES = 10;

    final TextField baseFontSize = textField(Integer.toString(DEFAULT_DOCUMENT_FONT_SIZE));
    final TextField lineSpacing = textField("1.35");
    final CheckBox keepActiveSentenceNearCenter = checkBox(true);
    final TextField initialReadySegments = textField(Integer.toString(DEFAULT_PREBUFFER_SENTENCES));
    final TextField lookaheadSegments = textField(Integer.toString(DEFAULT_LOOKAHEAD_SENTENCES));
    final CheckBox pauseWhenBufferMissing = checkBox(true);
    final ComboBox<String> resolutionPreset = combo("2K", "540P", "720P", "1080P", "2K", "4K");
    final TextField silentVisualBlockSeconds = textField("5.0");
    final CheckBox preferManagedVideoRuntime = checkBox(true);
    final ComboBox<String> generationResolution =
            combo("P1080", "P540", "P720", "P1080", "QHD_2K", "UHD_4K");
    final CheckBox upscaleEnabled = checkBox(false);
    final ComboBox<String> upscaleTargetResolution =
            combo("P1080", "P720", "P1080", "QHD_2K", "UHD_4K");
    final TextField upscaleModel = textField("RealESRGAN_x4plus.pth");
    final CheckBox refinementEnabled = checkBox(false);
    final ComboBox<String> refinementPreset =
            combo("Conservadora", "Conservadora", "Equilibrada");
    final ComboBox<String> computePolicy = combo("AUTO", "AUTO", "CPU_ONLY", "PREFER_GPU", "SPECIFIC_DEVICE");
    final ComboBox<String> computeSelectedDeviceId = combo("auto", "auto", "cpu");
    final CheckBox allowGpuForTts = checkBox(true);
    final CheckBox allowGpuForVideo = checkBox(true);
    final CheckBox allowGpuForContentAnalysis = checkBox(true);
    final CheckBox allowRamOffloadForContentAnalysis = checkBox(true);
    final ComboBox<String> videoEncoderPolicy = combo("AUTO", "AUTO", "CPU_X264", "NVIDIA_NVENC", "INTEL_QSV", "AMD_AMF");
    final TextField modelsDirectory = textField("models");
    final TextField exportsDirectory = textField("exports");
    final CheckBox preflightOnStartup = checkBox(true);
    final CheckBox writeLogs = checkBox(true);
    final CheckBox exportManifests = checkBox(true);
    private final java.util.LinkedHashMap<String, String> computeDeviceIdsByChoice =
            new java.util.LinkedHashMap<>();
    private OperationalSettings baseSettings = OperationalSettings.defaults();
    private java.util.List<String> savedValues;
    boolean operationRunning;

    private java.util.List<javafx.scene.control.Control> controls() {
        return java.util.List.of(baseFontSize, lineSpacing, keepActiveSentenceNearCenter,
                initialReadySegments, lookaheadSegments, pauseWhenBufferMissing, resolutionPreset,
                silentVisualBlockSeconds, preferManagedVideoRuntime, generationResolution, upscaleEnabled,
                upscaleTargetResolution, upscaleModel, refinementEnabled, refinementPreset, computePolicy,
                computeSelectedDeviceId, allowGpuForTts, allowGpuForVideo, allowGpuForContentAnalysis,
                allowRamOffloadForContentAnalysis, videoEncoderPolicy, modelsDirectory, exportsDirectory,
                writeLogs, exportManifests);
    }

    private java.util.List<String> values() {
        return controls().stream().map(control -> control instanceof TextField text ? text.getText()
                : control instanceof CheckBox check ? Boolean.toString(check.isSelected())
                : java.util.Objects.toString(((ComboBox<?>) control).getValue(), "")).toList();
    }

    void markSaved() { savedValues = values(); }
    void rebaseUneditedSettings(OperationalSettings latest) {
        baseSettings = latest;
        mediaEngines = latest.mediaEngines();
        frameGeneration = latest.frameGeneration();
    }
    boolean dirty() { return savedValues != null && !savedValues.equals(values()); }
    void observeChanges(Runnable changed) {
        for (var control : controls()) {
            if (control instanceof TextField text) text.textProperty().addListener((o, a, b) -> changed.run());
            else if (control instanceof CheckBox check) check.selectedProperty().addListener((o, a, b) -> changed.run());
            else ((ComboBox<?>) control).valueProperty().addListener((o, a, b) -> changed.run());
        }
    }
    private OperationalSettings.MediaEngineSelectionSettings mediaEngines =
            OperationalSettings.MediaEngineSelectionSettings.defaults();
    private com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings frameGeneration =
            com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings.defaults();

    SettingsFormModel(OperationalSettings settings) {
        computePolicy.setConverter(labels(value -> com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.from(value).label()));
        videoEncoderPolicy.setConverter(labels(value -> VideoEncoderPolicy.from(value).label()));
        computePolicy.setEditable(false);
        computeSelectedDeviceId.setEditable(false);
        load(settings);
        generationResolution.valueProperty().addListener((obs, oldValue, newValue) ->
                refreshUpscaleTargetChoices());
        upscaleEnabled.selectedProperty().addListener((obs, oldValue, newValue) ->
                refreshUpscaleTargetChoices());
        upscaleEnabled.selectedProperty().addListener((obs, oldValue, enabled) -> {
            refinementEnabled.setDisable(!enabled);
            if (!enabled) refinementEnabled.setSelected(false);
        });
        refinementEnabled.selectedProperty().addListener((obs, oldValue, enabled) ->
                refinementPreset.setDisable(!enabled));
        refreshUpscaleTargetChoices();
    }

    private void refreshUpscaleTargetChoices() {
        String previous = comboValue(upscaleTargetResolution, "");
        VisualResolutionProfile source = VisualResolutionProfile.from(
                comboValue(generationResolution, "P1080"), VisualResolutionProfile.P1080);
        java.util.List<String> choices = VisualResolutionProfile.higherThanProfile(source).stream()
                .filter(profile -> profile != VisualResolutionProfile.P540)
                .map(VisualResolutionProfile::name)
                .toList();
        upscaleTargetResolution.getItems().setAll(choices);
        upscaleTargetResolution.setValue(choices.contains(previous)
                ? previous : choices.stream().findFirst().orElse(null));
        boolean available = !choices.isEmpty();
        upscaleTargetResolution.setDisable(!upscaleEnabled.isSelected() || !available);
        if (!available) upscaleEnabled.setSelected(false);
    }

    void load(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        baseSettings = current;
        baseFontSize.setText(Integer.toString(current.readingDocument().baseFontSize()));
        lineSpacing.setText(Double.toString(current.readingDocument().lineSpacing()));
        keepActiveSentenceNearCenter.setSelected(current.readingDocument().keepActiveSentenceNearCenter());
        initialReadySegments.setText(Integer.toString(current.playbackBuffer().initialReadySegments()));
        lookaheadSegments.setText(Integer.toString(current.playbackBuffer().lookaheadSegments()));
        pauseWhenBufferMissing.setSelected(current.playbackBuffer().pauseWhenBufferMissing());
        resolutionPreset.setValue(current.video().resolutionPreset());
        silentVisualBlockSeconds.setText(Double.toString(current.video().silentVisualBlockSeconds()));
        preferManagedVideoRuntime.setSelected(current.video().preferManagedVideoRuntime());
        generationResolution.setValue(current.imageSuperResolution().generationProfile());
        upscaleEnabled.setSelected(current.imageSuperResolution().enabled());
        upscaleTargetResolution.setValue(current.imageSuperResolution().targetProfile());
        upscaleModel.setText(current.imageSuperResolution().modelName());
        upscaleModel.setEditable(false);
        refinementEnabled.setSelected(current.imageSuperResolution().refinementEnabled());
        refinementEnabled.setDisable(!upscaleEnabled.isSelected());
        refinementPreset.setValue("balanced".equalsIgnoreCase(
                current.imageSuperResolution().refinementPreset())
                ? "Equilibrada" : "Conservadora");
        refinementPreset.setDisable(!refinementEnabled.isSelected());
        frameGeneration = current.frameGeneration();
        mediaEngines = current.mediaEngines();
        computePolicy.setValue(current.compute().policy().name());
        computeSelectedDeviceId.setValue(current.compute().selectedDeviceId().isBlank() ? "auto" : current.compute().selectedDeviceId());
        allowGpuForTts.setSelected(current.compute().allowGpuForTts());
        allowGpuForVideo.setSelected(current.compute().allowGpuForVideo());
        allowGpuForContentAnalysis.setSelected(current.compute().allowGpuForContentAnalysis());
        allowRamOffloadForContentAnalysis.setSelected(
                current.compute().allowRamOffloadForContentAnalysis());
        videoEncoderPolicy.setValue(current.compute().videoEncoderPolicy().name());
        modelsDirectory.setText(current.storage().modelsDirectory());
        exportsDirectory.setText(current.storage().exportsDirectory());
        preflightOnStartup.setSelected(current.diagnostics().preflightOnStartup());
        writeLogs.setSelected(current.diagnostics().writeLogs());
        exportManifests.setSelected(current.diagnostics().exportManifests());
    }

    void refreshComputeDeviceChoices(java.util.List<ComputeDeviceDescriptor> devices) {
        String current = selectedComputeDeviceId();
        computeDeviceIdsByChoice.clear();
        computeDeviceIdsByChoice.put("Automático · prioriza GPU compatible", "");
        computeDeviceIdsByChoice.put("CPU local", "cpu");
        if (devices != null) {
            for (ComputeDeviceDescriptor device : devices) {
                if (device.gpu()) {
                    computeDeviceIdsByChoice.put(
                            device.displayName() + " · " + device.id(), device.id());
                }
            }
        }
        computeSelectedDeviceId.getItems().setAll(computeDeviceIdsByChoice.keySet());
        String selected = computeDeviceIdsByChoice.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(current))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> {
                    if (current.isBlank()) return "Automático · prioriza GPU compatible";
                    String missing = current + " · no detectado";
                    computeDeviceIdsByChoice.put(missing, current);
                    computeSelectedDeviceId.getItems().add(missing);
                    return missing;
                });
        computeSelectedDeviceId.setValue(selected);
    }

    void refreshVideoEncoderChoices(java.util.List<ComputeDeviceDescriptor> devices) {
        String current = comboValue(videoEncoderPolicy, "AUTO");
        java.util.LinkedHashSet<String> choices = new java.util.LinkedHashSet<>();
        choices.add(VideoEncoderPolicy.AUTO.name());
        choices.add(VideoEncoderPolicy.CPU_X264.name());
        if (devices != null) {
            for (ComputeDeviceDescriptor device : devices) {
                if (!device.gpu()) {
                    continue;
                }
                if (deviceLooksLike(device, "nvidia") || deviceLooksLike(device, "geforce")
                        || deviceLooksLike(device, "gtx") || deviceLooksLike(device, "rtx")) {
                    choices.add(VideoEncoderPolicy.NVIDIA_NVENC.name());
                }
                if (deviceLooksLike(device, "intel") || deviceLooksLike(device, "uhd")
                        || deviceLooksLike(device, "iris") || deviceLooksLike(device, "arc")) {
                    choices.add(VideoEncoderPolicy.INTEL_QSV.name());
                }
                if (deviceLooksLike(device, "amd") || deviceLooksLike(device, "radeon")) {
                    choices.add(VideoEncoderPolicy.AMD_AMF.name());
                }
            }
        }
        choices.add(current);
        videoEncoderPolicy.getItems().setAll(choices.stream().toList());
        videoEncoderPolicy.setValue(choices.contains(current) ? current : VideoEncoderPolicy.AUTO.name());
    }

    OperationalSettings toSettings() {
        requireRange(baseFontSize, "Tamaño base", 14, 28);
        requireRange(lineSpacing, "Interlineado", 1, 2);
        requireRange(initialReadySegments, "Fragmentos iniciales", 1, 25);
        requireRange(lookaheadSegments, "Fragmentos de anticipación", intValue(initialReadySegments, 5), 50);
        requireRange(silentVisualBlockSeconds, "Duración de visual silencioso", 1, 60);
        return new OperationalSettings(
                new OperationalSettings.ReadingDocumentSettings(
                        intValue(baseFontSize, 18),
                        doubleValue(lineSpacing, 1.35),
                        keepActiveSentenceNearCenter.isSelected()),
                new OperationalSettings.PlaybackBufferSettings(
                        intValue(initialReadySegments, 5),
                        intValue(lookaheadSegments, 10),
                        pauseWhenBufferMissing.isSelected()),
                baseSettings.tts(),
                baseSettings.video().withPresentationPreferences(comboValue(resolutionPreset, "2K"),
                                doubleValue(silentVisualBlockSeconds, 5.0))
                        .withRuntimePreference(preferManagedVideoRuntime.isSelected()),
                baseSettings.imageGeneration(),
                new ImageSuperResolutionSettings(
                        comboValue(generationResolution, "P1080"),
                        upscaleEnabled.isSelected(),
                        comboValue(upscaleTargetResolution, "QHD_2K"),
                        upscaleModel.getText(),
                        upscaleEnabled.isSelected() && refinementEnabled.isSelected(),
                        refinementPresetId(),
                        ImageSuperResolutionSettings.DEFAULT_REFINEMENT_ENGINE),
                mediaEngines,
                frameGeneration,
                new OperationalSettings.ComputeSettings(
                        comboValue(computePolicy, "AUTO"),
                        selectedComputeDeviceId(),
                        allowGpuForTts.isSelected(),
                        allowGpuForVideo.isSelected(),
                        comboValue(videoEncoderPolicy, "AUTO"),
                        allowGpuForContentAnalysis.isSelected(),
                        allowRamOffloadForContentAnalysis.isSelected()),
                baseSettings.ocr(),
                new OperationalSettings.StorageSettings(
                        modelsDirectory.getText(),
                        exportsDirectory.getText()),
                new OperationalSettings.DiagnosticSettings(
                        preflightOnStartup.isSelected(),
                        writeLogs.isSelected(),
                        exportManifests.isSelected())
        );
    }

    String currentComputeDeviceLabel() {
        String value = selectedComputeDeviceId();
        if (value.isBlank()) {
            return "Automático";
        }
        if ("cpu".equalsIgnoreCase(value)) {
            return "CPU";
        }
        return "Dispositivo específico";
    }

    OperationalSettings currentSettings() {
        return toSettings();
    }

    private static javafx.util.StringConverter<String> labels(java.util.function.Function<String, String> label) {
        return new javafx.util.StringConverter<>() {
            public String toString(String value) { return value == null ? "" : label.apply(value); }
            public String fromString(String value) { return value; }
        };
    }

    private String refinementPresetId() {
        return "Equilibrada".equalsIgnoreCase(comboValue(refinementPreset, "Conservadora"))
                ? "balanced" : "conservative";
    }

    private static TextField textField(String value) {
        TextField field = StudioFormControls.textField(java.util.Objects.toString(value, ""));
        field.setPrefColumnCount(34);
        return field;
    }

    private static CheckBox checkBox(boolean selected) {
        CheckBox checkBox = StudioFormControls.checkBox();
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static ComboBox<String> combo(String selected, String... values) {
        ComboBox<String> combo = StudioFormControls.comboBox();
        combo.getItems().setAll(values);
        combo.setEditable(false);
        combo.setValue(selected == null || selected.isBlank() ? values[0] : selected);
        combo.setPrefWidth(240);
        return combo;
    }

    private static int intValue(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().strip());
        } catch (RuntimeException ex) {
            field.requestFocus();
            throw new IllegalArgumentException("Introduce un número entero válido: " + field.getText());
        }
    }

    private static double doubleValue(TextField field, double fallback) {
        try {
            return Double.parseDouble(field.getText().strip());
        } catch (RuntimeException ex) {
            field.requestFocus();
            throw new IllegalArgumentException("Introduce un número válido: " + field.getText());
        }
    }

    private static String comboValue(ComboBox<String> combo, String fallback) {
        String value = combo.getValue();
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static void requireRange(TextField field, String label, double min, double max) {
        double value = doubleValue(field, min);
        if (!Double.isFinite(value) || value < min || value > max) {
            field.requestFocus();
            throw new IllegalArgumentException(label + ": introduce un valor entre " + min + " y " + max + ".");
        }
    }

    private String selectedComputeDeviceId() {
        String choice = comboValue(computeSelectedDeviceId, "");
        String mapped = computeDeviceIdsByChoice.get(choice);
        if (mapped != null) return mapped;
        String normalized = choice.strip();
        return normalized.equalsIgnoreCase("auto")
                || normalized.startsWith("Automático")
                || normalized.startsWith("Automático") ? "" : normalized;
    }

    private static boolean deviceLooksLike(ComputeDeviceDescriptor device, String token) {
        String haystack = (device.id() + " " + device.displayName() + " " + device.vendor())
                .toLowerCase(java.util.Locale.ROOT);
        return haystack.contains(token);
    }
}
