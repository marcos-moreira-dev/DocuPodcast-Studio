package com.marcosmoreiradev.docupodcaststudio.domain.batch;

/** Only the per-document properties; null visibility inherits the common value. */
public record DocumentBackgroundOverride(String imagePath, Double visibility) {
    public DocumentBackgroundOverride {
        imagePath = imagePath == null ? "" : imagePath.strip();
        if (visibility != null) visibility = Double.isFinite(visibility) ? Math.max(0.05, Math.min(1, visibility)) : null;
    }
}
