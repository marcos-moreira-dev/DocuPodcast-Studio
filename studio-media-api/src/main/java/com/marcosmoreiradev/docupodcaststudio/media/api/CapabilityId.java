package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Extensible capability identifier. It intentionally is not an enum. */
public record CapabilityId(String value) {
    public static final CapabilityId VOICE_SYNTHESIS = new CapabilityId("voice-synthesis");
    public static final CapabilityId IMAGE_GENERATION = new CapabilityId("image-generation");
    public static final CapabilityId VIDEO_GENERATION = new CapabilityId("video-generation");
    public static final CapabilityId VIDEO_RENDERING = new CapabilityId("video-rendering");

    public CapabilityId {
        value = normalize(value);
        if (value.isBlank()) throw new IllegalArgumentException("capability id is required");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
