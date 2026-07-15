package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** Global defaults for voice synthesis shown in the dedicated Settings surface. */
public record VoiceSynthesisSettings(
        double speechRate,
        double volume,
        double sentencePauseSeconds,
        double paragraphPauseSeconds,
        String defaultLanguage
) {
    public VoiceSynthesisSettings {
        if (speechRate < 0.5 || speechRate > 2.0) {
            throw new IllegalArgumentException("speech rate must be between 0.5x and 2.0x");
        }
        if (volume < 0.0 || volume > 1.5) {
            throw new IllegalArgumentException("volume must be between 0.0 and 1.5");
        }
        if (sentencePauseSeconds < 0.0 || sentencePauseSeconds > 5.0) {
            throw new IllegalArgumentException("sentence pause must be between 0 and 5 seconds");
        }
        if (paragraphPauseSeconds < 0.0 || paragraphPauseSeconds > 10.0) {
            throw new IllegalArgumentException("paragraph pause must be between 0 and 10 seconds");
        }
        defaultLanguage = defaultLanguage == null || defaultLanguage.isBlank() ? "es" : defaultLanguage.strip();
    }

    public static VoiceSynthesisSettings defaults() {
        return new VoiceSynthesisSettings(1.0, 1.0, 0.4, 0.8, "es");
    }

    public String speechRateLabel() {
        return String.format("%.2fx", speechRate);
    }

    public String volumeLabel() {
        return Math.round(volume * 100) + "%";
    }
}
