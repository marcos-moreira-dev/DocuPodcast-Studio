package com.marcosmoreiradev.docupodcaststudio.infrastructure.settings;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Stores desktop operational settings in a simple properties file.
 *
 * <p>This is deliberately separate from source documents: Word/PDF/Markdown/TXT remain read-only.
 * The settings file governs the local technical warehouse: buffer, engines, models, FFmpeg,
 * storage and diagnostics.</p>
 */
public final class PropertiesOperationalSettingsRepository implements OperationalSettingsRepository {
    private final Path settingsFile;

    public PropertiesOperationalSettingsRepository(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    public static PropertiesOperationalSettingsRepository defaultRepository() {
        return new PropertiesOperationalSettingsRepository(defaultSettingsPath());
    }

    public static Path defaultSettingsPath() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, ".docupodcast-studio", "operational-settings.properties");
    }

    public Path settingsFile() {
        return settingsFile;
    }

    @Override
    public OperationalSettings load() throws IOException {
        if (settingsFile == null || !Files.isRegularFile(settingsFile)) {
            return OperationalSettings.defaults();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
        }
        return OperationalSettingsMigrationPolicy.repair(fromProperties(properties));
    }

    @Override
    public void save(OperationalSettings settings) throws IOException {
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        if (settingsFile.getParent() != null) {
            Files.createDirectories(settingsFile.getParent());
        }
        Properties properties = toProperties(current);
        try (OutputStream output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "DocuPodcast Studio operational settings");
        }
    }

    static OperationalSettings fromProperties(Properties properties) {
        Properties p = properties == null ? new Properties() : properties;
        return new OperationalSettings(
                new OperationalSettings.ReadingDocumentSettings(
                        intValue(p, "reading.fontSize", 18),
                        doubleValue(p, "reading.lineSpacing", 1.35),
                        boolValue(p, "reading.keepActiveSentenceNearCenter", true)),
                new OperationalSettings.PlaybackBufferSettings(
                        intValue(p, "playback.initialReadySegments", 5),
                        intValue(p, "playback.lookaheadSegments", 10),
                        boolValue(p, "playback.pauseWhenBufferMissing", true)),
                new OperationalSettings.TtsEngineSettings(
                        p.getProperty("capability.voice.engine", p.getProperty("tts.engineMode", "piper")),
                        engineProperty(p, "voice", "commandTemplate", p.getProperty("tts.commandTemplate", "")),
                        p.getProperty("tts.displayName", "Motor TTS local"),
                        p.getProperty("tts.language", "es"),
                        p.getProperty("tts.voiceProfileId", "VOC-NARRATOR"),
                        intValue(p, "tts.timeoutSeconds", 180),
                        intValue(p, "tts.maxRetries", 3),
                        p.getProperty("download.xtts.baseUrl", ""),
                        p.getProperty("download.piper.runtimeZipUrl", ""),
                        p.getProperty("download.piper.defaultVoiceUrl", ""),
                        p.getProperty("download.piper.defaultVoiceMetadataUrl", "")),
                new OperationalSettings.VideoRenderSettings(
                        p.getProperty("engine.ffmpeg.executable", p.getProperty("video.ffmpegExecutable", "")),
                        p.getProperty("video.resolutionPreset", "2K"),
                        boolValue(p, "video.preferEmbeddedFfmpeg", true),
                        doubleValue(p, "video.silentVisualBlockSeconds", 5.0),
                        p.getProperty("download.ffmpeg.runtimeZipUrl", "")),
                new ImageGenerationSettings(
                        legacyImageMode(p.getProperty("capability.image.engine",
                                p.getProperty("image.engineMode", "managed-local"))),
                        engineProperty(p, "image", "baseUrl",
                                p.getProperty("image.baseUrl", "http://127.0.0.1:8188")),
                        p.getProperty("image.devicePolicy", "AUTO"),
                        p.getProperty("image.preset", "PRODUCTION_SDXL_REFERENCE"),
                        p.getProperty("image.modelName", "v1-5-pruned-emaonly-fp16.safetensors"),
                        p.getProperty("image.adaptersDirectory", "models/image/adapters"),
                        intValue(p, "image.timeoutSeconds", ImageGenerationSettings.DEFAULT_TIMEOUT_SECONDS),
                        boolValue(p, "image.lowVram", true),
                        p.getProperty("image.memoryProfile", ""),
                        intValue(p, "image.maxAttempts", 2)),
                new FrameGenerationSettings(
                        p.getProperty("frames.mode", "SINGLE"),
                        p.getProperty("frames.scope", "ALL"),
                        p.getProperty("frames.outputDirectory", ""),
                        p.getProperty("frames.overwritePolicy", "UNIQUE")),
                new OperationalSettings.ComputeSettings(
                        p.getProperty("compute.policy", "AUTO"),
                        p.getProperty("compute.selectedDeviceId", ""),
                        boolValue(p, "compute.allowGpuForTts", true),
                        boolValue(p, "compute.allowGpuForVideo", true),
                        p.getProperty("video.encoderPolicy", "AUTO")),
                new OperationalSettings.OcrSettings(
                        p.getProperty("ocr.engineMode", "managed-local"),
                        p.getProperty("ocr.tesseractExecutable", ""),
                        p.getProperty("ocr.languages", OperationalSettings.OcrSettings.DEFAULT_LANGUAGES),
                        intValue(p, "ocr.dpi", 300),
                        intValue(p, "ocr.timeoutSeconds", 180),
                        boolValue(p, "ocr.cacheEnabled", true),
                        p.getProperty("download.ocr.tesseractRuntimeZipUrl", "")),
                new OperationalSettings.StorageSettings(
                        p.getProperty("storage.modelsDirectory", "models"),
                        p.getProperty("storage.exportsDirectory", "exports")),
                new OperationalSettings.DiagnosticSettings(
                        boolValue(p, "diagnostics.preflightOnStartup", true),
                        boolValue(p, "diagnostics.writeLogs", true),
                        boolValue(p, "diagnostics.exportManifests", true))
        );
    }

    static Properties toProperties(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Properties p = new Properties();
        p.setProperty("reading.fontSize", Integer.toString(current.readingDocument().baseFontSize()));
        p.setProperty("reading.lineSpacing", Double.toString(current.readingDocument().lineSpacing()));
        p.setProperty("reading.keepActiveSentenceNearCenter", Boolean.toString(current.readingDocument().keepActiveSentenceNearCenter()));
        p.setProperty("playback.initialReadySegments", Integer.toString(current.playbackBuffer().initialReadySegments()));
        p.setProperty("playback.lookaheadSegments", Integer.toString(current.playbackBuffer().lookaheadSegments()));
        p.setProperty("playback.pauseWhenBufferMissing", Boolean.toString(current.playbackBuffer().pauseWhenBufferMissing()));
        p.setProperty("tts.engineMode", current.tts().engineMode());
        p.setProperty("capability.voice.engine", current.tts().engineMode());
        p.setProperty("engine." + current.tts().engineMode() + ".commandTemplate", current.tts().commandTemplate());
        p.setProperty("tts.commandTemplate", current.tts().commandTemplate());
        p.setProperty("tts.displayName", current.tts().displayName());
        p.setProperty("tts.language", current.tts().language());
        p.setProperty("tts.voiceProfileId", current.tts().voiceProfileId());
        p.setProperty("tts.timeoutSeconds", Integer.toString(current.tts().timeoutSeconds()));
        p.setProperty("tts.maxRetries", Integer.toString(current.tts().maxRetries()));
        p.setProperty("download.xtts.baseUrl", current.tts().xttsDownloadBaseUrl());
        p.setProperty("download.piper.runtimeZipUrl", current.tts().piperRuntimeZipUrl());
        p.setProperty("download.piper.defaultVoiceUrl", current.tts().piperDefaultVoiceUrl());
        p.setProperty("download.piper.defaultVoiceMetadataUrl", current.tts().piperDefaultVoiceMetadataUrl());
        p.setProperty("video.ffmpegExecutable", current.video().ffmpegExecutable());
        p.setProperty("capability.video.render.engine", "ffmpeg");
        p.setProperty("engine.ffmpeg.executable", current.video().ffmpegExecutable());
        p.setProperty("video.resolutionPreset", current.video().resolutionPreset());
        p.setProperty("video.preferEmbeddedFfmpeg", Boolean.toString(current.video().preferEmbeddedFfmpeg()));
        p.setProperty("video.silentVisualBlockSeconds", Double.toString(current.video().silentVisualBlockSeconds()));
        p.setProperty("download.ffmpeg.runtimeZipUrl", current.video().ffmpegDownloadUrl());
        p.setProperty("image.engineMode", current.imageGeneration().engineMode());
        p.setProperty("capability.image.engine", canonicalImageEngine(current.imageGeneration().engineMode()));
        p.setProperty("engine." + canonicalImageEngine(current.imageGeneration().engineMode()) + ".baseUrl",
                current.imageGeneration().baseUrl());
        p.setProperty("image.baseUrl", current.imageGeneration().baseUrl());
        p.setProperty("image.devicePolicy", current.imageGeneration().devicePolicy());
        p.setProperty("image.preset", current.imageGeneration().preset());
        p.setProperty("image.modelName", current.imageGeneration().modelName());
        p.setProperty("image.adaptersDirectory", current.imageGeneration().adaptersDirectory());
        p.setProperty("image.timeoutSeconds", Integer.toString(current.imageGeneration().timeoutSeconds()));
        p.setProperty("image.lowVram", Boolean.toString(current.imageGeneration().lowVram()));
        p.setProperty("image.memoryProfile", current.imageGeneration().memoryProfile());
        p.setProperty("image.maxAttempts", Integer.toString(current.imageGeneration().maxAttempts()));
        p.setProperty("frames.mode", current.frameGeneration().mode());
        p.setProperty("frames.scope", current.frameGeneration().scope());
        p.setProperty("frames.outputDirectory", current.frameGeneration().outputDirectory());
        p.setProperty("frames.overwritePolicy", current.frameGeneration().overwritePolicy());
        p.setProperty("compute.policy", current.compute().policy().name());
        p.setProperty("compute.selectedDeviceId", current.compute().selectedDeviceId());
        p.setProperty("compute.allowGpuForTts", Boolean.toString(current.compute().allowGpuForTts()));
        p.setProperty("compute.allowGpuForVideo", Boolean.toString(current.compute().allowGpuForVideo()));
        p.setProperty("video.encoderPolicy", current.compute().videoEncoderPolicy().name());
        p.setProperty("ocr.engineMode", current.ocr().engineMode());
        p.setProperty("ocr.tesseractExecutable", current.ocr().tesseractExecutable());
        p.setProperty("ocr.languages", current.ocr().languages());
        p.setProperty("ocr.dpi", Integer.toString(current.ocr().dpi()));
        p.setProperty("ocr.timeoutSeconds", Integer.toString(current.ocr().timeoutSeconds()));
        p.setProperty("ocr.cacheEnabled", Boolean.toString(current.ocr().cacheEnabled()));
        p.setProperty("download.ocr.tesseractRuntimeZipUrl", current.ocr().tesseractRuntimeZipUrl());
        p.setProperty("storage.modelsDirectory", current.storage().modelsDirectory());
        p.setProperty("storage.exportsDirectory", current.storage().exportsDirectory());
        p.setProperty("diagnostics.preflightOnStartup", Boolean.toString(current.diagnostics().preflightOnStartup()));
        p.setProperty("diagnostics.writeLogs", Boolean.toString(current.diagnostics().writeLogs()));
        p.setProperty("diagnostics.exportManifests", Boolean.toString(current.diagnostics().exportManifests()));
        return p;
    }

    private static int intValue(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).strip());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static String engineProperty(Properties properties, String capability, String field, String fallback) {
        String engineId = properties.getProperty("capability." + capability + ".engine", "").strip();
        if (engineId.isBlank()) return fallback;
        return properties.getProperty("engine." + engineId + "." + field, fallback);
    }

    private static String canonicalImageEngine(String legacyMode) {
        return "managed-local".equalsIgnoreCase(legacyMode) ? "comfyui" : legacyMode;
    }

    private static String legacyImageMode(String engineId) {
        return "comfyui".equalsIgnoreCase(engineId) ? "managed-local" : engineId;
    }

    private static double doubleValue(Properties p, String key, double fallback) {
        try {
            return Double.parseDouble(p.getProperty(key, Double.toString(fallback)).strip());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean boolValue(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.strip());
    }
}
