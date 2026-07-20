package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Semantic role of a conditioning or continuity reference. */
public record MediaReferenceRole(String value) {
    public static final MediaReferenceRole IDENTITY = new MediaReferenceRole("identity");
    public static final MediaReferenceRole OBJECT = new MediaReferenceRole("object");
    public static final MediaReferenceRole ENVIRONMENT = new MediaReferenceRole("environment");
    public static final MediaReferenceRole STYLE = new MediaReferenceRole("style");
    public static final MediaReferenceRole START_FRAME = new MediaReferenceRole("start-frame");

    public MediaReferenceRole {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("media reference role is required");
    }
}
