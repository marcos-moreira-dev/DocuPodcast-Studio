package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;

import java.util.Map;

/** One visual slot or traced visual asset in a project production view. */
public record VisualSlotState(
        FragmentAssetRole role,
        String bindingId,
        String assetId,
        String assetPath,
        FragmentAssetSource source,
        String status,
        String prompt,
        String notes,
        boolean missingAsset,
        Map<String, String> metadata
) {
    public VisualSlotState {
        role = role == null ? FragmentAssetRole.NOTE : role;
        bindingId = normalize(bindingId);
        assetId = normalize(assetId);
        assetPath = normalize(assetPath).replace('\\', '/');
        source = source == null ? FragmentAssetSource.UNKNOWN : source;
        status = normalize(status).isBlank() ? "UNKNOWN" : normalize(status);
        prompt = normalize(prompt);
        notes = normalize(notes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static VisualSlotState empty(FragmentAssetRole role) {
        return new VisualSlotState(role, "", "", "", FragmentAssetSource.UNKNOWN, "MISSING", "", "", false, Map.of());
    }

    public boolean assigned() {
        return !assetId.isBlank() || !assetPath.isBlank();
    }

    public boolean ready() {
        return assigned() && !missingAsset && !"MISSING".equalsIgnoreCase(status);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
