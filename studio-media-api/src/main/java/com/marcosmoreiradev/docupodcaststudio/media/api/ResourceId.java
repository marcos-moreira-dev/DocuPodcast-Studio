package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Identifier for a scarce local execution resource. */
public record ResourceId(String value) implements Comparable<ResourceId> {
    public static final ResourceId MODEL_MEMORY = new ResourceId("model-memory");
    public static final ResourceId GPU = new ResourceId("gpu");
    public static final ResourceId CPU_HEAVY = new ResourceId("cpu-heavy");
    public static final ResourceId VIDEO_ENCODER = new ResourceId("video-encoder");

    public ResourceId {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("resource id is required");
    }

    @Override public int compareTo(ResourceId other) { return value.compareTo(other.value); }
}
