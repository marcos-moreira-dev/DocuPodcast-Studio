package com.marcosmoreiradev.docupodcaststudio.application.settings;

import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;

import java.util.Locale;

/** Persistent local image-generation engine settings. */
public record ImageGenerationSettings(
        String engineMode,
        String baseUrl,
        String devicePolicy,
        String preset,
        String modelName,
        String adaptersDirectory,
        int timeoutSeconds,
        boolean lowVram,
        String memoryProfile,
        int maxAttempts
) {
    public ImageGenerationSettings {
        engineMode = clean(engineMode, "managed-local").toLowerCase(Locale.ROOT);
        baseUrl = clean(baseUrl, "http://127.0.0.1:8188");
        devicePolicy = clean(devicePolicy, "AUTO").toUpperCase(Locale.ROOT);
        preset = clean(preset, "TEST_4GB_SD15").toUpperCase(Locale.ROOT);
        modelName = clean(modelName, "v1-5-pruned-emaonly-fp16.safetensors");
        adaptersDirectory = clean(adaptersDirectory, "models/image/adapters");
        timeoutSeconds = timeoutSeconds < 10 || timeoutSeconds > 3600 ? 300 : timeoutSeconds;
        ImageGenerationMemoryProfile profile = ImageGenerationMemoryProfile.from(memoryProfile, lowVram);
        memoryProfile = profile.name();
        lowVram = profile.legacyLowVram();
        maxAttempts = maxAttempts < 1 || maxAttempts > 10
                ? GenerationAttemptPolicy.DEFAULT_IMAGE_MAX_ATTEMPTS
                : maxAttempts;
    }

    public ImageGenerationSettings(String engineMode,
                                   String baseUrl,
                                   String devicePolicy,
                                   String preset,
                                   String modelName,
                                   String adaptersDirectory,
                                   int timeoutSeconds,
                                   boolean lowVram) {
        this(engineMode, baseUrl, devicePolicy, preset, modelName, adaptersDirectory, timeoutSeconds, lowVram,
                lowVram ? ImageGenerationMemoryProfile.SAFE_LOW_VRAM.name() : ImageGenerationMemoryProfile.NORMAL.name(),
                GenerationAttemptPolicy.DEFAULT_IMAGE_MAX_ATTEMPTS);
    }

    public ImageGenerationSettings(String engineMode,
                                   String baseUrl,
                                   String devicePolicy,
                                   String preset,
                                   String modelName,
                                   String adaptersDirectory,
                                   int timeoutSeconds,
                                   boolean lowVram,
                                   int maxAttempts) {
        this(engineMode, baseUrl, devicePolicy, preset, modelName, adaptersDirectory, timeoutSeconds, lowVram,
                lowVram ? ImageGenerationMemoryProfile.SAFE_LOW_VRAM.name() : ImageGenerationMemoryProfile.NORMAL.name(),
                maxAttempts);
    }

    public static ImageGenerationSettings defaults() {
        return new ImageGenerationSettings(
                "managed-local",
                "http://127.0.0.1:8188",
                "AUTO",
                "TEST_4GB_SD15",
                "v1-5-pruned-emaonly-fp16.safetensors",
                "models/image/adapters",
                300,
                true,
                ImageGenerationMemoryProfile.SAFE_LOW_VRAM.name(),
                GenerationAttemptPolicy.DEFAULT_IMAGE_MAX_ATTEMPTS);
    }

    public ImageGenerationMemoryProfile memoryProfileValue() {
        return ImageGenerationMemoryProfile.from(memoryProfile, lowVram);
    }

    private static String clean(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
