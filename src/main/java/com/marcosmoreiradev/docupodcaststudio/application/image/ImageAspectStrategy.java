package com.marcosmoreiradev.docupodcaststudio.application.image;

/** Explicit aspect-ratio strategy used before upscale or final delivery. */
public enum ImageAspectStrategy {
    PRESERVE_WITH_PADDING("Ajustar con bordes", false),
    CENTER_CROP("Recortar al centro", false),
    OUTPAINT_TO_TARGET("Expandir a 16:9 / outpainting", true),
    KEEP_PROPORTION("Mantener proporcion", false);

    private final String displayName;
    private final boolean requiresOutpainting;

    ImageAspectStrategy(String displayName, boolean requiresOutpainting) {
        this.displayName = displayName;
        this.requiresOutpainting = requiresOutpainting;
    }

    public String displayName() {
        return displayName;
    }

    public boolean requiresOutpainting() {
        return requiresOutpainting;
    }
}
