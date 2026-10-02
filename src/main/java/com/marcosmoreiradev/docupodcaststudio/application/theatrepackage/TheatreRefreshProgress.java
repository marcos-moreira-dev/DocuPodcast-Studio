package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import java.util.Objects;

public record TheatreRefreshProgress(TheatreRefreshStage stage, int completed, int total, String message) {
    public TheatreRefreshProgress {
        stage = Objects.requireNonNull(stage, "stage");
        completed = Math.max(0, completed);
        total = Math.max(completed, total);
        message = Objects.requireNonNullElse(message, "").strip();
    }

    public double fraction() { return total == 0 ? -1.0 : Math.min(1.0, (double) completed / total); }
}
