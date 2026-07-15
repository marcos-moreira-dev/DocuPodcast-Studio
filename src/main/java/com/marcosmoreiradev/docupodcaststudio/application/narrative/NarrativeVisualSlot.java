package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;

/** One visual slot attached to a fragment in Video narrativo. */
public record NarrativeVisualSlot(
        FragmentAssetRole role,
        String bindingId,
        String assetId,
        String assetPath,
        FragmentAssetSource source,
        String prompt,
        String notes
) {
    public NarrativeVisualSlot {
        role = role == null ? FragmentAssetRole.MAIN_IMAGE : role;
        bindingId = normalize(bindingId);
        assetId = normalize(assetId);
        assetPath = normalize(assetPath).replace('\\', '/');
        source = source == null ? FragmentAssetSource.UNKNOWN : source;
        prompt = normalize(prompt);
        notes = normalize(notes);
    }

    public static NarrativeVisualSlot empty(FragmentAssetRole role) {
        return new NarrativeVisualSlot(role, "", "", "", FragmentAssetSource.UNKNOWN, "", "");
    }

    public boolean assigned() {
        return !assetId.isBlank() || !assetPath.isBlank();
    }

    public boolean mainImage() {
        return role == FragmentAssetRole.MAIN_IMAGE;
    }

    public boolean bridgeImage() {
        return role == FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
