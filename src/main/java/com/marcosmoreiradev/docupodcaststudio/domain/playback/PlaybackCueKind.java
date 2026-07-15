package com.marcosmoreiradev.docupodcaststudio.domain.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;

/** Minimal text origin carried by playback cues so pause timing is not inferred from raw text. */
public enum PlaybackCueKind {
    NARRATION(false),
    TITLE(true),
    HEADING(true),
    SUBHEADING(true),
    INDEX(true);

    private final boolean structuralText;

    PlaybackCueKind(boolean structuralText) {
        this.structuralText = structuralText;
    }

    public boolean structuralText() {
        return structuralText;
    }

    public static PlaybackCueKind fromSegmentType(NarrationSegmentType type) {
        if (type == null) {
            return NARRATION;
        }
        return switch (type) {
            case TITLE -> TITLE;
            case HEADING -> HEADING;
            case SUBHEADING -> SUBHEADING;
            default -> NARRATION;
        };
    }
}
