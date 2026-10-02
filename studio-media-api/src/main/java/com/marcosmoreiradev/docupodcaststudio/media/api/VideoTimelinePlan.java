package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Map;

/** Provider-neutral deterministic video composition. */
public record VideoTimelinePlan(
        List<VideoTimelineItem> items,
        List<TimelineAudioTrack> overlays,
        int width,
        int height,
        int framesPerSecond,
        VideoEncodingPreference encodingPreference,
        Map<String, String> metadata) {
    public VideoTimelinePlan {
        items = items == null ? List.of() : List.copyOf(items);
        overlays = overlays == null ? List.of() : List.copyOf(overlays);
        width = Math.max(320, width);
        height = Math.max(180, height);
        framesPerSecond = Math.max(1, Math.min(120, framesPerSecond));
        encodingPreference = encodingPreference == null ? VideoEncodingPreference.AUTO : encodingPreference;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (items.isEmpty()) throw new IllegalArgumentException("video timeline items are required");
    }

    public double durationSeconds() {
        return items.stream().mapToDouble(VideoTimelineItem::durationSeconds).sum();
    }
}
