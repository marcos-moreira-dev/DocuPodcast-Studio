package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public record GenerationArtifact(String kind, URI location, Map<String, String> metadata) {
    public GenerationArtifact {
        kind = kind == null || kind.isBlank() ? "artifact" : kind.strip();
        location = Objects.requireNonNull(location, "location");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
