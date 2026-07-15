package com.marcosmoreiradev.docupodcaststudio.domain.render;

/**
 * Effective unit mode used by the cross-media render contract.
 *
 * <p>TI1 separates the document/script projection from the final media decision:
 * a unit can produce narration, visual output, both, or be intentionally omitted
 * from video/storyboard rendering.</p>
 */
public enum RenderUnitKind {
    /** Narrated unit without a visual assigned. It is audio/playback material, but not a video frame. */
    SPOKEN_ONLY,
    /** Narrated unit with a visual assigned. It can become image + audio in video. */
    SPOKEN_WITH_VISUAL,
    /** Visual unit with no narration. It renders silently using configured still duration. */
    VISUAL_SILENT,
    /** Unit intentionally skipped from visual rendering. */
    OMITTED;

    public boolean spoken() {
        return this == SPOKEN_ONLY || this == SPOKEN_WITH_VISUAL;
    }

    public boolean visual() {
        return this == SPOKEN_WITH_VISUAL || this == VISUAL_SILENT;
    }

    public boolean silentVisual() {
        return this == VISUAL_SILENT;
    }

    public boolean omitted() {
        return this == OMITTED;
    }
}
