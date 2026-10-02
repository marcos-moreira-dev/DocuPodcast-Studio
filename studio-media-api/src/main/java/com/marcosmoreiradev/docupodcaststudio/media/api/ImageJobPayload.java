package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

public record ImageJobPayload(String prompt, int width, int height,
                              Map<String, String> portableFields) implements GenerationPayload {
    public ImageJobPayload {
        prompt = prompt == null ? "" : prompt.strip();
        width = Math.max(64, width);
        height = Math.max(64, height);
        portableFields = portableFields == null ? Map.of() : Map.copyOf(portableFields);
    }
    @Override public String kind() { return "image-generation"; }
}
