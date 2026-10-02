package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

public record VoiceJobPayload(List<String> unitIds, String language,
                              Map<String, String> portableFields) implements GenerationPayload {
    public VoiceJobPayload {
        unitIds = unitIds == null ? List.of() : List.copyOf(unitIds);
        language = language == null || language.isBlank() ? "es" : language.strip();
        portableFields = portableFields == null ? Map.of() : Map.copyOf(portableFields);
    }
    @Override public String kind() { return "voice-synthesis"; }
}
