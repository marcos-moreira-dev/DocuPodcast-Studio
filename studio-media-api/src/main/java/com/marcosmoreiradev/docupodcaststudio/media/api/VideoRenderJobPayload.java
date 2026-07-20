package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

public record VideoRenderJobPayload(List<String> timelineItemIds,
                                    Map<String, String> portableFields) implements GenerationPayload {
    public VideoRenderJobPayload {
        timelineItemIds = timelineItemIds == null ? List.of() : List.copyOf(timelineItemIds);
        portableFields = portableFields == null ? Map.of() : Map.copyOf(portableFields);
    }
    @Override public String kind() { return "video-rendering"; }
}
