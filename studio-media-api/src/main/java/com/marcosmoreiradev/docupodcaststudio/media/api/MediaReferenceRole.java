package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Semantic role of a conditioning or continuity reference. */
public record MediaReferenceRole(String value) {
    public static final MediaReferenceRole IDENTITY = new MediaReferenceRole("identity");
    public static final MediaReferenceRole REGIONAL_IDENTITY = new MediaReferenceRole("regional-identity");
    public static final MediaReferenceRole OBJECT = new MediaReferenceRole("object");
    public static final MediaReferenceRole ENVIRONMENT = new MediaReferenceRole("environment");
    public static final MediaReferenceRole STYLE = new MediaReferenceRole("style");
    public static final MediaReferenceRole START_FRAME = new MediaReferenceRole("start-frame");
    public static final MediaReferenceRole PREVIOUS_FRAME = new MediaReferenceRole("previous-frame");
    public static final MediaReferenceRole NEXT_FRAME = new MediaReferenceRole("next-frame");
    public static final MediaReferenceRole COMPOSITION_GUIDE = new MediaReferenceRole("composition-guide");
    public static final MediaReferenceRole DRAWN_GUIDE = new MediaReferenceRole("drawn-guide");
    public static final MediaReferenceRole CAMERA_GUIDE = new MediaReferenceRole("camera-guide");

    public MediaReferenceRole {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("media reference role is required");
    }
}
