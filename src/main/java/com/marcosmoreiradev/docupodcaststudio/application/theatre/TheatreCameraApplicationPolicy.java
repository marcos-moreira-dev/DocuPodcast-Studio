package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-fragment switch that decides whether the assigned theatre camera is used as visual context. */
public final class TheatreCameraApplicationPolicy {
    public static final String APPLY_CAMERA_METADATA = "theatreApplyCamera";

    public boolean appliesToSegment(StoryboardDocument storyboard,
            com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment segment) {
        if (segment == null) return true;
        if (storyboard != null && storyboard.bindingForSegment(segment.id()).isPresent()) return applies(storyboard, segment.id());
        return !isFalse(segment.metadata().get(APPLY_CAMERA_METADATA));
    }

    public boolean applies(StoryboardDocument storyboard, String segmentId) {
        if (storyboard == null || segmentId == null || segmentId.isBlank()) {
            return true;
        }
        return storyboard.bindingForSegment(segmentId)
                .map(StoryboardBinding::metadata)
                .map(metadata -> !isFalse(metadata.get(APPLY_CAMERA_METADATA)))
                .orElse(true);
    }

    public Map<String, String> metadataWithApplyCamera(Map<String, String> metadata, boolean applyCamera) {
        LinkedHashMap<String, String> next = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        if (applyCamera) {
            next.remove(APPLY_CAMERA_METADATA);
        } else {
            next.put(APPLY_CAMERA_METADATA, "false");
        }
        return Map.copyOf(next);
    }

    private static boolean isFalse(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("false") || normalized.equals("no") || normalized.equals("0");
    }
}
