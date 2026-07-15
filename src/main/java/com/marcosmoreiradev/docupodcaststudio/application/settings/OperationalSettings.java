package com.marcosmoreiradev.docupodcaststudio.application.settings;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;

import java.util.Locale;
import java.util.Objects;

/**
 * Persistent operational settings for the desktop brain.
 *
 * <p>The main reader should remain simple; these values belong to the technical warehouse:
 * reading comfort, playback buffer, TTS, FFmpeg, storage and diagnostics. They are not
 * source-document edits and must be saved separately from Word/PDF/Markdown/TXT files.</p>
 */
public record OperationalSettings(
        ReadingDocumentSettings readingDocument,
        PlaybackBufferSettings playbackBuffer,
        TtsEngineSettings tts,
        VideoRenderSettings video,
        ImageGenerationSettings imageGeneration,
        FrameGenerationSettings frameGeneration,
        ComputeSettings compute,
        OcrSettings ocr,
        StorageSettings storage,
        DiagnosticSettings diagnostics
) {
    public OperationalSettings {
        readingDocument = readingDocument == null ? ReadingDocumentSettings.defaults() : readingDocument;
        playbackBuffer = playbackBuffer == null ? PlaybackBufferSettings.defaults() : playbackBuffer;
        tts = tts == null ? TtsEngineSettings.defaults() : tts;
        video = video == null ? VideoRenderSettings.defaults() : video;
        imageGeneration = imageGeneration == null ? ImageGenerationSettings.defaults() : imageGeneration;
        frameGeneration = frameGeneration == null ? FrameGenerationSettings.defaults() : frameGeneration;
        compute = compute == null ? ComputeSettings.defaults() : compute;
        ocr = ocr == null ? OcrSettings.defaults() : ocr;
        storage = storage == null ? StorageSettings.defaults() : storage;
        diagnostics = diagnostics == null ? DiagnosticSettings.defaults() : diagnostics;
    }

    public static OperationalSettings defaults() {
        return new OperationalSettings(
                ReadingDocumentSettings.defaults(),
                PlaybackBufferSettings.defaults(),
                TtsEngineSettings.defaults(),
                VideoRenderSettings.defaults(),
                ImageGenerationSettings.defaults(),
                FrameGenerationSettings.defaults(),
                ComputeSettings.defaults(),
                OcrSettings.defaults(),
                StorageSettings.defaults(),
                DiagnosticSettings.defaults());
    }

    /** Compatibility constructor for callers created before OCR runtime became configurable. */
    public OperationalSettings(
            ReadingDocumentSettings readingDocument,
            PlaybackBufferSettings playbackBuffer,
            TtsEngineSettings tts,
            VideoRenderSettings video,
            ImageGenerationSettings imageGeneration,
            FrameGenerationSettings frameGeneration,
            ComputeSettings compute,
            StorageSettings storage,
            DiagnosticSettings diagnostics
    ) {
        this(readingDocument, playbackBuffer, tts, video, imageGeneration, frameGeneration,
                compute, OcrSettings.defaults(), storage, diagnostics);
    }

    /** Compatibility constructor for callers created before image generation became configurable. */
    public OperationalSettings(
            ReadingDocumentSettings readingDocument,
            PlaybackBufferSettings playbackBuffer,
            TtsEngineSettings tts,
            VideoRenderSettings video,
            ComputeSettings compute,
            StorageSettings storage,
            DiagnosticSettings diagnostics
    ) {
        this(readingDocument, playbackBuffer, tts, video,
                ImageGenerationSettings.defaults(), FrameGenerationSettings.defaults(),
                compute, OcrSettings.defaults(), storage, diagnostics);
    }

    /** Compatibility constructor for callers created before compute policy became a first-class setting. */
    public OperationalSettings(
            ReadingDocumentSettings readingDocument,
            PlaybackBufferSettings playbackBuffer,
            TtsEngineSettings tts,
            VideoRenderSettings video,
            StorageSettings storage,
            DiagnosticSettings diagnostics
    ) {
        this(readingDocument, playbackBuffer, tts, video,
                ImageGenerationSettings.defaults(), FrameGenerationSettings.defaults(),
                ComputeSettings.defaults(), OcrSettings.defaults(), storage, diagnostics);
    }

    public record ReadingDocumentSettings(
            int baseFontSize,
            double lineSpacing,
            boolean keepActiveSentenceNearCenter
    ) {
        public ReadingDocumentSettings {
            baseFontSize = clamp(baseFontSize, 14, 28, 18);
            lineSpacing = lineSpacing <= 0 ? 1.35 : Math.max(1.0, Math.min(2.0, lineSpacing));
        }

        public static ReadingDocumentSettings defaults() {
            return new ReadingDocumentSettings(18, 1.35, true);
        }
    }

    public record PlaybackBufferSettings(
            int initialReadySegments,
            int lookaheadSegments,
            boolean pauseWhenBufferMissing
    ) {
        public PlaybackBufferSettings {
            initialReadySegments = clamp(initialReadySegments, 1, 25, 5);
            lookaheadSegments = clamp(lookaheadSegments, initialReadySegments, 50, Math.max(10, initialReadySegments));
        }

        public static PlaybackBufferSettings defaults() {
            return new PlaybackBufferSettings(5, 10, true);
        }
    }

    public record TtsEngineSettings(
            String engineMode,
            String commandTemplate,
            String displayName,
            String language,
            String voiceProfileId,
            int timeoutSeconds,
            int maxRetries,
            String xttsDownloadBaseUrl,
            String piperRuntimeZipUrl,
            String piperDefaultVoiceUrl,
            String piperDefaultVoiceMetadataUrl
    ) {
        public static final String DEFAULT_XTTS_DOWNLOAD_BASE_URL = "https://huggingface.co/coqui/XTTS-v2";
        public static final String DEFAULT_PIPER_RUNTIME_ZIP_URL = "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip";
        public static final String DEFAULT_PIPER_DEFAULT_VOICE_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/sharvard/medium/es_ES-sharvard-medium.onnx?download=1";
        public static final String DEFAULT_PIPER_DEFAULT_VOICE_METADATA_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/sharvard/medium/es_ES-sharvard-medium.onnx.json?download=1";

        public TtsEngineSettings {
            engineMode = normalize(engineMode).isBlank() ? "mock" : normalize(engineMode).toLowerCase(Locale.ROOT);
            commandTemplate = normalize(commandTemplate);
            displayName = normalize(displayName).isBlank() ? "Motor TTS local" : normalize(displayName);
            language = normalize(language).isBlank() ? "es" : normalize(language).toLowerCase(Locale.ROOT);
            voiceProfileId = normalize(voiceProfileId).isBlank() ? "VOC-NARRATOR" : normalize(voiceProfileId);
            timeoutSeconds = clamp(timeoutSeconds, 15, 3600, 180);
            maxRetries = clamp(maxRetries, 0, 10, 3);
            xttsDownloadBaseUrl = configuredUrl(xttsDownloadBaseUrl, "docupodcast.xttsDownloadBaseUrl", "DOCUPODCAST_XTTS_DOWNLOAD_BASE_URL", DEFAULT_XTTS_DOWNLOAD_BASE_URL);
            piperRuntimeZipUrl = configuredUrl(piperRuntimeZipUrl, "docupodcast.piperRuntimeZipUrl", "DOCUPODCAST_PIPER_RUNTIME_ZIP_URL", DEFAULT_PIPER_RUNTIME_ZIP_URL);
            piperDefaultVoiceUrl = configuredUrl(piperDefaultVoiceUrl, "docupodcast.piperDefaultVoiceUrl", "DOCUPODCAST_PIPER_DEFAULT_VOICE_URL", DEFAULT_PIPER_DEFAULT_VOICE_URL);
            piperDefaultVoiceMetadataUrl = configuredUrl(piperDefaultVoiceMetadataUrl, "docupodcast.piperDefaultVoiceMetadataUrl", "DOCUPODCAST_PIPER_DEFAULT_VOICE_METADATA_URL", DEFAULT_PIPER_DEFAULT_VOICE_METADATA_URL);
        }

        public TtsEngineSettings(String engineMode, String commandTemplate, String displayName,
                                 String language, String voiceProfileId, int timeoutSeconds, int maxRetries) {
            this(engineMode, commandTemplate, displayName, language, voiceProfileId, timeoutSeconds, maxRetries,
                    "", "", "", "");
        }

        public static TtsEngineSettings defaults() {
            return new TtsEngineSettings("mock", "", "Motor TTS local", "es", "VOC-NARRATOR", 180, 3,
                    "", "", "", "");
        }

        public boolean externalProcessRequested() {
            return !commandTemplate.isBlank() || "external".equals(engineMode) || "piper".equals(engineMode) || "xtts".equals(engineMode);
        }
    }

    public record VideoRenderSettings(
            String ffmpegExecutable,
            String resolutionPreset,
            boolean preferEmbeddedFfmpeg,
            double silentVisualBlockSeconds,
            String ffmpegDownloadUrl
    ) {
        public static final String DEFAULT_FFMPEG_DOWNLOAD_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

        public VideoRenderSettings {
            ffmpegExecutable = normalize(ffmpegExecutable);
            resolutionPreset = normalize(resolutionPreset).isBlank() ? "2K" : normalize(resolutionPreset).toUpperCase(Locale.ROOT);
            silentVisualBlockSeconds = silentVisualBlockSeconds <= 0 ? 5.0 : Math.max(1.0, Math.min(60.0, silentVisualBlockSeconds));
            ffmpegDownloadUrl = configuredUrl(ffmpegDownloadUrl, "docupodcast.ffmpegDownloadUrl", "DOCUPODCAST_FFMPEG_DOWNLOAD_URL", DEFAULT_FFMPEG_DOWNLOAD_URL);
        }

        public VideoRenderSettings(String ffmpegExecutable, String resolutionPreset, boolean preferEmbeddedFfmpeg,
                                   double silentVisualBlockSeconds) {
            this(ffmpegExecutable, resolutionPreset, preferEmbeddedFfmpeg, silentVisualBlockSeconds, "");
        }

        public VideoRenderSettings(String ffmpegExecutable, String resolutionPreset, boolean preferEmbeddedFfmpeg) {
            this(ffmpegExecutable, resolutionPreset, preferEmbeddedFfmpeg, 5.0, "");
        }

        public static VideoRenderSettings defaults() {
            return new VideoRenderSettings("", "2K", true, 5.0, "");
        }
    }

    public record ComputeSettings(
            ComputeDevicePolicy policy,
            String selectedDeviceId,
            boolean allowGpuForTts,
            boolean allowGpuForVideo,
            VideoEncoderPolicy videoEncoderPolicy
    ) {
        public ComputeSettings {
            policy = policy == null ? ComputeDevicePolicy.AUTO : policy;
            selectedDeviceId = normalize(selectedDeviceId);
            videoEncoderPolicy = videoEncoderPolicy == null ? VideoEncoderPolicy.AUTO : videoEncoderPolicy;
        }

        public ComputeSettings(String policy, String selectedDeviceId, boolean allowGpuForTts,
                               boolean allowGpuForVideo, String videoEncoderPolicy) {
            this(ComputeDevicePolicy.from(policy), selectedDeviceId, allowGpuForTts,
                    allowGpuForVideo, VideoEncoderPolicy.from(videoEncoderPolicy));
        }

        public static ComputeSettings defaults() {
            return new ComputeSettings(ComputeDevicePolicy.AUTO, "", true, true, VideoEncoderPolicy.AUTO);
        }
    }

    public record OcrSettings(
            String engineMode,
            String tesseractExecutable,
            String languages,
            int dpi,
            int timeoutSeconds,
            boolean cacheEnabled,
            String tesseractRuntimeZipUrl
    ) {
        public static final String DEFAULT_LANGUAGES = "spa+eng";

        public OcrSettings {
            engineMode = normalize(engineMode).isBlank() ? "managed-local" : normalize(engineMode).toLowerCase(Locale.ROOT);
            tesseractExecutable = normalize(tesseractExecutable);
            languages = normalize(languages).isBlank() ? DEFAULT_LANGUAGES : normalize(languages);
            dpi = clamp(dpi, 72, 600, 300);
            timeoutSeconds = clamp(timeoutSeconds, 15, 1800, 180);
            tesseractRuntimeZipUrl = configuredUrl(tesseractRuntimeZipUrl,
                    "docupodcast.tesseractRuntimeZipUrl",
                    "DOCUPODCAST_TESSERACT_RUNTIME_ZIP_URL",
                    "");
        }

        public static OcrSettings defaults() {
            return new OcrSettings("managed-local", "", DEFAULT_LANGUAGES, 300, 180, true, "");
        }
    }

    public record StorageSettings(
            String modelsDirectory,
            String exportsDirectory
    ) {
        public StorageSettings {
            modelsDirectory = normalize(modelsDirectory).isBlank() ? "models" : normalize(modelsDirectory);
            exportsDirectory = normalize(exportsDirectory).isBlank() ? "exports" : normalize(exportsDirectory);
        }

        public static StorageSettings defaults() {
            return new StorageSettings("models", "exports");
        }
    }

    public record DiagnosticSettings(
            boolean preflightOnStartup,
            boolean writeLogs,
            boolean exportManifests
    ) {
        public static DiagnosticSettings defaults() {
            return new DiagnosticSettings(true, true, true);
        }
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }


    private static String configuredUrl(String explicitValue, String systemProperty, String environmentVariable, String fallback) {
        String value = normalize(explicitValue);
        if (value.isBlank()) {
            value = normalize(System.getProperty(systemProperty));
        }
        if (value.isBlank()) {
            value = normalize(System.getenv(environmentVariable));
        }
        return value.isBlank() ? fallback : value;
    }

    static String normalize(String value) {
        return Objects.toString(value, "").strip();
    }
}
