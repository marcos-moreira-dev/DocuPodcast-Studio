package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;

import java.util.Map;

/** Explicit user request to create a visual candidate for one fragment and role. */
public record VisualGenerationRequest(
        String fragmentId,
        String segmentId,
        FragmentAssetRole role,
        String title,
        String fragmentText,
        String prompt,
        String notes,
        Map<String, String> metadata
) {
    public VisualGenerationRequest {
        fragmentId = normalize(fragmentId);
        segmentId = normalize(segmentId);
        role = role == null ? FragmentAssetRole.MAIN_IMAGE : role;
        title = normalize(title);
        fragmentText = normalize(fragmentText);
        prompt = normalize(prompt);
        notes = normalize(notes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
