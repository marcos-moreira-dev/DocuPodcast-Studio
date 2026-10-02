package com.marcosmoreiradev.docupodcaststudio.domain.study;

/** Presentation policy; independent of the image generation engine and its cache. */
public record DocumentAiIllustrationAppearance(boolean background, double opacity, String fit) {
    public DocumentAiIllustrationAppearance {
        opacity = Double.isFinite(opacity) ? Math.max(0.05, Math.min(1, opacity)) : 0.35;
        fit = java.util.Set.of("CONTAIN", "COVER", "BLUR_AND_CONTAIN").contains(fit == null ? "" : fit) ? fit : "COVER";
    }
    public static DocumentAiIllustrationAppearance defaults() {
        return new DocumentAiIllustrationAppearance(false, 0.35, "COVER");
    }
}
