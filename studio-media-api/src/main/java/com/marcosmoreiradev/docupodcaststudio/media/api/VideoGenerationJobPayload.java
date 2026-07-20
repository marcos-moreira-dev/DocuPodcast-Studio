package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

public record VideoGenerationJobPayload(String prompt, String initialImageRelativePath,
                                        double durationSeconds,
                                        Map<String, String> portableFields) implements GenerationPayload {
    public VideoGenerationJobPayload {
        prompt = prompt == null ? "" : prompt.strip();
        initialImageRelativePath = initialImageRelativePath == null ? "" : initialImageRelativePath.replace('\\', '/').strip();
        durationSeconds = Math.max(0.1, durationSeconds);
        portableFields = portableFields == null ? Map.of() : Map.copyOf(portableFields);
    }
    @Override public String kind() { return "video-generation"; }
}
