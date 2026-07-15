package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

/** Shared retry policy for local audio and image generation. */
public record GenerationAttemptPolicy(int audioMaxAttempts, int imageMaxAttempts) {
    public static final int DEFAULT_AUDIO_MAX_ATTEMPTS = 4;
    public static final int DEFAULT_IMAGE_MAX_ATTEMPTS = 2;

    public GenerationAttemptPolicy {
        audioMaxAttempts = clamp(audioMaxAttempts, 1, 11, DEFAULT_AUDIO_MAX_ATTEMPTS);
        imageMaxAttempts = clamp(imageMaxAttempts, 1, 10, DEFAULT_IMAGE_MAX_ATTEMPTS);
    }

    public static GenerationAttemptPolicy defaults() {
        return new GenerationAttemptPolicy(DEFAULT_AUDIO_MAX_ATTEMPTS, DEFAULT_IMAGE_MAX_ATTEMPTS);
    }

    public static GenerationAttemptPolicy fromSettings(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return new GenerationAttemptPolicy(
                audioMaxAttemptsFromRetries(current.tts().maxRetries()),
                current.imageGeneration().maxAttempts());
    }

    public static int audioMaxAttemptsFromRetries(int maxRetries) {
        return clamp(maxRetries, 0, 10, DEFAULT_AUDIO_MAX_ATTEMPTS - 1) + 1;
    }

    public static int robustAudioMaxAttemptsFromRetries(int maxRetries) {
        return Math.max(DEFAULT_AUDIO_MAX_ATTEMPTS, audioMaxAttemptsFromRetries(maxRetries));
    }

    public int maxAttempts(GenerationTaskKind kind) {
        return kind != null && kind.image() ? imageMaxAttempts : audioMaxAttempts;
    }

    private static int clamp(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }
}
