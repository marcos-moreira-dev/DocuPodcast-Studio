package com.marcosmoreiradev.docupodcaststudio.application.theatre;

/** Observable progress for theatre multi-character synthesis and mixing. */
public record TheatreChoralVoiceRenderProgress(int completedSteps, int totalSteps, String message) {
    public TheatreChoralVoiceRenderProgress {
        totalSteps = Math.max(1, totalSteps);
        completedSteps = Math.max(0, Math.min(completedSteps, totalSteps));
        message = message == null ? "" : message.strip();
    }

    public double fraction() {
        return (double) completedSteps / (double) totalSteps;
    }
}
