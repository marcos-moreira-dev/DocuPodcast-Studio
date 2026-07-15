package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadXttsOfficialModelUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

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
    final ComboBox<String> ttsEngineMode = engineModeCombo();
    final TextField ttsCommandTemplate = textField("");
    final TextField ttsDisplayName = textField("Voz local");
    final TextField ttsLanguage = textField("es");
    final TextField ttsVoiceProfileId = textField("VOC-NARRATOR");
    final TextField ttsTimeoutSeconds = textField("180");
    final TextField ttsMaxRetries = textField("3");
    final TextField xttsDownloadBaseUrl = textField(OperationalSettings.TtsEngineSettings.DEFAULT_XTTS_DOWNLOAD_BASE_URL);
    final TextField piperRuntimeZipUrl = textField(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_RUNTIME_ZIP_URL);
    final TextField piperDefaultVoiceUrl = textField(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_DEFAULT_VOICE_URL);
    final TextField piperDefaultVoiceMetadataUrl = textField(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_DEFAULT_VOICE_METADATA_URL);
    final TextField ffmpegExecutable = textField("");
    final TextField ffmpegDownloadUrl = textField(OperationalSettings.VideoRenderSettings.DEFAULT_FFMPEG_DOWNLOAD_URL);
    final TextField ocrTesseractExecutable = textField("");
    final TextField ocrLanguages = textField(OperationalSettings.OcrSettings.DEFAULT_LANGUAGES);
    final TextField ocrDpi = textField("300");
    final TextField ocrTimeoutSeconds = textField("180");
    final CheckBox ocrCacheEnabled = checkBox(true);
    final TextField ocrTesseractRuntimeZipUrl = textField("");
    final ComboBox<String> resolutionPreset = combo("2K", "720P", "1080P", "2K", "4K");
    final CheckBox preferEmbeddedFfmpeg = checkBox(true);
    final TextField silentVisualBlockSeconds = textField("5.0");
    final TextField imageBaseUrl = textField("http://127.0.0.1:8188");
    final ComboBox<String> imagePreset = combo("TEST_4GB_SD15", "TEST_4GB_SD15", "SD15_DREAMSHAPER", "HIGH_QUALITY_FLUX", "CUSTOM_COMFY_WORKFLOW");
    final TextField imageModelName = textField("v1-5-pruned-emaonly-fp16.safetensors");
    final TextField imageAdaptersDirectory = textField("models/image/adapters");
    final TextField imageTimeoutSeconds = textField("300");
    final TextField imageMaxAttempts = textField("2");
    final CheckBox imageLowVram = checkBox(true);
    final ComboBox<String> imageMemoryProfile = combo("SAFE_LOW_VRAM", "SAFE_LOW_VRAM", "NORMAL", "VRAM_RAM_OFFLOAD", "HIGH_MEMORY");
    final ComboBox<String> computePolicy = combo("AUTO", "AUTO", "CPU_ONLY", "PREFER_GPU", "SPECIFIC_DEVICE");
    final ComboBox<String> computeSelectedDeviceId = combo("auto", "auto", "cpu");
    final CheckBox allowGpuForTts = checkBox(true);
    final CheckBox allowGpuForVideo = checkBox(true);
    final ComboBox<String> videoEncoderPolicy = combo("AUTO", "AUTO", "CPU_X264", "NVIDIA_NVENC", "INTEL_QSV", "AMD_AMF");
    final TextField modelsDirectory = textField("models");
    final TextField exportsDirectory = textField("exports");
    final CheckBox preflightOnStartup = checkBox(true);
    final CheckBox writeLogs = checkBox(true);
    final CheckBox exportManifests = checkBox(true);
    private OperationalSettings.OcrSettings ocrSettings = OperationalSettings.OcrSettings.defaults();
    private com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings frameGeneration =
            com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings.defaults();

    SettingsFormModel(OperationalSettings settings) {
        load(settings);
    }

    void load(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        baseFontSize.setText(Integer.toString(current.readingDocument().baseFontSize()));
        lineSpacing.setText(Double.toString(current.readingDocument().lineSpacing()));
        keepActiveSentenceNearCenter.setSelected(current.readingDocument().keepActiveSentenceNearCenter());
        initialReadySegments.setText(Integer.toString(current.playbackBuffer().initialReadySegments()));
        lookaheadSegments.setText(Integer.toString(current.playbackBuffer().lookaheadSegments()));
        pauseWhenBufferMissing.setSelected(current.playbackBuffer().pauseWhenBufferMissing());
        ttsEngineMode.setValue(current.tts().engineMode());
        ttsCommandTemplate.setText(current.tts().commandTemplate());
        ttsDisplayName.setText(current.tts().displayName());
        ttsLanguage.setText(current.tts().language());
        ttsVoiceProfileId.setText(current.tts().voiceProfileId());
        ttsTimeoutSeconds.setText(Integer.toString(current.tts().timeoutSeconds()));
        ttsMaxRetries.setText(Integer.toString(current.tts().maxRetries()));
        xttsDownloadBaseUrl.setText(DownloadXttsOfficialModelUseCase.normalizeRepositoryUrlForDisplay(current.tts().xttsDownloadBaseUrl()));
        xttsDownloadBaseUrl.setPromptText(OperationalSettings.TtsEngineSettings.DEFAULT_XTTS_DOWNLOAD_BASE_URL);
        piperRuntimeZipUrl.setText(current.tts().piperRuntimeZipUrl());
        piperRuntimeZipUrl.setPromptText(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_RUNTIME_ZIP_URL);
        piperDefaultVoiceUrl.setText(current.tts().piperDefaultVoiceUrl());
        piperDefaultVoiceUrl.setPromptText(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_DEFAULT_VOICE_URL);
        piperDefaultVoiceMetadataUrl.setText(current.tts().piperDefaultVoiceMetadataUrl());
        piperDefaultVoiceMetadataUrl.setPromptText(OperationalSettings.TtsEngineSettings.DEFAULT_PIPER_DEFAULT_VOICE_METADATA_URL);
        ffmpegExecutable.setText(current.video().ffmpegExecutable());
        ffmpegDownloadUrl.setText(current.video().ffmpegDownloadUrl());
        ffmpegDownloadUrl.setPromptText(OperationalSettings.VideoRenderSettings.DEFAULT_FFMPEG_DOWNLOAD_URL);
        ocrTesseractExecutable.setText(current.ocr().tesseractExecutable());
        ocrLanguages.setText(current.ocr().languages());
        ocrDpi.setText(Integer.toString(current.ocr().dpi()));
        ocrTimeoutSeconds.setText(Integer.toString(current.ocr().timeoutSeconds()));
        ocrCacheEnabled.setSelected(current.ocr().cacheEnabled());
        ocrTesseractRuntimeZipUrl.setText(current.ocr().tesseractRuntimeZipUrl());
        resolutionPreset.setValue(current.video().resolutionPreset());
        preferEmbeddedFfmpeg.setSelected(current.video().preferEmbeddedFfmpeg());
        silentVisualBlockSeconds.setText(Double.toString(current.video().silentVisualBlockSeconds()));
        imageBaseUrl.setText(current.imageGeneration().baseUrl());
        imagePreset.setValue(current.imageGeneration().preset());
        imageModelName.setText(current.imageGeneration().modelName());
        imageAdaptersDirectory.setText(current.imageGeneration().adaptersDirectory());
        imageTimeoutSeconds.setText(Integer.toString(current.imageGeneration().timeoutSeconds()));
        imageMaxAttempts.setText(Integer.toString(current.imageGeneration().maxAttempts()));
        imageLowVram.setSelected(current.imageGeneration().lowVram());
        imageMemoryProfile.setValue(current.imageGeneration().memoryProfile());
        frameGeneration = current.frameGeneration();
        ocrSettings = current.ocr();
        computePolicy.setValue(current.compute().policy().name());
        computeSelectedDeviceId.setValue(current.compute().selectedDeviceId().isBlank() ? "auto" : current.compute().selectedDeviceId());
        allowGpuForTts.setSelected(current.compute().allowGpuForTts());
        allowGpuForVideo.setSelected(current.compute().allowGpuForVideo());
        videoEncoderPolicy.setValue(current.compute().videoEncoderPolicy().name());
        modelsDirectory.setText(current.storage().modelsDirectory());
        exportsDirectory.setText(current.storage().exportsDirectory());
        preflightOnStartup.setSelected(current.diagnostics().preflightOnStartup());
        writeLogs.setSelected(current.diagnostics().writeLogs());
        exportManifests.setSelected(current.diagnostics().exportManifests());
    }

    void refreshComputeDeviceChoices(java.util.List<ComputeDeviceDescriptor> devices) {
        String current = comboValue(computeSelectedDeviceId, "auto");
        java.util.ArrayList<String> choices = new java.util.ArrayList<>();
        choices.add("auto");
        choices.add("cpu");
        if (devices != null) {
            for (ComputeDeviceDescriptor device : devices) {
                if (device.gpu()) {
                    choices.add(device.id());
                }
            }
        }
        computeSelectedDeviceId.getItems().setAll(choices.stream().distinct().toList());
        computeSelectedDeviceId.setValue(current == null || current.isBlank() ? "auto" : current);
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
        videoEncoderPolicy.getItems().setAll(choices.stream().toList());
        videoEncoderPolicy.setValue(choices.contains(current) ? current : VideoEncoderPolicy.AUTO.name());
    }

    OperationalSettings toSettings() {
        return new OperationalSettings(
                new OperationalSettings.ReadingDocumentSettings(
                        intValue(baseFontSize, 18),
                        doubleValue(lineSpacing, 1.35),
                        keepActiveSentenceNearCenter.isSelected()),
                new OperationalSettings.PlaybackBufferSettings(
                        intValue(initialReadySegments, 5),
                        intValue(lookaheadSegments, 10),
                        pauseWhenBufferMissing.isSelected()),
                new OperationalSettings.TtsEngineSettings(
                        comboValue(ttsEngineMode, "mock"),
                        ttsCommandTemplate.getText(),
                        ttsDisplayName.getText(),
                        ttsLanguage.getText(),
                        ttsVoiceProfileId.getText(),
                        intValue(ttsTimeoutSeconds, 180),
                        intValue(ttsMaxRetries, 3),
                        DownloadXttsOfficialModelUseCase.normalizeRepositoryUrlForDisplay(xttsDownloadBaseUrl.getText()),
                        piperRuntimeZipUrl.getText(),
                        piperDefaultVoiceUrl.getText(),
                        piperDefaultVoiceMetadataUrl.getText()),
                new OperationalSettings.VideoRenderSettings(
                        ffmpegExecutable.getText(),
                        comboValue(resolutionPreset, "2K"),
                        preferEmbeddedFfmpeg.isSelected(),
                        doubleValue(silentVisualBlockSeconds, 5.0),
                        ffmpegDownloadUrl.getText()),
                new com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings(
                        "managed-local",
                        imageBaseUrl.getText(),
                        comboValue(computePolicy, "AUTO"),
                        comboValue(imagePreset, "TEST_4GB_SD15"),
                        imageModelName.getText(),
                        imageAdaptersDirectory.getText(),
                        intValue(imageTimeoutSeconds, 300),
                        imageLowVram.isSelected(),
                        comboValue(imageMemoryProfile, "SAFE_LOW_VRAM"),
                        intValue(imageMaxAttempts, 2)),
                frameGeneration,
                new OperationalSettings.ComputeSettings(
                        comboValue(computePolicy, "AUTO"),
                        normalizeSelectedDevice(comboValue(computeSelectedDeviceId, "auto")),
                        allowGpuForTts.isSelected(),
                        allowGpuForVideo.isSelected(),
                        comboValue(videoEncoderPolicy, "AUTO")),
                new OperationalSettings.OcrSettings(
                        ocrSettings.engineMode(),
                        ocrTesseractExecutable.getText(),
                        ocrLanguages.getText(),
                        intValue(ocrDpi, 300),
                        intValue(ocrTimeoutSeconds, 180),
                        ocrCacheEnabled.isSelected(),
                        ocrTesseractRuntimeZipUrl.getText()),
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
        String value = comboValue(computeSelectedDeviceId, "auto");
        if (value == null || value.isBlank() || "auto".equalsIgnoreCase(value)) {
            return "Automático";
        }
        if ("cpu".equalsIgnoreCase(value)) {
            return "CPU";
        }
        return "Dispositivo específico";
    }

    void applyOcrSettings(OperationalSettings.OcrSettings settings) {
        OperationalSettings.OcrSettings current = settings == null
                ? OperationalSettings.OcrSettings.defaults()
                : settings;
        ocrSettings = current;
        ocrTesseractExecutable.setText(current.tesseractExecutable());
        ocrLanguages.setText(current.languages());
        ocrDpi.setText(Integer.toString(current.dpi()));
        ocrTimeoutSeconds.setText(Integer.toString(current.timeoutSeconds()));
        ocrCacheEnabled.setSelected(current.cacheEnabled());
        ocrTesseractRuntimeZipUrl.setText(current.tesseractRuntimeZipUrl());
    }

    private static TextField textField(String value) {
        TextField field = new TextField(java.util.Objects.toString(value, ""));
        field.setPrefColumnCount(34);
        return field;
    }

    private static CheckBox checkBox(boolean selected) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static ComboBox<String> combo(String selected, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(values);
        combo.setEditable(true);
        combo.setValue(selected == null || selected.isBlank() ? values[0] : selected);
        combo.setPrefWidth(240);
        return combo;
    }

    private static ComboBox<String> engineModeCombo() {
        ComboBox<String> combo = combo(TtsEngineModes.TEST, TtsEngineModes.TEST, TtsEngineModes.EXTERNAL,
                TtsEngineModes.LOCAL_SIMPLE, TtsEngineModes.ADVANCED_AI);
        combo.setEditable(false);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(String value) { return TtsEngineModes.safeLabel(value); }
            @Override public String fromString(String value) { return value; }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : TtsEngineModes.safeLabel(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : TtsEngineModes.safeLabel(item));
            }
        });
        return combo;
    }

    private static int intValue(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().strip());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static double doubleValue(TextField field, double fallback) {
        try {
            return Double.parseDouble(field.getText().strip());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static String comboValue(ComboBox<String> combo, String fallback) {
        String value = combo.getValue();
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String normalizeSelectedDevice(String value) {
        String normalized = value == null ? "" : value.strip();
        return "auto".equalsIgnoreCase(normalized) ? "" : normalized;
    }

    private static boolean deviceLooksLike(ComputeDeviceDescriptor device, String token) {
        String haystack = (device.id() + " " + device.displayName() + " " + device.vendor())
                .toLowerCase(java.util.Locale.ROOT);
        return haystack.contains(token);
    }
}
