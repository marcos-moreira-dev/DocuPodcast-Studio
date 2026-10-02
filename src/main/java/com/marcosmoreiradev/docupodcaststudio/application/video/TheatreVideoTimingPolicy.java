package com.marcosmoreiradev.docupodcaststudio.application.video;

/** Keeps spoken theatre cuts tight while reserving enough time to read truly silent stage directions. */
final class TheatreVideoTimingPolicy {
    static final double SPOKEN_TAIL_SECONDS = 0.25;
    private static final double WORDS_PER_SECOND = 2.6;
    private static final double READING_MARGIN_SECONDS = 1.25;
    private static final double MAX_SILENT_STAGE_DIRECTION_SECONDS = 45.0;

    private TheatreVideoTimingPolicy() {
    }

    static double spokenTailSeconds() {
        return SPOKEN_TAIL_SECONDS;
    }

    static double silentStageDirectionSeconds(String text, double configuredSilentVisualSeconds) {
        String normalized = text == null ? "" : text.strip();
        int words = normalized.isBlank() ? 0 : normalized.split("\\s+").length;
        double readingSeconds = words / WORDS_PER_SECOND + READING_MARGIN_SECONDS;
        double configured = Math.max(1.0, configuredSilentVisualSeconds);
        return Math.min(MAX_SILENT_STAGE_DIRECTION_SECONDS, Math.max(configured, readingSeconds));
    }
}
