package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.UUID;

public record GenerationJobId(String value) {
    public GenerationJobId {
        value = value == null ? "" : value.strip();
        if (value.isBlank()) throw new IllegalArgumentException("generation job id is required");
    }

    public static GenerationJobId create() { return new GenerationJobId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
