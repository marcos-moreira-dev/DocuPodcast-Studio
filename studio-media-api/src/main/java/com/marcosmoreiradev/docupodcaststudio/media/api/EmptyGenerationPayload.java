package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

public record EmptyGenerationPayload(Map<String, String> portableFields) implements GenerationPayload {
    public EmptyGenerationPayload {
        portableFields = portableFields == null ? Map.of() : Map.copyOf(portableFields);
    }
    @Override public String kind() { return "empty"; }
}
